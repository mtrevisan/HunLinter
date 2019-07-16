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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.vos.DictionaryEntry;
import unit731.hunspeller.parsers.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.DictionaryInclusionTestWorker;
import unit731.hunspeller.services.ExceptionHelper;
import unit731.hunspeller.services.SetHelper;
import unit731.hunspeller.services.StringHelper;


abstract class WordGeneratorCompound extends WordGeneratorBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordGeneratorCompound.class);

	private static final Map<StringHelper.Casing, Set<StringHelper.Casing>> COMPOUND_WORD_BOUNDARY_COLLISIONS
		= new EnumMap<>(StringHelper.Casing.class);
	static{
		Set<StringHelper.Casing> lowerOrTitleCase = SetHelper.setOf(StringHelper.Casing.TITLE_CASE, StringHelper.Casing.ALL_CAPS,
			StringHelper.Casing.CAMEL_CASE, StringHelper.Casing.PASCAL_CASE);
		COMPOUND_WORD_BOUNDARY_COLLISIONS.put(StringHelper.Casing.LOWER_CASE, lowerOrTitleCase);
		COMPOUND_WORD_BOUNDARY_COLLISIONS.put(StringHelper.Casing.TITLE_CASE, lowerOrTitleCase);
		Set<StringHelper.Casing> allCaps = SetHelper.setOf(StringHelper.Casing.LOWER_CASE, StringHelper.Casing.TITLE_CASE,
			StringHelper.Casing.CAMEL_CASE, StringHelper.Casing.PASCAL_CASE);
		COMPOUND_WORD_BOUNDARY_COLLISIONS.put(StringHelper.Casing.ALL_CAPS, allCaps);
	}


	protected final DictionaryParser dicParser;
	protected final WordGenerator wordGenerator;

	private DictionaryInclusionTestWorker dicInclusionTestWorker;
	private final Set<String> compoundAsReplacement = new HashSet<>();


	WordGeneratorCompound(final AffixData affixData, final DictionaryParser dicParser, final WordGenerator wordGenerator){
		super(affixData);

		this.dicParser = dicParser;
		this.wordGenerator = wordGenerator;
	}

	protected List<List<List<Production>>> generateCompounds(final List<List<String>> permutations, final Map<String, Set<DictionaryEntry>> inputs){
		final List<List<List<Production>>> entries = new ArrayList<>();
		final Map<String, List<Production>> dicEntries = new HashMap<>();
		outer:
		for(final List<String> permutation : permutations){
			//expand permutation
			final List<List<Production>> expandedPermutationEntries = new ArrayList<>();
			for(final String flag : permutation){
				if(!dicEntries.containsKey(flag)){
					final Set<DictionaryEntry> input = inputs.get(flag);
					dicEntries.put(flag, input.stream()
						.map(entry -> applyAffixRules(entry, true, null))
						.map(entry -> entry.stream().filter(prod -> prod.hasContinuationFlag(flag)).collect(Collectors.toList()))
						.flatMap(List::stream)
						.collect(Collectors.toList()));
				}
				final List<Production> de = dicEntries.get(flag);
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

	protected List<Production> applyCompound(final List<List<List<Production>>> entries, final int limit) throws IllegalArgumentException{
		final String compoundFlag = affixData.getCompoundFlag();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();
		final String forceCompoundUppercaseFlag = affixData.getForceCompoundUppercaseFlag();
		final boolean checkCompoundReplacement = affixData.isCheckCompoundReplacement();

		compoundAsReplacement.clear();

		final StringBuffer sb = new StringBuffer();
		final Set<Production> productions = new LinkedHashSet<>();
		//generate compounds:
		for(final List<List<Production>> entry : entries){
			//compose compound:
			boolean completed = false;
			final int[] indexes = new int[entry.size()];
			while(!completed){
				final List<DictionaryEntry> compoundEntries = composeCompound(indexes, entry, sb);

				if(sb.length() > 0 && (!checkCompoundReplacement || !existsCompoundAsReplacement(sb.toString()))){
					final List<String> continuationFlags = extractCompoundFlagsByComponents(compoundEntries, compoundFlag);
					if(!continuationFlags.contains(forbiddenWordFlag)){
						final String compoundWord = sb.toString();
						addToProductions(compoundWord, compoundEntries, continuationFlags, productions);

						if(productions.size() >= limit)
							break;
					}
				}


				completed = getNextTuple(indexes, entry);
			}
		}

		compoundAsReplacement.clear();

		applyOutputConversions(productions, forceCompoundUppercaseFlag);

		if(LOGGER.isTraceEnabled())
			productions.forEach(production -> LOGGER.trace("Produced word: {}", production));

		return limitResponse(productions, limit);
	}

	private void applyOutputConversions(final Set<Production> productions, final String forceCompoundUppercaseFlag){
		//convert using output table
		for(final Production production : productions){
			production.applyOutputConversionTable(affixData::applyOutputConversionTable);
			production.capitalizeIfContainsFlag(forceCompoundUppercaseFlag);
			production.removeContinuationFlag(forceCompoundUppercaseFlag);
		}
	}

	private List<Production> limitResponse(final Set<Production> productions, final int limit){
		List<Production> response = new ArrayList<>(productions);
		if(response.size() > limit)
			response = response.subList(0, limit);
		return response;
	}

	private void addToProductions(final String compoundWord, final List<DictionaryEntry> compoundEntries, final List<String> continuationFlags,
			final Set<Production> productions) throws IllegalArgumentException{
		final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		final boolean hasForbidCompoundFlag = (affixData.getForbidCompoundFlag() != null);
		final boolean hasPermitCompoundFlag = (affixData.getPermitCompoundFlag() != null);
		final boolean allowTwofoldAffixesInCompound = affixData.allowTwofoldAffixesInCompound();

		final String flags = (!continuationFlags.isEmpty()? String.join(StringUtils.EMPTY, continuationFlags): null);
		final Production p = Production.createFromCompound(compoundWord, flags, compoundEntries, strategy);
		if(hasForbidCompoundFlag || hasPermitCompoundFlag)
			productions.add(p);
		else{
			//add boundary affixes
			final List<Production> prods = applyAffixRules(p, false, null);
			
			//remove twofold because they're not allowed in compounds
			if(!allowTwofoldAffixesInCompound)
				removeTwofolds(prods);
			
			productions.addAll(prods);
		}
	}

	private List<DictionaryEntry> composeCompound(final int[] indexes, final List<List<Production>> entry, final StringBuffer sb){
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();
		final boolean forbidDifferentCasesInCompound = affixData.isForbidDifferentCasesInCompound();
		final boolean forbidTriples = affixData.isForbidTriplesInCompound();
		final boolean simplifyTriples = affixData.isSimplifyTriplesInCompound();

		final List<DictionaryEntry> compoundEntries = new ArrayList<>();

		sb.setLength(0);
		StringHelper.Casing lastWordCasing = null;
		for(int i = 0; i < indexes.length; i ++){
			final Production next = entry.get(i).get(indexes[i]);
			
			//skip forbidden words
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
				else /*if(forbidTriples)*/{
					sb.setLength(0);
					break;
				}
			}
			//enforce forbidden case if CHECKCOMPOUNDCASE is set
			if(sb.length() > 0 && forbidDifferentCasesInCompound){
				if(lastWordCasing == null)
					lastWordCasing = StringHelper.classifyCasing(sb.toString());
				final StringHelper.Casing nextWordCasing = StringHelper.classifyCasing(nextCompound);
				
				final char lastChar = sb.charAt(sb.length() - 1);
				nextCompound = enforceNextCompoundCase(lastChar, nextCompound, lastWordCasing, nextWordCasing);
				
				lastWordCasing = nextWordCasing;
			}
			
			sb.append(nextCompound);
		}
		return compoundEntries;
	}

	private boolean containsTriple(final StringBuffer sb, final String compound){
		int count = 0;
		final int size = sb.length() - 1;
		if(size > 1){
			final String interCompounds = sb.substring(Math.max(size - 1, 0), size + 1).concat(compound.substring(0, Math.min(compound.length(), 2)));

			char lastChar = 0;
			final int len = interCompounds.length();
			for(int i = 0; count < 3 && i < len; i ++){
				final char chr = interCompounds.charAt(i);
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

	private String enforceNextCompoundCase(final char lastChar, String nextCompound, final StringHelper.Casing lastWordCasing,
			final StringHelper.Casing nextWordCasing){
		final char nextChar = nextCompound.charAt(0);
		if(Character.isAlphabetic(lastChar) && Character.isAlphabetic(nextChar)){
			final Set<StringHelper.Casing> collisions = COMPOUND_WORD_BOUNDARY_COLLISIONS.get(lastWordCasing);
			//convert nextChar to lowercase/uppercase and go on
			if(collisions != null && collisions.contains(nextWordCasing))
				nextCompound = (Character.isUpperCase(lastChar)? StringUtils.capitalize(nextCompound):
					StringUtils.uncapitalize(nextCompound));
		}
		return nextCompound;
	}

	/** @return	A list of prefixes from first entry, suffixes from last entry, and terminals from both */
	private List<String> extractCompoundFlagsByComponents(final List<DictionaryEntry> compoundEntries, final String compoundFlag){
		final List<String[]> prefixes = compoundEntries.get(0).extractAllAffixes(affixData, false);
		final List<String[]> suffixes = compoundEntries.get(compoundEntries.size() - 1).extractAllAffixes(affixData, false);

		final Set<String> terminals = SetHelper.setOf(prefixes.get(2));
		terminals.addAll(Arrays.asList(suffixes.get(2)));
		terminals.remove(compoundFlag);

		final String compoundPrefixes = String.join(StringUtils.EMPTY, prefixes.get(0));
		final String compoundSuffixes = String.join(StringUtils.EMPTY, suffixes.get(1));
		final String compoundTerminals = String.join(StringUtils.EMPTY, terminals);
		return Arrays.asList(compoundPrefixes, compoundSuffixes, compoundTerminals);
	}

	private void removeTwofolds(final List<Production> prods){
		final Iterator<Production> itr = prods.iterator();
		while(itr.hasNext())
			if(itr.next().isTwofolded())
				itr.remove();
	}

	//is word a non compound with a REP substitution (see checkcompoundrep)?
	private boolean existsCompoundAsReplacement(final String word){
		boolean exists = false;

		for(final String cr : compoundAsReplacement)
			if(word.contains(cr)){
				exists = true;
				break;
			}

		if(!exists && word.length() >= 2){
			final List<String> conversions = affixData.applyReplacementTable(word);
			for(final String candidate : conversions)
				if(dicInclusionTestWorker.isInDictionary(candidate)){
					compoundAsReplacement.add(word);

					exists = true;
					break;
				}
		}

		return exists;
	}

	private boolean getNextTuple(final int[] indexes, final List<List<Production>> entry){
		//obtain next tuple
		boolean completed = false;
		for(int i = indexes.length - 1; !completed && i >= 0; i --){
			indexes[i] ++;
			if(indexes[i] < entry.get(i).size())
				break;
			
			indexes[i] = 0;
			if(i == 0)
				completed = true;
		}
		return completed;
	}

	/** Merge the distribution with the others */
	protected Map<String, Set<DictionaryEntry>> mergeDistributions(Map<String, Set<DictionaryEntry>> compoundRules,
			final Map<String, Set<DictionaryEntry>> distribution, final int compoundMinimumLength, final String forbiddenWordFlag){
		compoundRules = Stream.of(compoundRules, distribution)
			.flatMap(m -> m.entrySet().stream())
			.map(m -> {
				final String key = m.getKey();
				final Set<DictionaryEntry> value = m.getValue().stream()
					.filter(entry -> entry.getWord().length() >= compoundMinimumLength && !entry.hasContinuationFlag(forbiddenWordFlag))
					.collect(Collectors.toSet());
				return new AbstractMap.SimpleEntry<>(key, value);
			})
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
				(entries1, entries2) -> { entries1.addAll(entries2); return entries1; }));
		return compoundRules;
	}

	protected void loadDictionaryForInclusionTest(){
		if(dicInclusionTestWorker == null && affixData.isCheckCompoundReplacement()){
			dicInclusionTestWorker = new DictionaryInclusionTestWorker(affixData.getLanguage(), dicParser, wordGenerator);

			try{
				dicInclusionTestWorker.executeInline();
			}
			catch(final Exception e){
				LOGGER.error(Backbone.MARKER_APPLICATION, "Cannot read dictionary: {}", ExceptionHelper.getMessage(e));
				LOGGER.error("Cannot read dictionary", e);
			}
		}
	}

}
