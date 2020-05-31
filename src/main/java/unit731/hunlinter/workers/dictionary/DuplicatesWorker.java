package unit731.hunlinter.workers.dictionary;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.datastructures.bloomfilter.BloomFilterInterface;
import unit731.hunlinter.datastructures.bloomfilter.BloomFilterParameters;
import unit731.hunlinter.datastructures.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.dictionary.Duplicate;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.ParserHelper;

import static unit731.hunlinter.services.system.LoopHelper.forEach;


public class DuplicatesWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(DuplicatesWorker.class);


	private static class DuplicatesDictionaryBaseData extends BloomFilterParameters{

		private static class SingletonHelper{
			private static final DuplicatesDictionaryBaseData INSTANCE = new DuplicatesDictionaryBaseData();
		}


		public static DuplicatesDictionaryBaseData getInstance(){
			return SingletonHelper.INSTANCE;
		}

		protected DuplicatesDictionaryBaseData(){}

		@Override
		public int getExpectedNumberOfElements(){
			return 1_000_000;
		}

		@Override
		public double getFalsePositiveProbability(){
			return 0.000_000_4;
		}

		@Override
		public double getGrowRatioWhenFull(){
			return 1.3;
		}

	}

	public static final String WORKER_NAME = "Duplicates extraction";


	private final DictionaryParser dicParser;
	private final WordGenerator wordGenerator;

	private final Comparator<String> comparator;
	private final BloomFilterParameters dictionaryBaseData;


	public DuplicatesWorker(final ParserManager parserManager, final File outputFile){
		this(parserManager.getLanguage(), parserManager.getDicParser(), parserManager.getWordGenerator(), outputFile);
	}

	public DuplicatesWorker(final String language, final DictionaryParser dicParser, final WordGenerator wordGenerator,
			final File outputFile){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		Objects.requireNonNull(language);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(outputFile);


		this.dicParser = dicParser;
		this.wordGenerator = wordGenerator;

		comparator = BaseBuilder.getComparator(language);
		dictionaryBaseData = BaseBuilder.getDictionaryBaseData(language);

		final Function<Void, BloomFilterInterface<String>> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/3)");

			return collectDuplicates();
		};
		final Function<BloomFilterInterface<String>, List<Duplicate>> step2 = this::extractDuplicates;
		final Function<List<Duplicate>, File> step3 = duplicates -> {
			writeDuplicates(outputFile, duplicates);

			finalizeProcessing("Duplicates extracted successfully");

			return outputFile;
		};
		final Function<File, Void> step4 = WorkerManager.openFileStep(LOGGER);
		setProcessor(step1.andThen(step2).andThen(step3).andThen(step4));
	}

	private BloomFilterInterface<String> collectDuplicates(){
		final File dicFile = dicParser.getDicFile();
		final Charset charset = dicParser.getCharset();

		final BloomFilterInterface<String> bloomFilter = new ScalableInMemoryBloomFilter<>(charset, dictionaryBaseData);
		final BloomFilterInterface<String> duplicatesBloomFilter = new ScalableInMemoryBloomFilter<>(charset,
			DuplicatesDictionaryBaseData.getInstance());

		final BiConsumer<Integer, String> fun = (lineIndex, line) -> {
			try{
				final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
				final Inflection[] inflections = wordGenerator.applyAffixRules(dicEntry);

				for(final Inflection inflection : inflections){
					final String str = inflection.toStringWithPartOfSpeechAndStem();
					if(!bloomFilter.add(str))
						duplicatesBloomFilter.add(str);
				}
			}
			catch(final LinterException e){
				LOGGER.info(ParserManager.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), lineIndex, line);
			}
		};
		final Consumer<Integer> progressCallback = lineIndex -> {
			setProgress(lineIndex);

			sleepOnPause();
		};
		ParserHelper.forEachLine(dicFile, charset, fun, progressCallback,
			ParserHelper.COMMENT_MARK_SHARP, ParserHelper.COMMENT_MARK_SLASH);

		bloomFilter.close();
		final int totalInflections = bloomFilter.getAddedElements();
		final double falsePositiveProbability = bloomFilter.getTrueFalsePositiveProbability();
		bloomFilter.clear();

		duplicatesBloomFilter.close();

		final int falsePositiveCount = (int)Math.ceil(totalInflections * falsePositiveProbability);
		LOGGER.info(ParserManager.MARKER_APPLICATION, "Total inflections: {}", DictionaryParser.COUNTER_FORMATTER.format(totalInflections));
		LOGGER.info(ParserManager.MARKER_APPLICATION, "False positive probability is {} (overall duplicates ≲ {})",
			DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability), falsePositiveCount);

		return duplicatesBloomFilter;
	}

	private List<Duplicate> extractDuplicates(final BloomFilterInterface<String> duplicatesBloomFilter){
		final ArrayList<Duplicate> result = new ArrayList<>();

		if(duplicatesBloomFilter.getAddedElements() > 0){
			resetProcessing("Extracting duplicates (step 2/3)");

			final Charset charset = dicParser.getCharset();
			final File dicFile = dicParser.getDicFile();
			final BiConsumer<Integer, String> fun = (lineIndex, line) -> {
				try{
					final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
					final Inflection[] inflections = wordGenerator.applyAffixRules(dicEntry);

					final String word = inflections[WordGenerator.BASE_INFLECTION_INDEX].getWord();
					result.ensureCapacity(result.size() + inflections.length);
					for(final Inflection inflection : inflections){
						final String text = inflection.toStringWithPartOfSpeechAndStem();
						if(duplicatesBloomFilter.contains(text))
							result.add(new Duplicate(inflection, word, lineIndex));
					}
				}
				catch(final LinterException e){
					LOGGER.info(ParserManager.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), lineIndex, line);
				}
			};
			final Consumer<Integer> progressCallback = lineIndex -> {
				setProgress(lineIndex);

				sleepOnPause();
			};
			ParserHelper.forEachLine(dicFile, charset, fun, progressCallback,
				ParserHelper.COMMENT_MARK_SHARP, ParserHelper.COMMENT_MARK_SLASH);

			final int totalDuplicates = duplicatesBloomFilter.getAddedElements();
			final double falsePositiveProbability = duplicatesBloomFilter.getTrueFalsePositiveProbability();
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Total duplicates: {}", DictionaryParser.COUNTER_FORMATTER.format(totalDuplicates));
			LOGGER.info(ParserManager.MARKER_APPLICATION, "False positive probability is {} (overall duplicates ≲ {})",
				DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability), (int)Math.ceil(totalDuplicates * falsePositiveProbability));

			duplicatesBloomFilter.clear();

			result.sort((d1, d2) -> comparator.compare(d1.getInflection().getWord(), d2.getInflection().getWord()));
		}
		else
			LOGGER.info(ParserManager.MARKER_APPLICATION, "No duplicates found, skip remaining steps");

		return result;
	}

	private void writeDuplicates(final File duplicatesFile, final List<Duplicate> duplicates){
		final int totalSize = duplicates.size();
		if(totalSize > 0){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Write results to file (step 3/3)");

			int writtenSoFar = 0;
			final List<List<Duplicate>> mergedDuplicates = mergeDuplicates(duplicates);
			try(final BufferedWriter writer = Files.newBufferedWriter(duplicatesFile.toPath(), dicParser.getCharset())){
				for(final List<Duplicate> entries : mergedDuplicates){
					final Inflection prod = entries.get(0).getInflection();
					final String origin = prod.getWord() + "(" + String.join(", ", prod.getMorphologicalFieldPartOfSpeech())
						+ "): ";
					writer.write(origin);
					final StringJoiner sj = new StringJoiner(", ");
					forEach(entries,
						duplicate -> sj.add(StringUtils.join(Arrays.asList(duplicate.getWord(), " (", Integer.toString(duplicate.getLineIndex()), (duplicate.getInflection().hasInflectionRules()? " via " + duplicate.getInflection().getRulesSequence(): StringUtils.EMPTY), ")"), StringUtils.EMPTY)));
					writer.write(sj.toString());
					writer.newLine();

					setProgress(++ writtenSoFar, totalSize);

					sleepOnPause();
				}
			}
			catch(final Exception e){
				throw new RuntimeException(e);
			}

			LOGGER.info(ParserManager.MARKER_APPLICATION, "File written: {}", duplicatesFile.getAbsolutePath());
		}
	}

	private List<List<Duplicate>> mergeDuplicates(final List<Duplicate> duplicates){
		final Map<String, List<Duplicate>> dupls = duplicates.stream()
			.collect(Collectors.toMap(duplicate -> duplicate.getInflection().toStringWithPartOfSpeechAndStem(),
				duplicate -> {
					final List<Duplicate> list = new ArrayList<>(1);
					list.add(duplicate);
					return list;
				},
				(oldValue, newValue) -> {
					oldValue.addAll(newValue);
					return oldValue;
				}));

		final List<List<Duplicate>> result = new ArrayList<>(dupls.values());
		result.sort(Comparator.<List<Duplicate>>comparingInt(List::size).reversed()
			.thenComparing(list -> list.get(0).getInflection().getWord(), comparator));
		return result;
	}

}
