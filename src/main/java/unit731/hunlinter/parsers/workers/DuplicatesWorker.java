package unit731.hunlinter.parsers.workers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.Backbone;
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
import unit731.hunlinter.parsers.workers.core.WorkerBase;
import unit731.hunlinter.parsers.workers.core.WorkerData;
import unit731.hunlinter.parsers.workers.exceptions.HunLintException;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.ParserHelper;


public class DuplicatesWorker extends WorkerBase<Void, Void>{

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
	private final File outputFile;

	private final Comparator<String> comparator;
	private final BloomFilterParameters dictionaryBaseData;


	public DuplicatesWorker(final String language, final DictionaryParser dicParser, final WordGenerator wordGenerator, final File outputFile){
		Objects.requireNonNull(language);
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(outputFile);

		this.dicParser = dicParser;
		this.wordGenerator = wordGenerator;
		this.outputFile = outputFile;

		workerData = WorkerData.createParallelPreventExceptionRelaunch(WORKER_NAME, dicParser);
		comparator = BaseBuilder.getComparator(language);
		dictionaryBaseData = BaseBuilder.getDictionaryBaseData(language);
	}

	@Override
	protected Void doInBackground(){
		try{
			exception = null;

			LOGGER.info(Backbone.MARKER_APPLICATION, "Opening Dictionary file for duplicates extraction (pass 1/3)");

			watch.reset();

			final BloomFilterInterface<String> duplicatesBloomFilter = collectDuplicates();

			final List<Duplicate> duplicates = extractDuplicates(duplicatesBloomFilter);

			writeDuplicates(duplicates);

			watch.stop();

			LOGGER.info(Backbone.MARKER_APPLICATION, "Duplicates extracted successfully (in {})", watch.toStringMinuteSeconds());

			if(!duplicates.isEmpty()){
				try{
					FileHelper.openFileWithChosenEditor(outputFile);
				}
				catch(final IOException | InterruptedException e){
					LOGGER.warn("Exception while opening the resulting file", e);
				}
			}
		}
		catch(final Exception e){
			exception = e;

			if(e instanceof ClosedChannelException)
				LOGGER.warn(Backbone.MARKER_APPLICATION, "Duplicates thread interrupted");
			else{
				LOGGER.error(Backbone.MARKER_APPLICATION, e.getMessage());
			}

			LOGGER.info(Backbone.MARKER_APPLICATION, "Stopped reading Dictionary file", e);

			cancel(true);
		}

		return null;
	}

	private BloomFilterInterface<String> collectDuplicates() throws IOException{
		final BloomFilterInterface<String> bloomFilter = new ScalableInMemoryBloomFilter<>(dicParser.getCharset(), dictionaryBaseData);
		final BloomFilterInterface<String> duplicatesBloomFilter = new ScalableInMemoryBloomFilter<>(dicParser.getCharset(),
			DuplicatesDictionaryBaseData.getInstance());

		setProgress(0);
		final File dicFile = dicParser.getDicFile();
		final Charset charset = getCharset();
		try(final LineNumberReader br = FileHelper.createReader(dicFile.toPath(), dicParser.getCharset())){
			String line = ParserHelper.extractLine(br);

			long readSoFar = line.getBytes(charset).length + 2;

			if(!NumberUtils.isCreatable(line))
				throw new HunLintException(WRONG_FILE_FORMAT.format(new Object[]{line}));

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
					catch(final HunLintException e){
						LOGGER.error(Backbone.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), lineIndex, line);
					}
				}

				setProgress(getProgress(readSoFar, totalSize));
			}

			bloomFilter.close();
			duplicatesBloomFilter.close();
		}
		bloomFilter.clear();

		setProgress(100);

		final int totalProductions = bloomFilter.getAddedElements();
		final double falsePositiveProbability = bloomFilter.getTrueFalsePositiveProbability();
		final int falsePositiveCount = (int)Math.ceil(totalProductions * falsePositiveProbability);
		LOGGER.info(Backbone.MARKER_APPLICATION, "Total productions: {}", DictionaryParser.COUNTER_FORMATTER.format(totalProductions));
		LOGGER.info(Backbone.MARKER_APPLICATION, "False positive probability is {} (overall duplicates ≲ {})",
			DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability), falsePositiveCount);

		return duplicatesBloomFilter;
	}

	private List<Duplicate> extractDuplicates(final BloomFilterInterface<String> duplicatesBloomFilter) throws IOException{
		final List<Duplicate> result = new ArrayList<>();

		if(duplicatesBloomFilter.getAddedElements() > 0){
			LOGGER.info(Backbone.MARKER_APPLICATION, "Extracting duplicates (pass 2/3)");
			setProgress(0);

			final File dicFile = dicParser.getDicFile();
			final Charset charset = getCharset();
			try(final LineNumberReader br = new LineNumberReader(Files.newBufferedReader(dicFile.toPath(), dicParser.getCharset()))){
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
							LOGGER.warn(Backbone.MARKER_APPLICATION, e.getMessage());
						}
					}

					setProgress(getProgress(readSoFar, totalSize));
				}
			}
			setProgress(100);

			final int totalDuplicates = duplicatesBloomFilter.getAddedElements();
			final double falsePositiveProbability = duplicatesBloomFilter.getTrueFalsePositiveProbability();
			LOGGER.info(Backbone.MARKER_APPLICATION, "Total duplicates: {}", DictionaryParser.COUNTER_FORMATTER.format(totalDuplicates));
			LOGGER.info(Backbone.MARKER_APPLICATION, "False positive probability is {} (overall duplicates ≲ {})",
				DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability), (int)Math.ceil(totalDuplicates * falsePositiveProbability));

			duplicatesBloomFilter.clear();

			result.sort((d1, d2) -> comparator.compare(d1.getProduction().getWord(), d2.getProduction().getWord()));
		}
		else
			LOGGER.info(Backbone.MARKER_APPLICATION, "No duplicates found, skip remaining passes");

		return result;
	}

	private void writeDuplicates(final List<Duplicate> duplicates) throws IOException{
		final int totalSize = duplicates.size();
		if(totalSize > 0){
			LOGGER.info(Backbone.MARKER_APPLICATION, "Write results to file (pass 3/3)");
			setProgress(0);

			int writtenSoFar = 0;
			final List<List<Duplicate>> mergedDuplicates = mergeDuplicates(duplicates);
			setProgress(getProgress(1., totalSize + 1));
			try(final BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), dicParser.getCharset())){
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

					writtenSoFar ++;
					setProgress(getProgress(writtenSoFar, totalSize + 1));
				}
			}
			setProgress(100);

			LOGGER.info(Backbone.MARKER_APPLICATION, "File written: {}", outputFile.getAbsolutePath());
		}
	}

	private int getProgress(final double index, final double total){
		return Math.min((int)Math.floor((index * 100.) / total), 100);
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
