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
package unit731.hunlinter.workers.dictionary;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.languages.DictionaryCorrectnessChecker;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.affix.AffixParser;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import unit731.hunlinter.workers.exceptions.LinterException;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

	private static final MessageFormat UNUSED_FLAGS = new MessageFormat("Unused flags: {0}");


	public DictionaryLinterWorker(final ParserManager parserManager){
		this(parserManager.getAffParser(), parserManager.getDicParser(), parserManager.getChecker(), parserManager.getWordGenerator());
	}

	public DictionaryLinterWorker(final AffixParser affParser, final DictionaryParser dicParser, final DictionaryCorrectnessChecker checker,
			final WordGenerator wordGenerator){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withParallelProcessing();

		Objects.requireNonNull(checker);
		Objects.requireNonNull(wordGenerator);

		//collectors of flags
		final Set<String> flags = ConcurrentHashMap.newKeySet();

		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			checker.checkCircumfix(dicEntry);
			final Collection<Inflection> inflections = new ArrayList<>(Arrays.asList(wordGenerator.applyAffixRules(dicEntry)));
//prefixes after suffixes
//boolean prefix = false;
//unit731.hunlinter.parsers.affix.AffixData affixData = affParser.getAffixData();
//if(dicEntry.getContinuationFlags() != null)
//	for(String flag : dicEntry.getContinuationFlags()){
//		unit731.hunlinter.parsers.vos.RuleEntry rule = affixData.getData(flag);
//		if(rule == null)
//			continue;;
//		if(rule.getType() == unit731.hunlinter.parsers.enums.AffixType.PREFIX)
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
				if(inflection.getContinuationFlags() != null)
					flags.addAll(Arrays.asList(inflection.getContinuationFlags()));

				try{
					checker.checkInflection(inflection, indexData.getIndex());
				}
				catch(final Exception e){
					final LinterException wrappedException = wrapException(e, inflection, indexData);
					manageException(wrappedException);

					//remove all inflections derived from this one
					final AffixEntry lastAppliedRule = inflection.getLastAppliedRule();
					if(lastAppliedRule != null)
						itr = removeDerivedInflections(lastAppliedRule.getFlag(), inflections);
				}
			}
		};

		final Function<Void, List<IndexDataPair<String>>> step1 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName());

			final Path dicPath = dicParser.getDicFile().toPath();
			final Charset charset = dicParser.getCharset();
			processLines(dicPath, charset, lineProcessor);

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			final Set<String> unusedFlags = affParser.getAffixData().getProductableFlag();
			unusedFlags.removeAll(flags);
			if(!unusedFlags.isEmpty())
				manageException(new LinterException(
					UNUSED_FLAGS.format(new Object[]{StringUtils.join(unusedFlags, ", ")}),
					IndexDataPair.NULL_INDEX_DATA_PAIR));

			return null;
		};
		setProcessor(step1);
	}

	private Iterator<Inflection> removeDerivedInflections(final String lastAppliedRuleFlag, final Collection<Inflection> inflections){
		inflections.removeIf(inflection -> inflection.hasAppliedRule(lastAppliedRuleFlag));
		return inflections.iterator();
	}

	private LinterException wrapException(final Exception e, final Inflection inflection, final IndexDataPair<String> data){
		String message = e.getMessage();
		if(inflection.hasInflectionRules())
			message += " (via " + inflection.getRulesSequence() + ")";
		return new LinterException(message, data);
	}

}
