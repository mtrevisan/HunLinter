package unit731.hunlinter.parsers.dictionary.generators;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.workers.dictionary.DictionaryInclusionTestWorker;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.vos.RuleEntry;
import unit731.hunlinter.services.SetHelper;

import java.util.ArrayList;
import java.util.Arrays;
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

			dicInclusionTestWorker.executeSynchronously();
		}
	}

	public List<DictionaryEntry> inferAffixRules(final DictionaryEntry dicEntry){
		final List<DictionaryEntry> originators = extractAllAffixes(dicEntry);
		originators.add(0, dicEntry);

		originators.removeIf(originator -> !dicInclusionTestWorker.isInDictionary(originator.getWord()));

		//TODO

		if(LOGGER.isTraceEnabled())
			originators.forEach(production -> LOGGER.trace("Inferred word: {}", production));
		return originators;
	}

	private List<DictionaryEntry> extractAllAffixes(final DictionaryEntry dicEntry){
		final String word = dicEntry.getWord();
		final String[] partOfSpeech = dicEntry.getMorphologicalFieldPartOfSpeech();

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

						Production[] productions = wordGenerator.applyAffixRules(originatorEntry, ruleEntry);
						//remove base production
						productions = ArrayUtils.remove(productions, WordGenerator.BASE_PRODUCTION_INDEX);

						//FIXME consider also the cases where a word can be attached to multiple derivations from an originating word
						if(productions.length != 1)
							continue;

						final String[] baseProductionPartOfSpeech = productions[0].getMorphologicalFieldPartOfSpeech();
						if(baseProductionPartOfSpeech.length == 0 && partOfSpeech.length == 0
								|| baseProductionPartOfSpeech != null && Arrays.equals(baseProductionPartOfSpeech, partOfSpeech))
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
						if(partOfSpeech.isEmpty() || !originatingRule.hasPartOfSpeech() || originatingRule.hasPartOfSpeech(partOfSpeech))
							originatingRulesFromEntry.add(originatingRule);
					}
				}
			if(!originatingRulesFromEntry.isEmpty()){
				//originatingRulesFromEntry should not have productions from identical word
				final Map<String, List<Production>> wordBucket = SetHelper.bucket(originatingRulesFromEntry, DictionaryEntry::getWord);
				boolean identicalOriginatingWord = false;
				for(final List<Production> prods : wordBucket.values())
					if(prods.size() > 1){
						identicalOriginatingWord = true;
						break;
					}
				if(!identicalOriginatingWord)
					originatingRules.addAll(originatingRulesFromEntry);
			}
		}
		return originatingRules;
	}

}
