package unit731.hunspeller.parsers.dictionary.generators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.vos.DictionaryEntry;
import unit731.hunspeller.parsers.vos.Production;
import unit731.hunspeller.services.PermutationsWithRepetitions;


class WordGeneratorCompoundFlag extends WordGeneratorCompound{

	WordGeneratorCompoundFlag(final AffixData affixData, final DictionaryParser dicParser, final WordGenerator wordGenerator){
		super(affixData, dicParser, wordGenerator);
	}

	/**
	 * Generates a list of stems for the provided flag from words in the dictionary marked with AffixOption.COMPOUND_FLAG
	 * 
	 * @param inputCompounds	List of compounds used to generate the production through the compound rule
	 * @param limit	Limit result count
	 * @param maxCompounds	Maximum compound count
	 * @return	The list of productions
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	List<Production> applyCompoundFlag(final String[] inputCompounds, final int limit, final int maxCompounds) throws IllegalArgumentException{
		Objects.requireNonNull(inputCompounds);
		if(limit <= 0)
			throw new IllegalArgumentException("Limit cannot be non-positive");
		if(maxCompounds <= 0 && maxCompounds != PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY)
			throw new IllegalArgumentException("Max compounds cannot be non-positive");

		final boolean forbidDuplicates = affixData.isForbidDuplicatesInCompound();

		loadDictionaryForInclusionTest();

		//extract list of dictionary entries
		final List<DictionaryEntry> inputs = extractCompoundFlags(inputCompounds);

		//check if it's possible to compound some words
		if(inputs.isEmpty())
			return Collections.emptyList();

		final PermutationsWithRepetitions perm = new PermutationsWithRepetitions(inputs.size(), maxCompounds, forbidDuplicates);
		final List<int[]> permutations = perm.permutations(limit);

		final List<List<List<Production>>> entries = generateCompounds(permutations, inputs);

		return applyCompound(entries, limit);
	}

	private List<DictionaryEntry> extractCompoundFlags(final String[] inputCompounds){
		final int compoundMinimumLength = affixData.getCompoundMinimumLength();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		final List<DictionaryEntry> result = new ArrayList<>();
		for(final String inputCompound : inputCompounds){
			final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(inputCompound, affixData);

			//filter input set by minimum length and forbidden flag
			if(dicEntry.getWord().length() >= compoundMinimumLength && !dicEntry.hasContinuationFlag(forbiddenWordFlag))
				result.add(dicEntry);
		}
		return result;
	}

	private List<List<List<Production>>> generateCompounds(final List<int[]> permutations, final List<DictionaryEntry> inputs){
		final List<List<List<Production>>> entries = new ArrayList<>();
		final Map<Integer, List<Production>> dicEntries = new HashMap<>();
		for(final int[] permutation : permutations){
			final List<List<Production>> compound = generateCompound(permutation, dicEntries, inputs);
			if(compound != null)
				entries.add(compound);
		}
		return entries;
	}

	private List<List<Production>> generateCompound(final int[] permutation, final Map<Integer, List<Production>> dicEntries,
			final List<DictionaryEntry> inputs) throws IllegalArgumentException{
		final List<List<Production>> expandedPermutationEntries = new ArrayList<>();
		for(final int index : permutation){
			if(!dicEntries.containsKey(index)){
				final DictionaryEntry input = inputs.get(index);
				dicEntries.put(index, applyAffixRules(input, true, null));
			}
			final List<Production> de = dicEntries.get(index);
			if(!de.isEmpty())
				expandedPermutationEntries.add(de);
		}
		return (!expandedPermutationEntries.isEmpty()? expandedPermutationEntries: null);
	}

}
