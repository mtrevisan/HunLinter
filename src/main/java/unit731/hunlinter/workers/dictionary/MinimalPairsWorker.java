package unit731.hunlinter.workers.dictionary;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.languages.DictionaryCorrectnessChecker;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import unit731.hunlinter.workers.exceptions.LinterException;
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

		comparator = BaseBuilder.getComparator(language);

		final Function<Void, File> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/3)");

			createSupportFile(outputFile);

			LOGGER.info(ParserManager.MARKER_APPLICATION, "Support file written");

			sortSupportFile(outputFile);

			LOGGER.info(ParserManager.MARKER_APPLICATION, "Support file sorted");

			return outputFile;
		};
		final Function<File, Map<String, List<String>>> step2 = supportFile -> {
			setProgress(0);
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Extracting minimal pairs (step 2/3)");

			return extractMinimalPairs(outputFile);
		};
		final Function<Map<String, List<String>>, File> step3 = minimalPairs -> {
			setProgress(0);
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Reordering minimal pairs (step 3/3)");

			createMinimalPairsFile(outputFile, minimalPairs);
			sortMinimalPairs(outputFile);

			setProgress(100);
			LOGGER.info(ParserManager.MARKER_APPLICATION, "File written: {}", outputFile.getAbsolutePath());

			finalizeProcessing("Minimal pairs extracted successfully");

			return outputFile;
		};
		final Function<File, Void> step4 = WorkerManager.openFileStep(LOGGER);
		setProcessor(step1.andThen(step2).andThen(step3).andThen(step4));
	}

	private void createSupportFile(final File supportFile){
		final Charset charset = dicParser.getCharset();
		final File dicFile = dicParser.getDicFile();
		int currentLine = 0;
		final int totalLines = FileHelper.countLines(dicFile.toPath());
		try(
				final LineNumberReader br = FileHelper.createReader(dicFile.toPath(), charset);
				final BufferedWriter writer = Files.newBufferedWriter(supportFile.toPath(), charset);
				){
			String line = ParserHelper.extractLine(br);
			currentLine ++;

			if(!NumberUtils.isCreatable(line))
				throw new LinterException(WRONG_FILE_FORMAT.format(new Object[]{line}));

			while((line = br.readLine()) != null){
				currentLine ++;

				if(!ParserHelper.isComment(line, ParserHelper.COMMENT_MARK_SHARP, ParserHelper.COMMENT_MARK_SLASH)){
					try{
						final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
						final Production[] productions = wordGenerator.applyAffixRules(dicEntry);

						for(final Production production : productions)
							if(checker.shouldBeProcessedForMinimalPair(production)){
								final String word = production.getWord();
								writer.write(word);
								writer.newLine();

								sleepOnPause();
							}
					}
					catch(final LinterException e){
						LOGGER.info(ParserManager.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), currentLine, line);
					}
				}

				setProgress(currentLine, totalLines);

				sleepOnPause();
			}
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}

	private void sortSupportFile(final File supportFile){
		//sort file by length first and by alphabet after:
		final ExternalSorterOptions options = ExternalSorterOptions.builder()
			.charset(dicParser.getCharset())
			.comparator(BaseBuilder.COMPARATOR_LENGTH.thenComparing(comparator))
			.useZip(true)
			.removeDuplicates(true)
			.build();
		try{
			dicParser.getSorter().sort(supportFile, options, supportFile);
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}

	private Map<String, List<String>> extractMinimalPairs(final File outputFile){
		final Charset charset = dicParser.getCharset();
		int totalPairs = 0;
		final Map<String, List<String>> minimalPairs = new HashMap<>();
		try(final BufferedReader sourceBR = Files.newBufferedReader(outputFile.toPath(), dicParser.getCharset())){
			String sourceLine;
			long readSoFarSource = 0;
			final long totalSizeSource = outputFile.length();
			while((sourceLine = sourceBR.readLine()) != null){
				//FIXME find a way to have the newline size
				readSoFarSource += sourceLine.getBytes(charset).length + 2;

				sourceBR.mark((int)(totalSizeSource - readSoFarSource + 2));

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
								minimalPairs.computeIfAbsent(key, k -> new ArrayList<>(1))
									.add(value);

								totalPairs ++;
							}

							sleepOnPause();
						}
					}
				}
				catch(final Exception ignored){
					//FIXME
					//length varied, consider another line for minimal pair search
				}

				sourceBR.reset();

				setProgress(readSoFarSource, totalSizeSource);
			}

			LOGGER.info(ParserManager.MARKER_APPLICATION, "Total minimal pairs: {}", DictionaryParser.COUNTER_FORMATTER.format(totalPairs));
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
		return minimalPairs;
	}

	private void createMinimalPairsFile(final File file, final Map<String, List<String>> minimalPairs){
		try(final BufferedWriter destinationWriter = Files.newBufferedWriter(file.toPath(), dicParser.getCharset())){
			int index = 0;
			final int size = minimalPairs.size();
			for(final Map.Entry<String, List<String>> entry : minimalPairs.entrySet()){
				final String key = entry.getKey();
				final List<String> values = entry.getValue();

				destinationWriter.write(key + ": " + StringUtils.join(values, ", "));
				destinationWriter.newLine();

				setProgress((int)((index * 100.) / size));

				sleepOnPause();
			}
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}

	private void sortMinimalPairs(final File file){
		//sort file alphabetically:
		final ExternalSorterOptions options = ExternalSorterOptions.builder()
			.charset(dicParser.getCharset())
			.comparator(comparator)
			.useZip(true)
			.removeDuplicates(true)
			.build();
		try{
			dicParser.getSorter().sort(file, options, file);
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}

}
