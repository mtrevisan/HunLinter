package unit731.hunspeller.parsers.dictionary.generators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import unit731.hunspeller.languages.DictionaryBaseData;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.NoApplicableRuleException;
import unit731.hunspeller.parsers.dictionary.vos.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.services.regexgenerator.HunspellRegexWordGenerator;


class WordGeneratorCompoundBeginMiddleEnd extends WordGeneratorCompound{

	WordGeneratorCompoundBeginMiddleEnd(AffixParser affParser, DictionaryParser dicParser, DictionaryBaseData dictionaryBaseData,
			WordGenerator wordGenerator){
		super(affParser, dicParser, dictionaryBaseData, wordGenerator);
	}

	/**
	 * Generates a list of stems for the provided flag from words in the dictionary marked with AffixTag.COMPOUND_BEGIN, AffixTag.COMPOUND_MIDDLE,
	 * and AffixTag.COMPOUND_END
	 * 
	 * @param inputCompounds	List of compounds used to generate the production through the compound rule
	 * @param limit	Limit result count
	 * @return	The list of productions
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	List<Production> applyCompoundBeginMiddleEnd(String[] inputCompounds, int limit) throws IllegalArgumentException,
			NoApplicableRuleException{
		Objects.requireNonNull(inputCompounds);
		if(limit <= 0)
			throw new IllegalArgumentException("Limit cannot be non-positive");

		String compoundBeginFlag = affixData.getCompoundBeginFlag();
		String compoundMiddleFlag = affixData.getCompoundMiddleFlag();
		String compoundEndFlag = affixData.getCompoundEndFlag();

		loadDictionaryForInclusionTest();

		//extract map flag -> dictionary entries
		Map<String, Set<DictionaryEntry>> inputs = extractCompoundBeginMiddleEnd(inputCompounds, compoundBeginFlag, compoundMiddleFlag,
			compoundEndFlag);

		checkCompoundBeginMiddleEndInputCorrectness(inputs);

		String[] compoundRule = new String[]{compoundBeginFlag, "*",
			compoundMiddleFlag, "*",
			compoundEndFlag, "*"};
		HunspellRegexWordGenerator regexWordGenerator = new HunspellRegexWordGenerator(compoundRule);
		//generate all the words that matches the given regex
		List<List<String>> permutations = regexWordGenerator.generateAll(2, limit);

		List<List<List<Production>>> entries = generateCompounds(permutations, inputs);

		return applyCompound(entries, limit);
	}

	private Map<String, Set<DictionaryEntry>> extractCompoundBeginMiddleEnd(String[] inputCompounds, String compoundBeginFlag,
			String compoundMiddleFlag, String compoundEndFlag){
		FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		int compoundMinimumLength = affixData.getCompoundMinimumLength();
		String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		//extract map flag -> compounds
		Map<String, Set<DictionaryEntry>> compoundRules = new HashMap<>();
		for(String inputCompound : inputCompounds){
			DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(inputCompound, strategy);
			dicEntry.applyInputConversionTable(affixData);

			List<Production> productions = applyAffixRules(dicEntry, false);
			for(Production production : productions){
				Map<String, Set<DictionaryEntry>> distribution = production.distributeByCompoundBeginMiddleEnd(compoundBeginFlag,
					compoundMiddleFlag, compoundEndFlag);
				compoundRules = mergeDistributions(compoundRules, distribution, compoundMinimumLength, forbiddenWordFlag);
			}
		}
		return compoundRules;
	}

	private void checkCompoundBeginMiddleEndInputCorrectness(Map<String, Set<DictionaryEntry>> inputs){
		for(Map.Entry<String, Set<DictionaryEntry>> entry : inputs.entrySet())
			if(entry.getValue().isEmpty())
				throw new IllegalArgumentException("Missing word(s) for rule " + entry.getKey() + " in compound begin-middle-end");
	}

}
