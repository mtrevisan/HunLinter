package unit731.hunspeller.parsers.dictionary.workers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.LineNumberReader;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.SwingWorker;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.languages.builders.ComparatorBuilder;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.HammingDistance;
import unit731.hunspeller.services.TimeWatch;
import unit731.hunspeller.services.externalsorter.ExternalSorterOptions;


@AllArgsConstructor
@Slf4j
public class MinimalPairsWorker extends SwingWorker<Void, String>{

	private static final String SLASH = "/";

	private final Backbone backbone;
	private final File outputFile;


	@Override
	protected Void doInBackground() throws Exception{
		boolean stopped = false;
		try{
			publish("Opening Dictionary file for minimal pairs extraction (pass 1/3)");

			TimeWatch watch = TimeWatch.start();

			setProgress(0);
			File dicFile = backbone.dicParser.getDicFile();
			try(
					LineNumberReader br = new LineNumberReader(Files.newBufferedReader(dicFile.toPath(), backbone.getCharset()));
					BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), backbone.getCharset());
					){
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
							List<RuleProductionEntry> productions = backbone.applyRules(line);

							for(RuleProductionEntry production : productions)
								if(backbone.dicParser.shouldBeProcessedForMinimalPair(production)){
									String word = production.getWord();
									writer.write(word);
									writer.newLine();
								}
						}
						catch(IllegalArgumentException e){
							publish(e.getMessage() + " on line " + lineIndex + ": " + line);
						}
					}

					setProgress((int)Math.ceil((readSoFar * 100.) / totalSize));
				}
			}
			publish("Support file written");

			//sort file by length first and by alphabet after:
			ExternalSorterOptions options = ExternalSorterOptions.builder()
				.charset(backbone.getCharset())
				.comparator(ComparatorBuilder.COMPARATOR_LENGTH.thenComparing(ComparatorBuilder.getComparator(backbone.getLanguage())))
				.useZip(true)
				.removeDuplicates(true)
				.build();
			backbone.getDictionarySorter().sort(outputFile, options, outputFile);

			setProgress(100);

			publish("Support file sorted");


			publish("Extracting minimal pairs (pass 2/3)");
			setProgress(0);

			int totalPairs = 0;
			Map<String, List<String>> minimalPairs = new HashMap<>();
			try(BufferedReader sourceBR = Files.newBufferedReader(outputFile.toPath(), backbone.getCharset())){
				String sourceLine;
				long readSoFarSource = 0;
				long totalSizeSource = outputFile.length();
				while((sourceLine = sourceBR.readLine()) != null){
					readSoFarSource += sourceLine.length();

					sourceBR.mark((int)(totalSizeSource - readSoFarSource));

					try{
						String sourceLineLowercase = sourceLine.toLowerCase(Locale.ROOT);

						String line2;
						while((line2 = sourceBR.readLine()) != null){
							String line2Lowercase = line2.toLowerCase(Locale.ROOT);

							//calculate distance
							int distance = HammingDistance.getDistance(sourceLineLowercase, line2Lowercase);
							if(distance == 1){
								Pair<Character, Character> difference = HammingDistance.findFirstDifference(sourceLineLowercase, line2Lowercase);
								char left = difference.getLeft();
								char right = difference.getRight();
								if(backbone.dicParser.isConsonant(left) && backbone.dicParser.isConsonant(right)){
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

			publish("Total minimal pairs: " + DictionaryParser.COUNTER_FORMATTER.format(totalPairs));


			publish("Reordering minimal pairs (pass 3/3)");
			setProgress(0);

			//write result
			try(BufferedWriter destinationWriter = Files.newBufferedWriter(outputFile.toPath(), backbone.getCharset())){
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
				.charset(backbone.getCharset())
				.comparator(ComparatorBuilder.getComparator(backbone.getLanguage()))
				.useZip(true)
				.removeDuplicates(true)
				.build();
			backbone.getDictionarySorter().sort(outputFile, options, outputFile);

			watch.stop();

			setProgress(100);

			publish("File written: " + outputFile.getAbsolutePath());
			publish("Minimal pairs extracted successfully (it takes " + watch.toStringMinuteSeconds() + ")");

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

	@Override
	protected void process(List<String> chunks){
		for(String chunk : chunks)
			log.info(Backbone.MARKER_APPLICATION, chunk);
	}

}
