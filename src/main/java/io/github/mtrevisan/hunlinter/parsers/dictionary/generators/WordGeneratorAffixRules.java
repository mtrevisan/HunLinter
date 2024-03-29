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

import io.github.mtrevisan.hunlinter.languages.DictionaryCorrectnessChecker;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.vos.AffixEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.parsers.vos.RuleEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;


public class WordGeneratorAffixRules extends WordGeneratorBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordGeneratorAffixRules.class);


	public WordGeneratorAffixRules(final AffixData affixData, final DictionaryCorrectnessChecker checker){
		super(affixData, checker);
	}

	public final List<Inflection> applyAffixRules(final DictionaryEntry dicEntry){
		return applyAffixRules(dicEntry, null);
	}

	final List<Inflection> applyAffixRulesWithCompounds(final DictionaryEntry dicEntry){
		return applyAffixRules(dicEntry, null, false);
	}

	final List<Inflection> applyAffixRules(final DictionaryEntry dicEntry, final RuleEntry overriddenRule){
		return applyAffixRules(dicEntry, overriddenRule, true);
	}

	private List<Inflection> applyAffixRules(final DictionaryEntry dicEntry, final RuleEntry overriddenRule,
			final boolean enforceOnlyInCompound){
		final List<Inflection> inflections = applyAffixRules(dicEntry, false, overriddenRule);

		if(enforceOnlyInCompound)
			enforceOnlyInCompound(inflections);

		//convert using output table
		for(int i = 0; i < inflections.size(); i ++)
			inflections.get(i).applyOutputConversionTable(affixData::applyOutputConversionTable);

		if(LOGGER.isTraceEnabled())
			for(int i = 0; i < inflections.size(); i ++)
				LOGGER.trace("Inflected word: {}", inflections.get(i));

		return inflections;
	}

	/** Remove rules that invalidate the onlyInCompound rule. */
	private void enforceOnlyInCompound(final Iterable<Inflection> inflections){
		final String onlyInCompoundFlag = affixData.getOnlyInCompoundFlag();
		if(onlyInCompoundFlag != null){
			final Iterator<Inflection> itr = inflections.iterator();
			while(itr.hasNext()){
				final Inflection inflection = itr.next();
				final boolean hasOnlyInCompoundFlag = inflection.hasContinuationFlag(onlyInCompoundFlag);
				final AffixEntry[] appliedRules = inflection.getAppliedRules();
				final boolean hasOnlyInCompoundFlagInAppliedRules = (match(appliedRules, onlyInCompoundFlag) != null);
				if(hasOnlyInCompoundFlag || hasOnlyInCompoundFlagInAppliedRules)
					itr.remove();
			}
		}
	}

	private static AffixEntry match(final AffixEntry[] array, final String onlyInCompoundFlag){
		final int size = (array != null? array.length: 0);
		for(int i = 0; i < size; i ++){
			final AffixEntry appliedRule = array[i];
			if(appliedRule.hasContinuationFlag(onlyInCompoundFlag))
				return appliedRule;
		}
		return null;
	}

}
