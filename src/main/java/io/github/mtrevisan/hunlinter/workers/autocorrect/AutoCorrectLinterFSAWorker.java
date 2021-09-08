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
package io.github.mtrevisan.hunlinter.workers.autocorrect;

import io.github.mtrevisan.hunlinter.datastructures.fsa.lookup.DictionaryLookup;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.autocorrect.AutoCorrectParser;
import io.github.mtrevisan.hunlinter.parsers.autocorrect.CorrectionEntry;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;
import io.github.mtrevisan.hunlinter.workers.core.WorkerAutoCorrect;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDataParser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;


public class AutoCorrectLinterFSAWorker extends WorkerAutoCorrect{

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoCorrectLinterFSAWorker.class);

	public static final String WORKER_NAME = "AutoCorrect linter against dictionary FSA";

	private static final String ENTRY_NOT_IN_DICTIONARY = "Dictionary doesn't contain correct entry {} (from entry {})";


	public AutoCorrectLinterFSAWorker(final AutoCorrectParser acoParser, final DictionaryParser dicParser, final WordGenerator wordGenerator,
			final DictionaryLookup dictionaryLookup){
		super(new WorkerDataParser<>(WORKER_NAME, acoParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		Objects.requireNonNull(dicParser, "Dictionary parser cannot be null");
		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");
		Objects.requireNonNull(dictionaryLookup, "Dictionary lookup cannot be null");

		final Consumer<CorrectionEntry> dataProcessor = data -> {
			final String correctForm = data.getCorrectForm()
				.toLowerCase(Locale.ROOT);

			boolean containsSpecialChars = false;
			final int bound = correctForm.length();
			for(int i = 0; i < bound; i ++){
				final char chr = correctForm.charAt(i);
				if(!Character.isLetter(chr) && !Character.isWhitespace(chr)){
					containsSpecialChars = true;
					break;
				}
			}

			if(!containsSpecialChars){
				//check if the word is present in the dictionary
				final String[] words = StringUtils.split(correctForm, " –");
				for(final String word : words)
					if(dictionaryLookup.lookup(word).isEmpty())
						LOGGER.info(ParserManager.MARKER_APPLICATION, JavaHelper.textFormat(ENTRY_NOT_IN_DICTIONARY, word, correctForm));
			}
		};

		final Function<Void, List<IndexDataPair<CorrectionEntry>>> step1 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName());

			processLines(dataProcessor);

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		setProcessor(step1);
	}

}
