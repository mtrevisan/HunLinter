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
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


class WordGeneratorCompoundRules extends WordGeneratorCompound{

	private static final MessageFormat NON_POSITIVE_LIMIT = new MessageFormat("Limit cannot be non-positive: {0}");
	private static final MessageFormat MISSING_WORD = new MessageFormat("Missing word(s) for rule {0} in compound rule {1}");


	WordGeneratorCompoundRules(final AffixData affixData, final DictionaryParser dicParser, final WordGenerator wordGenerator,
			final DictionaryCorrectnessChecker checker){
		super(affixData, dicParser, wordGenerator, checker);
	}

	/**
	 * Generates a list of stems for the provided rule from words in the dictionary marked with AffixOption.COMPOUND_RULE
	 *
	 * @param inputCompounds	List of compounds used to generate the inflection through the compound rule
	 * @param compoundRule	Rule used to generate the inflections for
	 * @param limit	Limit result count
	 * @return	The list of inflections for the given rule
	 * @throws NoApplicableRuleException	If there is a rule that doesn't apply to the word
	 */
	Inflection[] applyCompoundRules(final String[] inputCompounds, final String compoundRule, final int limit){
		Objects.requireNonNull(inputCompounds, "Input compounds cannot be null");
		Objects.requireNonNull(compoundRule, "Compound rule cannot be null");
		if(limit <= 0)
			throw new LinterException(NON_POSITIVE_LIMIT.format(new Object[]{limit}));

		final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();

		loadDictionaryForInclusionTest();

		//extract map flag -> dictionary entries
		final Map<String, DictionaryEntry[]> inputs = extractCompoundRules(inputCompounds);

		final String[] compoundRuleComponents = strategy.extractCompoundRule(compoundRule);

		checkCompoundRuleInputCorrectness(inputs, compoundRuleComponents);

		final HunSpellRegexWordGenerator regexWordGenerator = new HunSpellRegexWordGenerator(compoundRuleComponents);
		//generate all the words that matches the given regex
		final List<List<String>> permutations = regexWordGenerator.generateAll(2, limit);

		final List<List<Inflection[]>> entries = generateCompounds(permutations, inputs);

		return applyCompound(entries, limit);
	}

	/** Extract a map of flag > dictionary entry from input compounds */
	private Map<String, DictionaryEntry[]> extractCompoundRules(final String[] inputCompounds){
		final int compoundMinimumLength = affixData.getCompoundMinimumLength();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		//extract map flag -> compounds
		Map<String, DictionaryEntry[]> compoundRules = new HashMap<>();
		for(final String inputCompound : inputCompounds){
			final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(inputCompound, affixData);
			final Map<String, DictionaryEntry[]> distribution = dicEntry.distributeByCompoundRule(affixData);
			compoundRules = mergeDistributions(compoundRules, distribution, compoundMinimumLength, forbiddenWordFlag);
		}
		return compoundRules;
	}

	private void checkCompoundRuleInputCorrectness(final Map<String, DictionaryEntry[]> inputs,
			final String[] compoundRuleComponents){
		for(final String component : compoundRuleComponents)
			if(raiseError(inputs, component))
				throw new LinterException(MISSING_WORD.format(new Object[]{component,
					StringUtils.join(compoundRuleComponents, StringUtils.EMPTY)}));
	}

	private boolean raiseError(final Map<String, DictionaryEntry[]> inputs, final String component){
		final char chr = (component.length() == 1? component.charAt(0): 0);
		return (chr != '*' && chr != '?' && inputs.get(component) == null);
	}

}
