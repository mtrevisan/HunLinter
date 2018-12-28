package unit731.hunspeller.parsers.dictionary.generators;

import java.util.List;
import unit731.hunspeller.languages.DictionaryBaseData;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.vos.Production;


public class WordGenerator{

	private final WordGeneratorAffixRules wordGeneratorAffixRules;
	private final WordGeneratorCompoundRules wordGeneratorCompoundRules;
	private final WordGeneratorCompoundFlag wordGeneratorCompoundFlag;
	private final WordGeneratorCompoundBeginMiddleEnd wordGeneratorCompoundBeginMiddleEnd;


	public WordGenerator(AffixParser affParser, DictionaryParser dicParser, DictionaryBaseData dictionaryBaseData){
		wordGeneratorAffixRules = new WordGeneratorAffixRules(affParser);
		wordGeneratorCompoundRules = new WordGeneratorCompoundRules(affParser, dicParser, dictionaryBaseData, this);
		wordGeneratorCompoundFlag = new WordGeneratorCompoundFlag(affParser, dicParser, dictionaryBaseData, this);
		wordGeneratorCompoundBeginMiddleEnd = new WordGeneratorCompoundBeginMiddleEnd(affParser, dicParser, dictionaryBaseData, this);
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
