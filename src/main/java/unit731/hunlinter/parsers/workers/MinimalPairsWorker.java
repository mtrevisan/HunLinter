package unit731.hunlinter.parsers.workers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.Backbone;
import unit731.hunlinter.languages.DictionaryCorrectnessChecker;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.workers.core.WorkerDataParser;
import unit731.hunlinter.parsers.workers.core.WorkerDictionary;
import unit731.hunlinter.parsers.workers.exceptions.LinterException;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.text.HammingDistance;
import unit731.hunlinter.services.ParserHelper;
import unit731.hunlinter.services.externalsorter.ExternalSorterOptions;


public class MinimalPairsWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(MinimalPairsWorker.class);

	private static final MessageFormat WRONG_FILE_FORMAT = new MessageFormat("Dictionary file malformed, the first line is not a number, was ''{0}''");

	public static final String WORKER_NAME = "Minimal pairs extraction";

	private static final String SLASH = "/";

	private final DictionaryCorrectnessChecker checker;
	private final WordGenerator wordGenerator;
	private final DictionaryParser dicParser;
	private final File outputFile;
	private final Comparator<String> comparator;


	public MinimalPairsWorker(final String language, final DictionaryParser dicParser, final DictionaryCorrectnessChecker checker,
			final WordGenerator wordGenerator, final File outputFile){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser)
			.withParallelProcessing(true)
			.withRelaunchException(false));

		Objects.requireNonNull(language);
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(checker);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(outputFile);


		this.dicParser = dicParser;
		this.checker = checker;
		this.wordGenerator = wordGenerator;
		this.outputFile = outputFile;

		comparator = BaseBuilder.getComparator(language);
	}

	@Override
	protected Void doInBackground(){
		try{
			prepareProcessing("Opening Dictionary file for minimal pairs extraction (pass 1/3)");

			final Charset charset = dicParser.getCharset();
			final File dicFile = dicParser.getDicFile();
			try(
					final LineNumberReader br = FileHelper.createReader(dicFile.toPath(), charset);
					final BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), charset);
					){
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

							for(final Production production : productions)
								if(checker.shouldBeProcessedForMinimalPair(production)){
									final String word = production.getWord();
									writer.write(word);
									writer.newLine();

									waitIfPaused();
								}
						}
						catch(final LinterException e){
							LOGGER.info(Backbone.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), lineIndex, line);
						}
					}

					setProcessingProgress(readSoFar, totalSize);

					waitIfPaused();
				}
			}
			LOGGER.info(Backbone.MARKER_APPLICATION, "Support file written");

			//sort file by length first and by alphabet after:
			ExternalSorterOptions options = ExternalSorterOptions.builder()
				.charset(dicParser.getCharset())
				.comparator(BaseBuilder.COMPARATOR_LENGTH.thenComparing(comparator))
				.useZip(true)
				.removeDuplicates(true)
				.build();
			dicParser.getSorter().sort(outputFile, options, outputFile);

			setProgress(100);

			LOGGER.info(Backbone.MARKER_APPLICATION, "Support file sorted");


			LOGGER.info(Backbone.MARKER_APPLICATION, "Extracting minimal pairs (pass 2/3)");
			setProgress(0);

			int totalPairs = 0;
			final Map<String, List<String>> minimalPairs = new HashMap<>();
			try(final BufferedReader sourceBR = Files.newBufferedReader(outputFile.toPath(), dicParser.getCharset())){
				String sourceLine;
				long readSoFarSource = 0;
				final long totalSizeSource = outputFile.length();
				while((sourceLine = sourceBR.readLine()) != null){
					readSoFarSource += sourceLine.getBytes(charset).length + 2;

					sourceBR.mark((int)(totalSizeSource - readSoFarSource));

					try{
						final String sourceLineLowercase = sourceLine.toLowerCase(Locale.ROOT);

						String line2;
						while((line2 = sourceBR.readLine()) != null){
							final String line2Lowercase = line2.toLowerCase(Locale.ROOT);

							//calculate distance
							int distance = HammingDistance.getDistance(sourceLineLowercase, line2Lowercase);
							if(distance == 1){
								final Pair<Character, Character> difference = HammingDistance.findFirstDifference(sourceLineLowercase, line2Lowercase);
								final char left = difference.getLeft();
								final char right = difference.getRight();
								if(checker.isConsonant(left) && checker.isConsonant(right)){
									final String key = left + SLASH + right;
									final String value = sourceLine + SLASH + line2;
									minimalPairs.computeIfAbsent(key, k -> new ArrayList<>())
										.add(value);

									totalPairs ++;
								}

								waitIfPaused();
							}
						}
					}
					catch(final Exception ignored){
						//FIXME
						//length varied, consider another line for minimal pair search
					}

					sourceBR.reset();

					setProcessingProgress(readSoFarSource, totalSizeSource);
				}
			}
			setProgress(100);

			LOGGER.info(Backbone.MARKER_APPLICATION, "Total minimal pairs: {}", DictionaryParser.COUNTER_FORMATTER.format(totalPairs));


			LOGGER.info(Backbone.MARKER_APPLICATION, "Reordering minimal pairs (pass 3/3)");
			setProgress(0);

			//write result
			try(final BufferedWriter destinationWriter = Files.newBufferedWriter(outputFile.toPath(), dicParser.getCharset())){
				int index = 0;
				final int size = minimalPairs.size();
				for(final Map.Entry<String, List<String>> entry : minimalPairs.entrySet()){
					final String key = entry.getKey();
					final List<String> values = entry.getValue();

					destinationWriter.write(key + ": " + StringUtils.join(values, ", "));
					destinationWriter.newLine();

					setProgress((int)((index * 100.) / size));

					waitIfPaused();
				}
			}

			setProgress(100);

			LOGGER.info(Backbone.MARKER_APPLICATION, "Minimal pairs file written");

			//sort file alphabetically:
			options = ExternalSorterOptions.builder()
				.charset(dicParser.getCharset())
				.comparator(comparator)
				.useZip(true)
				.removeDuplicates(true)
				.build();
			dicParser.getSorter().sort(outputFile, options, outputFile);

			LOGGER.info(Backbone.MARKER_APPLICATION, "File written: {}", outputFile.getAbsolutePath());
			finalizeProcessing("Minimal pairs extracted successfully");

			try{
				FileHelper.openFileWithChosenEditor(outputFile);
			}
			catch(final IOException | InterruptedException e){
				LOGGER.warn("Exception while opening the resulting file", e);
			}
		}
		catch(final Exception e){
			cancel(e);
		}

		return null;
	}

}
