/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package unit731.hunlinter.workers.dictionary;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.languages.DictionaryCorrectnessChecker;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.services.ParserHelper;
import unit731.hunlinter.services.system.LoopHelper;
import unit731.hunlinter.services.text.HammingDistance;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import unit731.hunlinter.workers.exceptions.LinterException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;


public class MinimalPairsWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(MinimalPairsWorker.class);

	public static final String WORKER_NAME = "Minimal pairs extraction";

	private static final String SLASH = "/";

	private static final char[] NEW_LINE = {'\r', '\n'};


	private final DictionaryCorrectnessChecker checker;
	private final WordGenerator wordGenerator;
	private final DictionaryParser dicParser;
	private final Comparator<String> comparator;


	public MinimalPairsWorker(final ParserManager parserManager, final File outputFile){
		this(parserManager.getLanguage(), parserManager.getDicParser(), parserManager.getChecker(), parserManager.getWordGenerator(),
			outputFile);
	}

	public MinimalPairsWorker(final String language, final DictionaryParser dicParser, final DictionaryCorrectnessChecker checker,
			final WordGenerator wordGenerator, final File outputFile){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

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

			final List<String> words = extractWords();
			writeSupportFile(outputFile, words);

			LOGGER.info(ParserManager.MARKER_APPLICATION, "Support file written");

			return outputFile;
		};
		final Function<File, Map<String, List<String>>> step2 = supportFile -> {
			resetProcessing("Extracting minimal pairs (step 2/3)");

			return extractMinimalPairs(outputFile);
		};
		final Function<Map<String, List<String>>, File> step3 = minimalPairs -> {
			resetProcessing("Reordering minimal pairs (step 3/3)");

			createMinimalPairsFile(outputFile, minimalPairs);

			LOGGER.info(ParserManager.MARKER_APPLICATION, "File written: {}", outputFile.getAbsolutePath());

			finalizeProcessing("Minimal pairs extracted successfully");

			return outputFile;
		};
		final Function<File, Void> step4 = WorkerManager.openFileStep(LOGGER);
		setProcessor(step1.andThen(step2).andThen(step3).andThen(step4));
	}

	private List<String> extractWords(){
		final List<String> list = new ArrayList<>();

		final Charset charset = dicParser.getCharset();
		final File dicFile = dicParser.getDicFile();
		final BiConsumer<Integer, String> fun = (lineIndex, line) -> {
			try{
				final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
				final Inflection[] inflections = wordGenerator.applyAffixRules(dicEntry);

				LoopHelper.applyIf(inflections,
					checker::shouldBeProcessedForMinimalPair,
					inflection -> list.add(inflection.getWord()));
			}
			catch(final LinterException e){
				LOGGER.info(ParserManager.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), lineIndex, line);
			}
		};
		final Consumer<Integer> progressCallback = lineIndex -> {
			setProgress(Math.min(lineIndex, 100));

			sleepOnPause();
		};
		ParserHelper.forEachLine(dicFile, charset, fun, progressCallback,
			ParserHelper.COMMENT_MARK_SHARP, ParserHelper.COMMENT_MARK_SLASH);

		list.sort(BaseBuilder.COMPARATOR_LENGTH.thenComparing(comparator));
		return list;
	}

	private void writeSupportFile(final File file, final Iterable<String> list){
		final Charset charset = dicParser.getCharset();
		try(final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset))){
			LoopHelper.forEach(list, line -> writeLine(writer, line, NEW_LINE));
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
					while((line2 = sourceBR.readLine()) != null && line2.length() == sourceLine.length()){
						final String line2Lowercase = line2.toLowerCase(Locale.ROOT);

						//calculate distance
						final int distance = HammingDistance.getDistance(sourceLineLowercase, line2Lowercase);
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
			final Map<String, List<String>> map = new TreeMap<>(comparator);
			map.putAll(minimalPairs);
			for(final Map.Entry<String, List<String>> entry : map.entrySet()){
				final String key = entry.getKey();
				final List<String> values = entry.getValue();

				destinationWriter.write(key + ": " + StringUtils.join(values, ", "));
				destinationWriter.newLine();

				setProgress(index ++, size);

				sleepOnPause();
			}
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}

}
