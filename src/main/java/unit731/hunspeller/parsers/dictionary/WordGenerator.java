package unit731.hunspeller.parsers.dictionary;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.languages.DictionaryBaseData;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;
import unit731.hunspeller.parsers.dictionary.vos.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.DictionaryInclusionTestWorker;
import unit731.hunspeller.services.ExceptionHelper;
import unit731.hunspeller.services.StringHelper;


public class WordGenerator{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordGenerator.class);

	private static final Map<StringHelper.Casing, Set<StringHelper.Casing>> COMPOUND_WORD_BOUNDARY_COLLISIONS
		= new EnumMap<>(StringHelper.Casing.class);
	static{
		Set<StringHelper.Casing> lowerOrTitleCase = new HashSet<>(Arrays.asList(StringHelper.Casing.TITLE_CASE, StringHelper.Casing.ALL_CAPS,
			StringHelper.Casing.CAMEL_CASE, StringHelper.Casing.PASCAL_CASE));
		COMPOUND_WORD_BOUNDARY_COLLISIONS.put(StringHelper.Casing.LOWER_CASE, lowerOrTitleCase);
		COMPOUND_WORD_BOUNDARY_COLLISIONS.put(StringHelper.Casing.TITLE_CASE, lowerOrTitleCase);
		Set<StringHelper.Casing> allCaps = new HashSet<>(Arrays.asList(StringHelper.Casing.LOWER_CASE, StringHelper.Casing.TITLE_CASE,
			StringHelper.Casing.CAMEL_CASE, StringHelper.Casing.PASCAL_CASE));
		COMPOUND_WORD_BOUNDARY_COLLISIONS.put(StringHelper.Casing.ALL_CAPS, allCaps);
	}


	private final AffixParser affParser;
	protected final AffixData affixData;
	private final DictionaryParser dicParser;
	private final DictionaryBaseData dictionaryBaseData;

	private DictionaryInclusionTestWorker dicInclusionTestWorker;
	protected final Set<String> compoundAsReplacement = new HashSet<>();


	protected WordGenerator(AffixParser affParser, DictionaryParser dicParser, DictionaryBaseData dictionaryBaseData){
		Objects.requireNonNull(affParser);

		this.affParser = affParser;
		this.affixData = affParser.getAffixData();
		this.dicParser = dicParser;
		this.dictionaryBaseData = dictionaryBaseData;
	}

	/**
	 * Generates a list of stems for the provided word
	 * 
	 * @param dicEntry	{@link DictionaryEntry dictionary entry} used to generate the productions for
	 * @param isCompound	Whether the word is-a or belongs-to a compound word
	 * @return	The list of productions for the given word
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	protected List<Production> applyAffixRules(DictionaryEntry dicEntry, boolean isCompound) throws IllegalArgumentException,
			NoApplicableRuleException{
		String forbiddenWordFlag = affixData.getForbiddenWordFlag();
		if(dicEntry.hasContinuationFlag(forbiddenWordFlag))
			return Collections.<Production>emptyList();

		//extract base production
		Production baseProduction = getBaseProduction(dicEntry);
		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("Base production:");
			LOGGER.debug("   {}", baseProduction);
		}

		//extract suffixed productions
		List<Production> onefoldProductions = getOnefoldProductions(baseProduction, isCompound, !affixData.isComplexPrefixes());
		printProductions((affixData.isComplexPrefixes()? "Prefix productions:": "Suffix productions:"), onefoldProductions);

		List<Production> twofoldProductions = Collections.<Production>emptyList();
		if(!isCompound || affixData.allowTwofoldAffixesInCompound()){
			//extract prefixed productions
			twofoldProductions = getTwofoldProductions(onefoldProductions, isCompound, !affixData.isComplexPrefixes());
			printProductions((affixData.isComplexPrefixes()? "Suffix productions:": "Prefix productions:"), twofoldProductions);
		}

		//extract lastfold productions
		List<Production> lastfoldProductions = new ArrayList<>();
		lastfoldProductions.add(baseProduction);
		lastfoldProductions.addAll(onefoldProductions);
		lastfoldProductions.addAll(twofoldProductions);
		lastfoldProductions = getTwofoldProductions(lastfoldProductions, isCompound, affixData.isComplexPrefixes());
		printProductions("Twofold productions:", lastfoldProductions);

		checkTwofoldCorrectness(lastfoldProductions);

		//remove rules that invalidate the circumfix rule
		enforceCircumfix(lastfoldProductions);

		List<Production> productions = new ArrayList<>();
		productions.add(baseProduction);
		productions.addAll(onefoldProductions);
		productions.addAll(twofoldProductions);
		productions.addAll(lastfoldProductions);

		//remove rules that invalidate the onlyInCompound rule
		if(isCompound)
			enforceOnlyInCompound(productions);

		//remove rules that invalidate the affix rule
		enforceNeedAffixFlag(productions);

		return productions;
	}


	private void printProductions(String title, List<Production> productions){
		if(LOGGER.isDebugEnabled() && !productions.isEmpty()){
			LOGGER.debug(title);
			productions.forEach(production -> LOGGER.debug("   {} from {}", production.toString(affixData.getFlagParsingStrategy()),
				production.getRulesSequence()));
		}
	}


	protected List<List<List<Production>>> generateCompounds(List<List<String>> permutations, Map<String, Set<DictionaryEntry>> inputs){
		List<List<List<Production>>> entries = new ArrayList<>();
		Map<String, List<Production>> dicEntries = new HashMap<>();
		outer:
		for(List<String> permutation : permutations){
			//expand permutation
			List<List<Production>> expandedPermutationEntries = new ArrayList<>();
			for(String flag : permutation){
				if(!dicEntries.containsKey(flag)){
					Set<DictionaryEntry> input = inputs.get(flag);
					dicEntries.put(flag, input.stream()
						.map(entry -> applyAffixRules(entry, true))
						.map(entry -> entry.stream().filter(prod -> prod.hasContinuationFlag(flag)).collect(Collectors.toList()))
						.flatMap(List::stream)
						.collect(Collectors.toList()));
				}
				List<Production> de = dicEntries.get(flag);
				if(!de.isEmpty())
					expandedPermutationEntries.add(de);
				else{
					//it is not possible to compound some words, return empty list
					entries.clear();
					break outer;
				}
			}
			if(!expandedPermutationEntries.isEmpty())
				entries.add(expandedPermutationEntries);
		}
		return entries;
	}

	//FIXME refactor
	protected List<Production> applyCompound(List<List<List<Production>>> entries, int limit) throws IllegalArgumentException,
			NoApplicableRuleException{
		FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		String compoundFlag = affixData.getCompoundFlag();
		String forbiddenWordFlag = affixData.getForbiddenWordFlag();
		String forceCompoundUppercaseFlag = affixData.getForceCompoundUppercaseFlag();
		boolean hasForbidCompoundFlag = (affixData.getForbidCompoundFlag() != null);
		boolean hasPermitCompoundFlag = (affixData.getPermitCompoundFlag() != null);
		boolean forbidDifferentCasesInCompound = affixData.isForbidDifferentCasesInCompound();
		boolean checkCompoundReplacement = affixData.isCheckCompoundReplacement();
		boolean forbidTriples = affixData.isForbidTriplesInCompound();
		boolean simplifyTriples = affixData.isSimplifyTriplesInCompound();
		boolean allowTwofoldAffixesInCompound = affixData.allowTwofoldAffixesInCompound();

		compoundAsReplacement.clear();

		StringBuilder sb = new StringBuilder();
		Set<Production> productions = new LinkedHashSet<>();
		//generate compounds:
		for(List<List<Production>> entry : entries){
			//compose compound:
			boolean completed = false;
			int[] indexes = new int[entry.size()];
			while(!completed){
				sb.setLength(0);
				List<DictionaryEntry> compoundEntries = new ArrayList<>();
				StringHelper.Casing lastWordCasing = null;
				for(int i = 0; i < indexes.length; i ++){
					Production next = entry.get(i).get(indexes[i]);
					if(next.hasContinuationFlag(forbiddenWordFlag)){
						sb.setLength(0);
						break;
					}
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
					if(sb.length() > 0 && forbidDifferentCasesInCompound){
						if(lastWordCasing == null)
							lastWordCasing = StringHelper.classifyCasing(sb.toString());
						StringHelper.Casing nextWord = StringHelper.classifyCasing(nextCompound);

						char lastChar = sb.charAt(sb.length() - 1);
						char nextChar = nextCompound.charAt(0);
						if(Character.isAlphabetic(lastChar) && Character.isAlphabetic(nextChar)){
							Set<StringHelper.Casing> collisions = COMPOUND_WORD_BOUNDARY_COLLISIONS.get(lastWordCasing);
							//convert nextChar to lowercase/uppercase and go on
							if(collisions != null && collisions.contains(nextWord))
								nextCompound = (Character.isUpperCase(lastChar)? StringUtils.capitalize(nextCompound):
									StringUtils.uncapitalize(nextCompound));
						}

						lastWordCasing = nextWord;
					}
					sb.append(nextCompound);
				}

				if(sb.length() > 0 && (!checkCompoundReplacement || !existsCompoundAsReplacement(sb.toString()))){
					List<String> continuationFlags = extractAffixesComponents(compoundEntries, compoundFlag);
					if(!continuationFlags.contains(forbiddenWordFlag)){
						String word = sb.toString();
						String flags = (!continuationFlags.isEmpty()? String.join(StringUtils.EMPTY, continuationFlags): null);
						Production p = Production.createFromCompound(word, flags, compoundEntries, strategy);
						if(hasForbidCompoundFlag || hasPermitCompoundFlag)
							productions.add(p);
						else{
							//add boundary affixes
							List<Production> prods = applyAffixRules(p, false);

							//remove twofold because they're not allowed in compounds
							if(!allowTwofoldAffixesInCompound){
								Iterator<Production> itr = prods.iterator();
								while(itr.hasNext())
									if(itr.next().isTwofolded())
										itr.remove();
							}

							productions.addAll(prods);
						}

						if(productions.size() >= limit)
							completed = true;
					}
				}


				//obtain next tuple
				for(int i = indexes.length - 1; !completed && i >= 0; i --){
					indexes[i] ++;
					if(indexes[i] < entry.get(i).size())
						break;

					indexes[i] = 0;
					if(i == 0)
						completed = true;
				}
			}
		}

		compoundAsReplacement.clear();

		//convert using output table
		for(Production production : productions){
			production.applyOutputConversionTable(affixData);
			production.capitalizeIfContainsFlag(forceCompoundUppercaseFlag);
			production.removeContinuationFlag(forceCompoundUppercaseFlag);
		}

		if(LOGGER.isTraceEnabled())
			productions.forEach(production -> LOGGER.trace("Produced word: {}", production));

		List<Production> response = new ArrayList<>(productions);
		if(response.size() > limit)
			response = response.subList(0, limit);
		return response;
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

	//is word a non compound with a REP substitution (see checkcompoundrep)?
	private boolean existsCompoundAsReplacement(String word){
		boolean exists = false;

		for(String cr : compoundAsReplacement)
			if(word.contains(cr))
				return true;

		if(word.length() >= 2){
			List<String> conversions = affixData.applyReplacementTable(word);
			for(String candidate : conversions)
				if(dicInclusionTestWorker.isInDictionary(candidate)){
					compoundAsReplacement.add(word);

					return true;
				}
		}
		return exists;
	}


	/** Merge the distribution with the others */
	protected Map<String, Set<DictionaryEntry>> mergeDistributions(Map<String, Set<DictionaryEntry>> compoundRules,
			Map<String, Set<DictionaryEntry>> distribution, int compoundMinimumLength, String forbiddenWordFlag){
		compoundRules = Stream.of(compoundRules, distribution)
			.flatMap(m -> m.entrySet().stream())
			.map(m -> {
				String key = m.getKey();
				Set<DictionaryEntry> value = m.getValue().stream()
					.filter(entry -> entry.getWord().length() >= compoundMinimumLength && !entry.hasContinuationFlag(forbiddenWordFlag))
					.collect(Collectors.toSet());
				return new AbstractMap.SimpleEntry<>(key, value);
			})
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
				(entries1, entries2) -> { entries1.addAll(entries2); return entries1; }));
		return compoundRules;
	}

	protected void loadDictionaryForInclusionTest(){
		boolean checkCompoundReplacement = affixData.isCheckCompoundReplacement();
		if(checkCompoundReplacement && dicInclusionTestWorker == null){
			Objects.requireNonNull(dicParser);
			Objects.requireNonNull(dictionaryBaseData);

			WordGeneratorAffixRules wordGeneratorAffixeRules = new WordGeneratorAffixRules(affParser, dicParser, dictionaryBaseData);
			dicInclusionTestWorker = new DictionaryInclusionTestWorker(dicParser, wordGeneratorAffixeRules, dictionaryBaseData, affParser);

			try{
				dicInclusionTestWorker.executeInline();
			}
			catch(Exception e){
				LOGGER.error(Backbone.MARKER_APPLICATION, "Cannot read dictionary: {}", ExceptionHelper.getMessage(e));
				LOGGER.error("Cannot read dictionary", e);
			}
		}
	}


	/** @return	A list of prefixes from first entry, suffixes from last entry, and terminals from both */
	private List<String> extractAffixesComponents(List<DictionaryEntry> compoundEntries, String compoundFlag){
		List<String[]> prefixes = compoundEntries.get(0).extractAllAffixes(affixData, false);
		List<String[]> suffixes = compoundEntries.get(compoundEntries.size() - 1).extractAllAffixes(affixData, false);

		Set<String> terminals = new HashSet<>(Arrays.asList(prefixes.get(2)));
		terminals.addAll(Arrays.asList(suffixes.get(2)));
		terminals.remove(compoundFlag);

		String compoundPrefixes = String.join(StringUtils.EMPTY, prefixes.get(0));
		String compoundSuffixes = String.join(StringUtils.EMPTY, suffixes.get(1));
		String compoundTerminals = String.join(StringUtils.EMPTY, terminals);
		return Arrays.asList(compoundPrefixes, compoundSuffixes, compoundTerminals);
	}

	private Production getBaseProduction(DictionaryEntry dicEntry){
		return Production.clone(dicEntry);
	}

	protected List<Production> getOnefoldProductions(DictionaryEntry dicEntry, boolean isCompound, boolean reverse)
			throws NoApplicableRuleException{
		List<String[]> applyAffixes = dicEntry.extractAllAffixes(affixData, reverse);
		return applyAffixRules(dicEntry, applyAffixes, isCompound);
	}

	private List<Production> getTwofoldProductions(List<Production> onefoldProductions, boolean isCompound, boolean reverse)
			throws NoApplicableRuleException{
		List<Production> twofoldProductions = new ArrayList<>();
		for(Production production : onefoldProductions)
			if(production.isCombineable()){
				List<Production> prods = getOnefoldProductions(production, isCompound, reverse);

				List<AffixEntry> appliedRules = production.getAppliedRules();
				for(Production prod : prods)
					//add parent derivations
					prod.prependAppliedRules(appliedRules);

				twofoldProductions.addAll(prods);
			}
		return twofoldProductions;
	}

	private void checkTwofoldCorrectness(List<Production> twofoldProductions) throws IllegalArgumentException{
		boolean complexPrefixes = affixData.isComplexPrefixes();
		for(Production prod : twofoldProductions){
			List<String[]> affixes = prod.extractAllAffixes(affixData, false);
			String[] aff = affixes.get(complexPrefixes? 1: 0);
			if(aff.length > 0){
				String overabundantAffixes = affixData.getFlagParsingStrategy().joinFlags(aff);
				throw new IllegalArgumentException("Twofold rule violated for '" + prod + " from " + prod.getRulesSequence()
					+ "' (" + prod.getRulesSequence() + " still has rules " + overabundantAffixes + ")");
			}
		}
	}

	protected List<Production> enforceOnlyInCompound(List<Production> productions){
		String onlyInCompoundFlag = affixData.getOnlyInCompoundFlag();

		if(StringUtils.isNotBlank(onlyInCompoundFlag)){
			Iterator<Production> itr = productions.iterator();
			while(itr.hasNext()){
				Production production = itr.next();

				if(!production.hasContinuationFlag(onlyInCompoundFlag))
					itr.remove();
			}
		}
		return productions;
	}

	private List<Production> enforceCircumfix(List<Production> lastfoldProductions){
		String circumfixFlag = affixData.getCircumfixFlag();
		if(circumfixFlag != null){
			Iterator<Production> itr = lastfoldProductions.iterator();
			while(itr.hasNext()){
				Production production = itr.next();

				List<AffixEntry> appliedRules = production.getAppliedRules();
				boolean rulesContainsCircumfixFlag = appliedRules.stream()
					.anyMatch(rule -> rule.hasContinuationFlag(circumfixFlag));
				if(rulesContainsCircumfixFlag){
					//check if at least one SFX and one PFX have the circumfix flag
					boolean suffixWithCircumfix = appliedRules.stream()
						.filter(AffixEntry::isSuffix)
						.anyMatch(rule -> rule.hasContinuationFlag(circumfixFlag));
					boolean prefixWithCircumfix = appliedRules.stream()
						.filter(Predicate.not(AffixEntry::isSuffix))
						.anyMatch(rule -> rule.hasContinuationFlag(circumfixFlag));
					if(suffixWithCircumfix ^ prefixWithCircumfix)
						itr.remove();
				}
			}
		}
		return lastfoldProductions;
	}

	protected void enforceNeedAffixFlag(List<Production> productions){
		String needAffixFlag = affixData.getNeedAffixFlag();
		if(needAffixFlag != null){
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
							lastSuffixNeedAffix = appliedRule.hasContinuationFlag(needAffixFlag);
						}
						else if(!lastPrefix && !appliedRule.isSuffix()){
							lastPrefix = true;
							lastPrefixNeedAffix = appliedRule.hasContinuationFlag(needAffixFlag);
						}
					}
					hasNeedAffixFlag = (!lastSuffix || lastSuffixNeedAffix) && (!lastPrefix || lastPrefixNeedAffix);
				}
				if(hasNeedAffixFlag)
					itr.remove();
			}
		}
	}

	private List<Production> applyAffixRules(DictionaryEntry dicEntry, List<String[]> applyAffixes, boolean isCompound)
			throws NoApplicableRuleException{
		String[] appliedAffixes = applyAffixes.get(0);
		//add COMPOUNDBEGIN, COMPOUNDMIDDLE, and COMPOUNDEND flags
		//FIXME
//		String[] postponedAffixes = ArrayUtils.addAll(applyAffixes.get(1), applyAffixes.get(3));
		String[] postponedAffixes = applyAffixes.get(1);

		String forbiddenWordFlag = affixData.getForbiddenWordFlag();

		List<Production> productions = new ArrayList<>();
		if(appliedAffixes.length > 0 && !dicEntry.hasContinuationFlag(forbiddenWordFlag)){
			String forbidCompoundFlag = affixData.getForbidCompoundFlag();
			String permitCompoundFlag = affixData.getPermitCompoundFlag();

			String word = dicEntry.getWord();

			for(String affix : appliedAffixes){
				RuleEntry rule = affixData.getData(affix);
				if(rule == null){
					if(affixData.isManagedByCompoundRule(affix))
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

				List<AffixEntry> applicableAffixes = AffixData.extractListOfApplicableAffixes(word, rule.getEntries());
				if(applicableAffixes.isEmpty())
					throw new NoApplicableRuleException("No applicable rules for " + affix + " from " + dicEntry.getWord());

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
					boolean hasForbidFlag = entry.hasContinuationFlag(forbidCompoundFlag);
					boolean hasPermitFlag = entry.hasContinuationFlag(permitCompoundFlag);
//if("SFX A 0 0/WXD .".equals(entry.toString()))
//	System.out.println("");
					if(isCompound && (hasForbidFlag || !hasPermitFlag))
						continue;
//					if(isCompound && hasForbidFlag)
//						continue;


					//produce the new word
					String newWord = entry.applyRule(word, affixData.isFullstrip());

					Production production = Production.createFromProduction(newWord, entry, dicEntry, postponedAffixes, rule.isCombineable());
					if(!production.hasContinuationFlag(forbiddenWordFlag))
						productions.add(production);
//					if(!production.hasContinuationFlag(forbiddenWordFlag)){
//						if(isCompound && !hasPermitFlag)
//							production.removeAffixes(affParser);
//
//						productions.add(production);
//					}
				}
			}
		}

		return productions;
	}

}
