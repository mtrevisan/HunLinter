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
package io.github.mtrevisan.hunlinter.workers.thesaurus;

import io.github.mtrevisan.hunlinter.datastructures.fsa.lookup.DictionaryLookup;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.parsers.thesaurus.SynonymsEntry;
import io.github.mtrevisan.hunlinter.parsers.thesaurus.ThesaurusDictionary;
import io.github.mtrevisan.hunlinter.parsers.thesaurus.ThesaurusEntry;
import io.github.mtrevisan.hunlinter.parsers.thesaurus.ThesaurusParser;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDataParser;
import io.github.mtrevisan.hunlinter.workers.core.WorkerThesaurus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;


public class ThesaurusLinterFSAWorker extends WorkerThesaurus{

	private static final Logger LOGGER = LoggerFactory.getLogger(ThesaurusLinterFSAWorker.class);

	public static final String WORKER_NAME = "Thesaurus linter against dictionary FSA";

	private static final String MISSING_ENTRY = "Thesaurus doesn't contain definition {} with part-of-speech {} (from entry {})";
	private static final String ENTRY_NOT_IN_DICTIONARY = "Dictionary doesn't contain definition {} (from entry {})";


	public ThesaurusLinterFSAWorker(final ThesaurusParser theParser, final DictionaryParser dicParser, final WordGenerator wordGenerator,
			final DictionaryLookup dictionaryLookup){
		super(new WorkerDataParser<>(WORKER_NAME, theParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		Objects.requireNonNull(dicParser, "Dictionary parser cannot be null");
		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");
		Objects.requireNonNull(dictionaryLookup, "Dictionary lookup cannot be null");

		final Consumer<ThesaurusEntry> dataProcessor = data -> {
			final String originalDefinition = data.getDefinition()
				.toLowerCase(Locale.ROOT);

			//check if the word is present in the dictionary
			final String[] words = StringUtils.split(originalDefinition, " –");
			for(int i = 0; i < words.length; i ++)
				if(dictionaryLookup.lookup(words[i]).isEmpty())
					LOGGER.info(ParserManager.MARKER_APPLICATION, JavaHelper.textFormat(ENTRY_NOT_IN_DICTIONARY, words[i], originalDefinition));

			//check if each part of `entry`, with appropriate PoS, exists
			final List<SynonymsEntry> syns = data.getSynonyms();
			for(int i = 0; i < syns.size(); i ++){
				final SynonymsEntry syn = syns.get(i);
				final List<String> definitions = syn.getSynonyms();
				final String[] partOfSpeeches = syn.getPartOfSpeeches();
				for(int j = 0; j < definitions.size(); j ++){
					final String definition = ThesaurusDictionary.removeSynonymUse(definitions.get(j));
					//check also that the found PoS has `originalDefinition` among its synonyms
					if(!theParser.contains(definition, partOfSpeeches, originalDefinition))
						LOGGER.info(ParserManager.MARKER_APPLICATION, JavaHelper.textFormat(MISSING_ENTRY, definition,
							Arrays.toString(partOfSpeeches), originalDefinition));
				}
			}
		};

		final Function<Void, List<IndexDataPair<ThesaurusEntry>>> step1 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName());

			processLines(dataProcessor);

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		setProcessor(step1);
	}

}
