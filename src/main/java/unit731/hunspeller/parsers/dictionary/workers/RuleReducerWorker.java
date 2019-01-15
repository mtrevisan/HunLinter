package unit731.hunspeller.parsers.dictionary.workers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.collections.radixtree.sequencers.RegExpSequencer;
import unit731.hunspeller.languages.BaseBuilder;
import unit731.hunspeller.parsers.affix.AffixData;
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

	private static final String TAB = "\t";

	private static final String NOT_GROUP_START = "[^";
	private static final String GROUP_START = "[";
	private static final String GROUP_END = "]";

	private static final RegExpSequencer SEQUENCER = new RegExpSequencer();


	private class LineEntry{
		private final List<String> originalWords;
		private final String removal;
		private final String addition;
		private String condition;

		LineEntry(String removal, String addition, String condition){
			this(removal, addition, condition, null);
		}

		LineEntry(String removal, String addition, String condition, String originalWord){
			originalWords = new ArrayList<>();
			originalWords.add(originalWord);
			this.removal = removal;
			this.addition = addition;
			this.condition = condition;
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


	private Comparator<String> comparator;
	private Comparator<LineEntry> shortestConditionComparator;
	private Comparator<LineEntry> lineEntryComparator;


	public RuleReducerWorker(AffixData affixData, DictionaryParser dicParser, WordGenerator wordGenerator){
		Objects.requireNonNull(affixData);
		Objects.requireNonNull(wordGenerator);

		comparator = BaseBuilder.getComparator(affixData.getLanguage());
		shortestConditionComparator = Comparator.comparingInt(entry -> entry.condition.length());
		lineEntryComparator = Comparator.comparingInt((LineEntry entry) -> entry.addition.length())
			.thenComparing(Comparator.comparing(entry -> entry.addition))
			.thenComparing(Comparator.comparingInt(entry -> entry.condition.length()))
			.thenComparing(Comparator.comparing(entry -> entry.condition))
			.thenComparing(Comparator.comparingInt(entry -> entry.removal.length()))
			.thenComparing(Comparator.comparing(entry -> entry.removal));

String flag = "v1";
		RuleEntry originalRuleEntry = (RuleEntry)affixData.getData(flag);
		if(originalRuleEntry == null)
			throw new IllegalArgumentException("Non-existent rule " + flag + ", cannot reduce");

		List<LineEntry> flaggedEntries = new ArrayList<>();
		BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			List<Production> productions = wordGenerator.applyAffixRules(line);

			collectFlagProductions(productions, flag, flaggedEntries);
		};
		Runnable completed = () -> {
			Map<String, List<LineEntry>> bucketedEntries = bucketByConditionEndingWith(flaggedEntries);

			Map<String, LineEntry> nonOverlappingBucketedEntries = removeOverlappingRules(bucketedEntries);

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
			List<LineEntry> nonOverlappingEntries = new ArrayList<>(nonOverlappingBucketedEntries.values());
			List<String> rules = reduceEntriesToRules(originalRuleEntry, nonOverlappingEntries);

			AffixEntry.Type type = (originalRuleEntry.isSuffix()? AffixEntry.Type.SUFFIX: AffixEntry.Type.PREFIX);
			LOGGER.info(Backbone.MARKER_APPLICATION, composeHeader(type, flag, originalRuleEntry.isCombineable(), rules.size()));
			rules.stream()
				.forEach(rule -> LOGGER.info(Backbone.MARKER_APPLICATION, rule));
		};
		WorkerData data = WorkerData.createParallel(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		createReadWorker(data, lineProcessor);
	}

	private void collectFlagProductions(List<Production> productions, String flag, List<LineEntry> newAffixEntries){
		Iterator<Production> itr = productions.iterator();
		//skip base production
		itr.next();
		while(itr.hasNext()){
			Production production = itr.next();

			AffixEntry lastAppliedRule = production.getLastAppliedRule();
			if(lastAppliedRule != null && lastAppliedRule.getFlag().equals(flag)){
				String word = lastAppliedRule.undoRule(production.getWord());
				LineEntry affixEntry = (lastAppliedRule.isSuffix()? createSuffixEntry(production, word): createPrefixEntry(production, word));

				int index = newAffixEntries.indexOf(affixEntry);
				if(index >= 0)
					newAffixEntries.get(index).originalWords.add(word);
				else
					newAffixEntries.add(affixEntry);
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
		String condition = (lastCommonLetter < wordLength? removal: word.substring(wordLength - 1));
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
		String condition = (firstCommonLetter < wordLength? removal: word.substring(wordLength - 1));
		return new LineEntry(removal, addition, condition, word);
	}

	private Map<String, List<LineEntry>> bucketByConditionEndingWith(List<LineEntry> entries){
		entries.sort(shortestConditionComparator);

		Map<String, List<LineEntry>> bucket = new HashMap<>();
		while(!entries.isEmpty()){
			//collect all entries that has the condition that ends with `condition`
			String condition = entries.get(0).condition;
			List<LineEntry> list = collectByCondition(entries, a -> a.endsWith(condition));

			//manage same condition rules (entry.condition == firstCondition)
			if(list.size() > 1){
				//find same condition entries
				Map<String, List<LineEntry>> equalsBucket = bucketByConditionEqualsTo(list);

				boolean hasSameConditions = equalsBucket.values().stream().map(List::size).anyMatch(count -> count > 1);
				if(hasSameConditions){
					//expand same condition entries
					boolean expansionHappened = expandOverlappingRules(equalsBucket);

					//expand again if needed
					while(expansionHappened){
						expansionHappened = false;
						for(List<LineEntry> set : equalsBucket.values()){
							equalsBucket = bucketByConditionEqualsTo(set);

							//expand same condition entries
							hasSameConditions = equalsBucket.values().stream().map(List::size).anyMatch(count -> count > 1);
							if(hasSameConditions)
								expansionHappened |= expandOverlappingRules(equalsBucket);
						}
					}
					for(List<LineEntry> set : equalsBucket.values())
						for(LineEntry le : set)
							bucket.put(le.condition, Collections.singletonList(le));
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
		return bucket;
	}

	private List<LineEntry> collectByCondition(List<LineEntry> entries, Function<String, Boolean> comparator){
		List<LineEntry> list = new ArrayList<>();
		Iterator<LineEntry> itr = entries.iterator();
		while(itr.hasNext()){
			LineEntry entry = itr.next();
			if(comparator.apply(entry.condition)){
				itr.remove();

				list.add(entry);
			}
		}
		return list;
	}

	private Map<String, LineEntry> removeOverlappingRules(Map<String, List<LineEntry>> bucketedEntries){
		Map<String, LineEntry> nonOverlappingBucketedEntries = new HashMap<>();
		for(Map.Entry<String, List<LineEntry>> entry : bucketedEntries.entrySet()){
			List<LineEntry> aggregatedRules = entry.getValue();
			if(aggregatedRules.size() > 1){
				removeOverlappingConditions(aggregatedRules);

				for(LineEntry en : aggregatedRules)
					nonOverlappingBucketedEntries.put(en.condition, en);
			}
			else
				nonOverlappingBucketedEntries.put(entry.getKey(), aggregatedRules.get(0));
		}
		return nonOverlappingBucketedEntries;
	}

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
		List<String> sortedLetters = letters.stream().map(String::valueOf).collect(Collectors.toList());
		Collections.sort(sortedLetters, comparator);
		String addedCondition = StringUtils.join(sortedLetters, StringUtils.EMPTY);
		firstRule.condition = NOT_GROUP_START + addedCondition + GROUP_END + firstRule.condition;

		return aggregatedRules;
	}

	private Map<String, List<LineEntry>> bucketByConditionEqualsTo(List<LineEntry> entries){
		List<LineEntry> copy = new ArrayList<>(entries);
		Map<String, List<LineEntry>> bucket = new HashMap<>();
		while(!copy.isEmpty()){
			//collect all entries that has the condition that is `condition`
			String condition = copy.get(0).condition;
			List<LineEntry> list = collectByCondition(copy, a -> a.equals(condition));

			bucket.put(condition, list);
		}
		return bucket;
	}

	private boolean expandOverlappingRules(Map<String, List<LineEntry>> bucket){
		boolean expanded = false;
		for(List<LineEntry> entries : bucket.values())
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
						//entry1: AB
						//entry2: CB
						//extract B, keep the rules
						//now I have
						//entry1: A
						//entry2: C
						//add to A and C, [^B]
						entries = expandConditions(entries);

/*
SFX v1 o sta io	po:noun
SFX v1 o ista io	po:noun
SFX v1 o ista [^i]o
*/
						//TODO collect next letters, if it's possible...

throw new RuntimeException("to be tested");
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
		return expanded;
	}

	private List<LineEntry> extractCommonRules(List<LineEntry> entries, Set<Character> intersection){
		Iterator<LineEntry> itr = entries.iterator();
		List<LineEntry> commonRules = new ArrayList<>();
		while(itr.hasNext()){
			LineEntry entry = itr.next();

			boolean found = false;
			for(String word : entry.originalWords){
				char chr = (word.charAt(word.length() - entry.condition.length() - 1));
				if(intersection.contains(chr)){
					commonRules.add(new LineEntry(entry.removal, entry.addition, chr + entry.condition, word));

					found = true;
				}
			}
			if(found)
				itr.remove();
		}
		return commonRules;
	}

	//expand conditions by one letter
	private List<LineEntry> expandConditions(List<LineEntry> entries){
		List<LineEntry> expandedEntries = new ArrayList<>();
		for(LineEntry en : entries){
			Iterator<String> words = en.originalWords.iterator();
			while(words.hasNext()){
				String originalWord = words.next();

				int startingIndex = originalWord.length() - en.condition.length() - 1;
				if(startingIndex >= 0){
					String newCondition = originalWord.substring(startingIndex);
					LineEntry newEntry = new LineEntry(en.removal, en.addition, newCondition, originalWord);
					int index = expandedEntries.indexOf(newEntry);
					if(index >= 0)
						expandedEntries.get(index).originalWords.add(originalWord);
					else
						expandedEntries.add(newEntry);
				}
				else{
					LOGGER.info(Backbone.MARKER_APPLICATION, "Cannot reduce rule (because of '{0}' that is too short)", originalWord);

					throw new IllegalArgumentException("Cannot reduce rule");
				}
			}
		}
		entries.clear();
		entries.addAll(expandedEntries);
		return entries;
	}

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
		for(LineEntry en : entries){
			Set<Character> set = new HashSet<>();
			for(String originalWord : en.originalWords){
				char newLetterCondition = originalWord.charAt(originalWord.length() - en.condition.length() - 1);
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

	private List<String> reduceEntriesToRules(RuleEntry originalRuleEntry, List<LineEntry> nonOverlappingBucketedEntries){
		int size = nonOverlappingBucketedEntries.size();
		AffixEntry.Type type = (originalRuleEntry.isSuffix()? AffixEntry.Type.SUFFIX: AffixEntry.Type.PREFIX);
		String flag = originalRuleEntry.getEntries().get(0).getFlag();

		nonOverlappingBucketedEntries.sort(lineEntryComparator);

		List<String> rules = new ArrayList<>(size);
		for(LineEntry entry : nonOverlappingBucketedEntries)
			rules.add(composeLine(type, flag, entry));
		return rules;
	}

	private String composeHeader(AffixEntry.Type type, String flag, boolean isCombineable, int size){
		StringBuilder sb = new StringBuilder();
		return sb.append(type.getFlag().getCode())
			.append(StringUtils.SPACE)
			.append(flag)
			.append(StringUtils.SPACE)
			.append(isCombineable? RuleEntry.COMBINEABLE: RuleEntry.NOT_COMBINEABLE)
			.append(StringUtils.SPACE)
			.append(size)
			.toString();
	}

	private String composeLine(AffixEntry.Type type, String flag, LineEntry partialLine){
		StringBuilder sb = new StringBuilder();
		sb.append(type.getFlag().getCode())
			.append(StringUtils.SPACE)
			.append(flag)
			.append(StringUtils.SPACE)
			.append(partialLine.removal)
			.append(StringUtils.SPACE);
		int idx = partialLine.addition.indexOf(TAB);
		if(idx >= 0)
			sb.append(partialLine.addition.substring(0, idx))
				.append(StringUtils.SPACE)
				.append(partialLine.condition)
				.append(TAB)
				.append(partialLine.addition.substring(idx + 1));
		else
			sb.append(partialLine.addition)
				.append(StringUtils.SPACE)
				.append(partialLine.condition);
		return sb.toString();
	}

}
