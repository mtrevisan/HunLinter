/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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

import io.github.mtrevisan.hunlinter.gui.ProgressCallback;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.autocorrect.AutoCorrectParser;
import io.github.mtrevisan.hunlinter.parsers.autocorrect.CorrectionEntry;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.services.ParserHelper;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;
import io.github.mtrevisan.hunlinter.workers.core.WorkerAutoCorrect;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDataParser;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;


public class AutoCorrectLinterWorker extends WorkerAutoCorrect{

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoCorrectLinterWorker.class);

	public static final String WORKER_NAME = "AutoCorrect linter";

	private static final String CORRECT_WORD_NOT_IN_DICTIONARY = "Dictionary doesn't contain correct entry {} (from entry {})";


	/**
	 * The words of the (correct forms of the) corrections that have been found into the dictionary.
	 * NOTE: it's enough to look for those few words while the inflections are generated, there's no need to store all of them (an exact
	 * set is also way faster, and smaller, than a bloom filter with as many hash functions as needed for the required precision).
	 */
	private final Set<String> foundWords = ConcurrentHashMap.newKeySet();


	public AutoCorrectLinterWorker(final AutoCorrectParser acoParser, final String language, final DictionaryParser dicParser,
			final WordGenerator wordGenerator, final Consumer<Exception> onCancelled){
		super(new WorkerDataParser<>(WORKER_NAME, acoParser));

		getWorkerData()
			.withParallelProcessing()
			.withDataCancelledCallback(onCancelled)
			.withCancelOnException();

		Objects.requireNonNull(dicParser, "Dictionary parser cannot be null");
		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");

		final Consumer<CorrectionEntry> dataProcessor = data -> {
			final String correctForm = data.getCorrectForm();

			//check if the (correct) word is present in the dictionary
			final String[] words = extractWords(correctForm);
			for(int i = 0; i < words.length; i ++)
				if(!foundWords.contains(words[i]))
					LOGGER.warn(ParserManager.MARKER_APPLICATION, JavaHelper.textFormat(CORRECT_WORD_NOT_IN_DICTIONARY, words[i], correctForm));
		};

		final Function<Void, Void> step1 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName());
			resetProcessing("Reading dictionary file (step 1/2)");

			collectWords(dicParser, wordGenerator, extractSearchedWords(acoParser));

			return null;
		};
		final Function<Void, List<IndexDataPair<CorrectionEntry>>> step2 = ignored -> {
			resetProcessing("Execute " + workerData.getWorkerName() + " (step 2/2)");

			processLines(dataProcessor);

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		setProcessor(step1.andThen(step2));
	}

	/** @return	The words of the given correct form, nothing if the correct form contains some special characters. */
	private static String[] extractWords(final String correctForm){
		final int bound = correctForm.length();
		for(int i = 0; i < bound; i ++){
			final char chr = correctForm.charAt(i);
			if(!Character.isLetter(chr) && !Character.isWhitespace(chr))
				return new String[0];
		}

		return StringUtils.split(correctForm, " –");
	}

	/** @return	The words the (correct forms of the) corrections are made of. */
	private static Set<String> extractSearchedWords(final AutoCorrectParser acoParser){
		final Set<String> searchedWords = new HashSet<>();
		final List<CorrectionEntry> entries = acoParser.getCorrectionsDictionary();
		for(int i = 0, length = entries.size(); i < length; i ++)
			Collections.addAll(searchedWords, extractWords(entries.get(i).getCorrectForm()));
		return searchedWords;
	}

	private void collectWords(final DictionaryParser dicParser, final WordGenerator wordGenerator, final Set<String> searchedWords){
		final File dicFile = dicParser.getDicFile();
		final Charset charset = dicParser.getCharset();

		final BiConsumer<Integer, String> fun = (lineIndex, line) -> {
			try{
				final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
				final List<Inflection> inflections = wordGenerator.applyAffixRulesWithoutOutputConversion(dicEntry);

				for(int i = 0, length = inflections.size(); i < length; i ++){
					final String str = inflections.get(i).getWord();
					if(searchedWords.contains(str))
						foundWords.add(str);
				}
			}
			catch(final LinterException e){
				LOGGER.error(ParserManager.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), lineIndex + 1, line);
			}
		};
		final ProgressCallback progressCallback = lineIndex -> {
			setWorkerProgress(lineIndex);

			sleepOnPause();
		};
		//the inflections are generated in parallel (the bloom filter is thread-safe)
		ParserHelper.forEachDictionaryLineParallel(dicFile, charset, fun, progressCallback);

	}

}
