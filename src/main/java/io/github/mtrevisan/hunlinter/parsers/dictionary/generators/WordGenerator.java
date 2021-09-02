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
package io.github.mtrevisan.hunlinter.parsers.dictionary.generators;

import io.github.mtrevisan.hunlinter.languages.DictionaryCorrectnessChecker;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntryFactory;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.parsers.vos.RuleEntry;

import java.util.List;


public class WordGenerator{

	public static final int BASE_INFLECTION_INDEX = 0;

	private final DictionaryEntryFactory dictionaryEntryFactory;
	private final WordGeneratorAffixRules wordGeneratorAffixRules;
	private final WordGeneratorCompoundRules wordGeneratorCompoundRules;
	private final WordGeneratorCompoundFlag wordGeneratorCompoundFlag;
	private final WordGeneratorCompoundBeginMiddleEnd wordGeneratorCompoundBeginMiddleEnd;


	public WordGenerator(final AffixData affixData, final DictionaryParser dicParser, final DictionaryCorrectnessChecker checker){
		dictionaryEntryFactory = new DictionaryEntryFactory(affixData);
		wordGeneratorAffixRules = new WordGeneratorAffixRules(affixData, checker);
		wordGeneratorCompoundRules = new WordGeneratorCompoundRules(affixData, dicParser, checker);
		wordGeneratorCompoundFlag = new WordGeneratorCompoundFlag(affixData, dicParser, checker);
		wordGeneratorCompoundBeginMiddleEnd = new WordGeneratorCompoundBeginMiddleEnd(affixData, dicParser, checker);
	}

	public DictionaryEntry createFromDictionaryLine(final String line){
		return dictionaryEntryFactory.createFromDictionaryLine(line);
	}

	public DictionaryEntry createFromDictionaryLineNoStemTag(final String line){
		return dictionaryEntryFactory.createFromDictionaryLineNoStemTag(line);
	}

	public List<Inflection> applyAffixRules(final DictionaryEntry dicEntry){
		return wordGeneratorAffixRules.applyAffixRules(dicEntry);
	}

	public List<Inflection> applyAffixRulesWithCompounds(final DictionaryEntry dicEntry){
		return wordGeneratorAffixRules.applyAffixRulesWithCompounds(dicEntry);
	}

	public List<Inflection> applyAffixRules(final DictionaryEntry dicEntry, final RuleEntry overriddenRule){
		return wordGeneratorAffixRules.applyAffixRules(dicEntry, overriddenRule);
	}

	public List<Inflection> applyCompoundRules(final String[] inputCompounds, final String compoundRule, final int limit){
		return wordGeneratorCompoundRules.applyCompoundRules(inputCompounds, compoundRule, limit);
	}

	public List<Inflection> applyCompoundFlag(final String[] inputCompounds, final int limit, final int maxCompounds){
		return wordGeneratorCompoundFlag.applyCompoundFlag(inputCompounds, limit, maxCompounds);
	}

	public List<Inflection> applyCompoundBeginMiddleEnd(final String[] inputCompounds, final int limit){
		return wordGeneratorCompoundBeginMiddleEnd.applyCompoundBeginMiddleEnd(inputCompounds, limit);
	}

}
