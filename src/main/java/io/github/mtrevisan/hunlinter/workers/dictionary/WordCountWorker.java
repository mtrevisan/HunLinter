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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;


public class WordCountWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordCountWorker.class);

	public static final String WORKER_NAME = "Word count";

	private final AtomicInteger totalInflections = new AtomicInteger(0);
	private final Set<String> totalUniqueInflections;
	private final BloomFilterInterface<String> dictionary;


	public WordCountWorker(final ParserManager parserManager, final Consumer<Exception> onCancelled){
		this(parserManager.getLanguage(), parserManager.getDicParser(), parserManager.getWordGenerator(), onCancelled);
	}

	public WordCountWorker(final String language, final DictionaryParser dicParser, final WordGenerator wordGenerator,
			final Consumer<Exception> onCancelled){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
//			.withParallelProcessing()
			.withDataCancelledCallback(onCancelled)
			.withCancelOnException();

		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");


		final BloomFilterParameters dictionaryBaseData = BaseBuilder.getDictionaryBaseData(language);
		dictionary = new ScalableInMemoryBloomFilter<>(dicParser.getCharset(), dictionaryBaseData);

		totalUniqueInflections = new HashSet<>(50_000_000);
//		totalUniqueInflections = ConcurrentHashMap.newKeySet(50_000_000);


		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			final List<Inflection> inflections = wordGenerator.applyAffixRulesWithoutOutputConversion(dicEntry);

			totalInflections.addAndGet(inflections.size());
			for(int i = 0, length = inflections.size(); i < length; i ++){
				final String word = inflections.get(i).getWord();

				//bloom filter says it doesn't contain the word...
				if(!dictionary.contains(word)){
					//... so check also if it's present in the set...
					if(totalUniqueInflections.add(word))
						//... and if it's not, add it to the bloom filter
						dictionary.add(word);
				}
				else
					//bloom filter says it may contain the word, but it could be a false positive...
					totalUniqueInflections.add(word);
			}
		};
		final Consumer<Exception> cancelled = exc -> {
			dictionary.close();

			if(onCancelled != null)
				onCancelled.accept(exc);
		};

		getWorkerData()
			.withDataCancelledCallback(cancelled);

		final Function<Void, Void> step1 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName());

			final Path dicPath = dicParser.getDicFile()
				.toPath();
			final Charset charset = dicParser.getCharset();
			processLines(dicPath, charset, lineProcessor);

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		final Function<Void, Void> step2 = ignored -> {
			dictionary.close();


			LOGGER.info(ParserManager.MARKER_APPLICATION, "Total inflections: {}",
				DictionaryParser.COUNTER_FORMATTER.format(totalInflections));
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Total unique inflections: {}, {}",
				DictionaryParser.COUNTER_FORMATTER.format(totalUniqueInflections.size()),
				DictionaryParser.SHORT_PERCENT_FORMATTER.format((double)totalUniqueInflections.size() / totalInflections.get()));

			return null;
		};
		setProcessor(step1.andThen(step2));
	}

}
