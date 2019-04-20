package unit731.hunspeller.parsers.dictionary.workers;

import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
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

	private static final String SLASH = "/";
	private static final String NOT_GROUP_START = "[^";
	private static final String GROUP_START = "[";
	private static final String GROUP_END = "]";

	private static final RegExpSequencer SEQUENCER = new RegExpSequencer();


	private class LineEntry{

		private final Set<String> from;
		private final String removal;
		private final String addition;
		private String condition;


		LineEntry(String removal, String addition, String condition){
			this(removal, addition, condition, null);
		}

		LineEntry(String removal, String addition, String condition, String word){
			this.removal = removal;
			this.addition = addition;
			this.condition = condition;

			from = new HashSet<>();
			from.add(word);
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
	private Comparator<LineEntry> shortestConditionComparator;
	private Comparator<LineEntry> lineEntryComparator;


	public RuleReducerWorker(AffixData affixData, DictionaryParser dicParser, WordGenerator wordGenerator){
		Objects.requireNonNull(affixData);
		Objects.requireNonNull(wordGenerator);

		strategy = affixData.getFlagParsingStrategy();
		comparator = BaseBuilder.getComparator(affixData.getLanguage());
		shortestConditionComparator = Comparator.comparingInt(entry -> entry.condition.length());
		lineEntryComparator = Comparator.comparingInt((LineEntry entry) -> SEQUENCER.length(RegExpSequencer.splitSequence(entry.condition)))
			.thenComparing(Comparator.comparing(entry -> entry.condition, String.CASE_INSENSITIVE_ORDER))
			.thenComparing(Comparator.comparingInt(entry -> entry.removal.length()))
			.thenComparing(Comparator.comparing(entry -> entry.removal, String.CASE_INSENSITIVE_ORDER))
			.thenComparing(Comparator.comparingInt(entry -> entry.addition.length()))
			.thenComparing(Comparator.comparing(entry -> entry.addition, String.CASE_INSENSITIVE_ORDER));

String flag = "%0";
		RuleEntry originalRuleEntry = (RuleEntry)affixData.getData(flag);
		if(originalRuleEntry == null)
			throw new IllegalArgumentException("Non-existent rule " + flag + ", cannot reduce");

		Map<String, List<LineEntry>> entriesTable = new HashMap<>();
		BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			List<Production> productions = wordGenerator.applyAffixRules(line);

			collectFlagProductions(productions, flag, entriesTable);
		};
		Runnable completed = () -> {
//System.out.println("entries:");
//for(Map.Entry<String, List<LineEntry>> e : entriesTable.entrySet())
//	System.out.println(e.getKey() + ": " + StringUtils.join(e.getValue(), ","));
			AffixEntry.Type type = (originalRuleEntry.isSuffix()? AffixEntry.Type.SUFFIX: AffixEntry.Type.PREFIX);
			try{
				reduceEquivalenceClasses(type, entriesTable);
			}
			catch(Exception e){
				LOGGER.info(Backbone.MARKER_APPLICATION, e.getMessage());

				throw e;
			}
//System.out.println("cleaned entries:");
//for(Map.Entry<String, List<LineEntry>> e : entriesTable.entrySet())
//	System.out.println(e.getKey() + ": " + StringUtils.join(e.getValue(), ","));

			List<String> rules = convertEntriesToRules(originalRuleEntry, entriesTable);
			LOGGER.info(Backbone.MARKER_APPLICATION, composeHeader(type, flag, originalRuleEntry.isCombineable(), rules.size()));
			rules.stream()
				.forEach(rule -> LOGGER.info(Backbone.MARKER_APPLICATION, rule));



//			for(List<LineEntry> entries : entriesTable.values()){
//				List<String> rules = reduceEntriesToRules(originalRuleEntry, entries);
//
//				AffixEntry.Type type = (originalRuleEntry.isSuffix()? AffixEntry.Type.SUFFIX: AffixEntry.Type.PREFIX);
//				LOGGER.info(Backbone.MARKER_APPLICATION, composeHeader(type, flag, originalRuleEntry.isCombineable(), rules.size()));
//				rules.stream()
//					.forEach(rule -> LOGGER.info(Backbone.MARKER_APPLICATION, rule));
//			}



//			Map<String, List<LineEntry>> bucketedEntries = bucketByConditionEndsWith(flaggedEntries);

//			Map<String, LineEntry> nonOverlappingBucketedEntries = removeOverlappingRules(bucketedEntries);

/*
#SFX v1 o sta io
#SFX v1 e ista e
#SFX v1 0 ista l
#SFX v1 0 ista n
#SFX v1 o ista go
#SFX v1 o ista so
#SFX v1 o ista to
#SFX v1 o ista xo
#SFX v1 o ista ŧo
#SFX v1 o ista ƚo
#SFX v1 ía ista ía
#SFX v1 0 ista [^è]r
#SFX v1 a ista [^n]ia
#SFX v1 a ista [^ò]da
#SFX v1 a ista [^dií]a
#SFX v1 a ista [^ò]nia
#SFX v1 èr erista èr
#SFX v1 òda odista òda
#SFX v1 ònia onista ònia
*/
//			List<LineEntry> nonOverlappingEntries = new ArrayList<>(nonOverlappingBucketedEntries.values());
//			List<LineEntry> nonOverlappingEntries = bucketedEntries.values().stream()
//				.flatMap(List::stream)
//				.collect(Collectors.toList());
//			nonOverlappingEntries = mergeSameRule(nonOverlappingEntries);
//			List<String> rules = reduceEntriesToRules(originalRuleEntry, nonOverlappingEntries);
//
//			AffixEntry.Type type = (originalRuleEntry.isSuffix()? AffixEntry.Type.SUFFIX: AffixEntry.Type.PREFIX);
//			LOGGER.info(Backbone.MARKER_APPLICATION, composeHeader(type, flag, originalRuleEntry.isCombineable(), rules.size()));
//			rules.stream()
//				.forEach(rule -> LOGGER.info(Backbone.MARKER_APPLICATION, rule));
		};
		WorkerData data = WorkerData.createParallel(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		createReadWorker(data, lineProcessor);
	}

	private void reduceEquivalenceClasses(AffixEntry.Type type, Map<String, List<LineEntry>> entriesTable){
		while(true){
			List<List<String>> equivalenceClasses = calculateEquivalenceClasses(entriesTable);
//System.out.println("eq classes:");
//for(List<String> e : equivalenceClasses)
//	System.out.println(String.join(",", e));
			
			if(equivalenceClasses.stream().map(List::size).allMatch(size -> size == 1))
				break;
			
			removeCollidingClasses(equivalenceClasses, type, entriesTable);
		}
	}

	private List<List<String>> calculateEquivalenceClasses(Map<String, List<LineEntry>> entriesTable){
		List<List<String>> equivalenceClasses = new ArrayList<>();
		for(String cond : entriesTable.keySet()){
			List<String> foundClass = null;
			for(int i = 0; i < equivalenceClasses.size(); i ++)
				if(equivalenceClasses.get(i).stream().anyMatch(eq -> eq.endsWith(cond) || cond.endsWith(eq))){
					foundClass = equivalenceClasses.get(i);
					break;
				}
			if(foundClass != null)
				foundClass.add(cond);
			else
				equivalenceClasses.add(new ArrayList<>(Arrays.asList(cond)));
		}

		//order equivalence classes
		equivalenceClasses.forEach(classes -> classes.sort(Comparator.comparing(String::length).reversed()));

		return equivalenceClasses;
	}

	private void removeCollidingClasses(List<List<String>> equivalenceClasses, AffixEntry.Type type, Map<String, List<LineEntry>> entriesTable){
		for(List<String> equivalenceClass : equivalenceClasses){
			int classes = equivalenceClass.size();
			if(classes > 1){
				String baseKey = equivalenceClass.get(0);
				for(int i = 1; i < classes; i ++){
					String currentKey = equivalenceClass.get(i);
					//prepend characters to currentKey until it is no longer contained into baseKey
					if(baseKey.endsWith(currentKey))
						expandConditionRemovingCollingClasses(currentKey, type, entriesTable);
				}
			}
		}
	}

	private void expandConditionRemovingCollingClasses(String collidingKey, AffixEntry.Type type, Map<String, List<LineEntry>> entriesTable){
		List<LineEntry> currentEntry = entriesTable.remove(collidingKey);
		for(LineEntry entry : currentEntry){
			List<LineEntry> splittedEntries = entry.split(type);
			for(LineEntry splittedEntry : splittedEntries)
				collectFlagProduction(splittedEntry, splittedEntry.from.iterator().next(), entriesTable);
		}
	}

	private void collectFlagProductions(List<Production> productions, String flag, Map<String, List<LineEntry>> entries){
		Iterator<Production> itr = productions.iterator();
		//skip base production
		itr.next();
		while(itr.hasNext()){
			Production production = itr.next();

			AffixEntry lastAppliedRule = production.getLastAppliedRule();
			if(lastAppliedRule != null && lastAppliedRule.getFlag().equals(flag)){
				String word = lastAppliedRule.undoRule(production.getWord());
				LineEntry affixEntry = (lastAppliedRule.isSuffix()? createSuffixEntry(production, word): createPrefixEntry(production, word));

				collectFlagProduction(affixEntry, word, entries);
			}
		}
	}

	private void collectFlagProduction(LineEntry affixEntry, String word, Map<String, List<LineEntry>> entries){
		List<LineEntry> list = entries.computeIfAbsent(affixEntry.condition, k -> new ArrayList<>());
		int idx = list.indexOf(affixEntry);
		if(idx >= 0)
			list.get(idx).from.add(word);
		else
			list.add(affixEntry);
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

	private List<String> convertEntriesToRules(RuleEntry originalRuleEntry, Map<String, List<LineEntry>> entriesTable){
		List<LineEntry> entries = new ArrayList<>();
		entriesTable.values()
			.forEach(entries::addAll);
		entries.forEach(entry -> entry.condition = longestCommonSuffix(entry.from));
		entries.sort(lineEntryComparator);

		int size = entries.size();
		AffixEntry.Type type = (originalRuleEntry.isSuffix()? AffixEntry.Type.SUFFIX: AffixEntry.Type.PREFIX);
		String flag = originalRuleEntry.getEntries().get(0).getFlag();

		List<String> rules = new ArrayList<>(size);
		for(LineEntry entry : entries)
			rules.add(composeLine(type, flag, entry));
		return rules;
	}



	private Map<String, List<LineEntry>> bucketByConditionEndsWith(List<LineEntry> entries){
		entries.sort(shortestConditionComparator);
System.out.println("entries");
for(LineEntry le : entries)
	System.out.println(le);

		Map<String, List<LineEntry>> bucket = new HashMap<>();
		while(!entries.isEmpty()){
			//collect all entries that has the condition that ends with `condition`
			String condition = entries.get(0).condition;
			List<LineEntry> list = collectBy(entries, e -> e.condition.endsWith(condition));

			//manage same condition rules (entry.condition == firstCondition)
			if(list.size() > 1){
				//find same condition entries
				Map<String, List<LineEntry>> equalsBucket = bucketByConditionEqualsTo(list);

				boolean hasSameConditions = equalsBucket.values().stream().map(List::size).anyMatch(count -> count > 1);
				if(hasSameConditions){
					//expand same condition entries
					boolean expansionHappened = expandOverlappingRules(equalsBucket);

//FIXME needed?
					//expand again if needed
//					while(expansionHappened){
//						expansionHappened = false;
//						for(List<LineEntry> set : equalsBucket.values()){
//							equalsBucket = bucketByConditionEqualsTo(set);
//
//							//expand same condition entries
//							hasSameConditions = equalsBucket.values().stream().map(List::size).anyMatch(count -> count > 1);
//							if(hasSameConditions)
//								expansionHappened |= expandOverlappingRules(equalsBucket);
//						}
//					}

					bucket.putAll(equalsBucket);
				}
				else if(list.size() > 1){
					removeOverlappingConditions(list);

					for(LineEntry en : list)
						bucket.put(en.condition, Collections.singletonList(en));
				}
				else
					bucket.put(condition, list);
			}
			else
				bucket.put(condition, list);
		}
System.out.println("bucketed entries");
for(Map.Entry<String, List<LineEntry>> e : bucket.entrySet())
	System.out.println(e.getKey() + ": " + e.getValue().stream().map(LineEntry::toString).collect(Collectors.joining(",")));
		return bucket;
	}

//	private Map<String, LineEntry> removeOverlappingRules(Map<String, List<LineEntry>> bucketedEntries){
//		Map<String, LineEntry> nonOverlappingBucketedEntries = new HashMap<>();
//		for(Map.Entry<String, List<LineEntry>> entry : bucketedEntries.entrySet()){
//			List<LineEntry> aggregatedRules = entry.getValue();
//			if(aggregatedRules.size() > 1){
//				removeOverlappingConditions(aggregatedRules);
//
//				for(LineEntry en : aggregatedRules)
//					nonOverlappingBucketedEntries.put(en.condition, en);
//			}
//			else
//				nonOverlappingBucketedEntries.put(entry.getKey(), aggregatedRules.get(0));
//		}
//		return nonOverlappingBucketedEntries;
//	}

	private List<LineEntry> removeOverlappingConditions(List<LineEntry> aggregatedRules){
		//extract letters prior to first condition
		LineEntry firstRule = aggregatedRules.get(0);
		String firstCondition = firstRule.condition;
		int firstConditionLength = firstCondition.length();
		Set<Character> letters = new HashSet<>();
		int size = aggregatedRules.size();
		for(int index = 1; index < size; index ++){
			LineEntry entry = aggregatedRules.get(index);

			char[] additionalCondition = entry.condition.substring(0, entry.condition.length() - firstConditionLength).toCharArray();
			ArrayUtils.reverse(additionalCondition);

			//add letter additionalCondition.charAt(0) to [^...] * firstCondition
			letters.add(additionalCondition[0]);

			//add another rule(s) with [^additionalCondition.charAt(2)] * additionalCondition.charAt(1) * additionalCondition.charAt(0) * firstCondition
			String ongoingCondition = firstCondition;
			for(int i = 0; i < additionalCondition.length - 1; i ++){
//TODO manage same condition rules ([^x]ongoingCondition and [^y]ongoingCondition)?
				ongoingCondition = additionalCondition[i] + ongoingCondition;
				aggregatedRules.add(new LineEntry(firstRule.removal, firstRule.addition, NOT_GROUP_START + additionalCondition[i + 1] + GROUP_END
					+ ongoingCondition));
			}
		}
		addNotCondition(firstRule, letters);

		return aggregatedRules;
	}

	private Map<String, List<LineEntry>> bucketByConditionEqualsTo(List<LineEntry> entries){
		List<LineEntry> copy = new ArrayList<>(entries);
		Map<String, List<LineEntry>> bucket = new HashMap<>();
		while(!copy.isEmpty()){
			//collect all entries that has the condition that is `condition`
			String condition = copy.get(0).condition;
			List<LineEntry> list = collectBy(copy, e -> e.condition.equals(condition));

			bucket.put(condition, list);
		}
		return bucket;
	}

	private boolean expandOverlappingRules(Map<String, List<LineEntry>> bucket){
		boolean expanded = false;
		for(Map.Entry<String, List<LineEntry>> set : bucket.entrySet()){
			List<LineEntry> entries = set.getValue();
			if(entries.size() > 1){
				if(entries.size() == 2){
					List<Set<Character>> letters = collectPreviousLettersOfCondition(entries);
					Set<Character> intersection = calculateIntersection(letters);
					if(intersection.isEmpty()){
						List<String> conditions = convertSets(letters);
						int shortestSetIndex = extractShortestSetIndex(conditions);
						updateEntriesCondition(entries, conditions, shortestSetIndex);
					}
					else{
						List<LineEntry> commonRules = extractCommonRules(entries, intersection);

						while(!commonRules.isEmpty()){
							//collect all entries that has the condition that ends with `condition`
							String condition = commonRules.get(0).condition;
							List<LineEntry> list = collectBy(commonRules, e -> e.condition.endsWith(condition));
							bucket.put(condition, list);
						}

						bucket.putAll(bucketByConditionEndsWith(entries));

						if(entries.isEmpty())
							bucket.remove(set.getKey());
/*
SFX v1 o sta io	po:noun
SFX v1 o ista io	po:noun
SFX v1 o ista [^i]o
*/
					}
				}
				else{
/*
SFX v1 o sta io
SFX v1 o swa wo
SFX v1 o ista [^i]o

kanwo/v1	po:noun
*/
throw new RuntimeException("to be tested");
				}

				expanded = true;
			}
		}
		return expanded;
	}

	private List<LineEntry> extractCommonRules(List<LineEntry> entries, Set<Character> intersection){
		List<LineEntry> commonRules = new ArrayList<>();
		Iterator<LineEntry> itr = entries.iterator();
		while(itr.hasNext()){
			LineEntry entry = itr.next();

			Set<Character> addedConditions = new HashSet<>();
			Iterator<String> words = entry.from.iterator();
			while(words.hasNext()){
				String word = words.next();
				char chr = (word.charAt(word.length() - entry.condition.length() - 1));
				if(intersection.contains(chr)){
					commonRules.add(new LineEntry(entry.removal, entry.addition, chr + entry.condition, word));

					addedConditions.add(chr);

					words.remove();
				}
			}
			addNotCondition(entry, addedConditions);

			if(entry.from.isEmpty())
				itr.remove();
		}
		return commonRules;
	}

	private void addNotCondition(LineEntry entry, Set<Character> addedConditions){
		if(!addedConditions.isEmpty()){
			List<String> sortedLetters = addedConditions.stream().map(String::valueOf).collect(Collectors.toList());
			Collections.sort(sortedLetters, comparator);
			String addedCondition = StringUtils.join(sortedLetters, StringUtils.EMPTY);
			entry.condition = NOT_GROUP_START + addedCondition + GROUP_END + entry.condition;
		}
	}

	//expand conditions by one letter
//	private List<LineEntry> expandConditions(List<LineEntry> entries){
//		List<LineEntry> expandedEntries = new ArrayList<>();
//		for(LineEntry en : entries){
//			Iterator<String> words = en.originalWords.iterator();
//			while(words.hasNext()){
//				String originalWord = words.next();
//
//				int startingIndex = originalWord.length() - en.condition.length() - 1;
//				if(startingIndex >= 0){
//					String newCondition = originalWord.substring(startingIndex);
//					LineEntry newEntry = new LineEntry(en.removal, en.addition, newCondition, originalWord);
//					int index = expandedEntries.indexOf(newEntry);
//					if(index >= 0)
//						expandedEntries.get(index).originalWords.add(originalWord);
//					else
//						expandedEntries.add(newEntry);
//				}
//				else{
//					LOGGER.info(Backbone.MARKER_APPLICATION, "Cannot reduce rule (because of '{0}' that is too short)", originalWord);
//
//					throw new IllegalArgumentException("Cannot reduce rule");
//				}
//			}
//		}
//		entries.clear();
//		entries.addAll(expandedEntries);
//		return entries;
//	}

	private List<String> convertSets(List<Set<Character>> letters){
		return letters.stream()
			.map(set -> set.stream().map(String::valueOf).collect(Collectors.toList()))
			.map(sortedSet -> {
				Collections.sort(sortedSet, comparator);
				return StringUtils.join(sortedSet, StringUtils.EMPTY);
			})
			.collect(Collectors.toList());
	}

	private List<Set<Character>> collectPreviousLettersOfCondition(List<LineEntry> entries){
		List<Set<Character>> letters = new ArrayList<>();
		for(LineEntry entry : entries){
			Set<Character> set = new HashSet<>();
			for(String from : entry.from){
				char newLetterCondition = from.charAt(from.length() - entry.condition.length() - 1);
				set.add(newLetterCondition);
			}
			letters.add(set);
		}
		return letters;
	}

	private Set<Character> calculateIntersection(List<Set<Character>> letters){
		Iterator<Set<Character>> itr = letters.iterator();
		Set<Character> intersection = new HashSet<>(itr.next());
		while(itr.hasNext())
			intersection.retainAll(itr.next());
		return intersection;
	}

	private int extractShortestSetIndex(List<String> conditions){
		String shortest = conditions.stream()
			.min((set1, set2) -> Integer.compare(set1.length(), set2.length()))
			.get();
		return conditions.indexOf(shortest);
	}

	private void updateEntriesCondition(List<LineEntry> entries, List<String> conditions, int shortestSetIndex){
		String shortestSet = conditions.get(shortestSetIndex);
		entries.get(shortestSetIndex).condition = (shortestSet.length() > 1? GROUP_START + shortestSet + GROUP_END:
			shortestSet) + entries.get(shortestSetIndex).condition;
		entries.get((shortestSetIndex + 1) % 2).condition = NOT_GROUP_START + shortestSet + GROUP_END
			+ entries.get((shortestSetIndex + 1) % 2).condition;
	}

	/** Collect same condition length, removal, and addition */
	private List<LineEntry> mergeSameRule(List<LineEntry> entries){
		Map<String, List<LineEntry>> bucket = bucketByConditionRemovalAddition(entries);

		List<LineEntry> result = new ArrayList<>();
		for(List<LineEntry> list : bucket.values()){
			if(list.size() > 1){
				//merge rules
				Set<String> conditions = new HashSet<>();
				for(LineEntry entry : list){
					String[] condition = RegExpSequencer.splitSequence(entry.condition);
					conditions.add(condition[0]);
				}

				//TODO exclude groups, for now
				//if all [] then ok, merge inside
				//if all [^] then ok, merge inside
				//if some [] and no [^] then ok, merge inside
				List<String> sortedConditions = new ArrayList<>(conditions);
				Collections.sort(sortedConditions, comparator);
				String addedCondition = StringUtils.join(sortedConditions, StringUtils.EMPTY);
				if(addedCondition.length() == sortedConditions.size()){
					String[] condition = RegExpSequencer.splitSequence(list.get(0).condition);
					condition = ArrayUtils.remove(condition, 0);
					list.get(0).condition = GROUP_START + addedCondition + GROUP_END + StringUtils.join(condition);
				}
			}
			result.add(list.get(0));
		}
		return result;
	}

	private Map<String, List<LineEntry>> bucketByConditionRemovalAddition(List<LineEntry> entries){
		List<LineEntry> copy = new ArrayList<>(entries);
		Map<String, List<LineEntry>> bucket = new HashMap<>();
		while(!copy.isEmpty()){
			String[] condition = RegExpSequencer.splitSequence(copy.get(0).condition);
			condition = ArrayUtils.remove(condition, 0);
			String cond = StringUtils.join(condition);
			String removal = copy.get(0).removal;
			String addition = copy.get(0).addition;
			List<LineEntry> list = collectBy(copy, e -> {
				String[] econd = RegExpSequencer.splitSequence(e.condition);
				econd = ArrayUtils.remove(econd, 0);
				return (StringUtils.join(econd).equals(cond) && e.removal.equals(removal) && e.addition.equals(addition));
			});

			bucket.put(cond + "|" + removal + "|" + addition, list);
		}
		return bucket;
	}

	private List<LineEntry> collectBy(List<LineEntry> entries, Function<LineEntry, Boolean> comparator){
		List<LineEntry> list = new ArrayList<>();
		Iterator<LineEntry> itr = entries.iterator();
		while(itr.hasNext()){
			LineEntry entry = itr.next();
			if(comparator.apply(entry)){
				itr.remove();

				list.add(entry);
			}
		}
		return list;
	}

//	private void addCondition(LineEntry entry, Set<String> conditions){
//		List<String> sortedConditions = new ArrayList<>(conditions);
//		Collections.sort(sortedConditions, comparator);
//		String addedCondition = StringUtils.join(sortedConditions, StringUtils.EMPTY);
//		entry.condition = GROUP_START + addedCondition + GROUP_END + entry.condition;
//	}

//	private List<String> reduceEntriesToRules(RuleEntry originalRuleEntry, List<LineEntry> nonOverlappingBucketedEntries){
//		int size = nonOverlappingBucketedEntries.size();
//		AffixEntry.Type type = (originalRuleEntry.isSuffix()? AffixEntry.Type.SUFFIX: AffixEntry.Type.PREFIX);
//		String flag = originalRuleEntry.getEntries().get(0).getFlag();
//
//		nonOverlappingBucketedEntries.sort(lineEntryComparator);
//
//		List<String> rules = new ArrayList<>(size);
//		for(LineEntry entry : nonOverlappingBucketedEntries)
//			rules.add(composeLine(type, flag, entry));
//		return rules;
//	}

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

	//FIXME optimize!
	private String longestCommonSuffix(Set<String> texts){
		String firstText = null;
		int lastCommonLetter = 0;
		List<String> list = new ArrayList<>(texts);
		list.sort(Comparator.comparing(String::length).reversed());
		while(true){
			Iterator<String> itr = list.iterator();
			String text = itr.next();
			if(firstText == null){
				//extract longest word
				firstText = text;
			}

			int index = text.length() - lastCommonLetter - 1;
			if(index < 0)
				break;

			char commonLetter = text.charAt(index);
			while(itr.hasNext()){
				text = itr.next();
				index = text.length() - lastCommonLetter - 1;
				if(index < 0 || text.charAt(index) != commonLetter)
					return text.substring(index + 1);
			}

			lastCommonLetter ++;
		}
		return firstText;
	}

}
