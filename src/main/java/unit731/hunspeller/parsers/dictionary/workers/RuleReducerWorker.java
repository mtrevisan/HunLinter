package unit731.hunspeller.parsers.dictionary.workers;

import java.io.Serializable;
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
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
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
import unit731.hunspeller.services.SetHelper;


public class RuleReducerWorker extends WorkerDictionaryBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(RuleReducerWorker.class);

	public static final String WORKER_NAME = "Rule reducer";

	private static final RegExpSequencer SEQUENCER = new RegExpSequencer();

	private static final String TAB = "\t";
	private static final String DOT = ".";
	private static final String NOT_GROUP_START = "[^";
	private static final String GROUP_START = "[";
	private static final String GROUP_END = "]";


	private static class LineEntry implements Serializable{

		private static final long serialVersionUID = 8374397415767767436L;

		private final Set<String> from;

		private final String removal;
		private final Set<String> addition;
		private String condition;


		public static LineEntry createFrom(LineEntry entry, String condition){
			return new LineEntry(entry.removal, entry.addition, condition);
		}

		public static LineEntry createFrom(LineEntry entry, String condition, Collection<String> words){
			return new LineEntry(entry.removal, entry.addition, condition, words);
		}

		LineEntry(String removal, Set<String> addition, String condition){
			this(removal, addition, condition, Collections.<String>emptyList());
		}

		LineEntry(String removal, String addition, String condition, String word){
			this(removal, new HashSet<>(Arrays.asList(addition)), condition, word);
		}

		LineEntry(String removal, Set<String> addition, String condition, String word){
			this(removal, addition, condition, Arrays.asList(word));
		}

		LineEntry(String removal, String addition, String condition, Collection<String> words){
			this(removal, new HashSet<>(Arrays.asList(addition)), condition, words);
		}

		LineEntry(String removal, Set<String> addition, String condition, Collection<String> words){
			this.removal = removal;
			this.addition = addition;
			this.condition = condition;

			from = new HashSet<>();
			if(words != null)
				from.addAll(words);
		}

		public List<LineEntry> split(AffixEntry.Type type){
			List<LineEntry> split = new ArrayList<>();
			if(type == AffixEntry.Type.SUFFIX)
				for(String f : from){
					int index = f.length() - condition.length() - 1;
					if(index < 0)
						throw new IllegalArgumentException("Cannot reduce rule, should be splitted further because of '" + f + "'");

					split.add(new LineEntry(removal, addition, f.substring(index), f));
				}
			else
				for(String f : from){
					int index = condition.length() + 1;
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


	public RuleReducerWorker(String flag, boolean keepLongestCommonAffix, AffixData affixData, DictionaryParser dicParser,
			WordGenerator wordGenerator){
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

		RuleEntry originalRuleEntry = affixData.getData(flag);
		if(originalRuleEntry == null)
			throw new IllegalArgumentException("Non-existent rule " + flag + ", cannot reduce");

		AffixEntry.Type type = originalRuleEntry.getType();

		List<LineEntry> plainRules = new ArrayList<>();
		BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			List<Production> productions = wordGenerator.applyAffixRules(line);

			collectProductionsByFlag(productions, flag, type, plainRules);
		};
		Runnable completed = () -> {
			try{
				List<LineEntry> compactedRules = compactRules(plainRules);

				removeOverlappingConditions(compactedRules);
System.out.println("\r\nremoveOverlappingConditions (" + compactedRules.size() + "):");
compactedRules.forEach(System.out::println);

				List<String> rules = convertEntriesToRules(flag, type, keepLongestCommonAffix, compactedRules);

//TODO check feasibility of solution?

				LOGGER.info(Backbone.MARKER_RULE_REDUCER, composeHeader(type, flag, originalRuleEntry.isCombineable(), rules.size()));
				for(String rule : rules)
					LOGGER.info(Backbone.MARKER_RULE_REDUCER, rule);
			}
			catch(Exception e){
				LOGGER.info(Backbone.MARKER_RULE_REDUCER, e.getMessage());

				e.printStackTrace();
			}
		};
//		WorkerData data = WorkerData.createParallel(WORKER_NAME, dicParser);
WorkerData data = WorkerData.create(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		createReadWorker(data, lineProcessor);
	}

	private void collectProductionsByFlag(List<Production> productions, String flag, AffixEntry.Type type, List<LineEntry> plainRules){
		Iterator<Production> itr = productions.iterator();
		//skip base production
		itr.next();
		while(itr.hasNext()){
			Production production = itr.next();

			AffixEntry lastAppliedRule = production.getLastAppliedRule(type);
			if(lastAppliedRule != null && lastAppliedRule.getFlag().equals(flag)){
				String word = lastAppliedRule.undoRule(production.getWord());
				LineEntry affixEntry = (lastAppliedRule.isSuffix()?
					createSuffixEntry(production, word, type):
					createPrefixEntry(production, word, type));
				plainRules.add(affixEntry);
			}
		}
	}

	private List<LineEntry> compactRules(List<LineEntry> entries){
		//same originating word, same removal, and same condition parts
		List<LineEntry> intermediate = collect(entries,
			entry -> entry.from.hashCode() + TAB + entry.removal + TAB + entry.condition,
			(rule, entry) -> rule.addition.addAll(entry.addition));

		//same removal, same addition, and same condition parts
		return collect(intermediate,
			entry -> entry.removal + TAB + entry.addition.hashCode() + TAB + entry.condition,
			(rule, entry) -> rule.from.addAll(entry.from));
	}

	//FIXME tested bottom-up until v0
	private void removeOverlappingConditions(List<LineEntry> rules){
		//sort current-list by shortest condition
		List<LineEntry> sortedList = new ArrayList<>(rules);
		sortedList.sort(shortestConditionComparator);

		//while current-list is not empty
		while(!sortedList.isEmpty()){
			//extract rule from current-list
			LineEntry parent = sortedList.remove(0);

			List<LineEntry> children = sortedList.stream()
				.filter(entry -> entry.condition.endsWith(parent.condition))
				.collect(Collectors.toList());
			if(children.isEmpty())
				continue;

			int parentConditionLength = parent.condition.length();
			//find parent-group
			String parentGroup = extractGroup(parent.from, parentConditionLength);

			Set<String> childrenFrom = children.stream()
				 .flatMap(entry -> entry.from.stream())
				 .collect(Collectors.toSet());
			//find children-group
			String childrenGroup = extractGroup(childrenFrom, parentConditionLength);

			//if intersection(parent-group, children-group) is empty
			Set<Character> parenGroupSet = SetHelper.makeCharacterSetFrom(parentGroup);
			Set<Character> childrenGroupSet = SetHelper.makeCharacterSetFrom(childrenGroup);
			Set<Character> groupIntersection = SetHelper.intersection(parenGroupSet, childrenGroupSet);
			if(groupIntersection.isEmpty()){
				ahn(parent, parentGroup, children, childrenGroup, sortedList, rules);

				List<LineEntry> sameConditionChildren = children.stream()
					.filter(entry -> !entry.condition.isEmpty() && entry.condition.equals(parent.condition))
					.collect(Collectors.toList());
				//for each children-same-condition
				for(LineEntry child : sameConditionChildren){
					//add new rule from child with condition starting with (child-group) to final-list
					String childGroup = extractGroup(child.from, parentConditionLength);
					String condition = makeGroup(childGroup) + child.condition;
					LineEntry newEntry = LineEntry.createFrom(child, condition, child.from);
					rules.add(newEntry);
				}

				//remove same-condition-children from current-list
				sameConditionChildren.forEach(sortedList::remove);
				//remove same-condition-children from final-list
				sameConditionChildren.forEach(rules::remove);
			}
			//if parent.condition is empty
			else if(parent.condition.isEmpty()){
				//for each last-char of parent.from
				Map<Character, List<String>> fromBucket = bucket(parent.from, from -> from.charAt(from.length() - 1));
				for(Map.Entry<Character, List<String>> entry : fromBucket.entrySet()){
					Character key = entry.getKey();
					//if last-char is contained into intersection
					if(groupIntersection.contains(key)){
						//add new rule from parent with condition the last-char
						LineEntry newEntry = LineEntry.createFrom(parent, String.valueOf(key), entry.getValue());
						sortedList.add(newEntry);
					}
				}
				sortedList.sort(shortestConditionComparator);

				//if intersection is proper subset of parent-group
				parenGroupSet.removeAll(groupIntersection);
				if(!parenGroupSet.isEmpty()){
					//add new rule from parent with condition the difference between parent-grop and intersection to final-list
					String cond = mergeSet(parenGroupSet);
					LineEntry newEntry = LineEntry.createFrom(parent, makeGroup(cond));
					rules.add(newEntry);
				}
			}
			else{
				//calculate intersection between parent and children conditions
				Map<Boolean, List<LineEntry>> conditionBucket = bucket(children, rule -> rule.condition.equals(parent.condition));
				//if intersection is empty
				if(!conditionBucket.containsKey(Boolean.TRUE) && conditionBucket.containsKey(Boolean.FALSE))
					ahn(parent, parentGroup, children, childrenGroup, sortedList, rules);
				else if(conditionBucket.containsKey(Boolean.TRUE) && !conditionBucket.containsKey(Boolean.FALSE)){
//do nothing (?)
				}
				else if(conditionBucket.isEmpty()){
					throw new IllegalArgumentException("yet to be coded!");
				}
				else{
//do nothing (?)
				}
			}

			//remove parent from final list
			rules.remove(parent);

/*
#SFX v0 0 ía|ieta/F0 .	: {r}/aelno{r}	> add 'SFX v0 0 ía|ieta/F0 r' to current
#SFX v0 0 aría|arieta|ería|erieta/F0 .	: ln/aeor	> add 'SFX v0 0 aría|arieta|ería|erieta/F0 [ln]' to final
#SFX v0 0 ría|rieta/F0 .	: {ae}/{ae}or	> add 'SFX v0 0 ría|rieta/F0 a', 'SFX v0 0 ría|rieta/F0 e' to current
#SFX v0 e aría|arieta/F0 e	: {ƚt}/{ƚt}	> do nothing since conditions are both equals (0 ría|rieta/F0 e)
#SFX v0 a ería|erieta/F0 a	: {dđgijɉkñorstŧvx}/{dđgi}í{jɉk}lƚ{ñorstŧvx}	> (clash with 'SFX v0 0 ría|rieta/F0 a') ?
#SFX v0 o ería|erieta|aría|arieta/F0 o	: b{d}gi{k}ƚnrtv{x}/c{d}đ{kx} > add 'SFX v0 o ería|erieta|aría|arieta/F0 [^cdđkx]o' to final, bubble 'SFX v0 o ería|erieta|aría|arieta/F0 [^ò][cdk]o', 'SFX v0 o ería|erieta|aría|arieta/F0 [^è][dđx]o' to final
#*SFX v0 0 ía|ieta/F0 r	: {a}o/{a}è > 'SFX v0 o ería|erieta|aría|arieta/F0 [^aè]r' to final
#*SFX v0 0 ría|rieta/F0 a	: dđgijɉkñorstŧvx/ílƚ > add 'SFX v0 0 ría|rieta/F0 [^ílƚ]a' to final, bubble 'SFX v0 0 ría|rieta/F0 [^è][lƚ]a', 'SFX v0 0 ría|rieta/F0 [^r]ía', 'SFX v0 0 ría|rieta/F0 [^e]ría' to final
#*SFX v0 0 ría|rieta/F0 e	: no children
#SFX v0 èr aría|arieta|ería|erieta/F0 èr
#SFX v0 ar ería|erieta/F0 ar
#SFX v0 ía ieta/F0 ía
#SFX v0 èdo edaría|edarieta|edería|ederieta/F0 èdo
#SFX v0 èđo eđaría|eđarieta|eđería|eđerieta/F0 èđo
#SFX v0 òdo odaría|odarieta|odería|oderieta/F0 òdo
#SFX v0 èxo exaría|exarieta|exería|exerieta/F0 èxo
#SFX v0 òco ocaría|ocarieta|ocería|ocerieta/F0 òco
#SFX v0 òko okaría|okarieta|okería|okerieta/F0 òko
#SFX v0 èla elería|elerieta|elaría|elarieta/F0 èla
#SFX v0 èƚa eƚería|eƚerieta|eƚaría|eƚarieta/F0 èƚa
#SFX v0 ería aría|arieta/F0 ería

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

		if intersection is proper subset of parent-group
			add new rule from parent with condition the difference between parent-grop and intersection to final-list
	}
	else{
		?
	}

	remove parent from final-list
}
*/
		}
	}

	private void ahn(LineEntry parent, String parentGroup, List<LineEntry> children, String childrenGroup, List<LineEntry> sortedList, List<LineEntry> rules){
		//add new rule from parent with condition starting with NOT(children-group) to final-list
		String condition = (parent.condition.isEmpty()? makeGroup(parentGroup): makeNotGroup(childrenGroup) + parent.condition);
		LineEntry newEntry = LineEntry.createFrom(parent, condition);
		rules.add(newEntry);

		//if parent.condition is not empty
		if(!parent.condition.isEmpty()){
			List<LineEntry> bubbles = extractRuleBubbles(parent, children);
			//if can-bubble-up
			if(!bubbles.isEmpty()){
				List<LineEntry> bubbledRules = bubbleUpNotGroup(parent, bubbles);
				rules.addAll(bubbledRules);
				
				//remove bubbles from current-list
				bubbles.forEach(sortedList::remove);
			}
		}
	}

	private List<LineEntry> extractRuleBubbles(LineEntry parent, List<LineEntry> sortedList){
		int parentConditionLength = parent.condition.length();
		return sortedList.stream()
			.filter(entry -> entry.condition.length() > parentConditionLength + 1)
			.collect(Collectors.toList());
	}

	private List<LineEntry> bubbleUpNotGroup(LineEntry parent, List<LineEntry> children){
		int parentConditionLength = parent.condition.length();
		//bubble up by bucketing children for group-2
		List<String> bubblesCondition = children.stream()
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
		List<LineEntry> newParents = new ArrayList<>();
		Map<String, List<String>> communalitiesBucket = bucket(bubblesCondition, cond -> cond.substring(cond.length() - parentConditionLength - 1));
		for(Map.Entry<String, List<String>> e : communalitiesBucket.entrySet()){
			List<String> comm = e.getValue();
			if(comm.size() > 1){
				//FIXME
				if(e.getKey().length() + 1 != comm.get(0).length() || !comm.stream().allMatch(c -> c.length() == comm.get(0).length()))
					throw new IllegalArgumentException("e.key.length + 1 != comm[0].length || comm.get(.).length() differs: key '" + e.getKey() + "', comm '"
						+ comm.toString());

				String commonGroup = extractGroup(comm, e.getKey().length());
				String condition = makeNotGroup(commonGroup) + e.getKey();
				newParents.add(LineEntry.createFrom(parent, condition));

				comm.forEach(bubblesCondition::remove);
			}
		}

		Map<String, List<String>> conditionBucket = bucket(bubblesCondition, cond -> cond.substring(0, cond.length() - parentConditionLength - 1));
		//for each children-group-2
		for(Map.Entry<String, List<String>> conds : conditionBucket.entrySet()){
			//add new rule from parent with condition starting with NOT(children-group-2) to final-list
			String bubbleGroup = extractGroup(conds.getValue(), parentConditionLength);
			//do the bubble trick
			for(int i = conds.getKey().length(); i > 0; i --){
				String condition = makeNotGroup(conds.getKey().substring(i - 1, i)) + conds.getKey().substring(i) + makeGroup(bubbleGroup) + parent.condition;
				newParents.add(LineEntry.createFrom(parent, condition));
			}
		}
		return newParents;
	}

	private String mergeSet(Set<Character> set){
		return set.stream()
			.map(String::valueOf)
			.sorted(comparator)
			.collect(Collectors.joining());
	}

	private String extractGroup(Collection<String> words, int indexFromLast){
		Set<String> group = new HashSet<>();
		for(String word : words){
			int index = word.length() - indexFromLast - 1;
			if(index < 0)
				throw new IllegalArgumentException("Cannot extract group from [" + StringUtils.join(words, ",")
					+ "] at index " + indexFromLast + " from last because of the presence of the word " + word + " that is too short");

			group.add(String.valueOf(word.charAt(index)));
		}
		return group.stream()
			.sorted(comparator)
			.collect(Collectors.joining(StringUtils.EMPTY));
	}

	private <K, V> Map<K, List<V>> bucket(Collection<V> entries, Function<V, K> keyMapper){
		Map<K, List<V>> bucket = new HashMap<>();
		for(V entry : entries){
			K key = keyMapper.apply(entry);
			if(key != null)
				bucket.computeIfAbsent(key, k -> new ArrayList<>())
					.add(entry);
		}
		return bucket;
	}

	private <K, V> List<V> collect(Collection<V> entries, Function<V, K> keyMapper, BiConsumer<V, V> mergeFunction){
		Map<K, V> compaction = new HashMap<>();
		for(V entry : entries){
			K key = keyMapper.apply(entry);
			V rule = compaction.putIfAbsent(key, entry);
			if(rule != null)
				mergeFunction.accept(rule, entry);
		}
		return new ArrayList<>(compaction.values());
	}

	private LineEntry createSuffixEntry(Production production, String word, AffixEntry.Type type){
		int lastCommonLetter;
		int wordLength = word.length();
		String producedWord = production.getWord();
		for(lastCommonLetter = 0; lastCommonLetter < Math.min(wordLength, producedWord.length()); lastCommonLetter ++)
			if(word.charAt(lastCommonLetter) != producedWord.charAt(lastCommonLetter))
				break;

		String removal = (lastCommonLetter < wordLength? word.substring(lastCommonLetter): AffixEntry.ZERO);
		String addition = (lastCommonLetter < producedWord.length()? producedWord.substring(lastCommonLetter): AffixEntry.ZERO);
		if(production.getContinuationFlagCount() > 0)
			addition += production.getLastAppliedRule(type).toStringWithMorphologicalFields(strategy);
		String condition = (lastCommonLetter < wordLength? removal: StringUtils.EMPTY);
		return new LineEntry(removal, addition, condition, word);
	}

	private LineEntry createPrefixEntry(Production production, String word, AffixEntry.Type type){
		int firstCommonLetter;
		int wordLength = word.length();
		String producedWord = production.getWord();
		int productionLength = producedWord.length();
		int minLength = Math.min(wordLength, productionLength);
		for(firstCommonLetter = 1; firstCommonLetter <= minLength; firstCommonLetter ++)
			if(word.charAt(wordLength - firstCommonLetter) != producedWord.charAt(productionLength - firstCommonLetter))
				break;
		firstCommonLetter --;

		String removal = (firstCommonLetter < wordLength? word.substring(0, wordLength - firstCommonLetter): AffixEntry.ZERO);
		String addition = (firstCommonLetter < productionLength? producedWord.substring(0, productionLength - firstCommonLetter): AffixEntry.ZERO);
		if(production.getContinuationFlagCount() > 0)
			addition += production.getLastAppliedRule(type).toStringWithMorphologicalFields(strategy);
		String condition = (firstCommonLetter < wordLength? removal: StringUtils.EMPTY);
		return new LineEntry(removal, addition, condition, word);
	}

	private String makeGroup(String group){
		return (group.length() > 1? GROUP_START + group + GROUP_END: group);
	}

	private String makeNotGroup(String group){
		return NOT_GROUP_START + group + GROUP_END;
	}

	private List<String> convertEntriesToRules(String flag, AffixEntry.Type type, boolean keepLongestCommonAffix, Collection<LineEntry> entries){
		//restore original rules
		List<LineEntry> restoredRules = entries.stream()
			.flatMap(rule -> {
				return rule.addition.stream()
					.map(addition -> new LineEntry(rule.removal, addition, rule.condition, rule.from));
			})
			.collect(Collectors.toList());

		List<LineEntry> sortedEntries = prepareRules(type, keepLongestCommonAffix, restoredRules);

		return composeAffixRules(flag, type, sortedEntries);
	}

	private List<LineEntry> prepareRules(AffixEntry.Type type, boolean keepLongestCommonAffix, Collection<LineEntry> entries){
		if(keepLongestCommonAffix)
			entries.forEach(entry -> {
				String lcs = longestCommonAffix(entry.from, (type == AffixEntry.Type.SUFFIX? this::commonSuffix: this::commonPrefix));
				if(lcs == null)
					lcs = entry.condition;
				else if(entry.condition.contains(GROUP_END)){
					String[] entryCondition = RegExpSequencer.splitSequence(entry.condition);
					if(!SEQUENCER.endsWith(RegExpSequencer.splitSequence(lcs), entryCondition)){
						int tailCharactersToExclude = entryCondition.length;
						if(tailCharactersToExclude <= lcs.length())
							lcs = lcs.substring(0, lcs.length() - tailCharactersToExclude) + entry.condition;
						else
							lcs = entry.condition;
					}
				}
				if(lcs.length() < entry.condition.length())
					throw new IllegalArgumentException("really bad error, lcs.length < condition.length");

				entry.condition = lcs;
			});
		List<LineEntry> sortedEntries = new ArrayList<>(entries);
		sortedEntries.sort(lineEntryComparator);
		return sortedEntries;
	}

	private List<String> composeAffixRules(String flag, AffixEntry.Type type, List<LineEntry> entries){
		int size = entries.size();
		List<String> rules = new ArrayList<>(size);
		for(LineEntry entry : entries)
			rules.add(composeLine(type, flag, entry));
		return rules;
	}

	private String longestCommonAffix(Collection<String> texts, BiFunction<String, String, String> commonAffix){
		String lcs = null;
		if(!texts.isEmpty()){
			Iterator<String> itr = texts.iterator();
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
	private String commonSuffix(String a, String b){
		int s = 0;
		int aLength = a.length();
		int bLength = b.length();
		int maxSuffixLength = Math.min(aLength, bLength);
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
	private String commonPrefix(String a, String b){
		int p = 0;
		int maxPrefixLength = Math.min(a.length(), b.length());
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
	private boolean validSurrogatePairAt(CharSequence string, int index){
		return (index >= 0 && index <= (string.length() - 2)
			&& Character.isHighSurrogate(string.charAt(index))
			&& Character.isLowSurrogate(string.charAt(index + 1)));
	}

	private String composeHeader(AffixEntry.Type type, String flag, boolean isCombineable, int size){
		StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		return sj.add(type.getTag().getCode())
			.add(flag)
			.add(Character.toString(isCombineable? RuleEntry.COMBINEABLE: RuleEntry.NOT_COMBINEABLE))
			.add(Integer.toString(size))
			.toString();
	}

	private String composeLine(AffixEntry.Type type, String flag, LineEntry partialLine){
		StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		return sj.add(type.getTag().getCode())
			.add(flag)
			.add(partialLine.removal)
			.add(partialLine.anAddition())
			.add(partialLine.condition.isEmpty()? DOT: partialLine.condition)
			.toString();
	}

}
