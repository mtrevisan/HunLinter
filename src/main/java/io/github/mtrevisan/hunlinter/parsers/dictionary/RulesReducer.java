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

import io.github.mtrevisan.hunlinter.datastructures.SetHelper;
import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.services.RegexSequencer;
import io.github.mtrevisan.hunlinter.services.system.LoopHelper;
import io.github.mtrevisan.hunlinter.services.text.StringHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.mtrevisan.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixType;
import io.github.mtrevisan.hunlinter.parsers.vos.AffixEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.parsers.vos.RuleEntry;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
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
import java.util.function.Consumer;

import static io.github.mtrevisan.hunlinter.services.system.LoopHelper.applyIf;
import static io.github.mtrevisan.hunlinter.services.system.LoopHelper.forEach;
import static io.github.mtrevisan.hunlinter.services.system.LoopHelper.match;


public class RulesReducer{

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesReducer.class);

	private static final MessageFormat NON_EXISTENT_RULE = new MessageFormat("Non-existent rule `{0}`, cannot reduce");
	private static final MessageFormat VERY_BAD_ERROR = new MessageFormat("Something very bad occurs while producing from `{0}`, expected {1}, obtained {1}");

	private static final String PIPE = "|";

	private static final String TAB = "\t";
	private static final String ZERO = "0";
	private static final String DOT = ".";


	private final Comparator<LineEntry> shortestConditionComparator = Comparator.comparingInt(entry -> entry.condition.length());

	private final AffixData affixData;
	private final FlagParsingStrategy strategy;
	private final WordGenerator wordGenerator;
	private final Comparator<String> comparator;
	private final Comparator<LineEntry> lineEntryComparator;


	public RulesReducer(final AffixData affixData, final WordGenerator wordGenerator){
		Objects.requireNonNull(affixData, "Affix data cannot be null");
		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");

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


	public List<LineEntry> collectInflectionsByFlag(Inflection[] inflections, final String flag, final AffixType type){
		if(inflections.length > 0)
			//remove base inflection
			inflections = ArrayUtils.remove(inflections, WordGenerator.BASE_INFLECTION_INDEX);
		//collect all inflections that generates from the given flag
		final List<LineEntry> filteredRules = new ArrayList<>(inflections.length);
		for(final Inflection inflection : inflections){
			final AffixEntry lastAppliedRule = inflection.getLastAppliedRule(type);
			if(lastAppliedRule != null && lastAppliedRule.getFlag().equals(flag)){
				final String word = lastAppliedRule.undoRule(inflection.getWord());
				final LineEntry newEntry = createAffixEntry(inflection, word, type);
				filteredRules.add(newEntry);
			}
		}
		return compactInflections(filteredRules);
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

	private List<LineEntry> compactInflections(final List<LineEntry> rules){
		final ArrayList<LineEntry> compactedRules = new ArrayList<>(rules.size());
		if(rules.size() > 1){
			//retrieve rule with longest condition (all the other conditions must be this long)
			final LineEntry compactedRule = LoopHelper.max(rules, Comparator.comparingInt(rule -> rule.condition.length()));
			expandAddition(rules, compactedRule);

			compactedRules.add(compactedRule);
			compactedRules.trimToSize();
		}
		else
			compactedRules.addAll(rules);
		return compactedRules;
	}

	private void expandAddition(final List<LineEntry> rules, final LineEntry compactedRule){
		final String from = rules.get(0).from.iterator().next();
		final int longestConditionLength = compactedRule.condition.length();
		for(final LineEntry rule : rules){
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

	List<LineEntry> reduceRules(final Collection<LineEntry> plainRules){
		return reduceRules(plainRules, null);
	}

	public List<LineEntry> reduceRules(final Collection<LineEntry> plainRules, final Consumer<Integer> progressCallback){
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

		final Map<Integer, Set<Character>> overallLastGroups = collectOverallLastGroups(plainRules);
		compactedRules = disjoinConditions(compactedRules, overallLastGroups);

		if(progressCallback != null)
			progressCallback.accept(60);

		mergeSimilarRules(compactedRules);

		return compactedRules;
	}

	private List<LineEntry> redistributeAdditions(final Iterable<LineEntry> plainRules){
		final Map<String, LineEntry> map = new HashMap<>();
		LoopHelper.forEach(plainRules, entry -> redistributeAddition(entry, map));
		return SetHelper.collect(map.values(),
			entry -> entry.removal + TAB + entry.condition + TAB + RegexHelper.mergeSet(entry.from, comparator),
			(rule, entry) -> rule.addition.addAll(entry.addition));
	}

	private void redistributeAddition(final LineEntry entry, final Map<String, LineEntry> map){
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

	private List<LineEntry> compactRules(final Iterable<LineEntry> rules){
		//same removal, addition, and condition parts
		return SetHelper.collect(rules,
			entry -> entry.removal + TAB + RegexHelper.mergeSet(entry.addition, comparator) + TAB + entry.condition,
			(rule, entry) -> rule.from.addAll(entry.from));
	}

	private List<LineEntry> makeAdditionsDisjoint(final Collection<LineEntry> rules){
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
		final List<LineEntry> disjointedRules = new ArrayList<>();

		final Consumer<LineEntry> lineEntryConsumer = temporaryRule -> insertRuleOrUpdateFrom(disjointedRules, temporaryRule);
		final Collection<LineEntry> temporaryRules = new ArrayList<>();
		final List<String> keys = new ArrayList<>(0);
		for(final LineEntry rule : rules){
			temporaryRules.clear();

			final Map<String, List<String>> lcss = SetHelper.bucket(rule.addition,
				add -> StringHelper.longestCommonPrefix(add, rule.removal));
			if(lcss.size() > 1){
				//order keys from longer to shorter
				keys.clear();
				keys.addAll(lcss.keySet());
				keys.sort(Comparator.comparingInt(String::length).reversed());
				final List<String> additionsToBeRemoved = retrieveAdditionsToBeRemoved(rules, rule, temporaryRules, lcss, keys);

				LoopHelper.forEach(temporaryRules, lineEntryConsumer);
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
	private List<String> retrieveAdditionsToBeRemoved(final Collection<LineEntry> rules, final LineEntry rule,
			final Collection<LineEntry> temporaryRules, final Map<String, List<String>> lcss, final Iterable<String> keys){
		final List<String> additionsToBeRemoved = new ArrayList<>();
		for(final String key : keys){
			final int keyLength = key.length();
			final int conditionLength = rule.condition.length() - keyLength;
			if(conditionLength <= 0)
				break;

			final String condition = rule.condition.substring(keyLength);
			final String removal = (conditionLength <= rule.removal.length()? condition: rule.removal);
			final List<String> list = lcss.get(key);
			final Set<String> addition = new HashSet<>(list.size());
			for(final String add : list)
				addition.add(add.substring(keyLength));
			final LineEntry newEntry = new LineEntry(removal, addition, condition, rule.from);
			if(rules.contains(newEntry)){
				temporaryRules.add(newEntry);

				additionsToBeRemoved.addAll(list);
			}
		}
		return additionsToBeRemoved;
	}

	private void insertRuleOrUpdateFrom(final List<LineEntry> expandedRules, final LineEntry rule){
		final int ruleIndex = expandedRules.indexOf(rule);
		if(ruleIndex >= 0)
			expandedRules.get(ruleIndex).from.addAll(rule.from);
		else{
			LoopHelper.applyIf(expandedRules,
				expandedRule -> expandedRule.isContainedInto(rule),
				expandedRule -> {
					rule.addition.removeAll(expandedRule.addition);
					expandedRule.from.addAll(rule.from);
				});
			expandedRules.add(rule);
		}
	}

	private Map<Integer, Set<Character>> collectOverallLastGroups(final Collection<LineEntry> plainRules){
		final Map<Integer, Set<Character>> overallLastGroups = new HashMap<>();
		if(!plainRules.isEmpty()){
			try{
				final Set<String> overallFrom = new HashSet<>();
				final Consumer<String> add = overallFrom::add;
				for(final LineEntry entry : plainRules)
					LoopHelper.forEach(entry.from, add);
				for(int index = 0; ; index ++){
					final Set<Character> overallLastGroup = LineEntry.extractGroup(index, overallFrom);
					overallLastGroups.put(index, overallLastGroup);
				}
			}
			catch(final Exception ignored){}
		}
		return overallLastGroups;
	}

	private List<LineEntry> disjoinConditions(final List<LineEntry> rules, final Map<Integer, Set<Character>> overallLastGroups){
		//expand same conditions (if any); store surely disjoint rules
		final List<LineEntry> nonOverlappingRules = disjoinSameConditions(rules, overallLastGroups);

		//expand same ending conditions (if any); store surely disjoint rules
		nonOverlappingRules.addAll(disjoinSameEndingConditions(rules, overallLastGroups));

		return nonOverlappingRules;
	}

	private List<LineEntry> disjoinSameConditions(final Collection<LineEntry> rules,
			final Map<Integer, Set<Character>> overallLastGroups){
		final List<LineEntry> finalRules = new ArrayList<>();

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

	private void disjoinSameConditions(final Collection<LineEntry> rules, final Map<Integer, Set<Character>> overallLastGroups,
			final String condition, final Collection<LineEntry> sameCondition, final Collection<LineEntry> finalRules){
		//extract children
		final List<LineEntry> children = new ArrayList<>(rules.size());
		LoopHelper.applyIf(rules,
			rule -> rule.condition.endsWith(condition),
			children::add);

		final Map<LineEntry, Set<Character>> groups = new HashMap<>();
		if(LoopHelper.match(children, child -> groups.put(child, child.extractGroup(condition.length())) != null) != null)
			throw new IllegalStateException("Duplicate key");

		rules.removeAll(sameCondition);

		//separate conditions:
		final Set<Character> childrenGroup = new HashSet<>();
		final Collection<Character> notPresentConditions = new HashSet<>();
		//for each rule with same condition
		for(final LineEntry parent : sameCondition){
			//extract ratifying group
			final Set<Character> parentGroup = groups.get(parent);

			//extract negated group
			final List<LineEntry> childrenNotParent = new ArrayList<>(children.size());
			LoopHelper.applyIf(children,
				child -> child != parent,
				childrenNotParent::add);
			childrenGroup.clear();
			for(final LineEntry lineEntry : childrenNotParent)
				LoopHelper.forEach(groups.get(lineEntry), childrenGroup::add);

			final Set<Character> groupsIntersection = SetHelper.intersection(parentGroup, childrenGroup);
			parentGroup.removeAll(groupsIntersection);
			if(!parentGroup.isEmpty()){
				final String newCondition = chooseCondition(parent.condition, parentGroup, childrenGroup, groupsIntersection,
					overallLastGroups);
				final LineEntry newRule = LineEntry.createFrom(parent, newCondition);
				finalRules.add(newRule);
			}

			//extract and add new condition only if there are rules that ends with the new condition
			notPresentConditions.clear();
			final Iterator<Character> itr = groupsIntersection.iterator();
			while(itr.hasNext()){
				final Character chr = itr.next();

				final String cond = chr + parent.condition;
				final LineEntry newEntryFromChildrenNotParent = LineEntry.createFromWithRules(parent, cond, childrenNotParent);
				final LineEntry newEntryFromParent = LineEntry.createFrom(parent, cond);
				if(newEntryFromChildrenNotParent.from.equals(newEntryFromParent.from)){
					notPresentConditions.add(chr);

					itr.remove();
				}
			}

			final Set<Character> characters = overallLastGroups.get(parent.condition.length());
			if(!notPresentConditions.isEmpty() && characters != null){
				final String notCondition = RegexHelper.makeNotGroup(notPresentConditions, comparator) + parent.condition;
				final Collection<Character> overallLastGroup = new HashSet<>(characters);
				overallLastGroup.removeAll(notPresentConditions);
				final String yesCondition = RegexHelper.makeGroup(overallLastGroup, comparator) + parent.condition;
				final LineEntry notRule = LoopHelper.match(finalRules,
					rule -> rule.condition.equals(notCondition) || rule.condition.equals(yesCondition));
				if(notRule != null)
					notRule.condition = parent.condition;
				else{
					//find already present rule
					final List<LineEntry> alreadyPresentRules = new ArrayList<>(finalRules.size());
					LoopHelper.applyIf(finalRules,
						r -> r.removal.equals(parent.removal) && r.addition.equals(parent.addition)
							&& r.condition.endsWith(parent.condition),
						alreadyPresentRules::add);
					for(final LineEntry alreadyPresentRule : alreadyPresentRules){
						finalRules.remove(alreadyPresentRule);

						notPresentConditions.addAll(parentGroup);
						//keep the two sets disjoint
						overallLastGroup.removeAll(parentGroup);
					}

					//if intersection between notPresentConditions and overallLastGroup is not empty
					final String newCondition = (notPresentConditions.size() < overallLastGroup.size()?
						RegexHelper.makeGroup(notPresentConditions, comparator) + parent.condition:
						RegexHelper.makeNotGroup(overallLastGroup, comparator) + parent.condition);
					final LineEntry newEntry = LineEntry.createFrom(parent, newCondition);
					finalRules.add(newEntry);
				}
			}
			LoopHelper.forEach(groupsIntersection, chr -> rules.add(LineEntry.createFrom(parent, chr + parent.condition)));
		}
	}

	private String chooseCondition(final String parentCondition, final Set<Character> parentGroup,
			final Set<Character> childrenGroup, final Collection<Character> groupsIntersection,
			final Map<Integer, Set<Character>> overallLastGroups){
		final int parentConditionLength = parentCondition.length();
		final boolean chooseRatifyingOverNegated = chooseRatifyingOverNegated(parentConditionLength, parentGroup, childrenGroup,
			groupsIntersection);
		final Set<Character> overallLastGroup = overallLastGroups.get(parentConditionLength);
		final Set<Character> baseGroup = (chooseRatifyingOverNegated? parentGroup: childrenGroup);
		final BiFunction<Set<Character>, Comparator<String>, String> combineRatifying = (chooseRatifyingOverNegated?
			RegexHelper::makeGroup:
			RegexHelper::makeNotGroup);
		final BiFunction<Set<Character>, Comparator<String>, String> combineNegated = (chooseRatifyingOverNegated?
			RegexHelper::makeNotGroup:
			RegexHelper::makeGroup);

		final String preCondition;
		if(overallLastGroup != null){
			final Set<Character> group = SetHelper.difference(overallLastGroup, baseGroup);
			if(baseGroup.size() == overallLastGroup.size())
				preCondition = (parentConditionLength == 0? StringUtils.EMPTY: DOT);
			else
				preCondition = (baseGroup.size() <= group.size()?
					combineRatifying.apply(baseGroup, comparator):
					combineNegated.apply(group, comparator));
		}
		else
			preCondition = combineRatifying.apply(baseGroup, comparator);
		return preCondition + parentCondition;
	}

	private boolean chooseRatifyingOverNegated(final int parentConditionLength, final Collection<Character> parentGroup,
			final Collection<Character> childrenGroup, final Collection<Character> intersectionGroup){
		final int parentGroupSize = parentGroup.size();
		final int childrenGroupSize = childrenGroup.size();
		final boolean chooseRatifyingOverNegated = ((parentConditionLength == 0 || intersectionGroup.isEmpty())
			&& parentGroupSize <= childrenGroupSize && parentGroupSize != 0);
		return (chooseRatifyingOverNegated || parentGroupSize != 0 && childrenGroupSize == 0);
	}

	private List<LineEntry> disjoinSameEndingConditions(final List<LineEntry> rules,
			final Map<Integer, Set<Character>> overallLastGroups){
		//bucket by condition ending
		final List<List<LineEntry>> forest = bucketByConditionEnding(rules);

		final ArrayList<LineEntry> finalRules = new ArrayList<>(forest.size());
		//for each bush in the forest
		for(final List<LineEntry> bush : forest){
			//if there is only one rule, then it goes in the final set
			if(bush.size() == 1)
				finalRules.add(bush.get(0));
			//otherwise process the rules
			else
				finalRules.addAll(disjoinSameEndingConditionsBush(bush, overallLastGroups));
		}
		finalRules.trimToSize();
		return finalRules;
	}

	private List<LineEntry> disjoinSameEndingConditionsBush(final Collection<LineEntry> bush,
			final Map<Integer, Set<Character>> overallLastGroups){
		final List<LineEntry> finalRules = new ArrayList<>();

		final Queue<LineEntry> queue = new PriorityQueue<>(shortestConditionComparator);
		queue.addAll(bush);
		final Set<Character> childrenGroup = new HashSet<>();
		while(!queue.isEmpty()){
			final LineEntry parent = queue.remove();

			final List<LineEntry> bubbles = extractBubbles(queue, parent);
			if(bubbles.isEmpty()){
				finalRules.add(parent);
				continue;
			}

			//extract ratifying group
			final int parentConditionLength = parent.condition.length();
			final Set<Character> parentGroup = parent.extractGroup(parentConditionLength);

			//extract negated group
			childrenGroup.clear();
			for(final LineEntry child : bubbles)
				childrenGroup.add(child.condition.charAt(child.condition.length() - parentConditionLength - 1));

			//if intersection(parent-group, children-group) is empty
			final Set<Character> groupsIntersection = SetHelper.intersection(parentGroup, childrenGroup);
			parentGroup.removeAll(groupsIntersection);

			//calculate new condition
			final boolean chooseRatifyingOverNegated = chooseRatifyingOverNegated(parentConditionLength, parentGroup,
				childrenGroup);
			final String preCondition = (chooseRatifyingOverNegated? RegexHelper.makeGroup(parentGroup, comparator):
				RegexHelper.makeNotGroup(childrenGroup, comparator));
			LineEntry newEntry = LineEntry.createFrom(parent, preCondition + parent.condition);

			//keep only rules that matches some existent words
			if(newEntry.isProductive())
				finalRules.add(newEntry);
			else if(!growNewBush(queue, parent))
				continue;
			else
				LOGGER.debug("skip unused rule: {} {} {}", newEntry.removal, StringUtils.join(newEntry.addition, PIPE),
					(newEntry.condition.isEmpty()? DOT: newEntry.condition));

			final LineEntry maxConditionEntry = LoopHelper.max(queue, Comparator.comparingInt(e -> e.condition.length()));
			final int maxConditionLength = (maxConditionEntry != null? maxConditionEntry.condition.length(): 0);
			if(parentConditionLength + 1 >= maxConditionLength){
				queue.removeAll(bubbles);

				finalRules.addAll(bubbles);
			}
			else if(LoopHelper.allMatch(queue, rule -> rule.condition.length() > parentConditionLength + 1))
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

	/**
	 * @param queue	The queue to add the new rules to
	 * @param parent	The parent rule
	 * @return	{@code true} if a bush was created and added to the queue
	 */
	private boolean growNewBush(final Collection<LineEntry> queue, final LineEntry parent){
		final int parentConditionLength = parent.condition.length();

		final ArrayList<LineEntry> newBushes = new ArrayList<>();
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

	private List<LineEntry> extractSimilarRules(final Iterable<LineEntry> rules, final String parentCondition){
		final List<LineEntry> children = new ArrayList<>();
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

	private List<LineEntry> extractBubbles(final Iterable<LineEntry> bush, final LineEntry parent){
		final int parentConditionLength = parent.condition.length();
		final List<LineEntry> bubbles = new ArrayList<>();
		LoopHelper.applyIf(bush,
			child -> child.condition.length() > parentConditionLength && child.condition.endsWith(parent.condition),
			bubbles::add);

		//if the bush contains a rule whose `from` is contained into this bubble, then remove the bubble
		bubbles.removeIf(bubble -> parent.from.containsAll(bubble.from)
			&& bubble.from.equals(new HashSet<>(parent.extractFromEndingWith(bubble.condition))));

		return bubbles;
	}

	private boolean chooseRatifyingOverNegated(final int parentConditionLength, final Collection<Character> parentGroup,
			final Collection<Character> childrenGroup){
		final int parentGroupSize = parentGroup.size();
		final int childrenGroupSize = childrenGroup.size();
		final boolean chooseRatifyingOverNegated = ((parentConditionLength == 0 || parentGroupSize == 1 && childrenGroupSize > 1)
			&& parentGroupSize != 0);
		return (chooseRatifyingOverNegated || parentGroupSize != 0 && childrenGroupSize == 0);
	}

	/** Merge common conditions (ex. `[^a]bc` and `[^a]dc` will become `[^a][bd]c`) */
	private void mergeSimilarRules(final Collection<LineEntry> entries){
		final Map<String, List<LineEntry>> similarityBucket = SetHelper.bucket(entries,
			entry -> (entry.condition.contains(RegexHelper.GROUP_END)?
			entry.removal + TAB + entry.addition + TAB + RegexSequencer.splitSequence(entry.condition)[0] + TAB
				+ RegexSequencer.splitSequence(entry.condition).length:
			null));
		final Collection<Character> group = new HashSet<>();
		for(final List<LineEntry> similarities : similarityBucket.values())
			if(similarities.size() > 1){
				final LineEntry anEntry = similarities.iterator().next();
				final String[] aCondition = RegexSequencer.splitSequence(anEntry.condition);
				final String[] commonPreCondition = LineEntry.SEQUENCER_REGEXP.subSequence(aCondition, 0, 1);
				final String[] commonPostCondition = LineEntry.SEQUENCER_REGEXP.subSequence(aCondition, 2);
				//extract all the rules from `similarities` that has the condition compatible with firstEntry.condition
				group.clear();
				for(final LineEntry similarity : similarities)
					group.add(RegexSequencer.splitSequence(similarity.condition)[1].charAt(0));
				final String condition = StringUtils.join(commonPreCondition) + RegexHelper.makeGroup(group, comparator)
					+ StringUtils.join(commonPostCondition);
				entries.add(LineEntry.createFrom(anEntry, condition));

				for(final LineEntry similarity : similarities)
					entries.remove(similarity);
			}
	}

	public List<String> convertFormat(final String flag, final boolean keepLongestCommonAffix, final Iterable<LineEntry> compactedRules){
		final RuleEntry ruleToBeReduced = affixData.getData(flag);
		if(ruleToBeReduced == null)
			throw new LinterException(NON_EXISTENT_RULE.format(new Object[]{flag}));

		final AffixType type = ruleToBeReduced.getType();
		final List<String> prettyPrintRules = convertEntriesToRules(flag, type, keepLongestCommonAffix, compactedRules);
		prettyPrintRules.add(0, LineEntry.toHunspellHeader(type, flag, ruleToBeReduced.combinableChar(),
			prettyPrintRules.size()));
		return prettyPrintRules;
	}

	private List<String> convertEntriesToRules(final String flag, final AffixType type, final boolean keepLongestCommonAffix,
			final Iterable<LineEntry> entries){
		//restore original rules
		final ArrayList<LineEntry> restoredRules = new ArrayList<>();
		LoopHelper.forEach(entries, rule -> {
			restoredRules.ensureCapacity(restoredRules.size() + rule.addition.size());
			LoopHelper.forEach(rule.addition, addition -> {
				final int lcp = StringHelper.longestCommonPrefix(rule.removal, addition).length();
				final String removal = rule.removal.substring(lcp);
				final LineEntry entry = new LineEntry((removal.isEmpty()? ZERO: removal), addition.substring(lcp),
					rule.condition, rule.from);
				restoredRules.add(type == AffixType.SUFFIX? entry: entry.createReverse());
			});
		});
		final List<LineEntry> sortedEntries = prepareRules(keepLongestCommonAffix, restoredRules);

		return composeAffixRules(flag, type, sortedEntries);
	}

	private List<LineEntry> prepareRules(final boolean keepLongestCommonAffix, final Collection<LineEntry> entries){
		if(keepLongestCommonAffix)
			LoopHelper.forEach(entries, entry -> entry.expandConditionToMaxLength(comparator));

		final List<LineEntry> list = new ArrayList<>(entries);
		list.sort(lineEntryComparator);
		return list;
	}

	private List<String> composeAffixRules(final String flag, final AffixType type, final Collection<LineEntry> entries){
		final List<String> list = new ArrayList<>(entries.size());
		LoopHelper.forEach(entries, entry -> list.add(entry.toHunspellRule(type, flag)));
		return list;
	}

	void checkReductionCorrectness(final String flag, final List<String> reducedRules, final Collection<String> originalLines){
		checkReductionCorrectness(flag, reducedRules, originalLines, null);
	}

	public void checkReductionCorrectness(final String flag, final List<String> reducedRules, final Collection<String> originalLines,
			final Consumer<Integer> progressCallback){
		final RuleEntry ruleToBeReduced = affixData.getData(flag);
		if(ruleToBeReduced == null)
			throw new LinterException(NON_EXISTENT_RULE.format(new Object[]{flag}));

		final AffixType type = ruleToBeReduced.getType();

		//extract rules (skip the header)
		final AffixEntry[] entries = new AffixEntry[reducedRules.size() - 1];
		for(int i = 0; i < reducedRules.size() - 1; i ++){
			final String reducedRule = reducedRules.get(i + 1);
			entries[i] = new AffixEntry(reducedRule, i, type, flag, strategy, null, null);
		}

		int progress = 0;
		int progressIndex = 0;
		final int progressStep = (int)Math.ceil(originalLines.size() / 100.f);
		final RuleEntry overriddenRule = new RuleEntry(type, flag, ruleToBeReduced.combinableChar());
		overriddenRule.setEntries(entries);
		for(final String line : originalLines){
			final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(line, affixData);
			final Inflection[] originalInflections = wordGenerator.applyAffixRules(dicEntry);
			final Inflection[] inflections = wordGenerator.applyAffixRules(dicEntry, overriddenRule);

			final List<LineEntry> filteredOriginalRules = collectInflectionsByFlag(originalInflections, flag, type);
			final List<LineEntry> filteredRules = collectInflectionsByFlag(inflections, flag, type);
			if(!filteredOriginalRules.equals(filteredRules))
				throw new LinterException(VERY_BAD_ERROR.format(new Object[]{line, filteredOriginalRules, filteredRules}));

			if(progressCallback != null && ++ progress % progressStep == 0)
				progressCallback.accept(++ progressIndex);
		}
	}

}
