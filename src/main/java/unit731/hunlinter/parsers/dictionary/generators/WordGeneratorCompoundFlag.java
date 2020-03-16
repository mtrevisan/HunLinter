package unit731.hunlinter.parsers.dictionary.generators;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.PermutationsWithRepetitions;


class WordGeneratorCompoundFlag extends WordGeneratorCompound{

	private static final MessageFormat NON_POSITIVE_LIMIT = new MessageFormat("Limit cannot be non–positive: was {0}");
	private static final MessageFormat NON_POSITIVE_MAX_COMPOUNDS = new MessageFormat("Max compounds cannot be non–positive: was {0}");


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
	 * @throws NoApplicableRuleException	If there are no rules that apply to the word
	 */
	Production[] applyCompoundFlag(final String[] inputCompounds, final int limit, final int maxCompounds){
		Objects.requireNonNull(inputCompounds);
		if(limit <= 0)
			throw new LinterException(NON_POSITIVE_LIMIT.format(new Object[]{limit}));
		if(maxCompounds <= 0 && maxCompounds != PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY)
			throw new LinterException(NON_POSITIVE_MAX_COMPOUNDS.format(new Object[]{maxCompounds}));

		final boolean forbidDuplicates = affixData.isForbidDuplicatesInCompound();

		loadDictionaryForInclusionTest();

		//extract list of dictionary entries
		final List<DictionaryEntry> inputs = extractCompoundFlags(inputCompounds);

		//check if it's possible to compound some words
		if(inputs.isEmpty())
			return new Production[0];

		final PermutationsWithRepetitions perm = new PermutationsWithRepetitions(inputs.size(), maxCompounds, forbidDuplicates);
		final List<int[]> permutations = perm.permutations(limit);

		final List<List<Production[]>> entries = generateCompounds(permutations, inputs);

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

	private List<List<Production[]>> generateCompounds(final List<int[]> permutations, final List<DictionaryEntry> inputs){
		final Map<Integer, Production[]> dicEntries = new HashMap<>();
		final List<List<Production[]>> list = new ArrayList<>();
		for(final int[] permutation : permutations){
			final List<Production[]> productions = generateCompound(permutation, dicEntries, inputs);
			if(productions != null)
				list.add(productions);
		}
		return list;
	}

	private List<Production[]> generateCompound(final int[] permutation, final Map<Integer, Production[]> dicEntries,
			final List<DictionaryEntry> inputs){
		final List<Production[]> expandedPermutationEntries = new ArrayList<>();
		for(final int index : permutation){
			final Production[] list = dicEntries.computeIfAbsent(index, idx -> applyAffixRules(inputs.get(idx), true, null));
			if(list.length > 0)
				expandedPermutationEntries.add(list);
		}
		return (!expandedPermutationEntries.isEmpty()? expandedPermutationEntries: null);
	}

}
