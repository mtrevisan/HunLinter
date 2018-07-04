package unit731.hunspeller.parsers.dictionary.workers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.SwingWorker;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.languages.builders.ComparatorBuilder;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import static unit731.hunspeller.parsers.dictionary.DictionaryParser.COUNTER_FORMATTER;
import static unit731.hunspeller.parsers.dictionary.DictionaryParser.openFileWithChoosenEditor;
import unit731.hunspeller.parsers.dictionary.RuleProductionEntry;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.HammingDistance;
import unit731.hunspeller.services.TimeWatch;
import unit731.hunspeller.services.externalsorter.ExternalSorterOptions;


@AllArgsConstructor
public class MinimalPairsWorker extends SwingWorker<Void, String>{

	private static final String SLASH = "/";

	private final AffixParser affParser;
	private final DictionaryParser dicParser;
	private final File outputFile;
	private final Resultable resultable;


	@Override
	protected Void doInBackground() throws Exception{
		boolean stopped = false;
		try{
			publish("Opening Dictionary file for minimal pairs extraction: " + affParser.getLanguage() + ".dic (pass 1/3)");

			TimeWatch watch = TimeWatch.start();

			FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();

			setProgress(0);
			try(
					BufferedReader br = Files.newBufferedReader(dicParser.getDicFile().toPath(), dicParser.getCharset());
					BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), dicParser.getCharset());
					){
				String line = br.readLine();
				if(!NumberUtils.isCreatable(line))
					throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

				int lineIndex = 1;
				long readSoFar = line.length();
				long totalSize = dicParser.getDicFile().length();
				while(Objects.nonNull(line = br.readLine())){
					lineIndex ++;
					readSoFar += line.length();

					line = dicParser.cleanLine(line);
					if(!line.isEmpty()){
						DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);
						try{
							List<RuleProductionEntry> productions = dicParser.getWordGenerator().applyRules(dictionaryWord);

							for(RuleProductionEntry production : productions)
								if(dicParser.shouldBeProcessedForMinimalPair(production)){
									String word = production.getWord();
									writer.write(word);
									writer.newLine();
								}
						}
						catch(IllegalArgumentException e){
							publish(e.getMessage() + " on line " + lineIndex + ": " + dictionaryWord.toString());
						}
					}

					setProgress((int)Math.ceil((readSoFar * 100.) / totalSize));
				}
			}
			publish("Support file written");

			//sort file by length first and by alphabet after:
			ExternalSorterOptions options = ExternalSorterOptions.builder()
				.charset(dicParser.getCharset())
				.comparator(ComparatorBuilder.COMPARATOR_LENGTH.thenComparing(ComparatorBuilder.getComparator(dicParser.getLanguage())))
				.useZip(true)
				.removeDuplicates(true)
				.build();
			dicParser.getSorter().sort(outputFile, options, outputFile);

			setProgress(100);

			publish("Support file sorted");


			publish("Extracting minimal pairs (pass 2/3)");
			setProgress(0);

			int totalPairs = 0;
			Map<String, List<String>> minimalPairs = new HashMap<>();
			try(BufferedReader sourceBR = Files.newBufferedReader(outputFile.toPath(), dicParser.getCharset())){
				String sourceLine;
				long readSoFarSource = 0;
				long totalSizeSource = outputFile.length();
				while(Objects.nonNull(sourceLine = sourceBR.readLine())){
					readSoFarSource += sourceLine.length();

					sourceBR.mark((int)(totalSizeSource - readSoFarSource));

					try{
						String sourceLineLowercase = sourceLine.toLowerCase(Locale.ROOT);

						String line2;
						while(Objects.nonNull(line2 = sourceBR.readLine())){
							String line2Lowercase = line2.toLowerCase(Locale.ROOT);

							//calculate distance
							int distance = HammingDistance.getDistance(sourceLineLowercase, line2Lowercase);
							if(distance == 1){
								Pair<Character, Character> difference = HammingDistance.findFirstDifference(sourceLineLowercase, line2Lowercase);
								char left = difference.getLeft();
								char right = difference.getRight();
								if(dicParser.isConsonant(left) && dicParser.isConsonant(right)){
									String key = left + SLASH + right;
									String value = sourceLine + SLASH + line2;
									minimalPairs.computeIfAbsent(key, k -> new ArrayList<>())
										.add(value);

									totalPairs ++;
								}
							}
						}
					}
					catch(IllegalArgumentException e){
						//length varied, consider another line for minimal pair search
					}

					sourceBR.reset();

					setProgress((int)((readSoFarSource * 100.) / totalSizeSource));
				}
			}
			setProgress(100);

			publish("Total minimal pairs: " + COUNTER_FORMATTER.format(totalPairs));


			publish("Reordering minimal pairs (pass 3/3)");
			setProgress(0);

			//write result
			try(BufferedWriter destinationWriter = Files.newBufferedWriter(outputFile.toPath(), dicParser.getCharset())){
				int index = 0;
				int size = minimalPairs.size();
				for(Map.Entry<String, List<String>> entry : minimalPairs.entrySet()){
					String key = entry.getKey();
					List<String> values = entry.getValue();

					destinationWriter.write(key + ": " + values.stream().collect(Collectors.joining(", ")));
					destinationWriter.newLine();

					setProgress((int)((index * 100.) / size));
				}
			}

			setProgress(100);

			publish("Minimal pairs file written");

			//sort file alphabetically:
			options = ExternalSorterOptions.builder()
				.charset(dicParser.getCharset())
				.comparator(ComparatorBuilder.getComparator(dicParser.getLanguage()))
				.useZip(true)
				.removeDuplicates(true)
				.build();
			dicParser.getSorter().sort(outputFile, options, outputFile);

			watch.stop();

			setProgress(100);

			publish("File written: " + outputFile.getAbsolutePath());
			publish("Minimal pairs extracted successfully (it takes " + watch.toStringMinuteSeconds() + ")");

			openFileWithChoosenEditor(outputFile);
		}
		catch(IOException | IllegalArgumentException e){
			stopped = true;

			publish(e instanceof ClosedChannelException? "Minimal pairs thread interrupted": e.getClass().getSimpleName() + ": " + e.getMessage());
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

	@Override
	protected void process(List<String> chunks){
		resultable.printResultLine(chunks);
	}

}
