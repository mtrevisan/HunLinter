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


public class RuleReducerWorker extends WorkerDictionaryBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(RuleReducerWorker.class);

	public static final String WORKER_NAME = "Rule reducer";

	private static final RegExpSequencer SEQUENCER = new RegExpSequencer();

	private static final String TAB = "\t";
	private static final String NOT_GROUP_START = "[^";
	private static final String GROUP_START = "[";
	private static final String GROUP_END = "]";


	private static class LineEntry implements Serializable{

		private static final long serialVersionUID = 8374397415767767436L;

		private final Set<String> from;

		private final String removal;
		private final String addition;
		private String condition;


		public static LineEntry createFrom(LineEntry entry, String condition){
			return new LineEntry(entry.removal, entry.addition, condition);
		}

		public static LineEntry createFrom(LineEntry entry, String condition, String word){
			return new LineEntry(entry.removal, entry.addition, condition, word);
		}

		public static LineEntry createFrom(LineEntry entry, String condition, Collection<String> words){
			return new LineEntry(entry.removal, entry.addition, condition, words);
		}

		LineEntry(String removal, String addition, String condition){
			this(removal, addition, condition, (String)null);
		}

		LineEntry(String removal, String addition, String condition, String word){
			this(removal, addition, condition, Arrays.asList(word));
		}

		LineEntry(String removal, String addition, String condition, Collection<String> words){
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
			.thenComparing(Comparator.comparingInt(entry -> StringUtils.countMatches(entry.condition, ']')))
			.thenComparing(Comparator.comparing(entry -> StringUtils.reverse(entry.condition), comparator))
			.thenComparing(Comparator.comparingInt(entry -> entry.removal.length()))
			.thenComparing(Comparator.comparing(entry -> entry.removal, comparator))
			.thenComparing(Comparator.comparingInt(entry -> entry.addition.length()))
			.thenComparing(Comparator.comparing(entry -> entry.addition, comparator));

		RuleEntry originalRuleEntry = affixData.getData(flag);
		if(originalRuleEntry == null)
			throw new IllegalArgumentException("Non-existent rule " + flag + ", cannot reduce");

		AffixEntry.Type type = originalRuleEntry.getType();

		List<LineEntry> plainRules = new ArrayList<>();
		BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			List<Production> productions = wordGenerator.applyAffixRules(line);

			collectProductionsByFlag(productions, flag, plainRules);
		};
		Runnable completed = () -> {
			try{
				Set<LineEntry> disjointRules = removeSameConditions(type, plainRules);
//disjointRules.forEach(System.out::println);

				removeOverlappingConditions(type, disjointRules);

				mergeSimilarRules(disjointRules);

				List<String> rules = convertEntriesToRules(flag, type, keepLongestCommonAffix, disjointRules);

//TODO check feasibility of solution?

				LOGGER.info(Backbone.MARKER_RULE_REDUCER, composeHeader(type, flag, originalRuleEntry.isCombineable(), rules.size()));
				rules.stream()
					.forEach(rule -> LOGGER.info(Backbone.MARKER_RULE_REDUCER, rule));
			}
			catch(Exception e){
				LOGGER.info(Backbone.MARKER_RULE_REDUCER, e.getMessage());
			}
		};
		//FIXME
//		WorkerData data = WorkerData.createParallel(WORKER_NAME, dicParser);
		WorkerData data = WorkerData.create(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		createReadWorker(data, lineProcessor);
	}

	private Set<LineEntry> removeSameConditions(AffixEntry.Type type, List<LineEntry> plainRules){
		Map<String, LineEntry> equivalenceTable = collectIntoEquivalenceClasses(plainRules);
		return new HashSet<>(equivalenceTable.values());
	}

	private void removeOverlappingConditions(AffixEntry.Type type, Set<LineEntry> rules){
		//sort by shortes condition
		List<LineEntry> sortedList = rules.stream()
			.sorted(shortestConditionComparator)
			.collect(Collectors.toList());

		while(!sortedList.isEmpty()){
			LineEntry parent = sortedList.remove(0);

			Set<LineEntry> children = sortedList.stream()
				.filter(entry -> entry.condition.endsWith(parent.condition))
				.collect(Collectors.toSet());
			if(!children.isEmpty()){
				int parentConditionLength = parent.condition.length();
				Set<String> parentFrom = parent.from;
				String parentGroup = extractGroup(parentFrom, parentConditionLength);
				if(!StringUtils.isEmpty(parentGroup)){
					Set<String> childrenFrom = children.stream()
						.flatMap(entry -> entry.from.stream())
						.collect(Collectors.toSet());
					String childrenGroup = extractGroup(childrenFrom, parentConditionLength);
					if(StringUtils.containsAny(parentGroup, childrenGroup)){
						//split parents between belonging to children group and not belonging to children group
						String notChildrenGroup = NOT_GROUP_START + childrenGroup + GROUP_END;
						Map<String, Set<String>> parentChildrenBucket = bucket(parentFrom,
							from -> {
								char chr = from.charAt(from.length() - parentConditionLength - 1);
								return (StringUtils.contains(childrenGroup, chr)? String.valueOf(chr): notChildrenGroup);
							}
						);
						//manage same condition parent and children:
						Set<LineEntry> newParents = splitParentAndChildren(parentChildrenBucket, notChildrenGroup, parent, rules);
						rules.addAll(newParents);
						if(rules.contains(parent)){
							//check rules
							//TODO
System.out.println("");
						}

						sortedList.addAll(newParents);
						sortedList.sort(shortestConditionComparator);
					}
					else{
						boolean removed = false;
						Iterator<LineEntry> itr = sortedList.iterator();
						while(itr.hasNext()){
							LineEntry le = itr.next();
							if(le.condition.endsWith(parent.condition)){
								int index = le.condition.length() - Math.max(parentConditionLength + 1, le.removal.length());
								if(index > 0)
									le.condition = le.condition.substring(index);

								if(parentConditionLength + 1 == le.removal.length()){
									itr.remove();
									removed = true;
								}
							}
						}
						if(StringUtils.isNotEmpty(childrenGroup)){
							if(!removed){
								String newParentCondition = childrenGroup + parent.condition;
								Set<String> newParentFrom = parent.from.stream()
									.filter(from -> from.endsWith(newParentCondition))
									.collect(Collectors.toSet());
								sortedList.add(LineEntry.createFrom(parent, newParentCondition, newParentFrom));
								sortedList.sort(shortestConditionComparator);
							}
							String nextPreGroup = NOT_GROUP_START + childrenGroup + GROUP_END;
							parent.condition = nextPreGroup + parent.condition;
						}
					}
				}
				else{
					Iterator<LineEntry> itr = sortedList.iterator();
					while(itr.hasNext()){
						LineEntry le = itr.next();
						if(le.condition.endsWith(parent.condition)){
							for(int i = 0; i < le.condition.length() - parent.condition.length(); i ++){
								String c = NOT_GROUP_START + le.condition.charAt(i) + GROUP_END + le.condition.substring(i + 1);
								rules.add(LineEntry.createFrom(parent, c));
							}
							itr.remove();
						}
					}
				}
			}
			else{
				//add not group for first character of the condition
//NOTE should NOT take the new rule from the parent!!!
//SFX §0 òvo ovato/FSM0 òvo > SFX §0 òvo ovato/FSM0 [^ò]vo : wrong!!
				String c = NOT_GROUP_START + parent.condition.charAt(0) + GROUP_END + parent.condition.substring(1);
System.out.println(parent);
//[from=[òvo],rem=òvo,add=ovato/FSM0,cond=òvo]
//[from=[òmo],rem=òmo,add=omato/FSM0,cond=òmo]
//[from=[gòbo],rem=òbo,add=obato/FSM0,cond=òbo]
//[from=[ròba, gòba],rem=òba,add=obata/F0,cond=òba]
//				rules.add(LineEntry.createFrom(parent, c));
			}
		}

System.out.println("");
	}

	private Set<LineEntry> splitParentAndChildren(Map<String, Set<String>> parentChildrenBucket, String notChildrenGroup, LineEntry parent,
			Set<LineEntry> rules){
		//add augmented rules to the initial set
		Set<LineEntry> newParents = new HashSet<>();
		for(Map.Entry<String, Set<String>> elem : parentChildrenBucket.entrySet()){
			String key = elem.getKey();
			if(!key.equals(notChildrenGroup)){
				Set<String> from = elem.getValue();
				newParents.add(LineEntry.createFrom(parent, key + parent.condition, from));
			}
		}
		//modify parent rule to cope with the splitting
		Set<String> newParentFrom = parentChildrenBucket.get(notChildrenGroup);
		if(newParentFrom != null){
			parent.from.clear();
			parent.from.addAll(newParentFrom);
			parent.condition = notChildrenGroup + parent.condition;
		}
		else
			rules.remove(parent);
		return newParents;
	}

	private Map<String, LineEntry> collectIntoEquivalenceClasses(List<LineEntry> entries){
		Map<String, LineEntry> equivalenceTable = new HashMap<>();
		for(LineEntry entry : entries){
			String key = entry.removal + TAB + entry.addition + TAB + entry.condition;
			LineEntry ruleSet = equivalenceTable.putIfAbsent(key, entry);
			if(ruleSet != null)
				ruleSet.from.addAll(entry.from);
		}
		return equivalenceTable;
	}

	private <K, V> Map<K, Set<V>> bucket(Collection<V> entries, Function<V, K> keyGenerator){
		Map<K, Set<V>> bucket = new HashMap<>();
		for(V entry : entries){
			K key = keyGenerator.apply(entry);
			if(key != null)
				bucket.computeIfAbsent(key, k -> new HashSet<>())
					.add(entry);
		}
		return bucket;
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

	private void collectProductionsByFlag(List<Production> productions, String flag, List<LineEntry> plainRules){
		Iterator<Production> itr = productions.iterator();
		//skip base production
		itr.next();
		while(itr.hasNext()){
			Production production = itr.next();

			AffixEntry lastAppliedRule = production.getLastAppliedRule();
			if(lastAppliedRule != null && lastAppliedRule.getFlag().equals(flag)){
				String word = lastAppliedRule.undoRule(production.getWord());
				LineEntry affixEntry = (lastAppliedRule.isSuffix()? createSuffixEntry(production, word): createPrefixEntry(production, word));
				plainRules.add(affixEntry);
			}
		}
	}

	private LineEntry createSuffixEntry(Production production, String word){
		int lastCommonLetter;
		int wordLength = word.length();
		String producedWord = production.getWord();
		for(lastCommonLetter = 0; lastCommonLetter < Math.min(wordLength, producedWord.length()); lastCommonLetter ++)
			if(word.charAt(lastCommonLetter) != producedWord.charAt(lastCommonLetter))
				break;

		String removal = (lastCommonLetter < wordLength? word.substring(lastCommonLetter): AffixEntry.ZERO);
		String addition = (lastCommonLetter < producedWord.length()? producedWord.substring(lastCommonLetter): AffixEntry.ZERO);
		if(production.getContinuationFlagCount() > 0)
			addition += production.getLastAppliedRule().toStringMorphologicalAndMorphologicalFields(strategy);
		String condition = (lastCommonLetter < wordLength? removal: StringUtils.EMPTY);
		return new LineEntry(removal, addition, condition, word);
	}

	private LineEntry createPrefixEntry(Production production, String word){
		int firstCommonLetter;
		int wordLength = word.length();
		String producedWord = production.getWord();
		for(firstCommonLetter = 0; firstCommonLetter < Math.min(wordLength, producedWord.length()); firstCommonLetter ++)
			if(word.charAt(firstCommonLetter) == producedWord.charAt(firstCommonLetter))
				break;

		String removal = (firstCommonLetter < wordLength? word.substring(0, firstCommonLetter): AffixEntry.ZERO);
		String addition = (firstCommonLetter > 0? producedWord.substring(0, firstCommonLetter): AffixEntry.ZERO);
		String condition = (firstCommonLetter < wordLength? removal: StringUtils.EMPTY);
		return new LineEntry(removal, addition, condition, word);
	}

	private void mergeSimilarRules(Set<LineEntry> entries){
		//merge similar rules
		Map<String, Set<LineEntry>> mergeBucket = bucket(entries,
			entry -> (!entry.condition.contains("]")? entry.removal + TAB + entry.addition + TAB + entry.condition.length(): null));
		for(Set<LineEntry> set : mergeBucket.values())
			if(set.size() > 1){
				LineEntry firstEntry = set.iterator().next();
				String commonCondition = firstEntry.condition.substring(1);
				String condition = set.stream()
					.map(entry -> String.valueOf(entry.condition.charAt(0)))
					.sorted(comparator)
					.collect(Collectors.joining(StringUtils.EMPTY, GROUP_START, GROUP_END));
				Set<String> from = set.stream()
					.flatMap(entry -> entry.from.stream())
					.collect(Collectors.toSet());
				LineEntry newEntry = LineEntry.createFrom(firstEntry, condition + commonCondition, from);
				entries.add(newEntry);

				set.stream()
					.forEach(entries::remove);
			}
	}

	private List<String> convertEntriesToRules(String flag, AffixEntry.Type type, boolean keepLongestCommonAffix, Set<LineEntry> entries){
		List<LineEntry> sortedEntries = prepareRules(type, keepLongestCommonAffix, entries);

		return composeAffixRules(flag, type, sortedEntries);
	}

	private List<LineEntry> prepareRules(AffixEntry.Type type, boolean keepLongestCommonAffix, Set<LineEntry> entries){
		if(keepLongestCommonAffix)
			entries.forEach(entry -> {
				String lcs = longestCommonAffix(entry.from, (type == AffixEntry.Type.SUFFIX? this::commonSuffix: this::commonPrefix));
				if(lcs == null)
					lcs = entry.condition;
				else if(entry.condition.contains("]")){
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
		return sj.add(type.getFlag().getCode())
			.add(flag)
			.add(Character.toString(isCombineable? RuleEntry.COMBINEABLE: RuleEntry.NOT_COMBINEABLE))
			.add(Integer.toString(size))
			.toString();
	}

	private String composeLine(AffixEntry.Type type, String flag, LineEntry partialLine){
		StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		return sj.add(type.getFlag().getCode())
			.add(flag)
			.add(partialLine.removal)
			.add(partialLine.addition)
			.add(partialLine.condition)
			.toString();
	}

}
