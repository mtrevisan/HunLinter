package unit731.hunspeller.parsers.dictionary;

import java.beans.PropertyChangeListener;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.dtos.Affixes;
import unit731.hunspeller.parsers.dictionary.valueobjects.AffixEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.workers.CompoundRulesWorker;


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


	private final AffixParser affParser;
	private final DictionaryParser dicParser;
	private final PropertyChangeListener listener;

	@Getter
	private CompoundRulesWorker compoundRulesWorker;


	public WordGenerator(AffixParser affParser, DictionaryParser dicParser, PropertyChangeListener listener){
		Objects.requireNonNull(affParser);

		this.affParser = affParser;
		this.dicParser = dicParser;
		this.listener = listener;
	}

	public FlagParsingStrategy getFlagParsingStrategy(){
		return affParser.getFlagParsingStrategy();
	}

	public List<Production> applyRules(String line){
		FlagParsingStrategy strategy = getFlagParsingStrategy();
		DictionaryEntry dicEntry = new DictionaryEntry(line, strategy);
		return applyRules(dicEntry);
	}

	/**
	 * Generates a list of stems for the provided word
	 * 
	 * @param dicEntry	{@link DictionaryEntry dictionary entry} to generate the productions for
	 * @return	The list of productions for the given word
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	public List<Production> applyRules(DictionaryEntry dicEntry) throws IllegalArgumentException, NoApplicableRuleException{
		//convert using input table
		String word = affParser.applyInputConversionTable(dicEntry.getWord());
		dicEntry.setWord(word);

		//extract base production
		Production baseProduction = getBaseProduction(dicEntry, getFlagParsingStrategy());
		if(log.isDebugEnabled()){
			log.debug("Base productions:");
			log.debug("   {}", baseProduction);
		}

		//extract suffixed productions
		List<Production> onefoldProductions = getOnefoldProductions(dicEntry);
		if(log.isDebugEnabled()){
			log.debug("Onefold productions:");
			onefoldProductions.forEach(production -> log.debug("   {} : {}", production, production.getRulesSequence()));
		}

		//extract prefixed productions
		List<Production> twofoldProductions = getTwofoldProductions(Arrays.asList(baseProduction));
		twofoldProductions.addAll(getTwofoldProductions(onefoldProductions));
//		checkTwofoldCorrectness(twofoldProductions);
		if(log.isDebugEnabled()){
			log.debug("Twofold productions:");
			twofoldProductions.forEach(production -> log.debug("   {} : {}", production, production.getRulesSequence()));
		}

		//extract second suffixed productions
		List<Production> lastfoldProductions = new ArrayList<>();
		lastfoldProductions.addAll(onefoldProductions);
		lastfoldProductions.addAll(twofoldProductions);
		lastfoldProductions = getLastfoldProductions(lastfoldProductions);
//		checkTwofoldCorrectness(lastfoldProductions);
		if(log.isDebugEnabled()){
			log.debug("Lastfold productions:");
			lastfoldProductions.forEach(production -> log.debug("   {} : {}", production, production.getRulesSequence()));
		}

		//remove rules that invalidate the onlyInCompound rule
//		removeRulesInvalidatingOnlyInCompound(productions);

		//remove rules that invalidate the circumfix rule
		removeRulesInvalidatingCircumfix(lastfoldProductions);

		List<Production> productions = new ArrayList<>();
		productions.add(baseProduction);
		productions.addAll(onefoldProductions);
		productions.addAll(twofoldProductions);
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
	 * 
	 * @param compoundRule	Rule used to generate the productions for
	 * @param fnDeferring	Function to be called whenever the list of production is ready
	 * @param limit	Limit results
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	public void applyCompoundRules(String compoundRule, BiConsumer<List<String>, Long> fnDeferring, long limit) throws IllegalArgumentException, NoApplicableRuleException{
		compoundRulesWorker = new CompoundRulesWorker(affParser, dicParser, this, limit);
		if(listener != null)
			compoundRulesWorker.addPropertyChangeListener(listener);

		compoundRulesWorker.extractCompounds(compoundRule, fnDeferring);
	}

	private Production getBaseProduction(DictionaryEntry productable, FlagParsingStrategy strategy){
		return new Production(productable, strategy);
	}

	private List<Production> getOnefoldProductions(DictionaryEntry productable) throws NoApplicableRuleException{
		List<String[]> applyAffixes = extractAffixes(productable, !affParser.isComplexPrefixes());
		return applyAffixRules(productable, applyAffixes);
	}

	private List<Production> getTwofoldProductions(List<Production> onefoldProductions) throws NoApplicableRuleException{
		List<Production> twofoldProductions = new ArrayList<>();
		for(Production production : onefoldProductions){
			List<String[]> applyAffixes = extractAffixes(production, affParser.isComplexPrefixes());
//			applyAffixes.set(1, null);
			List<Production> productions = applyAffixRules(production, applyAffixes);

			List<AffixEntry> appliedRules = production.getAppliedRules();
			for(Production prod : productions)
				//add parent derivations
				prod.prependAppliedRules(appliedRules);

			twofoldProductions.addAll(productions);
		}
		return twofoldProductions;
	}

	private List<Production> getLastfoldProductions(List<Production> productions) throws NoApplicableRuleException{
		List<Production> lastfoldProductions = new ArrayList<>();
		for(Production production : productions)
			if(production.isCombineable()){
				List<String[]> applyAffixes = extractAffixes(production, !affParser.isComplexPrefixes());
				applyAffixes.set(1, null);
				List<Production> prods = applyAffixRules(production, applyAffixes);

				List<AffixEntry> appliedRules = production.getAppliedRules();
				for(Production prod : prods)
					//add parent derivations
					prod.prependAppliedRules(appliedRules);

				lastfoldProductions.addAll(prods);
			}
		return lastfoldProductions;
	}

	private List<String[]> extractAffixes(DictionaryEntry productable, boolean reverse){
		Affixes affixes = productable.separateAffixes(affParser);
		List<String[]> applyAffixes = new ArrayList<>(3);
		applyAffixes.add(affixes.getPrefixes());
		applyAffixes.add(affixes.getSuffixes());
		if(reverse)
			Collections.reverse(applyAffixes);
		applyAffixes.add(affixes.getTerminalAffixes());
		return applyAffixes;
	}

	private void checkTwofoldCorrectness(List<Production> twofoldProductions) throws IllegalArgumentException{
		for(Production prod : twofoldProductions)
			if(prod.hasContinuationFlags(affParser))
				throw new IllegalArgumentException("Twofold rule violated (" + prod.getRulesSequence() + " still has rules "
					+ prod.getContinuationFlags() + ")");
	}

	private List<Production> removeRulesInvalidatingOnlyInCompound(List<Production> productions){
		String onlyInCompoundFlag = affParser.getOnlyInCompoundFlag();
		Iterator<Production> itr = productions.iterator();
		while(itr.hasNext()){
			Production production = itr.next();

			if(production.getAppliedRules() == null && production.containsContinuationFlag(onlyInCompoundFlag))
				itr.remove();
		}
		return productions;
	}

	private List<Production> removeRulesInvalidatingCircumfix(List<Production> lastfoldProductions){
		String circumfixFlag = affParser.getCircumfixFlag();
		Iterator<Production> itr = lastfoldProductions.iterator();
		while(itr.hasNext()){
			Production production = itr.next();

			List<AffixEntry> appliedRules = production.getAppliedRules();
			boolean rulesContainsCircumfixFlag = appliedRules.stream()
				.anyMatch(rule -> rule.containsContinuationFlag(circumfixFlag));
			if(rulesContainsCircumfixFlag){
				//check if at least one SFX and one PFX have the circumfix flag
				boolean suffixWithCircumfix = appliedRules.stream()
					.filter(rule -> rule.isSuffix())
					.anyMatch(rule -> rule.containsContinuationFlag(circumfixFlag));
				boolean prefixWithCircumfix = appliedRules.stream()
					.filter(rule -> !rule.isSuffix())
					.anyMatch(rule -> rule.containsContinuationFlag(circumfixFlag));
				if(suffixWithCircumfix ^ prefixWithCircumfix)
					itr.remove();
			}
		}
		return lastfoldProductions;
	}

	private void enforceNeedAffixFlag(List<Production> productions){
//		Iterator<Production> itr = productions.iterator();
//		while(itr.hasNext()){
//			Production production = itr.next();
//			if(production.containsContinuationFlag(affParser.getNeedAffixFlag()))
//				itr.remove();
//		}
	}

	private List<Production> applyAffixRules(DictionaryEntry productable, List<String[]> applyAffixes) throws NoApplicableRuleException{
		String[] appliedAffixes = applyAffixes.get(0);
		String[] postponedAffixes = applyAffixes.get(1);
		String[] terminalAffixes = applyAffixes.get(2);

		List<Production> productions = new ArrayList<>();
		if(appliedAffixes.length > 0){
			String word = productable.getWord();

			for(String affix : appliedAffixes){
				RuleEntry rule = affParser.getData(affix);
				if(rule == null){
					if(affParser.isManagedByCompoundRule(affix))
						continue;

					String parentFlag = null;
					if(productable instanceof Production){
						List<AffixEntry> appliedRules = ((Production)productable).getAppliedRules();
						if(!appliedRules.isEmpty())
							parentFlag = appliedRules.get(0).getFlag();
					}
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

					String[] otherAffixes = ArrayUtils.addAll(postponedAffixes, terminalAffixes);
					Production production = new Production(newWord, entry, productable, (otherAffixes.length > 0? otherAffixes: null), rule.isCombineable(), getFlagParsingStrategy());
//					Production production = new Production(newWord, entry, productable, postponedAffixes, rule.isCombineable(), getFlagParsingStrategy());

					productions.add(production);
				}
			}
		}

		return productions;
	}

}
