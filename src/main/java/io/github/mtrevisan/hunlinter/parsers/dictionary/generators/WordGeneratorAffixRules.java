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
import io.github.mtrevisan.hunlinter.parsers.vos.AffixEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.parsers.vos.RuleEntry;
import io.github.mtrevisan.hunlinter.services.system.LoopHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.github.mtrevisan.hunlinter.services.system.LoopHelper.forEach;
import static io.github.mtrevisan.hunlinter.services.system.LoopHelper.match;


class WordGeneratorAffixRules extends WordGeneratorBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordGeneratorAffixRules.class);


	WordGeneratorAffixRules(final AffixData affixData, final DictionaryCorrectnessChecker checker){
		super(affixData, checker);
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
			inflections = LoopHelper.removeIf(inflections, inflection -> {
				final boolean hasOnlyInCompoundFlag = inflection.hasContinuationFlag(onlyInCompoundFlag);
				final AffixEntry[] appliedRules = inflection.getAppliedRules();
				final boolean hasOnlyInCompoundFlagInAppliedRules = (match(appliedRules,
					appliedRule -> appliedRule.hasContinuationFlag(onlyInCompoundFlag)) != null);
				return (hasOnlyInCompoundFlag || hasOnlyInCompoundFlagInAppliedRules);
			});
		return inflections;
	}

}
