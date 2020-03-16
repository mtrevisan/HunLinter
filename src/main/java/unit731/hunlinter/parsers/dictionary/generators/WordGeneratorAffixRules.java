package unit731.hunlinter.parsers.dictionary.generators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.vos.RuleEntry;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.services.system.JavaHelper;


class WordGeneratorAffixRules extends WordGeneratorBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordGeneratorAffixRules.class);


	WordGeneratorAffixRules(final AffixData affixData){
		super(affixData);
	}

	Production[] applyAffixRules(final DictionaryEntry dicEntry){
		return applyAffixRules(dicEntry, null);
	}

	Production[] applyAffixRules(final DictionaryEntry dicEntry, final RuleEntry overriddenRule){
		Production[] productions = applyAffixRules(dicEntry, false, overriddenRule);

		productions = enforceOnlyInCompound(productions);

		//convert using output table
		for(final Production production : productions)
			production.applyOutputConversionTable(affixData::applyOutputConversionTable);

		if(LOGGER.isTraceEnabled())
			for(final Production production : productions)
				LOGGER.trace("Produced word: {}", production);

		return productions;
	}

	/** Remove rules that invalidate the onlyInCompound rule */
	private Production[] enforceOnlyInCompound(Production[] productions){
		final String onlyInCompoundFlag = affixData.getOnlyInCompoundFlag();
		if(onlyInCompoundFlag != null)
			productions = JavaHelper.removeIf(productions, production -> production.hasContinuationFlag(onlyInCompoundFlag)
				|| JavaHelper.nullableToStream(production.getAppliedRules()).anyMatch(appliedRule -> appliedRule.hasContinuationFlag(onlyInCompoundFlag)));
		return productions;
	}

}
