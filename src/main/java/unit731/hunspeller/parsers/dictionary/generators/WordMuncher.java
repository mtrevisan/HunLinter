package unit731.hunspeller.parsers.dictionary.generators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.LineEntry;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;
import unit731.hunspeller.parsers.dictionary.vos.Production;

import javax.sound.sampled.Line;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


//https://github.com/nuspell/nuspell/blob/45d383c0e2f25e4ea48ee8efeca53c2bb51a3510/src/tools/munch.cxx
//https://github.com/nuspell/nuspell/blob/45d383c0e2f25e4ea48ee8efeca53c2bb51a3510/src/tools/munch.h
public class WordMuncher{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordMuncher.class);


	private final AffixData affixData;
	private final DictionaryParser dicParser;


	public WordMuncher(final AffixData affixData, final DictionaryParser dicParser){
		Objects.requireNonNull(affixData);
		Objects.requireNonNull(dicParser);

		this.affixData = affixData;
		this.dicParser = dicParser;
	}

	public List<Production> inferAffixRules(final String line){
		final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();

		final List<Production> productions = new ArrayList<>();

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
		final List<Production> originatingRules = new ArrayList<>();
		for(final AffixEntry affixEntry : entries){
			final String originatingWord = affixEntry.undoRule(word);
			final Production newProduction = Production.createFromProduction(originatingWord, affixEntry, null, null, false);
			originatingRules.add(newProduction);
		}
		return originatingRules;
	}

}
