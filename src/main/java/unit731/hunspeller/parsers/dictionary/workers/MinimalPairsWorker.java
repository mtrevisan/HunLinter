package unit731.hunspeller.parsers.dictionary.workers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.languages.CorrectnessChecker;
import unit731.hunspeller.languages.builders.ComparatorBuilder;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerBase;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.HammingDistance;
import unit731.hunspeller.services.TimeWatch;
import unit731.hunspeller.services.externalsorter.ExternalSorterOptions;


@Slf4j
public class MinimalPairsWorker extends WorkerBase<Void, Void>{

	private static final String SLASH = "/";

	private final CorrectnessChecker checker;
	private final WordGenerator wordGenerator;
	private final DictionaryParser dicParser;
	private final File outputFile;


	public MinimalPairsWorker(DictionaryParser dicParser, CorrectnessChecker checker, WordGenerator wordGenerator, File outputFile){
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(checker);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(outputFile);

		this.dicParser = dicParser;
		this.checker = checker;
		this.wordGenerator = wordGenerator;
		this.outputFile = outputFile;
	}

	@Override
	protected Void doInBackground() throws Exception{
		boolean stopped = false;
		try{
			log.info(Backbone.MARKER_APPLICATION, "Opening Dictionary file for minimal pairs extraction (pass 1/3)");

			watch = TimeWatch.start();

			setProgress(0);
			File dicFile = dicParser.getDicFile();
			try(
					LineNumberReader br = new LineNumberReader(Files.newBufferedReader(dicFile.toPath(), dicParser.getCharset()));
					BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), dicParser.getCharset());
					){
				String line = br.readLine();
				if(line == null)
					throw new IllegalArgumentException("Dictionary file empty");

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

							for(Production production : productions)
								if(checker.shouldBeProcessedForMinimalPair(production)){
									String word = production.getWord();
									writer.write(word);
									writer.newLine();
								}
						}
						catch(IllegalArgumentException e){
							log.info(Backbone.MARKER_APPLICATION, e.getMessage() + " on line " + lineIndex + ": " + line);
						}
					}

					setProgress((int)Math.ceil((readSoFar * 100.) / totalSize));
				}
			}
			log.info(Backbone.MARKER_APPLICATION, "Support file written");

			//sort file by length first and by alphabet after:
			ExternalSorterOptions options = ExternalSorterOptions.builder()
				.charset(dicParser.getCharset())
				.comparator(ComparatorBuilder.COMPARATOR_LENGTH.thenComparing(ComparatorBuilder.getComparator(dicParser.getLanguage())))
				.useZip(true)
				.removeDuplicates(true)
				.build();
			dicParser.getSorter().sort(outputFile, options, outputFile);

			setProgress(100);

			log.info(Backbone.MARKER_APPLICATION, "Support file sorted");


			log.info(Backbone.MARKER_APPLICATION, "Extracting minimal pairs (pass 2/3)");
			setProgress(0);

			int totalPairs = 0;
			Map<String, List<String>> minimalPairs = new HashMap<>();
			try(BufferedReader sourceBR = Files.newBufferedReader(outputFile.toPath(), dicParser.getCharset())){
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
								if(checker.isConsonant(left) && checker.isConsonant(right)){
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

			log.info(Backbone.MARKER_APPLICATION, "Total minimal pairs: " + DictionaryParser.COUNTER_FORMATTER.format(totalPairs));


			log.info(Backbone.MARKER_APPLICATION, "Reordering minimal pairs (pass 3/3)");
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

			log.info(Backbone.MARKER_APPLICATION, "Minimal pairs file written");

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

			log.info(Backbone.MARKER_APPLICATION, "File written: " + outputFile.getAbsolutePath());
			log.info(Backbone.MARKER_APPLICATION, "Minimal pairs extracted successfully (it takes " + watch.toStringMinuteSeconds() + ")");

			try{
				FileService.openFileWithChoosenEditor(outputFile);
			}
			catch(IOException | InterruptedException e){
				log.warn("Exception while opening the resulting file", e);
			}
		}
		catch(Exception e){
			stopped = true;

			if(e instanceof ClosedChannelException)
				log.info(Backbone.MARKER_APPLICATION, "Duplicates thread interrupted");
			else{
				String message = ExceptionService.getMessage(e);
				log.info(Backbone.MARKER_APPLICATION, e.getClass().getSimpleName() + ": " + message);
			}
		}
		if(stopped)
			log.info(Backbone.MARKER_APPLICATION, "Stopped reading Dictionary file");

		return null;
	}

}
