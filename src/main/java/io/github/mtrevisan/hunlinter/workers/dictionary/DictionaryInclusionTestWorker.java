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
import java.util.function.Consumer;
import java.util.function.Function;


public class DictionaryInclusionTestWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryInclusionTestWorker.class);

	private static final String WORKER_NAME = "Dictionary inclusion test";

	private final BloomFilterInterface<String> dictionary;


	public DictionaryInclusionTestWorker(final String language, final DictionaryParser dicParser, final WordGenerator wordGenerator){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		Objects.requireNonNull(language, "Language cannot be null");
		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");


		final BloomFilterParameters dictionaryBaseData = BaseBuilder.getDictionaryBaseData(language);
		dictionary = new ScalableInMemoryBloomFilter<>(dicParser.getCharset(), dictionaryBaseData);

		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			final List<Inflection> inflections = wordGenerator.applyAffixRules(dicEntry);

			for(final Inflection prod : inflections)
				dictionary.add(prod.getWord());
		};
		final Consumer<Exception> cancelled = exception -> dictionary.close();

		getWorkerData()
			.withDataCancelledCallback(cancelled);

		final Function<Void, Void> step1 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName());

			final Path dicPath = dicParser.getDicFile().toPath();
			final Charset charset = dicParser.getCharset();
			processLines(dicPath, charset, lineProcessor);

			return null;
		};
		final Function<Void, Void> step2 = ignored -> {
			dictionary.close();

			final int totalUniqueInflections = dictionary.getAddedElements();
			final double falsePositiveProbability = dictionary.getTrueFalsePositiveProbability();
			final int falsePositiveCount = (int)Math.ceil(totalUniqueInflections * falsePositiveProbability);
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Total unique inflections: {} ± {} ({})",
				DictionaryParser.COUNTER_FORMATTER.format(totalUniqueInflections),
				DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability),
				falsePositiveCount);

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		setProcessor(step1.andThen(step2));
	}

	public boolean isInDictionary(final String word){
		return dictionary.contains(word);
	}

}
