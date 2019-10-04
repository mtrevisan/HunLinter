package unit731.hunspeller.parsers.dictionary.generators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.vos.DictionaryEntry;
import unit731.hunspeller.parsers.vos.Production;
import unit731.hunspeller.services.regexgenerator.HunspellRegexWordGenerator;


class WordGeneratorCompoundBeginMiddleEnd extends WordGeneratorCompound{

	WordGeneratorCompoundBeginMiddleEnd(final AffixData affixData, final DictionaryParser dicParser, final WordGenerator wordGenerator){
		super(affixData, dicParser, wordGenerator);
	}

	/**
	 * Generates a list of stems for the provided flag from words in the dictionary marked with AffixOption.COMPOUND_BEGIN, AffixOption.COMPOUND_MIDDLE,
	 * and AffixOption.COMPOUND_END
	 * 
	 * @param inputCompounds	List of compounds used to generate the production through the compound rule
	 * @param limit	Limit result count
	 * @return	The list of productions
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	List<Production> applyCompoundBeginMiddleEnd(final String[] inputCompounds, final int limit) throws IllegalArgumentException{
		Objects.requireNonNull(inputCompounds);
		if(limit <= 0)
			throw new IllegalArgumentException("Limit cannot be non-positive");

		final String compoundBeginFlag = affixData.getCompoundBeginFlag();
		final String compoundMiddleFlag = affixData.getCompoundMiddleFlag();
		final String compoundEndFlag = affixData.getCompoundEndFlag();

		loadDictionaryForInclusionTest();

		//extract map flag -> dictionary entries
		final Map<String, Set<DictionaryEntry>> inputs = extractCompoundBeginMiddleEnd(inputCompounds, compoundBeginFlag, compoundMiddleFlag,
			compoundEndFlag);

		checkCompoundBeginMiddleEndInputCorrectness(inputs);

		final String[] compoundRule = new String[]{compoundBeginFlag, "*",
			compoundMiddleFlag, "*",
			compoundEndFlag, "*"};
		final HunspellRegexWordGenerator regexWordGenerator = new HunspellRegexWordGenerator(compoundRule);
		//generate all the words that matches the given regex
		final List<List<String>> permutations = regexWordGenerator.generateAll(2, limit);

		final List<List<List<Production>>> entries = generateCompounds(permutations, inputs);

		return applyCompound(entries, limit);
	}

	private Map<String, Set<DictionaryEntry>> extractCompoundBeginMiddleEnd(final String[] inputCompounds, final String compoundBeginFlag,
			final String compoundMiddleFlag, final String compoundEndFlag){
		final int compoundMinimumLength = affixData.getCompoundMinimumLength();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		//extract map flag -> compounds
		Map<String, Set<DictionaryEntry>> compoundRules = new HashMap<>();
		for(final String inputCompound : inputCompounds){
			final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(inputCompound, affixData);

			final List<Production> productions = applyAffixRules(dicEntry, false, null);
			for(final Production production : productions){
				final Map<String, Set<DictionaryEntry>> distribution = production.distributeByCompoundBeginMiddleEnd(compoundBeginFlag,
					compoundMiddleFlag, compoundEndFlag);
				compoundRules = mergeDistributions(compoundRules, distribution, compoundMinimumLength, forbiddenWordFlag);
			}
		}
		return compoundRules;
	}

	private void checkCompoundBeginMiddleEndInputCorrectness(final Map<String, Set<DictionaryEntry>> inputs){
		for(final Map.Entry<String, Set<DictionaryEntry>> entry : inputs.entrySet())
			if(entry.getValue().isEmpty())
				throw new IllegalArgumentException("Missing word(s) for rule " + entry.getKey() + " in compound begin-middle-end");
	}

}
