package unit731.hunspeller.parsers.dictionary;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.collections.radixtree.sequencers.RegExpSequencer;
import unit731.hunspeller.languages.BaseBuilder;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.services.PatternHelper;
import unit731.hunspeller.services.SetHelper;


public class RulesReducer{

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesReducer.class);

	private static final String PATTERN_END_OF_WORD = "$";
	private static final String NOT_GROUP_START = "[^";
	private static final String GROUP_START = "[";
	private static final String GROUP_END = "]";
	private static final String TAB = "\t";
	private static final String ZERO = "0";
	private static final String DOT = ".";

	private static final RegExpSequencer SEQUENCER = new RegExpSequencer();


	private final Comparator<LineEntry> shortestConditionComparator = Comparator.comparingInt(entry -> entry.condition.length());

	private final AffixData affixData;
	private final FlagParsingStrategy strategy;
	private final WordGenerator wordGenerator;
	private final Comparator<String> comparator;
	private final Comparator<LineEntry> lineEntryComparator;


	public static class LineEntry implements Serializable{

		private static final long serialVersionUID = 8374397415767767436L;

		private final Set<String> from;

		private final String removal;
		private final Set<String> addition;
		private String condition;


		public static LineEntry createFrom(final LineEntry entry, final String condition, final Collection<String> words){
			return new LineEntry(entry.removal, entry.addition, condition, words);
		}

		LineEntry(final String removal, final String addition, final String condition, final String word){
			this(removal, new HashSet<>(Arrays.asList(addition)), condition, word);
		}

		LineEntry(final String removal, final String addition, final String condition, final Collection<String> words){
			this(removal, new HashSet<>(Arrays.asList(addition)), condition, words);
		}

		LineEntry(final String removal, final Set<String> addition, final String condition, final String word){
			this(removal, addition, condition, Arrays.asList(word));
		}

		LineEntry(final String removal, final Set<String> addition, final String condition, final Collection<String> words){
			this.removal = removal;
			this.addition = addition;
			this.condition = condition;

			from = new HashSet<>();
			if(words != null)
				from.addAll(words);
		}

		public List<LineEntry> split(final AffixEntry.Type type){
			final List<LineEntry> split = new ArrayList<>();
			if(type == AffixEntry.Type.SUFFIX)
				for(final String f : from){
					final int index = f.length() - condition.length() - 1;
					if(index < 0)
						throw new IllegalArgumentException("Cannot reduce rule, should be splitted further because of '" + f + "'");

					split.add(new LineEntry(removal, addition, f.substring(index), f));
				}
			else
				for(final String f : from){
					final int index = condition.length() + 1;
					if(index == f.length())
						throw new IllegalArgumentException("Cannot reduce rule, should be splitted further because of '" + f + "'");

					split.add(new LineEntry(removal, addition, f.substring(0, index), f));
				}
			return split;
		}

		public List<String> extractFromEndingWith(String suffix){
			final Pattern conditionPattern = PatternHelper.pattern(suffix + PATTERN_END_OF_WORD);
			return from.stream()
				.filter(from -> PatternHelper.find(from, conditionPattern))
				.collect(Collectors.toList());
		}

		public String anAddition(){
			return addition.iterator().next();
		}

		@Override
		public String toString(){
			return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE)
				.append("from", from)
				.append("rem", removal)
				.append("add", addition)
				.append("cond", condition)
				.toString();
		}

		@Override
		public int hashCode(){
			return new HashCodeBuilder()
				.append(removal)
				.append(addition)
				.append(condition)
				.toHashCode();
		}

		@Override
		public boolean equals(Object obj){
			if(this == obj)
				return true;
			if(obj == null || getClass() != obj.getClass())
				return false;

			final LineEntry other = (LineEntry)obj;
			return new EqualsBuilder()
				.append(removal, other.removal)
				.append(addition, other.addition)
				.append(condition, other.condition)
				.isEquals();
		}
	}


	public RulesReducer(final AffixData affixData, final WordGenerator wordGenerator){
		Objects.requireNonNull(affixData);
		Objects.requireNonNull(wordGenerator);

		this.affixData = affixData;
		strategy = affixData.getFlagParsingStrategy();
		this.wordGenerator = wordGenerator;
		comparator = BaseBuilder.getComparator(affixData.getLanguage());
		lineEntryComparator = Comparator.comparingInt((LineEntry entry) -> RegExpSequencer.splitSequence(entry.condition).length)
			.thenComparing(Comparator.comparingInt(entry -> StringUtils.countMatches(entry.condition, GROUP_END)))
			.thenComparing(Comparator.comparingInt(entry -> entry.removal.length()))
			.thenComparing(Comparator.comparing(entry -> StringUtils.reverse(entry.condition), comparator))
			.thenComparing(Comparator.comparing(entry -> entry.removal, comparator))
			.thenComparing(Comparator.comparingInt(entry -> entry.anAddition().length()))
			.thenComparing(Comparator.comparing(entry -> entry.anAddition(), comparator));
	}


	public LineEntry collectProductionsByFlag(final List<Production> productions, final String flag, final AffixEntry.Type type){
		//remove base production
		productions.remove(0);
		final List<LineEntry> filteredRules = new ArrayList<>();
		for(final Production production : productions){
			final AffixEntry lastAppliedRule = production.getLastAppliedRule(type);
			if(lastAppliedRule != null && lastAppliedRule.getFlag().equals(flag)){
				final String word = lastAppliedRule.undoRule(production.getWord());
				final LineEntry affixEntry = (lastAppliedRule.isSuffix()?
					createSuffixEntry(production, word, type):
					createPrefixEntry(production, word, type));
				filteredRules.add(affixEntry);
			}
		}

		LineEntry compactedFilteredRule = null;
		if(!filteredRules.isEmpty()){
			//same same removal and same condition parts
			final List<LineEntry> compactedFilteredRules = collect(filteredRules,
				entry -> entry.removal + TAB + entry.condition,
				(rule, entry) -> rule.addition.addAll(entry.addition));

			compactedFilteredRule = compactedFilteredRules.stream()
				.max(Comparator.comparingInt(rule -> rule.condition.length()))
				.get();
			if(compactedFilteredRules.size() > 1){
				final int longestConditionLength = compactedFilteredRule.condition.length();
				for(final LineEntry rule : compactedFilteredRules){
					final int delta = longestConditionLength - rule.condition.length();
					final String from = rule.from.iterator().next();
					final int startIndex = from.length() - longestConditionLength;
					final String deltaAddition = from.substring(startIndex, startIndex + delta);
					for(final String addition : rule.addition)
						compactedFilteredRule.addition.add(deltaAddition + addition);
				}
			}
		}
		return compactedFilteredRule;
	}

	private LineEntry createSuffixEntry(final Production production, final String word, final AffixEntry.Type type){
		int lastCommonLetter;
		final int wordLength = word.length();
		final String producedWord = production.getWord();
		for(lastCommonLetter = 0; lastCommonLetter < Math.min(wordLength, producedWord.length()); lastCommonLetter ++)
			if(word.charAt(lastCommonLetter) != producedWord.charAt(lastCommonLetter))
				break;

		final String removal = (lastCommonLetter < wordLength? word.substring(lastCommonLetter): AffixEntry.ZERO);
		String addition = (lastCommonLetter < producedWord.length()? producedWord.substring(lastCommonLetter): AffixEntry.ZERO);
		if(production.getContinuationFlagCount() > 0)
			addition += production.getLastAppliedRule(type).toStringWithMorphologicalFields(strategy);
		final String condition = (lastCommonLetter < wordLength? removal: StringUtils.EMPTY);
		return new LineEntry(removal, addition, condition, word);
	}

	private LineEntry createPrefixEntry(final Production production, final String word, final AffixEntry.Type type){
		int firstCommonLetter;
		final int wordLength = word.length();
		final String producedWord = production.getWord();
		final int productionLength = producedWord.length();
		final int minLength = Math.min(wordLength, productionLength);
		for(firstCommonLetter = 1; firstCommonLetter <= minLength; firstCommonLetter ++)
			if(word.charAt(wordLength - firstCommonLetter) != producedWord.charAt(productionLength - firstCommonLetter))
				break;
		firstCommonLetter --;

		final String removal = (firstCommonLetter < wordLength? word.substring(0, wordLength - firstCommonLetter): AffixEntry.ZERO);
		String addition = (firstCommonLetter < productionLength? producedWord.substring(0, productionLength - firstCommonLetter): AffixEntry.ZERO);
		if(production.getContinuationFlagCount() > 0)
			addition += production.getLastAppliedRule(type).toStringWithMorphologicalFields(strategy);
		final String condition = (firstCommonLetter < wordLength? removal: StringUtils.EMPTY);
		return new LineEntry(removal, addition, condition, word);
	}

	public List<LineEntry> reduceProductions(final List<LineEntry> plainRules){
		final List<LineEntry> compactedRules = compactRules(plainRules);

		LOGGER.info(Backbone.MARKER_APPLICATION, "Extracted {} rules", compactedRules.size());

		removeOverlappingConditions(compactedRules);

		mergeSimilarRules(compactedRules);

		return compactedRules;
	}

	public List<String> convertFormat(final String flag, final boolean keepLongestCommonAffix, final List<LineEntry> compactedRules) throws IllegalArgumentException{
		final RuleEntry ruleToBeReduced = affixData.getData(flag);
		if(ruleToBeReduced == null)
			throw new IllegalArgumentException("Non-existent rule " + flag + ", cannot reduce");

		final AffixEntry.Type type = ruleToBeReduced.getType();
		final List<String> rules = new ArrayList<>();
		rules.add(composeHeader(type, flag, ruleToBeReduced.combineableChar(), compactedRules.size()));
		rules.addAll(convertEntriesToRules(flag, type, keepLongestCommonAffix, compactedRules));
		return rules;
	}

	public void checkReductionCorrectness(final String flag, final List<String> reducedRules, final List<LineEntry> originalRules, final List<String> originalLines)
			throws IllegalArgumentException{
		final RuleEntry ruleToBeReduced = affixData.getData(flag);
		if(ruleToBeReduced == null)
			throw new IllegalArgumentException("Non-existent rule " + flag + ", cannot reduce");

		final Set<LineEntry> checkRules = new HashSet<>();
		final List<AffixEntry> entries = reducedRules.stream()
			.skip(1)
			.map(line -> new AffixEntry(line, strategy, null, null))
			.collect(Collectors.toList());
		final AffixEntry.Type type = ruleToBeReduced.getType();
		final RuleEntry overriddenRule = new RuleEntry((type == AffixEntry.Type.SUFFIX), ruleToBeReduced.combineableChar(), entries);
		for(String line : originalLines){
			final List<Production> productions = wordGenerator.applyAffixRules(line, overriddenRule);

			final LineEntry compactedFilteredRule = collectProductionsByFlag(productions, flag, type);
			checkRules.add(compactedFilteredRule);
		}
		Set<LineEntry> uniquePlainRules = new HashSet<>(originalRules);
		if(!checkRules.equals(uniquePlainRules)){
			//FIXME
Set<LineEntry> intersection = SetHelper.intersection(checkRules, uniquePlainRules);
checkRules.removeAll(intersection);
uniquePlainRules.removeAll(intersection);
LOGGER.info(Backbone.MARKER_RULE_REDUCER, "overproduced rules");
for(final LineEntry entry : checkRules)
	LOGGER.info(Backbone.MARKER_RULE_REDUCER, entry.toString());
LOGGER.info(Backbone.MARKER_RULE_REDUCER, "undersupplied rules");
for(final LineEntry entry : uniquePlainRules)
	LOGGER.info(Backbone.MARKER_RULE_REDUCER, entry.toString());
			throw new IllegalArgumentException("Something very bad occurs while reducing, expected " + uniquePlainRules.size() + " productions, obtained "
				+ checkRules.size());
		}
	}

	private List<LineEntry> compactRules(final Collection<LineEntry> rules){
		//same removal, addition, and condition parts
		return collect(rules, entry -> entry.hashCode(), (rule, entry) -> rule.from.addAll(entry.from));
	}

	private <K, V> List<V> collect(final Collection<V> entries, final Function<V, K> keyMapper, final BiConsumer<V, V> mergeFunction){
		final Map<K, V> compaction = new HashMap<>();
		for(final V entry : entries){
			final K key = keyMapper.apply(entry);
			final V rule = compaction.putIfAbsent(key, entry);
			if(rule != null)
				mergeFunction.accept(rule, entry);
		}
		return new ArrayList<>(compaction.values());
	}

	/**<pre>
	 * sort current-list by shortest condition
	 * while current-list is not empty{
	 * 	extract rule from current-list
	 * 	find parent-group
	 * 	find children-group
	 * 	if intersection(parent-group, children-group) is empty{
	 * 		add new rule from parent with condition starting with NOT(children-group) to final-list
	 *
	 * 		if parent.condition is not empty and can-bubble-up{
	 * 			bubble up by bucketing children for group-2
	 * 			for each children-group-2
	 * 				add new rule from parent with condition starting with NOT(children-group-1) to final-list
	 *
	 * 			remove bubbles from current-list
	 * 		}
	 *
	 * 		for each children-same-condition
	 * 			add new rule from child with condition starting with (child-group) to final-list
	 *
	 * 		remove same-condition-children from current-list
	 * 		remove same-condition-children from final-list
	 * 	}
	 * 	else if parent.condition is empty{
	 * 		for each last-char of parent.from
	 * 			if last-char is contained into intersection
	 * 				add new rule from parent with condition the last-char
	 *
	 * 		if intersection is proper subset of parent-group{
	 * 			add new rule from parent with condition the difference between parent-grop and intersection to final-list
	 * 			keep only rules that matches some existent words
	 * 		}
	 * 	}
	 * 	else{
	 * 		?
	 * //		calculate intersection between parent and children conditions
	 * //		if intersection is empty
	 * //			check if removal == 0 && exists a rule in children that have another-rule.removal = removal+condition and another-rule.condition == condition
	 * 	}
	 *
	 * 	remove parent from final-list
	 * }
	 * </pre>
	 */
	//FIXME tested bottom-up until r3
	private void removeOverlappingConditions(final List<LineEntry> rules){
		//sort current-list by shortest condition
		final List<LineEntry> sortedList = new ArrayList<>(rules);
		sortedList.sort(shortestConditionComparator);

		//while current-list is not empty
		while(!sortedList.isEmpty()){
			//extract rule from current-list
			final LineEntry parent = sortedList.remove(0);

			final List<LineEntry> children = sortedList.stream()
				.filter(entry -> entry.condition.endsWith(parent.condition))
				.collect(Collectors.toList());
			if(children.isEmpty()){
				if(!rules.contains(parent))
					rules.add(parent);

				continue;
			}

			final int parentConditionLength = parent.condition.length();
			//find parent-group
			final Set<Character> parentGroup = extractGroup(parent.from, parentConditionLength);

			final Set<String> childrenFrom = children.stream()
				 .flatMap(entry -> entry.from.stream())
				 .collect(Collectors.toSet());
			//find children-group
			final Set<Character> childrenGroup = extractGroup(childrenFrom, parentConditionLength);

			//if intersection(parent-group, children-group) is empty
			final Set<Character> groupIntersection = SetHelper.intersection(parentGroup, childrenGroup);
			if(groupIntersection.isEmpty()){
				//add new rule from parent with condition starting with NOT(children-group) to final-list
				String condition = (parentGroup.size() < childrenGroup.size()? makeGroup(parentGroup, parent.condition): makeNotGroup(childrenGroup, parent.condition));
				LineEntry newEntry = LineEntry.createFrom(parent, condition, parent.from);
				rules.add(newEntry);

				//if parent.condition is not empty
				if(!parent.condition.isEmpty()){
					final List<LineEntry> bubbles = extractRuleBubbles(parent, children);
					//if can-bubble-up
					if(!bubbles.isEmpty()){
						//FIXME
						final List<LineEntry> bubbledRules = bubbleUpNotGroup(parent, bubbles);
//						rules.addAll(bubbledRules);

						//remove bubbles from current-list
//						bubbles.forEach(sortedList::remove);
bubbledRules.size();
System.out.println("fix me");
					}
				}

				final List<LineEntry> sameConditionChildren = children.stream()
					.filter(entry -> !entry.condition.isEmpty() && entry.condition.equals(parent.condition))
					.collect(Collectors.toList());
				//for each children-same-condition
				for(final LineEntry child : sameConditionChildren){
					//add new rule from child with condition starting with (child-group) to final-list
					final Set<Character> childGroup = extractGroup(child.from, parentConditionLength);
					//FIXME
					condition = (sameConditionChildren.size() == 1 && !sameConditionChildren.containsAll(childGroup) && childGroup.size() > 1?
						makeNotGroup(parentGroup, child.condition): makeGroup(childGroup, child.condition));
//					condition = makeGroup(childGroup, child.condition);
					newEntry = LineEntry.createFrom(child, condition, child.from);
					rules.add(newEntry);
				}

				//remove same-condition-children from current-list
				sameConditionChildren.forEach(sortedList::remove);
				//remove same-condition-children from final-list
				sameConditionChildren.forEach(rules::remove);
			}
			else{
				final String notGroupIntersection = makeNotGroup(groupIntersection, StringUtils.EMPTY);

				//should be here...
				if(parentGroup.equals(groupIntersection)){
					final String condition = notGroupIntersection + parent.condition;
					final List<String> words = parent.extractFromEndingWith(condition);
					if(!words.isEmpty()){
						final LineEntry newEntry = LineEntry.createFrom(parent, condition, words);
						rules.add(newEntry);
					}
				}

				final Map<String, List<String>> fromBucket = bucket(parent.from,
					from -> {
						char chr = from.charAt(from.length() - parentConditionLength - 1);
						return (groupIntersection.contains(chr)? String.valueOf(chr): notGroupIntersection);
					});
				final List<String> notGroupList = fromBucket.remove(notGroupIntersection);
				if(notGroupList != null){
					final Set<Character> preCondition = extractGroup(notGroupList, parentConditionLength);
					final String condition = (parent.condition.isEmpty()? makeGroup(preCondition, parent.condition): makeNotGroup(childrenGroup, parent.condition));
					final LineEntry newEntry = LineEntry.createFrom(parent, condition, notGroupList);
					rules.add(newEntry);
				}
				//... or here?
//				else{
//					final String condition = notGroupIntersection + parent.condition;
//					final List<String> words = parent.extractFromEndingWith(condition);
//					if(!words.isEmpty()){
//						final LineEntry newEntry = LineEntry.createFrom(parent, condition, words);
//						rules.add(newEntry);
//					}
//				}
				for(Map.Entry<String, List<String>> entry : fromBucket.entrySet()){
					final String condition = entry.getKey() + parent.condition;
					final LineEntry newEntry = LineEntry.createFrom(parent, condition, entry.getValue());
					sortedList.add(newEntry);
				}

				sortedList.sort(shortestConditionComparator);
			}

			//remove parent from final list
			rules.remove(parent);
		}
	}

	private Set<Character> extractGroup(final Collection<String> words, final int indexFromLast){
		final Set<Character> group = new HashSet<>();
		for(String word : words){
			final int index = word.length() - indexFromLast - 1;
			if(index < 0)
				throw new IllegalArgumentException("Cannot extract group from [" + StringUtils.join(words, ",")
					+ "] at index " + indexFromLast + " from last because of the presence of the word " + word + " that is too short");

			group.add(word.charAt(index));
		}
		return group;
	}

	private String makeGroup(final Set<Character> group, final String suffix){
		String merge = mergeSet(group);
		return (merge.length() > 1? GROUP_START + merge + GROUP_END: merge) + suffix;
	}

	private String makeNotGroup(final Set<Character> group, final String suffix){
		String merge = mergeSet(group);
		return NOT_GROUP_START + merge + GROUP_END + suffix;
	}

	private String mergeSet(final Set<Character> set){
		return set.stream()
			.map(String::valueOf)
			.sorted(comparator)
			.collect(Collectors.joining());
	}

	private <K, V> Map<K, List<V>> bucket(final Collection<V> entries, final Function<V, K> keyMapper){
		final Map<K, List<V>> bucket = new HashMap<>();
		for(V entry : entries){
			final K key = keyMapper.apply(entry);
			if(key != null)
				bucket.computeIfAbsent(key, k -> new ArrayList<>())
					.add(entry);
		}
		return bucket;
	}

	/** Merge common conditions (ex. `[^a]bc` and `[^a]dc` will become `[^a][bd]c`) */
	private void mergeSimilarRules(List<LineEntry> entries){
		Map<String, List<LineEntry>> similarityBucket = bucket(entries, entry -> (entry.condition.contains(GROUP_END)?
			entry.removal + TAB + entry.addition + TAB + RegExpSequencer.splitSequence(entry.condition)[0] + TAB
				+ RegExpSequencer.splitSequence(entry.condition).length:
			null));
		for(List<LineEntry> similarities : similarityBucket.values())
			if(similarities.size() > 1){
				LineEntry firstEntry = similarities.iterator().next();
				String[] firstCondition = RegExpSequencer.splitSequence(firstEntry.condition);
				String[] commonPreCondition = SEQUENCER.subSequence(firstCondition, 0, 1);
				String[] commonPostCondition = SEQUENCER.subSequence(firstCondition, 2);
				//extract all the rules from `similarities` that has the condition compatible with firstEntry.condition
				String condition = similarities.stream()
					.map(entry -> RegExpSequencer.splitSequence(entry.condition)[1])
					.sorted(comparator)
					.distinct()
					.collect(Collectors.joining(StringUtils.EMPTY, GROUP_START, GROUP_END));
				condition = StringUtils.join(commonPreCondition) + condition + StringUtils.join(commonPostCondition);
				final List<String> words = firstEntry.extractFromEndingWith(condition);
				entries.add(LineEntry.createFrom(firstEntry, condition, words));

				similarities.forEach(entries::remove);
			}
	}

	private List<String> convertEntriesToRules(final String flag, final AffixEntry.Type type, final boolean keepLongestCommonAffix,
			final Collection<LineEntry> entries){
		//restore original rules
		final List<LineEntry> restoredRules = entries.stream()
			.flatMap(rule -> {
				return rule.addition.stream()
					.map(addition -> {
						final int lcp = commonPrefix(rule.removal, addition).length();
						final String removal = rule.removal.substring(lcp);
						return new LineEntry((removal.isEmpty()? ZERO: removal), addition.substring(lcp), rule.condition, rule.from);
					});
			})
			.collect(Collectors.toList());

		final List<LineEntry> sortedEntries = prepareRules(type, keepLongestCommonAffix, restoredRules);

		return composeAffixRules(flag, type, sortedEntries);
	}

	private List<LineEntry> prepareRules(final AffixEntry.Type type, final boolean keepLongestCommonAffix, final Collection<LineEntry> entries){
		if(keepLongestCommonAffix)
			for(final LineEntry entry : entries){
				String lcs = longestCommonAffix(entry.from, (type == AffixEntry.Type.SUFFIX? this::commonSuffix: this::commonPrefix));
				if(lcs == null)
					lcs = entry.condition;
				else if(entry.condition.contains(GROUP_END)){
					final String[] entryCondition = RegExpSequencer.splitSequence(entry.condition);
					if(!SEQUENCER.endsWith(RegExpSequencer.splitSequence(lcs), entryCondition)){
						final int tailCharactersToExclude = entryCondition.length;
						if(tailCharactersToExclude <= lcs.length())
							lcs = lcs.substring(0, lcs.length() - tailCharactersToExclude) + entry.condition;
						else
							lcs = entry.condition;
					}
				}
				if(lcs.length() < entry.condition.length())
					throw new IllegalArgumentException("really bad error, lcs.length < condition.length");

				entry.condition = lcs;
			}
		final List<LineEntry> sortedEntries = new ArrayList<>(entries);
		sortedEntries.sort(lineEntryComparator);
		return sortedEntries;
	}

	private String longestCommonAffix(final Collection<String> texts, final BiFunction<String, String, String> commonAffix){
		String lcs = null;
		if(!texts.isEmpty()){
			final Iterator<String> itr = texts.iterator();
			lcs = itr.next();
			while(!lcs.isEmpty() && itr.hasNext())
				lcs = commonAffix.apply(lcs, itr.next());
		}
		return lcs;
	}

	/**
	 * Returns the longest string {@code suffix} such that {@code a.toString().endsWith(suffix) &&
	 * b.toString().endsWith(suffix)}, taking care not to split surrogate pairs. If {@code a} and
	 * {@code b} have no common suffix, returns the empty string.
	 */
	private String commonSuffix(final String a, final String b){
		int s = 0;
		final int aLength = a.length();
		final int bLength = b.length();
		final int maxSuffixLength = Math.min(aLength, bLength);
		while(s < maxSuffixLength && a.charAt(aLength - s - 1) == b.charAt(bLength - s - 1))
			s ++;
		if(validSurrogatePairAt(a, aLength - s - 1) || validSurrogatePairAt(b, bLength - s - 1))
			s --;
		return a.subSequence(aLength - s, aLength).toString();
	}

	/**
	 * Returns the longest string {@code prefix} such that {@code a.toString().startsWith(prefix) &&
	 * b.toString().startsWith(prefix)}, taking care not to split surrogate pairs. If {@code a} and
	 * {@code b} have no common prefix, returns the empty string.
	 */
	private String commonPrefix(final String a, final String b){
		int p = 0;
		final int maxPrefixLength = Math.min(a.length(), b.length());
		while(p < maxPrefixLength && a.charAt(p) == b.charAt(p))
			p ++;
		if(validSurrogatePairAt(a, p - 1) || validSurrogatePairAt(b, p - 1))
			p --;
		return a.subSequence(0, p).toString();
	}

	/**
	 * True when a valid surrogate pair starts at the given {@code index} in the given {@code string}.
	 * Out-of-range indexes return false.
	 */
	private boolean validSurrogatePairAt(final CharSequence string, final int index){
		return (index >= 0 && index <= (string.length() - 2)
			&& Character.isHighSurrogate(string.charAt(index))
			&& Character.isLowSurrogate(string.charAt(index + 1)));
	}

	private List<String> composeAffixRules(final String flag, final AffixEntry.Type type, final List<LineEntry> entries){
		final int size = entries.size();
		final List<String> rules = new ArrayList<>(size);
		for(LineEntry entry : entries)
			rules.add(composeLine(type, flag, entry));
		return rules;
	}

	private String composeHeader(final AffixEntry.Type type, final String flag, final char combineableChar, final int size){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		return sj.add(type.getTag().getCode())
			.add(flag)
			.add(Character.toString(combineableChar))
			.add(Integer.toString(size))
			.toString();
	}

	private String composeLine(final AffixEntry.Type type, final String flag, final LineEntry partialLine){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		return sj.add(type.getTag().getCode())
			.add(flag)
			.add(partialLine.removal)
			.add(partialLine.anAddition())
			.add(partialLine.condition.isEmpty()? DOT: partialLine.condition)
			.toString();
	}



	private List<RulesReducer.LineEntry> extractRuleBubbles(final RulesReducer.LineEntry parent, final List<RulesReducer.LineEntry> sortedList){
		final int parentConditionLength = parent.condition.length();
		return sortedList.stream()
			.filter(entry -> entry.condition.length() > parentConditionLength)
			.collect(Collectors.toList());
	}

	private List<RulesReducer.LineEntry> bubbleUpNotGroup(final RulesReducer.LineEntry parent, final List<RulesReducer.LineEntry> children){
		final int parentConditionLength = parent.condition.length();
		//bubble up by bucketing children for group-2
		final Set<String> bubblesCondition = children.stream()
			.map(entry -> entry.condition)
			.collect(Collectors.toSet());

		/*
		extract communalities:
		from
			"è => [èdo, èđo, èxo]"
			"ò => [òdo, òco, òko]"
		transform into
			"è => [èđo, èxo]"
			"ò => [òco, òko]"
			"èò => [òdo, èdo]"
		extract common-group (key.length > 1)
		add new rule from parent with condition starting with NOT(common-group) to final-list
			'[^èò]do'
		*/
		final List<RulesReducer.LineEntry> newParents = new ArrayList<>();
		final Map<String, List<String>> communalitiesBucket = bucket(bubblesCondition,
			cond -> cond.substring(cond.length() - parentConditionLength - 1));
		//remove single conditions
		Iterator<Map.Entry<String, List<String>>> itr = communalitiesBucket.entrySet().iterator();
		while(itr.hasNext())
			if(itr.next().getValue().size() == 1)
				itr.remove();
		for(final Map.Entry<String, List<String>> e : communalitiesBucket.entrySet()){
			final List<String> comm = e.getValue();
			//FIXME
//			if(e.getKey().length() + 1 != comm.get(0).length() || !comm.stream().allMatch(c -> c.length() == comm.get(0).length()))
//				throw new IllegalArgumentException("e.key.length + 1 != comm[0].length || comm.get(.).length() differs: key '" + e.getKey()
//					+ "', comm '" + comm.toString());

			for(String c : comm){
				final List<String> gg = children.stream()
					.filter(entry -> entry.condition.equals(c))
					.flatMap(entry -> entry.from.stream())
					.collect(Collectors.toList());
				bucket(gg, cond -> cond.substring(cond.length() - parentConditionLength - 1));
			}
			final Set<Character> commonGroup = extractGroup(comm, e.getKey().length());
			final String condition = makeNotGroup(commonGroup, e.getKey());
			final List<String> words = parent.extractFromEndingWith(condition);
			final RulesReducer.LineEntry newEntry = RulesReducer.LineEntry.createFrom(parent, condition, words);
			//keep only rules that matches some existent words
			if(!words.isEmpty())
				newParents.add(newEntry);
			else
				LOGGER.debug("skip unused rule: {} {} {}", newEntry.removal, String.join("|", newEntry.addition),
					(newEntry.condition.isEmpty()? DOT: newEntry.condition));

			comm.forEach(bubblesCondition::remove);
		}

		final Map<String, List<String>> conditionBucket = bucket(bubblesCondition,
			cond -> cond.substring(0, cond.length() - parentConditionLength - 1));
		//for each children-group-2
		for(final Map.Entry<String, List<String>> conds : conditionBucket.entrySet()){
			//add new rule from parent with condition starting with NOT(children-group-2) to final-list
			final Set<Character> bubbleGroup = extractGroup(conds.getValue(), parentConditionLength);
			//do the bubble trick
			for(int i = conds.getKey().length(); i > 0; i --){
				final String condition = makeNotGroup(conds.getKey().charAt(i - 1))
					+ conds.getKey().substring(i)
					+ makeGroup(bubbleGroup, parent.condition);
				final List<String> words = parent.extractFromEndingWith(condition);
				final RulesReducer.LineEntry newEntry = RulesReducer.LineEntry.createFrom(parent, condition, words);
				//keep only rules that matches some existent words
				if(!words.isEmpty())
					newParents.add(newEntry);
				else
					LOGGER.debug("skip unused rule: {} {} {}", newEntry.removal, String.join("|", newEntry.addition),
						(newEntry.condition.isEmpty()? DOT: newEntry.condition));
			}
		}
		return newParents;
	}

	private String makeNotGroup(final char group){
		return NOT_GROUP_START + group + GROUP_END;
	}

}
