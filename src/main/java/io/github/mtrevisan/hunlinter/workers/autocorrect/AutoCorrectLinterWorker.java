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

import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.BloomFilterInterface;
import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.BloomFilterParameters;
import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.ScalableInMemoryBloomFilter;
import io.github.mtrevisan.hunlinter.gui.ProgressCallback;
import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
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
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;


public class AutoCorrectLinterWorker extends WorkerAutoCorrect{

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoCorrectLinterWorker.class);

	public static final String WORKER_NAME = "AutoCorrect linter";

	private static final String CORRECT_WORD_NOT_IN_DICTIONARY = "Dictionary doesn't contain correct entry {} (from entry {})";


	private final BloomFilterInterface<String> bloomFilter;


	public AutoCorrectLinterWorker(final AutoCorrectParser acoParser, final String language, final DictionaryParser dicParser,
			final WordGenerator wordGenerator){
		super(new WorkerDataParser<>(WORKER_NAME, acoParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		Objects.requireNonNull(dicParser, "Dictionary parser cannot be null");
		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");

		final Charset charset = dicParser.getCharset();
		final BloomFilterParameters dictionaryBaseData = BaseBuilder.getDictionaryBaseData(language);
		bloomFilter = new ScalableInMemoryBloomFilter<>(charset, dictionaryBaseData);

		final Consumer<CorrectionEntry> dataProcessor = data -> {
			final String correctForm = data.getCorrectForm();

			boolean correctWordContainsSpecialChars = false;
			final int bound = correctForm.length();
			for(int i = 0; i < bound; i ++){
				final char chr = correctForm.charAt(i);
				if(!Character.isLetter(chr) && !Character.isWhitespace(chr)){
					correctWordContainsSpecialChars = true;
					break;
				}
			}

			if(!correctWordContainsSpecialChars){
				//check if the (correct) word is present in the dictionary
				final String[] words = StringUtils.split(correctForm, " –");
				for(int i = 0; i < words.length; i ++)
					if(!bloomFilter.contains(words[i]))
						LOGGER.warn(ParserManager.MARKER_APPLICATION, JavaHelper.textFormat(CORRECT_WORD_NOT_IN_DICTIONARY, words[i], correctForm));
			}
		};

		final Function<Void, Void> step1 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName());
			resetProcessing("Reading dictionary file (step 1/2)");

			collectWords(dicParser, wordGenerator);

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

	private BloomFilterInterface<String> collectWords(final DictionaryParser dicParser, final WordGenerator wordGenerator){
		final File dicFile = dicParser.getDicFile();
		final Charset charset = dicParser.getCharset();

		final BiConsumer<Integer, String> fun = (lineIndex, line) -> {
			try{
				final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
				final List<Inflection> inflections = wordGenerator.applyAffixRules(dicEntry);

				for(int i = 0; i < inflections.size(); i ++){
					final String str = inflections.get(i).getWord();
					bloomFilter.add(str);
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
		ParserHelper.forEachDictionaryLine(dicFile, charset, fun, progressCallback);

		bloomFilter.close();

		return bloomFilter;
	}

}
