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
package unit731.hunlinter.workers.affix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.LineEntry;
import unit731.hunlinter.parsers.dictionary.RulesReducer;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.enums.AffixType;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.parsers.vos.RuleEntry;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import unit731.hunlinter.workers.exceptions.LinterException;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import static unit731.hunlinter.services.system.LoopHelper.forEach;


public class RulesReducerWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesReducerWorker.class);

	private static final MessageFormat NON_EXISTENT_RULE = new MessageFormat("Non-existent rule `{0}`, cannot reduce");

	private static final String WORKER_NAME = "Rules reducer";


	private final RulesReducer rulesReducer;


	public RulesReducerWorker(final String flag, final boolean keepLongestCommonAffix, final AffixData affixData,
			final DictionaryParser dicParser, final WordGenerator wordGenerator){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		Objects.requireNonNull(flag, "Flag cannot be null");
		Objects.requireNonNull(affixData, "Affix data cannot be null");
		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");

		rulesReducer = new RulesReducer(affixData, wordGenerator);

		final RuleEntry ruleToBeReduced = affixData.getData(flag);
		if(ruleToBeReduced == null)
			throw new LinterException(NON_EXISTENT_RULE.format(new Object[]{flag}));

		final AffixType type = ruleToBeReduced.getType();

		final Collection<String> originalLines = new ArrayList<>();
		final Collection<LineEntry> originalRules = new ArrayList<>();
		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(indexData.getData(), affixData);
			final Inflection[] inflections = wordGenerator.applyAffixRules(dicEntry);

			final List<LineEntry> filteredRules = rulesReducer.collectInflectionsByFlag(inflections, flag, type);
			if(!filteredRules.isEmpty()){
				originalLines.add(indexData.getData());
				originalRules.addAll(filteredRules);
			}
		};


		final Function<Void, Void> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/3)");
			LOGGER.info(ParserManager.MARKER_RULE_REDUCER_STATUS, "Reading dictionary file (step 1/3)…");

			final Path dicPath = dicParser.getDicFile().toPath();
			final Charset charset = dicParser.getCharset();
			processLines(dicPath, charset, lineProcessor);

			return null;
		};
		final Function<Void, List<LineEntry>> step2 = ignored -> {
			resetProcessing("Extracting minimal rules (step 2/3)");
			LOGGER.info(ParserManager.MARKER_RULE_REDUCER_STATUS, "Extracting minimal rules (step 2/3)…");

			final List<LineEntry> compactedRules = rulesReducer.reduceRules(originalRules, percent -> {
				setProgress(percent, 100);

				sleepOnPause();
			});

			return compactedRules;
		};
		final Function<List<LineEntry>, Void> step3 = compactedRules -> {
			resetProcessing("Verifying correctness (step 3/3)");
			LOGGER.info(ParserManager.MARKER_RULE_REDUCER_STATUS, "Verifying correctness (step 3/3)…");

			final List<String> reducedRules = rulesReducer.convertFormat(flag, keepLongestCommonAffix, compactedRules);

			rulesReducer.checkReductionCorrectness(flag, reducedRules, originalLines, percent -> {
				setProgress(percent, 100);

				sleepOnPause();
			});

			forEach(reducedRules, rule -> LOGGER.info(ParserManager.MARKER_RULE_REDUCER, rule));

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());
			LOGGER.info(ParserManager.MARKER_RULE_REDUCER_STATUS, "Successfully processed");

			return null;
		};
		setProcessor(step1.andThen(step2).andThen(step3));
	}

}
