/**
 * Copyright (c) 2019-2021 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.parsers.dictionary.generators;

import io.github.mtrevisan.hunlinter.datastructures.ArraySet;
import io.github.mtrevisan.hunlinter.datastructures.SetHelper;
import io.github.mtrevisan.hunlinter.languages.DictionaryCorrectnessChecker;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.vos.Affixes;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.services.system.LoopHelper;
import io.github.mtrevisan.hunlinter.services.text.StringHelper;
import io.github.mtrevisan.hunlinter.workers.dictionary.DictionaryInclusionTestWorker;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;


abstract class WordGeneratorCompound extends WordGeneratorBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordGeneratorCompound.class);

	private static final Map<StringHelper.Casing, Set<StringHelper.Casing>> COMPOUND_WORD_BOUNDARY_COLLISIONS
		= new EnumMap<>(StringHelper.Casing.class);
	static{
		final Set<StringHelper.Casing> lowerOrTitleCase = SetHelper.setOf(StringHelper.Casing.TITLE_CASE, StringHelper.Casing.ALL_CAPS,
			StringHelper.Casing.CAMEL_CASE, StringHelper.Casing.PASCAL_CASE);
		COMPOUND_WORD_BOUNDARY_COLLISIONS.put(StringHelper.Casing.LOWER_CASE, lowerOrTitleCase);
		COMPOUND_WORD_BOUNDARY_COLLISIONS.put(StringHelper.Casing.TITLE_CASE, lowerOrTitleCase);
		final Set<StringHelper.Casing> allCaps = SetHelper.setOf(StringHelper.Casing.LOWER_CASE, StringHelper.Casing.TITLE_CASE,
			StringHelper.Casing.CAMEL_CASE, StringHelper.Casing.PASCAL_CASE);
		COMPOUND_WORD_BOUNDARY_COLLISIONS.put(StringHelper.Casing.ALL_CAPS, allCaps);
	}


	private final DictionaryParser dicParser;
	private final WordGenerator wordGenerator;

	private DictionaryInclusionTestWorker dicInclusionTestWorker;
	private final Collection<String> compoundAsReplacement = new HashSet<>(0);


	WordGeneratorCompound(final AffixData affixData, final DictionaryParser dicParser, final WordGenerator wordGenerator,
			final DictionaryCorrectnessChecker checker){
		super(affixData, checker);

		this.dicParser = dicParser;
		this.wordGenerator = wordGenerator;
	}

	protected List<List<List<Inflection>>> generateCompounds(final Iterable<List<String>> permutations,
			final Map<String, List<DictionaryEntry>> inputs){
		final List<List<List<Inflection>>> entries = new ArrayList<>();
		final Map<String, List<Inflection>> dicEntries = new HashMap<>();
		outer:
		for(final List<String> permutation : permutations){
			//expand permutation
			final List<List<Inflection>> expandedPermutationEntries = new ArrayList<>();
			for(final String flag : permutation){
				if(!dicEntries.containsKey(flag)){
					final List<Inflection> dicEntriesPerFlag = new ArrayList<>(0);
					for(final DictionaryEntry entry : inputs.get(flag)){
						final List<Inflection> inflections = applyAffixRules(entry, true, null);

						final int size = (inflections != null? inflections.size(): 0);
						for(int i = 0; i < size; i ++){
							final Inflection inflection = inflections.get(i);
							if(inflection.hasContinuationFlag(flag))
								dicEntriesPerFlag.add(inflection);
						}
					}
					dicEntries.put(flag, dicEntriesPerFlag);
				}

				final List<Inflection> dicEntriesPerFlag = dicEntries.get(flag);
				if(!dicEntriesPerFlag.isEmpty())
					expandedPermutationEntries.add(dicEntriesPerFlag);
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

	protected List<Inflection> applyCompound(final Iterable<List<List<Inflection>>> entries, final int limit){
		final String compoundFlag = affixData.getCompoundFlag();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();
		final String forceCompoundUppercaseFlag = affixData.getForceCompoundUppercaseFlag();
		final boolean checkCompoundReplacement = affixData.isCheckCompoundReplacement();

		compoundAsReplacement.clear();

		final StringBuffer sb = new StringBuffer();
		final ArraySet<Inflection> inflections = new ArraySet<>();
		//generate compounds:
		for(final List<List<Inflection>> entry : entries){
			//compose compound:
			boolean completed = false;
			final int[] indexes = new int[entry.size()];
			while(!completed){
				final DictionaryEntry[] compoundEntries = composeCompound(indexes, entry, sb);

				if(!sb.isEmpty() && (!checkCompoundReplacement || !existsCompoundAsReplacement(sb.toString()))){
					final List<List<String>> continuationFlags = extractCompoundFlagsByComponent(compoundEntries, compoundFlag);
					if(forbiddenWordFlag == null
							|| !continuationFlags.get(Affixes.INDEX_PREFIXES).contains(forbiddenWordFlag)
							&& !continuationFlags.get(Affixes.INDEX_SUFFIXES).contains(forbiddenWordFlag)
							&& !continuationFlags.get(Affixes.INDEX_TERMINALS).contains(forbiddenWordFlag)){
						final String compoundWord = sb.toString();
						final List<Inflection> newInflections = generateInflections(compoundWord, compoundEntries, continuationFlags);

						inflections.addAll(newInflections);
						if(inflections.size() > limit)
							break;
					}
				}


				completed = (inflections.size() >= limit || getNextTuple(indexes, entry));
			}
		}

		compoundAsReplacement.clear();

		applyOutputConversions(inflections, forceCompoundUppercaseFlag);

		if(LOGGER.isTraceEnabled())
			for(final Inflection inflection : inflections)
				LOGGER.trace("Inflected word: {}", inflection);

		return limitResponse(inflections, limit);
	}

	private void applyOutputConversions(final Iterable<Inflection> inflections, final String forceCompoundUppercaseFlag){
		final Function<String, String> applyOutputConversionTable = affixData::applyOutputConversionTable;
		//convert using output table
		for(final Inflection inflection : inflections){
			inflection.applyOutputConversionTable(applyOutputConversionTable);
			inflection.capitalizeIfContainsFlag(forceCompoundUppercaseFlag);
			inflection.removeContinuationFlag(forceCompoundUppercaseFlag);
		}
	}

	private List<Inflection> limitResponse(final Set<Inflection> inflections, final int limit){
		return new ArrayList<>(inflections).subList(0, Math.min(inflections.size(), limit));
	}

	private List<Inflection> generateInflections(final String compoundWord, final DictionaryEntry[] compoundEntries,
			final List<List<String>> continuationFlags){
		final boolean hasForbidCompoundFlag = (affixData.getForbidCompoundFlag() != null);
		final boolean hasPermitCompoundFlag = (affixData.getPermitCompoundFlag() != null);
		final boolean allowTwofoldAffixesInCompound = affixData.allowTwofoldAffixesInCompound();

		final List<Inflection> inflections = new ArrayList<>(1);
		final List<String> flags = new ArrayList<>(continuationFlags.size());
		for(final List<String> continuationFlag : continuationFlags)
			flags.addAll(continuationFlag);
		final Inflection p = Inflection.createFromCompound(compoundWord, flags, compoundEntries);
		if(hasForbidCompoundFlag || hasPermitCompoundFlag)
			inflections.add(p);
		else{
			//add boundary affixes
			inflections.addAll(applyAffixRules(p, false, null));

			if(!allowTwofoldAffixesInCompound)
				//remove twofold because they're not allowed in compounds
				removeTwofolds(inflections);
		}
		return inflections;
	}

	private DictionaryEntry[] composeCompound(final int[] indexes, final List<List<Inflection>> entry, final StringBuffer sb){
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();
		final boolean forbidDifferentCasesInCompound = affixData.isForbidDifferentCasesInCompound();
		final boolean forbidTriples = affixData.isForbidTriplesInCompound();
		final boolean simplifyTriples = affixData.isSimplifyTriplesInCompound();

		DictionaryEntry[] compoundEntries = new DictionaryEntry[0];

		sb.setLength(0);
		StringHelper.Casing lastWordCasing = null;
		for(int i = 0; i < indexes.length; i ++){
			final Inflection next = entry.get(i).get(indexes[i]);

			//skip forbidden words
			if(next.hasContinuationFlag(forbiddenWordFlag)){
				sb.setLength(0);
				break;
			}

			compoundEntries = ArrayUtils.add(compoundEntries, next);

			String nextCompound = next.getWord();
			final boolean containsTriple = containsTriple(sb, nextCompound);
			//enforce simplification of triples if SIMPLIFIEDTRIPLE is set
			if(containsTriple && simplifyTriples)
				nextCompound = nextCompound.substring(1);
			//enforce not containment of a triple if CHECKCOMPOUNDTRIPLE is set
			else if(containsTriple && forbidTriples){
				sb.setLength(0);
				break;
			}
			//enforce forbidden case if CHECKCOMPOUNDCASE is set
			if(!sb.isEmpty() && forbidDifferentCasesInCompound){
				if(lastWordCasing == null)
					lastWordCasing = StringHelper.classifyCasing(sb.toString());
				final StringHelper.Casing nextWordCasing = StringHelper.classifyCasing(nextCompound);

				final char lastChar = sb.charAt(sb.length() - 1);
				//FIXME if nextCompound is changed, then check for duplicates
				nextCompound = enforceNextCompoundCase(lastChar, nextCompound, lastWordCasing, nextWordCasing);

				lastWordCasing = nextWordCasing;
			}

			sb.append(nextCompound);
		}
		return compoundEntries;
	}

	private boolean containsTriple(final StringBuffer sb, final String compound){
		boolean repeated = false;
		final int size = sb.length() - 1;
		if(size > 1){
			final String interCompounds = sb.substring(Math.max(size - 1, 0), size + 1) + compound.substring(0, Math.min(compound.length(), 2));
			final int len = interCompounds.length();
			if(len == 3 || len == 4){
				repeated = (interCompounds.charAt(0) == interCompounds.charAt(1) && interCompounds.charAt(0) == interCompounds.charAt(2));
				if(len == 4)
					repeated |= (interCompounds.charAt(1) == interCompounds.charAt(2) && interCompounds.charAt(1) == interCompounds.charAt(3));
			}
		}
		return repeated;
	}

	private String enforceNextCompoundCase(final char lastChar, String nextCompound, final StringHelper.Casing lastWordCasing,
			final StringHelper.Casing nextWordCasing){
		final char nextChar = nextCompound.charAt(0);
		if(Character.isLetter(lastChar) && Character.isLetter(nextChar)){
			final Set<StringHelper.Casing> collisions = COMPOUND_WORD_BOUNDARY_COLLISIONS.get(lastWordCasing);
			//convert nextChar to lowercase/uppercase and go on
			if(collisions != null && collisions.contains(nextWordCasing))
				nextCompound = (Character.isUpperCase(lastChar)? StringUtils.capitalize(nextCompound):
					StringUtils.uncapitalize(nextCompound));
		}
		return nextCompound;
	}

	/** @return	A list of prefixes from first entry, suffixes from last entry, and terminals from both. */
	private List<List<String>> extractCompoundFlagsByComponent(final DictionaryEntry[] compoundEntries,
			final String compoundFlag){
		final List<List<String>> prefixes = compoundEntries[0]
			.extractAllAffixes(affixData, false);
		final List<List<String>> suffixes = compoundEntries[compoundEntries.length - 1]
			.extractAllAffixes(affixData, false);
		final Set<String> terminals = new TreeSet<>();
		terminals.addAll(prefixes.get(Affixes.INDEX_TERMINALS));
		terminals.addAll(suffixes.get(Affixes.INDEX_TERMINALS));
		if(compoundFlag != null)
			terminals.remove(compoundFlag);

		final List<List<String>> result = new ArrayList<>(3);
		result.add(prefixes.get(Affixes.INDEX_PREFIXES));
		result.add(suffixes.get(Affixes.INDEX_SUFFIXES));
		result.add(new ArrayList<>(terminals));
		return result;
	}

	private void removeTwofolds(final List<Inflection> prods){
		final String circumfixFlag = affixData.getCircumfixFlag();
		if(circumfixFlag != null){
			final Iterator<Inflection> itr = prods.iterator();
			while(itr.hasNext()){
				final Inflection prod = itr.next();
				if(prod.isTwofolded(circumfixFlag))
					itr.remove();
			}
		}
	}

	//is word a non-compound with a REP substitution (see checkcompoundrep)?
	private boolean existsCompoundAsReplacement(final String word){
		boolean exists = (LoopHelper.match(compoundAsReplacement, word::contains) != null);
		if(!exists && word.length() >= 2){
			final String convertedWord = affixData.applyReplacementTable(word);
			if(dicInclusionTestWorker.isInDictionary(convertedWord)){
				compoundAsReplacement.add(word);

				exists = true;
			}
		}
		return exists;
	}

	private boolean getNextTuple(final int[] indexes, final List<List<Inflection>> entry){
		//obtain next tuple
		int i = indexes.length - 1;
		while(i >= 0){
			indexes[i] ++;
			if(indexes[i] < entry.get(i).size())
				break;

			indexes[i --] = 0;
		}
		return (i == -1);
	}

	/** Merge the distribution with the others. */
	protected Map<String, List<DictionaryEntry>> mergeDistributions(final Map<String, List<DictionaryEntry>> compoundRules,
			final Map<String, List<DictionaryEntry>> distribution, final int compoundMinimumLength, final String forbiddenWordFlag){
		final Collection<Map.Entry<String, List<DictionaryEntry>>> list = new ArrayList<>(compoundRules.entrySet());
		list.addAll(distribution.entrySet());

		final Map<String, List<DictionaryEntry>> map = new HashMap<>();
		for(final Map.Entry<String, List<DictionaryEntry>> m : list){
			final List<DictionaryEntry> entries = m.getValue();
			final List<DictionaryEntry> value = new ArrayList<>(0);
			final int size = (entries != null? entries.size(): 0);
			for(int i = 0; i < size; i ++){
				final DictionaryEntry entry = entries.get(i);
				if(entry.getWord().length() >= compoundMinimumLength && !entry.hasContinuationFlag(forbiddenWordFlag))
					value.add(entry);
			}
			final String key = m.getKey();
			final List<DictionaryEntry> v = map.get(key);
			if(v != null)
				v.addAll(value);
			else
				map.put(key, value);
		}
		return map;
	}

	protected void loadDictionaryForInclusionTest(){
		if(dicInclusionTestWorker == null && affixData.isCheckCompoundReplacement()){
			dicInclusionTestWorker = new DictionaryInclusionTestWorker(affixData.getLanguage(), dicParser, wordGenerator);

			dicInclusionTestWorker.executeSynchronously();
		}
	}

}
