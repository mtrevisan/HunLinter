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
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.services.text.PermutationsWithRepetitions;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;


class WordGeneratorCompoundFlag extends WordGeneratorCompound{

	private static final String NON_POSITIVE_LIMIT = "Limit cannot be non-positive: {}";
	private static final String NON_POSITIVE_MAX_COMPOUNDS = "Max compounds cannot be non-positive: {}";


	WordGeneratorCompoundFlag(final AffixData affixData, final DictionaryParser dicParser, final DictionaryCorrectnessChecker checker){
		super(affixData, dicParser, checker);
	}

	/**
	 * Generates a list of stems for the provided flag from words in the dictionary marked with {@code AffixOption.COMPOUND_FLAG}.
	 *
	 * @param inputCompounds	List of compounds used to generate the inflection through the compound rule.
	 * @param limit	Limit result count.
	 * @param maxCompounds	Maximum compound count.
	 * @return	The list of inflections.
	 * @throws NoApplicableRuleException	If there are no rules that apply to the word.
	 */
	final List<Inflection> applyCompoundFlag(final String[] inputCompounds, final int limit, final Integer maxCompounds){
		Objects.requireNonNull(inputCompounds, "Input compounds cannot be null");
		if(limit <= 0)
			throw new LinterException(NON_POSITIVE_LIMIT, limit);
		if(maxCompounds == null || maxCompounds <= 0 && maxCompounds != PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY)
			throw new LinterException(NON_POSITIVE_MAX_COMPOUNDS, maxCompounds);

		final boolean forbidDuplicates = affixData.isForbidDuplicatesInCompound();

		loadDictionaryForInclusionTest();

		//extract list of dictionary entries
		final List<DictionaryEntry> inputs = extractCompoundFlags(inputCompounds);

		//check if it's possible to compound some words
		if(inputs.isEmpty())
			return Collections.emptyList();

		final PermutationsWithRepetitions perm = new PermutationsWithRepetitions(inputs.size(), maxCompounds, forbidDuplicates);
		final List<int[]> permutations = perm.permutations(limit);

		final List<List<List<Inflection>>> entries = generateCompounds(permutations, inputs);

		return applyCompound(entries, limit);
	}

	private List<DictionaryEntry> extractCompoundFlags(final String[] inputCompounds){
		final Integer compoundMinimumLength = affixData.getCompoundMinimumLength();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		final List<DictionaryEntry> result = new ArrayList<>(inputCompounds.length);
		for(final String inputCompound : inputCompounds){
			final DictionaryEntry dicEntry = dictionaryEntryFactory.createFromDictionaryLine(inputCompound);

			//filter input set by minimum length and forbidden flag
			if(compoundMinimumLength != null && dicEntry.getWord().length() >= compoundMinimumLength
					&& !dicEntry.hasContinuationFlag(forbiddenWordFlag))
				result.add(dicEntry);
		}
		return result;
	}

	private List<List<List<Inflection>>> generateCompounds(final List<int[]> permutations, final List<DictionaryEntry> inputs){
		final Map<Integer, List<Inflection>> dicEntries = new HashMap<>(0);
		final List<List<List<Inflection>>> list = new ArrayList<>(permutations.size());
		for(int i = 0; i < permutations.size(); i ++){
			final List<List<Inflection>> inflections = generateCompound(permutations.get(i), dicEntries, inputs);
			if(inflections != null)
				list.add(inflections);
		}
		return list;
	}

	private List<List<Inflection>> generateCompound(final int[] permutation, final Map<Integer, List<Inflection>> dicEntries,
			final List<DictionaryEntry> inputs){
		final List<List<Inflection>> expandedPermutationEntries = new ArrayList<>(permutation.length);
		final Function<Integer, List<Inflection>> integerFunction = idx -> applyAffixRules(inputs.get(idx), true, null);
		for(final int index : permutation){
			final List<Inflection> list = dicEntries.computeIfAbsent(index, integerFunction);
			if(!list.isEmpty())
				expandedPermutationEntries.add(list);
		}
		return (!expandedPermutationEntries.isEmpty()? expandedPermutationEntries: null);
	}

}
