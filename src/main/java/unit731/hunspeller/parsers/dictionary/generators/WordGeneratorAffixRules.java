package unit731.hunspeller.parsers.dictionary.generators;

import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.vos.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.vos.Production;


public class WordGeneratorAffixRules extends WordGenerator{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordGeneratorAffixRules.class);


	public WordGeneratorAffixRules(AffixParser affParser){
		super(affParser.getAffixData());
	}

	public List<Production> applySingleAffixRule(String line){
		FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		List<String> aliasesFlag = affixData.getData(AffixTag.ALIASES_FLAG);
		List<String> aliasesMorphologicalField = affixData.getData(AffixTag.ALIASES_MORPHOLOGICAL_FIELD);

		DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLineWithAliases(line, strategy, aliasesFlag, aliasesMorphologicalField);
		dicEntry.applyInputConversionTable(affixData);

		List<Production> productions = Collections.<Production>emptyList();
		String circumfixFlag = affixData.getCircumfixFlag();
		if(!dicEntry.hasContinuationFlag(circumfixFlag)){
			String forbiddenWordFlag = affixData.getForbiddenWordFlag();
			if(dicEntry.hasContinuationFlag(forbiddenWordFlag))
				return Collections.<Production>emptyList();

			//extract suffixed productions
			boolean isCompound = false;
			productions = getOnefoldProductions(dicEntry, isCompound, !affixData.isComplexPrefixes());
			if(LOGGER.isDebugEnabled() && !productions.isEmpty()){
				LOGGER.debug("Suffix productions:");
				productions.forEach(production -> LOGGER.debug("   {} from {}", production.toString(affixData.getFlagParsingStrategy()),
					production.getRulesSequence()));
			}

			//remove rules that invalidate the onlyInCompound rule
			if(isCompound)
				enforceOnlyInCompound(productions);

			//remove rules that invalidate the affix rule
			enforceNeedAffixFlag(productions);

			//convert using output table
			for(Production production : productions)
				production.applyOutputConversionTable(affixData);

			if(LOGGER.isTraceEnabled())
				productions.forEach(production -> LOGGER.trace("Produced word: {}", production));
		}
		return productions;
	}

	public List<Production> applyAffixRules(String line){
		FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		List<String> aliasesFlag = affixData.getData(AffixTag.ALIASES_FLAG);
		List<String> aliasesMorphologicalField = affixData.getData(AffixTag.ALIASES_MORPHOLOGICAL_FIELD);

		DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLineWithAliases(line, strategy, aliasesFlag, aliasesMorphologicalField);
		dicEntry.applyInputConversionTable(affixData);

		List<Production> productions = applyAffixRules(dicEntry, false);

		//convert using output table
		for(Production production : productions)
			production.applyOutputConversionTable(affixData);

		if(LOGGER.isTraceEnabled())
			productions.forEach(production -> LOGGER.trace("Produced word: {}", production));

		return productions;
	}

}
