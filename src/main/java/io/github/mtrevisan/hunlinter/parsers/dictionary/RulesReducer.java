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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


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
 * 	<li>valid conditions are generated from a reverse trie of the words (pure sub-trees and groups of pure siblings; the
 * 		trie skeleton depends only on the removal, so it is built once and shared by all the transformations with that
 * 		removal), then enlarged by merging conditions that differ by a single token, by widening each token to every
 * 		character that does not let a negative word in, and by dropping the farthest token when it is not needed;</li>
 * 	<li>the candidates dominated by another one are dropped, then a set cover picks the fewest conditions that cover every
 * 		positive word (exact branch-and-bound on small instances, greedy otherwise, always with a deterministic limit on
 * 		the work done), a local search replaces pairs of conditions with a single one, and redundant ones are dropped;</li>
 * 	<li>each condition is finally written in the shortest form (positive or negated group, useless leading wildcard
 * 		removed).</li>
 * </ol>
 * Every limit is a count of operations (never a time), so the output does not depend on the load of the machine.
 * Everything is computed on suffix-oriented data, as the {@link RulesReducer} does (prefixes are reversed by
 * {@link #collectInflectionsByFlag(List, String, AffixType)} and restored by
 * {@link #convertFormat(String, boolean, List)}).
 */
public class RulesReducer{

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesReducer.class);

	private static final String NON_SEPARABLE = "Cannot separate the word `{}` from the other words ending with `{}`: "
		+ "no condition can select it for the transformation `{}` -> `{}`";
	private static final String INTERNAL_ERROR = "Internal error: the condition `{}` selects the word `{}` that does not undergo `{}` -> `{}`";

	private static final String TOKEN_SEPARATOR = "\u0001";
	/** Safety limit on the number of candidate conditions generated for a single transformation. */
	private static final int MAX_POOL_SIZE = 20_000;
	/** Limit on the number of word checks spent enlarging the pool of candidates of a single transformation. */
	private static final long EXPAND_MAX_WORD_CHECKS = 20_000_000L;
	/** Above these thresholds the exact set cover is not attempted (greedy + local search only). */
	private static final int EXACT_MAX_POSITIVES = 60;
	private static final int EXACT_MAX_POOL = 2_000;
	/** Limit on the number of nodes visited by the exact branch-and-bound set cover (the best solution found is used). */
	private static final long EXACT_MAX_NODES = 500_000L;
	/** Limit on the number of word checks spent merging the selected conditions of a single transformation. */
	private static final long BOX_MAX_WORD_CHECKS = 50_000_000L;


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
				//the rules applied after this one (of the other type, as the apheresis prefix applied to a suffixed word) change the
				// word too: they have to be undone first, otherwise the word this rule was applied to is not the right one
				final AffixEntry[] appliedRules = inflection.getAppliedRules();
				int ruleIndex = appliedRules.length - 1;
				while(ruleIndex >= 0 && appliedRules[ruleIndex] != lastAppliedRule)
					ruleIndex --;
				String producedWord = inflection.getWord();
				for(int j = appliedRules.length - 1; j > ruleIndex; j --)
					producedWord = appliedRules[j].undoRule(producedWord);
				final String word = lastAppliedRule.undoRule(producedWord);
				final LineEntry newEntry = createAffixEntry(producedWord, lastAppliedRule, word, type);
				filteredRules.add(newEntry);
			}
		}
		return filteredRules;
	}

	/**
	 * @param producedWord	The word produced by the rule (without the effect of the rules applied after it).
	 * @param lastAppliedRule	The rule that produced the word.
	 * @param word	The word the rule was applied to.
	 */
	private LineEntry createAffixEntry(String producedWord, final AffixEntry lastAppliedRule, String word, final AffixType type){
		if(type == AffixType.PREFIX){
			producedWord = StringUtils.reverse(producedWord);
			word = StringUtils.reverse(word);
		}

		final int lastCommonLetter = StringHelper.getLastCommonLetterIndex(word, producedWord);

		final int wordLength = word.length();
		final String removal = (lastCommonLetter < wordLength? word.substring(lastCommonLetter): StringUtils.EMPTY);
		String addition = (lastCommonLetter < producedWord.length()? producedWord.substring(lastCommonLetter): StringUtils.EMPTY);
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
		prettyPrintRules.addFirst(LineEntry.toHunspellHeader(type, flag, ruleToBeReduced.combinableChar(),
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
		return addition.substring(0, surfaceEnd(addition));
	}

	/** @return	The index where the continuation flags and the morphological fields start (the length if there are none). */
	private static int surfaceEnd(final String addition){
		for(int i = 0; i < addition.length(); i ++){
			final char chr = addition.charAt(i);
			if(chr == '/' || chr == '\t')
				return i;
		}
		return addition.length();
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
		final Transformation transformation = Transformation.of(removal, addition);
		return transformation.removal + TOKEN_SEPARATOR + transformation.addition;
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

				final Map<DictionaryEntry, Integer> originalInflectionsWhole = new HashMap<>(originalInflections.size());
				for(int j = 0, length2 = originalInflections.size(); j < length2; j ++)
					originalInflectionsWhole.merge(new DictionaryEntry(originalInflections.get(j)), 1, Integer::sum);
				final Map<DictionaryEntry, Integer> inflectionsWhole = new HashMap<>(inflections.size());
				for(int j = 0, length2 = inflections.size(); j < length2; j ++)
					inflectionsWhole.merge(new DictionaryEntry(inflections.get(j)), 1, Integer::sum);
				//the same inflections, none of them produced more times than before (the original rules may produce the same one more than once)
				if(!originalInflectionsWhole.keySet().equals(inflectionsWhole.keySet()) || !hasNoMoreOccurrences(inflectionsWhole, originalInflectionsWhole))
					throw new LinterException(VERY_BAD_ERROR, line, originalInflectionsWhole, inflectionsWhole);

				if(progressCallback != null && progress.incrementAndGet() % progressStep == 0)
					progressCallback.accept(progressIndex.incrementAndGet());
			});
	}

	private static boolean hasNoMoreOccurrences(final Map<DictionaryEntry, Integer> counts,
			final Map<DictionaryEntry, Integer> limits){
		for(final Map.Entry<DictionaryEntry, Integer> count : counts.entrySet())
			if(count.getValue() > limits.getOrDefault(count.getKey(), 0))
				return false;
		return true;
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


	//-- facts collection --

	/**
	 * Thread-safe accumulator of the plain rules, that keeps one record for each transformation (removal and addition)
	 * instead of one for each word and inflection, and shares the words among the records. A flag applied to every inflection
	 * of every word (as the clitics of the verbs) generates tens of millions of plain rules: keeping each one of them in memory
	 * is not feasible.
	 */
	public static final class FactCollector{

		private final Map<String, Fact> facts = new ConcurrentHashMap<>();
		private final Map<String, String> words = new ConcurrentHashMap<>();


		/** @param plainRules	The plain rules of a dictionary line, as returned by {@link #collectInflectionsByFlag(List, String, AffixType)}. */
		public final void add(final Collection<LineEntry> plainRules){
			if(plainRules == null)
				return;

			for(final LineEntry entry : plainRules)
				for(final String addition : entry.addition){
					final Fact fact = facts.computeIfAbsent(entry.removal + TOKEN_SEPARATOR + addition,
						k -> new Fact(entry.removal, addition));
					for(final String word : entry.from)
						fact.words.add(words.computeIfAbsent(word, k -> k));
				}
		}

		public final boolean isEmpty(){
			return facts.isEmpty();
		}

		/** @return	The accumulated facts as plain rules (one for each transformation, with all the words that undergo it). */
		public final List<LineEntry> toRules(){
			final List<LineEntry> rules = new ArrayList<>(facts.size());
			for(final Fact fact : facts.values())
				rules.add(new LineEntry(fact.removal, fact.addition, fact.removal, fact.words));
			return rules;
		}

	}

	private static final class Fact{

		final String removal;
		final String addition;
		final Set<String> words = ConcurrentHashMap.newKeySet();


		Fact(final String removal, final String addition){
			this.removal = removal;
			this.addition = addition;
		}

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
		final AffixType lastFoldType = (affixData.isComplexPrefixes()? AffixType.SUFFIX: AffixType.PREFIX);
		for(int i = 0, length = inflections.size(); i < length; i ++){
			final Inflection inflection = inflections.get(i);
			//a word that already has a rule of the last fold applied (the prefix, for the languages without complex prefixes) is
			// never inflected again: the flags it carries are just inherited by the rule, they do not apply to it
			if(inflection.hasContinuationFlag(flag) && inflection.getLastAppliedRule(lastFoldType) == null){
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
		return reduceRules(plainRules, inertWords, null, progressCallback);
	}

	/**
	 * Same as {@link #reduceRules(Collection, Collection, ProgressCallback)}, but when the flag is given the reduced conditions
	 * never accept a character, at any position, that the original conditions of the same transformation do not accept (nor are
	 * shorter than them), so the set of words a reduced rule can be applied to is never larger than the original one.
	 *
	 * @param flag	The flag being reduced (<code>null</code> to leave the conditions unconstrained).
	 */
	public final List<LineEntry> reduceRules(final Collection<LineEntry> plainRules, final Collection<String> inertWords,
			final String flag, final ProgressCallback progressCallback){
		final Map<Transformation, Constraint> constraints = (flag != null? extractConstraints(flag): Collections.emptyMap());
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

		//2. the trie of the words ending with a given removal (in reversed-lexicographic order, that is the order of the trie):
		// it does not depend on the addition, so it is built once for all the transformations that share the removal
		final Map<String, Skeleton> skeletonByRemoval = new HashMap<>();
		for(final Transformation transformation : positivesByTransformation.keySet())
			skeletonByRemoval.computeIfAbsent(transformation.removal, removal -> {
				final List<String> words = new ArrayList<>();
				for(final String word : allWords)
					if(word.endsWith(removal))
						words.add(word);
				return Skeleton.of(words, removal.length());
			});
		if(progressCallback != null)
			progressCallback.accept(10);

		//3. solve each transformation independently, in parallel
		final List<Transformation> transformations = new ArrayList<>(positivesByTransformation.keySet());
		final int total = transformations.size();
		final AtomicInteger done = new AtomicInteger(0);
		final List<Line> lines = IntStream.range(0, total)
			.parallel()
			.mapToObj(i -> {
				final Transformation transformation = transformations.get(i);
				final List<Line> sub = solve(transformation, positivesByTransformation.get(transformation),
					skeletonByRemoval.get(transformation.removal), constraints.get(transformation));
				if(progressCallback != null){
					final int value = 10 + 85 * done.incrementAndGet() / total;
					synchronized(progressCallback){
						progressCallback.accept(value);
					}
				}
				return sub;
			})
			.flatMap(List::stream)
			.collect(Collectors.toList());

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
	 * @param skeleton	The trie of all the words ending with the removal of the transformation.
	 * @return	The minimal set of lines that apply the transformation to all the positives and to no other word.
	 */
	private static List<Line> solve(final Transformation transformation, final Set<String> positives,
			final Skeleton skeleton, final Constraint constraint){
		final String removal = transformation.removal;
		final List<String> words = skeleton.words;

		//positive words get their index in the order of the trie, so the positives of any sub-tree are contiguous
		final Context context = new Context(words, positives, removal.length(), constraint);
		final int positiveCount = context.positiveCount;

		//generate the candidate conditions
		Map<String, Candidate> pool = new LinkedHashMap<>();
		collectCandidates(skeleton.root, new ArrayList<>(), transformation, context, pool);
		pool = padCandidates(pool, context);
		//the candidates from the trie are a partition of the positives: it is the (always valid) starting solution
		final List<Candidate> partition = new ArrayList<>(pool.values());
		mergeCandidates(pool);
		//enlarge the pool with the conditions that the trie cannot produce
		expandCandidates(pool, context);
		//a candidate that is dominated (same coverage at same or higher cost) can never be optimal
		pruneDominated(pool);

		//choose the cover, in which every positive word is selected by exactly one condition (a word selected by two lines of
		// the same transformation would be inflected twice): exact branch-and-bound for small instances, starting from the
		// partition of the trie
		List<Candidate> selected = partition;
		if(positiveCount <= EXACT_MAX_POSITIVES && pool.size() <= EXACT_MAX_POOL)
			selected = improveWithExact(pool.values(), positiveCount, transformation, partition);

		//try to replace the selected candidates that are inside a single pool candidate with it
		localSearchMerge(selected, pool.values());
		//try to replace groups of selected candidates with their bounding box
		mergeByBoundingBox(selected, context);

		//remove redundant conditions
		removeRedundant(selected, positiveCount);

		final List<Line> lines = new ArrayList<>(selected.size());
		for(final Candidate candidate : selected)
			lines.add(finalizeLine(candidate, transformation, positives, words, constraint));
		return lines;
	}


	//-- set cover helpers --

	/** Greedy set cover with a rarity-based tie-break: prefer covering elements that few other candidates can cover. */
	private static List<Candidate> greedyCover(final Collection<Candidate> candidates, final int positiveCount){
		final List<Candidate> pool = new ArrayList<>(candidates);
		final int[] coverCount = countCoverage(pool, positiveCount);

		final List<Candidate> selected = new ArrayList<>();
		final BitSet uncovered = new BitSet(positiveCount);
		uncovered.set(0, positiveCount);
		final BitSet gainMask = new BitSet(positiveCount);
		while(!uncovered.isEmpty()){
			Candidate best = null;
			int bestGain = 0;
			long bestRarity = Long.MAX_VALUE;
			for(final Candidate candidate : pool){
				gainMask.clear();
				gainMask.or(candidate.coverage);
				gainMask.and(uncovered);
				final int gain = gainMask.cardinality();
				if(gain <= 0)
					continue;

				if(gain > bestGain){
					best = candidate;
					bestGain = gain;
					bestRarity = raritySum(gainMask, coverCount);
				}
				else if(gain == bestGain){
					final long rarity = raritySum(gainMask, coverCount);
					if(rarity < bestRarity || rarity == bestRarity && candidate.isBetterThan(best)){
						best = candidate;
						bestRarity = rarity;
					}
				}
			}
			if(best == null)
				throw new IllegalStateException("Cannot cover the words " + uncovered);

			selected.add(best);
			uncovered.andNot(best.coverage);
		}
		return selected;
	}

	/**
	 * Exact set cover via branch-and-bound on the uncovered set. Starts from the greedy solution and only accepts
	 * improvements. Returns the best solution found within the (deterministic) limit on the visited nodes, or the greedy one.
	 */
	private static List<Candidate> improveWithExact(final Collection<Candidate> candidates, final int positiveCount,
			final Transformation transformation, final List<Candidate> partition){
		final List<Candidate> greedy = partition;
		if(positiveCount <= 1 || candidates.size() <= greedy.size())
			return greedy;

		final ExactSearch search = new ExactSearch(candidates, positiveCount, greedy);
		final BitSet uncovered = new BitSet(positiveCount);
		uncovered.set(0, positiveCount);
		search.search(uncovered);
		if(search.limitReached)
			LOGGER.debug("Exact set cover stopped after {} nodes for the transformation `{}`, the best solution found ({} rules) is used",
				search.nodes, transformation, search.best.size());
		return search.best;
	}

	/** Branch-and-bound over the candidates, with the covering structure computed once. */
	private static final class ExactSearch{

		/** Candidates, sorted by decreasing coverage, then by key (this order is the tie-break of the search). */
		private final List<Candidate> pool;
		/** For each positive word, the indices (in {@link #pool}) of the candidates that cover it. */
		private final int[][] coveringByElement;
		private final int maxCardinality;
		private final List<Candidate> chosen = new ArrayList<>();
		List<Candidate> best;
		private int bestSize;
		long nodes;
		boolean limitReached;


		ExactSearch(final Collection<Candidate> candidates, final int positiveCount, final List<Candidate> greedy){
			pool = new ArrayList<>(candidates);
			//large coverage first: better first solution, more pruning
			pool.sort((a, b) -> {
				final int cmp = Integer.compare(b.coverage.cardinality(), a.coverage.cardinality());
				return (cmp != 0? cmp: a.key.compareTo(b.key));
			});
			maxCardinality = Math.max(1, pool.getFirst().coverage.cardinality());

			final int[] counts = new int[positiveCount];
			for(final Candidate candidate : pool)
				for(int i = candidate.coverage.nextSetBit(0); i >= 0; i = candidate.coverage.nextSetBit(i + 1))
					counts[i] ++;
			coveringByElement = new int[positiveCount][];
			for(int i = 0; i < positiveCount; i ++)
				coveringByElement[i] = new int[counts[i]];
			final int[] fill = new int[positiveCount];
			for(int index = 0, size = pool.size(); index < size; index ++){
				final BitSet coverage = pool.get(index).coverage;
				for(int i = coverage.nextSetBit(0); i >= 0; i = coverage.nextSetBit(i + 1))
					coveringByElement[i][fill[i] ++] = index;
			}

			best = new ArrayList<>(greedy);
			bestSize = greedy.size();
		}

		void search(final BitSet uncovered){
			if(uncovered.isEmpty()){
				if(chosen.size() < bestSize){
					bestSize = chosen.size();
					best = new ArrayList<>(chosen);
				}
				return;
			}
			//each candidate covers at most `maxCardinality` words
			final int remaining = uncovered.cardinality();
			if(chosen.size() + (remaining + maxCardinality - 1) / maxCardinality >= bestSize)
				return;
			if(++ nodes > EXACT_MAX_NODES){
				limitReached = true;
				return;
			}

			//pivot: uncovered element covered by the fewest candidates
			int pivot = -1;
			int pivotCount = Integer.MAX_VALUE;
			for(int i = uncovered.nextSetBit(0); i >= 0; i = uncovered.nextSetBit(i + 1)){
				final int count = coveringByElement[i].length;
				if(count < pivotCount){
					pivotCount = count;
					pivot = i;
					if(count == 1)
						break;
				}
			}
			if(pivot < 0 || pivotCount == 0)
				return;

			//candidates covering the pivot, ordered by decreasing gain (then by the order of the pool)
			final long[] uncoveredBits = uncovered.toLongArray();
			final int[] covering = coveringByElement[pivot];
			final long[] ordered = new long[covering.length];
			for(int j = 0; j < covering.length; j ++){
				final int gain = andCount(pool.get(covering[j]).bits(), uncoveredBits);
				ordered[j] = ((long)(Integer.MAX_VALUE - gain) << 32) | covering[j];
			}
			Arrays.sort(ordered);

			final BitSet newUncovered = new BitSet(uncovered.size());
			for(final long key : ordered){
				final Candidate candidate = pool.get((int)(key & 0xFFFF_FFFFL));
				//the words must be selected exactly once: a candidate that selects an already covered word cannot be chosen
				newUncovered.clear();
				newUncovered.or(candidate.coverage);
				newUncovered.andNot(uncovered);
				if(!newUncovered.isEmpty())
					continue;

				newUncovered.or(uncovered);
				newUncovered.andNot(candidate.coverage);
				chosen.add(candidate);
				search(newUncovered);
				chosen.removeLast();
				if(limitReached)
					return;
			}
		}

	}

	/** @return	for each element, the number of candidates that cover it. */
	private static int[] countCoverage(final Collection<Candidate> pool, final int positiveCount){
		final int[] counts = new int[positiveCount];
		for(final Candidate candidate : pool)
			for(int i = candidate.coverage.nextSetBit(0); i >= 0; i = candidate.coverage.nextSetBit(i + 1))
				counts[i] ++;
		return counts;
	}

	/** @return	sum of the per-element cover counts over the given mask: lower = covers "harder" elements. */
	private static long raritySum(final BitSet mask, final int[] coverCount){
		long sum = 0;
		for(int i = mask.nextSetBit(0); i >= 0; i = mask.nextSetBit(i + 1))
			sum += coverCount[i];
		return sum;
	}

	/**
	 * Removes candidates dominated by another one (coverage subset, cost not smaller). A dominated candidate can
	 * never be part of an optimal solution, so removing it is safe.
	 * <p>
	 * A candidate that dominates another one covers (among the others) the first element of the dominated one, so only the
	 * kept candidates that cover that element are compared.
	 */
	private static void pruneDominated(final Map<String, Candidate> pool){
		if(pool.size() < 2)
			return;

		final List<Candidate> sorted = new ArrayList<>(pool.values());
		//largest coverage first, then cheapest
		sorted.sort((a, b) -> {
			final int cmp = Integer.compare(b.coverage.cardinality(), a.coverage.cardinality());
			if(cmp != 0)
				return cmp;
			final int cost = Integer.compare(a.size, b.size);
			if(cost != 0)
				return cost;
			return a.key.compareTo(b.key);
		});

		final List<Candidate> kept = new ArrayList<>(sorted.size());
		final Map<Integer, List<Candidate>> keptByElement = new HashMap<>();
		for(final Candidate candidate : sorted){
			final int first = candidate.coverage.nextSetBit(0);
			boolean dominated = false;
			final List<Candidate> competitors = keptByElement.get(first);
			if(competitors != null)
				for(final Candidate better : competitors)
					if(better.size <= candidate.size && better.tokens.size() <= candidate.tokens.size()
							&& isSubset(candidate.bits(), better.bits()) && isSubset(better.bits(), candidate.bits())){
						dominated = true;
						break;
					}
			if(!dominated){
				kept.add(candidate);
				final BitSet coverage = candidate.coverage;
				for(int i = coverage.nextSetBit(0); i >= 0; i = coverage.nextSetBit(i + 1))
					keptByElement.computeIfAbsent(i, k -> new ArrayList<>()).add(candidate);
			}
		}

		if(kept.size() < pool.size()){
			pool.clear();
			for(final Candidate candidate : kept)
				pool.put(candidate.key, candidate);
		}
	}

	/** @return	Whether every bit set in {@code subset} is set in {@code superset}. */
	private static boolean isSubset(final long[] subset, final long[] superset){
		for(int i = 0; i < subset.length; i ++){
			final long other = (i < superset.length? superset[i]: 0L);
			if((subset[i] & ~other) != 0L)
				return false;
		}
		return true;
	}

	/** @return	The number of bits set in both the arrays. */
	private static int andCount(final long[] a, final long[] b){
		int count = 0;
		for(int i = 0, length = Math.min(a.length, b.length); i < length; i ++)
			count += Long.bitCount(a[i] & b[i]);
		return count;
	}

	/**
	 * Replaces the selected conditions that are inside a single pool candidate with it (every selected condition that shares a
	 * word with the pool candidate must be inside it, so the words are still selected exactly once), as long as it strictly
	 * reduces the number of selected rules.
	 */
	private static void localSearchMerge(final List<Candidate> selected, final Collection<Candidate> pool){
		if(selected.size() < 2)
			return;

		final List<Candidate> poolList = new ArrayList<>(pool);
		boolean improved = true;
		while(improved){
			improved = false;

			for(final Candidate candidate : poolList){
				final long[] bits = candidate.bits();
				final List<Candidate> inside = new ArrayList<>();
				boolean valid = true;
				for(final Candidate chosen : selected){
					if(andCount(chosen.bits(), bits) == 0)
						continue;
					if(!isSubset(chosen.bits(), bits)){
						valid = false;
						break;
					}
					inside.add(chosen);
				}
				if(valid && inside.size() > 1){
					selected.removeAll(inside);
					selected.add(candidate);
					improved = true;
				}
			}
		}
	}

	/**
	 * Replaces groups of selected conditions with a single one, whose token at each position is the union of the tokens of the
	 * group (the "bounding box" of the group), as long as the box selects no negative word and no word selected by a condition
	 * outside the group (if it does select the words of another condition, that condition joins the group and the box is
	 * recomputed). This finds, for instance, {@code [ab][cd]x} in place of {@code [a]cx}, {@code [b]dx} and {@code [ab]cx}, that the
	 * trie and the single-token widening cannot reach.
	 * <p>
	 * The number of word checks is limited, so the result is deterministic.
	 */
	private static void mergeByBoundingBox(final List<Candidate> selected, final Context context){
		final int cost = context.negativeIndex.length + context.positiveIndex.length;
		long budget = BOX_MAX_WORD_CHECKS;
		boolean improved = true;
		while(improved && budget > 0){
			improved = false;

			outer:
			for(int i = 0; i < selected.size(); i ++)
				for(int j = i + 1; j < selected.size(); j ++){
					if(budget <= 0)
						break outer;

					List<Candidate> group = new ArrayList<>(2);
					group.add(selected.get(i));
					group.add(selected.get(j));
					Candidate merged = null;
					while(merged == null && budget > 0){
						final List<String> tokens = boundingBox(group, context.constraint.requiredLength());
						if(tokens == null)
							break;

						budget -= cost;
						final BitSet coverage = boxCoverage(tokens, context);
						if(coverage == null)
							break;

						final List<Candidate> touched = new ArrayList<>(group.size());
						for(final Candidate chosen : selected)
							if(chosen.coverage.intersects(coverage))
								touched.add(chosen);
						if(touched.size() == group.size())
							merged = new Candidate(tokens, coverage);
						else
							group = touched;
					}
					if(merged != null){
						selected.removeAll(group);
						selected.add(merged);
						improved = true;
						break outer;
					}
				}
		}
	}

	/** @return	The tokens of the bounding box of the group, or null if it would be shorter than the original conditions. */
	private static List<String> boundingBox(final List<Candidate> group, final int requiredLength){
		int length = Integer.MAX_VALUE;
		for(final Candidate candidate : group)
			length = Math.min(length, candidate.tokens.size());
		if(length < requiredLength)
			return null;

		final List<String> tokens = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final Set<Character> chars = new TreeSet<>();
			for(final Candidate candidate : group)
				for(final char chr : candidate.tokens.get(i).toCharArray())
					chars.add(chr);
			final StringBuilder sb = new StringBuilder(chars.size());
			for(final Character chr : chars)
				sb.append(chr);
			tokens.add(sb.toString());
		}
		return tokens;
	}

	/** @return	The positives selected by the condition, or null if it selects a negative word. */
	private static BitSet boxCoverage(final List<String> tokens, final Context context){
		for(final int index : context.negativeIndex)
			if(matchesTokens(context.words.get(index), tokens, context.removalLength))
				return null;

		final BitSet coverage = new BitSet();
		for(final int index : context.positiveIndex)
			if(matchesTokens(context.words.get(index), tokens, context.removalLength))
				coverage.set(context.prefix[index]);
		return coverage;
	}

	private static boolean matchesTokens(final String word, final List<String> tokens, final int removalLength){
		final int base = word.length() - removalLength - 1;
		for(int i = 0, length = tokens.size(); i < length; i ++){
			final int index = base - i;
			if(index < 0 || tokens.get(i).indexOf(word.charAt(index)) < 0)
				return false;
		}
		return true;
	}

	/** Removes selected candidates whose coverage is fully covered by the others. */
	private static void removeRedundant(final List<Candidate> selected, final int positiveCount){
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
	}


	//-- candidate generation --

	/**
	 * Visit the trie: a sub-tree with no negative words is a valid condition by itself; otherwise the children without
	 * negative words are grouped into a single token, and the children with both kinds of words are explored further.
	 */
	private static void collectCandidates(final Node node, final List<Character> path, final Transformation transformation,
			final Context context, final Map<String, Candidate> pool){
		final int positives = context.positives(node);
		if(positives == 0)
			return;

		if(positives == node.size()){
			addCandidate(pool, tokensOf(path, null), context.coverage(node));
			return;
		}

		if(node.exact >= 0 && context.positive[node.exact])
			//any condition that matches the word matches every other word ending with it too
			throw new LinterException(NON_SEPARABLE, context.words.get(node.exact), transformation.removal,
				transformation.removal, transformation.addition);

		final Set<Character> pure = new TreeSet<>();
		final BitSet pureCoverage = new BitSet();
		for(final Map.Entry<Character, Node> child : node.children.entrySet()){
			final Node n = child.getValue();
			final int childPositives = context.positives(n);
			if(childPositives > 0 && childPositives == n.size()){
				pure.add(child.getKey());
				pureCoverage.or(context.coverage(n));
			}
		}
		if(!pure.isEmpty())
			addCandidate(pool, tokensOf(path, pure), pureCoverage);

		for(final Map.Entry<Character, Node> child : node.children.entrySet()){
			final Node n = child.getValue();
			final int childPositives = context.positives(n);
			if(childPositives > 0 && childPositives < n.size()){
				path.add(child.getKey());
				collectCandidates(n, path, transformation, context, pool);
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
	 * <p>
	 * Possible future improvement (not needed so far): the closure is computed eagerly, to a fixed point, and stops silently at
	 * {@link #MAX_POOL_SIZE}, so on a very large pool the merges that are missed depend on the iteration order. A complete closure
	 * would generate the merged candidates lazily, on demand, from a priority queue (e.g. ordered by coverage gained per token),
	 * instead of materializing them all. It must be designed together with {@link #pruneDominated(Map)}: a dominated candidate
	 * has to be discarded as soon as it is generated, otherwise the lazy generation just moves the explosion of the pool
	 * elsewhere. Do it only with a real flag in which the pool limit gives a worse result than the exact one.
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
	 * Enlarges the pool with valid conditions the trie cannot produce, until no new condition is found or the work limit is
	 * reached (the limit counts word checks, so the result is deterministic):
	 * <ul>
	 * 	<li><em>widening</em>: a token is extended to every character that does not let a negative word in (the characters
	 * 		are independent of each other, so they can all be added at once);</li>
	 * 	<li><em>shortening</em>: the farthest token is dropped, if no negative word matches the shorter condition.</li>
	 * </ul>
	 * Both produce conditions that select a superset of the words selected by the original one, still without negative
	 * words.
	 */
	private static void expandCandidates(final Map<String, Candidate> pool, final Context context){
		final int wordCount = context.words.size();
		long budget = EXPAND_MAX_WORD_CHECKS;
		final Deque<Candidate> queue = new ArrayDeque<>(pool.values());
		while(!queue.isEmpty() && pool.size() < MAX_POOL_SIZE && budget > 0){
			final Candidate candidate = queue.poll();

			for(int i = 0, length = candidate.tokens.size(); i < length && budget > 0; i ++){
				budget -= wordCount;
				final Candidate widened = widen(candidate, i, context);
				if(widened != null && !pool.containsKey(widened.key)){
					pool.put(widened.key, widened);
					queue.add(widened);
				}
			}

			if(!candidate.tokens.isEmpty() && budget > 0){
				budget -= wordCount;
				final Candidate shortened = shorten(candidate, context);
				if(shortened != null && !pool.containsKey(shortened.key)){
					pool.put(shortened.key, shortened);
					queue.add(shortened);
				}
			}
		}
	}

	/** @return	The candidate with the token at `position` extended to all the admissible characters, or null if there is none. */
	private static Candidate widen(final Candidate candidate, final int position, final Context context){
		final List<String> tokens = candidate.tokens;
		final int length = tokens.size();
		final String token = tokens.get(position);

		//characters that can follow (at this position) a word that matches all the other tokens, with the positives they select
		final Map<Character, BitSet> admissible = new TreeMap<>();
		final Set<Character> blocked = new HashSet<>();
		for(int w = 0, count = context.words.size(); w < count; w ++){
			final String word = context.words.get(w);
			final int base = word.length() - context.removalLength - 1;
			if(base - (length - 1) < 0)
				continue;

			boolean matches = true;
			for(int i = 0; matches && i < length; i ++)
				if(i != position && tokens.get(i).indexOf(word.charAt(base - i)) < 0)
					matches = false;
			if(!matches)
				continue;

			final char chr = word.charAt(base - position);
			if(token.indexOf(chr) >= 0)
				continue;
			//the original conditions do not accept this character here, so the widened one cannot either
			if(!context.constraint.allows(position, chr))
				continue;

			if(context.positive[w])
				admissible.computeIfAbsent(chr, k -> new BitSet())
					.set(context.prefix[w]);
			else
				blocked.add(chr);
		}
		blocked.forEach(admissible::remove);
		if(admissible.isEmpty())
			return null;

		final Set<Character> chars = new TreeSet<>();
		for(final char chr : token.toCharArray())
			chars.add(chr);
		chars.addAll(admissible.keySet());
		final StringBuilder sb = new StringBuilder(chars.size());
		for(final Character chr : chars)
			sb.append(chr);

		final BitSet coverage = (BitSet)candidate.coverage.clone();
		for(final BitSet added : admissible.values())
			coverage.or(added);
		final List<String> newTokens = new ArrayList<>(tokens);
		newTokens.set(position, sb.toString());
		return new Candidate(newTokens, coverage);
	}

	/** @return	The candidate without the farthest token, or null if it would select a negative word. */
	private static Candidate shorten(final Candidate candidate, final Context context){
		//the original conditions are at least this long: a shorter one would accept words they do not
		if(candidate.tokens.size() - 1 < context.constraint.requiredLength())
			return null;

		final List<String> tokens = candidate.tokens.subList(0, candidate.tokens.size() - 1);
		final int length = tokens.size();

		final BitSet coverage = new BitSet();
		for(int w = 0, count = context.words.size(); w < count; w ++){
			final String word = context.words.get(w);
			final int base = word.length() - context.removalLength - 1;
			if(base - (length - 1) < 0)
				continue;

			boolean matches = true;
			for(int i = 0; matches && i < length; i ++)
				if(tokens.get(i).indexOf(word.charAt(base - i)) < 0)
					matches = false;
			if(!matches)
				continue;

			if(!context.positive[w])
				return null;
			coverage.set(context.prefix[w]);
		}
		return new Candidate(new ArrayList<>(tokens), coverage);
	}


	//-- finalization --

	/**
	 * Writes the condition in the shortest form: any group is positive or negated, whichever has the fewest characters
	 * (all with respect to the words that can reach that position), and a useless leading wildcard is removed.
	 */
	private static Line finalizeLine(final Candidate candidate, final Transformation transformation,
		final Set<String> positives, final List<String> words, final Constraint constraint){
		final Constraint limit = (constraint != null? constraint: Constraint.NONE);
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
				//the characters of the token that actually occur at this position: the positive group needs no more than these
				// (a token widened to every admissible character would otherwise lose against a negated group of the same size)
				final Set<Character> present = new TreeSet<>(observed);
				for(final char chr : token.toCharArray())
					observed.remove(chr);
				present.removeAll(observed);
				//a negated group accepts every other character: it is used only if the original conditions accept them all
				if(observed.size() < present.size() && limit.acceptsAllBut(i, observed))
					excluded = observed;
				else if(!present.isEmpty() && present.size() < token.length()){
					final StringBuilder sb = new StringBuilder(present.size());
					for(final Character chr : present)
						sb.append(chr);
					tokens.set(i, sb.toString());
				}
			}
			negated.set(i, excluded);
		}

		//drop the leading wildcard if it does not let any negative word in
		while(tokens.size() > limit.requiredLength() && negated.get(tokens.size() - 1) != null
				&& negated.get(tokens.size() - 1).isEmpty()){
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


	//-- constraints from the original conditions --

	/**
	 * For each (minimal) transformation, which characters the original conditions accept at each position (from the nearest to
	 * the removal), so the reduced conditions can be kept within them.
	 */
	private Map<Transformation, Constraint> extractConstraints(final String flag){
		final RuleEntry rule = affixData.getData(flag);
		final List<AffixEntry> originalEntries = (rule != null? rule.getEntries(): null);
		if(originalEntries == null || originalEntries.isEmpty())
			return Collections.emptyMap();

		final boolean prefix = (rule.getType() == AffixType.PREFIX);
		final Map<Transformation, List<List<Allowed>>> conditionsByTransformation = new HashMap<>();
		for(final AffixEntry entry : originalEntries){
			//the same transformation of the facts collected by `collectInflectionsByFlag`
			String removal = entry.getRemoving();
			String appending = entry.getAppending();
			if(prefix){
				removal = StringUtils.reverse(removal);
				appending = StringUtils.reverse(appending);
			}
			final Transformation transformation = Transformation.of(removal, appending + entry.toString(strategy));

			final List<Allowed> tokens = parseCondition(entry.getCondition());
			if(prefix)
				Collections.reverse(tokens);
			//drop the part of the condition that is the removal
			final int available = tokens.size() - transformation.removal.length();
			final List<Allowed> positions = new ArrayList<>(Math.max(0, available));
			for(int i = available - 1; i >= 0; i --)
				positions.add(tokens.get(i));
			conditionsByTransformation.computeIfAbsent(transformation, k -> new ArrayList<>(1))
				.add(positions);
		}

		final Map<Transformation, Constraint> constraints = new HashMap<>(conditionsByTransformation.size());
		for(final Map.Entry<Transformation, List<List<Allowed>>> conditions : conditionsByTransformation.entrySet()){
			int length = 0;
			for(final List<Allowed> positions : conditions.getValue())
				length = Math.max(length, positions.size());
			final List<Allowed> union = new ArrayList<>(length);
			for(int i = 0; i < length; i ++){
				Allowed allowed = null;
				for(final List<Allowed> positions : conditions.getValue()){
					final Allowed current = (i < positions.size()? positions.get(i): Allowed.ANY);
					allowed = (allowed == null? current: allowed.union(current));
				}
				union.add(allowed);
			}
			constraints.put(conditions.getKey(), new Constraint(union));
		}
		return constraints;
	}

	/** @return	The characters accepted at each position of an affix condition (in the order of the condition). */
	private static List<Allowed> parseCondition(final String condition){
		final List<Allowed> tokens = new ArrayList<>(condition.length());
		int i = 0;
		while(i < condition.length()){
			final char chr = condition.charAt(i);
			if(chr == '['){
				final int end = condition.indexOf(']', i + 1);
				if(end >= 0){
					final boolean negated = (i + 1 < end && condition.charAt(i + 1) == '^');
					final Set<Character> chars = new TreeSet<>();
					for(int j = i + 1 + (negated? 1: 0); j < end; j ++)
						chars.add(condition.charAt(j));
					tokens.add(new Allowed(negated, chars));
					i = end + 1;
					continue;
				}
			}

			tokens.add(chr == '.'? Allowed.ANY: new Allowed(false, Set.of(chr)));
			i ++;
		}
		return tokens;
	}

	/**
	 * Keeps the candidates inside the original conditions: a candidate shorter than the original conditions would accept any
	 * character in the missing positions, so it is extended with a token that accepts the characters allowed there (that occur in
	 * the words, plus the ones of the positives it covers, so the coverage is never lost). A candidate is never longer than the
	 * shortest positive word it covers.
	 */
	private static Map<String, Candidate> padCandidates(final Map<String, Candidate> pool, final Context context){
		final int required = context.constraint.requiredLength();
		if(required == 0)
			return pool;

		final String[] padTokens = new String[required];
		final Map<String, Candidate> padded = new LinkedHashMap<>(pool.size());
		for(final Candidate candidate : pool.values()){
			int limit = required;
			for(int i = candidate.coverage.nextSetBit(0); i >= 0; i = candidate.coverage.nextSetBit(i + 1))
				limit = Math.min(limit, context.positiveWords.get(i).length() - context.removalLength);

			final List<String> tokens = new ArrayList<>(candidate.tokens);
			while(tokens.size() < limit){
				final int position = tokens.size();
				if(padTokens[position] == null)
					padTokens[position] = padToken(position, context);
				final Set<Character> chars = new TreeSet<>();
				for(final char chr : padTokens[position].toCharArray())
					chars.add(chr);
				for(int i = candidate.coverage.nextSetBit(0); i >= 0; i = candidate.coverage.nextSetBit(i + 1)){
					final String word = context.positiveWords.get(i);
					chars.add(word.charAt(word.length() - context.removalLength - 1 - position));
				}
				final StringBuilder sb = new StringBuilder(chars.size());
				for(final Character chr : chars)
					sb.append(chr);
				tokens.add(sb.toString());
			}
			addCandidate(padded, tokens, candidate.coverage);
		}
		return padded;
	}

	/** @return	The characters of the words at the given position that the original conditions accept there. */
	private static String padToken(final int position, final Context context){
		final Set<Character> chars = new TreeSet<>();
		for(final String word : context.words){
			final int index = word.length() - context.removalLength - 1 - position;
			if(index >= 0 && context.constraint.allows(position, word.charAt(index)))
				chars.add(word.charAt(index));
		}
		final StringBuilder sb = new StringBuilder(chars.size());
		for(final Character chr : chars)
			sb.append(chr);
		return sb.toString();
	}


	//-- data holders --

	/** The characters a position of a condition accepts: the listed ones, or all the others if negated. */
	private static final class Allowed{

		static final Allowed ANY = new Allowed(true, Set.of());

		final boolean negated;
		final Set<Character> chars;


		Allowed(final boolean negated, final Set<Character> chars){
			this.negated = negated;
			this.chars = chars;
		}

		boolean isAny(){
			return (negated && chars.isEmpty());
		}

		boolean contains(final char chr){
			return (chars.contains(chr) != negated);
		}

		/** @return	What is accepted by this or by the other. */
		Allowed union(final Allowed other){
			final Set<Character> result = new TreeSet<>();
			if(!negated && !other.negated){
				result.addAll(chars);
				result.addAll(other.chars);
				return new Allowed(false, result);
			}
			if(negated && other.negated){
				result.addAll(chars);
				result.retainAll(other.chars);
				return new Allowed(true, result);
			}
			final Allowed positive = (negated? other: this);
			final Allowed negative = (negated? this: other);
			result.addAll(negative.chars);
			result.removeAll(positive.chars);
			return new Allowed(true, result);
		}

	}

	/** For each position (from the nearest to the removal) the characters accepted by the original conditions of a transformation. */
	private static final class Constraint{

		/** No limit at all. */
		static final Constraint NONE = new Constraint(new ArrayList<>(0));

		private final List<Allowed> positions;


		Constraint(final List<Allowed> positions){
			//a trailing position that accepts anything is the same as no position
			int size = positions.size();
			while(size > 0 && positions.get(size - 1).isAny())
				size --;
			this.positions = new ArrayList<>(positions.subList(0, size));
		}

		/** @return	The number of positions that limit the accepted characters. */
		int requiredLength(){
			return positions.size();
		}

		boolean allows(final int position, final char chr){
			return (position >= positions.size() || positions.get(position).contains(chr));
		}

		/** @return	Whether every character but the excluded ones is accepted at the given position. */
		boolean acceptsAllBut(final int position, final Set<Character> excluded){
			if(position >= positions.size())
				return true;

			final Allowed allowed = positions.get(position);
			return (allowed.negated && excluded.containsAll(allowed.chars));
		}

	}

	/** Minimal transformation: the removal and the addition (with continuation flags, if any) share no prefix. */
	private static final class Transformation implements Comparable<Transformation>{

		final String removal;
		final String addition;


		static Transformation of(final String removal, final String addition){
			//the continuation flags (and morphological fields) start after the surface of the addition
			int lcp = 0;
			final int maxLcp = Math.min(removal.length(), surfaceEnd(addition));
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

	/**
	 * Reverse trie of all the words that end with a given removal (the path from the root is the suffix before the removal,
	 * reversed). It does not depend on which words undergo a transformation, so it is shared by all the transformations with
	 * the same removal.
	 */
	private static final class Skeleton{

		final List<String> words;
		final Node root;


		static Skeleton of(final List<String> words, final int removalLength){
			final Node root = new Node();
			for(int index = 0, size = words.size(); index < size; index ++){
				final String word = words.get(index);
				Node node = root;
				node.visit(index);
				for(int i = word.length() - removalLength - 1; i >= 0; i --){
					node = node.children.computeIfAbsent(word.charAt(i), k -> new Node());
					node.visit(index);
				}
				node.exact = index;
			}
			return new Skeleton(words, root);
		}

		private Skeleton(final List<String> words, final Node root){
			this.words = words;
			this.root = root;
		}

	}

	/**
	 * Node of the reverse trie. The words are in the order of the trie, so the words below a node are the contiguous range
	 * {@code [first, last)}.
	 */
	private static final class Node{

		final Map<Character, Node> children = new TreeMap<>();
		int first = -1;
		int last;
		/** Index of the word that ends exactly at this node (if any). */
		int exact = -1;


		void visit(final int wordIndex){
			if(first < 0)
				first = wordIndex;
			last = wordIndex + 1;
		}

		int size(){
			return last - first;
		}

	}

	/** The words (in the order of the trie) of a transformation, and which ones undergo it. */
	private static final class Context{

		final List<String> words;
		final int removalLength;
		final boolean[] positive;
		/** {@code prefix[i]} is the number of positive words before the i-th one, that is the index of that word among the positives. */
		final int[] prefix;
		final int positiveCount;
		/** The positive words, indexed as the positives. */
		final List<String> positiveWords;
		/** The indexes (in {@link #words}) of the negative and of the positive words. */
		final int[] negativeIndex;
		final int[] positiveIndex;
		/** Which characters the original conditions accept at each position (never {@code null}). */
		final Constraint constraint;


		Context(final List<String> words, final Set<String> positives, final int removalLength, final Constraint constraint){
			this.words = words;
			this.removalLength = removalLength;
			this.constraint = (constraint != null? constraint: Constraint.NONE);
			final int size = words.size();
			positive = new boolean[size];
			prefix = new int[size + 1];
			positiveWords = new ArrayList<>(positives.size());
			for(int i = 0; i < size; i ++){
				positive[i] = positives.contains(words.get(i));
				prefix[i + 1] = prefix[i] + (positive[i]? 1: 0);
				if(positive[i])
					positiveWords.add(words.get(i));
			}
			positiveCount = prefix[size];
			final int[] negatives = new int[size - positiveCount];
			final int[] positiveIndexes = new int[positiveCount];
			int negativeFill = 0;
			int positiveFill = 0;
			for(int i = 0; i < size; i ++)
				if(positive[i])
					positiveIndexes[positiveFill ++] = i;
				else
					negatives[negativeFill ++] = i;
			negativeIndex = negatives;
			positiveIndex = positiveIndexes;
		}

		int positives(final Node node){
			return (node.first < 0? 0: prefix[node.last] - prefix[node.first]);
		}

		BitSet coverage(final Node node){
			final BitSet coverage = new BitSet();
			if(node.first >= 0 && positives(node) > 0)
				coverage.set(prefix[node.first], prefix[node.last]);
			return coverage;
		}

	}

	/** A condition made of tokens (each one is the sorted set of admitted characters) and the positives it selects. */
	private static final class Candidate{

		final List<String> tokens;
		final BitSet coverage;
		final String key;
		final int size;
		private long[] bits;


		Candidate(final List<String> tokens, final BitSet coverage){
			this.tokens = tokens;
			this.coverage = coverage;
			key = String.join(TOKEN_SEPARATOR, tokens);
			int length = 0;
			for(final String token : tokens)
				length += token.length();
			size = length;
		}

		/** @return	The coverage as an array of words (to be called once the coverage is not modified anymore). */
		long[] bits(){
			if(bits == null)
				bits = coverage.toLongArray();
			return bits;
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
				if(excluded != null){
					//empty excluded means "any character at this position": use the Hunspell wildcard
					if(excluded.isEmpty())
						sb.append('.');
					else
						sb.append(RegexHelper.makeNotGroup(excluded, comparator));
				}
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
