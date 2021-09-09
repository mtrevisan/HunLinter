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
import io.github.mtrevisan.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.services.regexgenerator.HunSpellRegexWordGenerator;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


class WordGeneratorCompoundBeginMiddleEnd extends WordGeneratorCompound{

	private static final String NON_POSITIVE_LIMIT = "Limit cannot be non-positive: {}";
	private static final String MISSING_WORD = "Missing word(s) for rule `{}` in compound begin-middle-end";


	WordGeneratorCompoundBeginMiddleEnd(final AffixData affixData, final DictionaryParser dicParser,
			final DictionaryCorrectnessChecker checker){
		super(affixData, dicParser, checker);
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
	final List<Inflection> applyCompoundBeginMiddleEnd(final String[] inputCompounds, final int limit){
		Objects.requireNonNull(inputCompounds, "Input compounds cannot be null");
		if(limit <= 0)
			throw new LinterException(NON_POSITIVE_LIMIT, limit);

		final String compoundBeginFlag = affixData.getCompoundBeginFlag();
		final String compoundMiddleFlag = affixData.getCompoundMiddleFlag();
		final String compoundEndFlag = affixData.getCompoundEndFlag();

		loadDictionaryForInclusionTest();

		//extract map flag -> dictionary entries
		final Map<String, List<DictionaryEntry>> inputs = extractCompoundBeginMiddleEnd(inputCompounds, compoundBeginFlag,
			compoundMiddleFlag, compoundEndFlag);

		checkCompoundBeginMiddleEndInputCorrectness(inputs);

		final String[] compoundRule = {
			compoundBeginFlag, FlagParsingStrategy.FLAG_OPTIONAL,
			compoundMiddleFlag, FlagParsingStrategy.FLAG_OPTIONAL,
			compoundEndFlag, FlagParsingStrategy.FLAG_OPTIONAL};
		final HunSpellRegexWordGenerator regexWordGenerator = new HunSpellRegexWordGenerator(compoundRule);
		//generate all the words that matches the given regex
		final List<List<String>> permutations = regexWordGenerator.generateAll(2, limit);

		final List<List<List<Inflection>>> entries = generateCompounds(permutations, inputs);

		return applyCompound(entries, limit);
	}

	private Map<String, List<DictionaryEntry>> extractCompoundBeginMiddleEnd(final String[] inputCompounds, final String compoundBeginFlag,
			final String compoundMiddleFlag, final String compoundEndFlag){
		final Integer compoundMinimumLength = affixData.getCompoundMinimumLength();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		//extract map flag -> compounds
		Map<String, List<DictionaryEntry>> compoundRules = new HashMap<>(0);
		for(final String inputCompound : inputCompounds){
			final DictionaryEntry dicEntry = dictionaryEntryFactory.createFromDictionaryLine(inputCompound);

			final List<Inflection> inflections = applyAffixRules(dicEntry, false, null);
			for(final Inflection inflection : inflections){
				final Map<String, List<DictionaryEntry>> distribution = inflection.distributeByCompoundBeginMiddleEnd(compoundBeginFlag,
					compoundMiddleFlag, compoundEndFlag);
				compoundRules = mergeDistributions(compoundRules, distribution, compoundMinimumLength, forbiddenWordFlag);
			}
		}
		return compoundRules;
	}

	private static void checkCompoundBeginMiddleEndInputCorrectness(final Map<String, List<DictionaryEntry>> inputs){
		for(final Map.Entry<String, List<DictionaryEntry>> entry : inputs.entrySet())
			if(entry.getValue().isEmpty())
				throw new LinterException(MISSING_WORD, entry.getKey());
	}

}
