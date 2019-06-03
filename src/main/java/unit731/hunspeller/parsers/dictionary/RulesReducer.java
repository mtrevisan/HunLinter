package unit731.hunspeller.parsers.dictionary;

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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
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
import unit731.hunspeller.services.SetHelper;


public class RulesReducer{

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesReducer.class);

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


	public List<LineEntry> collectProductionsByFlag(final List<Production> productions, final String flag, final AffixEntry.Type type){
		//remove base production
		productions.remove(0);
		//collect all productions that generates from the given flag
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
		return compactProductions(filteredRules);
	}

	private List<LineEntry> compactProductions(final List<LineEntry> filteredRules){
		//compact rules by aggregating productions with same originating word
		Map<String, List<LineEntry>> fromBucket = bucket(new HashSet<>(filteredRules), rule -> rule.from.iterator().next());
		for(final Map.Entry<String, List<LineEntry>> entry : fromBucket.entrySet()){
			final List<LineEntry> rules = entry.getValue();
			if(rules.size() > 1){
				//retrieve rule with longest condition (all the other conditions must be this long)
				final LineEntry compactedRule = rules.stream()
					.max(Comparator.comparingInt(rule -> rule.condition.length()))
					.get();
				final int longestConditionLength = compactedRule.condition.length();
				final String from = entry.getKey();
				for(final LineEntry rule : rules){
					//recover the missing characters for the current condition to become of length the maximum found earlier
					final int startIndex = from.length() - longestConditionLength;
					//FIXME what if a condition is not long enough? keep it separate?
//					if(startIndex < 0)
//						throw new IllegalArgumentException("condition '" + from + "' cannot be extended to reach longest condition '"
//							+ compactedFilteredRule.condition + "'");

					//if a condition is not long enough, keep it separate
					if(startIndex >= 0){
						final int delta = longestConditionLength - rule.condition.length();
						final String deltaAddition = from.substring(startIndex, startIndex + delta);
						//add addition
						for(final String addition : rule.addition)
							compactedRule.addition.add(deltaAddition + addition);
					}
				}
				rules.clear();
				rules.add(compactedRule);
			}
		}
		return fromBucket.values().stream()
			.flatMap(List::stream)
			.collect(Collectors.toList());
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
		final AffixEntry lastAppliedRule = production.getLastAppliedRule(type);
		if(lastAppliedRule != null)
			addition += lastAppliedRule.toStringWithMorphologicalFields(strategy);
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

	public List<LineEntry> reduceRules(final List<LineEntry> plainRules){
		final List<LineEntry> compactedRules = compactRules(plainRules);

		LOGGER.info(Backbone.MARKER_APPLICATION, "Extracted {} rules from {} productions", compactedRules.size(),
			DictionaryParser.COUNTER_FORMATTER.format(plainRules.size()));

		removeOverlappingConditions(compactedRules);

		mergeSimilarRules(compactedRules);

		return compactedRules;
	}

	public List<String> convertFormat(final String flag, final boolean keepLongestCommonAffix, final List<LineEntry> compactedRules)
			throws IllegalArgumentException{
		final RuleEntry ruleToBeReduced = affixData.getData(flag);
		if(ruleToBeReduced == null)
			throw new IllegalArgumentException("Non-existent rule " + flag + ", cannot reduce");

		final AffixEntry.Type type = ruleToBeReduced.getType();
		final List<String> prettyPrintRules = convertEntriesToRules(flag, type, keepLongestCommonAffix, compactedRules);
		prettyPrintRules.add(0, composeHeader(type, flag, ruleToBeReduced.combineableChar(), prettyPrintRules.size()));
		return prettyPrintRules;
	}

	public void checkReductionCorrectness(final String flag, final List<String> reducedRules, final List<LineEntry> originalRules,
			final List<String> originalLines) throws IllegalArgumentException{
		final RuleEntry ruleToBeReduced = affixData.getData(flag);
		if(ruleToBeReduced == null)
			throw new IllegalArgumentException("Non-existent rule " + flag + ", cannot reduce");

		final List<AffixEntry> entries = reducedRules.stream()
			.skip(1)
			.map(line -> new AffixEntry(line, strategy, null, null))
			.collect(Collectors.toList());
		final AffixEntry.Type type = ruleToBeReduced.getType();
		final RuleEntry overriddenRule = new RuleEntry((type == AffixEntry.Type.SUFFIX), ruleToBeReduced.combineableChar(), entries);
		for(final String line : originalLines){
			final List<Production> originalProductions = wordGenerator.applyAffixRules(line);
			final List<Production> productions = wordGenerator.applyAffixRules(line, overriddenRule);

			final List<LineEntry> filteredOriginalRules = collectProductionsByFlag(originalProductions, flag, type);
			final List<LineEntry> filteredRules = collectProductionsByFlag(productions, flag, type);
			if(!filteredOriginalRules.equals(filteredRules))
				throw new IllegalArgumentException("Something very bad occurs while producing from '" + line + "', expected "
					+ filteredOriginalRules + ", obtained " + filteredRules);
		}
	}

	private List<LineEntry> compactRules(final Collection<LineEntry> rules){
		//same removal, addition, and condition parts
		return collect(rules,
			entry -> entry.removal + TAB + mergeSet(entry.addition) + TAB + entry.condition,
			(rule, entry) -> rule.from.addAll(entry.from));
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
	private void removeOverlappingConditions(final List<LineEntry> rules/*, final AffixEntry.Type type*/){
		//sort current-list by shortest condition
		final List<LineEntry> sortedList = new ArrayList<>(rules);
		sortedList.sort(shortestConditionComparator);

AffixEntry.Type type = AffixEntry.Type.PREFIX;

		//while current-list is not empty
		while(!sortedList.isEmpty()){
			//extract rule from current-list
			final String parentCondition = sortedList.get(0).condition;
			final List<LineEntry> children = sortedList.stream()
				.filter(entry -> entry.condition.endsWith(parentCondition))
				.collect(Collectors.toList());
			if(children.size() == 1)
				children.stream()
					.filter(Predicate.not(rules::contains))
					.forEach(rules::add);
			else{
				redistributeAdditions(children, type);

				boolean wereNotIntersecting = false;
				//extract minimum and maximum of the conditions' length
				int[] minAndMaxConditionLength = extractMinAndMax(children);
				for(int index = minAndMaxConditionLength[0]; index < minAndMaxConditionLength[1]; index ++){
					//extract the group of each child
					final int indexFromLast = index;
					final Map<LineEntry, Set<Character>> groups = children.stream()
						.collect(Collectors.toMap(Function.identity(), child -> extractGroup(child.from, indexFromLast)));

					//calculate intersection between all groups
					final Set<Character> groupsIntersection = SetHelper.intersection(groups.values());

					//if there was no intersection on a previous run, then it means the conditions are already disjoint,
					//so if there is no intersection again it can be skipped as already disjoint
					if(wereNotIntersecting && groupsIntersection.isEmpty())
						break;

					//if intersection is empty
					if(groupsIntersection.isEmpty()){
						//for each group, either is the group itself or the negated group of all the others children
						//'Aèr': that is 'Aèr' or '[^Bi]èr'
						//'Bèr': that is 'Bèr' or '[^Ai]èr'
						//'ièr': that is 'ièr' or '[^AB]èr'
						Iterator<LineEntry> itr = children.iterator();
						while(itr.hasNext()){
							final LineEntry child = itr.next();
							final int childConditionLength = child.condition.length();
							if(childConditionLength <= index){
								//extract ratifing group
								final Set<Character> childGroup = groups.get(child);

								//extract negated group (excluding the current group)
								final Set<Character> childNotGroup = new HashSet<>();
								for(final Set<Character> group : groups.values())
									if(group != childGroup)
										childNotGroup.addAll(group);

								//calculate new condition (if it was empty or the ratifing group is smaller than the negated one, choose the ratifing)
								child.condition = (childConditionLength == 0 || childGroup.size() < childNotGroup.size()?
									makeGroup(childGroup): makeNotGroup(childNotGroup))
									+ (childConditionLength > 0? child.condition.substring(childConditionLength - index): child.condition);

								//exclude from further processing
								if(childConditionLength == index){
									//remove from subsequent runs
									itr.remove();
									//remove from processing list
									sortedList.remove(child);
								}
							}
						}

						//set condition is already disjoint (for further runs)
						wereNotIntersecting = true;
					}
					//if the group intersects the negated group of all the other children
					else{
						//TODO keep only intersecting rules?

						throw new IllegalArgumentException("and now?");
					}
				}
			}

			children.forEach(sortedList::remove);


//			final Set<String> childrenFrom = children.stream()
//				.flatMap(entry -> entry.from.stream())
//				.collect(Collectors.toSet());
			//find children-group
//			final Set<Character> childrenGroup = extractGroup(childrenFrom, parentConditionLength);
//
//			//if intersection(parent-group, children-group) is empty
//			final Set<Character> groupIntersection = SetHelper.intersection(parentGroup, childrenGroup);
//			if(groupIntersection.isEmpty()){
//				//add new rule from parent with condition starting with NOT(children-group) to final-list
//				//FIXME condition not always the best...
//				String condition = (parent.condition.isEmpty() || parentGroup.size() < childrenGroup.size()?
//					makeGroup(parentGroup, parent.condition): makeNotGroup(childrenGroup, parent.condition));
//				LineEntry newEntry = LineEntry.createFrom(parent, condition, parent.from);
//				rules.add(newEntry);
//
//				final List<LineEntry> sameConditionChildren = children.stream()
//					.filter(entry -> !entry.condition.isEmpty() && entry.condition.equals(parent.condition))
//					.collect(Collectors.toList());
//				//for each children-same-condition
//				for(final LineEntry child : sameConditionChildren){
//					//add new rule from child with condition starting with (child-group) to final-list
//					final Set<Character> childGroup = extractGroup(child.from, parentConditionLength);
//					final Set<String> sameConditionChildrenFrom = children.stream()
//						.filter(c -> c != child)
//						.filter(c -> c.condition.endsWith(child.condition))
//						.flatMap(entry -> entry.from.stream())
//						.collect(Collectors.toSet());
//					final Set<Character> sameConditionChildrenGroup = extractGroup(sameConditionChildrenFrom, parentConditionLength);
//					if(sameConditionChildrenGroup.isEmpty() && parentGroup.size() < childGroup.size() || parentGroup.equals(sameConditionChildrenGroup))
//						condition = makeNotGroup(parentGroup, child.condition);
//					else
//						condition = makeGroup(childGroup, child.condition);
//					newEntry = LineEntry.createFrom(child, condition, child.from);
//					rules.add(newEntry);
//				}
//
//				//remove same-condition-children from current-list
//				sameConditionChildren.forEach(sortedList::remove);
//				//remove same-condition-children from final-list
//				sameConditionChildren.forEach(rules::remove);
//			}
//			else{
//				final String notGroupIntersection = makeNotGroup(groupIntersection, StringUtils.EMPTY);
//				final Map<String, List<String>> fromBucket = bucket(parent.from,
//					from -> {
//						char chr = from.charAt(from.length() - parentConditionLength - 1);
//						return (groupIntersection.contains(chr)? String.valueOf(chr): notGroupIntersection);
//					});
//				final List<String> notGroupList = fromBucket.remove(notGroupIntersection);
//				if(notGroupList != null){
//					final Set<Character> preCondition = extractGroup(notGroupList, parentConditionLength);
//					final String condition = (parent.condition.isEmpty()
//							|| !childrenGroup.containsAll(preCondition) && preCondition.size() < childrenGroup.size()?
//						makeGroup(preCondition, parent.condition):
//						makeNotGroup(childrenGroup, parent.condition));
//					final LineEntry newEntry = LineEntry.createFrom(parent, condition, notGroupList);
//					rules.add(newEntry);
//				}
//				for(final Map.Entry<String, List<String>> entry : fromBucket.entrySet()){
//					final String condition = entry.getKey() + parent.condition;
//					final LineEntry newEntry = LineEntry.createFrom(parent, condition, entry.getValue());
//					sortedList.add(newEntry);
//				}
//
//				sortedList.sort(shortestConditionComparator);
//			}

//			sortedList.remove(0);

			//remove parent from final list
//			rules.remove(parent);
		}
	}

	private int[] extractMinAndMax(final List<LineEntry> children){
		//collect all the length of the children's conditions
		final List<Integer> conditionLengths = children.stream()
			.map(child -> child.condition.length())
			.sorted()
			.collect(Collectors.toList());
		//extract minimum and maximum of the conditions' length
		final int minConditionLength = conditionLengths.get(0);
		final int maxConditionLength = conditionLengths.get(conditionLengths.size() - 1);
		return new int[]{minConditionLength, maxConditionLength};
	}

	/** Reshuffle originating list to place the correct productions in the correct rule */
	private void redistributeAdditions(final List<LineEntry> children, AffixEntry.Type type){
		//cycle parent in all the children
		for(final LineEntry parent : children){
			//extract raw additions from parent
			final Set<String> parentAdditions = parent.addition.stream()
				.map(addition -> {
					final String lcs = longestCommonAffix(Arrays.asList(addition, parent.removal),
						(type == AffixEntry.Type.SUFFIX? this::commonSuffix: this::commonPrefix));
					return addition.substring(lcs.length());
				})
				.collect(Collectors.toSet());
			for(final LineEntry child : children)
				if(child != parent && child.removal.equals(parent.removal)){
					//extract raw additions from child
					int minimumLCSLength = Integer.MAX_VALUE;
					final Set<String> childAdditions = new HashSet<>();
					for(final String addition : child.addition){
						final int lcsLength = longestCommonAffix(Arrays.asList(addition, child.removal),
							(type == AffixEntry.Type.SUFFIX? this::commonSuffix: this::commonPrefix))
							.length();
						if(lcsLength < minimumLCSLength)
							minimumLCSLength = lcsLength;
						childAdditions.add(addition.substring(lcsLength));
					}

					//extract from each child all the additions present in parent
					if(child.condition.substring(minimumLCSLength).equals(parent.condition) && childAdditions.containsAll(parentAdditions)){
						//extract all the common from
						child.addition.removeAll(parent.addition);
						parent.from.addAll(child.from);
					}
				}
		}
	}

	private Set<Character> extractGroup(final Collection<String> words, final int indexFromLast){
		final Set<Character> group = new HashSet<>();
		for(final String word : words){
			final int index = word.length() - indexFromLast - 1;
			if(index < 0)
				throw new IllegalArgumentException("Cannot extract group from [" + StringUtils.join(words, ",")
					+ "] at index " + indexFromLast + " from last because of the presence of the word '" + word + "' that is too short");

			group.add(word.charAt(index));
		}
		return group;
	}

	private String makeGroup(final Set<Character> group){
		final String merge = mergeSet(group);
		return (merge.length() > 1? GROUP_START + merge + GROUP_END: merge);
	}

	private String makeGroup(final Set<Character> group, final String suffix){
		return makeGroup(group) + suffix;
	}

	private String makeNotGroup(final Set<Character> group){
		final String merge = mergeSet(group);
		return NOT_GROUP_START + merge + GROUP_END;
	}

	private String makeNotGroup(final Set<Character> group, final String suffix){
		return makeNotGroup(group) + suffix;
	}

	private <V> String mergeSet(final Set<V> set){
		return set.stream()
			.map(String::valueOf)
			.sorted(comparator)
			.collect(Collectors.joining());
	}

	private <K, V> Map<K, List<V>> bucket(final Collection<V> entries, final Function<V, K> keyMapper){
		final Map<K, List<V>> bucket = new HashMap<>();
		for(final V entry : entries){
			final K key = keyMapper.apply(entry);
			if(key != null)
				bucket.computeIfAbsent(key, k -> new ArrayList<>())
					.add(entry);
		}
		return bucket;
	}

	/** Merge common conditions (ex. `[^a]bc` and `[^a]dc` will become `[^a][bd]c`) */
	private void mergeSimilarRules(List<LineEntry> entries){
		final Map<String, List<LineEntry>> similarityBucket = bucket(entries, entry -> (entry.condition.contains(GROUP_END)?
			entry.removal + TAB + entry.addition + TAB + RegExpSequencer.splitSequence(entry.condition)[0] + TAB
				+ RegExpSequencer.splitSequence(entry.condition).length:
			null));
		for(final List<LineEntry> similarities : similarityBucket.values())
			if(similarities.size() > 1){
				final LineEntry anEntry = similarities.iterator().next();
				final String[] aCondition = RegExpSequencer.splitSequence(anEntry.condition);
				final String[] commonPreCondition = SEQUENCER.subSequence(aCondition, 0, 1);
				final String[] commonPostCondition = SEQUENCER.subSequence(aCondition, 2);
				//extract all the rules from `similarities` that has the condition compatible with firstEntry.condition
				final Set<Character> group = similarities.stream()
					.map(entry -> RegExpSequencer.splitSequence(entry.condition)[1].charAt(0))
					.collect(Collectors.toSet());
				final String condition = StringUtils.join(commonPreCondition) + makeGroup(group, StringUtils.EMPTY) + StringUtils.join(commonPostCondition);
				final List<String> words = anEntry.extractFromEndingWith(condition);
				entries.add(LineEntry.createFrom(anEntry, condition, words));

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
		for(final LineEntry entry : entries)
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
		String addition = partialLine.anAddition();
		String morphologicalRules = StringUtils.EMPTY;
		final int idx = addition.indexOf(TAB);
		if(idx >= 0){
			morphologicalRules = addition.substring(idx);
			addition = addition.substring(0, idx);
		}
		String line = type.getTag().getCode() + StringUtils.SPACE + flag + StringUtils.SPACE + partialLine.removal + StringUtils.SPACE
			+ addition + StringUtils.SPACE + (partialLine.condition.isEmpty()? DOT: partialLine.condition);
		if(idx >= 0)
			line += morphologicalRules;
		return line;
	}

}
