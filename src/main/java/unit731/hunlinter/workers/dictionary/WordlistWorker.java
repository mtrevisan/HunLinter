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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.services.sorters.externalsorter.ExternalSorter;
import unit731.hunlinter.services.sorters.externalsorter.ExternalSorterOptions;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import static unit731.hunlinter.services.system.LoopHelper.forEach;


public class WordlistWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordlistWorker.class);

	public static final String WORKER_NAME = "Wordlist";

	private static final char[] NEW_LINE = {'\n'};

	public enum WorkerType{COMPLETE, PLAIN_WORDS, PLAIN_WORDS_NO_DUPLICATES}


	public WordlistWorker(final ParserManager parserManager, final WorkerType type, final File outputFile){
		this(parserManager.getDicParser(), parserManager.getWordGenerator(), type, outputFile);
	}

	public WordlistWorker(final DictionaryParser dicParser, final WordGenerator wordGenerator, final WorkerType type,
			final File outputFile){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");
		Objects.requireNonNull(outputFile, "Output file cannot be null");


		final Charset charset = dicParser.getCharset();


		final BufferedWriter writer;
		try{
			writer = Files.newBufferedWriter(outputFile.toPath(), charset);
		}
		catch(final IOException e){
			throw new RuntimeException(e);
		}


		final Function<Inflection, String> toString = (type == WorkerType.COMPLETE? Inflection::toString: Inflection::getWord);
		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			final Inflection[] inflections = wordGenerator.applyAffixRules(dicEntry);

			forEach(inflections, inflection -> writeLine(writer, toString.apply(inflection), NEW_LINE));
		};

		getWorkerData()
			.withDataCancelledCallback(e -> closeWriter(writer));

		final Function<Void, File> step1 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName());

			final File dicFile = dicParser.getDicFile();
			processLines(dicFile.toPath(), charset, lineProcessor);
			closeWriter(writer);

			return outputFile;
		};
		final Function<File, File> step2 = file -> {
			resetProcessing("Sorting");

			//sort file & remove duplicates
			final ExternalSorter sorter = new ExternalSorter();
			final ExternalSorterOptions options = ExternalSorterOptions.builder()
				.charset(charset)
				.sortInParallel()
				.comparator(dicParser.getComparator())
				.useTemporaryAsZip()
				.removeDuplicates()
				.build();
			try{
				sorter.sort(outputFile, options, outputFile);
			}
			catch(final Exception e){
				throw new RuntimeException(e);
			}

			LOGGER.info(ParserManager.MARKER_APPLICATION, "File written: {}", file.getAbsolutePath());

			finalizeProcessing("Wordlist extracted successfully");

			return file;
		};
		final Function<File, Void> step3 = WorkerManager.openFileStep(LOGGER);
		setProcessor(step1.andThen(step2).andThen(step3));
	}

}
