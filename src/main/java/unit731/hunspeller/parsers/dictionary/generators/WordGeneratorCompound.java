package unit731.hunspeller.parsers.dictionary.generators;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.languages.DictionaryBaseData;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.vos.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.DictionaryInclusionTestWorker;
import unit731.hunspeller.services.ExceptionHelper;
import unit731.hunspeller.services.StringHelper;


abstract class WordGeneratorCompound extends WordGeneratorBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordGeneratorCompound.class);

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


	protected final AffixParser affParser;
	protected final DictionaryParser dicParser;
	protected final DictionaryBaseData dictionaryBaseData;
	protected final WordGenerator wordGenerator;

	protected DictionaryInclusionTestWorker dicInclusionTestWorker;
	protected final Set<String> compoundAsReplacement = new HashSet<>();


	WordGeneratorCompound(AffixParser affParser, DictionaryParser dicParser, DictionaryBaseData dictionaryBaseData,
			WordGenerator wordGenerator){
		super(affParser.getAffixData());

		this.affParser = affParser;
		this.dicParser = dicParser;
		this.dictionaryBaseData = dictionaryBaseData;
		this.wordGenerator = wordGenerator;
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

			dicInclusionTestWorker = new DictionaryInclusionTestWorker(dicParser, wordGenerator, dictionaryBaseData, affParser);

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

}
