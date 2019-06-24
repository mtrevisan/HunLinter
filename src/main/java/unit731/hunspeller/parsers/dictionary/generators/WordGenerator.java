package unit731.hunspeller.parsers.dictionary.generators;

import java.util.List;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.vos.Production;


public class WordGenerator{

	private final WordGeneratorAffixRules wordGeneratorAffixRules;
	private final WordGeneratorCompoundRules wordGeneratorCompoundRules;
	private final WordGeneratorCompoundFlag wordGeneratorCompoundFlag;
	private final WordGeneratorCompoundBeginMiddleEnd wordGeneratorCompoundBeginMiddleEnd;


	public WordGenerator(final AffixData affixData, final DictionaryParser dicParser){
		wordGeneratorAffixRules = new WordGeneratorAffixRules(affixData);
		wordGeneratorCompoundRules = new WordGeneratorCompoundRules(affixData, dicParser, this);
		wordGeneratorCompoundFlag = new WordGeneratorCompoundFlag(affixData, dicParser, this);
		wordGeneratorCompoundBeginMiddleEnd = new WordGeneratorCompoundBeginMiddleEnd(affixData, dicParser, this);
	}

	public List<Production> applyOnefoldAffixRule(final String line){
		return wordGeneratorAffixRules.applyOnefoldAffixRule(line, null);
	}

	public List<Production> applyOnefoldAffixRule(final String line, final RuleEntry overriddenRule){
		return wordGeneratorAffixRules.applyOnefoldAffixRule(line, overriddenRule);
	}

	public List<Production> applyAffixRules(final String line){
		return wordGeneratorAffixRules.applyAffixRules(line);
	}

	public List<Production> applyAffixRules(final String line, final RuleEntry overriddenRule){
		return wordGeneratorAffixRules.applyAffixRules(line, overriddenRule);
	}

	public List<Production> applyCompoundRules(final String[] inputCompounds, final String compoundRule, final int limit)
			throws IllegalArgumentException{
		return wordGeneratorCompoundRules.applyCompoundRules(inputCompounds, compoundRule, limit);
	}

	public List<Production> applyCompoundFlag(final String[] inputCompounds, final int limit, final int maxCompounds)
			throws IllegalArgumentException{
		return wordGeneratorCompoundFlag.applyCompoundFlag(inputCompounds, limit, maxCompounds);
	}

	public List<Production> applyCompoundBeginMiddleEnd(final String[] inputCompounds, final int limit) throws IllegalArgumentException{
		return wordGeneratorCompoundBeginMiddleEnd.applyCompoundBeginMiddleEnd(inputCompounds, limit);
	}

}
