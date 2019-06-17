package unit731.hunspeller.parsers.dictionary.generators;

import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.vos.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.vos.Production;


class WordGeneratorAffixRules extends WordGeneratorBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordGeneratorAffixRules.class);


	WordGeneratorAffixRules(final AffixData affixData){
		super(affixData);
	}

	List<Production> applySingleAffixRule(final String line, final RuleEntry overriddenRule){
		final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		final List<String> aliasesFlag = affixData.getData(AffixTag.ALIASES_FLAG);
		final List<String> aliasesMorphologicalField = affixData.getData(AffixTag.ALIASES_MORPHOLOGICAL_FIELD);

		final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLineWithAliases(line, strategy, aliasesFlag, aliasesMorphologicalField);
		dicEntry.applyInputConversionTable(affixData);

		List<Production> productions = Collections.emptyList();
		final String circumfixFlag = affixData.getCircumfixFlag();
		if(!dicEntry.hasContinuationFlag(circumfixFlag)){
			final String forbiddenWordFlag = affixData.getForbiddenWordFlag();
			if(dicEntry.hasContinuationFlag(forbiddenWordFlag))
				return Collections.emptyList();

			//extract suffixed productions
			boolean isCompound = false;
			productions = getOnefoldProductions(dicEntry, isCompound, !affixData.isComplexPrefixes(), overriddenRule);
			if(LOGGER.isDebugEnabled() && !productions.isEmpty()){
				LOGGER.debug("Suffix productions:");
				productions.forEach(production -> LOGGER.debug("   {} from {}", production.toString(affixData.getFlagParsingStrategy()),
					production.getRulesSequence()));
			}

			//remove rules that invalidate the affix rule
			enforceNeedAffixFlag(productions);

			//convert using output table
			for(final Production production : productions)
				production.applyOutputConversionTable(affixData);

			if(LOGGER.isTraceEnabled())
				productions.forEach(production -> LOGGER.trace("Produced word: {}", production));
		}
		return productions;
	}

	List<Production> applyAffixRules(final String line){
		return applyAffixRules(line, null);
	}

	List<Production> applyAffixRules(final String line, final RuleEntry overriddenRule){
		final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		final List<String> aliasesFlag = affixData.getData(AffixTag.ALIASES_FLAG);
		final List<String> aliasesMorphologicalField = affixData.getData(AffixTag.ALIASES_MORPHOLOGICAL_FIELD);

		final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLineWithAliases(line, strategy, aliasesFlag, aliasesMorphologicalField);
		dicEntry.applyInputConversionTable(affixData);

		final List<Production> productions = applyAffixRules(dicEntry, false, overriddenRule);

		//convert using output table
		for(final Production production : productions)
			production.applyOutputConversionTable(affixData);

		if(LOGGER.isTraceEnabled())
			productions.forEach(production -> LOGGER.trace("Produced word: {}", production));

		return productions;
	}

}
