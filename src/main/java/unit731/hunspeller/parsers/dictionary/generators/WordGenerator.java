package unit731.hunspeller.parsers.dictionary.generators;

import java.util.List;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.vos.Production;


public class WordGenerator{

	private final WordGeneratorAffixRules wordGeneratorAffixRules;
	private final WordGeneratorCompoundRules wordGeneratorCompoundRules;
	private final WordGeneratorCompoundFlag wordGeneratorCompoundFlag;
	private final WordGeneratorCompoundBeginMiddleEnd wordGeneratorCompoundBeginMiddleEnd;


	public WordGenerator(AffixData affixData, DictionaryParser dicParser){
		wordGeneratorAffixRules = new WordGeneratorAffixRules(affixData);
		wordGeneratorCompoundRules = new WordGeneratorCompoundRules(affixData, dicParser, this);
		wordGeneratorCompoundFlag = new WordGeneratorCompoundFlag(affixData, dicParser, this);
		wordGeneratorCompoundBeginMiddleEnd = new WordGeneratorCompoundBeginMiddleEnd(affixData, dicParser, this);
	}

	public List<Production> applySingleAffixRule(String line){
		return wordGeneratorAffixRules.applySingleAffixRule(line);
	}

	public List<Production> applyAffixRules(String line){
		return wordGeneratorAffixRules.applyAffixRules(line);
	}

	public List<Production> applyCompoundRules(String[] inputCompounds, String compoundRule, int limit) throws IllegalArgumentException,
			NoApplicableRuleException{
		return wordGeneratorCompoundRules.applyCompoundRules(inputCompounds, compoundRule, limit);
	}

	public List<Production> applyCompoundFlag(String[] inputCompounds, int limit, int maxCompounds) throws IllegalArgumentException,
			NoApplicableRuleException{
		return wordGeneratorCompoundFlag.applyCompoundFlag(inputCompounds, limit, maxCompounds);
	}

	public List<Production> applyCompoundBeginMiddleEnd(String[] inputCompounds, int limit) throws IllegalArgumentException,
			NoApplicableRuleException{
		return wordGeneratorCompoundBeginMiddleEnd.applyCompoundBeginMiddleEnd(inputCompounds, limit);
	}

}
