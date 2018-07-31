package unit731.hunspeller.parsers.dictionary;

import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.dtos.Affixes;
import unit731.hunspeller.parsers.dictionary.valueobjects.AffixEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import unit731.hunspeller.interfaces.Productable;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;


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
	 * TODO: manage AffixParser.TAG_NEED_AFFIX, AffixParser.TAG_CIRCUMFIX, AffixParser.TAG_ONLY_IN_COMPOUND
	 * 
	 * @param dicEntry	{@link DictionaryEntry dictionary entry} to generate the stems for
	 * @return	The list of stems for the given word
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	public List<RuleProductionEntry> applyRules(DictionaryEntry dicEntry) throws IllegalArgumentException, NoApplicableRuleException{
		affParser.acquireLock();

		try{
			//convert using input table
			String word = affParser.applyInputConversionTable(dicEntry.getWord());
			dicEntry.setWord(word);

			RuleProductionEntry baseProduction = getBaseProduction(dicEntry, getFlagParsingStrategy());

			List<RuleProductionEntry> onefoldProductions = getOnefoldProductions(dicEntry);

			List<RuleProductionEntry> twofoldProductions = getTwofoldProductions(onefoldProductions);
			checkTwofoldCorrectness(twofoldProductions);

			List<RuleProductionEntry> productions = new ArrayList<>();
			productions.add(baseProduction);
			productions.addAll(onefoldProductions);
			productions.addAll(twofoldProductions);
			List<RuleProductionEntry> lastfoldProductions = getLastfoldProductions(productions);
			checkTwofoldCorrectness(lastfoldProductions);
			productions.addAll(lastfoldProductions);

			//convert using output table
			productions.forEach(production -> production.setWord(affParser.applyOutputConversionTable(production.getWord())));

//			productions.forEach(production -> log.trace(Level.INFO, "Produced word {}", production));

			return productions;
		}
		finally{
			affParser.releaseLock();
		}
	}

	private RuleProductionEntry getBaseProduction(Productable productable, FlagParsingStrategy strategy){
		return new RuleProductionEntry(productable, strategy);
	}

	private List<RuleProductionEntry> getOnefoldProductions(Productable productable) throws NoApplicableRuleException{
		List<Set<String>> applyAffixes = extractAffixes(productable, !affParser.isComplexPrefixes());
		return applyAffixRules(productable, applyAffixes);
	}

	private List<RuleProductionEntry> getTwofoldProductions(List<RuleProductionEntry> onefoldProductions) throws NoApplicableRuleException{
		List<RuleProductionEntry> twofoldProductions = new ArrayList<>();
		for(RuleProductionEntry production : onefoldProductions){
			List<Set<String>> applyAffixes = extractAffixes(production, !affParser.isComplexPrefixes());
			applyAffixes.set(1, null);
			List<RuleProductionEntry> productions = applyAffixRules(production, applyAffixes);

			List<AffixEntry> appliedRules = production.getAppliedRules();
			for(RuleProductionEntry prod : productions)
				//add parent derivations
				prod.prependAppliedRules(appliedRules);

			twofoldProductions.addAll(productions);
		}
		return twofoldProductions;
	}

	private List<RuleProductionEntry> getLastfoldProductions(List<RuleProductionEntry> productions) throws NoApplicableRuleException{
		List<RuleProductionEntry> lastfoldProductions = new ArrayList<>();
		for(RuleProductionEntry production : productions)
			if(production.isCombineable()){
				List<Set<String>> applyAffixes = extractAffixes(production, affParser.isComplexPrefixes());
				applyAffixes.set(1, null);
				List<RuleProductionEntry> prods = applyAffixRules(production, applyAffixes);

				List<AffixEntry> appliedRules = production.getAppliedRules();
				for(RuleProductionEntry prod : prods)
					//add parent derivations
					prod.prependAppliedRules(appliedRules);

				lastfoldProductions.addAll(prods);
			}
		return lastfoldProductions;
	}

	private List<Set<String>> extractAffixes(Productable productable, boolean reverse){
		Affixes affixes = separateAffixes(productable.getContinuationFlags());
		List<Set<String>> applyAffixes = new ArrayList<>(3);
		applyAffixes.addAll(Arrays.asList(affixes.getPrefixes(), affixes.getSuffixes()));
		if(reverse)
			Collections.reverse(applyAffixes);
		applyAffixes.add(affixes.getTerminalAffixes());
		return applyAffixes;
	}

	private void checkTwofoldCorrectness(List<RuleProductionEntry> twofoldProductions) throws IllegalArgumentException{
		for(RuleProductionEntry prod : twofoldProductions)
			if(prod.getContinuationFlagsCount() - (prod.containsContinuationFlag(affParser.getKeepCaseFlag())? 1: 0)
				- (prod.containsContinuationFlag(affParser.getCircumfixFlag())? 1: 0) > 0)
				throw new IllegalArgumentException("Twofold rule violated (" + prod.getRulesSequence() + " still has rules "
					+ Arrays.stream(prod.getContinuationFlags()).collect(Collectors.joining(", ")) + ")");
	}

	public boolean isAffixProductive(String word, String affix){
		word = affParser.applyInputConversionTable(word);

		boolean productive = false;
		RuleEntry rule = affParser.getData(affix);
		if(rule != null){
			List<AffixEntry> applicableAffixes = extractListOfApplicableAffixes(word, rule.getEntries());
			productive = !applicableAffixes.isEmpty();
		}
		return productive;
	}

	/**
	 * Separate the prefixes from the suffixes
	 * 
	 * @param continuationFlags	List of flags
	 * @return	An object with separated flags, one for each group
	 */
	private Affixes separateAffixes(String[] continuationFlags) throws IllegalArgumentException{
		Set<String> terminalAffixes = new HashSet<>();
		Set<String> prefixes = new HashSet<>();
		Set<String> suffixes = new HashSet<>();
		if(continuationFlags != null){
			String keepCaseFlag = affParser.getKeepCaseFlag();
			String circumfixFlag = affParser.getCircumfixFlag();
			for(String continuationFlag : continuationFlags){
				if(continuationFlag.equals(keepCaseFlag) || continuationFlag.equals(circumfixFlag)){
					terminalAffixes.add(continuationFlag);
					continue;
				}

				Object rule = affParser.getData(continuationFlag);
				if(rule == null)
					throw new IllegalArgumentException("Non–existent rule " + continuationFlag + " found");

				if(rule instanceof RuleEntry){
					if(((RuleEntry)rule).isSuffix())
						suffixes.add(continuationFlag);
					else
						prefixes.add(continuationFlag);
				}
				else
					terminalAffixes.add(continuationFlag);
			}
		}

		return new Affixes(terminalAffixes, prefixes, suffixes);
	}

	private List<RuleProductionEntry> applyAffixRules(Productable productable, List<Set<String>> applyAffixes) throws NoApplicableRuleException{
		Set<String> appliedAffixes = applyAffixes.get(0);
		Set<String> postponedAffixes = applyAffixes.get(1);

		List<RuleProductionEntry> productions = new ArrayList<>();
		if(!appliedAffixes.isEmpty()){
			String word = productable.getWord();
			String[] morphologicalFields = productable.getMorphologicalFields();

			for(String affix : appliedAffixes){
				RuleEntry rule = affParser.getData(affix);
				if(rule == null)
					throw new IllegalArgumentException("Non–existent rule " + affix + " found");

				List<AffixEntry> applicableAffixes = extractListOfApplicableAffixes(word, rule.getEntries());
				if(applicableAffixes.isEmpty())
					throw new NoApplicableRuleException("Word has no applicable rules for " + affix + " from " + productable.toString());

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

					RuleProductionEntry production = new RuleProductionEntry(newWord, morphologicalFields, entry, postponedAffixes, rule.isCombineable(), getFlagParsingStrategy());

					productions.add(production);
				}
			}
		}

		return productions;
	}

	private List<AffixEntry> extractListOfApplicableAffixes(String word, List<AffixEntry> entries){
		//extract the list of applicable affixes...
		List<AffixEntry> applicableAffixes = new ArrayList<>();
		for(AffixEntry entry : entries)
			if(entry.match(word))
				applicableAffixes.add(entry);
		return applicableAffixes;
	}

//	private List<AffixEntry> extractListOfApplicableAffixes(String word, List<AffixEntry> entries){
//		//extract the list of applicable affixes...
//		List<AffixEntry> applicableAffixes = new ArrayList<>();
//		for(AffixEntry entry : entries){
//			Matcher match = entry.getMatch();
//			//... only if it matches the given word...
//			if(match == null || PatternService.find(word, match))
//				applicableAffixes.add(entry);
//		}
//		return applicableAffixes;
//	}

}
