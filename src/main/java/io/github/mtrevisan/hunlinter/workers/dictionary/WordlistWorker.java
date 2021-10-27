/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.workers.dictionary;

import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.parsers.exceptions.ParserException;
import io.github.mtrevisan.hunlinter.parsers.exceptions.SorterException;
import io.github.mtrevisan.hunlinter.parsers.vos.AffixEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.services.sorters.externalsorter.ExternalSorter;
import io.github.mtrevisan.hunlinter.services.sorters.externalsorter.ExternalSorterOptions;
import io.github.mtrevisan.hunlinter.workers.WorkerManager;
import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDataParser;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;


public class WordlistWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordlistWorker.class);

	public static final String WORKER_NAME = "Wordlist";

	private static final char[] NEW_LINE = {'\n'};
	private static final String SLASH = "/";

	public enum WorkerType{COMPLETE, PLAIN_WORDS, PLAIN_WORDS_NO_DUPLICATES, FULLSTRIP_WORDS}


	private volatile boolean writerClosed;


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

		try{
			final BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), charset);
			final Function<Inflection, String> toString = (type == WorkerType.COMPLETE || type == WorkerType.FULLSTRIP_WORDS
				? Inflection::toString
				: Inflection::getWord);
			final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
				final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
				final List<Inflection> inflections = wordGenerator.applyAffixRules(dicEntry);

				final int size = inflections.size();
				if(type != WorkerType.FULLSTRIP_WORDS)
					for(int i = 0; !writerClosed && i < size; i ++)
						writeLine(writer, toString.apply(inflections.get(i)), NEW_LINE);
				else
					for(int i = 0; !writerClosed && i < size; i ++){
						final Inflection inflection = inflections.get(i);
						if(inflection.isFullstrip()){
							final AffixEntry lastAppliedRule = inflection.getLastAppliedRule();
							final String originatingWord = lastAppliedRule.undoRule(inflection.getWord());
							writeLine(writer, originatingWord + SLASH + lastAppliedRule.getFlag(), NEW_LINE);
						}
					}
			};

			getWorkerData()
				.withDataCancelledCallback(e -> {
					closeWriter(writer);
					writerClosed = true;
				});

			final Function<Void, File> step1 = ignored -> {
				prepareProcessing(WORKER_NAME, "Execute " + workerData.getWorkerName());

				final File dicFile = dicParser.getDicFile();
				processLines(WORKER_NAME, dicFile.toPath(), charset, lineProcessor);

				return outputFile;
			};
			final Function<File, File> step2 = file -> {
				closeWriter(writer);

				resetProcessing(WORKER_NAME, "Sorting");

				//sort file & remove duplicates
				final ExternalSorterOptions options = ExternalSorterOptions.builder()
					.charset(charset)
					.sortInParallel()
					.comparator(dicParser.getComparator())
					.useTemporaryAsZip()
					.removeDuplicates()
					.build();
				try{
					ExternalSorter.sort(outputFile, options, outputFile);
				}
				catch(final IOException ioe){
					throw new SorterException(ioe);
				}

				LOGGER.info(ParserManager.MARKER_APPLICATION, "File written: {}", file.getAbsolutePath());

				finalizeProcessing(WORKER_NAME, "Wordlist extracted successfully");

				return file;
			};
			final Function<File, Void> step3 = WorkerManager.openFileStep(LOGGER);
			setProcessor(step1.andThen(step2).andThen(step3));
		}
		catch(final IOException ioe){
			throw new ParserException(ioe);
		}
	}

}
