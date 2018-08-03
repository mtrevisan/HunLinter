package unit731.hunspeller.parsers.dictionary;

import java.beans.PropertyChangeListener;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.dtos.Affixes;
import unit731.hunspeller.parsers.dictionary.valueobjects.AffixEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.interfaces.Productable;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.workers.CompoundRulesWorker;


@AllArgsConstructor
@Slf4j
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

	private static final long COMPOUND_WORDS_LIMIT = 20l;


	@NonNull
	private final AffixParser affParser;
	@NonNull
	private final DictionaryParser dicParser;
	private final PropertyChangeListener listener;


	public FlagParsingStrategy getFlagParsingStrategy(){
		return affParser.getFlagParsingStrategy();
	}

	public List<RuleProductionEntry> applyRules(String line){
		FlagParsingStrategy strategy = getFlagParsingStrategy();
		DictionaryEntry dicEntry = new DictionaryEntry(line, strategy);
		return applyRules(dicEntry);
	}

	/**
	 * Generates a list of stems for the provided word
	 * TODO: manage AffixParser.TAG_ONLY_IN_COMPOUND
	 * 
	 * @param dicEntry	{@link DictionaryEntry dictionary entry} to generate the productions for
	 * @return	The list of productions for the given word
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	public List<RuleProductionEntry> applyRules(DictionaryEntry dicEntry) throws IllegalArgumentException, NoApplicableRuleException{
		//convert using input table
		String word = affParser.applyInputConversionTable(dicEntry.getWord());
		dicEntry.setWord(word);

		//extract base production
		RuleProductionEntry baseProduction = getBaseProduction(dicEntry, getFlagParsingStrategy());

		//extract onefold production
		List<RuleProductionEntry> onefoldProductions = getOnefoldProductions(dicEntry);

		//extract twofold production
		List<RuleProductionEntry> twofoldProductions = getTwofoldProductions(onefoldProductions);
		checkTwofoldCorrectness(twofoldProductions);

		//collect productions
		List<RuleProductionEntry> productions = new ArrayList<>();
		productions.add(baseProduction);
		productions.addAll(onefoldProductions);
		productions.addAll(twofoldProductions);
		List<RuleProductionEntry> lastfoldProductions = getLastfoldProductions(productions);
		checkTwofoldCorrectness(lastfoldProductions);

		//remove rules that invalidate the circumfix rule
		removeRulesInvalidatingCircumfix(lastfoldProductions);

		productions.addAll(lastfoldProductions);

		//remove rules with the need affix flag
		enforceNeedAffixFlag(productions);

		//convert using output table
		productions.forEach(production -> production.setWord(affParser.applyOutputConversionTable(production.getWord())));

		if(log.isTraceEnabled())
			productions.forEach(production -> log.trace("Produced word {}", production));

		return productions;
	}

	/**
	 * Generates a list of stems for the provided word using only the AffixParser.TAG_COMPOUND_RULES
	 * TODO: manage AffixParser.TAG_ONLY_IN_COMPOUND
	 * 
	 * @param compoundRule	Rule used to generate the productions for
	 * @param fnDeferring	Function to be called whenever the list of production is ready
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	//TODO
	public void applyCompoundRules(String compoundRule, BiConsumer<List<String>, Long> fnDeferring) throws IllegalArgumentException, NoApplicableRuleException{
		CompoundRulesWorker compoundRulesWorker = new CompoundRulesWorker(affParser, dicParser, this, COMPOUND_WORDS_LIMIT);
		if(listener != null)
			compoundRulesWorker.addPropertyChangeListener(listener);

		BiConsumer<List<String>, Long> done = (words, wordCount) -> {
			fnDeferring.accept(words, wordCount);
		};
		compoundRulesWorker.extractCompounds(compoundRule, done);
//		BiConsumer<List<String>, Long> fnDeferring = (words, count) -> {
//			for(String word : words)
//				log.info(Backbone.MARKER_APPLICATION, word);
//			if(count != limit)
//				log.info(Backbone.MARKER_APPLICATION, "\u2026");
//		};
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
		Affixes affixes = separateAffixes(productable);
		List<Set<String>> applyAffixes = new ArrayList<>(3);
		applyAffixes.addAll(Arrays.asList(affixes.getPrefixes(), affixes.getSuffixes()));
		if(reverse)
			Collections.reverse(applyAffixes);
		applyAffixes.add(affixes.getTerminalAffixes());
		return applyAffixes;
	}

	/**
	 * Separate the prefixes from the suffixes
	 * 
	 * @param continuationFlags	List of flags
	 * @return	An object with separated flags, one for each group
	 */
	private Affixes separateAffixes(Productable productable) throws IllegalArgumentException{
		String[] affixes = productable.getContinuationFlags();

		Set<String> terminalAffixes = new HashSet<>();
		Set<String> prefixes = new HashSet<>();
		Set<String> suffixes = new HashSet<>();
		if(affixes != null)
			for(String affix : affixes){
				if(affParser.isTerminalAffix(affix)){
					terminalAffixes.add(affix);
					continue;
				}

				Object rule = affParser.getData(affix);
				if(rule == null){
					if(affParser.isManagedByCompoundRule(affix))
						continue;

					String parentFlag = (productable instanceof RuleProductionEntry? ((RuleProductionEntry)productable).getAppliedRules().get(0).getFlag(): null);
					throw new IllegalArgumentException("Non–existent rule " + affix + " found" + (parentFlag != null? " via " + parentFlag: StringUtils.EMPTY));
				}

				if(rule instanceof RuleEntry){
					if(((RuleEntry)rule).isSuffix())
						suffixes.add(affix);
					else
						prefixes.add(affix);
				}
				else
					terminalAffixes.add(affix);
			}

		return new Affixes(terminalAffixes, prefixes, suffixes);
	}

	private void checkTwofoldCorrectness(List<RuleProductionEntry> twofoldProductions) throws IllegalArgumentException{
		for(RuleProductionEntry prod : twofoldProductions)
			if(prod.getContinuationFlagsCount() - (prod.containsContinuationFlag(affParser.getKeepCaseFlag())? 1: 0)
				- (prod.containsContinuationFlag(affParser.getCircumfixFlag())? 1: 0) > 0)
				throw new IllegalArgumentException("Twofold rule violated (" + prod.getRulesSequence() + " still has rules "
					+ Arrays.stream(prod.getContinuationFlags()).collect(Collectors.joining(", ")) + ")");
	}

	private void removeRulesInvalidatingCircumfix(List<RuleProductionEntry> lastfoldProductions){
		String circumfixFlag = affParser.getCircumfixFlag();
		Iterator<RuleProductionEntry> itr = lastfoldProductions.iterator();
		while(itr.hasNext()){
			RuleProductionEntry production = itr.next();
			
			List<AffixEntry> appliedRules = production.getAppliedRules();
			boolean rulesContainsCircumfixFlag = appliedRules.stream()
				.anyMatch(rule -> ArrayUtils.contains(rule.getContinuationFlags(), circumfixFlag));
			if(rulesContainsCircumfixFlag){
				//check if at least one SFX and one PFX have the circumfix flag
				boolean suffixWithCircumfix = appliedRules.stream()
					.filter(rule -> rule.isSuffix())
					.anyMatch(rule -> ArrayUtils.contains(rule.getContinuationFlags(), circumfixFlag));
				boolean prefixWithCircumfix = appliedRules.stream()
					.filter(rule -> !rule.isSuffix())
					.anyMatch(rule -> ArrayUtils.contains(rule.getContinuationFlags(), circumfixFlag));
				if(suffixWithCircumfix ^ prefixWithCircumfix)
					itr.remove();
			}
		}
	}

	private void enforceNeedAffixFlag(List<RuleProductionEntry> productions){
		Iterator<RuleProductionEntry> itr = productions.iterator();
		while(itr.hasNext()){
			RuleProductionEntry production = itr.next();
			if(production.containsContinuationFlag(affParser.getNeedAffixFlag()))
				itr.remove();
		}
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
				if(rule == null){
					if(affParser.isManagedByCompoundRule(affix))
						continue;

					String parentFlag = (productable instanceof RuleProductionEntry? ((RuleProductionEntry)productable).getAppliedRules().get(0).getFlag(): null);
					throw new IllegalArgumentException("Non–existent rule " + affix + " found" + (parentFlag != null? " via " + parentFlag: StringUtils.EMPTY));
				}

				List<AffixEntry> applicableAffixes = AffixParser.extractListOfApplicableAffixes(word, rule.getEntries());
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

}
