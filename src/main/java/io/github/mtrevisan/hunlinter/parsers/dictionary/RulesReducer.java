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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


public class RulesReducer{

	private static final String NON_EXISTENT_RULE = "Non-existent rule `{}`, cannot reduce";
	private static final String VERY_BAD_ERROR = "Something very bad happened while inflecting from `{}`, expected {}, obtained {}";

	private static final String TAB = "\t";
	private static final String ZERO = "0";


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
		List<LineEntry> compactedRules = redistributeRules(plainRules, comparator);

		if(progressCallback != null)
			progressCallback.accept(25);

		//reshuffle originating list to place the correct inflections in the correct rule
		compactedRules = makeAdditionsDisjoint(compactedRules);

		if(progressCallback != null)
			progressCallback.accept(50);

		compactedRules = disjoinConditions(compactedRules);

		if(progressCallback != null)
			progressCallback.accept(75);

		mergeSimilarRules(compactedRules);

		return compactedRules;
	}

	private static List<LineEntry> redistributeRules(final List<LineEntry> plainRules, final Comparator<String> comparator){
		final Map<String, LineEntry> map = new HashMap<>(0);
		for(int i = 0; i < plainRules.size(); i ++)
			redistributeRule(plainRules.get(i), map);

		//redistribute:
		final List<LineEntry> redistributedRules = redistributeRules(map, comparator);

		//compact:
		return compactRules(redistributedRules, comparator);
	}

	private static void redistributeRule(final LineEntry entry, final Map<String, LineEntry> map){
		final StringBuilder key = new StringBuilder(entry.removal + TAB);
		final int additionIndex = key.length();
		for(final String addition : entry.addition){
			key.setLength(additionIndex);
			key.append(addition)
				.append(TAB)
				.append(entry.condition);
			final String keyString = key.toString();
			final LineEntry rule = map.get(keyString);
			if(rule == null){
				final LineEntry newEntry = new LineEntry(entry.removal, addition, entry.condition, entry.from);
				map.put(keyString, newEntry);
			}
			else
				rule.from.addAll(entry.from);
		}
	}

	private static List<LineEntry> redistributeRules(final Map<String, LineEntry> map, final Comparator<String> comparator){
		//same removal, condition, and from parts
		final Map<String, LineEntry> compaction = new HashMap<>(map.size());
		for(final LineEntry entry : map.values()){
			final String key = entry.removal + TAB + entry.condition + TAB + RegexHelper.sortAndMergeSet(entry.from, comparator);
			final LineEntry rule = compaction.putIfAbsent(key, entry);
			if(rule != null)
				rule.addition.addAll(entry.addition);
		}
		return new ArrayList<>(compaction.values());
	}

	private static List<LineEntry> compactRules(final List<LineEntry> rules, final Comparator<String> comparator){
		//same removal, addition, and condition parts
		final Map<String, LineEntry> compaction = new HashMap<>(rules.size());
		for(int i = 0; i < rules.size(); i ++){
			final LineEntry entry = rules.get(i);
			final String key = entry.removal + TAB + RegexHelper.sortAndMergeSet(entry.addition, comparator) + TAB + entry.condition;
			final LineEntry rule = compaction.putIfAbsent(key, entry);
			if(rule != null)
				rule.from.addAll(entry.from);
		}
		return new ArrayList<>(compaction.values());
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
				final Set<String> strings = rule.addition;
				for(final String s : additionsToBeRemoved)
					strings.remove(s);
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

	private synchronized List<LineEntry> disjoinConditions(final List<LineEntry> rules){
		final List<LineEntry> finalRules = new ArrayList<>(0);

		List<List<LineEntry>> branches = extractTree(rules);

		for(int i = 0; i < branches.size(); i ++){
			final List<LineEntry> branch = branches.get(i);
			final int branchSize = branch.size();
			if(branchSize == 1)
				continue;

			//find if all the branches share the same condition length:
			final int conditionLength = extractSameConditionLength(branch);
			if(conditionLength >= 0)
				disjoinSameConditionLength(branches, i, comparator);
			else if(isParentAFinalRule(branch)){
				final LineEntry finalRule = branch.remove(0);
				finalRules.add(finalRule);
			}
			else{
				disjoinDifferentConditionLength(branches, i);

				restoreRules(rules, branches);
				branches = extractTree(rules);
			}
		}

		restoreRules(rules, branches);



/*		final StringBuilder condition = new StringBuilder();

		final Collection<Character> parentRatifyingGroup = new HashSet<>(0);
		final Collection<Character> parentNegatedGroup = new HashSet<>(0);
		final Map<LineEntry, Set<Character>> collisions = new HashMap<>(0);
		final List<LineEntry> childrenRules = new ArrayList<>(0);
		final Collection<Character> childrenGroup = new HashSet<>(0);
		final Collection<Character> commonChildrenGroup = new HashSet<>(0);
		final Map<Integer, Set<Character>> allGroup = new HashMap<>();
		final Collection<Character> otherGroup = new HashSet<>(0);

		boolean restart = true;
		while(restart){
			restart = false;

			final List<List<LineEntry>> branches2 = extractTree(rules);

			//for each limb, level up the conditions so there is no intersection between the limb and the branches
			for(int i = 0; !restart && i < branches2.size(); i ++){
				final List<LineEntry> branch = branches2.get(i);
				int branchSize = branch.size();
				if(branchSize == 1)
					continue;

				//extract limb
				final int parentIndex = 0;
				final LineEntry parent = branch.get(parentIndex);

				//augment parent condition to avoid any intersection:
				final int parentConditionLength = RegexHelper.conditionLength(parent.condition);
				final Set<Character> parentGroup = parent.extractGroup(parentConditionLength);

				final Set<Character> agl = allGroup.computeIfAbsent(parentConditionLength, key -> {
					final Set<Character> ag = new HashSet<>(branches2.size() + finalRules.size());
					for(int j = 0; j < branches2.size(); j++)
						for(final LineEntry rule : branches2.get(j))
							ag.addAll(rule.extractGroup(parentConditionLength));
					for(int m = 0; m < finalRules.size(); m++)
						ag.addAll(finalRules.get(m).extractGroup(parentConditionLength));
					return ag;
				});
				if(parentGroup.equals(agl)){
//					finalRules.add(parent);
//					branch.remove(parentIndex);
//					i --;
//					continue;
				}

				parentRatifyingGroup.clear();
				parentNegatedGroup.clear();

				//for every character, if it collides with a children, put into `parentNegatedCondition`, otherwise put in
				//`parentRatifyingCondition`
				for(final Character chr : parentGroup){
					collisions.clear();
					for(int m = parentIndex + 1; m < branchSize; m ++){
						final LineEntry child = branch.get(m);
						final Set<Character> childGroup = child.extractGroup(parentConditionLength);
						if(childGroup.contains(chr))
							collisions.put(child, childGroup);
					}
					if(collisions.isEmpty())
						parentRatifyingGroup.add(chr);
					else{
						childrenRules.clear();
						childrenGroup.clear();
						commonChildrenGroup.clear();
						for(final LineEntry rule : collisions.keySet()){
							final Set<Character> childGroup = collisions.get(rule);
							if(!rule.from.containsAll(parent.from)){
								childrenRules.add(rule);
								childrenGroup.addAll(childGroup);
							}
							else
								commonChildrenGroup.addAll(childGroup);
						}
						commonChildrenGroup.removeAll(parentGroup);

						if(childrenGroup.isEmpty()){
							parentNegatedGroup.add(chr);
							continue;
						}

						condition.setLength(0);
						if(childrenGroup.contains(chr)){
							//if parent contains one of its children, then it has to remain in the final rule set
							boolean contained = false;
							for(int j = 0; j < childrenRules.size(); j ++){
								final LineEntry child = childrenRules.get(j);
								if(parent.from.containsAll(child.from)){
//									otherGroup.add(chr);

//									final Set<Character> newParentGroup = parent.extractGroup(parentConditionLength);
//									parent.condition = RegexHelper.makeGroup(parentGroup, comparator)
//										+ parent.condition;

									//extend `parent.condition` and `child.condition` to their maximum length
//									final String newParentCondition = StringHelper.longestCommonSuffix(parent.from.toArray(String[]::new));
//									final String newChildCondition = StringHelper.longestCommonSuffix(child.from.toArray(String[]::new));
//									if(newParentCondition.equals(newChildCondition)){
//										parent.condition = newParentCondition;
//										child.condition = newChildCondition;
//										final int newParentConditionLength = newParentCondition.length();
//										final Set<Character> newParentGroup = parent.extractGroup(newParentConditionLength);
//										final Set<Character> newChildGroup = child.extractGroup(newParentConditionLength);
//										final Set<Character> newIntersectionGroup = SetHelper.intersection(newParentGroup, newChildGroup);
//										if(newIntersectionGroup.isEmpty()){
//											final int ratifyingSize = newParentGroup.size();
//											final int negatedSize = newChildGroup.size();
//											boolean chooseRatifyingOverNegated = (ratifyingSize < negatedSize + Math.max(newParentConditionLength - 3, 0)
//												&& ratifyingSize > 0
//												|| negatedSize == 0
//											);
//											String augment = (chooseRatifyingOverNegated
//												? RegexHelper.makeGroup(newParentGroup, comparator)
//												: RegexHelper.makeNotGroup(newChildGroup, comparator));
//											condition.setLength(0);
//											condition.append(augment)
//												.append(parent.condition);
//											parent.condition = condition.toString();
//
//											//NOTE: here `parentRatifyingGroup` and `parentNegatedGroup` are swapped!!
//											chooseRatifyingOverNegated = (negatedSize < ratifyingSize + Math.max(newParentConditionLength - 3, 0)
//												&& negatedSize > 0
//												|| ratifyingSize == 0
//											);
//											augment = (chooseRatifyingOverNegated
//												? RegexHelper.makeGroup(newChildGroup, comparator)
//												: RegexHelper.makeNotGroup(newParentGroup, comparator));
//											condition.setLength(0);
//											condition.append(augment)
//												.append(child.condition);
//											child.condition = condition.toString();
//										}
//										else if(!parent.from.containsAll(child.from))
//											throw new IllegalStateException("Cannot `child.from` is not fully contained into `parent.from`, please report this case to the developer, thank you");
//									}

									contained = true;
									break;
								}
							}
							if(contained){
								//remove parent (it will be reinserted before exiting this method)
								finalRules.add(parent);
								branch.remove(parentIndex);
//								branchSize --;

								//FIXME: SFX
								parentRatifyingGroup.clear();
								//FIXME: PFX
//								parentNegatedGroup.clear();

								restart = true;
								break;
							}

							final String preCondition = bubbleUpCondition(parent, parentConditionLength, branch, chr, childrenRules);
							if(preCondition == null)
								throw new IllegalStateException("Cannot `bubble-up` condition, please report this case to the developer, thank you");

							condition.append(preCondition);
						}
						//resolve collision
						condition.append(RegexHelper.makeGroup(childrenGroup, comparator))
							.append(parent.condition);
						final Set<String> newParentFrom = parent.extractFromEndingWith(condition.toString());
						if(parent.from.equals(newParentFrom))
							parent.condition = condition.toString();
						else{
							final LineEntry newRule = LineEntry.createFromWithWords(parent, condition.toString(), newParentFrom);
							branch.add(newRule);
						}
						restart = true;

						if(!commonChildrenGroup.isEmpty())
							throw new IllegalStateException("`commonChildrenGroup` is not empty, please report this case to the developer, thank you");
					}
				}

				if(!otherGroup.isEmpty()){
					parentRatifyingGroup.clear();
					parentNegatedGroup.clear();
					parentRatifyingGroup.addAll(otherGroup);
					parentNegatedGroup.addAll(agl);
					parentNegatedGroup.removeAll(otherGroup);

					final int ratifyingSize = parentRatifyingGroup.size();
					final int negatedSize = parentNegatedGroup.size();
					final boolean chooseRatifyingOverNegated = (ratifyingSize < negatedSize + Math.max(parentConditionLength - 3, 0)
						&& ratifyingSize > 0
						|| negatedSize == 0
					);
					final String augment = (chooseRatifyingOverNegated
						? RegexHelper.makeGroup(parentRatifyingGroup, comparator)
						: RegexHelper.makeNotGroup(parentNegatedGroup, comparator));
					condition.setLength(0);
					condition.append(augment)
						.append(parent.condition);
					parent.condition = condition.toString();

					restart = true;
				}
				else if(!parentRatifyingGroup.isEmpty()){
					parentNegatedGroup.clear();
					for(int m = parentIndex + 1; m < branchSize; m ++){
						final LineEntry child = branch.get(m);
						final Set<Character> childGroup = child.extractGroup(parentConditionLength);
						parentNegatedGroup.addAll(childGroup);
					}
					for(int m = 0; m < finalRules.size(); m ++){
						final LineEntry child = finalRules.get(m);
						final Set<Character> childGroup = child.extractGroup(parentConditionLength);
						parentNegatedGroup.addAll(childGroup);
					}
					parentNegatedGroup.removeAll(parentRatifyingGroup);

					final int ratifyingSize = parentRatifyingGroup.size();
					final int negatedSize = parentNegatedGroup.size();
					final boolean chooseRatifyingOverNegated = (ratifyingSize < negatedSize + Math.max(parentConditionLength - 3, 0)
						&& ratifyingSize > 0
						|| negatedSize == 0
					);
					final String augment = (chooseRatifyingOverNegated
						? RegexHelper.makeGroup(parentRatifyingGroup, comparator)
						: RegexHelper.makeNotGroup(parentNegatedGroup, comparator));
					condition.setLength(0);
					condition.append(augment)
						.append(parent.condition);
					parent.condition = condition.toString();

					restart = true;
				}
				else if(!parentNegatedGroup.isEmpty()){
					parentRatifyingGroup.clear();
					for(int m = parentIndex + 1; m < branchSize; m ++){
						final LineEntry child = branch.get(m);
						final Set<Character> childGroup = child.extractGroup(parentConditionLength);
						parentRatifyingGroup.addAll(childGroup);
					}
					for(int m = 0; m < finalRules.size(); m ++){
						final LineEntry child = finalRules.get(m);
						final Set<Character> childGroup = child.extractGroup(parentConditionLength);
						parentRatifyingGroup.addAll(childGroup);
					}
					parentRatifyingGroup.removeAll(parentNegatedGroup);

					//NOTE: here `parentRatifyingGroup` and `parentNegatedGroup` are swapped!!
					final int ratifyingSize = parentNegatedGroup.size();
					final int negatedSize = parentRatifyingGroup.size();
					final boolean chooseRatifyingOverNegated = (ratifyingSize < negatedSize + Math.max(parentConditionLength - 3, 0)
						&& ratifyingSize > 0
						|| negatedSize == 0
					);
					final String augment = (chooseRatifyingOverNegated
						? RegexHelper.makeGroup(parentNegatedGroup, comparator)
						: RegexHelper.makeNotGroup(parentRatifyingGroup, comparator));
					condition.setLength(0);
					condition.append(augment)
						.append(parent.condition);
					parent.condition = condition.toString();

					restart = true;
				}
//				else if(!parent.condition.contains("[^")){
//					parentNegatedGroup.clear();
//					for(int m = parentIndex + 1; m < branchSize; m ++){
//						final LineEntry child = branch.get(m);
//						final Set<Character> childGroup = child.extractGroup(parentConditionLength);
//						parentNegatedGroup.addAll(childGroup);
//					}
//					for(int m = 0; m < finalRules.size(); m ++){
//						final LineEntry child = finalRules.get(m);
//						final Set<Character> childGroup = child.extractGroup(parentConditionLength);
//						parentNegatedGroup.addAll(childGroup);
//					}
//					parentNegatedGroup.removeAll(parentGroup);
//
//					//NOTE: here `parentRatifyingGroup` and `parentNegatedGroup` are swapped!!
//					final int ratifyingSize = parentNegatedGroup.size();
//					final int negatedSize = parentRatifyingGroup.size();
//					final boolean chooseRatifyingOverNegated = (ratifyingSize < negatedSize + Math.max(parentConditionLength - 3, 0)
//						&& ratifyingSize > 0
//						|| negatedSize == 0
//					);
//					final String augment = (chooseRatifyingOverNegated
//						? RegexHelper.makeGroup(parentNegatedGroup, comparator)
//						: RegexHelper.makeNotGroup(parentRatifyingGroup, comparator));
//					condition.setLength(0);
//					condition.append(augment)
//						.append(parent.condition);
//					parent.condition = condition.toString();
//
//					restart = true;
//				}
			}

			restoreRules(rules, branches2);
		}*/

		rules.addAll(finalRules);

		return rules;
	}

	private static int extractSameConditionLength(final List<LineEntry> branch){
		final int conditionLength = branch.get(0).condition.length();
		for(int j = 0; j < branch.size(); j ++){
			final LineEntry entry = branch.get(j);
			if(conditionLength != entry.condition.length())
				return -1;
		}
		return conditionLength;
	}

	private static void disjoinSameConditionLength(final List<List<LineEntry>> branches, final int branchIndex,
			final Comparator<String> comparator){
		final Map<LineEntry, Set<Character>> branchesGroup = new HashMap<>(branches.size());
		final List<LineEntry> branch = branches.get(branchIndex);
		final int conditionLength = branch.get(0).condition.length();
		for(int i = 0; i < branches.size(); i ++){
			final List<LineEntry> rules = branches.get(i);
			for(int j = 0; j < rules.size(); j ++){
				final LineEntry rule = rules.get(j);
				branchesGroup.put(rule, rule.extractGroup(conditionLength));
			}
		}

		final Set<Character> alphabetGroup = new HashSet<>(branchesGroup.size());
		for(final Set<Character> ruleGroup : branchesGroup.values())
			alphabetGroup.addAll(ruleGroup);

		final StringBuilder condition = new StringBuilder();
		for(int i = 0; i < branch.size(); i ++){
			final LineEntry entry = branch.get(i);

			final Set<Character> ratifyingGroup = branchesGroup.get(entry);
			final Collection<Character> negatedGroup = new HashSet<>(alphabetGroup);
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

	private static boolean isParentAFinalRule(final List<LineEntry> branch){
		//if parent contains into one if its children (that have child.condition.length > parent.length),
		//then the parent is a final rule
		final LineEntry parent = branch.get(0);

		for(int j = 1; j < branch.size(); j ++){
			final LineEntry rule = branch.get(j);
			if(parent.from.containsAll(rule.from))
				return true;
		}
		return false;
	}

	private static void disjoinDifferentConditionLength(final List<List<LineEntry>> branches, final int branchIndex){
		final List<LineEntry> branch = branches.get(branchIndex);
		final LineEntry parent = branch.get(0);

		final StringBuilder condition = new StringBuilder();
		for(int i = 1; i < branch.size(); i ++){
			//augment parent condition to avoid any intersection:
			final int parentConditionLength = parent.condition.length();
			final Set<Character> parentGroup = parent.extractGroup(parentConditionLength);

			final Set<Character> childGroup = parent.extractGroup(parentConditionLength);
			for(int j = 1; j < branch.size(); j ++){
				final LineEntry child = branch.get(j);
				childGroup.addAll(child.extractGroup(parentConditionLength));
			}

			System.out.println();
//			final Set<Character> ratifyingGroup = branchesGroup.get(rule);
//			final Collection<Character> negatedGroup = new HashSet<>(alphabetGroup);
//			negatedGroup.removeAll(ratifyingGroup);
//
//			final int ratifyingSize = ratifyingGroup.size();
//			final int negatedSize = negatedGroup.size();
//			final boolean chooseRatifyingOverNegated = (ratifyingSize < negatedSize + Math.max(conditionLength - 3, 0));
//			final String augment = (chooseRatifyingOverNegated
//				? RegexHelper.makeGroup(ratifyingGroup, comparator)
//				: RegexHelper.makeNotGroup(negatedGroup, comparator));
//			condition.setLength(0);
//			condition.append(augment)
//				.append(rule.condition);
//			rule.condition = condition.toString();
		}
	}




	//resolve o+elo that must be [^e]lo+[^l]o+elo:
	private String bubbleUpCondition(final LineEntry parent, final int parentConditionLength, final List<LineEntry> branch,
			Character chr, final List<LineEntry> childrenRules){
		//calculate maximum condition length:
		int minLength = -1;
		for(int i = 0; i < branch.size(); i ++){
			final int minimumFromLength = branch.get(i).getMinimumFromLength();
			if(minimumFromLength < minLength || minLength < 0){
				minLength = minimumFromLength;

				if(minLength == 1)
					break;
			}
		}

		//start expanding the condition (until the maximum length is reached):
		LineEntry shadowParent = parent;
		final Set<Character> shadowChildrenGroup = new HashSet<>(childrenRules.size());
		int k;
		for(k = parentConditionLength + 1; k <= minLength; k ++){
			shadowParent = LineEntry.createFrom(shadowParent, chr + shadowParent.condition);
			final Set<Character> shadowParentGroup = shadowParent.extractGroup(k);
			shadowChildrenGroup.clear();
			for(int m = 0; m < childrenRules.size(); m ++){
				final LineEntry child = childrenRules.get(m);
				final Set<Character> childGroup = child.extractGroup(k);
				shadowChildrenGroup.addAll(childGroup);
			}
			final Set<Character> shadowIntersectionGroup = SetHelper.intersection(shadowParentGroup, shadowChildrenGroup);
			final int shadowIntersectionSize = shadowIntersectionGroup.size();
			if(shadowIntersectionSize == 0){
				final int ratifyingSize = shadowParentGroup.size();
				final int negatedSize = shadowChildrenGroup.size();
				final boolean chooseRatifyingOverNegated = (ratifyingSize < negatedSize + Math.max(parentConditionLength - 3, 0)
					&& ratifyingSize > 0
					|| negatedSize == 0
				);
				return (chooseRatifyingOverNegated
					? RegexHelper.makeGroup(shadowParentGroup, comparator)
					: RegexHelper.makeNotGroup(shadowChildrenGroup, comparator));
			}
			else if(shadowIntersectionSize == 1){
				//restart cycle adding a character to the shadow parent
				chr = shadowIntersectionGroup.iterator().next();
				k --;
			}

			//otherwise, add one character to both shadowParent and shadowChildren, and restart
			//FIXME (to be removed after the bubbling-up works)
			else
				throw new IllegalStateException("cannot `bubble-up`, please report this case to the developer, thank you");
		}

		return null;
	}

	//NOTE: `rules` will be emptied.
	private List<List<LineEntry>> extractTree(final List<LineEntry> rules){
		//order by condition length
		rules.sort(Comparator.comparingInt(rule -> RegexHelper.conditionLength(rule.condition)));

		//extract branches whose conditions are disjoint, each branch contains all the rules that share the same ending condition (given by
		//the first item, the (limb) parent, so to say)
		final List<List<LineEntry>> branches = new ArrayList<>(rules.size());
		while(!rules.isEmpty()){
			final List<LineEntry> branch = extractBranch(rules);
			branches.add(branch);
		}
		return branches;
	}

	private void restoreRules(final Collection<LineEntry> rules, final List<List<LineEntry>> branches){
		rules.clear();
		for(int i = 0; i < branches.size(); i ++)
			rules.addAll(branches.get(i));
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

	/** Merge common conditions (ex. `[^a]bc` and `[^a]dc` will become `[^a][bd]c`). */
	private void mergeSimilarRules(final Collection<LineEntry> rules){
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
				for(int i = 0; i < similarities.size(); i ++)
					group.add(RegexSequencer.splitSequence(similarities.get(i).condition)[1].charAt(0));

				condition.setLength(0);
				for(final String cpc : commonPreCondition)
					condition.append(cpc);
				condition.append(RegexHelper.makeGroup(group, comparator));
				for(final String cpc : commonPostCondition)
					condition.append(cpc);
				condition.toString();

				final LineEntry newRule = LineEntry.createFrom(anEntry, condition.toString());
				for(int i = 1; i < similarities.size(); i ++)
					newRule.from.addAll(similarities.get(i).from);
				rules.add(newRule);

				for(int i = 0; i < similarities.size(); i ++)
					rules.remove(similarities.get(i));
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
		List<LineEntry> sortedEntries = Collections.emptyList();
		if(!entries.isEmpty()){
			//restore original rules
			final ArrayList<LineEntry> restoredRules = new ArrayList<>(0);
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
			sortedEntries = prepareRules(keepLongestCommonAffix, restoredRules);
		}

		return composeAffixRules(flag, type, sortedEntries);
	}

	private List<LineEntry> prepareRules(final boolean keepLongestCommonAffix, final List<LineEntry> entries){
		if(keepLongestCommonAffix)
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
		final Collection<DictionaryEntry> originalInflectionsWhole = new HashSet<>(0);
		final Collection<DictionaryEntry> inflectionsWhole = new HashSet<>(0);
		for(int i = 0; i < originalLines.size(); i ++){
			final String line = originalLines.get(i);
			final DictionaryEntry dicEntry = dictionaryEntryFactory.createFromDictionaryLine(line);
			final List<Inflection> originalInflections = wordGenerator.applyAffixRules(dicEntry);
			final List<Inflection> inflections = wordGenerator.applyAffixRules(dicEntry, overriddenParent);

			originalInflectionsWhole.clear();
			for(int j = 0; j < originalInflections.size(); j ++)
				originalInflectionsWhole.add(new DictionaryEntry(originalInflections.get(j)));
			inflectionsWhole.clear();
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
			for(int i = 0; i < rules.size(); i ++){
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
