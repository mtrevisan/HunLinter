package unit731.hunspeller.parsers.dictionary.workers;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
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
import unit731.hunspeller.Backbone;
import unit731.hunspeller.collections.bloomfilter.BloomFilterInterface;
import unit731.hunspeller.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunspeller.languages.DictionaryBaseData;
import unit731.hunspeller.languages.BaseBuilder;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGeneratorAffixRules;
import unit731.hunspeller.parsers.dictionary.dtos.Duplicate;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerBase;
import unit731.hunspeller.services.ExceptionHelper;
import unit731.hunspeller.services.FileHelper;
import unit731.hunspeller.services.TimeWatch;


public class DuplicatesWorker extends WorkerBase<Void, Void>{

	private static final Logger LOGGER = LoggerFactory.getLogger(DuplicatesWorker.class);

	public static final String WORKER_NAME = "Duplications extraction";

	private static final int EXPECTED_NUMBER_OF_DUPLICATIONS = 1_000_000;
	private static final double FALSE_POSITIVE_PROBABILITY_DUPLICATIONS = 0.000_000_4;


	private final DictionaryParser dicParser;
	private final WordGeneratorAffixRules wordGenerator;
	private final DictionaryBaseData dictionaryBaseData;
	private final File outputFile;
	private final Comparator<String> comparator;


	public DuplicatesWorker(String language, DictionaryParser dicParser, WordGeneratorAffixRules wordGenerator, DictionaryBaseData dictionaryBaseData,
			File outputFile){
		Objects.requireNonNull(language);
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(dictionaryBaseData);
		Objects.requireNonNull(outputFile);

		this.dicParser = dicParser;
		this.wordGenerator = wordGenerator;
		this.dictionaryBaseData = dictionaryBaseData;
		this.outputFile = outputFile;

		workerName = WORKER_NAME;
		charset = dicParser.getCharset();
		comparator = BaseBuilder.getComparator(language);
	}

	@Override
	protected Void doInBackground() throws Exception{
		try{
			LOGGER.info(Backbone.MARKER_APPLICATION, "Opening Dictionary file for duplications extraction (pass 1/3)");

			watch = TimeWatch.start();

			BloomFilterInterface<String> duplicatesBloomFilter = collectDuplicates();

			List<Duplicate> duplicates = extractDuplicates(duplicatesBloomFilter);

			writeDuplicates(duplicates);

			watch.stop();

			LOGGER.info(Backbone.MARKER_APPLICATION, "Duplicates extracted successfully (it takes {})", watch.toStringMinuteSeconds());

			if(!duplicates.isEmpty()){
				try{
					FileHelper.openFileWithChoosenEditor(outputFile);
				}
				catch(IOException | InterruptedException e){
					LOGGER.warn("Exception while opening the resulting file", e);
				}
			}
		}
		catch(Throwable t){
			if(t instanceof ClosedChannelException)
				LOGGER.warn(Backbone.MARKER_APPLICATION, "Duplicates thread interrupted");
			else{
				String message = ExceptionHelper.getMessage(t);
				LOGGER.error(Backbone.MARKER_APPLICATION, "{}: {}", t.getClass().getSimpleName(), message);
			}

			LOGGER.info(Backbone.MARKER_APPLICATION, "Stopped reading Dictionary file");

			cancel(true);
		}

		return null;
	}

	private BloomFilterInterface<String> collectDuplicates() throws IOException{
		BloomFilterInterface<String> bloomFilter = new ScalableInMemoryBloomFilter<>(dicParser.getCharset(),
			dictionaryBaseData.getExpectedNumberOfElements(), dictionaryBaseData.getFalsePositiveProbability(), dictionaryBaseData.getGrowRatioWhenFull());
		BloomFilterInterface<String> duplicatesBloomFilter = new ScalableInMemoryBloomFilter<>(dicParser.getCharset(),
			EXPECTED_NUMBER_OF_DUPLICATIONS, FALSE_POSITIVE_PROBABILITY_DUPLICATIONS, dictionaryBaseData.getGrowRatioWhenFull());

		setProgress(0);
		File dicFile = dicParser.getDicFile();
		try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(dicFile.toPath(), dicParser.getCharset()))){
			String line = extractLine(br);

			long readSoFar = line.getBytes(charset).length + 2;

			//ignore any BOM marker on first line
			if(br.getLineNumber() == 1)
				line = FileHelper.clearBOMMarker(line);
			if(!NumberUtils.isCreatable(line))
				throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

			int lineIndex = 1;
			long totalSize = dicFile.length();
			while((line = br.readLine()) != null){
				lineIndex ++;
				readSoFar += line.getBytes(charset).length + 2;
				line = DictionaryParser.cleanLine(line);
				if(!line.isEmpty()){
					try{
						List<Production> productions = wordGenerator.applyAffixRules(line);

						productions.stream()
							.map(Production::toStringWithPartOfSpeechFields)
							.filter(Predicate.not(bloomFilter::add))
							.forEach(duplicatesBloomFilter::add);
					}
					catch(IllegalArgumentException e){
						LOGGER.error(Backbone.MARKER_APPLICATION, "{} on line {}: {}", e.getMessage(), lineIndex, line);
					}
				}

				setProgress(getProgress(readSoFar, totalSize));
			}

			bloomFilter.close();
			duplicatesBloomFilter.close();
		}
		setProgress(100);

		int totalProductions = bloomFilter.getAddedElements();
		double falsePositiveProbability = bloomFilter.getTrueFalsePositiveProbability();
		int falsePositiveCount = (int)Math.ceil(totalProductions * falsePositiveProbability);
		LOGGER.info(Backbone.MARKER_APPLICATION, "Total productions: {}", DictionaryParser.COUNTER_FORMATTER.format(totalProductions));
		LOGGER.info(Backbone.MARKER_APPLICATION, "False positive probability is {} (overall duplicates ≲ {})",
			DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability), falsePositiveCount);

		bloomFilter.clear();

		return duplicatesBloomFilter;
	}

	private String extractLine(final LineNumberReader br) throws EOFException, IOException{
		String line = br.readLine();
		if(line == null)
			throw new EOFException("Unexpected EOF while reading Dictionary file");

		return DictionaryParser.cleanLine(line);
	}

	private List<Duplicate> extractDuplicates(BloomFilterInterface<String> duplicatesBloomFilter) throws IOException{
		List<Duplicate> result = new ArrayList<>();

		if(duplicatesBloomFilter.getAddedElements() > 0){
			LOGGER.info(Backbone.MARKER_APPLICATION, "Extracting duplicates (pass 2/3)");
			setProgress(0);

			File dicFile = dicParser.getDicFile();
			try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(dicFile.toPath(), dicParser.getCharset()))){
				String line = br.readLine();

				long readSoFar = line.getBytes(charset).length + 2;

				int lineIndex = 1;
				long totalSize = dicFile.length();
				while((line = br.readLine()) != null){
					lineIndex ++;
					readSoFar += line.getBytes(charset).length + 2;
					line = DictionaryParser.cleanLine(line);
					if(!line.isEmpty()){
						try{
							List<Production> productions = wordGenerator.applyAffixRules(line);
							String word = productions.get(0).getWord();
							for(Production production : productions){
								String text = production.toStringWithPartOfSpeechFields();
								if(duplicatesBloomFilter.contains(text))
									result.add(new Duplicate(production, word, lineIndex));
							}
						}
						catch(IllegalArgumentException e){
							LOGGER.warn(Backbone.MARKER_APPLICATION, e.getMessage());
						}
					}

					setProgress(getProgress(readSoFar, totalSize));
				}
			}
			setProgress(100);

			int totalDuplicates = duplicatesBloomFilter.getAddedElements();
			double falsePositiveProbability = duplicatesBloomFilter.getTrueFalsePositiveProbability();
			LOGGER.info(Backbone.MARKER_APPLICATION, "Total duplicates: {}", DictionaryParser.COUNTER_FORMATTER.format(totalDuplicates));
			LOGGER.info(Backbone.MARKER_APPLICATION, "False positive probability is {} (overall duplicates ≲ {})",
				DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability), (int)Math.ceil(totalDuplicates * falsePositiveProbability));

			duplicatesBloomFilter.clear();

			Collections.sort(result, (d1, d2) -> comparator.compare(d1.getProduction().getWord(), d2.getProduction().getWord()));
		}
		else
			LOGGER.info(Backbone.MARKER_APPLICATION, "No duplicates found, skip remaining passes");

		return result;
	}

	private void writeDuplicates(List<Duplicate> duplicates) throws IOException{
		int totalSize = duplicates.size();
		if(totalSize > 0){
			LOGGER.info(Backbone.MARKER_APPLICATION, "Write results to file (pass 3/3)");
			setProgress(0);

			int writtenSoFar = 0;
			List<List<Duplicate>> mergedDuplicates = mergeDuplicates(duplicates);
			setProgress(getProgress(1., totalSize + 1));
			try(BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), dicParser.getCharset())){
				for(List<Duplicate> entries : mergedDuplicates){
					writer.write(entries.get(0).getProduction().getWord());
					writer.write(": ");
					writer.write(entries.stream()
						.map(duplicate -> 
							String.join(StringUtils.EMPTY, duplicate.getWord(), " (", Integer.toString(duplicate.getLineIndex()),
								(duplicate.getProduction().hasProductionRules()? " via " + duplicate.getProduction().getRulesSequence(): StringUtils.EMPTY), ")")
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

	private int getProgress(double index, double total){
		return Math.min((int)Math.floor((index * 100.) / total), 100);
	}

	private List<List<Duplicate>> mergeDuplicates(List<Duplicate> duplicates){
		Map<String, List<Duplicate>> dupls = duplicates.stream()
			.collect(Collectors.toMap(duplicate -> duplicate.getProduction().getWord(),
				duplicate -> {
					List<Duplicate> list = new ArrayList<>();
					list.add(duplicate);
					return list;
				},
				(oldValue, newValue) -> {
					oldValue.addAll(newValue);
					return oldValue;
				}));

		List<List<Duplicate>> result = new ArrayList<>(dupls.values());
		result.sort(Comparator.<List<Duplicate>>comparingInt(List::size).reversed()
			.thenComparing(Comparator.comparing(list -> list.get(0).getProduction().getWord(), comparator)));
		return result;
	}

}
