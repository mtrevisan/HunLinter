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
package unit731.hunlinter.workers.thesaurus;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.datastructures.bloomfilter.BloomFilterInterface;
import unit731.hunlinter.datastructures.bloomfilter.BloomFilterParameters;
import unit731.hunlinter.datastructures.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.thesaurus.SynonymsEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusDictionary;
import unit731.hunlinter.parsers.thesaurus.ThesaurusEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.services.ParserHelper;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerThesaurus;
import unit731.hunlinter.workers.exceptions.LinterException;

import java.io.File;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;


public class ThesaurusLinterWorker extends WorkerThesaurus{

	private static final Logger LOGGER = LoggerFactory.getLogger(ThesaurusLinterWorker.class);

	public static final String WORKER_NAME = "Thesaurus linter";

	private static final MessageFormat MISSING_ENTRY = new MessageFormat("Thesaurus doesn''t contain definition {0} with part-of-speech {1} (from entry {2})");
	private static final MessageFormat ENTRY_NOT_IN_DICTIONARY = new MessageFormat("Dictionary doesn''t contain definition {0} (from entry {1})");


	private final BloomFilterInterface<String> bloomFilter;


	public ThesaurusLinterWorker(final ThesaurusParser theParser, final String language, final DictionaryParser dicParser,
			final WordGenerator wordGenerator){
		super(new WorkerDataParser<>(WORKER_NAME, theParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		Objects.requireNonNull(dicParser, "Dictionary parser cannot be null");
		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");

		//TODO orthography check (DictionaryCorrectnessChecker.checkInflection)
//		final ParserManager parserManager = workerData.getParserManager();
//		final DictionaryCorrectnessChecker correctnessChecker = parserManager.getCorrectnessChecker();
//		checker.checkInflection(inflection, index);

		final Charset charset = dicParser.getCharset();
		final BloomFilterParameters dictionaryBaseData = BaseBuilder.getDictionaryBaseData(language);
		bloomFilter = new ScalableInMemoryBloomFilter<>(charset, dictionaryBaseData);

		final Consumer<ThesaurusEntry> dataProcessor = data -> {
			final String originalDefinition = data.getDefinition();

			//check if the word is present in the dictionary
			final String[] words = StringUtils.split(originalDefinition, " –");
			for(final String word : words)
				if(!bloomFilter.contains(word))
					LOGGER.info(ParserManager.MARKER_APPLICATION, ENTRY_NOT_IN_DICTIONARY.format(
						new Object[]{word, originalDefinition}));

			//check if each part of `entry`, with appropriate PoS, exists
			final List<SynonymsEntry> syns = data.getSynonyms();
			for(final SynonymsEntry syn : syns){
				final List<String> definitions = syn.getSynonyms();
				final String[] partOfSpeeches = syn.getPartOfSpeeches();
				for(String definition : definitions){
					definition = ThesaurusDictionary.removeSynonymUse(definition);
					//check also that the found PoS has `originalDefinition` among its synonyms
					if(!theParser.contains(definition, partOfSpeeches, originalDefinition))
						LOGGER.info(ParserManager.MARKER_APPLICATION, MISSING_ENTRY.format(
							new Object[]{definition, Arrays.toString(partOfSpeeches), originalDefinition}));
				}
			}
		};

		final Function<Void, Void> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/2)");

			collectWords(dicParser, wordGenerator);

			return null;
		};
		final Function<Void, List<IndexDataPair<ThesaurusEntry>>> step2 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName() + " (step 2/2)");

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
				final Inflection[] inflections = wordGenerator.applyAffixRules(dicEntry);

				for(final Inflection inflection : inflections){
					final String str = inflection.getWord().toLowerCase(Locale.ROOT);
					bloomFilter.add(str);
				}
			}
			catch(final LinterException e){
				LOGGER.info(ParserManager.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), lineIndex, line);
			}
		};
		final Consumer<Integer> progressCallback = lineIndex -> {
			setProgress(Math.min(lineIndex, 100));

			sleepOnPause();
		};
		ParserHelper.forEachLine(dicFile, charset, fun, progressCallback,
			ParserHelper.COMMENT_MARK_SHARP, ParserHelper.COMMENT_MARK_SLASH);

		bloomFilter.close();

		return bloomFilter;
	}

}
