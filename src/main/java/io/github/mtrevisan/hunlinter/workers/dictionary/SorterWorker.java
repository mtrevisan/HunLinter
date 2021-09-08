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

import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDataParser;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;


public class SorterWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(SorterWorker.class);

	public static final String WORKER_NAME = "Sorting";

	private final DictionaryParser dicParser;

	private final Comparator<String> comparator;


	public SorterWorker(final File dicFile, final ParserManager parserManager, final int lineIndex){
		super(new WorkerDataParser<>(WORKER_NAME, parserManager.getDicParser()));

		Objects.requireNonNull(dicFile, "Dictionary file cannot be null");

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		dicParser = parserManager.getDicParser();

		comparator = BaseBuilder.getComparator(parserManager.getLanguage());
		final Map.Entry<Integer, Integer> boundary = dicParser.getBoundary(lineIndex);
		//here `boundary` cannot be null
		@SuppressWarnings("ConstantConditions")
		final int sectionStart = boundary.getKey();
		final int sectionEnd = boundary.getValue() + 1;

		final Function<Void, List<String>> step1 = ignored -> {
			prepareProcessing("Load dictionary file (step 1/3)");

			final List<String> lines;
			try{
				lines = FileHelper.readAllLines(dicParser.getDicFile().toPath(), dicParser.getCharset());
			}
			catch(final Exception e){
				throw new RuntimeException(e.getMessage());
			}

			setProgress(33);

			return lines;
		};
		final Function<List<String>, List<String>> step2 = lines -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Sort selected section (step 2/3)");

			//sort the chosen section
			lines.subList(sectionStart, sectionEnd)
				.sort(comparator);

			setProgress(67);

			return lines;
		};
		final Function<List<String>, Void> step3 = lines -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Merge sections (step 3/3)");

			try{
				FileHelper.saveFile(dicParser.getDicFile().toPath(), System.lineSeparator(), dicParser.getCharset(), lines);

				dicParser.removeBoundary(sectionStart);

				finalizeProcessing("Successfully processed " + workerData.getWorkerName());
			}
			catch(final Exception e){
				throw new RuntimeException(e.getMessage());
			}

			return null;
		};
		setProcessor(step1.andThen(step2).andThen(step3));
	}

}

