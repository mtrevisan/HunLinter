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

import io.github.mtrevisan.hunlinter.languages.DictionaryCorrectnessChecker;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.parsers.vos.AffixEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDataParser;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDictionary;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;


public class DictionaryLinterWorker extends WorkerDictionary{

	public static final String WORKER_NAME = "Dictionary linter";

	private static final String UNUSED_FLAGS = "Unused flags: {}";
	private static final String UNUSED_RULES = "Unused rules in flag {}: {}";


	public DictionaryLinterWorker(final ParserManager parserManager){
		this(parserManager.getAffParser(), parserManager.getDicParser(), parserManager.getChecker(), parserManager.getWordGenerator());
	}

	public DictionaryLinterWorker(final AffixParser affParser, final DictionaryParser dicParser, final DictionaryCorrectnessChecker checker,
			final WordGenerator wordGenerator){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withParallelProcessing();

		Objects.requireNonNull(checker, "Checker cannot be null");
		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");

		//collectors of flags
		final Set<String> usedFlags = ConcurrentHashMap.newKeySet();
//		final Map<String, Set<AffixEntry>> usedFlags = new ConcurrentHashMap();

		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			checker.checkCircumfix(dicEntry);
			final Collection<Inflection> inflections = wordGenerator.applyAffixRules(dicEntry);
//enforce prefixes flags after suffixes
//boolean prefix = false;
//io.github.mtrevisan.hunlinter.parsers.affix.AffixData affixData = affParser.getAffixData();
//if(dicEntry.getContinuationFlags() != null)
//	for(String flag : dicEntry.getContinuationFlags()){
//		io.github.mtrevisan.hunlinter.parsers.vos.RuleEntry rule = affixData.getData(flag);
//		if(rule == null)
//			continue;;
//		if(rule.getType() == io.github.mtrevisan.hunlinter.parsers.enums.AffixType.PREFIX)
//			prefix = true;
//		else if(prefix){
//			System.out.println("Suffix " + flag + " after prefix for " + indexData.getData());
//			break;
//		}
//	}

			Iterator<Inflection> itr = inflections.iterator();
			while(itr.hasNext()){
				final Inflection inflection = itr.next();
				itr.remove();

				final AffixEntry[] appliedRules = inflection.getAppliedRules();
				if(appliedRules != null)
					for(int j = 0; j < appliedRules.length; j ++){
						final AffixEntry appliedRule = appliedRules[j];
						usedFlags.add(appliedRule.getFlag());
//						usedFlags.computeIfAbsent(appliedRule.getFlag(), k -> new HashSet<>(1))
//							.add(appliedRule);
					}

				try{
					checker.checkInflection(inflection, indexData.getIndex());
				}
				catch(final RuntimeException re){
					final LinterException wrappedException = wrapException(re, inflection, indexData);
					manageException(wrappedException);

					//remove all inflections derived from this one
					final AffixEntry lastAppliedRule = inflection.getLastAppliedRule();
					if(lastAppliedRule != null)
						itr = removeDerivedInflections(lastAppliedRule.getFlag(), inflections);
				}
			}
		};

		final Function<Void, List<IndexDataPair<String>>> step1 = ignored -> {
			prepareProcessing(WORKER_NAME, "Execute " + workerData.getWorkerName());

			final Path dicPath = dicParser.getDicFile()
				.toPath();
			final Charset charset = dicParser.getCharset();
			processLines(WORKER_NAME, dicPath, charset, lineProcessor);

			finalizeProcessing(WORKER_NAME, "Successfully processed " + workerData.getWorkerName());

			final AffixData affixData = affParser.getAffixData();
			final Set<String> unusedFlags = affixData.getProductableFlags();
			unusedFlags.removeAll(usedFlags);
//			unusedFlags.removeAll(usedFlags.keySet());
			if(!unusedFlags.isEmpty())
				manageException(new LinterException(UNUSED_FLAGS, StringUtils.join(unusedFlags, ", "))
					.withIndexDataPair(IndexDataPair.NULL_INDEX_DATA_PAIR));
//			final StringBuilder originalRuleEntriesLog = new StringBuilder();
//			for(final Map.Entry<String, Set<AffixEntry>> flagRules : usedFlags.entrySet()){
//				final String flag = flagRules.getKey();
//				final Set<AffixEntry> rules = flagRules.getValue();
//				final RuleEntry originalRule = affixData.getData(flag);
//				if(originalRule != null && originalRule.getEntries().size() != rules.size()){
//					final Collection<AffixEntry> originalRuleEntries = new ArrayList<>(originalRule.getEntries());
//					originalRuleEntries.removeAll(rules);
//					originalRuleEntriesLog.setLength(0);
//					originalRuleEntriesLog.append(originalRuleEntries);
//					originalRuleEntriesLog.insert(1, "\r\n\t");
//					originalRuleEntriesLog.insert(originalRuleEntriesLog.length() - 1, "\r\n");
//					manageException(new LinterException(UNUSED_RULES, flag, StringUtils.replace(originalRuleEntriesLog.toString(), ", ", "\r\n\t"))
//						.withIndexDataPair(IndexDataPair.NULL_INDEX_DATA_PAIR));
//				}
//			}

			//FIXME
//			final Set<String> unusedUnproductableFlags = affixData.getUnproductableFlags();
//			unusedUnproductableFlags.removeAll(unproductableFlags);
//			if(!unusedUnproductableFlags.isEmpty())
//				manageException(new LinterException(UNUSED_FLAGS, StringUtils.join(unusedUnproductableFlags, ", "))
//					.withIndexDataPair(IndexDataPair.NULL_INDEX_DATA_PAIR));

			return null;
		};
		setProcessor(step1);
	}

	private static Iterator<Inflection> removeDerivedInflections(final String lastAppliedRuleFlag, final Iterable<Inflection> inflections){
		final Iterator<Inflection> itr = inflections.iterator();
		while(itr.hasNext())
			if(itr.next().hasAppliedRule(lastAppliedRuleFlag))
				itr.remove();
		return inflections.iterator();
	}

	private static LinterException wrapException(final Exception e, final Inflection inflection, final IndexDataPair<String> data){
		String message = e.getMessage();
		if(inflection.hasInflectionRules())
			message += " (via " + inflection.getRulesSequence() + ")";
		return new LinterException(e, message)
			.withData(data);
	}

}
