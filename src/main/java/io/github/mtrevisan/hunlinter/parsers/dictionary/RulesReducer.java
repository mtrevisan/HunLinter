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
package io.github.mtrevisan.hunlinter.parsers.dictionary;

import com.carrotsearch.hppcrt.IntObjectMap;
import com.carrotsearch.hppcrt.maps.IntObjectHashMap;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Pattern;


public class RulesReducer{

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesReducer.class);

	private static final String NON_EXISTENT_RULE = "Non-existent rule `{}`, cannot reduce";
	private static final String VERY_BAD_ERROR = "Something very bad happened while inflecting from `{}`, expected {}, obtained {}";

	private static final String PIPE = "|";

	private static final String TAB = "\t";
	private static final String ZERO = "0";
	private static final String DOT = ".";


	private final Comparator<LineEntry> shortestConditionComparator = Comparator.comparingInt(entry -> entry.condition.length());

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
		lineEntryComparator = Comparator.comparingInt((LineEntry entry) -> RegexSequencer.splitSequence(entry.condition).length)
			.thenComparingInt(entry -> StringUtils.countMatches(entry.condition, RegexHelper.GROUP_END))
			.thenComparingInt(entry -> entry.removal.length())
			.thenComparing(entry -> StringUtils.reverse(entry.condition), comparator)
			.thenComparing(entry -> entry.removal, comparator)
			.thenComparingInt(entry -> entry.anAddition().length())
			.thenComparing(LineEntry::anAddition, comparator);
	}


	private LineEntry createAffixEntry(final Inflection inflection, String word, final AffixType type){
		String producedWord = inflection.getWord();
		if(type ==AffixType.PREFIX){
			producedWord = StringUtils.reverse(producedWord);
			word = StringUtils.reverse(word);
		}

		final int lastCommonLetter = StringHelper.getLastCommonLetterIndex(word, producedWord);

		final int wordLength = word.length();
		final String removal = (lastCommonLetter < wordLength? word.substring(lastCommonLetter): ZERO);
		String addition = (lastCommonLetter < producedWord.length()? producedWord.substring(lastCommonLetter): ZERO);
		final AffixEntry lastAppliedRule = inflection.getLastAppliedRule(type);
		if(lastAppliedRule != null)
			addition += lastAppliedRule.toString(strategy);
		final String condition = (lastCommonLetter < wordLength? removal: StringUtils.EMPTY);
		return new LineEntry(removal, addition, condition, word);
	}

	final List<LineEntry> reduceRules(final List<LineEntry> plainRules){
		return reduceRules(plainRules, null);
	}

	public final List<LineEntry> reduceRules(final List<LineEntry> plainRules, final ProgressCallback progressCallback){
		List<LineEntry> compactedRules = redistributeAdditions(plainRules);

		if(progressCallback != null)
			progressCallback.accept(20);

		compactedRules = compactRules(compactedRules);

		if(progressCallback != null)
			progressCallback.accept(40);

		//reshuffle originating list to place the correct inflections in the correct rule
		compactedRules = makeAdditionsDisjoint(compactedRules);

		if(progressCallback != null)
			progressCallback.accept(50);

		final IntObjectMap<Set<Character>> overallLastGroups = collectOverallLastGroups(plainRules);
disjoinConditions2(new ArrayList<>(compactedRules));
		compactedRules = disjoinConditions(compactedRules, overallLastGroups);

		if(progressCallback != null)
			progressCallback.accept(60);

		mergeSimilarRules(compactedRules);

		return compactedRules;
	}

	private List<LineEntry> redistributeAdditions(final List<LineEntry> plainRules){
		final Map<String, LineEntry> map = new HashMap<>(0);
		for(int i = 0; i < plainRules.size(); i ++)
			redistributeAddition(plainRules.get(i), map);
		return SetHelper.collect(map.values(),
			entry -> entry.removal + TAB + entry.condition + TAB + RegexHelper.sortAndMergeSet(entry.from, comparator),
			(rule, entry) -> rule.addition.addAll(entry.addition));
	}

	private static void redistributeAddition(final LineEntry entry, final Map<String, LineEntry> map){
		final String removalTab = entry.removal + TAB;
		final String tabCondition = TAB + entry.condition;
		for(final String addition : entry.addition){
			final String key = removalTab + addition + tabCondition;
			final LineEntry newEntry = new LineEntry(entry.removal, addition, entry.condition, entry.from);
			final LineEntry rule = map.putIfAbsent(key, newEntry);
			if(rule != null)
				rule.from.addAll(entry.from);
		}
	}

	private List<LineEntry> compactRules(final Collection<LineEntry> rules){
		//same removal, addition, and condition parts
		return SetHelper.collect(rules,
			entry -> entry.removal + TAB + RegexHelper.sortAndMergeSet(entry.addition, comparator) + TAB + entry.condition,
			(rule, entry) -> rule.from.addAll(entry.from));
	}

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
		for(int i = 0; i < rules.size(); i ++){
			final LineEntry rule = rules.get(i);
			temporaryRules.clear();

			final Map<String, List<String>> lcss = SetHelper.bucket(rule.addition,
				add -> StringHelper.longestCommonPrefix(add, rule.removal));
			if(lcss.size() > 1){
				//order keys from longer to shorter
				keys.clear();
				keys.addAll(lcss.keySet());
				keys.sort(Comparator.comparingInt(String::length).reversed());
				final List<String> additionsToBeRemoved = retrieveAdditionsToBeRemoved(rules, rule, temporaryRules, lcss, keys);

				for(int j = 0; j < temporaryRules.size(); j ++)
					insertRuleOrUpdateFrom(disjointedRules, temporaryRules.get(j));
				additionsToBeRemoved.forEach(rule.addition::remove);
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
		for(int i = 0; i < keys.size(); i ++){
			final String key = keys.get(i);
			final int keyLength = key.length();
			final int conditionLength = rule.condition.length() - keyLength;
			if(conditionLength <= 0)
				break;

			final String condition = rule.condition.substring(keyLength);
			final String removal = (conditionLength <= rule.removal.length()? condition: rule.removal);
			final List<String> list = lcss.get(key);
			final Set<String> addition = new HashSet<>(list.size());
			for(int j = 0; j < list.size(); j ++)
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
			for(int i = 0; i < expandedRules.size(); i ++){
				final LineEntry expandedRule = expandedRules.get(i);
				if(expandedRule.isContainedInto(rule)){
					rule.addition.removeAll(expandedRule.addition);
					expandedRule.from.addAll(rule.from);
				}
			}
			expandedRules.add(rule);
		}
	}

	private static IntObjectMap<Set<Character>> collectOverallLastGroups(final List<LineEntry> plainRules){
		final IntObjectMap<Set<Character>> overallLastGroups;
		if(!plainRules.isEmpty()){
			final Collection<String> overallFrom = new HashSet<>(plainRules.size());
			for(int i = 0; i < plainRules.size(); i ++)
				overallFrom.addAll(plainRules.get(i).from);

			int maxLength = -1;
			for(final String from : overallFrom)
				if(maxLength < 0 || from.length() < maxLength)
					maxLength = from.length();

			overallLastGroups = new IntObjectHashMap<>(maxLength);
			for(int index = 0; index < maxLength; index ++){
				final Set<Character> overallLastGroup = LineEntry.extractGroup(index, overallFrom);
				overallLastGroups.put(index, overallLastGroup);
			}
		}
		else
			overallLastGroups = new IntObjectHashMap<>(0);
		return overallLastGroups;
	}

	private List<LineEntry> disjoinConditions2(final List<LineEntry> rules){
		restart:
		//order by condition length
		rules.sort(Comparator.comparingInt(rule -> RegexHelper.conditionLength(rule.condition)));

		//extract branches whose conditions are disjoint, each branch contains all the rules that share the same ending condition (given by
		//the first item, the (limb) parent, so to say)
		final List<List<LineEntry>> branches = new ArrayList<>(rules.size());
		while(!rules.isEmpty()){
			final List<LineEntry> branch = extractBranch(rules);
			branches.add(branch);
		}

		//for each limb, level up the conditions so there is no intersection between the limb and the branches
		final StringBuilder condition = new StringBuilder();
		final Set<Character> newParentCondition = new HashSet<>(0);
		for(int i = 0; i < branches.size(); i ++){
			final List<LineEntry> branch = branches.get(i);
			final int branchSize = branch.size();
			for(int j = 0; j < branchSize; j ++){
				//extract limb
				LineEntry parent = branch.get(j);
				for(int k = j + 1; k < branchSize; k ++){
					//extract branch of limb
					final LineEntry child = branch.get(k);

					if(hasNonEmptyIntersection(parent, child)){
						//augment parent condition to avoid any intersection:

						final int parentConditionLength = RegexHelper.conditionLength(parent.condition);
						final Set<Character> parentGroup = parent.extractGroup(parentConditionLength);

						final Set<Character> childrenGroup = new HashSet<>(branchSize - k + 1);
						for(int m = k; m < branchSize; m ++)
							childrenGroup.addAll(branch.get(m).extractGroup(parentConditionLength));

						final Set<Character> intersection = SetHelper.intersection(parentGroup, childrenGroup);
						//if parent (with the augmented condition) and children has no intersection:
						if(intersection.isEmpty()){
							//augment parent condition
							augmentCondition(parent, parentConditionLength, parentGroup, childrenGroup);

							break;
						}


						//if parent and child has a non-empty intersection:

						//extract all the children whose condition matches the intersection
						final Collection<LineEntry> affectedChildren = new HashSet<>(branchSize - k + 1);
						for(int m = k; m < branchSize; m ++){
							final LineEntry affectedChild = branch.get(m);
							final Set<Character> childGroup = affectedChild.extractGroup(parentConditionLength);
							final Set<Character> childIntersection = SetHelper.intersection(intersection, childGroup);
							if(!childIntersection.isEmpty())
								affectedChildren.add(affectedChild);
						}

						final Set<Character> nextParentGroup = parent.extractGroup(parentConditionLength + 1);
						newParentCondition.clear();
						for(final LineEntry affectedChild : affectedChildren){
							final Set<Character> nextChildGroup = affectedChild.extractGroup(parentConditionLength + 1);
							final Set<Character> nextChildIntersection = SetHelper.intersection(nextParentGroup, nextChildGroup);
							if(nextChildIntersection.isEmpty()){
								if(!nextParentGroup.isEmpty()){
									//augment parent condition
									final boolean chooseRatifyingOverNegated = chooseRatifyingOverNegated(parentConditionLength, childrenGroup, intersection);
									final String augment = (chooseRatifyingOverNegated || !nextChildGroup.isEmpty()
										? RegexHelper.makeGroup(intersection, comparator)
										: RegexHelper.makeNotGroup(childrenGroup, comparator));
									condition.setLength(0);
									condition.append(RegexHelper.makeNotGroup(nextChildGroup, comparator))
										.append(augment)
										.append(parent.condition);
									Set<String> newParentFrom = parent.extractFromEndingWith(condition.toString());
									if(!newParentFrom.equals(parent.from) && !newParentFrom.isEmpty()){
										final LineEntry newRule = LineEntry.createFromWithWords(parent, condition.toString(), newParentFrom);
										parent.from.removeAll(newRule.from);
										branch.add(newRule);
									}
									else
										newParentCondition.addAll(nextChildGroup);

									condition.setLength(0);
									condition.append(RegexHelper.makeGroup(nextChildGroup, comparator))
										.append(RegexHelper.makeGroup(intersection, comparator))
										.append(parent.condition);
									newParentFrom = parent.extractFromEndingWith(condition.toString());
									if(!newParentFrom.equals(parent.from) && !newParentFrom.isEmpty()){
										final LineEntry newRule = LineEntry.createFrom(parent, condition.toString());
										parent.from.removeAll(newRule.from);
										branch.add(newRule);
									}
								}
								else{
									if(nextChildGroup.isEmpty())
										//TODO
										System.out.println();
									if(!newParentCondition.isEmpty())
										//TODO
										System.out.println();

									condition.setLength(0);
									condition.append(RegexHelper.makeNotGroup(nextChildGroup, comparator))
										.append(affectedChild.condition);
									final LineEntry newRule = LineEntry.createFrom(parent, condition.toString());
									if(!newRule.from.isEmpty()){
										parent.from.removeAll(newRule.from);
										branch.add(newRule);
									}

									//augment child condition
									condition.setLength(0);
									condition.append(RegexHelper.makeGroup(nextChildGroup, comparator))
										.append(affectedChild.condition);
									affectedChild.condition = condition.toString();

									condition.setLength(0);
									condition.append(RegexHelper.makeNotGroup(childrenGroup, comparator))
										.append(parent.condition);
									parent = LineEntry.createFrom(parent, condition.toString());
									branch.set(j, parent);

									continue restart;
								}
							}
							else if(!newParentCondition.isEmpty())
								//TODO
								System.out.println();
							else{
								//TODO
								System.out.println();
							}
						}

						//TODO what if !parentGroup.isEmpty() and !newParentCondition.isEmpty() are both true?

						parentGroup.removeAll(intersection);
						if(!parentGroup.isEmpty()){
							final boolean chooseRatifyingOverNegated = chooseRatifyingOverNegated(parentConditionLength, parentGroup, intersection);
							final String augment = (chooseRatifyingOverNegated
								? RegexHelper.makeGroup(parentGroup, comparator)
								: RegexHelper.makeNotGroup(intersection, comparator));
							parent = LineEntry.createFrom(parent, augment + parent.condition);
							branch.set(j, parent);

							goto restart;
						}
						else if(!newParentCondition.isEmpty()){
							condition.setLength(0);
							condition.append(RegexHelper.makeNotGroup(newParentCondition, comparator))
								.append(RegexHelper.makeGroup(intersection, comparator))
								.append(parent.condition);
							final Set<String> newParentFrom = parent.extractFromEndingWith(condition.toString());
							if(newParentFrom.equals(parent.from)){
								parent.condition = condition.toString();

								continue restart;
							}
							else if(!newParentFrom.isEmpty()){
								parent = LineEntry.createFromWithWords(parent, condition.toString(), newParentFrom);
								branch.set(j, parent);

								continue restart;
							}
						}
					}
				}
			}
		}

		return Collections.emptyList();
	}

	/**
	 * Extract all the rules that have the condition in common with the one given.
	 *
	 * @param rules   Collection from which to extract the branch, based on a parent rule (the first item, the limb), whose condition is
	 * 	used to extract all the branches that ends with the very same condition.
	 * @return	The list of branches. The first element being the limb.
	 */
	private static List<LineEntry> extractBranch(final List<LineEntry> rules){
		final LineEntry parent = rules.get(0);
		final List<LineEntry> branches = new ArrayList<>(rules.size());
		final Iterator<LineEntry> itr = rules.iterator();
		while(itr.hasNext()){
			final LineEntry rule = itr.next();
			if(rule.condition.endsWith(parent.condition)){
				branches.add(rule);
				itr.remove();
			}
		}
		return branches;
	}

	private static boolean hasNonEmptyIntersection(final LineEntry rule1, final LineEntry rule2){
		if(RegexHelper.conditionLength(rule1.condition) < RegexHelper.conditionLength(rule2.condition)){
			//FIXME select ^ or $ based on rule type
			final Pattern pattern1 = RegexHelper.pattern(rule1.condition + "$");
			for(final String f : rule2.from)
				if(RegexHelper.find(f, pattern1))
					return true;
		}
		return false;
	}

	private void augmentCondition(final LineEntry rule, final int parentConditionLength, final Collection<Character> parentGroup,
			final Collection<Character> childGroup){
		final boolean chooseRatifyingOverNegated = chooseRatifyingOverNegated(parentConditionLength, parentGroup, childGroup);
		final String augment = (chooseRatifyingOverNegated
			? RegexHelper.makeGroup(parentGroup, comparator)
			: RegexHelper.makeNotGroup(childGroup, comparator));
		rule.condition = augment + rule.condition;
	}



	private List<LineEntry> disjoinConditions(final List<LineEntry> rules, final IntObjectMap<Set<Character>> overallLastGroups){
		//expand same conditions (if any); store surely disjoint rules
		final List<LineEntry> nonOverlappingRules = disjoinSameConditions(rules, overallLastGroups);

		//expand same ending conditions (if any); store surely disjoint rules
		nonOverlappingRules.addAll(disjoinSameEndingConditions(rules, overallLastGroups));

		return nonOverlappingRules;
	}

	private List<LineEntry> disjoinSameConditions(final Collection<LineEntry> rules, final IntObjectMap<Set<Character>> overallLastGroups){
		final List<LineEntry> finalRules = new ArrayList<>(0);

		//bucket by same condition
		final Map<String, List<LineEntry>> conditionBucket = SetHelper.bucket(rules, rule -> rule.condition);
		for(final Map.Entry<String, List<LineEntry>> entry : conditionBucket.entrySet()){
			final String condition = entry.getKey();
			final List<LineEntry> sameCondition = entry.getValue();

			//remove empty condition and multiple rules with the same condition
			if(condition.isEmpty() || sameCondition.size() > 1)
				disjoinSameConditions(rules, overallLastGroups, condition, sameCondition, finalRules);
		}

		return finalRules;
	}

	private void disjoinSameConditions(final Collection<LineEntry> rules, final IntObjectMap<Set<Character>> overallLastGroups,
			final String condition, final List<LineEntry> sameCondition, final List<LineEntry> finalRules){
		//extract children
		final List<LineEntry> children;
		if(condition.isEmpty())
			children = new ArrayList<>(rules);
		else{
			children = new ArrayList<>(rules.size());
			for(final LineEntry rule : rules)
				if(rule.condition.endsWith(condition))
					children.add(rule);
		}

		final Map<LineEntry, Set<Character>> groups = new HashMap<>(children.size());
		for(int i = 0; i < children.size(); i ++){
			final LineEntry child = children.get(i);
			if(groups.put(child, child.extractGroup(condition.length())) != null)
				throw new IllegalStateException("Duplicate key");
		}

		rules.removeAll(sameCondition);

		//separate conditions:
		final Set<Character> childrenGroup = new HashSet<>(children.size());
		final Collection<Character> notPresentConditions = new HashSet<>(0);
		final StringBuilder newCondition = new StringBuilder();
		//for each rule with same condition
		for(int i = 0; i < sameCondition.size(); i ++){
			final LineEntry parent = sameCondition.get(i);
			//extract ratifying group
			final Set<Character> parentGroup = new HashSet<>(groups.get(parent));

			//extract negated group
			final List<LineEntry> childrenWithoutParent = new ArrayList<>(children);
			childrenWithoutParent.remove(parent);
			childrenGroup.clear();
			for(int j = 0; j < childrenWithoutParent.size(); j ++)
				childrenGroup.addAll(groups.get(childrenWithoutParent.get(j)));

			final Set<Character> groupsIntersection = SetHelper.intersection(parentGroup, childrenGroup);
			parentGroup.removeAll(groupsIntersection);
			if(!parentGroup.isEmpty()){
				final String newCond = chooseCondition(parent.condition, parentGroup, childrenGroup, groupsIntersection, overallLastGroups);
				final LineEntry newRule = LineEntry.createFrom(parent, newCond);
				finalRules.add(newRule);
			}

			//extract and add new condition only if there are rules that ends with the new condition
			notPresentConditions.clear();
			final Iterator<Character> itr = groupsIntersection.iterator();
			while(itr.hasNext()){
				final Character chr = itr.next();

				final String cond = chr + parent.condition;
				final Set<String> newEntryFromChildrenNotParentFrom = createFromWithRules(cond, childrenWithoutParent);
				final Set<String> newEntryFromParentFrom = parent.extractFromEndingWith(cond);
				if(newEntryFromChildrenNotParentFrom.equals(newEntryFromParentFrom)){
					notPresentConditions.add(chr);

					itr.remove();
				}
			}

			final Set<Character> characters = overallLastGroups.get(parent.condition.length());
			if(!notPresentConditions.isEmpty() && characters != null){
				final String negativeCondition = RegexHelper.makeNotGroup(notPresentConditions, comparator) + parent.condition;
				final Collection<Character> overallLastGroup = new HashSet<>(characters);
				overallLastGroup.removeAll(notPresentConditions);
				String positiveCondition = RegexHelper.makeGroup(overallLastGroup, comparator) + parent.condition;

				LineEntry notRule = null;
				final int finalRulesCount = finalRules.size();
				for(int j = 0; j < finalRulesCount; j ++){
					final LineEntry rule = finalRules.get(j);
					if(rule.condition.equals(negativeCondition) || rule.condition.equals(positiveCondition)){
						notRule = rule;
						break;
					}
				}

				if(notRule == null || notRule.condition.charAt(0) == '['){
					//find already present rule
					for(int j = 0; j < finalRulesCount; j ++){
						final LineEntry fr = finalRules.get(j);
						if(fr.removal.equals(parent.removal) && fr.addition.equals(parent.addition) && fr.condition.endsWith(parent.condition)){
							//rule already present, remove from final rules' list
							finalRules.remove(fr);

							notPresentConditions.addAll(parentGroup);
							//keep the two sets disjoint
							overallLastGroup.removeAll(parentGroup);
						}
					}

					//if intersection between `notPresentConditions` and `overallLastGroup` is not empty
					newCondition.setLength(0);
					if(notPresentConditions.size() < overallLastGroup.size())
						newCondition.append(RegexHelper.makeGroup(notPresentConditions, comparator));
					else{
						final String subCondition = RegexHelper.makeNotGroup(overallLastGroup, comparator);
						if(parent.condition.isEmpty() || !subCondition.isEmpty() && subCondition.charAt(0) != '.')
							newCondition.append(subCondition);
					}
					newCondition.append(parent.condition);
					if(!newCondition.isEmpty()){
						final LineEntry newEntry = LineEntry.createFrom(parent, newCondition.toString());
						finalRules.add(newEntry);
					}
				}
				else{
					final Collection<Character> negatedConditions = new HashSet<>(notRule.condition.length());
					for(int k = 0; k < notRule.condition.length(); k ++)
						negatedConditions.add(notRule.condition.charAt(k));
					final LineEntry newEntry = LineEntry.createFrom(parent, RegexHelper.makeNotGroup(negatedConditions, comparator));
					//if rem/add are same, and cond is RegexHelper.makeGroup(negatedConditions, comparator)
					//then add from to the found rule
					boolean found = false;
					final String notNegatedCondition = RegexHelper.makeGroup(negatedConditions, comparator);
					for(int j = 0; j < finalRulesCount; j ++){
						final LineEntry fr = finalRules.get(j);
						if(fr.removal.equals(parent.removal) && fr.addition.equals(parent.addition)
								&& fr.condition.startsWith(notNegatedCondition)){
							fr.from.addAll(parent.from);
							fr.condition = (fr.condition.length() > notNegatedCondition.length()
								? fr.condition.substring(notNegatedCondition.length())
								: DOT);
							found = true;
							break;
						}
					}
					if(!found)
						finalRules.add(newEntry);
				}
			}
			for(final Character chr : groupsIntersection)
				rules.add(LineEntry.createFrom(parent, chr + parent.condition));
		}
	}

	private static Set<String> createFromWithRules(final String condition, final List<LineEntry> parentRulesFrom){
		final Pattern conditionPattern = RegexHelper.pattern(condition + LineEntry.PATTERN_END_OF_WORD);
		final Set<String> words = new HashSet<>(parentRulesFrom.size());
		for(int i = 0; i < parentRulesFrom.size(); i ++)
			words.addAll(parentRulesFrom.get(i).extractFromEndingWith(conditionPattern));
		return words;
	}

	private String chooseCondition(final String parentCondition, final Set<Character> parentGroup,
			final Set<Character> childrenGroup, final Collection<Character> groupsIntersection,
			final IntObjectMap<Set<Character>> overallLastGroups){
		final int parentConditionLength = parentCondition.length();
		final boolean chooseRatifyingOverNegated = chooseRatifyingOverNegated(parentConditionLength, parentGroup, childrenGroup,
			groupsIntersection);
		final Set<Character> overallLastGroup = overallLastGroups.get(parentConditionLength);
		final Set<Character> baseGroup = (chooseRatifyingOverNegated? parentGroup: childrenGroup);
		final BiFunction<Set<Character>, Comparator<String>, String> combineRatifying = (chooseRatifyingOverNegated
			? RegexHelper::makeGroup
			: RegexHelper::makeNotGroup);
		final BiFunction<Set<Character>, Comparator<String>, String> combineNegated = (chooseRatifyingOverNegated
			? RegexHelper::makeNotGroup
			: RegexHelper::makeGroup);

		final String preCondition;
		if(overallLastGroup != null){
			final Set<Character> group = SetHelper.difference(overallLastGroup, baseGroup);
			if(baseGroup.size() == overallLastGroup.size())
				preCondition = (parentConditionLength == 0? StringUtils.EMPTY: DOT);
			else
				preCondition = (baseGroup.size() <= group.size()
					? combineRatifying.apply(baseGroup, comparator)
					: combineNegated.apply(group, comparator));
		}
		else
			preCondition = combineRatifying.apply(baseGroup, comparator);
		return preCondition + parentCondition;
	}

	private static boolean chooseRatifyingOverNegated(final int parentConditionLength, final Collection<Character> parentGroup,
			final Collection<Character> childrenGroup, final Collection<Character> intersectionGroup){
		final int parentGroupSize = parentGroup.size();
		final int childrenGroupSize = childrenGroup.size();
		final boolean chooseRatifyingOverNegated = ((parentConditionLength == 0 || intersectionGroup.isEmpty())
			&& parentGroupSize <= childrenGroupSize);
		return ((chooseRatifyingOverNegated || childrenGroupSize == 0) && parentGroupSize > 0);
	}

	private List<LineEntry> disjoinSameEndingConditions(final List<LineEntry> rules, final IntObjectMap<Set<Character>> overallLastGroups){
		//bucket by condition ending
		final List<List<LineEntry>> forest = bucketByConditionEnding(rules);

		final ArrayList<LineEntry> finalRules = new ArrayList<>(forest.size());
		//for each bush in the forest
		for(int i = 0; i < forest.size(); i ++){
			final List<LineEntry> bush = forest.get(i);
			//if there is only one rule, then it goes in the final set
			if(bush.size() == 1)
				finalRules.add(bush.get(0));
			//otherwise, process the rules
			else
				finalRules.addAll(disjoinSameEndingConditionsBush(bush, overallLastGroups));
		}
		return finalRules;
	}

	private List<LineEntry> disjoinSameEndingConditionsBush(final Collection<LineEntry> bush,
			final IntObjectMap<Set<Character>> overallLastGroups){
		final List<LineEntry> finalRules = new ArrayList<>(0);

		final Queue<LineEntry> queue = new PriorityQueue<>(shortestConditionComparator);
		queue.addAll(bush);
		final Set<Character> childrenGroup = new HashSet<>(0);
		final StringBuilder preCondition = new StringBuilder();
		while(!queue.isEmpty()){
			final LineEntry parent = queue.remove();

			final List<LineEntry> bubbles = extractBubbles(queue, parent);
			if(bubbles.isEmpty()){
				//no other rules exist that end with `parent.condition`, therefore add it to the final rules
				finalRules.add(parent);
				continue;
			}

			//extract (parent) ratifying group
			final int parentConditionLength = parent.condition.length();
			final Set<Character> parentGroup = parent.extractGroup(parentConditionLength);

			//extract (bubbles) negated group
			childrenGroup.clear();
			for(int i = 0; i < bubbles.size(); i ++){
				final LineEntry child = bubbles.get(i);
				childrenGroup.add(child.condition.charAt(child.condition.length() - parentConditionLength - 1));
			}

			//if intersection(parent-group, children-group) is empty
			final Set<Character> groupsIntersection = SetHelper.intersection(parentGroup, childrenGroup);
			parentGroup.removeAll(groupsIntersection);

			//calculate new condition
			final boolean chooseRatifyingOverNegated = chooseRatifyingOverNegated(parentConditionLength, parentGroup,
				childrenGroup);
			preCondition.setLength(0);
			preCondition.append(chooseRatifyingOverNegated
					? RegexHelper.makeGroup(parentGroup, comparator)
					: RegexHelper.makeNotGroup(childrenGroup, comparator))
				.append(parent.condition);
			LineEntry newEntry = LineEntry.createFrom(parent, preCondition.toString());

			LineEntry newParentEntry = LineEntry.createFrom(parent, preCondition.toString());
			LineEntry newNotParentEntry = LineEntry.createFrom(parent,
				(chooseRatifyingOverNegated
					? RegexHelper.makeNotGroup(childrenGroup, comparator)
					: RegexHelper.makeGroup(childrenGroup, comparator)) + parent.condition);
			LineEntry newIntersectionEntry = LineEntry.createFrom(parent, RegexHelper.makeGroup(groupsIntersection, comparator) + parent.condition);

			//keep only rules that matches some existent words
			if(newEntry.isProductive())
				finalRules.add(newEntry);
			else if(!growNewBush(queue, parent))
				continue;
			else
				LOGGER.debug("skip unused rule: {} {} {}", newEntry.removal, StringUtils.join(newEntry.addition, PIPE),
					(newEntry.condition.isEmpty()? DOT: newEntry.condition));

			LineEntry maxConditionEntry = null;
			int maxConditionLength = 0;
			for(int i = 0; i < bubbles.size(); i ++){
				final LineEntry child = bubbles.get(i);
				if(maxConditionEntry == null || child.condition.length() > maxConditionLength){
					maxConditionEntry = child;
					maxConditionLength = child.condition.length();
				}
			}

			if(parentConditionLength + 1 >= maxConditionLength){
				queue.removeAll(bubbles);

				//TODO what should be done?
//				//add the remaining part of the parent
//				final StringBuilder condition = new StringBuilder(parent.condition.length() + 4);
//				condition.append(parent.condition);
//				if(chooseRatifyingOverNegated){
//					condition.insert(0, "[^ ]");
//					for(final Character chr : childrenGroup){
//						condition.replace(2, 3, String.valueOf(chr));
//						addWordsToBubble(bubbles, parent, condition);
//					}
//				}
//				else{
//					condition.insert(0, ' ');
//					for(final Character chr : childrenGroup){
//						condition.replace(0, 1, String.valueOf(chr));
//						addWordsToBubble(bubbles, parent, condition);
//					}
//				}

				finalRules.addAll(bubbles);
			}
			else if(matchesAll(queue, parentConditionLength + 1))
				for(final Character chr : childrenGroup){
					final LineEntry entry = LineEntry.createFrom(parent, chr + parent.condition);
					if(!queue.contains(entry))
						queue.add(entry);
				}
			else if(!groupsIntersection.isEmpty() && !parentGroup.isEmpty())
				//expand intersection
				for(final Character chr : groupsIntersection){
					newEntry = LineEntry.createFrom(parent, chr + parent.condition);
					queue.add(newEntry);

					finalRules.addAll(disjoinSameConditions(queue, overallLastGroups));
				}

			//continue until bubbles.condition length is reached
		}

		return finalRules;
	}

	private void addWordsToBubble(final List<LineEntry> bubbles, final LineEntry parent, final StringBuilder condition){
		final String subCondition = condition.toString();
		final Set<String> words = parent.extractFromEndingWith(subCondition);
		for(final LineEntry bubble : bubbles)
			if(bubble.condition.equals(subCondition)){
				bubble.from.addAll(words);
				break;
			}
	}

	private static boolean matchesAll(final Iterable<LineEntry> queue, final int maxLength){
		for(final LineEntry rule : queue)
			if(rule.condition.length() <= maxLength)
				return false;
		return true;
	}

	/**
	 * @param queue	The queue to add the new rules to
	 * @param parent	The parent rule
	 * @return	{@code true} if a bush was created and added to the queue
	 */
	private static boolean growNewBush(final Collection<LineEntry> queue, final LineEntry parent){
		final int parentConditionLength = parent.condition.length();

		final ArrayList<LineEntry> newBushes = new ArrayList<>(0);
		final Iterator<LineEntry> itr = queue.iterator();
		while(itr.hasNext()){
			final LineEntry child = itr.next();

			if(parent.from.containsAll(child.from)){
				final Set<Character> childGroup = child.extractGroup(parentConditionLength + 1);
				newBushes.ensureCapacity(newBushes.size() + childGroup.size());
				for(final Character chr : childGroup)
					newBushes.add(LineEntry.createFrom(child, chr + child.condition));

				itr.remove();
			}
		}
		final boolean bushAdded = !newBushes.isEmpty();
		if(bushAdded){
			queue.addAll(newBushes);
			queue.add(parent);
		}
		return bushAdded;
	}

	private List<List<LineEntry>> bucketByConditionEnding(final List<LineEntry> rules){
		rules.sort(shortestConditionComparator);

		final List<List<LineEntry>> forest = new ArrayList<>(rules.size());
		while(!rules.isEmpty()){
			//extract base condition
			final String parentCondition = rules.get(0).condition;

			//extract similar (same ending condition) rules from processing-list
			final List<LineEntry> children = extractSimilarRules(rules, parentCondition);
			forest.add(children);
		}
		return forest;
	}

	private static List<LineEntry> extractSimilarRules(final Collection<LineEntry> rules, final String parentCondition){
		final List<LineEntry> children = new ArrayList<>(rules.size());
		final Iterator<LineEntry> itr = rules.iterator();
		while(itr.hasNext()){
			final LineEntry rule = itr.next();

			if(rule.condition.endsWith(parentCondition)){
				children.add(rule);

				itr.remove();
			}
		}
		return children;
	}

	/**
	 * Extract all the rules that have the condition in common with the one given.
	 *
	 * @param bush	Collection from which to extract the bubbles.
	 * @param parent	The parent rule whose condition is used to extract the bubbles that ends with the very same condition.
	 * @return	The list of bubbles.
	 */
	private static List<LineEntry> extractBubbles(final Collection<LineEntry> bush, final LineEntry parent){
		final int parentConditionLength = parent.condition.length();
		final List<LineEntry> bubbles = new ArrayList<>(bush.size());
		for(final LineEntry child : bush)
			if(child.condition.length() > parentConditionLength && child.condition.endsWith(parent.condition))
				bubbles.add(child);

		//if the words that generates a bubble is fully contained within the parent, then remove the bubble
		final Iterator<LineEntry> itr = bubbles.iterator();
		while(itr.hasNext()){
			final LineEntry bubble = itr.next();
			if(parent.from.containsAll(bubble.from)
					&& bubble.from.equals(new HashSet<>(parent.extractFromEndingWith(bubble.condition))))
				itr.remove();
		}

		return bubbles;
	}

	private static boolean chooseRatifyingOverNegated(final int parentConditionLength, final Collection<Character> parentGroup,
			final Collection<Character> childrenGroup){
		final int parentGroupSize = parentGroup.size();
		final int childrenGroupSize = childrenGroup.size();
		final boolean chooseRatifyingOverNegated = (parentConditionLength == 0 || parentGroupSize <= 2 && childrenGroupSize > 1);
		return ((chooseRatifyingOverNegated || childrenGroupSize == 0) && parentGroupSize > 0);
	}

	/** Merge common conditions (ex. `[^a]bc` and `[^a]dc` will become `[^a][bd]c`). */
	private void mergeSimilarRules(final Collection<LineEntry> entries){
		final Map<String, List<LineEntry>> similarityBucket = SetHelper.bucket(entries,
			entry -> (entry.condition.contains(RegexHelper.GROUP_END)
				? entry.removal + TAB + entry.addition + TAB + RegexSequencer.splitSequence(entry.condition)[0] + TAB
					+ RegexSequencer.splitSequence(entry.condition).length
				: null));
		final Collection<Character> group = new HashSet<>(0);
		final StringBuilder condition = new StringBuilder();
		for(final List<LineEntry> similarities : similarityBucket.values())
			if(similarities.size() > 1){
				final LineEntry anEntry = similarities.iterator().next();
				final String[] aCondition = RegexSequencer.splitSequence(anEntry.condition);
				final String[] commonPreCondition = RegexSequencer.subSequence(aCondition, 0, 1);
				final String[] commonPostCondition = RegexSequencer.subSequence(aCondition, 2);
				//extract all the rules from `similarities` that has the condition compatible with firstEntry.condition
				group.clear();
				for(int i = 0; i < similarities.size(); i ++)
					group.add(RegexSequencer.splitSequence(similarities.get(i).condition)[1].charAt(0));

				condition.setLength(0);
				for(final String cpc : commonPreCondition)
					condition.append(cpc);
				condition.append(RegexHelper.makeGroup(group, comparator));
				for(final String cpc : commonPostCondition)
					condition.append(cpc);
				condition.toString();

				entries.add(LineEntry.createFrom(anEntry, condition.toString()));

				for(int i = 0; i < similarities.size(); i ++)
					entries.remove(similarities.get(i));
			}
	}

	public final List<String> convertFormat(final String flag, final boolean keepLongestCommonAffix, final List<LineEntry> compactedRules){
		final RuleEntry ruleToBeReduced = affixData.getData(flag);
		if(ruleToBeReduced == null)
			throw new LinterException(NON_EXISTENT_RULE, flag);

		final AffixType type = ruleToBeReduced.getType();
		final List<String> prettyPrintRules = convertEntriesToRules(flag, type, keepLongestCommonAffix, compactedRules);
		prettyPrintRules.add(0, LineEntry.toHunspellHeader(type, flag, ruleToBeReduced.combinableChar(), prettyPrintRules.size()));
		return prettyPrintRules;
	}

	private List<String> convertEntriesToRules(final String flag, final AffixType type, final boolean keepLongestCommonAffix,
			final List<LineEntry> entries){
		//restore original rules
		final ArrayList<LineEntry> restoredRules = new ArrayList<>(0);
		if(entries != null)
			for(int i = 0; i < entries.size(); i ++){
				final LineEntry rule = entries.get(i);
				restoredRules.ensureCapacity(rule.addition.size());
				for(final String addition : rule.addition){
					final int lcp = StringHelper.longestCommonPrefix(rule.removal, addition).length();
					final String removal = rule.removal.substring(lcp);
					final LineEntry entry = new LineEntry((removal.isEmpty()? ZERO: removal), addition.substring(lcp),
						rule.condition, rule.from);
					restoredRules.add(type == AffixType.SUFFIX? entry: entry.createReverse());
				}
			}
		final List<LineEntry> sortedEntries = prepareRules(keepLongestCommonAffix, restoredRules);

		return composeAffixRules(flag, type, sortedEntries);
	}

	private List<LineEntry> prepareRules(final boolean keepLongestCommonAffix, final List<LineEntry> entries){
		if(keepLongestCommonAffix && entries != null)
			for(int i = 0; i < entries.size(); i ++)
				entries.get(i).expandConditionToMaxLength(comparator);

		final List<LineEntry> list = (entries != null? new ArrayList<>(entries): new ArrayList<>(0));
		list.sort(lineEntryComparator);
		return list;
	}

	private static List<String> composeAffixRules(final String flag, final AffixType type, final List<LineEntry> entries){
		final List<String> list = new ArrayList<>(entries.size());
		for(int i = 0; i < entries.size(); i ++)
			list.add(entries.get(i).toHunspellRule(type, flag));
		return list;
	}

	final void checkReductionCorrectness(final String flag, final List<String> reducedRules, final List<String> originalLines){
		checkReductionCorrectness(flag, reducedRules, originalLines, null);
	}

	public final void checkReductionCorrectness(final String flag, final List<String> reducedRules, final List<String> originalLines,
			final ProgressCallback progressCallback){
		final RuleEntry ruleToBeReduced = affixData.getData(flag);
		if(ruleToBeReduced == null)
			throw new LinterException(NON_EXISTENT_RULE, flag);

		final AffixType type = ruleToBeReduced.getType();

		//read the header
		final RuleEntry overriddenParent = new RuleEntry(type, flag, ruleToBeReduced.combinableChar());
		//extract rules (skip the header)
		final List<AffixEntry> entries = new ArrayList<>(reducedRules.size() - 1);
		for(int i = 1; i < reducedRules.size(); i ++){
			final String reducedRule = reducedRules.get(i);
			final AffixEntry entry = new AffixEntry(reducedRule, i - 1, type, flag, strategy, null, null)
				.setParent(overriddenParent);
			entries.add(entry);
		}
		overriddenParent.setEntries(entries);

		int progress = 0;
		int progressIndex = 0;
		final int progressStep = (int)Math.ceil(originalLines.size() / 100.f);
		for(int i = 0; i < originalLines.size(); i ++){
			final String line = originalLines.get(i);
			final DictionaryEntry dicEntry = dictionaryEntryFactory.createFromDictionaryLine(line);
			final List<Inflection> originalInflections = wordGenerator.applyAffixRules(dicEntry);
			final List<Inflection> inflections = wordGenerator.applyAffixRules(dicEntry, overriddenParent);

			final Collection<DictionaryEntry> originalInflectionsWhole = new HashSet<>(originalInflections.size());
			for(int j = 0; j < originalInflections.size(); j ++)
				originalInflectionsWhole.add(new DictionaryEntry(originalInflections.get(j)));
			final Collection<DictionaryEntry> inflectionsWhole = new HashSet<>(inflections.size());
			for(int j = 0; j < inflections.size(); j ++)
				inflectionsWhole.add(new DictionaryEntry(inflections.get(j)));
			if(!originalInflectionsWhole.equals(inflectionsWhole))
				throw new LinterException(VERY_BAD_ERROR, line, originalInflectionsWhole, inflectionsWhole);

			if(progressCallback != null && ++ progress % progressStep == 0)
				progressCallback.accept(++ progressIndex);
		}
	}

	public final LineEntry collectInflectionsByFlag(final List<Inflection> inflections, final String flag, final AffixType type){
		//collect all inflections that generates from the given flag
		if(inflections.isEmpty())
			return null;

		final List<LineEntry> filteredRules = new ArrayList<>(inflections.size() - 1);
		//skip base inflection
		for(int i = WordGenerator.BASE_INFLECTION_INDEX + 1; i < inflections.size(); i ++){
			final Inflection inflection = inflections.get(i);
			final AffixEntry lastAppliedRule = inflection.getLastAppliedRule(type);
			if(lastAppliedRule != null && lastAppliedRule.getFlag().equals(flag)){
				final String word = lastAppliedRule.undoRule(inflection.getWord());
				final LineEntry newEntry = createAffixEntry(inflection, word, type);
				filteredRules.add(newEntry);
			}
		}
		return compactInflections(filteredRules);
	}

	private static LineEntry compactInflections(final List<LineEntry> rules){
		if(rules.size() > 1){
			//retrieve rule with the longest condition (all the other conditions must be this long)
			LineEntry maxConditionEntry = null;
			int maxConditionLength = 0;
			for(final LineEntry elem : rules)
				if(maxConditionEntry == null || elem.condition.length() > maxConditionLength){
					maxConditionEntry = elem;
					maxConditionLength = elem.condition.length();
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
		for(int i = 0; i < rules.size(); i ++){
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
