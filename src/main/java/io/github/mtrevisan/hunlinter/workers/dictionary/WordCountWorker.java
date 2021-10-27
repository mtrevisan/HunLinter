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

import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.BloomFilterInterface;
import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.BloomFilterParameters;
import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.ScalableInMemoryBloomFilter;
import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDataParser;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;


public class WordCountWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordCountWorker.class);

	public static final String WORKER_NAME = "Word count";

	private final AtomicInteger totalInflections = new AtomicInteger(0);
	private final BloomFilterInterface<String> dictionary;


	public WordCountWorker(final ParserManager parserManager){
		this(parserManager.getLanguage(), parserManager.getDicParser(), parserManager.getWordGenerator());
	}

	public WordCountWorker(final String language, final DictionaryParser dicParser, final WordGenerator wordGenerator){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");


		final BloomFilterParameters dictionaryBaseData = BaseBuilder.getDictionaryBaseData(language);
		dictionary = new ScalableInMemoryBloomFilter<>(dicParser.getCharset(), dictionaryBaseData);

		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			final List<Inflection> inflections = wordGenerator.applyAffixRules(dicEntry);

			totalInflections.addAndGet(inflections.size());
			for(int i = 0; i < inflections.size(); i ++)
				dictionary.add(inflections.get(i).getWord());
		};
		final Consumer<Exception> cancelled = exception -> dictionary.close();

		getWorkerData()
			.withDataCancelledCallback(cancelled);

		final Function<Void, Void> step1 = ignored -> {
			prepareProcessing(WORKER_NAME, "Execute " + workerData.getWorkerName());

			final Path dicPath = dicParser.getDicFile()
				.toPath();
			final Charset charset = dicParser.getCharset();
			processLines(WORKER_NAME, dicPath, charset, lineProcessor);

			finalizeProcessing(WORKER_NAME, "Successfully processed " + workerData.getWorkerName());

			return null;
		};
		final Function<Void, Void> step2 = ignored -> {
			dictionary.close();

			final int totalUniqueInflections = dictionary.getAddedElements();
			final double falsePositiveProbability = dictionary.getTrueFalsePositiveProbability();
			final int falsePositiveCount = (int)Math.ceil(totalUniqueInflections * falsePositiveProbability);
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Total inflections: {}", DictionaryParser.COUNTER_FORMATTER.format(totalInflections));
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Total unique inflections: {} ± {} ({}), {}",
				DictionaryParser.COUNTER_FORMATTER.format(totalUniqueInflections),
				DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability),
				falsePositiveCount,
				DictionaryParser.SHORT_PERCENT_FORMATTER.format((double)totalUniqueInflections / totalInflections.get()));

			return null;
		};
		setProcessor(step1.andThen(step2));
	}

}
