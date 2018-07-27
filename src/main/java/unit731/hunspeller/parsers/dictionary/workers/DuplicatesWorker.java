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
import java.util.stream.Collectors;
import javax.swing.SwingWorker;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.collections.bloomfilter.BloomFilterInterface;
import unit731.hunspeller.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunspeller.collections.bloomfilter.core.BitArrayBuilder;
import unit731.hunspeller.languages.builders.ComparatorBuilder;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.dtos.Duplicate;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.TimeWatch;


@AllArgsConstructor
@Slf4j
public class DuplicatesWorker extends SwingWorker<Void, String>{

	private static final int EXPECTED_NUMBER_OF_DUPLICATIONS = 1_000_000;
	private static final double FALSE_POSITIVE_PROBABILITY_DUPLICATIONS = 0.000_000_4;


	private final Backbone backbone;
	private final File outputFile;


	@Override
	protected Void doInBackground() throws Exception{
		boolean stopped = false;
		try{
			publish("Opening Dictionary file for duplications extraction (pass 1/3)");

			TimeWatch watch = TimeWatch.start();

			BloomFilterInterface<String> duplicatesBloomFilter = collectDuplicates();

			List<Duplicate> duplicates = extractDuplicates(duplicatesBloomFilter);

			writeDuplicates(duplicates);

			watch.stop();

			publish("Duplicates extracted successfully (it takes " + watch.toStringMinuteSeconds() + ")");

			if(!duplicates.isEmpty())
				DictionaryParser.openFileWithChoosenEditor(outputFile);
		}
		catch(Exception e){
			stopped = true;

			if(e instanceof ClosedChannelException)
				publish("Duplicates thread interrupted");
			else{
				String message = ExceptionService.getMessage(e);
				publish(e.getClass().getSimpleName() + ": " + message);
			}
		}
		if(stopped)
			publish("Stopped reading Dictionary file");

		return null;
	}

	private BloomFilterInterface<String> collectDuplicates() throws IOException{
		FlagParsingStrategy strategy = backbone.affParser.getFlagParsingStrategy();

		BitArrayBuilder.Type bloomFilterType = BitArrayBuilder.Type.FAST;
		BloomFilterInterface<String> bloomFilter = new ScalableInMemoryBloomFilter<>(bloomFilterType, backbone.dicParser.getExpectedNumberOfElements(), backbone.dicParser.getFalsePositiveProbability(), backbone.dicParser.getGrowRatioWhenFull());
		bloomFilter.setCharset(backbone.getCharset());
		BloomFilterInterface<String> duplicatesBloomFilter = new ScalableInMemoryBloomFilter<>(bloomFilterType, EXPECTED_NUMBER_OF_DUPLICATIONS, FALSE_POSITIVE_PROBABILITY_DUPLICATIONS, backbone.dicParser.getGrowRatioWhenFull());
		duplicatesBloomFilter.setCharset(backbone.getCharset());

		setProgress(0);
		try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(backbone.dicParser.getDicFile().toPath(), backbone.getCharset()))){
			String line = br.readLine();
			//ignore any BOM marker on first line
			if(br.getLineNumber() == 1)
				line = FileService.clearBOMMarker(line);
			if(!NumberUtils.isCreatable(line))
				throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

			int lineIndex = 1;
			long readSoFar = line.length();
			long totalSize = backbone.dicParser.getDicFile().length();
			while((line = br.readLine()) != null){
				lineIndex ++;
				readSoFar += line.length();
				line = DictionaryParser.cleanLine(line);
				if(!line.isEmpty()){
					DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);

					try{
						List<RuleProductionEntry> productions = backbone.dicParser.getWordGenerator().applyRules(dictionaryWord);

						productions.stream()
							.map(RuleProductionEntry::toStringWithSignificantMorphologicalFields)
							.filter(text -> !bloomFilter.add(text))
							.forEachOrdered(duplicatesBloomFilter::add);
					}
					catch(IllegalArgumentException e){
						publish(e.getMessage() + " on line " + lineIndex + ": " + dictionaryWord.toString());
					}
				}

				setProgress((int)((readSoFar * 100.) / totalSize));
			}
		}
		setProgress(100);

		int totalProductions = bloomFilter.getAddedElements();
		double falsePositiveProbability = bloomFilter.getTrueFalsePositiveProbability();
		int falsePositiveCount = (int)Math.ceil(totalProductions * falsePositiveProbability);
		publish("Total productions: " + DictionaryParser.COUNTER_FORMATTER.format(totalProductions));
		publish("False positive probability is " + DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability)
			+ " (overall duplicates ≲ " + falsePositiveCount + ")");

		bloomFilter.close();
		bloomFilter.clear();

		return duplicatesBloomFilter;
	}

	private List<Duplicate> extractDuplicates(BloomFilterInterface<String> duplicatesBloomFilter) throws IOException{
		List<Duplicate> result = new ArrayList<>();

		if(duplicatesBloomFilter.getAddedElements() > 0){
			publish("Extracting duplicates (pass 2/3)");
			setProgress(0);

			FlagParsingStrategy strategy = backbone.affParser.getFlagParsingStrategy();

			setProgress(0);
			try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(backbone.dicParser.getDicFile().toPath(), backbone.getCharset()))){
				String line = br.readLine();
				//ignore any BOM marker on first line
				if(br.getLineNumber() == 1)
					line = FileService.clearBOMMarker(line);
				if(!NumberUtils.isCreatable(line))
					throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

				int lineIndex = 1;
				long readSoFar = line.length();
				long totalSize = backbone.dicParser.getDicFile().length();
				while((line = br.readLine()) != null){
					lineIndex ++;
					readSoFar += line.length();
					line = DictionaryParser.cleanLine(line);
					if(!line.isEmpty()){
						try{
							DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);
							List<RuleProductionEntry> productions = backbone.dicParser.getWordGenerator().applyRules(dictionaryWord);
							for(RuleProductionEntry production : productions){
								String text = production.toStringWithSignificantMorphologicalFields();
								if(duplicatesBloomFilter.contains(text))
									result.add(new Duplicate(production, dictionaryWord, lineIndex));
							}
						}
						catch(IllegalArgumentException e){
							publish(e.getMessage());
						}
					}

					setProgress((int)((readSoFar * 100.) / totalSize));
				}
			}
			setProgress(100);

			int totalDuplicates = duplicatesBloomFilter.getAddedElements();
			double falsePositiveProbability = duplicatesBloomFilter.getTrueFalsePositiveProbability();
			publish("Total duplicates: " + DictionaryParser.COUNTER_FORMATTER.format(totalDuplicates));
			publish("False positive probability is " + DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability)
				+ " (overall duplicates ≲ " + (int)Math.ceil(totalDuplicates * falsePositiveProbability) + ")");

			duplicatesBloomFilter.close();
			duplicatesBloomFilter.clear();

			Comparator<String> comparator = ComparatorBuilder.getComparator(backbone.affParser.getLanguage());
			Collections.sort(result, (d1, d2) -> comparator.compare(d1.getProduction().getWord(), d2.getProduction().getWord()));
		}
		else
			publish("No duplicates found, skip remaining passes");

		return result;
	}

	private void writeDuplicates(List<Duplicate> duplicates) throws IOException{
		int totalSize = duplicates.size();
		if(totalSize > 0){
			publish("Write results to file (pass 3/3)");
			setProgress(0);

			int writtenSoFar = 0;
			List<List<Duplicate>> mergedDuplicates = mergeDuplicates(duplicates);
			setProgress((int)(100. / (totalSize + 1)));
			try(BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), backbone.getCharset())){
				for(List<Duplicate> entries : mergedDuplicates){
					writer.write(entries.get(0).getProduction().getWord());
					writer.write(": ");
					writer.write(entries.stream()
						.map(duplicate -> 
							String.join(StringUtils.EMPTY, duplicate.getDictionaryWord().getWord(), " (", Integer.toString(duplicate.getLineIndex()),
								(duplicate.getProduction().hasProductionRules()? " via " + duplicate.getProduction().getRulesSequence(): StringUtils.EMPTY), ")")
						)
						.collect(Collectors.joining(", ")));
					writer.newLine();

					writtenSoFar ++;
					setProgress((int)((writtenSoFar * 100.) / (totalSize + 1)));
				}
			}
			setProgress(100);

			publish("File written: " + outputFile.getAbsolutePath());
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

		Comparator<String> comparator = ComparatorBuilder.getComparator(backbone.affParser.getLanguage());
		List<List<Duplicate>> result = new ArrayList<>(dupls.values());
		result.sort(Comparator.<List<Duplicate>>comparingInt(List::size).reversed()
			.thenComparing(Comparator.comparing(list -> list.get(0).getProduction().getWord(), comparator)));
		return result;
	}

	@Override
	protected void process(List<String> chunks){
		for(String chunk : chunks)
			log.info(Backbone.MARKER_APPLICATION, chunk);
	}

}
