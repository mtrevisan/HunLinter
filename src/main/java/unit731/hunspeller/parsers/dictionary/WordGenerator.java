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

	/**
	 * Generates a list of stems for the provided word
	 * 
	 * @param dicEntry	{@link DictionaryEntry dictionary entry} to generate the stems for
	 * @return	The list of stems for the given word
	 */
	public List<RuleProductionEntry> applyRules(DictionaryEntry dicEntry) throws IllegalArgumentException{
		boolean complexPrefixes = affParser.isComplexPrefixes();

		RuleProductionEntry baseProduction = getBaseProduction(dicEntry);

		List<RuleProductionEntry> onefoldProductions = getOnefoldProductions(dicEntry, complexPrefixes);

		List<RuleProductionEntry> twofoldProductions = getTwofoldProductions(onefoldProductions, complexPrefixes);

		List<RuleProductionEntry> productions = new ArrayList<>();
		productions.add(baseProduction);
		productions.addAll(onefoldProductions);
		productions.addAll(twofoldProductions);
		List<RuleProductionEntry> lastfoldProductions = getLastfoldProductions(productions, complexPrefixes);
		productions.addAll(lastfoldProductions);

//		productions.forEach(production -> log.trace(Level.INFO, "Produced word {}", production));

		return productions;
	}

	private RuleProductionEntry getBaseProduction(Productable productable){
		return new RuleProductionEntry(productable);
	}

	private List<RuleProductionEntry> getOnefoldProductions(Productable productable, boolean complexPrefixes){
		List<Set<String>> applyAffixes = getProductiveAffixes(productable, complexPrefixes);
		return applyAffixRules(productable, applyAffixes);
	}

	private List<RuleProductionEntry> getTwofoldProductions(List<RuleProductionEntry> onefoldProductions, boolean complexPrefixes){
		List<RuleProductionEntry> twofoldProductions = new ArrayList<>();
		for(RuleProductionEntry production : onefoldProductions){
			List<Set<String>> applyAffixes = getProductiveAffixes(production, complexPrefixes);
			//FIXME is it correct?
			applyAffixes.set(1, null);
			List<RuleProductionEntry> productions = applyAffixRules(production, applyAffixes);

			List<AffixEntry> appliedRules = production.getAppliedRules();
			for(RuleProductionEntry prod : productions){
				//add parent derivations
				prod.prependAppliedRules(appliedRules);

				//check correctness
				if(prod.hasRuleFlags())
					throw new IllegalArgumentException("Twofold rule violated (" + prod.getRulesSequence() + " still has rules "
						+ Arrays.stream(prod.getRuleFlags()).collect(Collectors.joining(", ")) + ")");
			}

			twofoldProductions.addAll(productions);
		}
		return twofoldProductions;
	}

	private List<RuleProductionEntry> getLastfoldProductions(List<RuleProductionEntry> productions, boolean complexPrefixes){
		List<RuleProductionEntry> lastfoldProductions = new ArrayList<>();
		for(RuleProductionEntry production : productions)
			if(production.isCombineable()){
				List<Set<String>> applyAffixes = getProductiveAffixes(production, complexPrefixes);
				//swap prefixes with suffixes
				Collections.reverse(applyAffixes);
				//FIXME is it correct?
				applyAffixes.set(1, null);
				List<RuleProductionEntry> prods = applyAffixRules(production, applyAffixes);

				List<AffixEntry> appliedRules = production.getAppliedRules();
				for(RuleProductionEntry prod : prods){
					//add parent derivations
					prod.prependAppliedRules(appliedRules);

					//check correctness
					if(prod.hasRuleFlags())
						throw new IllegalArgumentException("Twofold rule violated (" + prod.getRulesSequence() + " still has rules "
							+ Arrays.stream(prod.getRuleFlags()).collect(Collectors.joining(", ")) + ")");
				}

				lastfoldProductions.addAll(prods);
			}
		return lastfoldProductions;
	}

	private List<Set<String>> getProductiveAffixes(Productable productable, boolean complexPrefixes){
		Affixes affixes = separateAffixes(productable.getRuleFlags());
		List<Set<String>> applyAffixes = Arrays.asList(affixes.getPrefixes(), affixes.getSuffixes());
		if(!complexPrefixes)
			Collections.reverse(applyAffixes);
		return applyAffixes;
	}

	/**
	 * Separate the prefixes from the suffixes
	 * 
	 * @param ruleFlags	List of flags
	 * @return	An object with separated flags, one for each group
	 */
	private Affixes separateAffixes(String[] ruleFlags) throws IllegalArgumentException{
		Set<String> terminalAffixes = new HashSet<>();
		Set<String> prefixes = new HashSet<>();
		Set<String> suffixes = new HashSet<>();
		if(ruleFlags != null){
			String keepCaseFlag = affParser.getKeepCaseFlag();
			for(String ruleFlag : ruleFlags){
				if(ruleFlag.equals(keepCaseFlag))
					continue;

				Object rule = affParser.getData(ruleFlag);
				if(rule == null)
					throw new IllegalArgumentException("Non-existent rule " + ruleFlag + " found");

				if(rule instanceof RuleEntry){
					if(((RuleEntry)rule).isSuffix())
						suffixes.add(ruleFlag);
					else
						prefixes.add(ruleFlag);
				}
				else
					terminalAffixes.add(ruleFlag);
			}
		}

		return new Affixes(terminalAffixes, prefixes, suffixes);
	}

	private List<RuleProductionEntry> applyAffixRules(Productable productable, List<Set<String>> applyAffixes){
		Set<String> appliedAffixes = applyAffixes.get(0);
		Set<String> postponedAffixes = applyAffixes.get(1);

		List<RuleProductionEntry> productions = new ArrayList<>();
		if(!appliedAffixes.isEmpty()){
			String word = productable.getWord();
			String[] ruleFlags = productable.getRuleFlags();
			String[] dataFields = productable.getDataFields();

			for(String affix : appliedAffixes){
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

					RuleProductionEntry production = new RuleProductionEntry(newWord, dataFields, entry, postponedAffixes, rule.isCombineable());

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

}
