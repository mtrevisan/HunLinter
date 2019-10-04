package unit731.hunspeller.parsers.dictionary.generators;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.enums.MorphologicalTag;
import unit731.hunspeller.parsers.vos.AffixEntry;
import unit731.hunspeller.parsers.vos.DictionaryEntry;
import unit731.hunspeller.parsers.vos.Production;
import unit731.hunspeller.parsers.vos.RuleEntry;
import unit731.hunspeller.services.SetHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


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

	public List<Production> inferAffixRules(final String line){
		final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();

		final String[] parts = StringUtils.split(line);
		final String word = parts[0];
		final String morphologicalField = (parts.length == 2 && parts[1].startsWith(MorphologicalTag.TAG_PART_OF_SPEECH.getCode())
			&& !StringUtils.containsWhitespace(parts[1])? parts[1].substring(MorphologicalTag.TAG_PART_OF_SPEECH.getCode().length()): null);
		final List<Production> originators = extractAllAffixes(word, morphologicalField);
		originators.size();

		//TODO from the original word extract all the suffixes

		//TODO from the original word extract all the prefixes

		if(affixData.isComplexPrefixes()){
			//twofold prefixes and onefold suffixes at most
			//TODO from the original word extract all the suffixes
			//TODO from each prefix extract all the prefixes

			//TODO from the original word extract all the prefixes
			//TODO from each suffix extract all the prefixes

			//TODO from the original word extract all the prefixes
			//TODO from each suffix extract all the suffixes

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

			//TODO from the original word extract all the suffixes
			//TODO from each suffix extract all the suffixes

			//TODO from the original word extract all the suffixes
			//TODO from each suffix extract all the prefixes

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

		if(LOGGER.isTraceEnabled())
			originators.forEach(production -> LOGGER.trace("Inferred word: {}", production));
		return originators;
	}

	private List<Production> extractAllAffixes(final String word, final String partOfSpeech){
		final List<Production> originatingRules = new ArrayList<>();
		final DictionaryEntry nullDicEntry = DictionaryEntry.createFromDictionaryLine(word, affixData);
		final List<RuleEntry> ruleEntries = affixData.getRuleEntries();
		for(final RuleEntry ruleEntry : ruleEntries){
			final List<Production> originatingRulesFromEntry = new ArrayList<>();
			for(final AffixEntry affixEntry : ruleEntry.getEntries())
				if(!affixEntry.hasContinuationFlags() && affixEntry.canInverseApplyTo(word)){
					final String originatingWord = affixEntry.undoRule(word);
					if(originatingWord != null){
						final Production originatingRule = Production.createFromProduction(originatingWord, affixEntry, ruleEntry.isCombinable());
						if(partOfSpeech == null || !originatingRule.hasPartOfSpeech() || originatingRule.hasPartOfSpeech(partOfSpeech))
							originatingRulesFromEntry.add(originatingRule);
					}
				}
			if(!originatingRulesFromEntry.isEmpty()){
				//originatingRulesFromEntry should not have productions from identical word
				final Map<String, List<Production>> wordBucket = SetHelper.bucket(originatingRulesFromEntry, rule -> rule.getWord());
				final boolean identicalOriginatingWord = wordBucket.values().stream().anyMatch(prods -> prods.size() > 1);
				if(!identicalOriginatingWord)
					originatingRules.addAll(originatingRulesFromEntry);
			}
		}
		return originatingRules;
	}

}
