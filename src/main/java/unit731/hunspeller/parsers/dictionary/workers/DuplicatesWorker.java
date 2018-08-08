package unit731.hunspeller.parsers.dictionary.workers;

import java.io.BufferedWriter;
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
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.collections.bloomfilter.BloomFilterInterface;
import unit731.hunspeller.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunspeller.collections.bloomfilter.core.BitArrayBuilder;
import unit731.hunspeller.languages.CorrectnessChecker;
import unit731.hunspeller.languages.builders.ComparatorBuilder;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.dtos.Duplicate;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerBase;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.TimeWatch;


@Slf4j
public class DuplicatesWorker extends WorkerBase<Void, Void>{

	public static final String WORKER_NAME = "Duplications extraction";

	private static final int EXPECTED_NUMBER_OF_DUPLICATIONS = 1_000_000;
	private static final double FALSE_POSITIVE_PROBABILITY_DUPLICATIONS = 0.000_000_4;


	private final DictionaryParser dicParser;
	private final WordGenerator wordGenerator;
	private final CorrectnessChecker checker;
	private final File outputFile;
	private final Comparator<String> comparator;


	public DuplicatesWorker(String language, DictionaryParser dicParser, WordGenerator wordGenerator, CorrectnessChecker checker,
			File outputFile){
		Objects.requireNonNull(language);
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(checker);
		Objects.requireNonNull(outputFile);

		this.dicParser = dicParser;
		this.wordGenerator = wordGenerator;
		this.checker = checker;
		this.outputFile = outputFile;
		workerName = WORKER_NAME;

		comparator = ComparatorBuilder.getComparator(language);
	}

	@Override
	protected Void doInBackground() throws Exception{
		boolean stopped = false;
		try{
			log.info(Backbone.MARKER_APPLICATION, "Opening Dictionary file for duplications extraction (pass 1/3)");

			watch = TimeWatch.start();

			BloomFilterInterface<String> duplicatesBloomFilter = collectDuplicates();

			List<Duplicate> duplicates = extractDuplicates(duplicatesBloomFilter);

			writeDuplicates(duplicates);

			watch.stop();

			log.info(Backbone.MARKER_APPLICATION, "Duplicates extracted successfully (it takes " + watch.toStringMinuteSeconds() + ")");

			if(!duplicates.isEmpty()){
				try{
					FileService.openFileWithChoosenEditor(outputFile);
				}
				catch(IOException | InterruptedException e){
					log.warn("Exception while opening the resulting file", e);
				}
			}
		}
		catch(Exception e){
			stopped = true;

			if(e instanceof ClosedChannelException)
				log.warn(Backbone.MARKER_APPLICATION, "Duplicates thread interrupted");
			else{
				String message = ExceptionService.getMessage(e);
				log.error(Backbone.MARKER_APPLICATION, e.getClass().getSimpleName() + ": " + message);
			}
		}
		if(stopped)
			log.info(Backbone.MARKER_APPLICATION, "Stopped reading Dictionary file");

		return null;
	}

	private BloomFilterInterface<String> collectDuplicates() throws IOException{
		BitArrayBuilder.Type bloomFilterType = BitArrayBuilder.Type.FAST;
		BloomFilterInterface<String> bloomFilter = new ScalableInMemoryBloomFilter<>(bloomFilterType,
			checker.getExpectedNumberOfElements(), checker.getFalsePositiveProbability(), checker.getGrowRatioWhenFull());
		bloomFilter.setCharset(dicParser.getCharset());
		BloomFilterInterface<String> duplicatesBloomFilter = new ScalableInMemoryBloomFilter<>(bloomFilterType,
			EXPECTED_NUMBER_OF_DUPLICATIONS, FALSE_POSITIVE_PROBABILITY_DUPLICATIONS, checker.getGrowRatioWhenFull());
		duplicatesBloomFilter.setCharset(dicParser.getCharset());

		setProgress(0);
		File dicFile = dicParser.getDicFile();
		try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(dicFile.toPath(), dicParser.getCharset()))){
			String line = br.readLine();
			//ignore any BOM marker on first line
			if(br.getLineNumber() == 1)
				line = FileService.clearBOMMarker(line);
			if(!NumberUtils.isCreatable(line))
				throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

			int lineIndex = 1;
			long readSoFar = line.length();
			long totalSize = dicFile.length();
			while((line = br.readLine()) != null){
				lineIndex ++;
				readSoFar += line.length();
				line = DictionaryParser.cleanLine(line);
				if(!line.isEmpty()){
					try{
						List<Production> productions = wordGenerator.applyRules(line);

						productions.stream()
							.map(Production::toStringWithSignificantMorphologicalFields)
							.filter(text -> !bloomFilter.add(text))
							.forEachOrdered(duplicatesBloomFilter::add);
					}
					catch(IllegalArgumentException e){
						log.error(Backbone.MARKER_APPLICATION, e.getMessage() + " on line " + lineIndex + ": " + line);
					}
				}

				setProgress((int)((readSoFar * 100.) / totalSize));
			}
		}
		setProgress(100);

		int totalProductions = bloomFilter.getAddedElements();
		double falsePositiveProbability = bloomFilter.getTrueFalsePositiveProbability();
		int falsePositiveCount = (int)Math.ceil(totalProductions * falsePositiveProbability);
		log.info(Backbone.MARKER_APPLICATION, "Total productions: " + DictionaryParser.COUNTER_FORMATTER.format(totalProductions));
		log.info(Backbone.MARKER_APPLICATION, "False positive probability is " + DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability)
			+ " (overall duplicates ≲ " + falsePositiveCount + ")");

		bloomFilter.close();
		bloomFilter.clear();

		return duplicatesBloomFilter;
	}

	private List<Duplicate> extractDuplicates(BloomFilterInterface<String> duplicatesBloomFilter) throws IOException{
		List<Duplicate> result = new ArrayList<>();

		if(duplicatesBloomFilter.getAddedElements() > 0){
			log.info(Backbone.MARKER_APPLICATION, "Extracting duplicates (pass 2/3)");
			setProgress(0);

			File dicFile = dicParser.getDicFile();
			try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(dicFile.toPath(), dicParser.getCharset()))){
				String line = br.readLine();
				//ignore any BOM marker on first line
				if(br.getLineNumber() == 1)
					line = FileService.clearBOMMarker(line);
				if(!NumberUtils.isCreatable(line))
					throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

				int lineIndex = 1;
				long readSoFar = line.length();
				long totalSize = dicFile.length();
				while((line = br.readLine()) != null){
					lineIndex ++;
					readSoFar += line.length();
					line = DictionaryParser.cleanLine(line);
					if(!line.isEmpty()){
						try{
							List<Production> productions = wordGenerator.applyRules(line);
							String word = productions.get(0).getWord();
							for(Production production : productions){
								String text = production.toStringWithSignificantMorphologicalFields();
								if(duplicatesBloomFilter.contains(text))
									result.add(new Duplicate(production, word, lineIndex));
							}
						}
						catch(IllegalArgumentException e){
							log.warn(Backbone.MARKER_APPLICATION, e.getMessage());
						}
					}

					setProgress((int)((readSoFar * 100.) / totalSize));
				}
			}
			setProgress(100);

			int totalDuplicates = duplicatesBloomFilter.getAddedElements();
			double falsePositiveProbability = duplicatesBloomFilter.getTrueFalsePositiveProbability();
			log.info(Backbone.MARKER_APPLICATION, "Total duplicates: " + DictionaryParser.COUNTER_FORMATTER.format(totalDuplicates));
			log.info(Backbone.MARKER_APPLICATION, "False positive probability is "
				+ DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability) + " (overall duplicates ≲ "
				+ (int)Math.ceil(totalDuplicates * falsePositiveProbability) + ")");

			duplicatesBloomFilter.close();
			duplicatesBloomFilter.clear();

			Collections.sort(result, (d1, d2) -> comparator.compare(d1.getProduction().getWord(), d2.getProduction().getWord()));
		}
		else
			log.info(Backbone.MARKER_APPLICATION, "No duplicates found, skip remaining passes");

		return result;
	}

	private void writeDuplicates(List<Duplicate> duplicates) throws IOException{
		int totalSize = duplicates.size();
		if(totalSize > 0){
			log.info(Backbone.MARKER_APPLICATION, "Write results to file (pass 3/3)");
			setProgress(0);

			int writtenSoFar = 0;
			List<List<Duplicate>> mergedDuplicates = mergeDuplicates(duplicates);
			setProgress((int)(100. / (totalSize + 1)));
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
					setProgress((int)((writtenSoFar * 100.) / (totalSize + 1)));
				}
			}
			setProgress(100);

			log.info(Backbone.MARKER_APPLICATION, "File written: " + outputFile.getAbsolutePath());
		}
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
