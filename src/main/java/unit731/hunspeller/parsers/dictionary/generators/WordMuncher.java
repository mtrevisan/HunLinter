package unit731.hunspeller.parsers.dictionary.generators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.workers.DictionaryInclusionTestWorker;
import unit731.hunspeller.parsers.vos.AffixEntry;
import unit731.hunspeller.parsers.vos.DictionaryEntry;
import unit731.hunspeller.parsers.vos.Production;
import unit731.hunspeller.parsers.vos.RuleEntry;
import unit731.hunspeller.services.ExceptionHelper;
import unit731.hunspeller.services.SetHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


//https://github.com/nuspell/nuspell/blob/45d383c0e2f25e4ea48ee8efeca53c2bb51a3510/src/tools/munch.cxx
//https://github.com/nuspell/nuspell/blob/45d383c0e2f25e4ea48ee8efeca53c2bb51a3510/src/tools/munch.h
public class WordMuncher{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordMuncher.class);

	private static final String SLASH = "/";


	private final AffixData affixData;
//	private final DictionaryParser dicParser;
	private final WordGenerator wordGenerator;

	private DictionaryInclusionTestWorker dicInclusionTestWorker;


	public WordMuncher(final AffixData affixData, final DictionaryParser dicParser, final WordGenerator wordGenerator){
		Objects.requireNonNull(affixData);
//		Objects.requireNonNull(dicParser);

		this.affixData = affixData;
//		this.dicParser = dicParser;
		this.wordGenerator = wordGenerator;

		loadDictionaryForInclusionTest(dicParser);
	}

	private void loadDictionaryForInclusionTest(final DictionaryParser dicParser){
		if(dicInclusionTestWorker == null){
			dicInclusionTestWorker = new DictionaryInclusionTestWorker(affixData.getLanguage(), dicParser, wordGenerator);

			try{
				dicInclusionTestWorker.executeInline();
			}
			catch(final Exception e){
				LOGGER.error(Backbone.MARKER_APPLICATION, "Cannot read dictionary: {}", ExceptionHelper.getMessage(e));
				LOGGER.error("Cannot read dictionary", e);
			}
		}
	}

	public List<DictionaryEntry> inferAffixRules(final DictionaryEntry dicEntry){
		final List<DictionaryEntry> originators = extractAllAffixes(dicEntry);

		originators.removeIf(originator -> !dicInclusionTestWorker.isInDictionary(originator.getWord()));

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

	private List<DictionaryEntry> extractAllAffixes(final DictionaryEntry dicEntry){
		final String word = dicEntry.getWord();
		final List<String> partOfSpeech = dicEntry.getMorphologicalFieldPartOfSpeech();

		final List<DictionaryEntry> originators = new ArrayList<>();
		final List<RuleEntry> ruleEntries = affixData.getRuleEntries();
		//for each rule
		for(final RuleEntry ruleEntry : ruleEntries)
			//for each affix entry in rule
			for(final AffixEntry affixEntry : ruleEntry.getEntries())
				if(affixEntry.canInverseApplyTo(word)){
					final String originatingWord = affixEntry.undoRule(word);
					if(originatingWord != null){
						final DictionaryEntry originatorEntry = wordGenerator.createFromDictionaryLineNoStemTag(originatingWord + SLASH + affixEntry.getFlag());

						final List<Production> productions = wordGenerator.applyAffixRules(originatorEntry, ruleEntry);
						//remove base production
						productions.remove(WordGenerator.BASE_PRODUCTION_INDEX);

						//FIXME consider also the cases where a word can be attached to multiple derivations from an originating word
						if(productions.size() != 1)
							continue;

						final List<String> baseProductionPartOfSpeech = productions.get(0).getMorphologicalFieldPartOfSpeech();
						if(baseProductionPartOfSpeech == null && partOfSpeech == null
								|| baseProductionPartOfSpeech != null && partOfSpeech != null && baseProductionPartOfSpeech.equals(partOfSpeech))
							originators.add(originatorEntry);
					}
				}

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
				final Map<String, List<Production>> wordBucket = SetHelper.bucket(originatingRulesFromEntry, DictionaryEntry::getWord);
				final boolean identicalOriginatingWord = wordBucket.values().stream().anyMatch(prods -> prods.size() > 1);
				if(!identicalOriginatingWord)
					originatingRules.addAll(originatingRulesFromEntry);
			}
		}
		return originatingRules;
	}

}
