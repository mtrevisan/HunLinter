package unit731.hunspeller.parsers.dictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import unit731.hunspeller.interfaces.Productable;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.PatternService;


@AllArgsConstructor
public class WordGenerator{

	//default morphological fields:
	public static final String TAG_STEM = "st:";
	public static final String TAG_ALLOMORPH = "al:";
	public static final String TAG_PART_OF_SPEECH = "po:";
	private static final String TAG_DERIVATIONAL_PREFIX = "dp:";
	public static final String TAG_INFLECTIONAL_PREFIX = "ip:";
	private static final String TAG_TERMINAL_PREFIX = "tp:";
	private static final String TAG_DERIVATIONAL_SUFFIX = "ds:";
	public static final String TAG_INFLECTIONAL_SUFFIX = "is:";
	public static final String TAG_TERMINAL_SUFFIX = "ts:";
	private static final String TAG_SURFACE_PREFIX = "sp:";
	private static final String TAG_FREQUENCY = "fr:";
	public static final String TAG_PHONETIC = "ph:";
	private static final String TAG_HYPHENATION = "hy:";
	private static final String TAG_PART = "pa:";
	private static final String TAG_FLAG = "fl:";


	@NonNull
	private final AffixParser affParser;


	public FlagParsingStrategy getFlagParsingStrategy(){
		return affParser.getFlagParsingStrategy();
	}

	public List<RuleProductionEntry> applyRules(DictionaryEntry dicEntry) throws IllegalArgumentException{
		boolean complexPrefixes = affParser.isComplexPrefixes();
		Affixes affixes = separateAffixes(dicEntry.getRuleFlags());
		List<Set<String>> applyAffixes = Arrays.asList(affixes.getPrefixes(), affixes.getSuffixes());
		if(!complexPrefixes)
			Collections.reverse(applyAffixes);

		RuleProductionEntry baseProduction = new RuleProductionEntry(dicEntry);

		List<RuleProductionEntry> firstfoldProductions = applyAffixRules(dicEntry, applyAffixes);

		List<RuleProductionEntry> twofoldProductions = new ArrayList<>();
		for(RuleProductionEntry production : firstfoldProductions){
			affixes = separateAffixes(production.getRuleFlags());
			applyAffixes = Arrays.asList(affixes.getPrefixes(), affixes.getSuffixes());
			if(!complexPrefixes)
				Collections.reverse(applyAffixes);

			twofoldProductions.addAll(applyAffixRules(production, applyAffixes));
		}

		List<RuleProductionEntry> productions = new ArrayList<>();
		productions.add(baseProduction);
		productions.addAll(firstfoldProductions);
		productions.addAll(twofoldProductions);
		List<RuleProductionEntry> lastfoldProductions = new ArrayList<>();
		for(RuleProductionEntry production : productions){
			affixes = separateAffixes(production.getRuleFlags());
			applyAffixes = Arrays.asList(affixes.getPrefixes(), affixes.getSuffixes());
			if(!complexPrefixes)
				Collections.reverse(applyAffixes);
			//swap prefixes with suffixes
			Collections.reverse(applyAffixes);

			lastfoldProductions.addAll(applyAffixRules(production, applyAffixes));
		}
		productions.addAll(lastfoldProductions);

/*
#case 1
SFX TA Y 1
SFX TA 0 e .

#case 2
SFX TB Y 1
SFX TB 0 e/TG .
SFX TG Y 1
SFX TG 0 a .

#case 3
SFX TC Y 1
SFX TC 0 e/TH .
PFX TH Y 1
PFX TH 0 a .

#case 4
SFX TD Y 1
SFX TD 0 e/TI .
PFX TI Y 1
PFX TI 0 a/D0 .

#case 5
SFX TE N 1
SFX TE 0 e/TJ .
PFX TJ Y 1
PFX TJ 0 a .

#case 6
SFX TF N 1
SFX TF 0 e/TK .
PFX TK Y 1
PFX TK 0 a/D0 .
*/

/*		checkTwofoldViolation(twofoldProductions);

		RuleProductionEntry baseProduction = getBaseProduction(dicEntry);
*/
/*		List<RuleProductionEntry> productions = new ArrayList<>();
		productions.add(baseProduction);
		productions.addAll(firstfoldProductions);
		productions.addAll(twofoldProductions);
		List<RuleProductionEntry> lastfoldProductions = new ArrayList<>();
		for(RuleProductionEntry production : productions){
			Affixes twofoldAffixes = separateAffixes(production.getRemainingRuleFlags());
			Set<String> affxs = (complexPrefixes? twofoldAffixes.getSuffixes(): twofoldAffixes.getPrefixes());

			lastfoldProductions.addAll(getLastfoldProductions(production, affxs));
		}
		productions.addAll(lastfoldProductions);

		checkTwofoldViolation(lastfoldProductions);
*/
//		productions.forEach(production -> log.trace(Level.INFO, "Produced word {0}", production));

		return productions;
	}
/*
	public List<RuleProductionEntry> applyRules2(DictionaryEntry dicEntry) throws IllegalArgumentException{
		boolean complexPrefixes = affParser.isComplexPrefixes();
		Affixes affixes = separateAffixes(dicEntry.getRemainingRuleFlags());
		Set<String> prefixes = affixes.getPrefixes();
		Set<String> suffixes = affixes.getSuffixes();

		List<RuleProductionEntry> firstfoldProductions = getFirstfoldProductions(dicEntry, (complexPrefixes? prefixes: suffixes));

		List<RuleProductionEntry> twofoldProductions = new ArrayList<>();
		for(RuleProductionEntry production : firstfoldProductions){
			Affixes twofoldAffixes = separateAffixes(production.getRemainingRuleFlags());
			Set<String> affxs = (complexPrefixes? twofoldAffixes.getPrefixes(): twofoldAffixes.getSuffixes());

			twofoldProductions.addAll(getTwofoldProductions(production, affxs));
		}

		checkTwofoldViolation(twofoldProductions);

		RuleProductionEntry baseProduction = getBaseProduction(dicEntry);

		List<RuleProductionEntry> productions = new ArrayList<>();
		productions.add(baseProduction);
		productions.addAll(firstfoldProductions);
		productions.addAll(twofoldProductions);
		List<RuleProductionEntry> lastfoldProductions = new ArrayList<>();
		for(RuleProductionEntry production : productions){
			Affixes twofoldAffixes = separateAffixes(production.getRemainingRuleFlags());
			Set<String> affxs = (complexPrefixes? twofoldAffixes.getSuffixes(): twofoldAffixes.getPrefixes());

			lastfoldProductions.addAll(getLastfoldProductions(production, affxs));
		}
		productions.addAll(lastfoldProductions);

		checkTwofoldViolation(lastfoldProductions);

//		productions.forEach(production -> log.trace(Level.INFO, "Produced word {0}", production));

		return productions;
	}

	private RuleProductionEntry getBaseProduction(DictionaryEntry dicEntry){
		return new RuleProductionEntry(dicEntry);
	}

	private List<RuleProductionEntry> getFirstfoldProductions(DictionaryEntry dicEntry, List<Set<String>> applyAffixes){
		return applyAffixRules(dicEntry, applyAffixes);
	}

	private List<RuleProductionEntry> getTwofoldProductions(RuleProductionEntry production, Set<String> affixes) throws IllegalArgumentException{
		List<RuleProductionEntry> productions = applyAffixRules(production, affixes);

		//add parent derivations
		List<AffixEntry> appliedRules = production.getAppliedRules();
		if(appliedRules != null)
			productions.forEach(prod -> prod.getAppliedRules().addAll(0, appliedRules));

		return productions;
	}

	private List<RuleProductionEntry> getLastfoldProductions(RuleProductionEntry production, Set<String> affixes){
		List<RuleProductionEntry> productions = (!production.hasProductionRules() || production.isCombineable()?
			applyAffixRules(production, affixes): Collections.<RuleProductionEntry>emptyList());

		//add parent derivations
		List<AffixEntry> appliedRules = production.getAppliedRules();
		if(appliedRules != null)
			productions.forEach(prod -> prod.getAppliedRules().addAll(0, appliedRules));

		return productions;
	}
*/
	/**
	 * Separate the prefixes from the suffixes
	 * 
	 * @param ruleFlags	List of flags
	 * @return	An object with separated flags, one for each group
	 */
	public Affixes separateAffixes(String[] ruleFlags) throws IllegalArgumentException{
		Set<String> terminalAffixes = new HashSet<>();
		Set<String> prefixes = new HashSet<>();
		Set<String> suffixes = new HashSet<>();
		if(ruleFlags != null)
			for(String ruleFlag : ruleFlags){
				//always keep these flags
				if(affParser.isProductiveFlag(ruleFlag)){
					terminalAffixes.add(ruleFlag);
					continue;
				}

				RuleEntry rule = affParser.getData(ruleFlag);
				if(rule == null)
					throw new IllegalArgumentException("Non-existent rule " + ruleFlag + " found");

				if(rule.isSuffix())
					suffixes.add(ruleFlag);
				else
					prefixes.add(ruleFlag);
			}

		return new Affixes(terminalAffixes, prefixes, suffixes);
	}

	private List<RuleProductionEntry> applyAffixRules(Productable productable, List<Set<String>> applyAffixes){
		List<RuleProductionEntry> productions = new ArrayList<>();
		if(!applyAffixes.get(0).isEmpty()){
			String word = productable.getWord();
			String[] ruleFlags = productable.getRuleFlags();
			String[] dataFields = productable.getDataFields();

			for(String affix : applyAffixes.get(0)){
				RuleEntry rule = affParser.getData(affix);
				if(rule == null)
					throw new IllegalArgumentException(affix);

				List<AffixEntry> applicableAffixes = extractListOfApplicableAffixes(word, rule.getEntries());
				if(applicableAffixes.isEmpty())
					throw new IllegalArgumentException("Word has no applicable rules for " + affix + " from " + word
						+ affParser.getStrategy().joinRuleFlags(ruleFlags));

//List<AffixEntry> en0 = new ArrayList<>(applicableAffixes);
//List<AffixEntry> en1 = new ArrayList<>();
//String[] arr = RegExpTrieSequencer.extractCharacters(word);
//Collection<TrieNode<String[], String, List<AffixEntry>>> lst;
//if(isSuffix){
//	ArrayUtils.reverse(arr);
//	for(AffixEntry entry : rule.getSuffixEntries()){
//		Matcher match = entry.getMatch();
//		//... only if it matches the given word
//		if(match == null || PatternService.find(arr, match))
//			en1.add(entry);
//	}
//}
//else{
//	for(AffixEntry entry : rule.getPrefixEntries()){
//		Matcher match = entry.getMatch();
//		//... only if it matches the given word
//		if(match == null || PatternService.find(arr, match))
//			en1.add(entry);
//	}
//}
//en0.sort((a1, a2) -> a1.toString().compareTo(a2.toString()));
//en1.sort((a1, a2) -> a1.toString().compareTo(a2.toString()));
				//List<RegExpPrefix<AffixEntry>> rePrefixes = (isSuffix? rule.getSuffixEntries().findSuffix(word): rule.getPrefixEntries().findPrefix(word));
				//List<AffixEntry> applicableAffixes = rePrefixes.stream()
				//	.map(RegExpPrefix::getNode)
				//	.map(RegExpTrieNode::getData)
				//	.flatMap(List::stream)
				//	.collect(Collectors.toList());

				for(AffixEntry entry : applicableAffixes){
					//produce the new word
					String newWord = entry.applyRule(word, affParser.isFullstrip());

					boolean combineable = (productable.isCombineable() && rule.isCombineable());
					RuleProductionEntry production = new RuleProductionEntry(newWord, dataFields, entry, applyAffixes.get(1), combineable);

					productions.add(production);
				}
			}
		}

		return productions;
	}

	private List<AffixEntry> extractListOfApplicableAffixes(String word, List<AffixEntry> entries){
		//extract the list of applicable affixes...
		List<AffixEntry> applicableAffixes = new ArrayList<>();
		for(AffixEntry entry : entries){
			Matcher match = entry.getMatch();
			//... only if it matches the given word...
			if(match == null || PatternService.find(word, match))
				applicableAffixes.add(entry);
		}
		return applicableAffixes;
	}

	private void checkTwofoldViolation(List<RuleProductionEntry> productions) throws IllegalArgumentException{
		for(RuleProductionEntry production : productions)
			if(production.hasRuleFlags())
				throw new IllegalArgumentException("Twofold rule violated (" + production.getRulesSequence() + " still has rules "
					+ Arrays.stream(production.getRuleFlags()).collect(Collectors.joining(", ")) + ")");
		}

}
