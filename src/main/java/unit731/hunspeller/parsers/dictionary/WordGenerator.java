package unit731.hunspeller.parsers.dictionary;

import java.util.AbstractMap;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.languages.DictionaryBaseData;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.workers.DictionaryInclusionTestWorker;
import unit731.hunspeller.services.ExceptionHelper;
import unit731.hunspeller.services.PermutationsWithRepetitions;
import unit731.hunspeller.services.StringHelper;
import unit731.hunspeller.services.regexgenerator.HunspellRegexWordGenerator;


@Slf4j
public class WordGenerator{

	private static final String LEFT_PARENTHESIS = "(";
	private static final String RIGHT_PARENTHESIS = ")";

	private static final Map<StringHelper.Casing, Set<StringHelper.Casing>> COMPOUND_WORD_BOUNDARY_COLLISIONS = new EnumMap<>(StringHelper.Casing.class);
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
	private final DictionaryParser dicParser;
	private final DictionaryBaseData dictionaryBaseData;

	private DictionaryInclusionTestWorker dicInclusionTestWorker;
	private final Set<String> compoundAsReplacement = new HashSet<>();


	public WordGenerator(AffixParser affParser, DictionaryParser dicParser, DictionaryBaseData dictionaryBaseData){
		Objects.requireNonNull(affParser);

		this.affParser = affParser;
		this.dicParser = dicParser;
		this.dictionaryBaseData = dictionaryBaseData;
	}

	public List<Production> applyAffixRules(String line){
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		List<String> aliasesFlag = affParser.getData(AffixTag.ALIASES_FLAG);
		List<String> aliasesMorphologicalField = affParser.getData(AffixTag.ALIASES_MORPHOLOGICAL_FIELD);

		DictionaryEntry dicEntry = new DictionaryEntry(line, strategy, aliasesFlag, aliasesMorphologicalField);
		dicEntry.applyConversionTable(affParser::applyInputConversionTable);

		List<Production> productions = applyAffixRules(dicEntry, false);

		//convert using output table
		int size = productions.size();
		for(int i = 0; i < size; i ++){
			Production production = productions.get(i);
			production.applyConversionTable(affParser::applyInputConversionTable);
		}

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
	private List<Production> applyAffixRules(DictionaryEntry dicEntry, boolean isCompound) throws IllegalArgumentException, NoApplicableRuleException{
		String forbiddenWordFlag = affParser.getForbiddenWordFlag();
		if(dicEntry.hasContinuationFlag(forbiddenWordFlag))
			return Collections.<Production>emptyList();

		//extract base production
		Production baseProduction = getBaseProduction(dicEntry);
		if(log.isDebugEnabled()){
			log.debug("Base production:");
			log.debug("   {}", baseProduction);
		}

		//extract suffixed productions
		List<Production> onefoldProductions = getOnefoldProductions(baseProduction, isCompound, !affParser.isComplexPrefixes());
		if(log.isDebugEnabled()){
			log.debug("Suffix productions:");
			onefoldProductions.forEach(production -> log.debug("   {} from {}", production.toString(affParser.getFlagParsingStrategy()), production.getRulesSequence()));
		}

		List<Production> twofoldProductions = Collections.<Production>emptyList();
		if(!isCompound || affParser.allowTwofoldAffixesInCompound()){
			//extract prefixed productions
			twofoldProductions = getTwofoldProductions(onefoldProductions, isCompound, !affParser.isComplexPrefixes());
			if(log.isDebugEnabled() && !twofoldProductions.isEmpty()){
				log.debug("Prefix productions:");
				twofoldProductions.forEach(production -> log.debug("   {} from {}", production.toString(affParser.getFlagParsingStrategy()), production.getRulesSequence()));
			}
		}

		//extract lastfold productions
		List<Production> lastfoldProductions = new ArrayList<>();
		lastfoldProductions.add(baseProduction);
		lastfoldProductions.addAll(onefoldProductions);
		lastfoldProductions.addAll(twofoldProductions);
		lastfoldProductions = getTwofoldProductions(lastfoldProductions, isCompound, affParser.isComplexPrefixes());
		if(log.isDebugEnabled() && !lastfoldProductions.isEmpty()){
			log.debug("Twofold productions:");
			lastfoldProductions.forEach(production -> log.debug("   {} from {}", production.toString(affParser.getFlagParsingStrategy()), production.getRulesSequence()));
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
		if(isCompound)
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

		loadDictionaryForInclusionTest();

		//extract map flag -> dictionary entries
		Map<String, Set<DictionaryEntry>> inputs = extractCompoundRules(inputCompounds);

		String[] compoundRuleComponents = strategy.extractCompoundRule(compoundRule);
		checkCompoundRuleInputCorrectness(inputs, compoundRuleComponents);

		String[] compRule = strategy.extractCompoundRule(compoundRule);
		HunspellRegexWordGenerator regexWordGenerator = new HunspellRegexWordGenerator(compRule);
		//generate all the words that matches the given regex
		List<List<String>> permutations = regexWordGenerator.generateAll(2, limit);

		//generate compounds:
		List<List<List<Production>>> entries = new ArrayList<>();
		for(List<String> permutation : permutations){
			//expand permutation
			List<List<Production>> expandedPermutationEntries = new ArrayList<>();
			for(String flag : permutation)
				expandedPermutationEntries.add(inputs.get(flag).stream()
					.map(entry -> applyAffixRules(entry, true))
					.flatMap(List::stream)
					.collect(Collectors.toList()));
			if(!expandedPermutationEntries.stream().anyMatch(List::isEmpty))
				entries.add(expandedPermutationEntries);
		}

		return applyCompound(entries, limit);
	}

	/** Extract a map of flag > dictionary entry from input compounds */
	private Map<String, Set<DictionaryEntry>> extractCompoundRules(String[] inputCompounds){
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		int compoundMinimumLength = affParser.getCompoundMinimumLength();
		String forbiddenWordFlag = affParser.getForbiddenWordFlag();

		//extract map flag -> compounds
		Map<String, Set<DictionaryEntry>> compoundRules = new HashMap<>();
		for(String inputCompound : inputCompounds){
			DictionaryEntry dicEntry = new DictionaryEntry(inputCompound, strategy);
			dicEntry.applyConversionTable(affParser::applyInputConversionTable);

			Map<String, Set<DictionaryEntry>> distribution = dicEntry.distributeByCompoundRule(affParser);
			//merge the distribution with the others
			compoundRules = Stream.of(compoundRules, distribution)
				.flatMap(m -> m.entrySet().stream())
				.map(m -> {
					String key = m.getKey();
					Set<DictionaryEntry> value = m.getValue().stream()
						.filter(entry -> entry.getWord().length() >= compoundMinimumLength && !entry.hasContinuationFlag(forbiddenWordFlag))
						.collect(Collectors.toSet());
					return new AbstractMap.SimpleEntry<>(key, value);
				})
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (entries1, entries2) -> { entries1.addAll(entries2); return entries1; }));
		}
		return compoundRules;
	}

	private void checkCompoundRuleInputCorrectness(Map<String, Set<DictionaryEntry>> inputs, String[] compoundRuleComponents){
		for(String component : compoundRuleComponents){
			char chr = (component.length() == 1? component.charAt(0): 0);
			if(chr != '*' && chr != '?' && inputs.get(component) == null)
				throw new IllegalArgumentException("Missing word(s) for rule " + component + " in compound rule "
					+ StringUtils.join(compoundAsReplacement, StringUtils.EMPTY));
		}
	}


	/**
	 * Generates a list of stems for the provided flag from words in the dictionary marked with AffixTag.COMPOUND_FLAG
	 * 
	 * @param inputCompounds	List of compounds used to generate the production through the compound rule
	 * @param limit	Limit result count
	 * @param maxCompounds	Maximum compound count
	 * @return	The list of productions
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	public List<Production> applyCompoundFlag(String[] inputCompounds, int limit, int maxCompounds) throws IllegalArgumentException,
			NoApplicableRuleException{
		Objects.requireNonNull(inputCompounds);
		if(limit <= 0)
			throw new IllegalArgumentException("Limit cannot be non-positive");
		if(maxCompounds <= 0 && maxCompounds != PermutationsWithRepetitions.MAX_COMPOUNDS_INFINITY)
			throw new IllegalArgumentException("Max compounds cannot be non-positive");

		boolean forbidDuplications = affParser.isForbidDuplicationsInCompound();

		loadDictionaryForInclusionTest();

		//extract list of dictionary entries
		List<DictionaryEntry> inputs = extractCompoundFlags(inputCompounds);

		PermutationsWithRepetitions perm = new PermutationsWithRepetitions(inputs.size(), maxCompounds, forbidDuplications);
		List<int[]> permutations = perm.permutations(limit);

		//generate compounds:
		List<List<List<Production>>> entries = new ArrayList<>();
		for(int[] permutation : permutations){
			//expand permutation
			List<List<Production>> expandedPermutationEntries = Arrays.stream(permutation)
				.mapToObj(inputs::get)
				.map(entry -> applyAffixRules(entry, true))
				.collect(Collectors.toList());
			if(!expandedPermutationEntries.stream().anyMatch(List::isEmpty))
				entries.add(expandedPermutationEntries);
		}

		return applyCompound(entries, limit);
	}

	private List<DictionaryEntry> extractCompoundFlags(String[] inputCompounds){
		int compoundMinimumLength = affParser.getCompoundMinimumLength();
		String forbiddenWordFlag = affParser.getForbiddenWordFlag();

		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		List<DictionaryEntry> result = new ArrayList<>();
		for(String inputCompound : inputCompounds){
			DictionaryEntry dicEntry = new DictionaryEntry(inputCompound, strategy);
			dicEntry.applyConversionTable(affParser::applyInputConversionTable);

			//filter input set by minimum length and forbidden flag
			if(dicEntry.getWord().length() >= compoundMinimumLength && !dicEntry.hasContinuationFlag(forbiddenWordFlag))
				result.add(dicEntry);
		}
		return result;
	}

	private List<Production> applyCompound(List<List<List<Production>>> entries, int limit) throws IllegalArgumentException, NoApplicableRuleException{
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		String compoundFlag = affParser.getCompoundFlag();
		String forbiddenWordFlag = affParser.getForbiddenWordFlag();
		String forceCompoundUppercaseFlag = affParser.getForceCompoundUppercaseFlag();
		boolean hasForbidCompoundFlag = (affParser.getForbidCompoundFlag() != null);
		boolean hasPermitCompoundFlag = (affParser.getPermitCompoundFlag() != null);
		boolean forbidDifferentCasesInCompound = affParser.isForbidDifferentCasesInCompound();
		boolean checkCompoundReplacement = affParser.isCheckCompoundReplacement();
		boolean forbidTriples = affParser.isForbidTriplesInCompound();
		boolean simplifyTriples = affParser.isSimplifyTriplesInCompound();
		boolean allowTwofoldAffixesInCompound = affParser.allowTwofoldAffixesInCompound();

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
					if(sb.length() > 0){
						if(forbidDifferentCasesInCompound){
							if(lastWordCasing == null)
								lastWordCasing = StringHelper.classifyCasing(sb.toString());
							StringHelper.Casing nextWord = StringHelper.classifyCasing(nextCompound);

							char lastChar = sb.charAt(sb.length() - 1);
							char nextChar = nextCompound.charAt(0);
							if(Character.isAlphabetic(lastChar) && Character.isAlphabetic(nextChar)){
								Set<StringHelper.Casing> collisions = COMPOUND_WORD_BOUNDARY_COLLISIONS.get(lastWordCasing);
								if(collisions != null && collisions.contains(nextWord)){
									sb.setLength(0);
									break;
								}
							}

							lastWordCasing = nextWord;
						}
					}
					sb.append(nextCompound);
				}

				if(sb.length() > 0 && (!checkCompoundReplacement || !existsCompoundAsReplacement(sb.toString()))){
					List<String> continuationFlags = extractAffixesComponents(compoundEntries, compoundFlag);
					if(!continuationFlags.contains(forbiddenWordFlag)){
						String word = sb.toString();
						String flags = (!continuationFlags.isEmpty()? String.join(StringUtils.EMPTY, continuationFlags): null);
						Production p = new Production(word, flags, compoundEntries, strategy);
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
			production.applyConversionTable(affParser::applyOutputConversionTable);
			production.capitalizeIfContainsFlag(forceCompoundUppercaseFlag);
			production.removeContinuationFlag(forceCompoundUppercaseFlag);
		}

		if(log.isTraceEnabled())
			productions.forEach(production -> log.trace("Produced word: {}", production));

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

		List<Pair<String, String>> replacementTable = affParser.getReplacementTable();
		if(word.length() >= 2 && replacementTable != null && !replacementTable.isEmpty())
			for(Pair<String, String> entry : replacementTable){
				String pattern = entry.getKey();
				String value = entry.getValue();

				int idx = -1;
				int patternLength = pattern.length();
				StringBuilder sb = new StringBuilder();
				//search every occurence of the pattern in the word
				while((idx = word.indexOf(pattern, idx + 1)) >= 0){
					sb.setLength(0);
					sb.append(word);
					sb.replace(idx, idx + patternLength, value);
					String candidate = sb.toString();
					if(dicInclusionTestWorker.isInDictionary(candidate)){
						compoundAsReplacement.add(word);

						return true;
					}
				}
			}
		return exists;
	}


	/**
	 * Generates a list of stems for the provided flag from words in the dictionary marked with AffixTag.COMPOUND_BEGIN, AffixTag.COMPOUND_MIDDLE,
	 * and AffixTag.COMPOUND_END
	 * 
	 * @param inputCompounds	List of compounds used to generate the production through the compound rule
	 * @param limit	Limit result count
	 * @return	The list of productions
	 * @throws NoApplicableRuleException	If there is a rule that does not apply to the word
	 */
	public List<Production> applyCompoundBeginMiddleEnd(String[] inputCompounds, int limit) throws IllegalArgumentException,
			NoApplicableRuleException{
		Objects.requireNonNull(inputCompounds);
		if(limit <= 0)
			throw new IllegalArgumentException("Limit cannot be non-positive");

		String compoundBeginFlag = affParser.getCompoundBeginFlag();
		String compoundMiddleFlag = affParser.getCompoundMiddleFlag();
		String compoundEndFlag = affParser.getCompoundEndFlag();

		loadDictionaryForInclusionTest();

		//extract map flag -> dictionary entries
		Map<String, Set<DictionaryEntry>> inputs = extractCompoundBeginMiddleEnd(inputCompounds, compoundBeginFlag, compoundMiddleFlag, compoundEndFlag);

		checkCompoundBeginMiddleEndInputCorrectness(inputs);

		String[] compoundRule = new String[]{compoundBeginFlag, "*",
			compoundMiddleFlag, "*",
			compoundEndFlag, "*"};
		HunspellRegexWordGenerator regexWordGenerator = new HunspellRegexWordGenerator(compoundRule);
		//generate all the words that matches the given regex
		List<List<String>> permutations = regexWordGenerator.generateAll(2, limit);

		//generate compounds:
		List<List<List<Production>>> entries = new ArrayList<>();
		for(List<String> permutation : permutations){
			//expand permutation
			List<List<Production>> expandedPermutationEntries = new ArrayList<>();
			for(String flag : permutation)
				expandedPermutationEntries.add(inputs.get(flag).stream()
					.map(entry -> applyAffixRules(entry, true))
//FIXME
					.map(entry -> entry.stream().filter(prod -> prod.hasContinuationFlag(flag)).collect(Collectors.toList()))
					.flatMap(List::stream)
					.collect(Collectors.toList()));
			if(!expandedPermutationEntries.stream().anyMatch(List::isEmpty))
				entries.add(expandedPermutationEntries);
		}

		return applyCompound(entries, limit);
	}

	private Map<String, Set<DictionaryEntry>> extractCompoundBeginMiddleEnd(String[] inputCompounds, String compoundBeginFlag, String compoundMiddleFlag, String compoundEndFlag){
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		int compoundMinimumLength = affParser.getCompoundMinimumLength();
		String forbiddenWordFlag = affParser.getForbiddenWordFlag();

		//extract map flag -> compounds
		Map<String, Set<DictionaryEntry>> compoundRules = new HashMap<>();
		for(String inputCompound : inputCompounds){
			DictionaryEntry dicEntry = new DictionaryEntry(inputCompound, strategy);
			dicEntry.applyConversionTable(affParser::applyInputConversionTable);

			List<Production> productions = applyAffixRules(dicEntry, false);
			for(Production production : productions){
				Map<String, Set<DictionaryEntry>> distribution = production.distributeByCompoundBeginMiddleEnd(affParser, compoundBeginFlag, compoundMiddleFlag, compoundEndFlag);
				//merge the distribution with the others
				compoundRules = Stream.of(compoundRules, distribution)
					.flatMap(m -> m.entrySet().stream())
					.map(m -> {
						String key = m.getKey();
						Set<DictionaryEntry> value = m.getValue().stream()
							.filter(entry -> entry.getWord().length() >= compoundMinimumLength && !entry.hasContinuationFlag(forbiddenWordFlag))
							.collect(Collectors.toSet());
						return new AbstractMap.SimpleEntry<>(key, value);
					})
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (entries1, entries2) -> { entries1.addAll(entries2); return entries1; }));
			}
		}
		return compoundRules;
	}

	private void checkCompoundBeginMiddleEndInputCorrectness(Map<String, Set<DictionaryEntry>> inputs){
		for(Map.Entry<String, Set<DictionaryEntry>> entry : inputs.entrySet())
			if(entry.getValue().isEmpty())
				throw new IllegalArgumentException("Missing word(s) for rule " + entry.getKey() + " in compound begin-middle-end");
	}

	private void loadDictionaryForInclusionTest(){
		boolean checkCompoundReplacement = affParser.isCheckCompoundReplacement();
		if(checkCompoundReplacement && dicInclusionTestWorker == null){
			Objects.requireNonNull(dicParser);
			Objects.requireNonNull(dictionaryBaseData);

			dicInclusionTestWorker = new DictionaryInclusionTestWorker(dicParser, this, dictionaryBaseData, affParser);

			try{
				dicInclusionTestWorker.executeInline();
			}
			catch(Exception e){
				log.error(Backbone.MARKER_APPLICATION, "Cannot read dictionary: {}", ExceptionHelper.getMessage(e));
				log.error("Cannot read dictionary", e);
			}
		}
	}


	/** @return	A list of prefixes from first entry, suffixes from last entry, and terminals from both */
	private List<String> extractAffixesComponents(List<DictionaryEntry> compoundEntries, String compoundFlag){
		List<String[]> prefixes = compoundEntries.get(0).extractAllAffixes(affParser, false);
		List<String[]> suffixes = compoundEntries.get(compoundEntries.size() - 1).extractAllAffixes(affParser, false);

		Set<String> terminals = new HashSet<>(Arrays.asList(prefixes.get(2)));
		terminals.addAll(Arrays.asList(suffixes.get(2)));
		terminals.remove(compoundFlag);

		String compoundPrefixes = String.join(StringUtils.EMPTY, prefixes.get(0));
		String compoundSuffixes = String.join(StringUtils.EMPTY, suffixes.get(1));
		String compoundTerminals = String.join(StringUtils.EMPTY, terminals);
		return Arrays.asList(compoundPrefixes, compoundSuffixes, compoundTerminals);
	}

	private Production getBaseProduction(DictionaryEntry dicEntry){
		return new Production(dicEntry);
	}

	private List<Production> getOnefoldProductions(DictionaryEntry dicEntry, boolean isCompound, boolean reverse) throws NoApplicableRuleException{
		List<String[]> applyAffixes = dicEntry.extractAllAffixes(affParser, reverse);
		return applyAffixRules(dicEntry, applyAffixes, isCompound);
	}

	private List<Production> getTwofoldProductions(List<Production> onefoldProductions, boolean isCompound, boolean reverse) throws NoApplicableRuleException{
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
		boolean complexPrefixes = affParser.isComplexPrefixes();
		for(Production prod : twofoldProductions){
			List<String[]> affixes = prod.extractAllAffixes(affParser, false);
			String[] aff = affixes.get(complexPrefixes? 1: 0);
			if(aff.length > 0){
				String overabundantAffixes = affParser.getFlagParsingStrategy().joinFlags(aff);
				throw new IllegalArgumentException("Twofold rule violated for '" + prod + " from " + prod.getRulesSequence()
					+ "' (" + prod.getRulesSequence() + " still has rules " + overabundantAffixes + ")");
			}
		}
	}

	private List<Production> enforceOnlyInCompound(List<Production> productions){
		String onlyInCompoundFlag = affParser.getOnlyInCompoundFlag();

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
		String circumfixFlag = affParser.getCircumfixFlag();
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
						.filter(rule -> rule.isSuffix())
						.anyMatch(rule -> rule.hasContinuationFlag(circumfixFlag));
					boolean prefixWithCircumfix = appliedRules.stream()
						.filter(rule -> !rule.isSuffix())
						.anyMatch(rule -> rule.hasContinuationFlag(circumfixFlag));
					if(suffixWithCircumfix ^ prefixWithCircumfix)
						itr.remove();
				}
			}
		}
		return lastfoldProductions;
	}

	private void enforceNeedAffixFlag(List<Production> productions){
		String needAffixFlag = affParser.getNeedAffixFlag();
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

	private List<Production> applyAffixRules(DictionaryEntry dicEntry, List<String[]> applyAffixes, boolean isCompound) throws NoApplicableRuleException{
		String[] appliedAffixes = applyAffixes.get(0);
		//add COMPOUNDBEGIN, COMPOUNDMIDDLE, and COMPOUNDEND flags
		//FIXME
//		String[] postponedAffixes = ArrayUtils.addAll(applyAffixes.get(1), applyAffixes.get(3));
		String[] postponedAffixes = applyAffixes.get(1);

		String forbiddenWordFlag = affParser.getForbiddenWordFlag();

		List<Production> productions = new ArrayList<>();
		if(appliedAffixes.length > 0 && !dicEntry.hasContinuationFlag(forbiddenWordFlag)){
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
					boolean hasForbidFlag = entry.hasContinuationFlag(forbidCompoundFlag);
					boolean hasPermitFlag = entry.hasContinuationFlag(permitCompoundFlag);
//if("SFX A 0 0/WXD .".equals(entry.toString()))
//	System.out.println("");
					if(isCompound && (hasForbidFlag || !hasPermitFlag))
						continue;
//					if(isCompound && hasForbidFlag)
//						continue;


					//produce the new word
					String newWord = entry.applyRule(word, affParser.isFullstrip());

					Production production = new Production(newWord, entry, dicEntry, postponedAffixes, rule.isCombineable(), strategy);
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
