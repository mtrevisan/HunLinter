package unit731.hunspeller.parsers.dictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import unit731.hunspeller.services.StringHelper;


public class RulesReducer{

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesReducer.class);

	private static final Pattern SPLITTER_ADDITION = PatternHelper.pattern("(?=[/\\t])");

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
			.thenComparingInt(entry -> StringUtils.countMatches(entry.condition, PatternHelper.GROUP_END))
			.thenComparingInt(entry -> entry.removal.length())
			.thenComparing(entry -> StringUtils.reverse(entry.condition), comparator)
			.thenComparing(entry -> entry.removal, comparator)
			.thenComparingInt(entry -> entry.anAddition().length())
			.thenComparing(LineEntry::anAddition, comparator);
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
				final LineEntry newEntry = createAffixEntry(production, word, type);
				filteredRules.add(newEntry);
			}
		}
		return compactProductions(filteredRules);
	}

	private List<LineEntry> compactProductions(final List<LineEntry> rules){
		final List<LineEntry> compactedRules = new ArrayList<>();
		if(rules.size() > 1){
			final String from = rules.get(0).from.iterator().next();
			//retrieve rule with longest condition (all the other conditions must be this long)
			final LineEntry compactedRule = rules.stream()
				.max(Comparator.comparingInt(rule -> rule.condition.length()))
				.get();
			final int longestConditionLength = compactedRule.condition.length();
			for(final LineEntry rule : rules){
				//recover the missing characters for the current condition to become of length the maximum found earlier
				final int startIndex = from.length() - longestConditionLength;
				//if a condition is not long enough, keep it separate
				if(startIndex >= 0){
					final int delta = longestConditionLength - rule.condition.length();
					final String deltaAddition = from.substring(startIndex, startIndex + delta);
					//add addition
					rule.addition.forEach(addition -> compactedRule.addition.add(deltaAddition + addition));
				}
			}
			compactedRules.add(compactedRule);
		}
		else
			compactedRules.addAll(rules);
		return compactedRules;
	}

	private LineEntry createAffixEntry(final Production production, String word, final AffixEntry.Type type){
		String producedWord = production.getWord();
		if(type == AffixEntry.Type.PREFIX){
			producedWord = StringUtils.reverse(producedWord);
			word = StringUtils.reverse(word);
		}

		final int lastCommonLetter = calculateLastCommonLetterIndex(word, producedWord);

		final int wordLength = word.length();
		final String removal = (lastCommonLetter < wordLength? word.substring(lastCommonLetter): AffixEntry.ZERO);
		String addition = (lastCommonLetter < producedWord.length()? producedWord.substring(lastCommonLetter): AffixEntry.ZERO);
		final AffixEntry lastAppliedRule = production.getLastAppliedRule(type);
		if(lastAppliedRule != null)
			addition += lastAppliedRule.toStringWithMorphologicalFields(strategy);
		final String condition = (lastCommonLetter < wordLength? removal: StringUtils.EMPTY);
		return new LineEntry(removal, addition, condition, word);
	}

	private int calculateLastCommonLetterIndex(final String word1, final String word2){
		int lastCommonLetter;
		final int minWordLength = Math.min(word1.length(), word2.length());
		for(lastCommonLetter = 0; lastCommonLetter < minWordLength; lastCommonLetter ++)
			if(word1.charAt(lastCommonLetter) != word2.charAt(lastCommonLetter))
				break;
		return lastCommonLetter;
	}

	public List<LineEntry> reduceRules(List<LineEntry> plainRules){
		List<LineEntry> compactedRules = redistributeAdditions(plainRules);

		compactedRules = compactRules(compactedRules);

		//reshuffle originating list to place the correct productions in the correct rule
		compactedRules = makeAdditionsDisjoint(compactedRules);

		compactedRules = disjoinConditions(compactedRules);

		mergeSimilarRules(compactedRules);

		return compactedRules;
	}

	private List<LineEntry> redistributeAdditions(final List<LineEntry> plainRules){
		final Map<String, LineEntry> map = new HashMap<>();
		for(final LineEntry entry : plainRules)
			for(final String addition : entry.addition){
				final String key = entry.removal + TAB + addition + TAB + entry.condition;
				final LineEntry newEntry = new LineEntry(entry.removal, addition, entry.condition, entry.from);
				final LineEntry rule = map.putIfAbsent(key, newEntry);
				if(rule != null)
					rule.from.addAll(entry.from);
			}

		return SetHelper.collect(map.values(),
			entry -> entry.removal + TAB + entry.condition + TAB + PatternHelper.mergeSet(entry.from, comparator),
			(rule, entry) -> rule.addition.addAll(entry.addition));
	}

	public List<String> convertFormat(final String flag, final boolean keepLongestCommonAffix, final List<LineEntry> compactedRules)
			throws IllegalArgumentException{
		final RuleEntry ruleToBeReduced = affixData.getData(flag);
		if(ruleToBeReduced == null)
			throw new IllegalArgumentException("Non-existent rule " + flag + ", cannot reduce");

		final AffixEntry.Type type = ruleToBeReduced.getType();
		final List<String> prettyPrintRules = convertEntriesToRules(flag, type, keepLongestCommonAffix, compactedRules);
		prettyPrintRules.add(0, composeHeader(type, flag, ruleToBeReduced.combinableChar(), prettyPrintRules.size()));
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
		final RuleEntry overriddenRule = new RuleEntry((type == AffixEntry.Type.SUFFIX), ruleToBeReduced.combinableChar(), entries);
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
		return SetHelper.collect(rules,
			entry -> entry.removal + TAB + PatternHelper.mergeSet(entry.addition, comparator) + TAB + entry.condition,
			(rule, entry) -> rule.from.addAll(entry.from));
	}

	private List<LineEntry> makeAdditionsDisjoint(final List<LineEntry> rules){
		//tranform
		//	[rem=èra,add=[ereta, ara, era, iera, ièra, areta, iereta],cond=èra,from=...]
		//	[rem=èra,add=[ereta, ara, era, areta],cond=èra,from=...]
		//into
		//	[rem=èra,add=[iera, ièra, iereta],    cond=èra,from=...]
		//	[rem=èra,add=[ereta, ara, era, areta],cond=èra,from=...]

		//transform
		//	[rem=ièr,add=[ar, areto, ereto, ier, èr, iar, iereto, er],cond=ièr,from=...]
		//into
		//	[rem= èr,add=[er, ar, ereto],           cond=ièr,from=...]
		//	[rem=ièr,add=[ar, areto, ereto, èr, er],cond=ièr,from=...]
		final List<LineEntry> disjointedRules = new ArrayList<>();

		for(final LineEntry rule : rules){
			final List<LineEntry> temporaryRules = new ArrayList<>();

			final Map<String, List<String>> lcss = SetHelper.bucket(rule.addition,
				add -> StringHelper.longestCommonPrefix(Arrays.asList(add, rule.removal)));
			if(lcss.size() > 1){
				//order keys from longer to shorter
				final List<String> keys = new ArrayList<>(lcss.keySet());
				keys.sort(Comparator.comparingInt(String::length).reversed());

				//add each key, remove the list from the addition
				final List<String> additionsToBeRemoved = new ArrayList<>();
				for(final String key : keys){
					final int keyLength = key.length();
					final String condition = rule.condition.substring(keyLength);
					if(condition.isEmpty())
						break;

					final String removal = (condition.length() <= rule.removal.length()? condition: rule.removal);
					final Set<String> addition = lcss.get(key).stream()
						.map(add -> add.substring(keyLength))
						.collect(Collectors.toSet());
					final LineEntry newEntry = new LineEntry(removal, addition, condition, rule.from);
					if(rules.contains(newEntry)){
						temporaryRules.add(newEntry);

						additionsToBeRemoved.addAll(lcss.get(key));
					}
				}

				temporaryRules.forEach(temporaryRule -> insertRuleOrUpdateFrom(disjointedRules, temporaryRule));
				rule.addition.removeAll(additionsToBeRemoved);
				if(!rule.addition.isEmpty())
					temporaryRules.clear();
			}

			if(temporaryRules.isEmpty())
				insertRuleOrUpdateFrom(disjointedRules, rule);
		}

		return disjointedRules;
	}

	private void insertRuleOrUpdateFrom(final List<LineEntry> expandedRules, final LineEntry rule){
		final int ruleIndex = expandedRules.indexOf(rule);
		if(ruleIndex >= 0)
			expandedRules.get(ruleIndex).from.addAll(rule.from);
		else{
			expandedRules.stream()
				.filter(expandedRule -> isContainedInto(expandedRule, rule))
				.forEach(expandedRule -> {
					rule.addition.removeAll(expandedRule.addition);
					expandedRule.from.addAll(rule.from);
				});
			expandedRules.add(rule);
		}
	}

	private List<LineEntry> disjoinConditions(final List<LineEntry> rules){
		//expand same conditions (if any); store surely disjoint rules
		final List<LineEntry> nonOverlappingRules = disjoinSameConditions(rules);

		//expand same ending conditions (if any); store surely disjoint rules
		nonOverlappingRules.addAll(disjoinSameEndingConditions(rules));

		return nonOverlappingRules;
	}

	private List<LineEntry> disjoinSameConditions(final List<LineEntry> rules){
		final List<LineEntry> finalRules = new ArrayList<>();

		//bucket by same condition
		final Map<String, List<LineEntry>> conditionBucket = SetHelper.bucket(rules, rule -> rule.condition);

		for(final Map.Entry<String, List<LineEntry>> entry : conditionBucket.entrySet()){
			final List<LineEntry> sameCondition = entry.getValue();
			//remove empty condition and multiple rules with the same condition
			if(entry.getKey().isEmpty() || sameCondition.size() > 1){
				final String condition = entry.getKey();

				//extract children
				final List<LineEntry> children = rules.stream()
					.filter(rule -> rule.condition.endsWith(condition))
					.collect(Collectors.toList());

				final Map<LineEntry, Set<Character>> groups = children.stream()
					.collect(Collectors.toMap(Function.identity(), child -> extractGroup(child.from, condition.length())));

				rules.removeAll(sameCondition);

				//separate conditions:
				//for each rule with same condition
				for(final LineEntry parent : sameCondition){
					//extract ratifying group
					final Set<Character> parentGroup = groups.get(parent);

					//extract negated group
					final List<LineEntry> childrenNotParent = children.stream()
						.filter(child -> child != parent)
						.collect(Collectors.toList());
					final Set<Character> childrenGroup = childrenNotParent.stream()
						.map(groups::get)
						.flatMap(Set::stream)
						.collect(Collectors.toSet());

					final Set<Character> groupsIntersection = SetHelper.intersection(parentGroup, childrenGroup);
					parentGroup.removeAll(groupsIntersection);
					if(!parentGroup.isEmpty()){
						final boolean chooseRatifyingOverNegated = chooseRatifyingOverNegated(parent.condition.length(), parentGroup, childrenGroup,
							groupsIntersection);
						final String preCondition = (chooseRatifyingOverNegated? PatternHelper.makeGroup(parentGroup, comparator):
							PatternHelper.makeNotGroup(childrenGroup, comparator));
						final LineEntry newRule = LineEntry.createFrom(parent, preCondition + parent.condition);
						finalRules.add(newRule);
					}

					//extract and add new condition only if there are rules that ends with the new condition
					final Set<Character> notPresentConditions = new HashSet<>();
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

					if(!notPresentConditions.isEmpty()){
						final String notCondition = PatternHelper.makeNotGroup(notPresentConditions, comparator) + parent.condition;
						final Optional<LineEntry> notRule = finalRules.stream()
							.filter(rule -> rule.condition.equals(notCondition))
							.findFirst();
						if(notRule.isPresent())
							notRule.get().condition = parent.condition;
						else{
							//find already present rule
							final List<LineEntry> alreadyPresentRules = finalRules.stream()
								.filter(r -> r.removal.equals(parent.removal) && r.addition.equals(parent.addition)
									&& r.condition.endsWith(parent.condition))
								.collect(Collectors.toList());
							for(final LineEntry alreadyPresentRule : alreadyPresentRules){
								finalRules.remove(alreadyPresentRule);

								notPresentConditions.addAll(parentGroup);
							}

							final LineEntry newEntry = LineEntry.createFrom(parent, PatternHelper.makeGroup(notPresentConditions, comparator)
								+ parent.condition);
							finalRules.add(newEntry);
						}
					}
					groupsIntersection.stream()
						.map(chr -> LineEntry.createFrom(parent, chr + parent.condition))
						.forEach(rules::add);
				}
			}
		}

		return finalRules;
	}

	private List<LineEntry> disjoinSameEndingConditions(final List<LineEntry> rules){
		final List<LineEntry> finalRules = new ArrayList<>();

		//bucket by condition ending
		final List<List<LineEntry>> forest = bucketByConditionEnding(rules);

		//for each bush in the forest
		for(final List<LineEntry> bush : forest){
			//if there is only one rule, then it goes in the final set
			if(bush.size() == 1){
				final LineEntry parent = bush.get(0);
				finalRules.add(parent);
			}
			//otherwise process the rules
			else{
				Iterator<LineEntry> itr = bush.iterator();
				while(itr.hasNext()){
					final LineEntry parent = itr.next();

					bush.remove(parent);

					final List<LineEntry> bubbles = extractBubbles(bush, parent);
					if(!bubbles.isEmpty()){
						//extract ratifying group
						final int parentConditionLength = parent.condition.length();
						final Set<Character> parentGroup = extractGroup(parent.from, parentConditionLength);

						//extract negated group
						final Set<Character> childrenGroup = bubbles.stream()
							.map(child -> child.condition.charAt(child.condition.length() - parentConditionLength - 1))
							.collect(Collectors.toSet());

						//if intersection(parent-group, children-group) is empty
						final Set<Character> groupsIntersection = SetHelper.intersection(parentGroup, childrenGroup);
						parentGroup.removeAll(groupsIntersection);

						//calculate new condition
						final boolean chooseRatifyingOverNegated = chooseRatifyingOverNegated(parentConditionLength, parentGroup, childrenGroup);
						final String preCondition = (chooseRatifyingOverNegated? PatternHelper.makeGroup(parentGroup, comparator):
							PatternHelper.makeNotGroup(childrenGroup, comparator));
						LineEntry newEntry = LineEntry.createFrom(parent, preCondition + parent.condition);

						//keep only rules that matches some existent words
						if(newEntry.isProductive())
							finalRules.add(newEntry);
						else{
							final List<LineEntry> newBushes = new ArrayList<>();
							final Iterator<LineEntry> itr2 = bush.iterator();
							while(itr2.hasNext()){
								final LineEntry child = itr2.next();
								if(parent.from.containsAll(child.from)){
									final Set<Character> childGroup = extractGroup(child.from, parentConditionLength + 1);
									childGroup.forEach(chr -> newBushes.add(LineEntry.createFrom(child, chr + child.condition)));

									itr2.remove();
								}
							}
							if(!newBushes.isEmpty()){
								bush.addAll(newBushes);
								bush.add(parent);
								bush.sort(shortestConditionComparator);
								itr = bush.iterator();

								continue;
							}

							LOGGER.debug("skip unused rule: {} {} {}", newEntry.removal, String.join("|", newEntry.addition),
								(newEntry.condition.isEmpty()? DOT: newEntry.condition));
						}

						final int maxConditionLength = bush.get(bush.size() - 1).condition.length();
						if(parentConditionLength + 1 >= maxConditionLength){
							bush.removeAll(bubbles);

							finalRules.addAll(bubbles);
						}
						else if(bush.stream().allMatch(rule -> rule.condition.length() > parentConditionLength + 1)){
							final List<LineEntry> bushes = new ArrayList<>(bush);
							bushes.add(parent);
							for(final Character chr : childrenGroup){
								newEntry = LineEntry.createFrom(parent, chr + parent.condition);
								if(!bush.contains(newEntry))
									bush.add(newEntry);
							}

							bush.sort(shortestConditionComparator);
						}
						else if(!groupsIntersection.isEmpty() && !parentGroup.isEmpty()){
							//expand intersection
							for(final Character chr : groupsIntersection){
								newEntry = LineEntry.createFrom(parent, chr + parent.condition);
								bush.add(newEntry);

								finalRules.addAll(disjoinSameConditions(bush));
							}

							bush.sort(shortestConditionComparator);
						}

						//continue until bubbles.condition length is reached
					}
					else
						finalRules.add(parent);

					itr = bush.iterator();
				}
			}
		}

		return finalRules;
	}

	private boolean chooseRatifyingOverNegated(final int parentConditionLength, final Set<Character> parentGroup,
			final Set<Character> childrenGroup){
		final int parentGroupSize = parentGroup.size();
		final int childrenGroupSize = childrenGroup.size();
		final boolean chooseRatifyingOverNegated = ((parentConditionLength == 0 || parentGroupSize == 1 && childrenGroupSize > 1)
			&& parentGroupSize != 0);
		return (chooseRatifyingOverNegated || parentGroupSize != 0 && childrenGroupSize == 0);
	}

	private boolean chooseRatifyingOverNegated(final int parentConditionLength, final Set<Character> parentGroup,
			final Set<Character> childrenGroup, final Set<Character> intersectionGroup){
		final int parentGroupSize = parentGroup.size();
		final int childrenGroupSize = childrenGroup.size();
		final boolean chooseRatifyingOverNegated = ((parentConditionLength == 0 || intersectionGroup.isEmpty())
			&& parentGroupSize <= childrenGroupSize && parentGroupSize != 0);
		return (chooseRatifyingOverNegated || parentGroupSize != 0 && childrenGroupSize == 0);
	}

	private List<List<LineEntry>> bucketByConditionEnding(final List<LineEntry> rules){
		rules.sort(shortestConditionComparator);

		List<List<LineEntry>> forest = new ArrayList<>();
		while(!rules.isEmpty()){
			//extract base condition
			final String parentCondition = rules.get(0).condition;
			
			//extract similar (same ending condition) rules from processing-list
			final List<LineEntry> children = extractSimilarRules(rules, parentCondition);
			forest.add(children);
		}
		return forest;
	}

	private List<LineEntry> extractSimilarRules(final List<LineEntry> rules, final String parentCondition){
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

	private List<LineEntry> extractBubbles(final List<LineEntry> bush, final LineEntry parent){
		final int parentConditionLength = parent.condition.length();
		final List<LineEntry> bubbles = bush.stream()
			.filter(child -> child.condition.length() > parentConditionLength && child.condition.endsWith(parent.condition))
			.collect(Collectors.toList());

		//if the bush contains a rule whose `from` is contained into this bubble, then remove the bubble
		bubbles.removeIf(bubble -> parent.from.containsAll(bubble.from)
			&& bubble.from.equals(new HashSet<>(parent.extractFromEndingWith(bubble.condition))));

		return bubbles;
	}

	private boolean isContainedInto(final LineEntry parent, final LineEntry child){
		final Set<String> parentBones = extractRuleSpine(parent);
		final Set<String> childBones = extractRuleSpine(child);
		return childBones.containsAll(parentBones);
	}

	private Set<String> extractRuleSpine(final LineEntry rule){
		final Set<String> parentBones = new HashSet<>();
		for(final String add : rule.addition){
			final int lcsLength = StringHelper.longestCommonPrefix(Arrays.asList(add, rule.removal))
				.length();
			parentBones.add(rule.removal.substring(lcsLength) + TAB + add.substring(lcsLength));
		}
		return parentBones;
	}

	private Set<Character> extractGroup(final Collection<String> words, final int indexFromLast){
		final Set<Character> group = new HashSet<>();
		for(final String word : words){
			final int index = word.length() - indexFromLast - 1;
			if(index < 0)
				throw new IllegalArgumentException("Cannot extract group from [" + StringUtils.join(words, ",") + "] at index " + indexFromLast
					+ " from last because of the presence of the word '" + word + "' that is too short");

			group.add(word.charAt(index));
		}
		return group;
	}

	/** Merge common conditions (ex. `[^a]bc` and `[^a]dc` will become `[^a][bd]c`) */
	private void mergeSimilarRules(final List<LineEntry> entries){
		final Map<String, List<LineEntry>> similarityBucket = SetHelper.bucket(entries, entry -> (entry.condition.contains(PatternHelper.GROUP_END)?
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
				final String condition = StringUtils.join(commonPreCondition) + PatternHelper.makeGroup(group, comparator)
					+ StringUtils.join(commonPostCondition);
				entries.add(LineEntry.createFrom(anEntry, condition));

				similarities.forEach(entries::remove);
			}
	}

	private List<String> convertEntriesToRules(final String flag, final AffixEntry.Type type, final boolean keepLongestCommonAffix,
			final Collection<LineEntry> entries){
		//restore original rules
		Stream<LineEntry> stream = entries.stream()
			.flatMap(rule -> rule.addition.stream()
				.map(addition -> {
					final int lcp = StringHelper.longestCommonPrefix(Arrays.asList(rule.removal, addition)).length();
					final String removal = rule.removal.substring(lcp);
					return new LineEntry((removal.isEmpty()? ZERO: removal), addition.substring(lcp), rule.condition, rule.from);
				}));
		if(type == AffixEntry.Type.PREFIX)
			stream = stream.map(this::createReverseOf);
		final List<LineEntry> restoredRules = stream
			.collect(Collectors.toList());

		final List<LineEntry> sortedEntries = prepareRules(keepLongestCommonAffix, restoredRules);

		return composeAffixRules(flag, type, sortedEntries);
	}

	private LineEntry createReverseOf(final LineEntry entry){
		final String removal = StringUtils.reverse(entry.removal);
		final Set<String> addition = entry.addition.stream()
			.map(add -> {
				final String[] additions = PatternHelper.split(add, SPLITTER_ADDITION);
				additions[0] = StringUtils.reverse(additions[0]);
				return String.join(StringUtils.EMPTY, additions);
			})
			.collect(Collectors.toSet());
		final String condition = SEQUENCER.toString(SEQUENCER.reverse(RegExpSequencer.splitSequence(entry.condition)));
		return new LineEntry(removal, addition, condition, Collections.emptyList());
	}

	private List<LineEntry> prepareRules(final boolean keepLongestCommonAffix, final Collection<LineEntry> entries){
		if(keepLongestCommonAffix)
			entries.forEach(this::expandConditionToMaxLength);

		final List<LineEntry> sortedEntries = new ArrayList<>(entries);
		sortedEntries.sort(lineEntryComparator);
		return sortedEntries;
	}

	private void expandConditionToMaxLength(final LineEntry entry){
		final String lcs = StringHelper.longestCommonSuffix(entry.from);
		if(lcs != null){
			final Set<Character> group = new HashSet<>();
			try{
				group.addAll(extractGroup(entry.from, lcs.length()));
			}
			catch(IllegalArgumentException ignored){}
			final int entryConditionLength = SEQUENCER.length(RegExpSequencer.splitSequence(entry.condition));
			if(lcs.length() + (group.isEmpty()? 0: 1) > entryConditionLength)
				entry.condition = PatternHelper.makeGroup(group, comparator) + lcs;
		}
	}

	private List<String> composeAffixRules(final String flag, final AffixEntry.Type type, final List<LineEntry> entries){
		return entries.stream()
			.map(entry -> composeLine(type, flag, entry))
			.collect(Collectors.toList());
	}

	private String composeHeader(final AffixEntry.Type type, final String flag, final char combinableChar, final int size){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		return sj.add(type.getTag().getCode())
			.add(flag)
			.add(Character.toString(combinableChar))
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
		final String line = type.getTag().getCode() + StringUtils.SPACE + flag + StringUtils.SPACE + partialLine.removal + StringUtils.SPACE
			+ addition + StringUtils.SPACE + (partialLine.condition.isEmpty()? DOT: partialLine.condition);
		return (idx >= 0? line + morphologicalRules: line);
	}

}
