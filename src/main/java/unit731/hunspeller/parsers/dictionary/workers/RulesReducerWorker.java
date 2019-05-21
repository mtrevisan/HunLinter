package unit731.hunspeller.parsers.dictionary.workers;

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
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerData;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;
import unit731.hunspeller.services.PatternHelper;
import unit731.hunspeller.services.SetHelper;


public class RulesReducerWorker extends WorkerDictionaryBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesReducerWorker.class);

	public static final String WORKER_NAME = "Rules reducer";

	private static final RegExpSequencer SEQUENCER = new RegExpSequencer();

	private static final String ZERO = "0";
	private static final String TAB = "\t";
	private static final String DOT = ".";
	private static final String NOT_GROUP_START = "[^";
	private static final String GROUP_START = "[";
	private static final String GROUP_END = "]";
	private static final String PATTERN_END_OF_WORD = "$";


	private static class LineEntry implements Serializable{

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


	private FlagParsingStrategy strategy;
	private Comparator<String> comparator;
	private final Comparator<LineEntry> shortestConditionComparator = Comparator.comparingInt(entry -> entry.condition.length());
	private Comparator<LineEntry> lineEntryComparator;


	public RulesReducerWorker(final String flag, final boolean keepLongestCommonAffix, final AffixData affixData, final DictionaryParser dicParser,
			final WordGenerator wordGenerator){
		Objects.requireNonNull(flag);
		Objects.requireNonNull(affixData);
		Objects.requireNonNull(wordGenerator);

		strategy = affixData.getFlagParsingStrategy();
		comparator = BaseBuilder.getComparator(affixData.getLanguage());
		lineEntryComparator = Comparator.comparingInt((LineEntry entry) -> RegExpSequencer.splitSequence(entry.condition).length)
			.thenComparing(Comparator.comparingInt(entry -> StringUtils.countMatches(entry.condition, GROUP_END)))
			.thenComparing(Comparator.comparing(entry -> StringUtils.reverse(entry.condition), comparator))
			.thenComparing(Comparator.comparingInt(entry -> entry.removal.length()))
			.thenComparing(Comparator.comparing(entry -> entry.removal, comparator))
			.thenComparing(Comparator.comparingInt(entry -> entry.anAddition().length()))
			.thenComparing(Comparator.comparing(entry -> entry.anAddition(), comparator));

		final RuleEntry ruleToBeReduced = affixData.getData(flag);
		if(ruleToBeReduced == null)
			throw new IllegalArgumentException("Non-existent rule " + flag + ", cannot reduce");

		final AffixEntry.Type type = ruleToBeReduced.getType();

		final List<String> originalLines = new ArrayList<>();
		final List<LineEntry> plainRules = new ArrayList<>();
		final BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			final List<Production> productions = wordGenerator.applyAffixRules(line);

			final LineEntry compactedFilteredRule = collectProductionsByFlag(productions, flag, type);
			if(compactedFilteredRule != null){
				originalLines.add(line);
				plainRules.add(compactedFilteredRule);
			}
		};
		final Runnable completed = () -> {
			try{
				LOGGER.info(Backbone.MARKER_APPLICATION, "Extracted {} rules", plainRules.size());

				final List<LineEntry> compactedRules = compactRules(plainRules);

				removeOverlappingConditions(compactedRules);

				final List<String> rules = convertEntriesToRules(flag, type, keepLongestCommonAffix, compactedRules);


				checkReductionCorrectness(rules, originalLines, wordGenerator, flag, ruleToBeReduced, plainRules);


				LOGGER.info(Backbone.MARKER_RULE_REDUCER, composeHeader(type, flag, ruleToBeReduced.combineableChar(), rules.size()));
				for(final String rule : rules)
					LOGGER.info(Backbone.MARKER_RULE_REDUCER, rule);
			}
			catch(final Exception e){
				LOGGER.info(Backbone.MARKER_RULE_REDUCER, e.getMessage());

				e.printStackTrace();
			}
		};
//		final WorkerData data = WorkerData.createParallel(WORKER_NAME, dicParser);
final WorkerData data = WorkerData.create(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		createReadWorker(data, lineProcessor);
	}

	private void checkReductionCorrectness(final List<String> rules, final List<String> originalLines, final WordGenerator wordGenerator,
			final String flag, final RuleEntry ruleToBeReduced, final List<LineEntry> plainRules) throws IllegalArgumentException{
		final Set<LineEntry> checkRules = new HashSet<>();
		final List<AffixEntry> entries = rules.stream()
			.map(line -> new AffixEntry(line, strategy, null, null))
			.collect(Collectors.toList());
		final AffixEntry.Type type = ruleToBeReduced.getType();
		final RuleEntry overriddenRule = new RuleEntry((type == AffixEntry.Type.SUFFIX), ruleToBeReduced.combineableChar(), entries);
		for(String line : originalLines){
			final List<Production> productions = wordGenerator.applyAffixRules(line, overriddenRule);

			final LineEntry compactedFilteredRule = collectProductionsByFlag(productions, flag, type);
			checkRules.add(compactedFilteredRule);
		}
		if(!checkRules.equals(new HashSet<>(plainRules))){
for(final String rule : rules)
	LOGGER.info(Backbone.MARKER_RULE_REDUCER, rule);
			throw new IllegalArgumentException("Something very bad occurs while reducing");
		}
	}

	private LineEntry collectProductionsByFlag(final List<Production> productions, final String flag, final AffixEntry.Type type){
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
			final String parentGroup = extractGroup(parent.from, parentConditionLength);

			final Set<String> childrenFrom = children.stream()
				 .flatMap(entry -> entry.from.stream())
				 .collect(Collectors.toSet());
			//find children-group
			final String childrenGroup = extractGroup(childrenFrom, parentConditionLength);

			//if intersection(parent-group, children-group) is empty
			final Set<Character> parentGroupSet = SetHelper.makeCharacterSetFrom(parentGroup);
			final Set<Character> childrenGroupSet = SetHelper.makeCharacterSetFrom(childrenGroup);
			final Set<Character> groupIntersection = SetHelper.intersection(parentGroupSet, childrenGroupSet);
			if(groupIntersection.isEmpty()){
				//add new rule from parent with condition starting with NOT(children-group) to final-list
				String condition = (parent.condition.isEmpty()? makeGroup(parentGroup): makeNotGroup(childrenGroup) + parent.condition);
				LineEntry newEntry = LineEntry.createFrom(parent, condition, parent.from);
				rules.add(newEntry);

				//if parent.condition is not empty
				if(!parent.condition.isEmpty()){
					final List<LineEntry> bubbles = extractRuleBubbles(parent, children);
					//if can-bubble-up
					if(!bubbles.isEmpty()){
						final List<LineEntry> bubbledRules = bubbleUpNotGroup(parent, bubbles);
						rules.addAll(bubbledRules);

						//remove bubbles from current-list
						bubbles.forEach(sortedList::remove);
					}
				}

				final List<LineEntry> sameConditionChildren = children.stream()
					.filter(entry -> !entry.condition.isEmpty() && entry.condition.equals(parent.condition))
					.collect(Collectors.toList());
				//for each children-same-condition
				for(final LineEntry child : sameConditionChildren){
					//add new rule from child with condition starting with (child-group) to final-list
					final String childGroup = extractGroup(child.from, parentConditionLength);
					condition = makeGroup(childGroup) + child.condition;
					newEntry = LineEntry.createFrom(child, condition, child.from);
					rules.add(newEntry);
				}

				//remove same-condition-children from current-list
				sameConditionChildren.forEach(sortedList::remove);
				//remove same-condition-children from final-list
				sameConditionChildren.forEach(rules::remove);
			}
			else{
				//create new (parent) rule with condition as parent.condition \ intersection
				boolean groupChanged = parentGroupSet.removeAll(groupIntersection);
				if(groupChanged && !parentGroupSet.isEmpty()){
					String condition = makeGroup(mergeSet(parentGroupSet)) + parent.condition;
					final Pattern conditionPattern = PatternHelper.pattern(condition + PATTERN_END_OF_WORD);
					final List<String> words = parent.from.stream()
						.filter(from -> PatternHelper.find(from, conditionPattern))
						.collect(Collectors.toList());
					LineEntry newEntry = LineEntry.createFrom(parent, condition, words);
					sortedList.add(newEntry);

					//calculate intersection of the parent
					parent.from.removeAll(words);
					condition = makeGroup(mergeSet(groupIntersection)) + parent.condition;
					newEntry = LineEntry.createFrom(parent, condition, parent.from);
					sortedList.add(newEntry);
					rules.add(newEntry);
				}
				else if(parent.condition.isEmpty()){
					final String condition = extractGroup(parent.from, parentConditionLength) + parent.condition;
					final LineEntry newEntry = LineEntry.createFrom(parent, condition, parent.from);
					sortedList.add(newEntry);
					rules.add(newEntry);
				}

				//create new (child) rule with condition as child.condition \ intersection
				for(LineEntry child : children){
					final String childGroup = extractGroup(child.from, parentConditionLength);
					final Set<Character> childGroupSet = SetHelper.makeCharacterSetFrom(childGroup);
					groupChanged = childGroupSet.removeAll(groupIntersection);
					if(groupChanged && !childGroupSet.isEmpty()){
						String condition = makeGroup(mergeSet(childGroupSet)) + parent.condition;
						final Pattern conditionPattern = PatternHelper.pattern(condition + PATTERN_END_OF_WORD);
						final List<String> words = child.from.stream()
							.filter(from -> PatternHelper.find(from, conditionPattern))
							.collect(Collectors.toList());
						LineEntry newEntry = LineEntry.createFrom(child, condition, words);
						sortedList.add(newEntry);

						//calculate intersection of the child
						child.from.removeAll(words);
						condition = makeGroup(mergeSet(groupIntersection)) + parent.condition;
						newEntry = LineEntry.createFrom(child, condition, child.from);
						sortedList.add(newEntry);
						sortedList.remove(child);
					}
				}
				sortedList.sort(shortestConditionComparator);

				//what to do with the intersection?
System.out.println("");
			}
//			//if parent.condition is empty
//			else if(parent.condition.isEmpty()){
//				//for each last-char of parent.from
//				final Map<Character, List<String>> fromBucket = bucket(parent.from, from -> from.charAt(from.length() - 1));
//				for(final Map.Entry<Character, List<String>> entry : fromBucket.entrySet()){
//					final Character key = entry.getKey();
//					//if last-char is contained into intersection
//					if(groupIntersection.contains(key)){
//						//add new rule from parent with condition the last-char
//						final LineEntry newEntry = LineEntry.createFrom(parent, String.valueOf(key), entry.getValue());
//						sortedList.add(newEntry);
//					}
//				}
//				sortedList.sort(shortestConditionComparator);
//
//				//if intersection is proper subset of parent-group
//				parenGroupSet.removeAll(groupIntersection);
//				if(!parenGroupSet.isEmpty()){
//					//add new rule from parent with condition the difference between parent-grop and intersection to final-list
//					final String condition = makeGroup(mergeSet(parenGroupSet));
//					final Pattern conditionPattern = PatternHelper.pattern(condition + PATTERN_END_OF_WORD);
//					final List<String> words = parent.from.stream()
//						.filter(from -> PatternHelper.find(from, conditionPattern))
//						.collect(Collectors.toList());
//					final LineEntry newEntry = LineEntry.createFrom(parent, condition, words);
//					//keep only rules that matches some existent words
//					if(!words.isEmpty())
//						rules.add(newEntry);
//					else
//						LOGGER.debug("skip unused rule: {} {} {}", newEntry.removal, String.join("|", newEntry.addition),
//							(newEntry.condition.isEmpty()? DOT: newEntry.condition));
//				}
//			}
//			else{
//				//FIXME really ugly!!
//
//				//calculate intersection between parent and children conditions
//				final Map<Boolean, List<LineEntry>> conditionBucket = bucket(children, rule -> rule.condition.equals(parent.condition));
//				//if intersection is empty
//				if(!conditionBucket.containsKey(Boolean.TRUE) && conditionBucket.containsKey(Boolean.FALSE))
//					ahn(parent, parentGroup, children, childrenGroup, sortedList, rules);
//				else if(conditionBucket.containsKey(Boolean.TRUE) && !conditionBucket.containsKey(Boolean.FALSE)){
//					final List<LineEntry> t = conditionBucket.get(Boolean.TRUE);
//					if(t.size() == 1){
//						//check if removal == 0 && exists a rule in children that have another-rule.removal = removal+condition and
//						//another-rule.condition == condition
//						final LineEntry te = t.get(0);
//						final List<LineEntry> list = new ArrayList<>(children);
//						list.add(parent);
//						final List<LineEntry> as = list.stream()
//							.filter(rule -> rule.removal.equals((te.removal.equals(ZERO)? te.condition: te.removal + te.condition))
//								&& rule.condition.equals(te.condition))
//							.collect(Collectors.toList());
//						if(as.size() == 1 && as.get(0).from.equals(te.from)){
//							as.get(0).addition.addAll(te.addition.stream().map(add -> te.condition + add).collect(Collectors.toList()));
//
//							rules.add(as.get(0));
//							sortedList.remove(te);
//							sortedList.add(parent);
//							sortedList.sort(shortestConditionComparator);
//						}
//						else
//							throw new IllegalArgumentException("yet to be coded! (1)");
//					}
//					else
//						throw new IllegalArgumentException("yet to be coded! (2)");
//				}
//				else if(conditionBucket.isEmpty()){
//					throw new IllegalArgumentException("yet to be coded! (3)");
//				}
//				else{
////do nothing (?)
//					throw new IllegalArgumentException("do nothing?");
//				}
//			}

			//remove parent from final list
			rules.remove(parent);

/*
sort current-list by shortest condition
while current-list is not empty{
	extract rule from current-list
	find parent-group
	find children-group
	if intersection(parent-group, children-group) is empty{
		add new rule from parent with condition starting with NOT(children-group) to final-list

		if parent.condition is not empty and can-bubble-up{
			bubble up by bucketing children for group-2
			for each children-group-2
				add new rule from parent with condition starting with NOT(children-group-1) to final-list

			remove bubbles from current-list
		}

		for each children-same-condition
			add new rule from child with condition starting with (child-group) to final-list

		remove same-condition-children from current-list
		remove same-condition-children from final-list
	}
	else if parent.condition is empty{
		for each last-char of parent.from
			if last-char is contained into intersection
				add new rule from parent with condition the last-char

		if intersection is proper subset of parent-group{
			add new rule from parent with condition the difference between parent-grop and intersection to final-list
			keep only rules that matches some existent words
		}
	}
	else{
		?
//		calculate intersection between parent and children conditions
//		if intersection is empty
//			check if removal == 0 && exists a rule in children that have another-rule.removal = removal+condition and another-rule.condition == condition
	}

	remove parent from final-list
}
*/
		}
	}

	private void ahn(final LineEntry parent, final String parentGroup, final List<LineEntry> children, final String childrenGroup,
			final List<LineEntry> sortedList, final List<LineEntry> rules){
		//add new rule from parent with condition starting with NOT(children-group) to final-list
		final String condition = (parent.condition.isEmpty()? makeGroup(parentGroup): makeNotGroup(childrenGroup) + parent.condition);
		final LineEntry newEntry = LineEntry.createFrom(parent, condition, parent.from);
		rules.add(newEntry);

		//if parent.condition is not empty
		if(!parent.condition.isEmpty()){
			final List<LineEntry> bubbles = extractRuleBubbles(parent, children);
			//if can-bubble-up
			if(!bubbles.isEmpty()){
				final List<LineEntry> bubbledRules = bubbleUpNotGroup(parent, bubbles);
				rules.addAll(bubbledRules);

				//remove bubbles from current-list
				bubbles.forEach(sortedList::remove);
			}
		}
	}

	private List<LineEntry> extractRuleBubbles(final LineEntry parent, final List<LineEntry> sortedList){
		final int parentConditionLength = parent.condition.length();
		return sortedList.stream()
			.filter(entry -> entry.condition.length() > parentConditionLength + 1)
			.collect(Collectors.toList());
	}

	private List<LineEntry> bubbleUpNotGroup(final LineEntry parent, final List<LineEntry> children){
		final int parentConditionLength = parent.condition.length();
		//bubble up by bucketing children for group-2
		final List<String> bubblesCondition = children.stream()
			.map(entry -> entry.condition)
			.collect(Collectors.toList());

		/*
		extract communalities:
		from
		"ò => [òdo, òco, òko]"
		"è => [èdo, èđo, èxo]"
		transform into
		"ò => [òco, òko]"
		"è => [èđo, èxo]"
		"òè => [òdo, èdo]"
		so add condition
		'[^èò]do'
		*/
		final List<LineEntry> newParents = new ArrayList<>();
		final Map<String, List<String>> communalitiesBucket = bucket(bubblesCondition,
			cond -> cond.substring(cond.length() - parentConditionLength - 1));
		for(final Map.Entry<String, List<String>> e : communalitiesBucket.entrySet()){
			final List<String> comm = e.getValue();
			if(comm.size() > 1){
				//FIXME
				if(e.getKey().length() + 1 != comm.get(0).length() || !comm.stream().allMatch(c -> c.length() == comm.get(0).length()))
					throw new IllegalArgumentException("e.key.length + 1 != comm[0].length || comm.get(.).length() differs: key '" + e.getKey()
						+ "', comm '" + comm.toString());

				final String commonGroup = extractGroup(comm, e.getKey().length());
				final String condition = makeNotGroup(commonGroup)
					+ e.getKey();
				final Pattern conditionPattern = PatternHelper.pattern(condition + PATTERN_END_OF_WORD);
				final List<String> words = parent.from.stream()
					.filter(from -> PatternHelper.find(from, conditionPattern))
					.collect(Collectors.toList());
				final LineEntry newEntry = LineEntry.createFrom(parent, condition, words);
				//keep only rules that matches some existent words
				if(!words.isEmpty())
					newParents.add(newEntry);
				else
					LOGGER.debug("skip unused rule: {} {} {}", newEntry.removal, String.join("|", newEntry.addition),
						(newEntry.condition.isEmpty()? DOT: newEntry.condition));

				comm.forEach(bubblesCondition::remove);
			}
		}

		final Map<String, List<String>> conditionBucket = bucket(bubblesCondition,
			cond -> cond.substring(0, cond.length() - parentConditionLength - 1));
		//for each children-group-2
		for(final Map.Entry<String, List<String>> conds : conditionBucket.entrySet()){
			//add new rule from parent with condition starting with NOT(children-group-2) to final-list
			final String bubbleGroup = extractGroup(conds.getValue(), parentConditionLength);
			//do the bubble trick
			for(int i = conds.getKey().length(); i > 0; i --){
				final String condition = makeNotGroup(conds.getKey().substring(i - 1, i))
					+ conds.getKey().substring(i)
					+ makeGroup(bubbleGroup)
					+ parent.condition;
				final Pattern conditionPattern = PatternHelper.pattern(condition + PATTERN_END_OF_WORD);
				final List<String> words = parent.from.stream()
					.filter(from -> PatternHelper.find(from, conditionPattern))
					.collect(Collectors.toList());
				final LineEntry newEntry = LineEntry.createFrom(parent, condition, words);
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

	private String mergeSet(final Set<Character> set){
		return set.stream()
			.map(String::valueOf)
			.sorted(comparator)
			.collect(Collectors.joining());
	}

	private String extractGroup(final Collection<String> words, final int indexFromLast){
		final Set<String> group = new HashSet<>();
		for(String word : words){
			final int index = word.length() - indexFromLast - 1;
			if(index < 0)
				throw new IllegalArgumentException("Cannot extract group from [" + StringUtils.join(words, ",")
					+ "] at index " + indexFromLast + " from last because of the presence of the word " + word + " that is too short");

			group.add(String.valueOf(word.charAt(index)));
		}
		return group.stream()
			.sorted(comparator)
			.collect(Collectors.joining(StringUtils.EMPTY));
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

	private String makeGroup(final String group){
		return (group.length() > 1? GROUP_START + group + GROUP_END: group);
	}

	private String makeNotGroup(final String group){
		return NOT_GROUP_START + group + GROUP_END;
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

	private List<String> composeAffixRules(final String flag, final AffixEntry.Type type, final List<LineEntry> entries){
		final int size = entries.size();
		final List<String> rules = new ArrayList<>(size);
		for(LineEntry entry : entries)
			rules.add(composeLine(type, flag, entry));
		return rules;
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

}
