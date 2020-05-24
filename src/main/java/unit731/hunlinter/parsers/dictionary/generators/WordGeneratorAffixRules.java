package unit731.hunlinter.parsers.dictionary.generators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.parsers.vos.RuleEntry;
import unit731.hunlinter.parsers.vos.DictionaryEntry;

import static unit731.hunlinter.services.system.LoopHelper.forEach;
import static unit731.hunlinter.services.system.LoopHelper.match;
import static unit731.hunlinter.services.system.LoopHelper.removeIf;


class WordGeneratorAffixRules extends WordGeneratorBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordGeneratorAffixRules.class);


	WordGeneratorAffixRules(final AffixData affixData){
		super(affixData);
	}

	Inflection[] applyAffixRules(final DictionaryEntry dicEntry){
		return applyAffixRules(dicEntry, null);
	}

	Inflection[] applyAffixRules(final DictionaryEntry dicEntry, final RuleEntry overriddenRule){
		Inflection[] inflections = applyAffixRules(dicEntry, false, overriddenRule);

		inflections = enforceOnlyInCompound(inflections);

		//convert using output table
		forEach(inflections,
			inflection -> inflection.applyOutputConversionTable(affixData::applyOutputConversionTable));

		if(LOGGER.isTraceEnabled())
			forEach(inflections, inflection -> LOGGER.trace("Inflected word: {}", inflection));

		return inflections;
	}

	/** Remove rules that invalidate the onlyInCompound rule */
	private Inflection[] enforceOnlyInCompound(Inflection[] inflections){
		final String onlyInCompoundFlag = affixData.getOnlyInCompoundFlag();
		if(onlyInCompoundFlag != null)
			inflections = removeIf(inflections, inflection -> {
				final boolean hasOnlyInCompoundFlag = inflection.hasContinuationFlag(onlyInCompoundFlag);
				final AffixEntry[] appliedRules = inflection.getAppliedRules();
				final boolean hasOnlyInCompoundFlagInAppliedRules = (match(appliedRules,
					appliedRule -> appliedRule.hasContinuationFlag(onlyInCompoundFlag)) != null);
				return (hasOnlyInCompoundFlag || hasOnlyInCompoundFlagInAppliedRules);
			});
		return inflections;
	}

}
