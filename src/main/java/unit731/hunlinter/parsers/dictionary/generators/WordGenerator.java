package unit731.hunlinter.parsers.dictionary.generators;

import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.RuleEntry;
import unit731.hunlinter.parsers.vos.Production;


public class WordGenerator{

	public static final int BASE_PRODUCTION_INDEX = 0;

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

	public DictionaryEntry createFromDictionaryLine(final String line){
		return DictionaryEntry.createFromDictionaryLine(line, wordGeneratorAffixRules.affixData);
	}

	public DictionaryEntry createFromDictionaryLineNoStemTag(final String line){
		return DictionaryEntry.createFromDictionaryLineNoStemTag(line, wordGeneratorAffixRules.affixData);
	}

	public Production[] applyAffixRules(final DictionaryEntry dicEntry){
		return wordGeneratorAffixRules.applyAffixRules(dicEntry);
	}

	public Production[] applyAffixRules(final DictionaryEntry dicEntry, final RuleEntry overriddenRule){
		return wordGeneratorAffixRules.applyAffixRules(dicEntry, overriddenRule);
	}

	public Production[] applyCompoundRules(final String[] inputCompounds, final String compoundRule, final int limit){
		return wordGeneratorCompoundRules.applyCompoundRules(inputCompounds, compoundRule, limit);
	}

	public Production[] applyCompoundFlag(final String[] inputCompounds, final int limit, final int maxCompounds){
		return wordGeneratorCompoundFlag.applyCompoundFlag(inputCompounds, limit, maxCompounds);
	}

	public Production[] applyCompoundBeginMiddleEnd(final String[] inputCompounds, final int limit){
		return wordGeneratorCompoundBeginMiddleEnd.applyCompoundBeginMiddleEnd(inputCompounds, limit);
	}

}
