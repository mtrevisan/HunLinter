/**
 * Copyright (c) 2025 Mauro Trevisan
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
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenationParser;
import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDataParser;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDictionary;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;


public class AffixExtractorWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(AffixExtractorWorker.class);

	public static final String WORKER_NAME = "Affix extraction";

	private final Set<String> wordSet = new HashSet<>();


	public AffixExtractorWorker(final String partOfSpeech, final int minimumAffixLength,
			final ParserManager parserManager, final Runnable onCompleted, final Consumer<Exception> onCancelled){
		this(partOfSpeech, minimumAffixLength, parserManager.getDicParser(), onCompleted, onCancelled);
	}

	public AffixExtractorWorker(final String partOfSpeech, final int minimumAffixLength,
			final DictionaryParser dicParser, final Runnable onCompleted, final Consumer<Exception> onCancelled){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withDataCompletedCallback(onCompleted)
			.withDataCancelledCallback(onCancelled)
			.withCancelOnException();


		wordSet.clear();

		final Map<String, Set<String>> suffixMap = new HashMap<>();
		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			String data = indexData.getData();
			data = data.trim();
			if(!data.startsWith("/") && data.contains("po:" + partOfSpeech)){
				int idx = data.indexOf('/');
				if(idx > 0)
					data = data.substring(0, idx);
				else{
					idx = data.indexOf('\t');
					if(idx > 0)
						data = data.substring(0, idx);
				}

				wordSet.add(data);
			}

			//construction of the suffix map
			for(final String word : wordSet)
				for(int i = minimumAffixLength; i < word.length(); i ++){
					final String suffix = word.substring(word.length() - i);
					final int prefixLength = word.length() - suffix.length();
					final String prefix = word.substring(0, prefixLength);

					//adds only if the suffix is also a valid word in the list and the prefix is not a compound word
					final int compoundIndex = prefix.indexOf(HyphenationParser.MINUS_SIGN);
					boolean compoundedPrefix = (compoundIndex >= 0 && compoundIndex < prefix.length() - 1);
					if(wordSet.contains(suffix) && !compoundedPrefix)
						suffixMap.computeIfAbsent(suffix, k -> new HashSet<>()).add(word);
				}
			};

		final Consumer<Exception> cancelled = exc -> {
			wordSet.clear();

			if(onCancelled != null)
				onCancelled.accept(exc);
		};

		getWorkerData()
			.withDataCancelledCallback(cancelled);

		final Function<Void, Void> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/2)");
			LOGGER.info(ParserManager.MARKER_AFFIX_EXTRACTOR_STATUS, "Reading dictionary file (step 1/2)…");

			final Path dicPath = dicParser.getDicFile()
				.toPath();
			final Charset charset = dicParser.getCharset();
			processLines(dicPath, charset, lineProcessor);

			return null;
		};
		final Function<Void, Void> step2 = ignored -> {
			resetProcessing("Extracting affixes (step 2/2)");
			LOGGER.info(ParserManager.MARKER_AFFIX_EXTRACTOR_STATUS, "Extracting affixes (step 2/2)…");

			//sort by length of suffix (descending), then by number of words (descending)
			final List<Map.Entry<String, Set<String>>> validSuffixes = new ArrayList<>(suffixMap.entrySet());
			validSuffixes.sort((a, b) -> {
				final int lengthDifference = b.getKey().length() - a.getKey().length();
				return (lengthDifference != 0? lengthDifference: b.getValue().size() - a.getValue().size());
			});

			for(int j = 0, validSuffixesSize = validSuffixes.size(); j < validSuffixesSize; j ++){
				Map.Entry<String, Set<String>> entry = validSuffixes.get(j);
				final String suffix = entry.getKey();
				final List<String> group = new ArrayList<>(entry.getValue());

				//sort the words from shortest to longest
				group.sort(Comparator.comparingInt(String::length)
					.thenComparing(Comparator.naturalOrder()));

				LOGGER.info(ParserManager.MARKER_AFFIX_EXTRACTOR, "Suffix: {}", suffix);
				final StringJoiner sj = new StringJoiner(", ");
				for(int i = 0, groupSize = group.size(); i < groupSize; i ++){
					final String word = group.get(i);
					final int prefixLength = word.length() - suffix.length();
					final String prefix = word.substring(0, prefixLength);
					sj.add(prefix);
				}
				LOGGER.info(ParserManager.MARKER_AFFIX_EXTRACTOR, "Prefix: " + sj);

				if(j < validSuffixesSize - 1)
					LOGGER.info(ParserManager.MARKER_AFFIX_EXTRACTOR, StringUtils.EMPTY);
			}

			return null;
		};
		final Function<Void, Void> step3 = ignored -> {
			wordSet.clear();

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());
			LOGGER.info(ParserManager.MARKER_AFFIX_EXTRACTOR_STATUS, "Successfully processed");

			return null;
		};
		setProcessor(step1.andThen(step2).andThen(step3));
	}

}
