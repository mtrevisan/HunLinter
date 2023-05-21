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
package io.github.mtrevisan.hunlinter.parsers.dictionary.generators;

import io.github.mtrevisan.hunlinter.datastructures.SetHelper;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.vos.AffixEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntryFactory;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.parsers.vos.RuleEntry;
import io.github.mtrevisan.hunlinter.workers.dictionary.DictionaryInclusionTestWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;


//https://github.com/nuspell/nuspell/blob/45d383c0e2f25e4ea48ee8efeca53c2bb51a3510/src/tools/munch.cxx
//https://github.com/nuspell/nuspell/blob/45d383c0e2f25e4ea48ee8efeca53c2bb51a3510/src/tools/munch.h
public class WordMuncher{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordMuncher.class);

	private static final String SLASH = "/";


	private final AffixData affixData;
	protected final DictionaryEntryFactory dictionaryEntryFactory;
//	private final DictionaryParser dicParser;
	private final WordGenerator wordGenerator;

	private DictionaryInclusionTestWorker dicInclusionTestWorker;


	public WordMuncher(final AffixData affixData, final DictionaryParser dicParser, final WordGenerator wordGenerator){
		Objects.requireNonNull(affixData, "Affix data cannot be null");
//		Objects.requireNonNull(dicParser, "Dictionary parser cannot be null");

		dictionaryEntryFactory = new DictionaryEntryFactory(affixData);
		this.affixData = affixData;
//		this.dicParser = dicParser;
		this.wordGenerator = wordGenerator;

		loadDictionaryForInclusionTest(dicParser);
	}

	private void loadDictionaryForInclusionTest(final DictionaryParser dicParser){
		if(dicInclusionTestWorker == null){
			dicInclusionTestWorker = new DictionaryInclusionTestWorker(affixData, dicParser);

			dicInclusionTestWorker.executeSynchronously();
		}
	}

	public final List<DictionaryEntry> inferAffixRules(final DictionaryEntry dicEntry){
		final List<DictionaryEntry> originators = extractAllAffixes(dicEntry);
		originators.add(0, dicEntry);

		originators.removeIf(originator -> !dicInclusionTestWorker.isInDictionary(originator.getWord()));

		//TODO

		if(LOGGER.isTraceEnabled())
			for(int i = 0; i < originators.size(); i ++)
				LOGGER.trace("Inferred inflection: {}", originators.get(i));
		return originators;
	}

	private List<DictionaryEntry> extractAllAffixes(final DictionaryEntry dicEntry){
		final String word = dicEntry.getWord();
		final List<String> partOfSpeech = dicEntry.getMorphologicalFieldPartOfSpeech();

		final List<DictionaryEntry> originators = new ArrayList<>(0);
		final List<RuleEntry> ruleEntries = affixData.getRuleEntries();
		//for each rule
		for(int i = 0; i < ruleEntries.size(); i ++){
			final RuleEntry ruleEntry = ruleEntries.get(i);
			//for each affix entry in rule
			final List<AffixEntry> affixEntries = ruleEntry.getEntries();
			for(int j = 0; j < affixEntries.size(); j ++){
				final AffixEntry affixEntry = affixEntries.get(j);
				if(affixEntry.canInverseApplyTo(word)){
					final String originatingWord = affixEntry.undoRule(word);
					final DictionaryEntry originatorEntry = wordGenerator.createFromDictionaryLineNoStemTag(originatingWord + SLASH + affixEntry.getFlag());

					final List<Inflection> inflections = wordGenerator.applyAffixRules(originatorEntry, ruleEntry);
					//remove base inflection
					inflections.remove(WordGenerator.BASE_INFLECTION_INDEX);

					//FIXME consider also the cases where a word can be attached to multiple derivations from an originating word
					if(inflections.size() != 1)
						continue;

					final List<String> baseInflectionPartOfSpeech = inflections.get(0).getMorphologicalFieldPartOfSpeech();
					if(baseInflectionPartOfSpeech.isEmpty() && partOfSpeech.isEmpty() || baseInflectionPartOfSpeech.equals(partOfSpeech))
						originators.add(originatorEntry);
				}
			}
		}
		return originators;
	}

	private List<Inflection> extractAllAffixes(final String word, final String partOfSpeech){
		final List<Inflection> originatingRules = new ArrayList<>(0);
		final DictionaryEntry nullDicEntry = dictionaryEntryFactory.createFromDictionaryLine(word);
		final List<RuleEntry> ruleEntries = affixData.getRuleEntries();
		final Collection<Inflection> originatingRulesFromEntry = new ArrayList<>(0);
		for(int i = 0; i < ruleEntries.size(); i ++){
			final RuleEntry ruleEntry = ruleEntries.get(i);
			originatingRulesFromEntry.clear();
			final List<AffixEntry> affixEntries = ruleEntry.getEntries();
			for(int j = 0; j < affixEntries.size(); j ++){
				final AffixEntry affixEntry = affixEntries.get(j);
				if(!affixEntry.hasContinuationFlags() && affixEntry.canInverseApplyTo(word)){
					final String originatingWord = affixEntry.undoRule(word);
					final Inflection originatingRule = Inflection.createFromInflection(originatingWord, affixEntry, ruleEntry.isCombinable());
					if(partOfSpeech.isEmpty() || !originatingRule.hasPartOfSpeech() || originatingRule.hasPartOfSpeech(partOfSpeech))
						originatingRulesFromEntry.add(originatingRule);
				}
			}
			if(!originatingRulesFromEntry.isEmpty()){
				//originatingRulesFromEntry should not have inflections from identical word
				final Map<String, List<Inflection>> wordBucket = SetHelper.bucket(originatingRulesFromEntry, DictionaryEntry::getWord);
				boolean identicalOriginatingWord = false;
				for(final List<Inflection> prods : wordBucket.values())
					if(prods.size() > 1){
						identicalOriginatingWord = true;
						break;
					}
				if(!identicalOriginatingWord)
					originatingRules.addAll(originatingRulesFromEntry);
			}
		}
		return originatingRules;
	}

}
