package unit731.hunspeller.parsers.dictionary.generators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import unit731.hunspeller.languages.DictionaryBaseData;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.NoApplicableRuleException;
import unit731.hunspeller.parsers.dictionary.vos.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.services.PermutationsWithRepetitions;


class WordGeneratorCompoundFlag extends WordGeneratorCompound{

	WordGeneratorCompoundFlag(AffixParser affParser, DictionaryParser dicParser, DictionaryBaseData dictionaryBaseData,
			WordGenerator wordGenerator){
		super(affParser, dicParser, dictionaryBaseData, wordGenerator);
	}

	/**
	 * Generates a list of stems for the provided flag from words in the dictionary marked with AffixTag.COMPOUND_FLAG
	 * 
	 * @param inputCompounds	List of compounds used to generate the production through the compound rule
	 * @param limit	Limit result count
	 * @param maxCompounds	Maximum compound count
	 * @return	The list of productions
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	List<Production> applyCompoundFlag(String[] inputCompounds, int limit, int maxCompounds) throws IllegalArgumentException,
			NoApplicableRuleException{
		Objects.requireNonNull(inputCompounds);
		if(limit <= 0)
			throw new IllegalArgumentException("Limit cannot be non-positive");
		if(maxCompounds <= 0 && maxCompounds != PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY)
			throw new IllegalArgumentException("Max compounds cannot be non-positive");

		boolean forbidDuplications = affixData.isForbidDuplicationsInCompound();

		loadDictionaryForInclusionTest();

		//extract list of dictionary entries
		List<DictionaryEntry> inputs = extractCompoundFlags(inputCompounds);

		//check if it's possible to compound some words
		if(inputs.isEmpty())
			return Collections.<Production>emptyList();

		PermutationsWithRepetitions perm = new PermutationsWithRepetitions(inputs.size(), maxCompounds, forbidDuplications);
		List<int[]> permutations = perm.permutations(limit);

		List<List<List<Production>>> entries = generateCompounds(permutations, inputs);

		return applyCompound(entries, limit);
	}

	private List<DictionaryEntry> extractCompoundFlags(String[] inputCompounds){
		int compoundMinimumLength = affixData.getCompoundMinimumLength();
		String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		List<DictionaryEntry> result = new ArrayList<>();
		for(String inputCompound : inputCompounds){
			DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(inputCompound, strategy);
			dicEntry.applyInputConversionTable(affixData);

			//filter input set by minimum length and forbidden flag
			if(dicEntry.getWord().length() >= compoundMinimumLength && !dicEntry.hasContinuationFlag(forbiddenWordFlag))
				result.add(dicEntry);
		}
		return result;
	}

	private List<List<List<Production>>> generateCompounds(List<int[]> permutations, List<DictionaryEntry> inputs){
		List<List<List<Production>>> entries = new ArrayList<>();
		Map<Integer, List<Production>> dicEntries = new HashMap<>();
		for(int[] permutation : permutations){
			//expand permutation
			List<List<Production>> expandedPermutationEntries = new ArrayList<>();
			for(int index : permutation){
				if(!dicEntries.containsKey(index)){
					DictionaryEntry input = inputs.get(index);
					dicEntries.put(index, applyAffixRules(input, true));
				}
				List<Production> de = dicEntries.get(index);
				if(!de.isEmpty())
					expandedPermutationEntries.add(de);
			}
			if(!expandedPermutationEntries.isEmpty())
				entries.add(expandedPermutationEntries);
		}
		return entries;
	}

}
