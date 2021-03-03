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

import unit731.hunlinter.datastructures.SimpleDynamicArray;
import unit731.hunlinter.languages.DictionaryCorrectnessChecker;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.services.text.PermutationsWithRepetitions;
import unit731.hunlinter.workers.exceptions.LinterException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;


class WordGeneratorCompoundFlag extends WordGeneratorCompound{

	private static final MessageFormat NON_POSITIVE_LIMIT = new MessageFormat("Limit cannot be non-positive: was {0}");
	private static final MessageFormat NON_POSITIVE_MAX_COMPOUNDS = new MessageFormat("Max compounds cannot be non-positive: was {0}");


	WordGeneratorCompoundFlag(final AffixData affixData, final DictionaryParser dicParser, final WordGenerator wordGenerator,
			final DictionaryCorrectnessChecker checker){
		super(affixData, dicParser, wordGenerator, checker);
	}

	/**
	 * Generates a list of stems for the provided flag from words in the dictionary marked with AffixOption.COMPOUND_FLAG
	 *
	 * @param inputCompounds	List of compounds used to generate the inflection through the compound rule
	 * @param limit	Limit result count
	 * @param maxCompounds	Maximum compound count
	 * @return	The list of inflections
	 * @throws NoApplicableRuleException	If there are no rules that apply to the word
	 */
	Inflection[] applyCompoundFlag(final String[] inputCompounds, final int limit, final int maxCompounds){
		Objects.requireNonNull(inputCompounds, "Input compounds cannot be null");
		if(limit <= 0)
			throw new LinterException(NON_POSITIVE_LIMIT.format(new Object[]{limit}));
		if(maxCompounds <= 0 && maxCompounds != PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY)
			throw new LinterException(NON_POSITIVE_MAX_COMPOUNDS.format(new Object[]{maxCompounds}));

		final boolean forbidDuplicates = affixData.isForbidDuplicatesInCompound();

		loadDictionaryForInclusionTest();

		//extract list of dictionary entries
		final DictionaryEntry[] inputs = extractCompoundFlags(inputCompounds);

		//check if it's possible to compound some words
		if(inputs.length == 0)
			return new Inflection[0];

		final PermutationsWithRepetitions perm = new PermutationsWithRepetitions(inputs.length, maxCompounds, forbidDuplicates);
		final List<int[]> permutations = perm.permutations(limit);

		final List<List<Inflection[]>> entries = generateCompounds(permutations, inputs);

		return applyCompound(entries, limit);
	}

	private DictionaryEntry[] extractCompoundFlags(final String[] inputCompounds){
		final int compoundMinimumLength = affixData.getCompoundMinimumLength();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		final SimpleDynamicArray<DictionaryEntry> result = new SimpleDynamicArray<>(DictionaryEntry.class, inputCompounds.length);
		for(final String inputCompound : inputCompounds){
			final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(inputCompound, affixData);

			//filter input set by minimum length and forbidden flag
			if(dicEntry.getWord().length() >= compoundMinimumLength && !dicEntry.hasContinuationFlag(forbiddenWordFlag))
				result.add(dicEntry);
		}
		return result.extractCopy();
	}

	private List<List<Inflection[]>> generateCompounds(final Iterable<int[]> permutations, final DictionaryEntry[] inputs){
		final Map<Integer, Inflection[]> dicEntries = new HashMap<>();
		final List<List<Inflection[]>> list = new ArrayList<>();
		for(final int[] permutation : permutations){
			final List<Inflection[]> inflections = generateCompound(permutation, dicEntries, inputs);
			if(inflections != null)
				list.add(inflections);
		}
		return list;
	}

	private List<Inflection[]> generateCompound(final int[] permutation, final Map<Integer, Inflection[]> dicEntries,
			final DictionaryEntry[] inputs){
		final List<Inflection[]> expandedPermutationEntries = new ArrayList<>();
		final Function<Integer, Inflection[]> integerFunction = idx -> applyAffixRules(inputs[idx], true, null);
		for(final int index : permutation){
			final Inflection[] list = dicEntries.computeIfAbsent(index, integerFunction);
			if(list.length > 0)
				expandedPermutationEntries.add(list);
		}
		return (!expandedPermutationEntries.isEmpty()? expandedPermutationEntries: null);
	}

}
