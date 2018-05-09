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
		Set<String> prefixes = affixes.getPrefixes();
		Set<String> suffixes = affixes.getSuffixes();

		List<RuleProductionEntry> firstfoldProductions = getFirstfoldProductions(dicEntry, (complexPrefixes? prefixes: suffixes));

		List<RuleProductionEntry> twofoldProductions = new ArrayList<>();
		for(RuleProductionEntry production : firstfoldProductions)
			twofoldProductions.addAll(getTwofoldProductions(production, complexPrefixes));

		checkTwofoldViolation(twofoldProductions);

		RuleProductionEntry baseProduction = getBaseProduction(dicEntry);

		List<RuleProductionEntry> productions = new ArrayList<>();
		productions.add(baseProduction);
		productions.addAll(firstfoldProductions);
		productions.addAll(twofoldProductions);
		List<RuleProductionEntry> lastfoldProductions = new ArrayList<>();
		for(RuleProductionEntry production : productions)
			lastfoldProductions.addAll(getLastfoldProductions(production, (complexPrefixes? suffixes: prefixes)));
		productions.addAll(lastfoldProductions);

		checkTwofoldViolation(lastfoldProductions);

//		productions.forEach(production -> log.trace(Level.INFO, "Produced word {0}", production));

		return productions;
	}

	private RuleProductionEntry getBaseProduction(DictionaryEntry dicEntry){
		return new RuleProductionEntry(dicEntry);
	}

	private List<RuleProductionEntry> getFirstfoldProductions(DictionaryEntry dicEntry, Set<String> affixes){
		return applyAffixRules(dicEntry, affixes);
	}

	private List<RuleProductionEntry> getTwofoldProductions(RuleProductionEntry production, boolean complexPrefixes) throws IllegalArgumentException{
		Affixes twofoldAffixes = separateAffixes(production.getRuleFlags());
		Set<String> affixes = (complexPrefixes? twofoldAffixes.getPrefixes(): twofoldAffixes.getSuffixes());

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

	private List<RuleProductionEntry> applyAffixRules(Productable productable, Set<String> affixes){
		List<RuleProductionEntry> productions = new ArrayList<>();
		if(!affixes.isEmpty()){
			String word = productable.getWord();
			String[] ruleFlags = productable.getRuleFlags();
			String[] dataFields = productable.getDataFields();

			for(String affix : affixes){
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

					RuleProductionEntry production = new RuleProductionEntry(newWord, dataFields, entry, rule.isCombineable());

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
