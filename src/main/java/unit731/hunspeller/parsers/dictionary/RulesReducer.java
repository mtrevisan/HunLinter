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
			.thenComparingInt(entry -> StringUtils.countMatches(entry.condition, GROUP_END))
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
		List<LineEntry> compactedRules = compactRules(plainRules);

		LOGGER.info(Backbone.MARKER_APPLICATION, "Extracted {} rules from {} productions", compactedRules.size(),
			DictionaryParser.COUNTER_FORMATTER.format(plainRules.size()));

		compactedRules = removeOverlappingConditions(compactedRules);

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

	private List<LineEntry> removeOverlappingConditions(final List<LineEntry> rules/*, final AffixEntry.Type type*/){
		//reshuffle originating list to place the correct productions in the correct rule
//FIXME
AffixEntry.Type type = AffixEntry.Type.SUFFIX;
		redistributeAdditions(rules, type);

		final List<LineEntry> nonOverlappingRules = new ArrayList<>();

		//expand empty conditions (if any)
		//store surely disjoint rules
		nonOverlappingRules.addAll(expandEmptyCondition(rules));

		//expand same conditions (if any)
		//store surely disjoint rules
		nonOverlappingRules.addAll(expandSameCondition(rules));

		//subdivide rules by condition ending
		final List<List<LineEntry>> forest = bucketByConditionEnding(rules);

		//for each bush in the forest
		for(final List<LineEntry> bush : forest){
			//if there is only one rule, then it goes in the final set
			if(bush.size() == 1){
				final LineEntry parent = bush.get(0);
				nonOverlappingRules.add(parent);
			}
			//otherwise process the rules
			else{
				Iterator<LineEntry> itr = bush.iterator();
				while(itr.hasNext()){
					final LineEntry parent = itr.next();

final Map<LineEntry, Set<Character>> groups2 = bush.stream()
	.collect(Collectors.toMap(Function.identity(), child -> extractGroup(child.from, parent.condition.length())));
					bush.remove(parent);

					final int parentConditionLength = parent.condition.length();
					final List<LineEntry> bubbles = bush.stream()
						.filter(child -> child.condition.endsWith(parent.condition) && child.condition.length() > parentConditionLength)
						.collect(Collectors.toList());
					if(!bubbles.isEmpty()){
						//extract ratifying group
						final Set<Character> parentGroup = extractGroup(parent.from, parentConditionLength);

						//extract negated group
						final Set<Character> childrenGroup = bubbles.stream()
							.map(child -> child.condition.charAt(child.condition.length() - parentConditionLength - 1))
							.collect(Collectors.toSet());

						//if intersection(parent-group, children-group) is empty
						final Set<Character> groupIntersection = SetHelper.intersection(parentGroup, childrenGroup);
						parentGroup.removeAll(groupIntersection);

						//calculate new condition (if it was empty or the ratifying group is of cardinality 1, choose the ratifying over the negated)
						final String condition = (parentConditionLength == 0 || parentGroup.size() == 1 && childrenGroup.size() > 1?
							makeGroup(parentGroup): makeNotGroup(childrenGroup))
							+ parent.condition;
						LineEntry newEntry = LineEntry.createFrom(parent, condition);

						//keep only rules that matches some existent words
						if(newEntry.isProductive())
							nonOverlappingRules.add(newEntry);
						else
							LOGGER.debug("skip unused rule: {} {} {}", newEntry.removal, String.join("|", newEntry.addition),
								(newEntry.condition.isEmpty()? DOT: newEntry.condition));

						final int maxConditionLength = bush.get(bush.size() - 1).condition.length();
						if(parentConditionLength + 1 >= maxConditionLength){
							bush.removeAll(bubbles);

							nonOverlappingRules.addAll(bubbles);
						}
						else if(bush.stream().allMatch(rule -> rule.condition.length() > parentConditionLength + 1)){
							final List<LineEntry> bushes = new ArrayList<>(bush);
							bushes.add(parent);
							for(final Character chr : childrenGroup){
								final String cond = chr + parent.condition;
								newEntry = LineEntry.createFromWithRules(parent, cond, bushes);
								if(!bush.contains(newEntry))
									bush.add(newEntry);
							}

							bush.sort(shortestConditionComparator);
						}
						else if(!groupIntersection.isEmpty() && !parentGroup.isEmpty()){
							//expand intersection
							for(final Character chr : groupIntersection){
								final String cond = chr + parent.condition;
								newEntry = LineEntry.createFrom(parent, cond);
								bush.add(newEntry);

								nonOverlappingRules.addAll(expandSameCondition(bush));
							}

							bush.sort(shortestConditionComparator);
						}

						//continue until bubbles.condition length is reached
					}
					else
						nonOverlappingRules.add(parent);

					itr = bush.iterator();
				}



//				final Iterator<LineEntry> itr = bush.iterator();
//				while(itr.hasNext()){
//					final LineEntry parent = itr.next();
//
//					//extract the group of each bush of the forest
//					final int indexFromLast = parent.condition.length();
//					final Map<LineEntry, Set<Character>> groups = bush.stream()
//						.collect(Collectors.toMap(Function.identity(), child -> extractGroup(child.from, indexFromLast)));
//
//					//extract ratifying group
//					final Set<Character> parentGroup = groups.get(parent);
//
//					//extract negated group
//					Set<Character> childrenGroup = bush.stream()
//						.filter(child -> child != parent)
//						.map(groups::get)
//						.flatMap(Set::stream)
//						.collect(Collectors.toSet());
//
//					final Set<Character> groupsIntersection = SetHelper.intersection(parentGroup, childrenGroup);
//					if(groupsIntersection.isEmpty()){
////						final List<LineEntry> bubbles = extractRuleBubbles(bush);
////						if(!bubbles.isEmpty()){
////							final List<LineEntry> newRules = bubbleUpNotGroup(bubbles);
////System.out.println("");
////						}
//
//						if(!childrenGroup.isEmpty()){
//							//calculate new condition (if it was empty, choose the ratifying over the negated)
//							if(indexFromLast == 0){
//								for(final LineEntry rule : bush)
//									if(rule.condition.length() == indexFromLast)
//										rule.condition = makeGroup(groups.get(rule)) + rule.condition;
//							}
//							else{
//								final int maxConditionLength = bush.get(bush.size() - 1).condition.length() - 1;
//								for(final LineEntry rule : bush)
//									if(rule.condition.length() == indexFromLast){
//										childrenGroup = bush.stream()
//											.filter(child -> child != parent)
//											.map(groups::get)
//											.flatMap(Set::stream)
//											.collect(Collectors.toSet());
//										for(int i = indexFromLast; i < maxConditionLength; i ++){
//											final LineEntry newEntry = LineEntry.createFromWithWords(rule, /*addition.substring(lcp) +*/ rule.condition,
//												Collections.<String>emptyList());
//											nonOverlappingRules.add(newEntry);
//										}
//										rule.condition = makeNotGroup(childrenGroup) + rule.condition;
//									}
//							}
//						}
//
//						//add new rule to final-list
//						nonOverlappingRules.addAll(bush);
//
//						break;
//					}
//					else{
//						//TODO
//						for(final LineEntry child : bush){
//							final Set<Character> group = groups.get(child);
//							child.condition = makeGroup(group) + child.condition;
//							nonOverlappingRules.add(child);
//						}
////						throw new IllegalArgumentException("to do");
//					}
//				}
			}
		}



//		List<LineEntry> sortedRules = new ArrayList<>(rules);
//		//while processing-list is not empty
//		while(!sortedRules.isEmpty()){
//			//extract base condition
//			final LineEntry parent = sortedRules.get(0);
//
//			//extract similar (same ending condition) rules from processing-list
//			final List<LineEntry> children = sortedRules.stream()
//				.filter(rule -> rule.condition.endsWith(parent.condition))
//				.collect(Collectors.toList());
//
//			if(children.size() == 1)
//				//if there is only one rule that ends with the current condition, then accept it (and add to final-list if it's not already there)
//				children.stream()
//					.filter(Predicate.not(rules::contains))
//					.forEach(rules::add);
//			else{
//				//reshuffle originating list to place the correct productions in the correct rule
//				redistributeAdditions(children, type);
//
//				boolean wereNotIntersecting = false;
//				//extract minimum and maximum of the conditions' length
//				final int[] minAndMaxConditionLength = extractMinAndMax(children);
//				for(int index = minAndMaxConditionLength[0]; children.size() > 1 && index < minAndMaxConditionLength[1]; index ++){
//					//extract the group of each child
//					final int indexFromLast = index;
//					final Map<LineEntry, Set<Character>> groups = children.stream()
//						.collect(Collectors.toMap(Function.identity(), child -> extractGroup(child.from, indexFromLast)));
//
//					//calculate intersection between all groups
//					final Set<Character> groupsIntersection = SetHelper.intersection(groups.values());
//
//					//if there was no intersection on a previous run, then it means the conditions are already disjoint,
//					//so if there is no intersection again it can be skipped as already disjoint
//					if(wereNotIntersecting && groupsIntersection.isEmpty())
//						break;
//
////TODO
//					//if intersection is empty
//					if(groupsIntersection.isEmpty()){
//						//for each group, either is the group itself or the negated group of all the others children
//						//'Aèr': that is 'Aèr' or '[^Bi]èr'
//						//'Bèr': that is 'Bèr' or '[^Ai]èr'
//						//'ièr': that is 'ièr' or '[^AB]èr'
//						final Iterator<LineEntry> itr2 = children.iterator();
//						while(itr2.hasNext()){
//							final LineEntry child = itr2.next();
//
//							//process only smaller conditions
//							final int childConditionLength = child.condition.length();
//							if(childConditionLength <= index){
//								//extract ratifying group
//								final Set<Character> childGroup = groups.get(child);
//
//								//extract negated group (excluding the current group)
//								final Set<Character> childNotGroup = new HashSet<>();
//								for(final Set<Character> group : groups.values())
//									if(group != childGroup)
//										childNotGroup.addAll(group);
//
//								//calculate new condition (if it was empty or the ratifying group is smaller than the negated one, choose the ratifying)
//								final String condition = (childConditionLength == 0 /*|| childGroup.size() < childNotGroup.size()*/?
//									makeGroup(childGroup): makeNotGroup(childNotGroup))
//									+ (childConditionLength > 0? child.condition.substring(childConditionLength - index): child.condition);
//								final LineEntry newEntry = LineEntry.createFromWithWords(child, condition, child.from);
//								rules.add(newEntry);
//
//								//exclude from further processing
//								if(childConditionLength == index){
//									//remove from subsequent runs
//									itr2.remove();
//									//remove from final-list
//									rules.remove(child);
//									//remove from processing-list
//									sortedRules.remove(child);
//								}
//							}
//						}
//
//						//set condition is already disjoint (for further runs)
//						wereNotIntersecting = true;
//					}
//					//if the group intersects the negated group of all the other children
//					else{
//						//separate parent condition into belonging to not-intersection and to intersection
//						final int parentConditionLength = parent.condition.length();
//						final String notGroupIntersection = makeNotGroup(groupsIntersection, StringUtils.EMPTY);
//						final Map<String, List<String>> parentBucket = bucket(parent.from,
//							from -> {
//								char chr = from.charAt(from.length() - parentConditionLength - 1);
//								return (groupsIntersection.contains(chr)? String.valueOf(chr): notGroupIntersection);
//							});
//						final List<String> notGroupList = parentBucket.remove(notGroupIntersection);
//						if(notGroupList != null){
//							//remove parent from final list
//							rules.remove(parent);
//							children.remove(parent);
//							sortedRules.remove(parent);
//
//							final Set<Character> preCondition = extractGroup(notGroupList, parentConditionLength);
//							final String condition = (parent.condition.isEmpty()
//									|| !groupsIntersection.containsAll(preCondition) && preCondition.size() < groupsIntersection.size()?
//								makeGroup(preCondition): makeNotGroup(groupsIntersection)) + parent.condition;
//if("[^ò]o".equals(condition))
//	System.out.println("");
//							final LineEntry newEntry = LineEntry.createFromWithWords(parent, condition, notGroupList);
//							rules.add(newEntry);
//						}
//						for(final Map.Entry<String, List<String>> entry : parentBucket.entrySet()){
//							final String condition = entry.getKey() + parent.condition;
//							final LineEntry newEntry = LineEntry.createFromWithWords(parent, condition, entry.getValue());
//							children.add(newEntry);
//						}
//
//						children.sort(shortestConditionComparator);
//
//						//TODO keep only intersecting rules?
//
////						throw new IllegalArgumentException("and now?");
//
////						break;
//					}
//				}
//			}
//
//			children.forEach(sortedRules::remove);


//			final Set<String> childrenFrom = children.stream()
//				.flatMap(entry -> entry.from.stream())
//				.collect(Collectors.toSet());
			//find children-group
//			final Set<Character> childrenGroup = extractGroup(childrenFrom, parentConditionLength);
//
//			//if intersection(parent-group, children-group) is empty
//			final Set<Character> groupIntersection = SetHelper.intersection(parentGroup, childrenGroup);
//			if(groupIntersection.isEmpty()){
//				...
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
//					final LineEntry newEntry = LineEntry.createFromWithWords(parent, condition, notGroupList);
//					rules.add(newEntry);
//				}
//				for(final Map.Entry<String, List<String>> entry : fromBucket.entrySet()){
//					final String condition = entry.getKey() + parent.condition;
//					final LineEntry newEntry = LineEntry.createFromWithWords(parent, condition, entry.getValue());
//					sortedList.add(newEntry);
//				}
//
//				sortedList.sort(shortestConditionComparator);
//			}

//			sortedList.remove(0);

			//remove parent from final list
//			rules.remove(parent);
//		}

		return nonOverlappingRules;
	}

	private List<LineEntry> extractBubbles(final LineEntry parent, final List<LineEntry> rules){
		final int parentConditionLength = parent.condition.length();
		return rules.stream()
			.filter(rule -> rule.condition.length() > parentConditionLength)
			.collect(Collectors.toList());
	}

	private List<LineEntry> bubbleUpNotGroup(final LineEntry parent, final List<LineEntry> rules){
		final int parentConditionLength = parent.condition.length();
		//bubble up by bucketing children for group-2
		final Set<String> bubblesCondition = rules.stream()
			.map(rule -> rule.condition)
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
		final List<LineEntry> newParents = new ArrayList<>();
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
				final List<String> gg = rules.stream()
					.filter(entry -> entry.condition.equals(c))
					.flatMap(entry -> entry.from.stream())
					.collect(Collectors.toList());
				bucket(gg, cond -> cond.substring(cond.length() - parentConditionLength - 1));
			}
			final Set<Character> commonGroup = extractGroup(comm, e.getKey().length());
			final String condition = makeNotGroup(commonGroup, e.getKey());
			final LineEntry newEntry = LineEntry.createFrom(parent, condition);
			//keep only rules that matches some existent words
			if(newEntry.isProductive())
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
				final LineEntry newEntry = LineEntry.createFrom(parent, condition);
				//keep only rules that matches some existent words
				if(newEntry.isProductive())
					newParents.add(newEntry);
				else
					LOGGER.debug("skip unused rule: {} {} {}", newEntry.removal, String.join("|", newEntry.addition),
						(newEntry.condition.isEmpty()? DOT: newEntry.condition));
			}
		}
		return newParents;
	}

	private List<List<LineEntry>> bucketByConditionEnding(final List<LineEntry> sortedRules){
		List<List<LineEntry>> forest = new ArrayList<>();
		while(!sortedRules.isEmpty()){
			//extract base condition
			final String parentCondition = sortedRules.get(0).condition;
			
			//extract similar (same ending condition) rules from processing-list
			final List<LineEntry> children = new ArrayList<>();
			final Iterator<LineEntry> itr = sortedRules.iterator();
			while(itr.hasNext()){
				final LineEntry rule = itr.next();

				if(rule.condition.endsWith(parentCondition)){
					children.add(rule);
					itr.remove();
				}
			}
			forest.add(children);
		}
		return forest;
	}

	private List<LineEntry> expandEmptyCondition(final List<LineEntry> rules){
		final List<LineEntry> finalRules = new ArrayList<>();
		final LineEntry emptyConditionParent = rules.get(0);
		if(emptyConditionParent.condition.isEmpty()){
			//collect empty conditions
			final List<LineEntry> parents = new ArrayList<>();
			final Iterator<LineEntry> itr = rules.iterator();
			while(itr.hasNext()){
				final LineEntry parent = itr.next();
				if(parent.condition.isEmpty()){
					parents.add(parent);
					itr.remove();
				}
			}

			final Set<String> otherFrom = rules.stream()
				.flatMap(rule -> rule.from.stream())
				.collect(Collectors.toSet());
			final Set<Character> otherGroup = extractGroup(otherFrom, 0);

			//for each rule with empty condition
			for(final LineEntry parent : parents){
				//expand empty condition:
				final Set<Character> parentGroup = extractGroup(parent.from, 0);
				final Set<Character> intersection = SetHelper.intersection(parentGroup, otherGroup);
				parentGroup.removeAll(intersection);
				if(!parentGroup.isEmpty()){
					final String condition = makeGroup(parentGroup);
					final LineEntry newRule = LineEntry.createFrom(parent, condition);
					finalRules.add(newRule);
				}
				for(final Character chr : intersection){
					final String condition = String.valueOf(chr);
					final LineEntry newRule = LineEntry.createFrom(parent, condition);
					rules.add(newRule);
				}
			}
			rules.sort(shortestConditionComparator);
		}
		return finalRules;
	}

	private List<LineEntry> expandSameCondition(final List<LineEntry> rules){
		final List<LineEntry> finalRules = new ArrayList<>();

		//bucket by same condition
		final Map<String, List<LineEntry>> conditionBucket = bucket(rules, rule -> rule.condition);
		for(final Map.Entry<String, List<LineEntry>> entry : conditionBucket.entrySet()){
			final List<LineEntry> sameCondition = entry.getValue();
			if(sameCondition.size() > 1){
				//extract children
				final String condition = entry.getKey();
				final List<LineEntry> children = rules.stream()
					.filter(rule -> rule.condition.endsWith(condition))
					.collect(Collectors.toList());

				final Map<LineEntry, Set<Character>> groups = children.stream()
					.collect(Collectors.toMap(Function.identity(), child -> extractGroup(child.from, condition.length())));

				//separate conditions:
				//for each rule with empty condition
				for(final LineEntry parent : sameCondition){
					//extract ratifying group
					final Set<Character> parentGroup = groups.get(parent);

					//extract negated group
					final Set<Character> childrenGroup = children.stream()
						.filter(child -> child != parent)
						.map(groups::get)
						.flatMap(Set::stream)
						.collect(Collectors.toSet());

					final Set<Character> groupsIntersection = SetHelper.intersection(parentGroup, childrenGroup);
					parentGroup.removeAll(groupsIntersection);
					if(!parentGroup.isEmpty()){
						final String cond = (groupsIntersection.isEmpty() && (parent.condition.isEmpty() || parentGroup.size() <= childrenGroup.size())?
							makeGroup(parentGroup): makeNotGroup(childrenGroup)) + parent.condition;
						final LineEntry newRule = LineEntry.createFrom(parent, cond);
						finalRules.add(newRule);
					}
					for(final Character chr : groupsIntersection){
						final String cond = String.valueOf(chr);
						final LineEntry newRule = LineEntry.createFrom(parent, cond);
						rules.add(newRule);
					}
				}
				rules.removeAll(sameCondition);
			}
		}

		return finalRules;
	}

	/** Reshuffle originating list to place the correct productions in the correct rule */
	private void redistributeAdditions(final List<LineEntry> rules, AffixEntry.Type type){
		//sort processing-list by shortest condition
		rules.sort(shortestConditionComparator);

		//cycle parent in all the children
		for(final LineEntry parent : rules){
			//extract raw additions from parent
			final Set<String> parentAdditions = parent.addition.stream()
				.map(addition -> {
					final String lcs = longestCommonAffix(Arrays.asList(addition, parent.removal),
						(type == AffixEntry.Type.SUFFIX? this::commonPrefix: this::commonSuffix));
					return addition.substring(lcs.length());
				})
				.collect(Collectors.toSet());
			for(final LineEntry child : rules)
				if(child != parent && child.removal.equals(parent.removal)){
					//extract raw additions from child
					int minimumLCSLength = Integer.MAX_VALUE;
					final Set<String> childAdditions = new HashSet<>();
					for(final String addition : child.addition){
						final int lcsLength = longestCommonAffix(Arrays.asList(addition, child.removal),
							(type == AffixEntry.Type.SUFFIX? this::commonPrefix: this::commonSuffix))
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
		return (group.size() > 1? GROUP_START + merge + GROUP_END: merge);
	}

	private String makeGroup(final Set<Character> group, final String suffix){
		return makeGroup(group) + suffix;
	}

	private String makeNotGroup(final Character group){
		return NOT_GROUP_START + group + GROUP_END;
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
				entries.add(LineEntry.createFrom(anEntry, condition));

				similarities.forEach(entries::remove);
			}
	}

	private List<String> convertEntriesToRules(final String flag, final AffixEntry.Type type, final boolean keepLongestCommonAffix,
			final Collection<LineEntry> entries){
		//restore original rules
		final List<LineEntry> restoredRules = entries.stream()
			.flatMap(rule -> rule.addition.stream()
				.map(addition -> {
					final int lcp = commonPrefix(rule.removal, addition).length();
					final String removal = rule.removal.substring(lcp);
					return new LineEntry((removal.isEmpty()? ZERO: removal), addition.substring(lcp), rule.condition, rule.from);
				}))
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
		String line = type.getTag().getCode() + StringUtils.SPACE + flag + StringUtils.SPACE + partialLine.removal + StringUtils.SPACE
			+ addition + StringUtils.SPACE + (partialLine.condition.isEmpty()? DOT: partialLine.condition);
		if(idx >= 0)
			line += morphologicalRules;
		return line;
	}

}
