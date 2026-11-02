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

import io.github.mtrevisan.hunlinter.gui.ProgressCallback;
import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
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
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Reduces the set of Hunspell rules of a single flag to the smallest set of lines that reproduces <em>exactly</em> the
 * same inflections on the words that use the flag.
 * <p>
 * The dictionary is the oracle. Every plain rule collected from the dictionary (see
 * {@link #collectInflectionsByFlag(List, String, AffixType)}) states a <em>fact</em>: the word {@code w} produces,
 * through this flag, the string {@code w[:-removal] + addition}. The addition carries any continuation flag, hence a
 * second application of a rule is handled like any other: the `from` word of a fact is the word the rule was applied to,
 * whatever its origin.
 * <p>
 * Reconstruction goes as follows:
 * <ol>
 * 	<li>each fact is normalized to its <em>minimal</em> transformation (the common prefix of removal and addition is
 * 		dropped); facts with different minimal transformations can never share a line, so the problem splits into
 * 		independent sub-problems, one per transformation;</li>
 * 	<li>for a transformation {@code (r, a)}, the words ending with {@code r} are <em>positive</em> if they have the fact,
 * 		<em>negative</em> otherwise: a condition is valid iff it matches no negative word (this is the oracle: a valid
 * 		line can only produce expected inflections);</li>
 * 	<li>valid conditions are generated from a reverse trie of the words (pure sub-trees and groups of pure siblings),
 * 		then enlarged by merging conditions that differ by a single token;</li>
 * 	<li>a greedy set cover picks the fewest conditions that cover every positive word, and redundant ones are dropped;</li>
 * 	<li>each condition is finally written in the shortest form (positive or negated group, useless leading wildcard
 * 		removed).</li>
 * </ol>
 * Everything is computed on suffix-oriented data, as the {@link RulesReducer} does (prefixes are reversed by
 * {@link #collectInflectionsByFlag(List, String, AffixType)} and restored by
 * {@link #convertFormat(String, boolean, List)}).
 */
public class RulesReducer{

	private static final String NON_SEPARABLE = "Cannot separate the word `{}` from the other words ending with `{}`: "
		+ "no condition can select it for the transformation `{}` -> `{}`";
	private static final String INTERNAL_ERROR = "Internal error: the condition `{}` selects the word `{}` that does not undergo `{}` -> `{}`";

	private static final String TOKEN_SEPARATOR = "\u0001";
	/** Safety limit on the number of candidate conditions generated for a single transformation. */
	private static final int MAX_POOL_SIZE = 20_000;


	private static final String NON_EXISTENT_RULE = "Non-existent rule `{}`, cannot reduce";
	private static final String VERY_BAD_ERROR = "Something very bad happened while inflecting from `{}`, expected {}, obtained {}";


	private final AffixData affixData;
	private final DictionaryEntryFactory dictionaryEntryFactory;
	private final FlagParsingStrategy strategy;
	private final WordGenerator wordGenerator;
	private final Comparator<String> comparator;
	private final Comparator<LineEntry> lineEntryComparator;
	private final Comparator<LineEntry> outputComparator;


	public RulesReducer(final AffixData affixData, final WordGenerator wordGenerator){
		Objects.requireNonNull(affixData, "Affix data cannot be null");
		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");

		this.affixData = affixData;
		this.wordGenerator = wordGenerator;
		dictionaryEntryFactory = new DictionaryEntryFactory(affixData);
		strategy = affixData.getFlagParsingStrategy();
		comparator = BaseBuilder.getComparator(affixData.getLanguage());
		//order of the rules once written in the `.aff` format
		outputComparator = Comparator.comparingInt((LineEntry entry)
				-> RegexSequencer.splitSequence(entry.condition).length)
			.thenComparingInt(entry -> StringUtils.countMatches(entry.condition, RegexHelper.GROUP_END))
			.thenComparingInt(entry -> entry.removal.length())
			.thenComparing(entry -> StringUtils.reverse(entry.condition), comparator)
			.thenComparing(entry -> entry.removal, comparator)
			.thenComparingInt(entry -> entry.firstAddition().length())
			.thenComparing(LineEntry::firstAddition, comparator);
		lineEntryComparator = Comparator.comparingInt((LineEntry entry) -> RegexHelper.conditionLength(entry.condition))
			.thenComparing((LineEntry entry) -> entry.condition, comparator)
			.thenComparing((LineEntry entry) -> entry.removal, comparator)
			.thenComparing((LineEntry entry) -> String.join(TOKEN_SEPARATOR, new TreeSet<>(entry.addition)), comparator);
	}


	//-- collect, format and verify --

	/**
	 * Collects all the inflections generated by the given flag, as plain rules (one for each word and inflection).
	 * Everything is suffix-oriented: for a prefix, words are reversed (and restored by
	 * {@link #convertFormat(String, boolean, List)}).
	 */
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

	private LineEntry createAffixEntry(final Inflection inflection, String word, final AffixType type){
		String producedWord = inflection.getWord();
		if(type == AffixType.PREFIX){
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
			sortedEntries = prepareRules(flag, keepLongestCommonAffix, restoredRules);
		}

		return composeAffixRules(flag, type, sortedEntries);
	}

	/**
	 * Sorts the rules from the shortest added part to the longest one (the continuation flags and the morphological fields
	 * are not part of it). The rules with an added part of the same length are sorted by the collator of the language (for
	 * Venetan, the one defined by <code>WordVEC.COLLATOR_RULE</code>). The remaining ties keep the order they have in the
	 * original `.aff` file, so the order of the morphological tags is not changed (the tags themselves are never touched).
	 */
	private List<LineEntry> prepareRules(final String flag, final boolean keepLongestCommonAffix,
			final List<LineEntry> entries){
		if(keepLongestCommonAffix)
			for(int i = 0, length = entries.size(); i < length; i ++)
				entries.get(i).expandConditionToMaxLength(comparator);

		final Map<String, Integer> originalOrder = extractOriginalOrder(flag);
		final List<LineEntry> list = new ArrayList<>(entries);
		list.sort(Comparator.comparingInt((LineEntry entry) -> addedPart(entry.firstAddition()).length())
			.thenComparing(entry -> addedPart(entry.firstAddition()), comparator)
			.thenComparingInt(entry -> originalOrder.getOrDefault(orderKey(entry.removal, entry.firstAddition()),
				Integer.MAX_VALUE))
			.thenComparing(outputComparator));
		return list;
	}

	/** @return	The part that is actually added to the word, that is without the continuation flags and the morphological fields. */
	private static String addedPart(final String addition){
		for(int i = 0; i < addition.length(); i ++){
			final char chr = addition.charAt(i);
			if(chr == '/' || chr == '\t')
				return addition.substring(0, i);
		}
		return addition;
	}

	/** @return	The position, in the `.aff` file, of the first rule that does a given transformation. */
	private Map<String, Integer> extractOriginalOrder(final String flag){
		final Map<String, Integer> order = new HashMap<>();
		final RuleEntry rule = affixData.getData(flag);
		final List<AffixEntry> originalEntries = (rule != null? rule.getEntries(): null);
		for(int i = 0, length = (originalEntries != null? originalEntries.size(): 0); i < length; i ++){
			final AffixEntry entry = originalEntries.get(i);
			order.putIfAbsent(orderKey(entry.getRemoving(), entry.getAppending() + entry.toString(strategy)), i);
		}
		return order;
	}

	/**
	 * @param removal	The part to be removed.
	 * @param addition	The part to be added, followed by the continuation flags and the morphological fields (if any).
	 * @return	An identifier of the transformation, independent of the common prefix of removal and addition.
	 */
	private static String orderKey(final String removal, final String addition){
		int surfaceEnd = addition.length();
		for(int i = 0; i < addition.length(); i ++){
			final char chr = addition.charAt(i);
			if(chr == '/' || chr == '\t'){
				surfaceEnd = i;
				break;
			}
		}
		int lcp = 0;
		final int maxLcp = Math.min(removal.length(), surfaceEnd);
		while(lcp < maxLcp && removal.charAt(lcp) == addition.charAt(lcp))
			lcp ++;
		return removal.substring(lcp) + TOKEN_SEPARATOR + addition.substring(lcp);
	}

	private static List<String> composeAffixRules(final String flag, final AffixType type,
			final List<LineEntry> entries){
		final List<String> list = new ArrayList<>(entries.size());
		for(int i = 0, length = entries.size(); i < length; i ++)
			list.add(entries.get(i).toHunspellRule(type, flag));
		return list;
	}

	/**
	 * Verifies that the reduced rules generate, for each dictionary line, exactly the same inflections of the original rules.
	 *
	 * @throws LinterException	If an inflection differs.
	 */
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

		//each line is independent from the others, so they are verified in parallel
		final AtomicInteger progress = new AtomicInteger(0);
		final AtomicInteger progressIndex = new AtomicInteger(0);
		final int progressStep = Math.max(1, (int)Math.ceil(originalLines.size() / 100.f));
		originalLines.parallelStream()
			.forEach(line -> {
				final DictionaryEntry dicEntry = dictionaryEntryFactory.createFromDictionaryLine(line);
				final List<Inflection> originalInflections = wordGenerator.applyAffixRules(dicEntry);
				final List<Inflection> inflections = wordGenerator.applyAffixRulesWithoutOutputConversion(dicEntry, overriddenParent);

				final Collection<DictionaryEntry> originalInflectionsWhole = new HashSet<>(originalInflections.size());
				for(int j = 0, length2 = originalInflections.size(); j < length2; j ++)
					originalInflectionsWhole.add(new DictionaryEntry(originalInflections.get(j)));
				final Collection<DictionaryEntry> inflectionsWhole = new HashSet<>(inflections.size());
				for(int j = 0, length2 = inflections.size(); j < length2; j ++)
					inflectionsWhole.add(new DictionaryEntry(inflections.get(j)));
				if(!originalInflectionsWhole.equals(inflectionsWhole))
					throw new LinterException(VERY_BAD_ERROR, line, originalInflectionsWhole, inflectionsWhole);

				if(progressCallback != null && progress.incrementAndGet() % progressStep == 0)
					progressCallback.accept(progressIndex.incrementAndGet());
			});
	}

	/**
	 * Collects the flags that can lead to the given flag: the flag itself, and any flag with a rule that has (directly or
	 * through other flags) the given flag as continuation flag.
	 * <p>
	 * A dictionary line that does not carry any of these flags can never be inflected by the given flag, so it can be
	 * skipped (see {@link #hasAnyFlag(DictionaryEntry, Collection)}).
	 */
	public final Set<String> collectFlagsLeadingTo(final String flag){
		final Set<String> flags = new HashSet<>();
		flags.add(flag);
		final List<RuleEntry> rules = affixData.getRuleEntries();
		boolean changed;
		do{
			changed = false;

			for(final RuleEntry rule : rules){
				if(flags.contains(rule.getFlag()))
					continue;

				boolean leads = false;
				final List<AffixEntry> ruleEntries = rule.getEntries();
				for(int i = 0, length = (ruleEntries != null? ruleEntries.size(): 0); !leads && i < length; i ++){
					final AffixEntry entry = ruleEntries.get(i);
					if(entry.hasContinuationFlags())
						for(final String target : flags)
							if(entry.hasContinuationFlag(target)){
								leads = true;
								break;
							}
				}
				if(leads){
					flags.add(rule.getFlag());
					changed = true;
				}
			}
		}while(changed);
		return flags;
	}

	/** @return	Whether the dictionary entry carries at least one of the given flags. */
	public static boolean hasAnyFlag(final DictionaryEntry dicEntry, final Collection<String> flags){
		final List<String> continuationFlags = dicEntry.getContinuationFlags();
		if(continuationFlags != null)
			for(int i = 0, length = continuationFlags.size(); i < length; i ++)
				if(flags.contains(continuationFlags.get(i)))
					return true;
		return false;
	}


	//-- reduction --

	public final List<LineEntry> reduceRules(final Collection<LineEntry> plainRules){
		return reduceRules(plainRules, null, null);
	}

	public final List<LineEntry> reduceRules(final Collection<LineEntry> plainRules, final ProgressCallback progressCallback){
		return reduceRules(plainRules, null, progressCallback);
	}

	/**
	 * Collects the words (lemma or inflections) that carry the flag but the rule does not apply to them: they have to be negative words for the
	 * reduction, otherwise a reduced condition could select it (and the original rules, that do not apply to it, would be
	 * changed).
	 *
	 * @param inflections	The inflections of a dictionary line.
	 * @param flag	The flag being reduced.
	 * @param type	The type of the flag.
	 * @param plainRules	The plain rules collected for the same dictionary line.
	 * @return	The (suffix-oriented) lemma, if it has the flag and generates nothing.
	 */
	public final Set<String> collectInertWords(final List<Inflection> inflections, final String flag, final AffixType type,
			final Collection<LineEntry> plainRules){
		final Set<String> applied = new HashSet<>(0);
		if(plainRules != null)
			for(final LineEntry entry : plainRules)
				applied.addAll(entry.from);

		//every word (lemma or inflection) that carries the flag but undergoes no transformation
		final Set<String> inert = new HashSet<>(0);
		for(int i = 0, length = inflections.size(); i < length; i ++){
			final Inflection inflection = inflections.get(i);
			if(inflection.hasContinuationFlag(flag)){
				final String word = (type == AffixType.PREFIX? StringUtils.reverse(inflection.getWord()): inflection.getWord());
				if(!applied.contains(word))
					inert.add(word);
			}
		}
		return inert;
	}

	/**
	 * Reduces the plain rules (one for each word and inflection) to the minimal set of rules.
	 *
	 * @param plainRules	The plain rules, as returned by {@link #collectInflectionsByFlag(List, String, AffixType)}.
	 * @param inertWords	Optional (suffix-oriented) words that carry the flag but undergo no transformation (see
	 * 	{@link #collectInertWords(List, String, AffixType, Collection)}): no condition can select them.
	 * @param progressCallback	Optional callback that receives the progress percentage.
	 * @return	The reduced rules.
	 * @throws LinterException	If a word cannot be separated from the others (the input is not representable by a Hunspell
	 * 	suffix rule set).
	 */
	public final List<LineEntry> reduceRules(final Collection<LineEntry> plainRules, final Collection<String> inertWords,
			final ProgressCallback progressCallback){
		final List<LineEntry> result = new ArrayList<>(0);
		if(plainRules == null || plainRules.isEmpty())
			return result;

		//1. collect the facts, grouped by minimal transformation
		final Map<Transformation, Set<String>> positivesByTransformation = new TreeMap<>();
		final Set<String> allWords = new TreeSet<>(Comparator.comparing(RulesReducer::reverse));
		for(final LineEntry entry : plainRules)
			for(final String addition : entry.addition)
				for(final String word : entry.from){
					if(!word.endsWith(entry.removal))
						throw new IllegalArgumentException("Word `" + word + "` does not end with the removal `" + entry.removal + "`");

					positivesByTransformation.computeIfAbsent(Transformation.of(entry.removal, addition), k -> new TreeSet<>())
						.add(word);
					allWords.add(word);
				}
		//a word that has the flag but undergoes no transformation is a negative word for every transformation
		if(inertWords != null)
			allWords.addAll(inertWords);
		if(progressCallback != null)
			progressCallback.accept(5);

		//2. words ending with a given removal (in reversed-lexicographic order, that is the order of the trie)
		final Map<String, List<String>> wordsByRemoval = new HashMap<>();
		for(final Transformation transformation : positivesByTransformation.keySet())
			wordsByRemoval.computeIfAbsent(transformation.removal, removal -> {
				final List<String> words = new ArrayList<>();
				for(final String word : allWords)
					if(word.endsWith(removal))
						words.add(word);
				return words;
			});
		if(progressCallback != null)
			progressCallback.accept(10);

		//3. solve each transformation independently
		final List<Line> lines = new ArrayList<>();
		int done = 0;
		final int total = positivesByTransformation.size();
		for(final Map.Entry<Transformation, Set<String>> entry : positivesByTransformation.entrySet()){
			final Transformation transformation = entry.getKey();
			lines.addAll(solve(transformation, entry.getValue(), wordsByRemoval.get(transformation.removal)));

			if(progressCallback != null)
				progressCallback.accept(10 + 85 * (++ done) / total);
		}

		//4. express the lines as entries
		result.addAll(toEntries(lines));
		result.sort(lineEntryComparator);
		if(progressCallback != null)
			progressCallback.accept(100);
		return result;
	}


	//-- one transformation --

	/**
	 * @param transformation	The transformation shared by all the words in {@code positives}.
	 * @param positives	The words that undergo the transformation.
	 * @param words	All the words ending with the removal of the transformation, in reversed-lexicographic order.
	 * @return	The minimal set of lines that apply the transformation to all the positives and to no other word.
	 */
	private static List<Line> solve(final Transformation transformation, final Set<String> positives,
			final List<String> words){
		final String removal = transformation.removal;
		final int removalLength = removal.length();

		//build the reverse trie; positive words get their index in the order of insertion (that is the order of the trie, so
		// the positives of any sub-tree are contiguous)
		final Node root = new Node();
		final List<String> positiveList = new ArrayList<>(positives.size());
		for(final String word : words){
			final boolean positive = positives.contains(word);
			final int index = (positive? positiveList.size(): -1);
			if(positive)
				positiveList.add(word);

			Node node = root;
			node.add(index);
			for(int i = word.length() - removalLength - 1; i >= 0; i --){
				node = node.children.computeIfAbsent(word.charAt(i), k -> new Node());
				node.add(index);
			}
			if(positive)
				node.exactPositive = word;
		}
		final int positiveCount = positiveList.size();

		//generate the candidate conditions
		final Map<String, Candidate> pool = new LinkedHashMap<>();
		collectCandidates(root, new ArrayList<>(), transformation, removal, pool);
		mergeCandidates(pool);

		//greedy set cover
		final List<Candidate> selected = new ArrayList<>();
		final BitSet uncovered = new BitSet(positiveCount);
		uncovered.set(0, positiveCount);
		final BitSet gainMask = new BitSet(positiveCount);
		while(!uncovered.isEmpty()){
			Candidate best = null;
			int bestGain = 0;
			for(final Candidate candidate : pool.values()){
				gainMask.clear();
				gainMask.or(candidate.coverage);
				gainMask.and(uncovered);
				final int gain = gainMask.cardinality();
				if(gain > 0 && (best == null || gain > bestGain || gain == bestGain && candidate.isBetterThan(best))){
					best = candidate;
					bestGain = gain;
				}
			}
			if(best == null)
				throw new IllegalStateException("Cannot cover the words " + uncovered + " for " + transformation);

			selected.add(best);
			uncovered.andNot(best.coverage);
		}

		//remove the redundant conditions
		for(int i = selected.size() - 1; i >= 0; i --){
			final BitSet others = new BitSet(positiveCount);
			for(int j = 0; j < selected.size(); j ++)
				if(j != i)
					others.or(selected.get(j).coverage);
			final BitSet own = (BitSet)selected.get(i).coverage.clone();
			own.andNot(others);
			if(own.isEmpty())
				selected.remove(i);
		}

		final List<Line> lines = new ArrayList<>(selected.size());
		for(final Candidate candidate : selected)
			lines.add(finalizeLine(candidate, transformation, positives, words));
		return lines;
	}

	/**
	 * Visit the trie: a sub-tree with no negative words is a valid condition by itself; otherwise the children without
	 * negative words are grouped into a single token, and the children with both kinds of words are explored further.
	 */
	private static void collectCandidates(final Node node, final List<Character> path, final Transformation transformation,
			final String removal, final Map<String, Candidate> pool){
		if(node.positives == 0)
			return;

		if(node.negatives == 0){
			addCandidate(pool, tokensOf(path, null), node.coverage());
			return;
		}

		if(node.exactPositive != null)
			//any condition that matches the word matches every other word ending with it too
			throw new LinterException(NON_SEPARABLE, node.exactPositive, removal, transformation.removal,
				transformation.addition);

		final Set<Character> pure = new TreeSet<>();
		final BitSet pureCoverage = new BitSet();
		for(final Map.Entry<Character, Node> child : node.children.entrySet()){
			final Node n = child.getValue();
			if(n.positives > 0 && n.negatives == 0){
				pure.add(child.getKey());
				pureCoverage.or(n.coverage());
			}
		}
		if(!pure.isEmpty())
			addCandidate(pool, tokensOf(path, pure), pureCoverage);

		for(final Map.Entry<Character, Node> child : node.children.entrySet()){
			final Node n = child.getValue();
			if(n.positives > 0 && n.negatives > 0){
				path.add(child.getKey());
				collectCandidates(n, path, transformation, removal, pool);
				path.removeLast();
			}
		}
	}

	/** Tokens are ordered from the nearest to the removal to the farthest. */
	private static List<String> tokensOf(final List<Character> path, final Set<Character> group){
		final List<String> tokens = new ArrayList<>(path.size() + 1);
		for(final Character chr : path)
			tokens.add(String.valueOf(chr));
		if(group != null){
			final StringBuilder sb = new StringBuilder();
			for(final Character chr : group)
				sb.append(chr);
			tokens.add(sb.toString());
		}
		return tokens;
	}

	private static void addCandidate(final Map<String, Candidate> pool, final List<String> tokens, final BitSet coverage){
		final Candidate candidate = new Candidate(tokens, coverage);
		final Candidate existing = pool.get(candidate.key);
		if(existing == null)
			pool.put(candidate.key, candidate);
		else
			existing.coverage.or(coverage);
	}

	/**
	 * Two conditions of the same length that differ in exactly one token select the union of the words they select
	 * (still no negative word), so they are replaced by a condition whose token is the union of the two.
	 */
	private static void mergeCandidates(final Map<String, Candidate> pool){
		boolean changed;
		do{
			changed = false;

			final Map<String, List<Candidate>> clusters = new LinkedHashMap<>();
			final Map<String, Integer> clusterPositions = new HashMap<>();
			for(final Candidate candidate : pool.values())
				for(int i = 0, length = candidate.tokens.size(); i < length; i ++){
					final String key = i + TOKEN_SEPARATOR + length + TOKEN_SEPARATOR + candidate.keyWithout(i);
					clusters.computeIfAbsent(key, k -> new ArrayList<>(2))
						.add(candidate);
					clusterPositions.put(key, i);
				}

			for(final Map.Entry<String, List<Candidate>> cluster : clusters.entrySet()){
				final List<Candidate> members = cluster.getValue();
				if(members.size() < 2)
					continue;

				final int position = clusterPositions.get(cluster.getKey());
				final Set<Character> union = new TreeSet<>();
				final BitSet coverage = new BitSet();
				for(final Candidate member : members){
					for(final char chr : member.tokens.get(position).toCharArray())
						union.add(chr);
					coverage.or(member.coverage);
				}
				final StringBuilder sb = new StringBuilder();
				for(final Character chr : union)
					sb.append(chr);

				final List<String> tokens = new ArrayList<>(members.getFirst().tokens);
				tokens.set(position, sb.toString());
				final Candidate merged = new Candidate(tokens, coverage);
				if(!pool.containsKey(merged.key)){
					pool.put(merged.key, merged);
					changed = true;

					if(pool.size() >= MAX_POOL_SIZE)
						return;
				}
			}
		}while(changed);
	}

	/**
	 * Writes the condition in the shortest form: any group is positive or negated, whichever has the fewest characters
	 * (all with respect to the words that can reach that position), and a useless leading wildcard is removed.
	 */
	private static Line finalizeLine(final Candidate candidate, final Transformation transformation,
			final Set<String> positives, final List<String> words){
		final String removal = transformation.removal;
		final int removalLength = removal.length();

		final List<String> tokens = new ArrayList<>(candidate.tokens);
		final List<Set<Character>> negated = new ArrayList<>(tokens.size());
		for(int i = 0, length = tokens.size(); i < length; i ++)
			negated.add(null);
		//the tokens are rewritten one at a time, each one against the form (positive or negated) chosen for the others: two
		// negated tokens are not equivalent to the positive ones taken together
		for(int i = 0, length = tokens.size(); i < length; i ++){
			Set<Character> excluded = null;
			final String token = tokens.get(i);
			if(token.length() > 1){
				//the characters at this position of the words that match all the other tokens
				final Set<Character> observed = new TreeSet<>();
				for(final String word : words)
					if(matchesAllBut(word, tokens, negated, removalLength, i))
						observed.add(word.charAt(word.length() - removalLength - 1 - i));
				for(final char chr : token.toCharArray())
					observed.remove(chr);
				if(observed.size() < token.length())
					excluded = observed;
			}
			negated.set(i, excluded);
		}

		//drop the leading wildcard if it does not let any negative word in
		while(!tokens.isEmpty() && negated.get(tokens.size() - 1) != null && negated.get(tokens.size() - 1).isEmpty()){
			final List<String> shorter = tokens.subList(0, tokens.size() - 1);
			final List<Set<Character>> shorterNegated = negated.subList(0, negated.size() - 1);
			boolean valid = true;
			for(final String word : words)
				if(!positives.contains(word) && matches(word, shorter, shorterNegated, removalLength)){
					valid = false;
					break;
				}
			if(!valid)
				break;

			tokens.removeLast();
			negated.removeLast();
		}

		//the words selected, and the oracle check
		final Set<String> from = new LinkedHashSet<>();
		for(final String word : words)
			if(matches(word, tokens, negated, removalLength)){
				if(!positives.contains(word))
					throw new LinterException(INTERNAL_ERROR, tokens, word, transformation.removal, transformation.addition);

				from.add(word);
			}
		return new Line(transformation.removal, transformation.addition, tokens, negated, from);
	}

	private static boolean matches(final String word, final List<String> tokens, final List<Set<Character>> negated,
			final int removalLength){
		return matchesAllBut(word, tokens, negated, removalLength, -1);
	}

	/** The token at index `skip` just has to exist as a character. */
	private static boolean matchesAllBut(final String word, final List<String> tokens, final List<Set<Character>> negated,
			final int removalLength, final int skip){
		final int base = word.length() - removalLength - 1;
		for(int i = 0, length = tokens.size(); i < length; i ++){
			final int index = base - i;
			if(index < 0)
				return false;
			if(i != skip){
				final char chr = word.charAt(index);
				final Set<Character> excluded = negated.get(i);
				if(excluded != null? excluded.contains(chr): tokens.get(i).indexOf(chr) < 0)
					return false;
			}
		}
		return true;
	}


	//-- assembling --

	/**
	 * Lines with the same condition that select the same words are shown together, using the longest removal (the
	 * additions are extended accordingly, as the {@link RulesReducer} does).
	 */
	private List<LineEntry> toEntries(final List<Line> lines){
		final Map<String, List<Line>> groups = new LinkedHashMap<>();
		for(final Line line : lines){
			final String condition = line.condition(comparator);
			groups.computeIfAbsent(condition + TOKEN_SEPARATOR + String.join(TOKEN_SEPARATOR, new TreeSet<>(line.from)),
				k -> new ArrayList<>(1)).add(line);
		}

		final List<LineEntry> entries = new ArrayList<>(lines.size());
		for(final List<Line> group : groups.values()){
			final String condition = group.getFirst()
				.condition(comparator);
			final List<Line> remaining = new ArrayList<>(group);
			remaining.sort(Comparator.comparingInt((Line line) -> line.removal.length())
				.reversed());
			while(!remaining.isEmpty()){
				final String longest = remaining.getFirst().removal;
				final Set<String> additions = new LinkedHashSet<>();
				final Set<String> from = remaining.getFirst().from;
				for(int i = 0; i < remaining.size(); i ++){
					final Line line = remaining.get(i);
					if(longest.endsWith(line.removal)){
						additions.add(longest.substring(0, longest.length() - line.removal.length()) + line.addition);
						remaining.remove(i --);
					}
				}
				entries.add(new LineEntry(longest, additions, condition, from));
			}
		}
		return entries;
	}

	private static String reverse(final String text){
		return StringUtils.reverse(text);
	}


	//-- data holders --

	/** Minimal transformation: the removal and the addition (with continuation flags, if any) share no prefix. */
	private static final class Transformation implements Comparable<Transformation>{

		final String removal;
		final String addition;


		static Transformation of(final String removal, final String addition){
			//the continuation flags (and morphological fields) start after the surface of the addition
			int surfaceEnd = addition.length();
			for(int i = 0; i < addition.length(); i ++){
				final char chr = addition.charAt(i);
				if(chr == '/' || chr == '\t'){
					surfaceEnd = i;
					break;
				}
			}
			int lcp = 0;
			final int maxLcp = Math.min(removal.length(), surfaceEnd);
			while(lcp < maxLcp && removal.charAt(lcp) == addition.charAt(lcp))
				lcp ++;
			return new Transformation(removal.substring(lcp), addition.substring(lcp));
		}

		private Transformation(final String removal, final String addition){
			this.removal = removal;
			this.addition = addition;
		}

		@Override
		public int compareTo(final Transformation other){
			final int cmp = removal.compareTo(other.removal);
			return (cmp != 0? cmp: addition.compareTo(other.addition));
		}

		@Override
		public boolean equals(final Object obj){
			return (obj instanceof Transformation other && removal.equals(other.removal) && addition.equals(other.addition));
		}

		@Override
		public int hashCode(){
			return 31 * removal.hashCode() + addition.hashCode();
		}

		@Override
		public String toString(){
			return removal + " -> " + addition;
		}

	}

	/** Node of the reverse trie of the words (the path from the root is the suffix before the removal, reversed). */
	private static final class Node{

		final Map<Character, Node> children = new TreeMap<>();
		int positives;
		int negatives;
		/** Indices (contiguous) of the positive words below this node. */
		int firstPositive = -1;
		int lastPositive = -1;
		/** The positive word that ends exactly at this node (if any). */
		String exactPositive;


		void add(final int positiveIndex){
			if(positiveIndex < 0)
				negatives ++;
			else{
				positives ++;
				if(firstPositive < 0)
					firstPositive = positiveIndex;
				lastPositive = positiveIndex;
			}
		}

		BitSet coverage(){
			final BitSet coverage = new BitSet();
			if(positives > 0)
				coverage.set(firstPositive, lastPositive + 1);
			return coverage;
		}

	}

	/** A condition made of tokens (each one is the sorted set of admitted characters) and the positives it selects. */
	private static final class Candidate{

		final List<String> tokens;
		final BitSet coverage;
		final String key;
		final int size;


		Candidate(final List<String> tokens, final BitSet coverage){
			this.tokens = tokens;
			this.coverage = coverage;
			key = String.join(TOKEN_SEPARATOR, tokens);
			int length = 0;
			for(final String token : tokens)
				length += token.length();
			size = length;
		}

		String keyWithout(final int index){
			final StringBuilder sb = new StringBuilder();
			for(int i = 0; i < tokens.size(); i ++)
				if(i != index)
					sb.append(tokens.get(i))
						.append(TOKEN_SEPARATOR);
			return sb.toString();
		}

		/** Prefer the shortest conditions, then the one with the fewest characters, then a stable order. */
		boolean isBetterThan(final Candidate other){
			if(tokens.size() != other.tokens.size())
				return tokens.size() < other.tokens.size();
			if(size != other.size)
				return size < other.size;
			return key.compareTo(other.key) < 0;
		}

	}

	private static final class Line{

		final String removal;
		final String addition;
		final List<String> tokens;
		final List<Set<Character>> negated;
		final Set<String> from;


		Line(final String removal, final String addition, final List<String> tokens, final List<Set<Character>> negated,
				final Set<String> from){
			this.removal = removal;
			this.addition = addition;
			this.tokens = tokens;
			this.negated = negated;
			this.from = from;
		}

		String condition(final Comparator<String> comparator){
			final StringBuilder sb = new StringBuilder();
			for(int i = tokens.size() - 1; i >= 0; i --){
				final Set<Character> excluded = negated.get(i);
				if(excluded != null)
					sb.append(RegexHelper.makeNotGroup(excluded, comparator));
				else{
					final Set<Character> admitted = new LinkedHashSet<>();
					for(final char chr : tokens.get(i).toCharArray())
						admitted.add(chr);
					sb.append(RegexHelper.makeGroup(admitted, comparator));
				}
			}
			sb.append(removal);
			return sb.toString();
		}

	}

}
