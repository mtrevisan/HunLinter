/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.parsers.dictionary;

import io.github.mtrevisan.hunlinter.datastructures.SetHelper;
import io.github.mtrevisan.hunlinter.gui.ProgressCallback;
import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixType;
import io.github.mtrevisan.hunlinter.parsers.vos.AffixEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntryFactory;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.parsers.vos.RuleEntry;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.services.RegexSequencer;
import io.github.mtrevisan.hunlinter.services.text.StringHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;


public class RulesReducer{

	private static final String NON_EXISTENT_RULE = "Non-existent rule `{}`, cannot reduce";
	private static final String VERY_BAD_ERROR = "Something very bad happened while inflecting from `{}`, expected {}, obtained {}";

	private static final String TAB = "\t";
	private static final String PIPE = "|";


	private final AffixData affixData;
	protected final DictionaryEntryFactory dictionaryEntryFactory;
	private final FlagParsingStrategy strategy;
	private final WordGenerator wordGenerator;
	private final Comparator<String> comparator;
	private final Comparator<LineEntry> lineEntryComparator;


	public RulesReducer(final AffixData affixData, final WordGenerator wordGenerator){
		Objects.requireNonNull(affixData, "Affix data cannot be null");
		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");

		dictionaryEntryFactory = new DictionaryEntryFactory(affixData);
		this.affixData = affixData;
		strategy = affixData.getFlagParsingStrategy();
		this.wordGenerator = wordGenerator;
		comparator = BaseBuilder.getComparator(affixData.getLanguage());
		lineEntryComparator = Comparator.comparingInt((LineEntry entry)
				-> RegexSequencer.splitSequence(entry.condition).length)
			.thenComparingInt(entry -> StringUtils.countMatches(entry.condition, RegexHelper.GROUP_END))
			.thenComparingInt(entry -> entry.removal.length())
			.thenComparing(entry -> StringUtils.reverse(entry.condition), comparator)
			.thenComparing(entry -> entry.removal, comparator)
			.thenComparingInt(entry -> entry.firstAddition().length())
			.thenComparing(LineEntry::firstAddition, comparator);
	}


	private LineEntry createAffixEntry(final Inflection inflection, String word, final AffixType type){
		String producedWord = inflection.getWord();
		if(type ==AffixType.PREFIX){
			producedWord = StringUtils.reverse(producedWord);
			word = StringUtils.reverse(word);
		}

		final int lastCommonLetter = StringHelper.getLastCommonLetterIndex(word, producedWord);

		final int wordLength = word.length();
		final String removal = (lastCommonLetter < wordLength? word.substring(lastCommonLetter): StringUtils.EMPTY);
		String addition = (lastCommonLetter < producedWord.length()? producedWord.substring(lastCommonLetter): StringUtils.EMPTY);
		final AffixEntry lastAppliedRule = inflection.getLastAppliedRule(type);
		if(lastAppliedRule != null)
			addition += lastAppliedRule.toString(strategy);
		final String condition = (lastCommonLetter < wordLength? removal: StringUtils.EMPTY);
		return new LineEntry(removal, addition, condition, word);
	}

	final List<LineEntry> reduceRules(final Collection<LineEntry> plainRules){
		return reduceRules(plainRules, null);
	}

	/**
	 * Reduces the initial full set of plain rules into the minimal set of Hunspell sub-rules
	 * by minimizing conditions, merging identical transformations, and resolving condition overlaps.
	 *
	 * @param plainRules	The initial list of fully expanded dictionary line entries.
	 * @param progressCallback	Optional callback to track the status percentage of the execution.
	 * @return	A minimized list of {@code LineEntry} objects representing the exact minimal rule set.
	 */
	public final List<LineEntry> reduceRules(final Collection<LineEntry> plainRules,
			final ProgressCallback progressCallback){
		// 1. First reduction pass: group and merge identical conditions, removals, and additions
		// to consolidate their 'from' source words.
		final List<LineEntry> compactedRulesFrom = compactRulesFrom(plainRules, comparator);
		if(progressCallback != null)
			progressCallback.accept(14);

		// 2. Second reduction pass: group identical conditions, removals, and source sets
		// to consolidate their 'addition' fields.
		final List<LineEntry> compactedRulesAddition = compactRulesAddition(compactedRulesFrom, comparator);
		if(progressCallback != null)
			progressCallback.accept(29);

		// 3. Third reduction pass: check elements with the exact same 'from' word set,
		// extracting the maximum common removal suffix and shifting the remaining characters into the additions.
		final List<LineEntry> compactedRulesSameFrom = compactRulesByFrom(compactedRulesAddition);
		if(progressCallback != null)
			progressCallback.accept(43);

		// 4. Disjoin originating word sets: use a reverse trie to partition overlapping word sets
		// into maximal clean subsets based on their ending letters.
		final List<LineEntry> disjoinFromsRules = disjoinFroms(compactedRulesSameFrom);
		if(progressCallback != null)
			progressCallback.accept(57);

		// 5. Vertical exclusion ladder and collision solver: detect overlaps where a generic rule
		// improperly triggers on words reserved for a more specific rule, applying negative groups
		// (e.g., [^è]ƚo) or generating intersection subsets.
		final List<LineEntry> nonCollidingRules = resolveCollisions(disjoinFromsRules, comparator);
//		final List<LineEntry> nonCollidingRules = LineEntry.eliminateCollisions(compactedRulesSameFrom, comparator);
		//reshuffle the originating list to place the correct inflections in the correct rule
//		compactedRules = makeAdditionsDisjoint(compactedRules);
		if(progressCallback != null)
			progressCallback.accept(71);

		//FIXME
//		final List<LineEntry> compactedRules = compactRulesFrom(nonCollidingRules, comparator);
//		final List<LineEntry> compactedRules = nonCollidingRules;

//		compactedRules = disjoinConditions(compactedRules);

//		if(progressCallback != null)
//			progressCallback.accept(71);

//		mergeSimilarRules(compactedRules, comparator);

//		final List<LineEntry> res = LineEntry.eliminateCollisions(compactedRules, comparator);

		// 6. Flatten rules: unfold additions back into unified rules to allow final conditional grouping.
		final List<LineEntry> redistributedRules = flattenRulesByAddition(nonCollidingRules, comparator);
		if(progressCallback != null)
			progressCallback.accept(86);

		// 7. Final condition compactor: merge lines that share identical removals, additions,
		// and trailing contexts, blending their leading characters into a compressed character group (e.g., [nr]).
		//TODO same removal, same add, then add condition, add from
		final List<LineEntry> lazyCompactedRules = compactRulesCondition(redistributedRules, comparator);
//		final List<LineEntry> lazyCompactedRules = compactRulesButConditionAndFrom(redistributedRules, comparator);

		return lazyCompactedRules;
	}

	//TODO
	private static List<LineEntry> disjoinFroms(final Collection<LineEntry> entries){
		//TODO '9 HERE
		//build a map where for each word in `from` stores which entry indices contain it
		final Map<String, Set<LineEntry>> ownersByFrom = new HashMap<>();
		for(final LineEntry entry : entries)
			for(final String word : entry.from)
				ownersByFrom.computeIfAbsent(word, k -> new LinkedHashSet<>())
					.add(entry);

		//group `from` by identical owners set
		final Map<List<LineEntry>, List<String>> groupByOwners = new LinkedHashMap<>();
		for(Map.Entry<String, Set<LineEntry>> e : ownersByFrom.entrySet()){
			final String f = e.getKey();
			final List<LineEntry> key = new ArrayList<>(e.getValue());

			key.sort(Comparator.comparingInt(rule -> rule.from.hashCode()));
			groupByOwners.computeIfAbsent(key, k -> new ArrayList<>())
				.add(f);
		}


		final List<LineEntry> result = new ArrayList<>();

		//process each group independently
		for(final Map.Entry<List<LineEntry>, List<String>> group : groupByOwners.entrySet()){
			//indices of entries
			final List<LineEntry> owners = group.getKey();
			//from-values in this group
			final List<String> groupFrom = group.getValue();

			if(groupFrom.isEmpty())
				continue;

			//build union across owners of (owner.from - groupFrom)
			final Set<String> groupSet = new HashSet<>(groupFrom);
			final List<String> nonTargetsUnion = new ArrayList<>();
			for(final LineEntry e : owners)
				for(final String u : e.from)
					if(!groupSet.contains(u))
						nonTargetsUnion.add(u);


			//partition groupFrom into maximal clean subsets using a reverse trie
			final List<SubsetBySuffix> subsets = partitionByCleanSuffixes(groupFrom, nonTargetsUnion);

			//emit clones: for each subset, produce one rule per owner with the same 'from' set
			for(final SubsetBySuffix ss : subsets){
				final String suffix = ss.suffix;
				for(final LineEntry ownerEntry : owners){
					final LineEntry entry = LineEntry.createFrom(ownerEntry, suffix);
					//FIXME problem: length of `condition` less than length of `removal`... fixed? what if `removal`
					// contains a group?
					if(entry.condition.isEmpty())
						entry.condition = entry.removal;

					result.add(entry);
				}
			}
		}

		return result;
	}

	/**
	 * A small holder: the subset of 'from' (as a list) defined by a clean suffix.
	 */
	private static final class SubsetBySuffix{
		final String suffix;
		final List<String> froms;

		SubsetBySuffix(final String suffix, final List<String> froms){
			this.suffix = suffix;
			this.froms = froms;
		}
	}

	/**
	 * Partition `groupFrom` into maximal subsets, each identified by a suffix `s` such that:
	 * - `s` length >= minLen,
	 * - no `nonTarget` ends with `s`,
	 * - the subset is exactly the set of `groupFrom` strings that end with `s`,
	 * - and no ancestor suffix of `s` satisfies the same property (maximality by depth).
	 * <p>
	 * We use a reverse trie over `groupFrom` to discover the deepest clean suffixes.
	 */
	private static List<SubsetBySuffix> partitionByCleanSuffixes(final List<String> groupFrom,
			final List<String> nonTargetsUnion){
		//build reverse trie and store at each node the indices of groupFrom covered by the subtree
		final TrieNode root = new TrieNode();
		for(int i = 0; i < groupFrom.size(); i ++)
			insertReversed(root, groupFrom.get(i), i);

		final List<SubsetBySuffix> result = new ArrayList<>();
		//DFS to collect maximal clean nodes
		dfsCollect(root, "", groupFrom, nonTargetsUnion, result);
		return result;
	}

	/**
	 * Trie node for reversed strings. Each node aggregates the indices of all strings in its subtree.
	 */
	private static final class TrieNode{
		Map<Character, TrieNode> child = new HashMap<>();
		//indices of groupFrom under this node
		List<Integer> indices = new ArrayList<>();
	}

	/**
	 * Insert a string into the reverse trie, storing its index along the path.
	 * We traverse from last char to first.
	 */
	private static void insertReversed(final TrieNode root, final String s, final int index){
		TrieNode cur = root;
		cur.indices.add(index);
		for(int p = s.length() - 1; p >= 0; p --){
			final char c = s.charAt(p);
			cur = cur.child.computeIfAbsent(c, k -> new TrieNode());
			cur.indices.add(index);
		}
	}

	/**
	 * Depth-first traversal: at each node, we know the suffix represented by the path.
	 * Because we traverse from the last character to the first, we build the human-readable suffix
	 * by prepending the current character to `suffixSoFar`.
	 * <p>
	 * If the suffix is "clean" (length >= minLen and no non-target ends with it), we emit
	 * a subset for all strings under this node and DO NOT descend further (maximality).
	 * Otherwise, we keep descending.
	 */
	private static void dfsCollect(final TrieNode node, final String suffixSoFar, final List<String> groupFrom,
			final List<String> nonTargetsUnion, final List<SubsetBySuffix> out){
		//check cleanliness only if suffix length is enough
		if(!anyEndsWith(nonTargetsUnion, suffixSoFar)){
			//clean: emit the subset defined by this suffix (all strings under this node)
			List<String> subset = new ArrayList<>(node.indices.size());
			for(final int idx : node.indices)
				subset.add(groupFrom.get(idx));

			//de-duplicate: 'indices' is appended along multiple paths; remove duplicates while preserving order
			subset = dedupPreserveOrder(subset);
			out.add(new SubsetBySuffix(suffixSoFar, subset));
			//maximal: do not go deeper
			return;
		}

		//not clean (or too short): descend to children
		for(final Map.Entry<Character, TrieNode> e : node.child.entrySet()){
			final char c = e.getKey();
			final TrieNode child = e.getValue();

			//prepend current reversed char to build the forward suffix
			dfsCollect(child, c + suffixSoFar, groupFrom, nonTargetsUnion, out);
		}
	}

	/**
	 * Return true if any string in the list ends with the given suffix.
	 */
	private static boolean anyEndsWith(final List<String> xs, final String suffix){
		for(final String s : xs)
			if(s.endsWith(suffix))
				return true;
		return false;
	}

	/**
	 * Remove duplicates while preserving first occurrence order.
	 */
	private static <T> List<T> dedupPreserveOrder(final List<T> xs){
		final List<T> res = new ArrayList<>(xs.size());
		final Set<T> seen = new HashSet<>();
		for(final T x : xs)
			if(seen.add(x))
				res.add(x);
		return res;
	}


	private static List<LineEntry> disjoinFroms2(final Collection<LineEntry> entries){
		final List<LineEntry> result = new ArrayList<>(entries);
		boolean changed;
		do{
			changed = false;

			//sort by number of `from` elements
			result.sort(Comparator.comparingInt(rule -> rule.from.size()));

			for(int i = 0, length = result.size(); !changed && i < length; i ++){
				//candidate generic
				final LineEntry generic = result.get(i);

				for(int j = i + 1; !changed && j < length; j ++){
					//candidate specific
					final LineEntry specific = result.get(j);

					//rules can be split if removal is the same
					if(specific.from.containsAll(generic.from)
							&& specific.from.size() > generic.from.size()
							&& generic.condition.equals(specific.condition)){
						generic.addition.addAll(specific.addition);
						specific.from.removeAll(generic.from);
						changed = true;
					}
				}
			}
		}while(changed);


		do{
			changed = false;

				//sort by number of `from` elements
			result.sort(Comparator.comparingInt(rule -> ((LineEntry)rule).from.size())
				.thenComparingInt(rule -> ((LineEntry)rule).condition.length()));

			for(int i = 0, length = result.size(); !changed && i < length; i ++){
				//candidate generic
				final LineEntry generic = result.get(i);

				for(int j = i + 1; !changed && j < length; j ++){
					//candidate specific
					final LineEntry specific = result.get(j);

					//rules can be merged if `from` is the same
					if(specific.from.equals(generic.from)){
						final String deltaRemoval = specific.removal
							.substring(0, specific.removal.length() - generic.removal.length());
						final Set<String> newGenericAddition = new HashSet<>(generic.addition.size());
						for(final String addition : generic.addition)
							newGenericAddition.add(deltaRemoval + addition);
						generic.addition.clear();
						generic.addition.addAll(newGenericAddition);
						generic.addition.addAll(specific.addition);
						generic.removal = specific.removal;
						generic.condition = specific.condition;
						specific.from.removeAll(generic.from);
						if(specific.from.isEmpty())
							result.remove(specific);
						changed = true;
					}
				}
			}
		}while(changed);

		return result;
	}

	private static List<LineEntry> compactRulesCondition(final Collection<LineEntry> entries,
			final Comparator<String> comparator){
		final List<LineEntry> result = new ArrayList<>(entries);
		final Map<String, List<LineEntry>> map = new HashMap<>();
		for(final LineEntry entry : entries){
			final String key = new StringJoiner(PIPE)
				.add(entry.removal)
//				.add(RegexHelper.sortAndMergeSet(entry.addition, comparator))
				.add(Integer.toString(sortAndMergeAndHash(entry.addition, comparator)))
				.toString();
			final List<LineEntry> existingList = map.computeIfAbsent(key, k -> new ArrayList<>(0));
			if(existingList.isEmpty())
				existingList.add(entry);
			else{
				final String[] entryCondition = RegexSequencer.splitSequence(entry.condition);

				boolean removed = false;
				for(int i = 0, length = existingList.size(); !removed && i < length; i ++){
					final LineEntry existing = existingList.get(i);

					final String[] ruleCondition = RegexSequencer.splitSequence(existing.condition);
					final String[] ruleFollowingConditions = Arrays.copyOfRange(ruleCondition, 1, ruleCondition.length);
					final String[] entryFollowingConditions = Arrays.copyOfRange(entryCondition, 1, entryCondition.length);
					if(Arrays.equals(ruleFollowingConditions, entryFollowingConditions)
							&& existing.removal.equals(entry.removal)
							&& existing.addition.equals(entry.addition)){
						existing.from.addAll(entry.from);
						final Set<Character> ruleConditions = extractCharacters(ruleCondition[0]);
						final Set<Character> entryConditions = extractCharacters(entryCondition[0]);
						ruleConditions.addAll(entryConditions);
						ruleCondition[0] = RegexHelper.makeGroup(ruleConditions, comparator);
						existing.condition = StringUtils.join(ruleCondition);
						result.remove(entry);
						removed = true;
					}
				}
				if(!removed)
					existingList.add(entry);
			}
		}
		return result;
	}

	private static Set<Character> extractCharacters(final String str){
		final Set<Character> result = new HashSet<>();
		for(final char c : str.toCharArray())
			result.add(c);
		result.remove('[');
		result.remove(']');
		return result;
	}

	/**
	 * Compacts a collection of {@code LineEntry}s by grouping and merging the entries based on their corresponding
	 * keys built from conditions, removals, and sorted/hashed additions. Entries with the same key are merged,
	 * consolidating their `from` fields.
	 *
	 * @param entries	The collection of {@code LineEntry} objects to be compacted.
	 * @param comparator	A {@code Comparator<String>} used to sort and merge additions during the key comparison and
	 * 	hashing.
	 * @return	A list of {@code LineEntry} objects where entries with matching keys have been merged and compacted.
	 */
	private static List<LineEntry> compactRulesFrom(final Collection<LineEntry> entries,
			final Comparator<String> comparator){
		return compactRules(
			entries,
			entry -> new StringJoiner(PIPE)
				.add(entry.condition)
				.add(entry.removal)
//				.add(RegexHelper.sortAndMergeSet(entry.addition, comparator))
				.add(Integer.toString(sortAndMergeAndHash(entry.addition, comparator)))
				.toString(),
			(rule, entry) -> rule.from.addAll(entry.from)
		);
	}

	/**
	 * Compacts a collection of {@code LineEntry} objects by grouping and merging entries based on their conditions,
	 * removals, and hashed/sorted additions. Entries with matching keys are combined, consolidating their `addition`
	 * fields.
	 *
	 * @param entries	The collection of {@code LineEntry} objects to be compacted.
	 * @param comparator	A {@code Comparator<String>} used to sort and hash the `from` field during the key generation
	 * 	and merging process.
	 * @return	A list of {@code LineEntry} objects resulting from the compacted and merged entries.
	 */
	private static List<LineEntry> compactRulesAddition(final Collection<LineEntry> entries,
			final Comparator<String> comparator){
		return compactRules(
			entries,
			entry -> new StringJoiner(PIPE)
				.add(entry.condition)
				.add(entry.removal)
//				.add(RegexHelper.sortAndMergeSet(entry.from, comparator))
				.add(Integer.toString(sortAndMergeAndHash(entry.from, comparator)))
				.toString(),
			(rule, entry) -> rule.addition.addAll(entry.addition)
		);
	}

	/**
	 * Compacts a list of {@code LineEntry} objects by grouping them based on their `from` property and merging entries
	 * with the same `from` set into a single LineEntry.
	 *
	 * @param entries	The list of {@code LineEntry} objects to be compacted.
	 * @return	A new list of LineEntry objects where entries with the same `from` set are combined into a single
	 * 	entry, with additions and removal data merged.
	 */
	private static List<LineEntry> compactRulesByFrom(List<LineEntry> entries){
		//group by `from`
		final Map<Set<String>, List<LineEntry>> fromGroups = entries.stream()
			.collect(Collectors.groupingBy(e -> new HashSet<>(e.from)));

		final List<LineEntry> result = new ArrayList<>();
		for(final Map.Entry<Set<String>, List<LineEntry>> group : fromGroups.entrySet()){
			final List<LineEntry> sameFrom = group.getValue();
			if(sameFrom.size() == 1){
				result.add(sameFrom.getFirst());
				continue;
			}

			//find longest `removal`
			final LineEntry longest = sameFrom.stream()
				.max(Comparator.comparingInt(e -> e.removal.length()))
				.orElseThrow();

			final int newRemovalLength = longest.removal.length();
			final Set<String> mergedAdd = new LinkedHashSet<>();
			for(int i = 0, length = sameFrom.size(); i < length; i ++){
				final LineEntry e = sameFrom.get(i);
				final int delta = newRemovalLength - e.removal.length();
				final String baseRemoval = longest.removal.substring(0, delta);
				for(final String add : e.addition)
					mergedAdd.add(baseRemoval + add);
			}

			final LineEntry newEntry = new LineEntry(longest.removal, mergedAdd, longest.condition, group.getKey());
			result.add(newEntry);
		}

		return result;
	}

	private static List<LineEntry> flattenRulesByAddition(final Collection<LineEntry> entries,
		final Comparator<String> comparator){
		final List<LineEntry> flattened = new ArrayList<>();
		for(final LineEntry entry : entries){
			for(final String add : entry.addition){
				final Set<String> singleAdd = new LinkedHashSet<>();
				singleAdd.add(add);
				flattened.add(new LineEntry(entry.removal, singleAdd, entry.condition, new HashSet<>(entry.from)));
			}
		}
		return flattened;
	}

	/**
	 * Resolves potential collisions in a list of {@code LineEntry} objects by refining entries so that more specific
	 * conditions do not conflict with generic ones. It adjusts the conditions of entries to eliminate overlaps between
	 * conditions while maintaining the order.
	 *
	 * @param entries	The list of {@code LineEntry} objects to be analyzed and refined.
	 * @param comparator	A {@link Comparator} for comparing string entries within the {@code LineEntry} objects.
	 * @return	A new list of {@code LineEntry} objects with collisions resolved and conditions refined.
	 */
	private static List<LineEntry> resolveCollisions(final List<LineEntry> entries, final Comparator<String> comparator){
		//group by condition
//		final Map<String, List<LineEntry>> groupByCondition = new HashMap<>(0);
//		for(int i = 0, length = entries.size(); i < length; i ++){
//			final LineEntry entry = entries.get(i);
//			groupByCondition.computeIfAbsent(entry.condition, k -> new ArrayList<>(0))
//				.add(entry);
//		}

		//separate the various `from`s so that they don't intersect with each other
//		for(final Map.Entry<String, List<LineEntry>> group : groupByCondition.entrySet()){
//			final List<LineEntry> sameConditionList = group.getValue();
//			if(sameConditionList.size() > 1){
//				final List<LineEntry> disjointList = makeFromsDisjoint(sameConditionList);
//				group.setValue(disjointList);
//			}
//		}

//		for(final Map.Entry<String, List<LineEntry>> group : groupByCondition.entrySet()){
//			final String condition = group.getKey();
//			final List<LineEntry> sameConditionList = group.getValue();
//			if(sameConditionList.size() == 1)
//				continue;
//
//
//			//build per-length buckets
//			final Map<Integer, List<LineEntry>> buckets = new HashMap<>(0);
//			for(final LineEntry entry : sameConditionList){
//				final int conditionLength = RegexSequencer.splitSequence(entry.condition).length;
//				buckets.computeIfAbsent(conditionLength, k -> new ArrayList<>(0))
//					.add(entry);
//			}
//
//			//process each fixed-length bucket independently
//			for(final Map.Entry<Integer, List<LineEntry>> bucket : buckets.entrySet()){
//				final Integer bucketLength = bucket.getKey();
//				final List<LineEntry> bucketEntries = bucket.getValue();
//
//				for(int i = 0, length = bucketEntries.size(); i < length; i ++){
//					final LineEntry entry = bucketEntries.get(i);
//
//					final String[] entryCondition = RegexSequencer.splitSequence(entry.condition);
//					//TODO
//				}
//			}
//		}
//---

		final List<LineEntry> result = new ArrayList<>(entries);
		boolean changed;
		do{
			changed = false;

			//sort by condition length (more generic first)
			result.sort(Comparator.comparingInt(rule -> RegexHelper.conditionLength(rule.condition)));

			for(int i = 0, length = result.size(); !changed && i < length; i ++){
				//candidate generic
				final LineEntry generic = result.get(i);
				final String[] genericCondition = RegexSequencer.splitSequence(generic.condition);

				for(int j = i + 1; !changed && j < length; j ++){
					//candidate specific
					final LineEntry specific = result.get(j);
					final String[] specificCondition = RegexSequencer.splitSequence(specific.condition);

					if(RegexSequencer.endsWith(specificCondition, genericCondition) && !generic.from.equals(specific.from)){
						//collision detected: `generic` is too generic compared to `specific`
						//try to refine `generic` using its `from` words
						refineCondition(generic, genericCondition, specific, specificCondition, result, comparator);
						changed = true;
					}
				}
			}
		}while(changed);
		return result;
	}

	/**
	 * Given a list of sets (possibly overlapping), produce a new list of sets that are
	 * pairwise disjoint and whose union equals the union of all input sets.
	 *
	 * The algorithm computes, for each element, its membership pattern across the input sets
	 * (as a BitSet). Elements with identical membership patterns are grouped into the same "atom".
	 * Each atom is a disjoint block. The original sets can be reconstructed as the union of all
	 * atoms whose BitSet has the corresponding index set.
	 *
	 * Properties:
	 * - Coverage preserved: the union of the output sets equals the union of the input sets.
	 * - Pairwise disjoint output: no two output sets share an element.
	 * - Stable element order: elements appear in the order of their first appearance across inputs.
	 * - Efficient: O(U * (k/wordSize)) to build membership, where U=#unique elements, k=#input sets.
	 *
	 * @param entries	List of input sets (may contain nulls or empty sets)
	 * @return	A new list of pairwise-disjoint sets ("atoms of membership")
	 */
	private static List<LineEntry> makeFromsDisjoint(final List<LineEntry> entries){
		final int size = entries.size();

		//1) Build element -> BitSet (membership pattern across input sets).
		final Map<String, BitSet> membership = new HashMap<>();
		for(int idx = 0; idx < size; idx ++){
			final LineEntry entry = entries.get(idx);
			for(final String elem : entry.from){
				BitSet bs = membership.get(elem);
				if(bs == null){
					bs = new BitSet(size);
					membership.put(elem, bs);
				}
				bs.set(idx);
			}
		}

		//if there were no valid elements, return empty
		if(membership.isEmpty())
			return Collections.emptyList();

		final LineEntry first = entries.getFirst();
		final String removal = first.removal;
		final Set<String> addition = first.addition;
		final String condition = first.condition;
		// 2) Group elements by identical BitSet (the "atoms").
		// BitSet equals/hashCode are content-based, so they can be used as keys.
		// We clone them to avoid any accidental mutation in the map keys.
		final Map<BitSet, LineEntry> atoms = new LinkedHashMap<>();
		for(final Map.Entry<String, BitSet> en : membership.entrySet()){
			final String elem = en.getKey();
			final BitSet owners = en.getValue();

			final LineEntry entry = atoms.computeIfAbsent(owners,
				k -> new LineEntry(removal, addition, condition, new HashSet<>(1)));
			entry.from.add(elem);
		}

		// 3) Materialize result as a list of disjoint sets, preserving atom order by first appearance.
		return new ArrayList<>(atoms.values());
	}

	/**
	 * Refines colliding rule conditions by extracting character intersections and
	 * partitioning word sets to avoid over-generalized negation groups.
	 *
	 * @param generic            The generic rule entry to refine.
	 * @param genericCondition   The split condition sequence of the generic rule.
	 * @param specific           The specific rule entry causing the collision.
	 * @param specificCondition  The split condition sequence of the specific rule.
	 * @param entries            The total list of line entries being processed.
	 * @param comparator         The character ordering comparator.
	 */
	private static void refineCondition(final LineEntry generic, final String[] genericCondition,
			final LineEntry specific, final String[] specificCondition,
			final List<LineEntry> entries, final Comparator<String> comparator){
		//calculate intersection (I = T1 ∩ T2):
		final int genericConditionLength = genericCondition.length
			//NOTE: If the general and specific conditions have the same length, and one starts with a group, no token will
			// be added, but a change will be made to the current conditions
			- (genericCondition.length == specificCondition.length
				&& (genericCondition.length > 1 && genericCondition[0].length() > 1
				|| specificCondition.length > 0 && specificCondition[0].length() > 1)? 1: 0);
		final Set<Character> genericToken = generic.extractGroup(genericConditionLength);
		final Set<Character> specificToken = specific.extractGroup(genericConditionLength);
		final Set<Character> intersectionToken = SetHelper.intersection(genericToken, specificToken);

		if(!intersectionToken.isEmpty()){
			//calculate the differences (S1 = T1 \ I e S2 = T2 \ I)
			final Set<Character> genericOnlyToken = new HashSet<>(genericToken);
			genericOnlyToken.removeAll(intersectionToken);
			final Set<Character> specificOnlyToken = new HashSet<>(specificToken);
			specificOnlyToken.removeAll(intersectionToken);

			if(genericOnlyToken.isEmpty() && specificOnlyToken.isEmpty()){
				//if the conditions of only the generic token and only the specific token are both empty
				// (S1 = ∅ ∧ S2 = ∅), add a token to the head of the generic condition...
				final Set<Character> otherTokens = new HashSet<>();
				for(final LineEntry entry : entries){
					if(entry == generic)
						continue;

					if(entry.condition.endsWith(generic.condition)){
						final String[] entryCond = RegexSequencer.splitSequence(entry.condition);
						if(entryCond.length > genericConditionLength){
							final Set<Character> token = entry.extractGroup(genericConditionLength);
							otherTokens.addAll(token);
						}
					}
				}

				final boolean chooseRatifyingOverNegated = (genericToken.size() + 1 <= otherTokens.size());
				final String newGenericCondition = (chooseRatifyingOverNegated
						? RegexHelper.makeGroup(genericToken, comparator)
						: RegexHelper.makeNotGroup(otherTokens, comparator))
					+ generic.condition;
				final LineEntry newGeneric = LineEntry.createFrom(generic, newGenericCondition);
				entries.remove(generic);
				if(!entries.contains(newGeneric))
					entries.add(newGeneric);
			}
			else{
				//... otherwise, replace both rules with three rules, each with the condition of only the first (S1), only
				// the second (S2), and the intersection (I), redistribute the words in "from" appropriately

				final boolean applyExclusionLadder = applyExclusionLadder(generic, genericCondition,
					specificCondition,
					entries, comparator,
					genericConditionLength);

				// If they share the same condition length or already contain character classes (e.g., "[ab]o"),
				// it makes sense to split into 3 parts to cleanly isolate the intersection.
				int specificConditionLength = specificCondition.length;
				if(genericConditionLength == specificConditionLength || !applyExclusionLadder){
					if(!genericOnlyToken.isEmpty()){
						entries.remove(generic);
						final StringBuilder sb = new StringBuilder(RegexHelper.makeGroup(genericOnlyToken, comparator));
						for(int i = (genericConditionLength > 1? 1: 0), length = 1; i < length; i ++)
							sb.append(genericCondition[i]);
						final String genericOnlyCondition = sb.toString();
						final LineEntry newGenericOnly = LineEntry.createFrom(generic, genericOnlyCondition);
						if(!entries.contains(newGenericOnly))
							entries.add(newGenericOnly);
					}
					if(!specificOnlyToken.isEmpty()){
						entries.remove(specific);

						final StringBuilder sb = new StringBuilder(RegexHelper.makeGroup(specificOnlyToken, comparator));
						for(int i = (specificConditionLength > 1? 1: 0), length = 1; i < length; i ++)
							sb.append(specificCondition[i]);
						final String specificOnlyCondition = sb.toString();
						final LineEntry newSpecificOnly = LineEntry.createFrom(specific, specificOnlyCondition);
						if(!entries.contains(newSpecificOnly))
							entries.add(newSpecificOnly);
					}
					if(!intersectionToken.isEmpty()){
						final StringBuilder sb = new StringBuilder(RegexHelper.makeGroup(intersectionToken, comparator));
						for(int i = (genericConditionLength > 1? 1: 0), length = 1; i < length; i ++)
							sb.append(genericCondition[i]);
						final String intersectionCondition = sb.toString();
						final LineEntry newIntersectionFromGeneric = LineEntry.createFrom(generic, intersectionCondition);
						final LineEntry newIntersectionFromSpecific = LineEntry.createFrom(specific, intersectionCondition);
						if(!entries.contains(newIntersectionFromGeneric))
							entries.add(newIntersectionFromGeneric);
						if(!entries.contains(newIntersectionFromSpecific))
							entries.add(newIntersectionFromSpecific);
					}
				}
			}
		}
		else{
			//check if the condition of the generic rule to be preceded by the token begins with a group
			if(genericCondition.length > 0 && genericCondition[0].length() > 1){
				//replace the generic rule with the same number of rules of the first token by expanding and removing it,
				// redistribute the words in "from" appropriately
				final char[] charArray = genericCondition[0].toCharArray();
				final String newGenericOnlyBaseCondition = generic.condition.substring(charArray.length);
				entries.remove(generic);
				for(int i = 1, length = charArray.length - 1; i < length; i ++){
					final char chr = charArray[i];

					final LineEntry newGenericOnly = LineEntry.createFrom(generic,
						chr + newGenericOnlyBaseCondition);
					if(!entries.contains(newGenericOnly))
						entries.add(newGenericOnly);
				}
			}
			else{
				final Set<Character> otherTokens = new HashSet<>();
				for(final LineEntry entry : entries){
					if(entry == generic)
						continue;

					if(entry.condition.endsWith(generic.condition)){
						final String[] entryCond = RegexSequencer.splitSequence(entry.condition);
						if(entryCond.length > genericConditionLength){
							final Set<Character> token = entry.extractGroup(genericConditionLength);
							otherTokens.addAll(token);
						}
					}
				}

				final boolean chooseRatifyingOverNegated = (genericToken.size() + 1 <= otherTokens.size());
				final String newGenericCondition = (chooseRatifyingOverNegated
						? RegexHelper.makeGroup(genericToken, comparator)
						: RegexHelper.makeNotGroup(otherTokens, comparator))
					+ generic.condition;
				final LineEntry newGeneric = LineEntry.createFrom(generic, newGenericCondition);
				entries.remove(generic);
				if(!entries.contains(newGeneric))
					entries.add(newGeneric);
			}
		}


		//FIXME fin kuà -- apply collision detection e resolution
//		final Set<String> suffixes = new HashSet<>();
//		for(final String w : generic.from){
//			if(w.length() >= genericConditionLength){
//				//extract the actual suffix of the word with same length as condition
//				final String suffix = w.substring(w.length() - genericConditionLength);
//				suffixes.add(suffix);
//			}
//		}
//
//		//if all words share the same suffix, we can refine
//		if(suffixes.size() == 1){
//			final String refined = suffixes.iterator()
//				.next();
//			assert (refined != null && !refined.equals(generic.condition));
//			generic.condition = refined;
//		}
	}

	private static boolean applyExclusionLadder(LineEntry generic, String[] genericCondition, String[] specificCondition, List<LineEntry> entries, Comparator<String> comparator, int genericConditionLength){
		boolean applyExclusionLadder = false;
		// If the generic rule has a shorter condition length than the specific one (e.g., "o" vs "èƚo"),
		// we are dealing with a vertical inclusion hierarchy: apply a negative character class progression.
		int specificConditionLength = specificCondition.length;
		if(genericConditionLength < specificConditionLength){
			// Check if both conditions consist exclusively of single-character tokens (e.g. "a", not "[ab]")
			boolean allSingleCharacters = true;
			for(int i = 0; i < genericConditionLength; i ++){
				final String token = genericCondition[i];
				if(token.length() != 1){
					allSingleCharacters = false;
					break;
				}
			}
			if(allSingleCharacters)
				for(int i = 0; i < specificConditionLength; i ++){
					final String token = specificCondition[i];
					if(token.length() != 1){
						allSingleCharacters = false;
						break;
					}
				}

			if(allSingleCharacters){
				applyExclusionLadder = true;

				entries.remove(generic);

				// 1. Strip the matching context part to find exactly which characters are left over
				int extraCharsCount = specificConditionLength - genericConditionLength;

				// 2. Iterate backwards through the remaining specific path to build the progressive exclusions
				for(int i = extraCharsCount - 1; i >= 0; i --){
					final StringBuilder sb = new StringBuilder();

					// Add the current character as a negative group
					final char targetChar = specificCondition[i].charAt(0);
					final Set<Character> toExclude = Collections.singleton(targetChar);
					sb.append(RegexHelper.makeNotGroup(toExclude, comparator));

					// Append the remaining characters of the specific condition that follow this index, up to the end
					for(int j = i + 1; j < specificConditionLength; j ++)
						sb.append(specificCondition[j]);

					final LineEntry newProgressiveEntry = LineEntry.createFrom(generic, sb.toString());
					if(!entries.contains(newProgressiveEntry))
						entries.add(newProgressiveEntry);
				}
			}
		}
		return applyExclusionLadder;
	}

	private static List<LineEntry> compactRulesButConditionAndFrom(final List<LineEntry> plainRules,
			final Comparator<String> comparator){
		final Map<String, LineEntry> map = new HashMap<>(0);
		for(int i = 0, length = plainRules.size(); i < length; i ++){
			final LineEntry entry = plainRules.get(i);

			final StringJoiner key = new StringJoiner(PIPE)
				.add(entry.removal)
//				.add(RegexHelper.sortAndMergeSet(entry.addition, comparator));
				.add(Integer.toString(sortAndMergeAndHash(entry.addition, comparator)));
			final String keyString = key.toString();

			final String[] conditions = RegexSequencer.splitSequence(entry.condition);
			if(conditions.length != 1)
				map.put(keyString, entry);
			else{
				final LineEntry rule = map.get(keyString);
				if(rule == null){
					final LineEntry newEntry = new LineEntry(entry.removal, entry.addition, entry.condition, entry.from);
					map.put(keyString, newEntry);
				}
				else{
					//merge conditions (remember that `conditions` is of length 1 here):
					final String[] ruleBaseCondition = RegexSequencer.splitSequence(rule.condition);
					if(ruleBaseCondition.length != 1)
						map.put(keyString, entry);
					else{
						String ruleConditions = ruleBaseCondition[0];
						if(ruleConditions.length() > 1)
							ruleConditions = ruleConditions.substring(1, ruleConditions.length() - 1);

						final Set<Character> addedGroup = entry.extractGroup(0);
						final char[] charArray = ruleConditions.toCharArray();
						for(int j = 0, length2 = charArray.length; j < length2; j ++)
							addedGroup.add(charArray[j]);

						rule.condition = RegexHelper.makeGroup(addedGroup, comparator);
						rule.from.addAll(entry.from);
					}
				}
			}
		}
		return new ArrayList<>(map.values());
	}

	/**
	 * Compacts a collection of {@code LineEntry} objects by merging entries with the same key.
	 * The key for each {@code LineEntry} is determined using the provided keyBuilder function. When multiple
	 * {@code LineEntry} objects share the same key, the merger consumer is used to combine them.
	 *
	 * @param <K>	The type of the key used for grouping LineEntry objects.
	 * @param plainRules	The input collection of LineEntry objects to be compacted.
	 * @param keyBuilder	A function that generates a key for each LineEntry object for grouping purposes.
	 * @param merger	A consumer that defines how two LineEntry objects with the same key are merged.
	 * @return	A list of compacted {@code LineEntry} objects, each representing a unique key.
	 */
	private static <K> List<LineEntry> compactRules(final Collection<LineEntry> plainRules,
			final Function<LineEntry, K> keyBuilder, final BiConsumer<LineEntry, LineEntry> merger){
		final Map<K, LineEntry> map = new HashMap<>();
		for(final LineEntry entry : plainRules){
			final K key = keyBuilder.apply(entry);
			final LineEntry existing = map.get(key);
			if(existing == null)
				map.put(key, entry);
			else
				merger.accept(existing, entry);
		}
		return new ArrayList<>(map.values());
	}

	/**
	 * Sorts the provided collection of elements using the given comparator, merges them into a single string
	 * separated by a pipe symbol, and returns the hash code of the resulting string.
	 *
	 * @param <V>	The type of elements in the collection.
	 * @param set	The collection of elements to be sorted, merged, and hashed.
	 * @param comparator	The comparator used to define the order of sorting.
	 * @return	The hash code of the concatenated string resulting from the sorted collection.
	 */
	private static <V> int sortAndMergeAndHash(final Collection<V> set, final Comparator<String> comparator){
		return set.stream()
			.map(String::valueOf)
			.sorted(comparator)
			.collect(Collectors.joining(PIPE))
			.hashCode();
	}

	/**
	 * Redistributes rules from the given list by splitting each entry's additions into separate entries.
	 * For each addition, a new element is created and added to the resulting list.
	 *
	 * @param entries	The input list of objects to be redistributed. Each element in this list can have multiple
	 * 	additions that will be split into separate entries.
	 * @param comparator	A {@code Comparator<String>} used to sort and merge additions during the key comparison and
	 * 	hashing.
	 * @return	A list of objects where each addition from the original entries has been separated into its own element.
	 */
	private static List<LineEntry> flattenRulesByAddition(final List<LineEntry> entries,
			final Comparator<String> comparator){
		final Map<String, List<LineEntry>> map = new HashMap<>(entries.size());
		for(int i = 0, length = entries.size(); i < length; i ++){
			final LineEntry entry = entries.get(i);
			for(final String addition : entry.addition)
				map.computeIfAbsent(addition, k -> new ArrayList<>(1))
					.add(new LineEntry(entry.removal, addition, entry.condition, entry.from));
		}

		//FIXME
		for(final Map.Entry<String, List<LineEntry>> entry : map.entrySet()){
			final int size = entry.getValue()
				.size();
			if(size > 1){
				//bucket by same `removal`
				final Map<String, List<LineEntry>> merged = new HashMap<>(size);
				for(final LineEntry e : entry.getValue()){
					final String[] condition = RegexSequencer.splitSequence(e.condition);
					merged.computeIfAbsent(e.removal + PIPE + condition.length, k -> new ArrayList<>(1))
						.add(e);
				}
				for(final Map.Entry<String, List<LineEntry>> kv : merged.entrySet()){
					final List<LineEntry> v = kv.getValue();
					if(v.size() > 1){
						final String removal = kv.getKey().substring(0, kv.getKey().indexOf(PIPE));

						//merge `condition`, `addition`, and `from`
						final Set<Character> newConditionSet = new HashSet<>(v.size());
						for(final LineEntry e : v){
							final String[] condition = RegexSequencer.splitSequence(e.condition);
							if(condition.length == 0)
								continue;
//FIXME %0
//	0 = {LineEntry@6489} "LineEntry[cond=[ov]e,rem=e,add=[eneta/F2\tds:eto],from=[dexnove, ñove, nove, noe, dixnove, dexenove, dixenove]]"
//	1 = {LineEntry@6490} "LineEntry[cond=iexe,rem=e,add=[eneta/F2\tds:eto],from=[diexe]]"
//							if(condition.length > 1)
//								throw new IllegalStateException("Condition length is more than 1! that must be handled, please contact developer");

							if(condition.length == 1)
								newConditionSet.addAll(extractCharacters(condition[0]));
						}
						final String newCondition = RegexHelper.makeGroup(newConditionSet, comparator);
						final Set<String> newAddition = v.stream()
							.flatMap(le -> le.addition.stream())
							.collect(Collectors.toSet());
						final Set<String> newFrom = v.stream()
							.flatMap(le -> le.from.stream())
							.collect(Collectors.toSet());
						final LineEntry newEntry = new LineEntry(removal, newAddition, newCondition, newFrom);
						v.clear();
						v.add(newEntry);
					}
				}
				entry.getValue().clear();
				for(final List<LineEntry> e : merged.values())
					entry.getValue().addAll(e);
			}
		}

		final List<LineEntry> list = map.values()
			.stream()
			.flatMap(Collection::stream)
			.toList();
		return list;
	}

//	private static List<LineEntry> redistributeRules(final List<LineEntry> plainRules, final Comparator<String> comparator){
//		final Map<String, LineEntry> map = new HashMap<>(0);
//		for(int i = 0, length = plainRules.size(); i < length; i ++)
//			redistributeRule(plainRules.get(i), map);
//
//		final List<LineEntry> redistributedRules = redistributeRules(map, comparator);
//
//		return compactRules(redistributedRules, comparator);
//	}

//	private static void redistributeRule(final LineEntry entry, final Map<String, LineEntry> map){
//		final StringBuilder key = new StringBuilder(entry.removal + TAB);
//		final int additionIndex = key.length();
//		for(final String addition : entry.addition){
//			key.setLength(additionIndex);
//			key.append(addition)
//				.append(TAB)
//				.append(entry.condition);
//			final String keyString = key.toString();
//			final LineEntry rule = map.get(keyString);
//			if(rule == null){
//				final LineEntry newEntry = new LineEntry(entry.removal, addition, entry.condition, entry.from);
//				map.put(keyString, newEntry);
//			}
//			else
//				rule.from.addAll(entry.from);
//		}
//	}

//	private static List<LineEntry> redistributeRules(final Map<String, LineEntry> map, final Comparator<String> comparator){
//		//same removal, condition, and from parts
//		final Map<String, LineEntry> compaction = new HashMap<>(map.size());
//		for(final LineEntry entry : map.values()){
//			final String key = entry.removal + TAB + entry.condition + TAB + RegexHelper.sortAndMergeSet(entry.from, comparator);
//			final LineEntry rule = compaction.putIfAbsent(key, entry);
//			if(rule != null)
//				rule.addition.addAll(entry.addition);
//		}
//		return new ArrayList<>(compaction.values());
//	}

//	private static List<LineEntry> compactRules(final List<LineEntry> rules, final Comparator<String> comparator){
//		//same removal, addition, and condition parts
//		final Map<String, LineEntry> compaction = new HashMap<>(rules.size());
//		for(int i = 0, length = rules.size(); i < length; i ++){
//			final LineEntry entry = rules.get(i);
//			final String key = entry.removal + TAB + RegexHelper.sortAndMergeSet(entry.addition, comparator) + TAB + entry.condition;
//			final LineEntry rule = compaction.putIfAbsent(key, entry);
//			if(rule != null)
//				rule.from.addAll(entry.from);
//		}
//		return new ArrayList<>(compaction.values());
//	}

	private static List<LineEntry> makeAdditionsDisjoint(final List<LineEntry> rules){
		//transform
		//	[rem=èra,add=[ereta, ara, era, iera, ièra, areta, iereta],cond=èra,from=…]
		//	[rem=èra,add=[ereta, ara, era, areta],cond=èra,from=…]
		//into
		//	[rem=èra,add=[iera, ièra, iereta],    cond=èra,from=…]
		//	[rem=èra,add=[ereta, ara, era, areta],cond=èra,from=…]

		//transform
		//	[rem=ièr,add=[ar, areto, ereto, ier, èr, iar, iereto, er],cond=ièr,from=…]
		//into
		//	[rem= èr,add=[er, ar, ereto],           cond=ièr,from=…]
		//	[rem=ièr,add=[ar, areto, ereto, èr, er],cond=ièr,from=…]
		final List<LineEntry> disjointedRules = new ArrayList<>(0);

		final List<LineEntry> temporaryRules = new ArrayList<>(0);
		final List<String> keys = new ArrayList<>(0);
		for(int i = 0, length = rules.size(); i < length; i ++){
			final LineEntry rule = rules.get(i);
			temporaryRules.clear();

			final Map<String, List<String>> lcss = SetHelper.bucket(rule.addition,
				add -> StringHelper.longestCommonPrefix(add, rule.removal));
			if(lcss.size() > 1){
				//order keys from longer to shorter
				keys.clear();
				keys.addAll(lcss.keySet());
				keys.sort(Comparator.comparingInt(String::length).reversed());
				final List<String> additionsToBeRemoved = retrieveAdditionsToBeRemoved(rules, rule, temporaryRules, lcss,
					keys);

				for(int j = 0, length2 = temporaryRules.size(); j < length2; j ++)
					insertRuleOrUpdateFrom(disjointedRules, temporaryRules.get(j));
				final Set<String> strings = rule.addition;
				for(int j = 0, length2 = additionsToBeRemoved.size(); j < length2; j ++)
					strings.remove(additionsToBeRemoved.get(j));
				if(!rule.addition.isEmpty())
					temporaryRules.clear();
			}

			if(temporaryRules.isEmpty())
				insertRuleOrUpdateFrom(disjointedRules, rule);
		}

		return disjointedRules;
	}

	//add each key, remove the list from the addition
	private static List<String> retrieveAdditionsToBeRemoved(final Collection<LineEntry> rules, final LineEntry rule,
			final Collection<LineEntry> temporaryRules, final Map<String, List<String>> lcss, final List<String> keys){
		final List<String> additionsToBeRemoved = new ArrayList<>(0);
		for(int i = 0, length = keys.size(); i < length; i ++){
			final String key = keys.get(i);
			final int keyLength = key.length();
			final int conditionLength = rule.condition.length() - keyLength;
			if(conditionLength <= 0)
				break;

			final String condition = rule.condition.substring(keyLength);
			final String removal = (conditionLength <= rule.removal.length()? condition: rule.removal);
			final List<String> list = lcss.get(key);
			final Set<String> addition = new HashSet<>(list.size());
			for(int j = 0, length2 = list.size(); j < length2; j ++)
				addition.add(list.get(j).substring(keyLength));
			final LineEntry newEntry = new LineEntry(removal, addition, condition, rule.from);
			if(rules.contains(newEntry)){
				temporaryRules.add(newEntry);

				additionsToBeRemoved.addAll(list);
			}
		}
		return additionsToBeRemoved;
	}

	private static void insertRuleOrUpdateFrom(final List<LineEntry> expandedRules, final LineEntry rule){
		final int ruleIndex = expandedRules.indexOf(rule);
		if(ruleIndex >= 0)
			expandedRules.get(ruleIndex).from.addAll(rule.from);
		else{
			for(int i = 0, length = expandedRules.size(); i < length; i ++){
				final LineEntry expandedRule = expandedRules.get(i);
				if(expandedRule.isContainedInto(rule)){
					rule.addition.removeAll(expandedRule.addition);
					expandedRule.from.addAll(rule.from);
				}
			}
			expandedRules.add(rule);
		}
	}

	private synchronized List<LineEntry> disjoinConditions(final List<LineEntry> rules){
		final ArrayList<List<LineEntry>> branches = new ArrayList<>(0);
		final List<LineEntry> finalRules = new ArrayList<>(0);
		final StringBuilder condition = new StringBuilder();
		boolean restart = true;
		while(restart){
			restart = false;
			extractTree(branches, rules);

			for(int i = 0, length = branches.size(); !restart && i < length; i ++){
				final List<LineEntry> branch = branches.get(i);
				final int branchSize = branch.size();
				if(branchSize == 1){
					finalRules.addAll(branch);
					branch.clear();
					continue;
				}

				//find if all the branches share the same condition length:
				final int conditionLength = extractSameConditionLength(branch);
				if(conditionLength >= 0){
					disjoinSameConditionLength(branch, comparator);

					finalRules.addAll(branch);
					branch.clear();
					continue;
				}

				final List<LineEntry> properChildren = getProperChildren(branch);
				int properChildrenSize = properChildren.size();
				if(properChildrenSize > 1 && properChildrenSize <= branchSize){
					//must separate each element of the `properChildren` list
					properChildren.clear();
					final LineEntry parent = branch.get(0);
					properChildren.add(parent);
					for(int j = 1; j < branchSize; j ++){
						final LineEntry child = branch.get(j);
						if(child.from.size() < parent.from.size() && parent.condition.length() == child.condition.length()
								&& parent.from.containsAll(child.from))
							properChildren.add(child);
					}
					properChildrenSize = properChildren.size();

					int parentConditionLength = parent.condition.length();
					Set<Character> parentGroup = parent.extractGroup(parentConditionLength);

					//assert each child is disjointed:
					final Map<LineEntry, Set<Character>> childrenGroup = new HashMap<>(branchSize);
					for(int j = 1; j < branchSize; j ++){
						final LineEntry child = branch.get(j);
						childrenGroup.put(child, child.extractGroup(parentConditionLength));
					}
					for(int j = 1; j < properChildrenSize; j ++)
						for(int k = j + 1; k < properChildrenSize; k ++)
							if(!SetHelper.intersection(childrenGroup.get(properChildren.get(j)),
									childrenGroup.get(properChildren.get(k))).isEmpty())
								//TODO
								throw new IllegalStateException("Children are not disjointed, please report this case to the developer, thank you");

					if(properChildrenSize == 1){
						for(int j = 1; !restart && j < branchSize; j ++){
							final LineEntry child = branch.get(j);
							if(!parent.from.containsAll(child.from))
								//TODO
								throw new IllegalStateException("No proper children found, please report this case to the developer, thank you");

							//assert each child is disjointed:
							Set<Character> childGroup = childrenGroup.get(child);

							final Set<Character> intersection = SetHelper.intersection(parentGroup, childGroup);
							if(intersection.equals(childGroup)){
								if(parent.extractFromEndingWith(child.condition).equals(child.from))
									continue;

								//FIXME revise the following piece of code
								//try to separate the child
								childGroup = child.extractGroup(child.condition.length());
								child.condition = RegexHelper.makeGroup(childGroup, comparator) + child.condition;

								extractRules(rules, branches);
								restart = true;
								continue;
							}

							if(!intersection.isEmpty() && parent.condition.length() < child.condition.length()){
								parent.condition = RegexHelper.makeGroup(parentGroup, comparator) + parent.condition;

								parentConditionLength ++;
								parentGroup = parent.extractGroup(parentConditionLength);
								j --;
							}
							else
								//TODO
								throw new IllegalStateException("Case 1, please report this case to the developer, thank you");
						}

						if(restart)
							continue;

						finalRules.addAll(branch);
						branch.clear();
						extractRules(rules, branches);
						restart = true;
						continue;
					}

					for(int j = 1; j < properChildrenSize; j ++){
						final LineEntry child = properChildren.get(j);
						final Set<Character> properChildrenGroup = childrenGroup.get(child);

						final Set<Character> intersectionGroup = SetHelper.intersection(parentGroup, properChildrenGroup);
						parentGroup.removeAll(intersectionGroup);

						condition.setLength(0);
						condition.append(RegexHelper.makeNotGroup(parentGroup, comparator))
							.append(child.condition);
						child.condition = condition.toString();
					}

					finalRules.addAll(properChildren);
					branch.removeAll(properChildren);
					extractRules(rules, branches);
					restart = true;
					continue;
				}

				final List<LineEntry> finals = disjoinDifferentConditionLength(branches, i, comparator);
				if(finals == null){
					extractRules(rules, branches);
					restart = true;
					continue;
				}

				if(!finals.isEmpty()){
					finalRules.addAll(finals);
					branch.removeAll(finals);

					if(!branch.isEmpty()){
						extractRules(rules, branches);
						restart = true;
					}
				}
			}
		}

		return finalRules;
	}

	//NOTE: `rules` will be emptied.
	private static void extractTree(final ArrayList<List<LineEntry>> branches, final List<LineEntry> rules){
		//order by condition length
		rules.sort(Comparator.comparingInt(rule -> RegexHelper.conditionLength(rule.condition)));

		//extract branches whose conditions are disjoint, each branch contains all the rules that share the same ending
		// condition (given by the first item, the (limb) parent, so to say)
		branches.clear();
		branches.ensureCapacity(rules.size());
		while(!rules.isEmpty()){
			final List<LineEntry> branch = extractBranch(rules);
			branches.add(branch);
		}
	}

	/**
	 * Extract all the rules that have the condition in common with the one given.
	 *
	 * @param rules	Collection from which to extract the branch, based on a parent rule (the first item, the limb),
	 * 	whose condition is used to extract all the branches that ends with the very same condition.
	 * @return	The list of branches. The first element being the limb.
	 */
	private static List<LineEntry> extractBranch(final List<LineEntry> rules){
		final LineEntry parent = rules.get(0);
		final List<LineEntry> branch = new ArrayList<>(rules.size());
		final Iterator<LineEntry> itr = rules.iterator();
		while(itr.hasNext()){
			final LineEntry rule = itr.next();
			if(rule.condition.endsWith(parent.condition)){
				branch.add(rule);
				itr.remove();
			}
		}
		return branch;
	}

	private static int extractSameConditionLength(final List<LineEntry> branch){
		final int conditionLength = branch.get(0).condition.length();
		for(int i = 0, length = branch.size(); i < length; i ++){
			final LineEntry entry = branch.get(i);
			if(conditionLength != entry.condition.length())
				return -1;
		}
		return conditionLength;
	}

	//TODO if parent group has a non-empty intersection with one of its children, then parent.from must contain child.from (for child with condition as parent.condition?)
	private static void disjoinSameConditionLength(final List<LineEntry> branch, final Comparator<String> comparator){
		final Map<LineEntry, Set<Character>> branchGroup = new HashMap<>(branch.size());
		final int conditionLength = branch.getFirst()
			.condition.length();
		for(int i = 0, length = branch.size(); i < length; i ++){
			final LineEntry rule = branch.get(i);
			branchGroup.put(rule, rule.extractGroup(conditionLength));
		}

		final Collection<Character> alphabetGroup = new HashSet<>(branchGroup.size());
		for(final Set<Character> ruleGroup : branchGroup.values())
			alphabetGroup.addAll(ruleGroup);

		final StringBuilder condition = new StringBuilder();
		final Collection<Character> negatedGroup = new HashSet<>(0);
		for(int i = 0, length = branch.size(); i < length; i ++){
			final LineEntry entry = branch.get(i);

			final Set<Character> ratifyingGroup = branchGroup.get(entry);
			negatedGroup.addAll(alphabetGroup);
			negatedGroup.removeAll(ratifyingGroup);

			final int ratifyingSize = ratifyingGroup.size();
			final int negatedSize = negatedGroup.size();
			final boolean chooseRatifyingOverNegated = (ratifyingSize < negatedSize + Math.max(conditionLength - 3, 0));
			final String augment = (chooseRatifyingOverNegated
				? RegexHelper.makeGroup(ratifyingGroup, comparator)
				: RegexHelper.makeNotGroup(negatedGroup, comparator));
			condition.setLength(0);
			condition.append(augment)
				.append(entry.condition);
			entry.condition = condition.toString();
		}
	}

	private static List<LineEntry> getProperChildren(final List<LineEntry> branch){
		//if parent contains into one if its children (that have child.condition.length > parent.length),
		//then the parent is a final rule
		final List<LineEntry> properChildren = new ArrayList<>(branch.size());
		final LineEntry parent = branch.get(0);
		properChildren.add(parent);
		for(int i = 1, length = branch.size(); i < length; i ++){
			final LineEntry child = branch.get(i);
			if(parent.condition.length() < child.condition.length() && parent.from.containsAll(child.from))
				properChildren.add(child);
		}
		return properChildren;
	}

	private static List<LineEntry> disjoinDifferentConditionLength(final List<List<LineEntry>> branches,
			final int branchIndex, final Comparator<String> comparator){
		final List<LineEntry> branch = branches.get(branchIndex);
		final LineEntry parent = branch.get(0);

		final Set<Character> childrenGroup = new HashSet<>(0);
		final StringBuilder condition = new StringBuilder();
		for(int i = 1, length = branch.size(); i < length; i ++){
			//augment parent condition to avoid any intersection:
			final int parentConditionLength = parent.condition.length();
			final Set<Character> parentGroup = parent.extractGroup(parentConditionLength);

			//FIXME useful?
//			childrenGroup.clear();
			for(int j = 1, length2 = branch.size(); j < length2; j ++){
				final LineEntry child = branch.get(j);
				childrenGroup.addAll(child.extractGroup(parentConditionLength));
			}

			final Set<Character> intersectionGroup = SetHelper.intersection(parentGroup, childrenGroup);
			if(intersectionGroup.isEmpty()){
				final int ratifyingSize = parentGroup.size();
				final int negatedSize = childrenGroup.size();
				final boolean chooseRatifyingOverNegated = (ratifyingSize < negatedSize + Math.max(parentConditionLength - 3, 0)
					&& ratifyingSize > 0
					|| negatedSize == 0
				);
				final String augment = (chooseRatifyingOverNegated
					? RegexHelper.makeGroup(parentGroup, comparator)
					: RegexHelper.makeNotGroup(childrenGroup, comparator));
				condition.setLength(0);
				condition.append(augment)
					.append(parent.condition);
				parent.condition = condition.toString();

				return Collections.singletonList(parent);
			}

			if(parentGroup.equals(intersectionGroup)){
				if(parentGroup.size() == 1)
					parent.condition = parentGroup.iterator().next() + parent.condition;
				else{
					branch.remove(parent);
					for(final Character chr : parentGroup){
						final LineEntry newRule = LineEntry.createFrom(parent, chr + parent.condition);
						branch.add(newRule);
					}
				}

				return null;
			}

			if(childrenGroup.equals(intersectionGroup)){
				for(final Character chr : intersectionGroup){
					final LineEntry newRule = LineEntry.createFrom(parent, chr + parent.condition);
					branch.add(newRule);
				}

				final int ratifyingSize = parentGroup.size();
				final int negatedSize = childrenGroup.size();
				final boolean chooseRatifyingOverNegated = (ratifyingSize < negatedSize + Math.max(parentConditionLength - 3, 0)
					&& ratifyingSize > 0
					|| negatedSize == 0
				);
				final String augment = (chooseRatifyingOverNegated
					? RegexHelper.makeGroup(parentGroup, comparator)
					: RegexHelper.makeNotGroup(childrenGroup, comparator));
				condition.setLength(0);
				condition.append(augment)
					.append(parent.condition);
				parent.condition = condition.toString();

				return Collections.singletonList(parent);
			}

			parentGroup.removeAll(intersectionGroup);

			final int ratifyingSize = parentGroup.size();
			final int negatedSize = childrenGroup.size();
			final boolean chooseRatifyingOverNegated = (ratifyingSize < negatedSize + Math.max(parentConditionLength - 3, 0)
				&& ratifyingSize > 0
				|| negatedSize == 0
			);
			final String augment = (chooseRatifyingOverNegated
				? RegexHelper.makeGroup(parentGroup, comparator)
				: RegexHelper.makeNotGroup(childrenGroup, comparator));
			condition.setLength(0);
			condition.append(augment)
				.append(parent.condition);
			final LineEntry newParent = LineEntry.createFrom(parent, condition.toString());
			branch.remove(0);

			for(final Character chr : intersectionGroup){
				final LineEntry newRule = LineEntry.createFrom(parent, chr + parent.condition);
				branch.add(newRule);
			}

			return Collections.singletonList(newParent);
		}

		return Collections.emptyList();
	}

	private static void extractRules(final Collection<LineEntry> rules, final List<List<LineEntry>> branches){
		rules.clear();
		for(int i = 0, length = branches.size(); i < length; i ++)
			rules.addAll(branches.get(i));
	}

	/** Merge common conditions (ex. `[^a]bc` and `[^a]dc` will become `[^a][bd]c`). */
	private static void mergeSimilarRules(final Collection<LineEntry> rules, final Comparator<String> comparator){
		final Map<String, List<LineEntry>> similarityBucket = SetHelper.bucket(rules,
			rule -> (rule.condition.contains(RegexHelper.GROUP_END)
				? rule.removal + TAB + rule.addition + TAB + RegexSequencer.splitSequence(rule.condition)[0] + TAB
				+ RegexSequencer.splitSequence(rule.condition).length
				: null));
		final Collection<Character> group = new HashSet<>(0);
		final StringBuilder condition = new StringBuilder();
		for(final List<LineEntry> similarities : similarityBucket.values())
			if(similarities.size() > 1){
				final LineEntry anEntry = similarities.get(0);
				final String[] aCondition = RegexSequencer.splitSequence(anEntry.condition);
				final String[] commonPreCondition = RegexSequencer.subSequence(aCondition, 0, 1);
				final String[] commonPostCondition = RegexSequencer.subSequence(aCondition, 2);
				//extract all the rules from `similarities` that has the condition compatible with `firstEntry.condition`
				group.clear();
				for(int i = 0, length = similarities.size(); i < length; i ++)
					group.add(RegexSequencer.splitSequence(similarities.get(i).condition)[1].charAt(0));

				condition.setLength(0);
				for(int i = 0, length = commonPreCondition.length; i < length; i ++)
					condition.append(commonPreCondition[i]);
				condition.append(RegexHelper.makeGroup(group, comparator));
				for(int i = 0, length = commonPostCondition.length; i < length; i ++)
					condition.append(commonPostCondition[i]);
				condition.toString();

				final LineEntry newRule = LineEntry.createFrom(anEntry, condition.toString());
				for(int i = 1, length = similarities.size(); i < length; i ++)
					newRule.from.addAll(similarities.get(i).from);
				rules.add(newRule);

				for(int i = 0, length = similarities.size(); i < length; i ++)
					rules.remove(similarities.get(i));
			}
	}





	public final List<String> convertFormat(final String flag, final boolean keepLongestCommonAffix,
			final List<LineEntry> compactedRules){
		compactedRules.sort(
			Comparator.comparingInt(rule -> RegexHelper.conditionLength(((LineEntry)rule).condition))
				.thenComparing(rule -> ((LineEntry)rule).condition));

		final RuleEntry ruleToBeReduced = affixData.getData(flag);
		if(ruleToBeReduced == null)
			throw new LinterException(NON_EXISTENT_RULE, flag);

		final AffixType type = ruleToBeReduced.getType();
		final List<String> prettyPrintRules = convertEntriesToRules(flag, type, keepLongestCommonAffix, compactedRules);
		prettyPrintRules.add(0, LineEntry.toHunspellHeader(type, flag, ruleToBeReduced.combinableChar(),
			prettyPrintRules.size()));
		return prettyPrintRules;
	}

	private List<String> convertEntriesToRules(final String flag, final AffixType type,
			final boolean keepLongestCommonAffix, final List<LineEntry> entries){
		List<LineEntry> sortedEntries = Collections.emptyList();
		if(!entries.isEmpty()){
			//restore original rules
			final ArrayList<LineEntry> restoredRules = new ArrayList<>(0);
			for(int i = 0, length = entries.size(); i < length; i ++){
				final LineEntry rule = entries.get(i);
				restoredRules.ensureCapacity(rule.addition.size());
				for(final String addition : rule.addition){
					final int lcp = StringHelper.longestCommonPrefix(rule.removal, addition).length();
					final String removal = rule.removal.substring(lcp);
					final LineEntry entry = new LineEntry((removal.isEmpty()? StringUtils.EMPTY: removal),
						addition.substring(lcp), rule.condition, rule.from);
					restoredRules.add(type == AffixType.SUFFIX? entry: entry.reverse());
				}
			}
			sortedEntries = prepareRules(keepLongestCommonAffix, restoredRules);
		}

		return composeAffixRules(flag, type, sortedEntries);
	}

	private List<LineEntry> prepareRules(final boolean keepLongestCommonAffix, final List<LineEntry> entries){
		if(keepLongestCommonAffix)
			for(int i = 0, length = entries.size(); i < length; i ++)
				entries.get(i).expandConditionToMaxLength(comparator);

		final List<LineEntry> list = (entries != null? new ArrayList<>(entries): new ArrayList<>(0));
		list.sort(lineEntryComparator);
		return list;
	}

	private static List<String> composeAffixRules(final String flag, final AffixType type,
			final List<LineEntry> entries){
		final List<String> list = new ArrayList<>(entries.size());
		for(int i = 0, length = entries.size(); i < length; i ++)
			list.add(entries.get(i).toHunspellRule(type, flag));
		return list;
	}


	public final void checkReductionCorrectness(final String flag, final List<String> reducedRules,
			final Set<String> originalLines, final ProgressCallback progressCallback){
		final RuleEntry ruleToBeReduced = affixData.getData(flag);
		if(ruleToBeReduced == null)
			throw new LinterException(NON_EXISTENT_RULE, flag);

		final AffixType type = ruleToBeReduced.getType();

		//read the header
		final RuleEntry overriddenParent = new RuleEntry(type, flag, ruleToBeReduced.combinableChar());
		//extract rules (skip the header)
		final List<AffixEntry> entries = new ArrayList<>(reducedRules.size() - 1);
		for(int i = 1, length = reducedRules.size(); i < length; i ++){
			final String reducedRule = reducedRules.get(i);
			final AffixEntry entry = new AffixEntry(reducedRule, i - 1, type, flag, strategy, null,
					null)
				.setParent(overriddenParent);
			entries.add(entry);
		}
		overriddenParent.setEntries(entries);

		int progress = 0;
		int progressIndex = 0;
		final int progressStep = (int)Math.ceil(originalLines.size() / 100.f);
		final Collection<DictionaryEntry> originalInflectionsWhole = new HashSet<>(0);
		final Collection<DictionaryEntry> inflectionsWhole = new HashSet<>(0);
		for(final String line : originalLines){
			final DictionaryEntry dicEntry = dictionaryEntryFactory.createFromDictionaryLine(line);
			final List<Inflection> originalInflections = wordGenerator.applyAffixRules(dicEntry);
			final List<Inflection> inflections = wordGenerator.applyAffixRulesWithoutOutputConversion(dicEntry, overriddenParent);

			originalInflectionsWhole.clear();
			for(int j = 0, length2 = originalInflections.size(); j < length2; j ++)
				originalInflectionsWhole.add(new DictionaryEntry(originalInflections.get(j)));
			inflectionsWhole.clear();
			for(int j = 0, length2 = inflections.size(); j < length2; j ++)
				inflectionsWhole.add(new DictionaryEntry(inflections.get(j)));
			if(!originalInflectionsWhole.equals(inflectionsWhole))
				throw new LinterException(VERY_BAD_ERROR, line, originalInflectionsWhole, inflectionsWhole);

			if(progressCallback != null && ++ progress % progressStep == 0)
				progressCallback.accept(++ progressIndex);
		}
	}


	public final List<LineEntry> collectInflectionsByFlag(final List<Inflection> inflections, final String flag,
			final AffixType type){
		//collect all inflections that generate from the given flag
		if(inflections.isEmpty())
			return null;

		final List<LineEntry> filteredRules = new ArrayList<>(inflections.size() - 1);
		//skip base inflection
		for(int i = WordGenerator.BASE_INFLECTION_INDEX + 1, length = inflections.size(); i < length; i ++){
			final Inflection inflection = inflections.get(i);
			final AffixEntry lastAppliedRule = inflection.getLastAppliedRule(type);
			if(lastAppliedRule != null && lastAppliedRule.getFlag().equals(flag)){
				final String word = lastAppliedRule.undoRule(inflection.getWord());
				final LineEntry newEntry = createAffixEntry(inflection, word, type);
				filteredRules.add(newEntry);
			}
		}
		return filteredRules;
	}

	private static LineEntry compactInflections(final List<LineEntry> rules){
		if(rules.size() > 1){
			//retrieve rule with the longest condition (all the other conditions must be this long)
			LineEntry maxConditionEntry = null;
			int maxConditionLength = 0;
			for(int i = 0, length = rules.size(); i < length; i ++){
				final LineEntry elem = rules.get(i);
				if(maxConditionEntry == null || elem.condition.length() > maxConditionLength){
					maxConditionEntry = elem;
					maxConditionLength = elem.condition.length();
				}
			}

			expandAddition(rules, maxConditionEntry);

			return maxConditionEntry;
		}
		else
			return (!rules.isEmpty()? rules.get(0): null);
	}

	private static void expandAddition(final List<LineEntry> rules, final LineEntry compactedRule){
		final String from = rules.get(0).from.iterator().next();
		final int longestConditionLength = compactedRule.condition.length();
		for(int i = 0, length = rules.size(); i < length; i ++){
			final LineEntry rule = rules.get(i);
			//recover the missing characters for the current condition to become of length the maximum found earlier
			final int startIndex = from.length() - longestConditionLength;
			//if a condition is not long enough, keep it separate
			if(startIndex >= 0){
				final int delta = longestConditionLength - rule.condition.length();
				final String deltaAddition = from.substring(startIndex, startIndex + delta);
				//add addition
				for(final String addition : rule.addition)
					compactedRule.addition.add(deltaAddition + addition);
			}
		}
	}

}
