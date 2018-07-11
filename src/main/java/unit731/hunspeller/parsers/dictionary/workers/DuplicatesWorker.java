package unit731.hunspeller.parsers.dictionary.workers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.SwingWorker;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.collections.bloomfilter.BloomFilterInterface;
import unit731.hunspeller.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunspeller.collections.bloomfilter.core.BitArrayBuilder;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.languages.builders.ComparatorBuilder;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.dtos.Duplicate;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.TimeWatch;


@AllArgsConstructor
public class DuplicatesWorker extends SwingWorker<Void, String>{

	private final AffixParser affParser;
	private final DictionaryParser dicParser;
	private final File outputFile;
	private final Resultable resultable;


	@Override
	protected Void doInBackground() throws Exception{
		boolean stopped = false;
		try{
			publish("Opening Dictionary file for duplications extraction: " + affParser.getLanguage() + ".dic (pass 1/3)");

			TimeWatch watch = TimeWatch.start();

			BloomFilterInterface<String> duplicatesBloomFilter = collectDuplicates();

			List<Duplicate> duplicates = extractDuplicates(duplicatesBloomFilter);

			writeDuplicates(duplicates);

			watch.stop();

			publish("Duplicates extracted successfully (it takes " + watch.toStringMinuteSeconds() + ")");

			if(!duplicates.isEmpty())
				DictionaryParser.openFileWithChoosenEditor(outputFile);
		}
		catch(IOException | IllegalArgumentException e){
			stopped = true;

			publish(e instanceof ClosedChannelException? "Duplicates thread interrupted": e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		catch(Exception e){
			stopped = true;

			String message = ExceptionService.getMessage(e, getClass());
			publish(e.getClass().getSimpleName() + ": " + message);
		}
		if(stopped)
			publish("Stopped reading Dictionary file");

		return null;
	}

	private BloomFilterInterface<String> collectDuplicates() throws IOException{
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();

		BitArrayBuilder.Type bloomFilterType = BitArrayBuilder.Type.FAST;
		BloomFilterInterface<String> bloomFilter = new ScalableInMemoryBloomFilter<>(bloomFilterType, 40_000_000, 0.000_000_01, 1.3);
		bloomFilter.setCharset(dicParser.getCharset());
		BloomFilterInterface<String> duplicatesBloomFilter = new ScalableInMemoryBloomFilter<>(bloomFilterType, 1_000_000, 0.000_000_4, 2.);
		duplicatesBloomFilter.setCharset(dicParser.getCharset());

		setProgress(0);
		try(BufferedReader br = Files.newBufferedReader(dicParser.getDicFile().toPath(), dicParser.getCharset())){
			String line = br.readLine();
			if(!NumberUtils.isCreatable(line))
				throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

			int lineIndex = 1;
			long readSoFar = line.length();
			long totalSize = dicParser.getDicFile().length();
			while(Objects.nonNull(line = br.readLine())){
				lineIndex ++;
				readSoFar += line.length();
				line = DictionaryParser.cleanLine(line);
				if(!line.isEmpty()){
					DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);

					try{
						List<RuleProductionEntry> productions = dicParser.getWordGenerator().applyRules(dictionaryWord);

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
		publish("Total productions: " + DictionaryParser.COUNTER_FORMATTER.format(totalProductions));
		publish("False positive probability is " + DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability * 100.)
			+ " (overall duplicates ≲ " + (int)Math.ceil(totalProductions * falsePositiveProbability) + ")");

		bloomFilter.close();
		bloomFilter.clear();

		return duplicatesBloomFilter;
	}

	private List<Duplicate> extractDuplicates(BloomFilterInterface<String> duplicatesBloomFilter) throws IOException{
		List<Duplicate> result = new ArrayList<>();

		if(duplicatesBloomFilter.getAddedElements() > 0){
			publish("Extracting duplicates (pass 2/3)");
			setProgress(0);

			FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();

			setProgress(0);
			try(BufferedReader br = Files.newBufferedReader(dicParser.getDicFile().toPath(), dicParser.getCharset())){
				String line = br.readLine();

				int lineIndex = 1;
				long readSoFar = line.length();
				long totalSize = dicParser.getDicFile().length();
				while(Objects.nonNull(line = br.readLine())){
					lineIndex ++;
					readSoFar += line.length();
					line = DictionaryParser.cleanLine(line);
					if(!line.isEmpty()){
						try{
							DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);
							List<RuleProductionEntry> productions = dicParser.getWordGenerator().applyRules(dictionaryWord);
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
			publish("False positive probability is " + DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability * 100.)
				+ " (overall duplicates ≲ " + (int)Math.ceil(totalDuplicates * falsePositiveProbability) + ")");

			duplicatesBloomFilter.close();
			duplicatesBloomFilter.clear();

			Comparator<String> comparator = ComparatorBuilder.getComparator(affParser.getLanguage());
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
			try(BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), dicParser.getCharset())){
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

		Comparator<String> comparator = ComparatorBuilder.getComparator(affParser.getLanguage());
		List<List<Duplicate>> result = new ArrayList<>(dupls.values());
		result.sort(Comparator.<List<Duplicate>>comparingInt(List::size).reversed()
			.thenComparing(Comparator.comparing(list -> list.get(0).getProduction().getWord(), comparator)));
		return result;
	}

	@Override
	protected void process(List<String> chunks){
		resultable.printResultLine(chunks);
	}

}
