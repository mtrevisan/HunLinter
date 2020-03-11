package unit731.hunlinter.workers.dictionary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.collections.bloomfilter.BloomFilterInterface;
import unit731.hunlinter.collections.bloomfilter.BloomFilterParameters;
import unit731.hunlinter.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.dictionary.Duplicate;
import unit731.hunlinter.parsers.enums.MorphologicalTag;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.ParserHelper;


public class DuplicatesWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(DuplicatesWorker.class);

	private static final MessageFormat WRONG_FILE_FORMAT = new MessageFormat("Dictionary file malformed, the first line is not a number, was ''{0}''");


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


	public DuplicatesWorker(final String language, final DictionaryParser dicParser, final WordGenerator wordGenerator,
			final File outputFile){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser)
			.withParallelProcessing(true));

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
		final Function<File, Void> step4 = file -> {
			try{
				FileHelper.openFileWithChosenEditor(file);
			}
			catch(final IOException | InterruptedException e){
				LOGGER.warn("Exception while opening the resulting file", e);
			}

			return null;
		};
		setProcessor(step1.andThen(step2).andThen(step3).andThen(step4));
	}

	private BloomFilterInterface<String> collectDuplicates(){
		final Charset charset = dicParser.getCharset();
		final BloomFilterInterface<String> bloomFilter = new ScalableInMemoryBloomFilter<>(charset, dictionaryBaseData);
		final BloomFilterInterface<String> duplicatesBloomFilter = new ScalableInMemoryBloomFilter<>(charset,
			DuplicatesDictionaryBaseData.getInstance());

		final File dicFile = dicParser.getDicFile();
		try(final LineNumberReader br = FileHelper.createReader(dicFile.toPath(), charset)){
			String line = ParserHelper.extractLine(br);

			long readSoFar = line.getBytes(charset).length + 2;

			if(!NumberUtils.isCreatable(line))
				throw new LinterException(WRONG_FILE_FORMAT.format(new Object[]{line}));

			int lineIndex = 1;
			final long totalSize = dicFile.length();
			while((line = br.readLine()) != null){
				lineIndex ++;
				readSoFar += line.getBytes(charset).length + 2;
				line = ParserHelper.cleanLine(line);
				if(!line.isEmpty()){
					try{
						final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
						final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);

						productions.stream()
							.map(Production::toStringWithPartOfSpeechFields)
							.filter(Predicate.not(bloomFilter::add))
							.forEach(duplicatesBloomFilter::add);
					}
					catch(final LinterException e){
						LOGGER.error(ParserManager.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), lineIndex, line);
					}
				}

				setProgress(readSoFar, totalSize);

				sleepOnPause();
			}

			bloomFilter.close();
			bloomFilter.clear();
			duplicatesBloomFilter.close();

			setProgress(100);

			final int totalProductions = bloomFilter.getAddedElements();
			final double falsePositiveProbability = bloomFilter.getTrueFalsePositiveProbability();
			final int falsePositiveCount = (int)Math.ceil(totalProductions * falsePositiveProbability);
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Total productions: {}", DictionaryParser.COUNTER_FORMATTER.format(totalProductions));
			LOGGER.info(ParserManager.MARKER_APPLICATION, "False positive probability is {} (overall duplicates ≲ {})",
				DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability), falsePositiveCount);

			return duplicatesBloomFilter;
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}

	private List<Duplicate> extractDuplicates(final BloomFilterInterface<String> duplicatesBloomFilter){
		final List<Duplicate> result = new ArrayList<>();

		if(duplicatesBloomFilter.getAddedElements() > 0){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Extracting duplicates (step 2/3)");
			setProgress(0);

			final Charset charset = dicParser.getCharset();
			final File dicFile = dicParser.getDicFile();
			try(final LineNumberReader br = new LineNumberReader(Files.newBufferedReader(dicFile.toPath(), charset))){
				String line = br.readLine();

				long readSoFar = line.getBytes(charset).length + 2;

				int lineIndex = 1;
				final long totalSize = dicFile.length();
				while((line = br.readLine()) != null){
					lineIndex ++;
					readSoFar += line.getBytes(charset).length + 2;
					line = ParserHelper.cleanLine(line);
					if(!line.isEmpty()){
						try{
							final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
							final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);
							final String word = productions.get(WordGenerator.BASE_PRODUCTION_INDEX).getWord();
							for(final Production production : productions){
								final String text = production.toStringWithPartOfSpeechFields();
								if(duplicatesBloomFilter.contains(text))
									result.add(new Duplicate(production, word, lineIndex));
							}
						}
						catch(final Exception e){
							LOGGER.warn(ParserManager.MARKER_APPLICATION, e.getMessage());
						}
					}

					setProgress(readSoFar, totalSize);

					sleepOnPause();
				}
			}
			catch(final Exception e){
				throw new RuntimeException(e);
			}
			setProgress(100);

			final int totalDuplicates = duplicatesBloomFilter.getAddedElements();
			final double falsePositiveProbability = duplicatesBloomFilter.getTrueFalsePositiveProbability();
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Total duplicates: {}", DictionaryParser.COUNTER_FORMATTER.format(totalDuplicates));
			LOGGER.info(ParserManager.MARKER_APPLICATION, "False positive probability is {} (overall duplicates ≲ {})",
				DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability), (int)Math.ceil(totalDuplicates * falsePositiveProbability));

			duplicatesBloomFilter.clear();

			result.sort((d1, d2) -> comparator.compare(d1.getProduction().getWord(), d2.getProduction().getWord()));
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
			setProgress(1, totalSize + 1);
			try(final BufferedWriter writer = Files.newBufferedWriter(duplicatesFile.toPath(), dicParser.getCharset())){
				for(final List<Duplicate> entries : mergedDuplicates){
					final Production prod = entries.get(0).getProduction();
					final String origin = prod.getWord() + prod.getMorphologicalFields(MorphologicalTag.TAG_PART_OF_SPEECH).stream()
						.collect(Collectors.joining(", ", " (", "): "));
					writer.write(origin);
					writer.write(entries.stream()
						.map(duplicate ->
							StringUtils.join(Arrays.asList(duplicate.getWord(), " (", Integer.toString(duplicate.getLineIndex()),
								(duplicate.getProduction().hasProductionRules()? " via " + duplicate.getProduction().getRulesSequence(): StringUtils.EMPTY), ")"),
								StringUtils.EMPTY)
						)
						.collect(Collectors.joining(", ")));
					writer.newLine();

					setProgress(++ writtenSoFar, totalSize + 1);

					sleepOnPause();
				}
			}
			catch(final Exception e){
				throw new RuntimeException(e);
			}
			setProgress(100);

			LOGGER.info(ParserManager.MARKER_APPLICATION, "File written: {}", duplicatesFile.getAbsolutePath());
		}
	}

	private List<List<Duplicate>> mergeDuplicates(final List<Duplicate> duplicates){
		final Map<String, List<Duplicate>> dupls = duplicates.stream()
			.collect(Collectors.toMap(duplicate -> duplicate.getProduction().toStringWithPartOfSpeechFields(),
				duplicate -> {
					final List<Duplicate> list = new ArrayList<>();
					list.add(duplicate);
					return list;
				},
				(oldValue, newValue) -> {
					oldValue.addAll(newValue);
					return oldValue;
				}));

		final List<List<Duplicate>> result = new ArrayList<>(dupls.values());
		result.sort(Comparator.<List<Duplicate>>comparingInt(List::size).reversed()
			.thenComparing(list -> list.get(0).getProduction().getWord(), comparator));
		return result;
	}

}
