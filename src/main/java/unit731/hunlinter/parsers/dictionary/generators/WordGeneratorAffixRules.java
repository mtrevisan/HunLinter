package unit731.hunlinter.parsers.dictionary.generators;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.vos.RuleEntry;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;


class WordGeneratorAffixRules extends WordGeneratorBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordGeneratorAffixRules.class);


	WordGeneratorAffixRules(final AffixData affixData){
		super(affixData);
	}

	List<Production> applyAffixRules(final DictionaryEntry dicEntry){
		return applyAffixRules(dicEntry, null);
	}

	List<Production> applyAffixRules(final DictionaryEntry dicEntry, final RuleEntry overriddenRule){
		final List<Production> productions = applyAffixRules(dicEntry, false, overriddenRule);

		//convert using output table
		for(final Production production : productions)
			production.applyOutputConversionTable(affixData::applyOutputConversionTable);

		if(LOGGER.isTraceEnabled())
			productions.forEach(production -> LOGGER.trace("Produced word: {}", production));

		return productions;
	}

}
