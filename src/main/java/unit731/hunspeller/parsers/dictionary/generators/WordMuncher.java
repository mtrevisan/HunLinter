package unit731.hunspeller.parsers.dictionary.generators;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;
import unit731.hunspeller.parsers.dictionary.vos.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.vos.Production;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;


//https://github.com/nuspell/nuspell/blob/45d383c0e2f25e4ea48ee8efeca53c2bb51a3510/src/tools/munch.cxx
//https://github.com/nuspell/nuspell/blob/45d383c0e2f25e4ea48ee8efeca53c2bb51a3510/src/tools/munch.h
public class WordMuncher{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordMuncher.class);


	private final AffixData affixData;
//	private final DictionaryParser dicParser;


	public WordMuncher(final AffixData affixData, final DictionaryParser dicParser){
		Objects.requireNonNull(affixData);
//		Objects.requireNonNull(dicParser);

		this.affixData = affixData;
//		this.dicParser = dicParser;
	}

	public List<Production> inferAffixRules(final String word){
		final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();

		final List<Production> productions = new ArrayList<>();

		final List<Production> originators = extractAllAffixes(word);
originators.size();

		if(affixData.isComplexPrefixes()){
			//twofold prefixes and onefold suffixes at most
			//TODO from the original word extract all the suffixes
			//TODO from each prefix extract all the prefixes
			//TODO from each suffix extract all the prefixes

			//TODO from the original word extract all the prefixes
			//TODO from each suffix extract all the prefixes
			//TODO from each suffix extract all the suffixes

			//TODO from the original word extract all the prefixes
			//TODO from each suffix extract all the suffixes
			//TODO from each suffix extract all the prefixes
		}
		else{
			//twofold suffixes and onefold prefixes at most
			//TODO from the original word extract all the prefixes
			//TODO from each prefix extract all the suffixes
			//TODO from each suffix extract all the suffixes

			//TODO from the original word extract all the suffixes
			//TODO from each suffix extract all the suffixes
			//TODO from each suffix extract all the prefixes

			//TODO from the original word extract all the suffixes
			//TODO from each suffix extract all the prefixes
			//TODO from each suffix extract all the suffixes
		}

//		final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLineWithAliases(line, strategy, aliasesFlag,
//			aliasesMorphologicalField);
//
//		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();
//		if(dicEntry.hasContinuationFlag(forbiddenWordFlag))
//			return Collections.emptyList();
//
//		//extract suffixed productions
//		final List<Production> productions = getOnefoldProductions(dicEntry, false, !affixData.isComplexPrefixes(), overriddenRule);
//		if(LOGGER.isDebugEnabled() && !productions.isEmpty()){
//			LOGGER.debug("Suffix productions:");
//			productions.forEach(production -> LOGGER.debug("   {} from {}", production.toString(affixData.getFlagParsingStrategy()),
//				production.getRulesSequence()));
//		}

		if(LOGGER.isTraceEnabled())
			productions.forEach(production -> LOGGER.trace("Inferred word: {}", production));
		return productions;
	}

	private List<Production> extractAllAffixes(final String word){
		final List<RuleEntry> ruleEntries = affixData.getRuleEntries();
		final List<Production> originatingRules = new ArrayList<>();
		final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		final DictionaryEntry nullDicEntry = DictionaryEntry.createFromDictionaryLine(word, strategy);
		ruleLoop:
		for(final RuleEntry ruleEntry : ruleEntries){
			Production originatingRuleFromEntry = null;
			for(final AffixEntry affixEntry : ruleEntry.getEntries())
				if(affixEntry.canInverseApplyTo(word)){
					if(originatingRuleFromEntry != null)
						continue ruleLoop;

					final String originatingWord = affixEntry.undoRule(word);
					if(originatingWord != null)
						originatingRuleFromEntry = Production.createFromProduction(originatingWord, affixEntry, nullDicEntry, null, ruleEntry.isCombinable());
				}
			if(originatingRuleFromEntry != null)
				originatingRules.add(originatingRuleFromEntry);
		}
		return originatingRules;
	}

}
