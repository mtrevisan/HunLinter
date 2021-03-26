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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.languages.DictionaryCorrectnessChecker;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.thesaurus.SynonymsEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusDictionary;
import unit731.hunlinter.parsers.thesaurus.ThesaurusEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerThesaurus;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;


public class ThesaurusLinterWorker extends WorkerThesaurus{

	private static final Logger LOGGER = LoggerFactory.getLogger(ThesaurusLinterWorker.class);

	public static final String WORKER_NAME = "Thesaurus linter";

	private static final MessageFormat MISSING_ENTRY = new MessageFormat("Thesaurus doesn''t contain definition {0} with part-of-speech {1} (from entry {2})");


	public ThesaurusLinterWorker(final ThesaurusParser theParser){
		super(new WorkerDataParser<>(WORKER_NAME, theParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		//TODO orthography check (DictionaryCorrectnessChecker.checkInflection)
//		final DictionaryCorrectnessChecker checker = parserManager.getChecker();
//		checker.checkInflection(inflection, index);

		final Consumer<ThesaurusEntry> dataProcessor = data -> {
			final String originalDefinition = data.getDefinition();
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

		final Function<Void, List<IndexDataPair<ThesaurusEntry>>> step1 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName());

			processLines(dataProcessor);

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		setProcessor(step1);
	}

}
