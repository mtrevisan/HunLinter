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
package io.github.mtrevisan.hunlinter.workers.affix;

import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.LineEntry;
import io.github.mtrevisan.hunlinter.parsers.dictionary.RulesReducer;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixType;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntryFactory;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.parsers.vos.RuleEntry;
import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDataParser;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDictionary;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;


public class RulesReducerWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesReducerWorker.class);

	private static final String NON_EXISTENT_RULE = "Non-existent rule `{}`, cannot reduce";

	public static final String WORKER_NAME = "Rules reducer";


	private final RulesReducer rulesReducer;


	public RulesReducerWorker(final String flag, final boolean keepLongestCommonAffix, final AffixData affixData,
			final DictionaryParser dicParser, final WordGenerator wordGenerator, final Runnable onCompleted,
			final Consumer<Exception> onCancelled){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		Objects.requireNonNull(flag, "Flag cannot be null");
		Objects.requireNonNull(affixData, "Affix data cannot be null");
		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");

		rulesReducer = new RulesReducer(affixData, wordGenerator);
		final DictionaryEntryFactory dictionaryEntryFactory = new DictionaryEntryFactory(affixData);

		final RuleEntry ruleToBeReduced = affixData.getData(flag);
		if(ruleToBeReduced == null)
			throw new LinterException(NON_EXISTENT_RULE, flag);

		final AffixType type = ruleToBeReduced.getType();

		final List<String> originalLines = new ArrayList<>(0);
		final List<LineEntry> originalRules = new ArrayList<>(0);
		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = dictionaryEntryFactory.createFromDictionaryLine(indexData.getData());
			final List<Inflection> inflections = wordGenerator.applyAffixRules(dicEntry);

			final LineEntry filteredRule = rulesReducer.collectInflectionsByFlag(inflections, flag, type);
			if(filteredRule != null){
				originalLines.add(indexData.getData());
				originalRules.add(filteredRule);
			}
		};

		getWorkerData()
			.withDataCompletedCallback(onCompleted)
			.withDataCancelledCallback(onCancelled);


		final String[] workerIDs = defineWorkerProgresses(WORKER_NAME, 3);
		final Function<Void, Void> step1 = ignored -> {
			prepareProcessing(workerIDs[0], "Reading dictionary file (step 1/3)");
			LOGGER.info(ParserManager.MARKER_RULE_REDUCER_STATUS, "Reading dictionary file (step 1/3)…");

			final Path dicPath = dicParser.getDicFile()
				.toPath();
			final Charset charset = dicParser.getCharset();
			processLines(workerIDs[0], dicPath, charset, lineProcessor);

			setWorkerProgress(workerIDs[0], 100);

			return null;
		};
		final Function<Void, List<LineEntry>> step2 = ignored -> {
			resetProcessing(WORKER_NAME, "Extracting minimal rules (step 2/3)");
			LOGGER.info(ParserManager.MARKER_RULE_REDUCER_STATUS, "Extracting minimal rules (step 2/3)…");

			try{
				final List<LineEntry> reducedRules = rulesReducer.reduceRules(originalRules, percent -> {
					setWorkerProgress(WORKER_NAME, percent);

					sleepOnPause();
				});

				setWorkerProgress(workerIDs[1], 100);

				return reducedRules;
			}
			catch(final Exception e){
				LOGGER.error(ParserManager.MARKER_RULE_REDUCER_STATUS, "Something very bad happened");

				throw e;
			}
		};
		final Function<List<LineEntry>, Void> step3 = compactedRules -> {
			resetProcessing(WORKER_NAME, "Verifying correctness (step 3/3)");
			LOGGER.info(ParserManager.MARKER_RULE_REDUCER_STATUS, "Verifying correctness (step 3/3)…");

			final List<String> reducedRules = rulesReducer.convertFormat(flag, keepLongestCommonAffix, compactedRules);

			try{
				rulesReducer.checkReductionCorrectness(flag, reducedRules, originalLines, percent -> {
					setWorkerProgress(WORKER_NAME, percent);

					sleepOnPause();
				});
			}
			catch(final Exception e){
				LOGGER.error(ParserManager.MARKER_RULE_REDUCER_STATUS, "Something very bad happened");

				throw e;
			}

			for(int i = 0; i < reducedRules.size(); i ++)
				LOGGER.info(ParserManager.MARKER_RULE_REDUCER, reducedRules.get(i));

			finalizeProcessing(WORKER_NAME, "Successfully processed " + workerData.getWorkerName());
			LOGGER.info(ParserManager.MARKER_RULE_REDUCER_STATUS, "Successfully processed");

			return null;
		};
		setProcessor(step1.andThen(step2).andThen(step3));
	}

}
