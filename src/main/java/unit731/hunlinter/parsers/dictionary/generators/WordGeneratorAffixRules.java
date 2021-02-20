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
package unit731.hunlinter.parsers.dictionary.generators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.parsers.vos.RuleEntry;

import static unit731.hunlinter.services.system.LoopHelper.forEach;
import static unit731.hunlinter.services.system.LoopHelper.match;
import static unit731.hunlinter.services.system.LoopHelper.removeIf;


class WordGeneratorAffixRules extends WordGeneratorBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordGeneratorAffixRules.class);


	WordGeneratorAffixRules(final AffixData affixData){
		super(affixData);
	}

	Inflection[] applyAffixRules(final DictionaryEntry dicEntry){
		return applyAffixRules(dicEntry, null);
	}

	Inflection[] applyAffixRules(final DictionaryEntry dicEntry, final RuleEntry overriddenRule){
		Inflection[] inflections = applyAffixRules(dicEntry, false, overriddenRule);

		inflections = enforceOnlyInCompound(inflections);

		//convert using output table
		forEach(inflections,
			inflection -> inflection.applyOutputConversionTable(affixData::applyOutputConversionTable));

		if(LOGGER.isTraceEnabled())
			forEach(inflections, inflection -> LOGGER.trace("Inflected word: {}", inflection));

		return inflections;
	}

	/** Remove rules that invalidate the onlyInCompound rule */
	private Inflection[] enforceOnlyInCompound(Inflection[] inflections){
		final String onlyInCompoundFlag = affixData.getOnlyInCompoundFlag();
		if(onlyInCompoundFlag != null)
			inflections = removeIf(inflections, inflection -> {
				final boolean hasOnlyInCompoundFlag = inflection.hasContinuationFlag(onlyInCompoundFlag);
				final AffixEntry[] appliedRules = inflection.getAppliedRules();
				final boolean hasOnlyInCompoundFlagInAppliedRules = (match(appliedRules,
					appliedRule -> appliedRule.hasContinuationFlag(onlyInCompoundFlag)) != null);
				return (hasOnlyInCompoundFlag || hasOnlyInCompoundFlagInAppliedRules);
			});
		return inflections;
	}

}
