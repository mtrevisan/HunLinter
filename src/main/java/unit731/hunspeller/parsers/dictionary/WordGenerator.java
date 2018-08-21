package unit731.hunspeller.parsers.dictionary;

import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.dtos.Affixes;
import unit731.hunspeller.parsers.dictionary.valueobjects.AffixEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.PermutationsWithRepetitions;
import unit731.hunspeller.services.regexgenerator.HunspellRegexWordGenerator;


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
	public static final String TAG_PART = "pa:";
	private static final String TAG_FLAG = "fl:";

	private static final String PIPE = "|";
	private static final String LEFT_PARENTHESIS = "(";
	private static final String RIGHT_PARENTHESIS = ")";


	private final AffixParser affParser;


	public WordGenerator(AffixParser affParser){
		Objects.requireNonNull(affParser);

		this.affParser = affParser;
	}

	public FlagParsingStrategy getFlagParsingStrategy(){
		return affParser.getFlagParsingStrategy();
	}

	public List<Production> applyRules(String line){
		FlagParsingStrategy strategy = getFlagParsingStrategy();
		List<String> aliasesFlag = affParser.getData(AffixTag.ALIASES_FLAG);
		List<String> aliasesMorphologicalField = affParser.getData(AffixTag.ALIASES_MORPHOLOGICAL_FIELD);
		DictionaryEntry dicEntry = new DictionaryEntry(line, aliasesFlag, aliasesMorphologicalField, strategy);
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
			FlagParsingStrategy strategy = affParser.getStrategy();
			log.debug("Onefold productions:");
			onefoldProductions.forEach(production -> log.debug("   {} from {}", production.toString(strategy), production.getRulesSequence()));
		}

		//extract prefixed productions
		List<Production> twofoldProductions = getTwofoldProductions(onefoldProductions);
		if(log.isDebugEnabled()){
			FlagParsingStrategy strategy = affParser.getStrategy();
			log.debug("Twofold productions:");
			twofoldProductions.forEach(production -> log.debug("   {} from {}", production.toString(strategy), production.getRulesSequence()));
		}

		//extract second suffixed productions
		List<Production> lastfoldProductions = new ArrayList<>();
		lastfoldProductions.add(baseProduction);
		lastfoldProductions.addAll(onefoldProductions);
		lastfoldProductions.addAll(twofoldProductions);
		lastfoldProductions = getLastfoldProductions(lastfoldProductions);
		checkTwofoldCorrectness(lastfoldProductions);
		if(log.isDebugEnabled()){
			FlagParsingStrategy strategy = affParser.getStrategy();
			log.debug("Lastfold productions:");
			lastfoldProductions.forEach(production -> log.debug("   {} from {}", production.toString(strategy), production.getRulesSequence()));
		}

		//remove rules that invalidate the circumfix rule
		enforceCircumfix(lastfoldProductions);

		List<Production> productions = new ArrayList<>();
		productions.add(baseProduction);
		productions.addAll(onefoldProductions);
		productions.addAll(twofoldProductions);
		productions.addAll(lastfoldProductions);

		//remove rules that invalidate the onlyInCompound rule
		enforceOnlyInCompound(productions);

		//remove rules that invalidate the affix rule
		enforceNeedAffixFlag(productions);

		//convert using output table
		productions.forEach(production -> production.setWord(affParser.applyOutputConversionTable(production.getWord())));

		if(log.isTraceEnabled())
			productions.forEach(production -> log.trace("Produced word: {}", production));

		return productions;
	}

	/**
	 * Generates a list of stems for the provided rule from words in the dictionary marked with AffixTag.COMPOUND_RULE or AffixTag.COMPOUND_FLAG
	 * 
	 * @param inputCompounds	List of compounds used to generate the production through the compound rule
	 * @param compoundRule	Rule used to generate the productions for
	 * @param limit	Limit result count
	 * @param maxCompounds	Maximum compound count
	 * @return	The list of productions for the given rule
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	//FIXME separate compound rule from compound flag
	public List<Production> applyCompoundRules(String[] inputCompounds, String compoundRule, int limit, int maxCompounds) throws IllegalArgumentException,
			NoApplicableRuleException{
		Objects.requireNonNull(inputCompounds);
		Objects.requireNonNull(compoundRule);
		if(limit <= 0)
			throw new IllegalArgumentException("Limit cannot be non-positive");

		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();

		//extract map flag -> regex of compounds
		Map<String, String> inputs = extractCompoundRules(inputCompounds);

		//compose true compound rule
		String expandedCompoundRule = composeTrueCompoundRule(inputs, compoundRule);

		List<Production> words;
		if(expandedCompoundRule != null){
			//compound rule applies

			HunspellRegexWordGenerator regexWordGenerator = new HunspellRegexWordGenerator(expandedCompoundRule, true);
			//generate all the words that matches the given regex
			List<String> generatedWords = regexWordGenerator.generateAll(limit);
			words = generatedWords.stream()
				.map(word -> new Production(word, null, (List<String>)null, strategy))
				.collect(Collectors.toList());
		}
		else{
			//compound flag applies

			List<DictionaryEntry> inputCompoundsFlag = extractCompoundFlags(inputCompounds, compoundRule);

			String forbidCompoundFlag = affParser.getForbidCompoundFlag();
			boolean forbidDuplications = affParser.isForbidDuplicationsInCompound();
			boolean forbidTriples = affParser.isForbidTriplesInCompound();
			boolean simplifyTriples = affParser.isSimplifyTriplesInCompound();
			PermutationsWithRepetitions perm = new PermutationsWithRepetitions(inputCompoundsFlag.size(), maxCompounds, forbidDuplications);
			words = new ArrayList<>();
			StringBuilder sb = new StringBuilder();
			List<String> compounds = new ArrayList<>();
			List<int[]> permutations = perm.permutations(limit);
			for(int[] permutation : permutations){
				sb.setLength(0);
				compounds.clear();

				//compose compound
				DictionaryEntry prefixes = null;
				DictionaryEntry suffixes = null;
				for(int index = 0; index < permutation.length; index ++){
					DictionaryEntry next = inputCompoundsFlag.get(permutation[index]);
					String nextCompound = next.getWord();
					compounds.add(nextCompound);
					if(compounds.size() == 1){
						//record prefixes
						prefixes = next;

						if(prefixes.containsContinuationFlag(forbidCompoundFlag)){
							sb.setLength(0);
							break;
						}
					}
					//record suffixes
					suffixes = next;

					if((simplifyTriples || forbidTriples) && containsTriple(sb, nextCompound)){
						//enforce simplification of triples if SIMPLIFIEDTRIPLE is set
						if(simplifyTriples)
							nextCompound = nextCompound.substring(1);
						//enforce compound word not contains a triple if CHECKCOMPOUNDTRIPLE is set
						else if(forbidTriples){
							sb.setLength(0);
							break;
						}
					}

					sb.append(nextCompound);
				}
				if(sb.length() > 0 && (suffixes == null || !suffixes.containsContinuationFlag(forbidCompoundFlag))){
					String newWord = sb.toString();
					String[] pre = (prefixes != null? extractAffixes(prefixes, false).get(0): new String[0]);
					String[] suf = (suffixes != null? extractAffixes(suffixes, false).get(1): new String[0]);
					String continuationFlags = Arrays.stream(ArrayUtils.addAll(pre, suf))
						.collect(Collectors.joining());
					words.add(new Production(newWord, continuationFlags, compounds, strategy));
				}
			}
		}

		return words;
	}

	private Map<String, String> extractCompoundRules(String[] inputCompounds){
		int compoundMinimumLength = affParser.getCompoundMinimumLength();

		//extract map flag -> compounds
		FlagParsingStrategy strategy = getFlagParsingStrategy();
		Map<String, Set<String>> compoundRules = new HashMap<>();
		for(String inputCompound : inputCompounds){
			DictionaryEntry production = new DictionaryEntry(inputCompound, null, null, strategy);

			//filter input set by minimum length
			if(production.getWord().length() < compoundMinimumLength)
				continue;

			Map<String, Set<String>> c = production.collectFlagsFromCompoundRule(affParser);
			for(Map.Entry<String, Set<String>> entry: c.entrySet()){
				String affix = entry.getKey();
				Set<String> prods = entry.getValue();

				Set<String> sub = compoundRules.get(affix);
				if(sub == null)
					compoundRules.put(affix, prods);
				else
					sub.addAll(prods);
			}
		}

		//transform map into flag -> regex of compounds
		return compoundRules.entrySet().stream()
			.filter(entry -> affParser.isManagedByCompoundRule(entry.getKey()))
			.collect(Collectors.toMap(entry -> entry.getKey(), entry -> LEFT_PARENTHESIS + StringUtils.join(entry.getValue(), PIPE)
				+ RIGHT_PARENTHESIS));
	}

	private String composeTrueCompoundRule(Map<String, String> inputs, String compoundRule){
		FlagParsingStrategy strategy = getFlagParsingStrategy();
		List<String> compoundRuleComponents = strategy.extractCompoundRule(compoundRule);
		StringBuilder expandedCompoundRule = new StringBuilder();
		for(String component : compoundRuleComponents){
			String flag = strategy.cleanCompoundRuleComponent(component);
			String expandedComponent = inputs.get(flag);
			if(expandedComponent == null)
				log.info(Backbone.MARKER_APPLICATION, "Missing word(s) for rule {} in compound rule {}", flag, compoundRule);
			else{
				char lastChar = component.charAt(component.length() - 1);
				if(lastChar == '*' || lastChar == '?')
					expandedComponent += lastChar;

				if(expandedComponent.equals(component))
					log.info(Backbone.MARKER_APPLICATION, "Missing word(s) for rule {} in compound rule {}", flag, compoundRule);
				else
					expandedCompoundRule.append(expandedComponent);
			}
		}
		return (expandedCompoundRule.length() > 0? expandedCompoundRule.toString(): null);
	}

	private boolean containsTriple(StringBuilder sb, String compound){
		int count = 0;
		int size = sb.length() - 1;
		if(size > 1){
			String interCompounds = sb.substring(Math.max(size - 1, 0), size + 1).concat(compound.substring(0, Math.min(compound.length(), 2)));

			char lastChar = 0;
			int len = interCompounds.length();
			for(int i = 0; count < 3 && i < len; i ++){
				char chr = interCompounds.charAt(i);
				if(chr != lastChar){
					lastChar = chr;
					count = 1;
				}
				else
					count ++;
			}
		}
		return (count >= 3);
	}

	private List<DictionaryEntry> extractCompoundFlags(String[] inputCompounds, String compoundRule){
		int compoundMinimumLength = affParser.getCompoundMinimumLength();

		FlagParsingStrategy strategy = getFlagParsingStrategy();
		List<DictionaryEntry> result = new ArrayList<>();
		for(String inputCompound : inputCompounds){
			//filter for words with the given compound flag
			DictionaryEntry production = new DictionaryEntry(inputCompound, null, null, strategy);

			//filter input set by minimum length
			if(production.getWord().length() < compoundMinimumLength)
				continue;

			result.add(production);
		}
		return result;
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
			List<String[]> applyAffixes = extractAffixes(production, !affParser.isComplexPrefixes());
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
				List<String[]> applyAffixes = extractAffixes(production, affParser.isComplexPrefixes());
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
		return affixes.extractAffixes(reverse);
	}

	private void checkTwofoldCorrectness(List<Production> twofoldProductions) throws IllegalArgumentException{
		for(Production prod : twofoldProductions)
			if(prod.hasContinuationFlags(affParser))
				throw new IllegalArgumentException("Twofold rule violated (" + prod.getRulesSequence() + " still has rules "
					+ prod.getContinuationFlags() + ")");
	}

	private List<Production> enforceOnlyInCompound(List<Production> productions){
		String onlyInCompoundFlag = affParser.getOnlyInCompoundFlag();
		Iterator<Production> itr = productions.iterator();
		while(itr.hasNext()){
			Production production = itr.next();

			//all the rules generated by this method does not comes from a compound rule, so if it has the flag then it is to be discarded
			if(production.containsContinuationFlag(onlyInCompoundFlag))
				itr.remove();
		}
		return productions;
	}

	private List<Production> enforceCircumfix(List<Production> lastfoldProductions){
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
		String needAffixFlag = affParser.getNeedAffixFlag();
		Iterator<Production> itr = productions.iterator();
		while(itr.hasNext()){
			Production production = itr.next();

			boolean hasNeedAffixFlag = false;
			List<AffixEntry> appliedRules = production.getAppliedRules();
			if(appliedRules != null){
				//check that last suffix and last prefix shouldn't have the needaffix flag
				boolean lastSuffix = false;
				boolean lastPrefix = false;
				boolean lastSuffixNeedAffix = false;
				boolean lastPrefixNeedAffix = false;
				for(int i = appliedRules.size() - 1; (!lastSuffix || !lastPrefix) && i >= 0; i --){
					AffixEntry appliedRule = appliedRules.get(i);
					if(!lastSuffix && appliedRule.isSuffix()){
						lastSuffix = true;
						lastSuffixNeedAffix = appliedRule.containsContinuationFlag(needAffixFlag);
					}
					else if(!lastPrefix && !appliedRule.isSuffix()){
						lastPrefix = true;
						lastPrefixNeedAffix = appliedRule.containsContinuationFlag(needAffixFlag);
					}
				}
				hasNeedAffixFlag = (!lastSuffix || lastSuffixNeedAffix) && (!lastPrefix || lastPrefixNeedAffix);
			}
			if(hasNeedAffixFlag)
				itr.remove();
		}
	}

	private List<Production> applyAffixRules(DictionaryEntry productable, List<String[]> applyAffixes) throws NoApplicableRuleException{
		String[] appliedAffixes = applyAffixes.get(0);
		String[] postponedAffixes = applyAffixes.get(1);

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
					throw new IllegalArgumentException("Non–existent rule " + affix + " found"
						+ (parentFlag != null? " via " + parentFlag: StringUtils.EMPTY));
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

					Production production = new Production(newWord, entry, productable, postponedAffixes, rule.isCombineable(), getFlagParsingStrategy());

					productions.add(production);
				}
			}
		}

		return productions;
	}

}
