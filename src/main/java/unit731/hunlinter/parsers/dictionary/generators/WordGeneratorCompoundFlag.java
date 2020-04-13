package unit731.hunlinter.parsers.dictionary.generators;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.text.PermutationsWithRepetitions;


class WordGeneratorCompoundFlag extends WordGeneratorCompound{

	private static final MessageFormat NON_POSITIVE_LIMIT = new MessageFormat("Limit cannot be non–positive: was {0}");
	private static final MessageFormat NON_POSITIVE_MAX_COMPOUNDS = new MessageFormat("Max compounds cannot be non–positive: was {0}");


	WordGeneratorCompoundFlag(final AffixData affixData, final DictionaryParser dicParser, final WordGenerator wordGenerator){
		super(affixData, dicParser, wordGenerator);
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
		Objects.requireNonNull(inputCompounds);
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

		DictionaryEntry[] result = new DictionaryEntry[0];
		for(final String inputCompound : inputCompounds){
			final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(inputCompound, affixData);

			//filter input set by minimum length and forbidden flag
			if(dicEntry.getWord().length() >= compoundMinimumLength && !dicEntry.hasContinuationFlag(forbiddenWordFlag))
				result = ArrayUtils.add(result, dicEntry);
		}
		return result;
	}

	private List<List<Inflection[]>> generateCompounds(final List<int[]> permutations, final DictionaryEntry[] inputs){
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
		for(final int index : permutation){
			final Inflection[] list = dicEntries.computeIfAbsent(index, idx -> applyAffixRules(inputs[idx], true, null));
			if(list.length > 0)
				expandedPermutationEntries.add(list);
		}
		return (!expandedPermutationEntries.isEmpty()? expandedPermutationEntries: null);
	}

}
