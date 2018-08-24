package unit731.hunspeller.parsers.dictionary;

import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.AffixEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.PermutationsWithRepetitions;
import unit731.hunspeller.services.StringService;
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

	private static final Map<StringService.Casing, Set<StringService.Casing>> COMPOUND_WORD_BOUNDARY_COLLISIONS = new EnumMap<>(StringService.Casing.class);
	static{
		Set<StringService.Casing> lowerOrTitleCase = new HashSet<>(Arrays.asList(StringService.Casing.TITLE_CASE, StringService.Casing.ALL_CAPS,
			StringService.Casing.CAMEL_CASE, StringService.Casing.PASCAL_CASE));
		COMPOUND_WORD_BOUNDARY_COLLISIONS.put(StringService.Casing.LOWER_CASE, lowerOrTitleCase);
		COMPOUND_WORD_BOUNDARY_COLLISIONS.put(StringService.Casing.TITLE_CASE, lowerOrTitleCase);
		Set<StringService.Casing> allCaps = new HashSet<>(Arrays.asList(StringService.Casing.LOWER_CASE, StringService.Casing.TITLE_CASE,
			StringService.Casing.CAMEL_CASE, StringService.Casing.PASCAL_CASE));
		COMPOUND_WORD_BOUNDARY_COLLISIONS.put(StringService.Casing.ALL_CAPS, allCaps);
	}


	private final AffixParser affParser;


	public WordGenerator(AffixParser affParser){
		Objects.requireNonNull(affParser);

		this.affParser = affParser;
	}

	public List<Production> applyRules(String line){
		List<String> aliasesFlag = affParser.getData(AffixTag.ALIASES_FLAG);
		List<String> aliasesMorphologicalField = affParser.getData(AffixTag.ALIASES_MORPHOLOGICAL_FIELD);
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		DictionaryEntry dicEntry = new DictionaryEntry(line, aliasesFlag, aliasesMorphologicalField, strategy);

		//convert using input table
		String word = affParser.applyInputConversionTable(dicEntry.getWord());
		dicEntry.setWord(word);

		List<Production> productions = applyRules(dicEntry, false);

		//convert using output table
		productions.forEach(production -> production.setWord(affParser.applyOutputConversionTable(production.getWord())));

		return productions;
	}

	/**
	 * Generates a list of stems for the provided word
	 * 
	 * @param dicEntry	{@link DictionaryEntry dictionary entry} used to generate the productions for
	 * @param isCompound	Whether the word is-a or belongs-to a compound word
	 * @return	The list of productions for the given word
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	private List<Production> applyRules(DictionaryEntry dicEntry, boolean isCompound) throws IllegalArgumentException, NoApplicableRuleException{
		//extract base production
		Production baseProduction = getBaseProduction(dicEntry, affParser.getFlagParsingStrategy());
		if(log.isDebugEnabled()){
			log.debug("Base productions:");
			log.debug("   {}", baseProduction);
		}

		//extract suffixed productions
		List<Production> onefoldProductions = getOnefoldProductions(baseProduction, isCompound);
		if(log.isDebugEnabled()){
			FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
			log.debug("Onefold productions:");
			onefoldProductions.forEach(production -> log.debug("   {} from {}", production.toString(strategy), production.getRulesSequence()));
		}

		List<Production> twofoldProductions = Collections.<Production>emptyList();
		if(!isCompound || affParser.allowTwofoldAffixesInCompound()){
			//extract prefixed productions
			twofoldProductions = getTwofoldProductions(onefoldProductions, isCompound);
			if(log.isDebugEnabled()){
				FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
				log.debug("Twofold productions:");
				twofoldProductions.forEach(production -> log.debug("   {} from {}", production.toString(strategy), production.getRulesSequence()));
			}
		}

		//extract lastfold productions
		List<Production> lastfoldProductions = new ArrayList<>();
		lastfoldProductions.add(baseProduction);
		lastfoldProductions.addAll(onefoldProductions);
		lastfoldProductions.addAll(twofoldProductions);
		lastfoldProductions = getLastfoldProductions(lastfoldProductions, isCompound);
		if(log.isDebugEnabled()){
			FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
			log.debug("Lastfold productions:");
			lastfoldProductions.forEach(production -> log.debug("   {} from {}", production.toString(strategy), production.getRulesSequence()));
		}

		checkTwofoldCorrectness(lastfoldProductions);

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

		if(log.isTraceEnabled())
			productions.forEach(production -> log.trace("Produced word: {}", production));

		return productions;
	}

	/**
	 * Generates a list of stems for the provided rule from words in the dictionary marked with AffixTag.COMPOUND_RULE
	 * 
	 * @param inputCompounds	List of compounds used to generate the production through the compound rule
	 * @param compoundRule	Rule used to generate the productions for
	 * @param limit	Limit result count
	 * @return	The list of productions for the given rule
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	public List<Production> applyCompoundRules(String[] inputCompounds, String compoundRule, int limit) throws IllegalArgumentException,
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
		if(expandedCompoundRule == null)
			throw new NoApplicableRuleException("Cannot complete compound rule, some words are missing");

		HunspellRegexWordGenerator regexWordGenerator = new HunspellRegexWordGenerator(expandedCompoundRule, true);
		//generate all the words that matches the given regex
		List<String> generatedWords = regexWordGenerator.generateAll(limit);
		List<Production> productions = generatedWords.stream()
			.map(word -> new Production(word, null, (List<DictionaryEntry>)null, strategy))
			.collect(Collectors.toList());

		//convert using output table
		productions.forEach(production -> production.setWord(affParser.applyOutputConversionTable(production.getWord())));

		if(log.isTraceEnabled())
			productions.forEach(production -> log.trace("Produced word: {}", production));

		return productions;
	}

	private Map<String, String> extractCompoundRules(String[] inputCompounds){
		int compoundMinimumLength = affParser.getCompoundMinimumLength();

		//extract map flag -> compounds
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		Map<String, Set<String>> compoundRules = new HashMap<>();
		for(String inputCompound : inputCompounds){
			//convert using input table
			inputCompound = affParser.applyInputConversionTable(inputCompound);

			DictionaryEntry production = new DictionaryEntry(inputCompound, strategy);

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
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
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

	/**
	 * Generates a list of stems for the provided flag from words in the dictionary marked with AffixTag.COMPOUND_FLAG
	 * 
	 * @param inputCompounds	List of compounds used to generate the production through the compound rule
	 * @param limit	Limit result count
	 * @param maxCompounds	Maximum compound count
	 * @return	The list of productions for the given rule
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	public List<Production> applyCompoundFlag(String[] inputCompounds, int limit, int maxCompounds) throws IllegalArgumentException,
			NoApplicableRuleException{
		Objects.requireNonNull(inputCompounds);
		if(limit <= 0)
			throw new IllegalArgumentException("Limit cannot be non-positive");

		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		String compoundFlag = affParser.getCompoundFlag();
		boolean hasForbidCompoundFlag = (affParser.getForbidCompoundFlag() != null);
		boolean hasPermitCompoundFlag = (affParser.getPermitCompoundFlag() != null);
		boolean forbidDifferentCasesInCompound = affParser.isForbidDifferentCasesInCompound();
		boolean forbidDuplications = affParser.isForbidDuplicationsInCompound();
		boolean forbidTriples = affParser.isForbidTriplesInCompound();
		boolean simplifyTriples = affParser.isSimplifyTriplesInCompound();

		List<DictionaryEntry> inputCompoundsFlag = extractCompoundFlags(inputCompounds);
		PermutationsWithRepetitions perm = new PermutationsWithRepetitions(inputCompoundsFlag.size(), maxCompounds, forbidDuplications);

		StringBuilder sb = new StringBuilder();
		List<Production> productions = new ArrayList<>();
		List<int[]> permutations = perm.permutations(limit);
		for(int[] permutation : permutations){
			//expand permutation
			List<List<Production>> expandedPermutationEntries = Arrays.stream(permutation)
				.mapToObj(inputCompoundsFlag::get)
				.map(entry -> applyRules(entry, true))
				.collect(Collectors.toList());

			//compose compounds:
			boolean completed = false;
			int[] indexes = new int[permutation.length];
			while(!completed){
				sb.setLength(0);
				List<DictionaryEntry> compoundEntries = new ArrayList<>();
				StringService.Casing lastWordCasing = null;
				for(int i = 0; i < indexes.length; i ++){
					Production next = expandedPermutationEntries.get(i).get(indexes[i]);
					compoundEntries.add(next);

					String nextCompound = next.getWord();
					if((simplifyTriples || forbidTriples) && containsTriple(sb, nextCompound)){
						//enforce simplification of triples if SIMPLIFIEDTRIPLE is set
						if(simplifyTriples)
							nextCompound = nextCompound.substring(1);
						//enforce not containment of a triple if CHECKCOMPOUNDTRIPLE is set
						else if(forbidTriples){
							sb.setLength(0);
							break;
						}
					}
					if(forbidDifferentCasesInCompound && sb.length() > 0){
						if(lastWordCasing == null)
							lastWordCasing = StringService.classifyCasing(sb.toString());
						StringService.Casing nextWord = StringService.classifyCasing(nextCompound);

						char lastChar = sb.charAt(sb.length() - 1);
						char nextChar = nextCompound.charAt(0);
						if(Character.isAlphabetic(lastChar) && Character.isAlphabetic(nextChar)){
							Set<StringService.Casing> collisions = COMPOUND_WORD_BOUNDARY_COLLISIONS.get(lastWordCasing);
							if(collisions != null && collisions.contains(nextWord)){
								sb.setLength(0);
								break;
							}
						}

						lastWordCasing = nextWord;
					}
					sb.append(nextCompound);
				}
				if(sb.length() > 0){
					List<String> continuationFlags = extractAffixesComponents(compoundEntries, compoundFlag);
					String flags = (!continuationFlags.isEmpty()? String.join(StringUtils.EMPTY, continuationFlags): null);
					if(hasForbidCompoundFlag || hasPermitCompoundFlag)
						productions.add(new Production(sb.toString(), flags, compoundEntries, strategy));
					else{
						//add boundary affixes
						List<Production> prods = applyRules(new Production(sb.toString(), flags, compoundEntries, strategy), false);

						//remove twofold because they're not allowed in compounds
						if(!affParser.allowTwofoldAffixesInCompound()){
							Iterator<Production> itr = prods.iterator();
							while(itr.hasNext())
								if(itr.next().isTwofolded())
									itr.remove();
						}

						productions.addAll(prods);
					}
				}


				//obtain next tuple
				for(int i = indexes.length - 1; i >= 0; i --){
					indexes[i] ++;
					if(indexes[i] < expandedPermutationEntries.get(i).size())
						break;

					indexes[i] = 0;
					if(i == 0)
						completed = true;
				}
			}
		}


		//convert using output table
		productions.forEach(production -> production.setWord(affParser.applyOutputConversionTable(production.getWord())));

		if(log.isTraceEnabled())
			productions.forEach(production -> log.trace("Produced word: {}", production));

		return productions;
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

	private List<DictionaryEntry> extractCompoundFlags(String[] inputCompounds){
		int compoundMinimumLength = affParser.getCompoundMinimumLength();

		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		List<DictionaryEntry> result = new ArrayList<>();
		for(String inputCompound : inputCompounds){
			//convert using input table
			inputCompound = affParser.applyInputConversionTable(inputCompound);

			//filter for words with the given compound flag
			DictionaryEntry production = new DictionaryEntry(inputCompound, strategy);

			//filter input set by minimum length
			if(production.getWord().length() < compoundMinimumLength)
				continue;

			result.add(production);
		}
		return result;
	}

	/** @return	A list of prefixes from first entry, suffixes from last entry, and terminals from both */
	private List<String> extractAffixesComponents(List<DictionaryEntry> compoundEntries, String compoundFlag){
		List<String[]> prefixes = compoundEntries.get(0).extractAffixes(affParser, false);
		List<String[]> suffixes = compoundEntries.get(compoundEntries.size() - 1).extractAffixes(affParser, false);
		Set<String> terminals = new HashSet<>();
		for(String t : prefixes.get(2))
			terminals.add(t);
		for(String t : suffixes.get(2))
			terminals.add(t);
		terminals.remove(compoundFlag);
		String compoundPrefixes = String.join(StringUtils.EMPTY, prefixes.get(0));
		String compoundSuffixes = String.join(StringUtils.EMPTY, suffixes.get(1));
		String compoundTerminals = String.join(StringUtils.EMPTY, terminals);
		return Arrays.asList(compoundPrefixes, compoundSuffixes, compoundTerminals);
	}

	private Production getBaseProduction(DictionaryEntry dicEntry, FlagParsingStrategy strategy){
		return new Production(dicEntry, strategy);
	}

	private List<Production> getOnefoldProductions(DictionaryEntry dicEntry, boolean isCompound) throws NoApplicableRuleException{
		List<String[]> applyAffixes = dicEntry.extractAffixes(affParser, !affParser.isComplexPrefixes());
		return applyAffixRules(dicEntry, applyAffixes, isCompound);
	}

	private List<Production> getTwofoldProductions(List<Production> onefoldProductions, boolean isCompound) throws NoApplicableRuleException{
		List<Production> twofoldProductions = new ArrayList<>();
		for(Production production : onefoldProductions){
			List<String[]> applyAffixes = production.extractAffixes(affParser, !affParser.isComplexPrefixes());
			List<Production> productions = applyAffixRules(production, applyAffixes, isCompound);

			List<AffixEntry> appliedRules = production.getAppliedRules();
			for(Production prod : productions)
				//add parent derivations
				prod.prependAppliedRules(appliedRules);

			twofoldProductions.addAll(productions);
		}
		return twofoldProductions;
	}

	private List<Production> getLastfoldProductions(List<Production> productions, boolean isCompound) throws NoApplicableRuleException{
		List<Production> lastfoldProductions = new ArrayList<>();
		for(Production production : productions)
			if(production.isCombineable()){
				List<String[]> applyAffixes = production.extractAffixes(affParser, affParser.isComplexPrefixes());
				List<Production> prods = applyAffixRules(production, applyAffixes, isCompound);

				List<AffixEntry> appliedRules = production.getAppliedRules();
				for(Production prod : prods)
					//add parent derivations
					prod.prependAppliedRules(appliedRules);

				lastfoldProductions.addAll(prods);
			}
		return lastfoldProductions;
	}

	private void checkTwofoldCorrectness(List<Production> twofoldProductions) throws IllegalArgumentException{
		boolean complexPrefixes = affParser.isComplexPrefixes();
		for(Production prod : twofoldProductions){
			List<String[]> affixes = prod.extractAffixes(affParser, false);

			String overabundantAffixes = null;
			if(complexPrefixes && affixes.get(1).length > 0)
				overabundantAffixes = affParser.getFlagParsingStrategy().joinFlags(affixes.get(1));
			if(!complexPrefixes && affixes.get(0).length > 0)
				overabundantAffixes = affParser.getFlagParsingStrategy().joinFlags(affixes.get(0));

			if(overabundantAffixes != null)
				throw new IllegalArgumentException("Twofold rule violated for '" + prod + " from " + prod.getRulesSequence()
					+ "' (" + prod.getRulesSequence() + " still has rules " + overabundantAffixes + ")");
		}
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

	private List<Production> applyAffixRules(DictionaryEntry dicEntry, List<String[]> applyAffixes, boolean isCompound) throws NoApplicableRuleException{
		String[] appliedAffixes = applyAffixes.get(0);
		String[] postponedAffixes = applyAffixes.get(1);

		List<Production> productions = new ArrayList<>();
		if(appliedAffixes.length > 0){
			FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
			String forbidCompoundFlag = affParser.getForbidCompoundFlag();
			String permitCompoundFlag = affParser.getPermitCompoundFlag();

			String word = dicEntry.getWord();

			for(String affix : appliedAffixes){
				RuleEntry rule = affParser.getData(affix);
				if(rule == null){
					if(affParser.isManagedByCompoundRule(affix))
						continue;

					String parentFlag = null;
					if(dicEntry instanceof Production){
						List<AffixEntry> appliedRules = ((Production)dicEntry).getAppliedRules();
						if(!appliedRules.isEmpty())
							parentFlag = appliedRules.get(0).getFlag();
					}
					throw new IllegalArgumentException("Non–existent rule " + affix + " found"
						+ (parentFlag != null? " via " + parentFlag: StringUtils.EMPTY));
				}

				List<AffixEntry> applicableAffixes = AffixParser.extractListOfApplicableAffixes(word, rule.getEntries());
				if(applicableAffixes.isEmpty())
					throw new NoApplicableRuleException("Word has no applicable rules for " + affix + " from " + dicEntry.toString());

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
					boolean hasForbidFlag = (forbidCompoundFlag != null && entry.containsContinuationFlag(forbidCompoundFlag));
					boolean hasPermitFlag = (permitCompoundFlag != null && entry.containsContinuationFlag(permitCompoundFlag));
					if(isCompound && (hasForbidFlag || !hasPermitFlag))
						continue;


					//produce the new word
					String newWord = entry.applyRule(word, affParser.isFullstrip());

					Production production = new Production(newWord, entry, dicEntry, postponedAffixes, rule.isCombineable(), strategy);

					productions.add(production);
				}
			}
		}

		return productions;
	}

}
