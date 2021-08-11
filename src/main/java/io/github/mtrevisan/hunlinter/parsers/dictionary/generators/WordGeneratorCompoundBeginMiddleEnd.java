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
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.services.regexgenerator.HunSpellRegexWordGenerator;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


class WordGeneratorCompoundBeginMiddleEnd extends WordGeneratorCompound{

	private static final MessageFormat NON_POSITIVE_LIMIT = new MessageFormat("Limit cannot be non-positive: was {0}");
	private static final MessageFormat MISSING_WORD = new MessageFormat("Missing word(s) for rule `{0}` in compound begin-middle-end");


	WordGeneratorCompoundBeginMiddleEnd(final AffixData affixData, final DictionaryParser dicParser, final WordGenerator wordGenerator,
			final DictionaryCorrectnessChecker checker){
		super(affixData, dicParser, wordGenerator, checker);
	}

	/**
	 * Generates a list of stems for the provided flag from words in the dictionary marked with AffixOption.COMPOUND_BEGIN, AffixOption.COMPOUND_MIDDLE,
	 * and AffixOption.COMPOUND_END
	 *
	 * @param inputCompounds	List of compounds used to generate the inflection through the compound rule
	 * @param limit	Limit result count
	 * @return	The list of inflections
	 * @throws NoApplicableRuleException	If there is a rule that doesn't apply to the word
	 */
	Inflection[] applyCompoundBeginMiddleEnd(final String[] inputCompounds, final int limit){
		Objects.requireNonNull(inputCompounds, "Input compounds cannot be null");
		if(limit <= 0)
			throw new LinterException(NON_POSITIVE_LIMIT.format(new Object[]{limit}));

		final String compoundBeginFlag = affixData.getCompoundBeginFlag();
		final String compoundMiddleFlag = affixData.getCompoundMiddleFlag();
		final String compoundEndFlag = affixData.getCompoundEndFlag();

		loadDictionaryForInclusionTest();

		//extract map flag -> dictionary entries
		final Map<String, DictionaryEntry[]> inputs = extractCompoundBeginMiddleEnd(inputCompounds, compoundBeginFlag,
			compoundMiddleFlag, compoundEndFlag);

		checkCompoundBeginMiddleEndInputCorrectness(inputs);

		final String[] compoundRule = {compoundBeginFlag, "?", compoundMiddleFlag, "?", compoundEndFlag, "?"};
		final HunSpellRegexWordGenerator regexWordGenerator = new HunSpellRegexWordGenerator(compoundRule);
		//generate all the words that matches the given regex
		final List<List<String>> permutations = regexWordGenerator.generateAll(2, limit);

		final List<List<Inflection[]>> entries = generateCompounds(permutations, inputs);

		return applyCompound(entries, limit);
	}

	private Map<String, DictionaryEntry[]> extractCompoundBeginMiddleEnd(final String[] inputCompounds, final String compoundBeginFlag,
			final String compoundMiddleFlag, final String compoundEndFlag){
		final int compoundMinimumLength = affixData.getCompoundMinimumLength();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		//extract map flag -> compounds
		Map<String, DictionaryEntry[]> compoundRules = new HashMap<>();
		for(final String inputCompound : inputCompounds){
			final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(inputCompound, affixData);

			final Inflection[] inflections = applyAffixRules(dicEntry, false, null);
			for(final Inflection inflection : inflections){
				final Map<String, DictionaryEntry[]> distribution = inflection.distributeByCompoundBeginMiddleEnd(compoundBeginFlag,
					compoundMiddleFlag, compoundEndFlag);
				compoundRules = mergeDistributions(compoundRules, distribution, compoundMinimumLength, forbiddenWordFlag);
			}
		}
		return compoundRules;
	}

	private void checkCompoundBeginMiddleEndInputCorrectness(final Map<String, DictionaryEntry[]> inputs){
		for(final Map.Entry<String, DictionaryEntry[]> entry : inputs.entrySet())
			if(entry.getValue().length == 0)
				throw new LinterException(MISSING_WORD.format(new Object[]{entry.getKey()}));
	}

}
