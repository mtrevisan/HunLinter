package unit731.hunspeller.parsers.dictionary.workers;

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
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.collections.radixtree.sequencers.RegExpSequencer;
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

	private static final String NOT_GROUP_STARTING = "[^";
	private static final String GROUP_STARTING = "[";
	private static final String GROUP_ENDING = "]";

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


	/** NOTE: this works only if the rules produce only one output! ... for now */
	public RuleReducerWorker(AffixData affixData, DictionaryParser dicParser, WordGenerator wordGenerator){
		Objects.requireNonNull(affixData);
		Objects.requireNonNull(wordGenerator);

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
			Map<String, List<LineEntry>> aggregatedFlaggedEntries = aggregateEntries(flaggedEntries);

			List<LineEntry> reducedEntries = reduceEntriesToRules(aggregatedFlaggedEntries);

//			manageCollision(affixEntry, newAffixEntries);
/*
problem:
SFX v1 o ista/A2 [^i]o
SFX v1 o sta/A2 io
would be reduced to
SFX v1 o ista/A2 o
SFX v1 o sta/A2 o
how do I know the -o condition is not enough but it is necessary another character (-[^i]o, -io)?
if conditions are equals and the adding parts are one inside the other, take the shorter (sta/A2), add another char to the condition (io),
add the negated char to the other rule (ista/A2 [^i]o)
*/

System.out.println("");
			//aggregate rules
//			List<LineEntry> aggregatedAffixEntries = new ArrayList<>();
//			Comparator<String> comparator = BaseBuilder.getComparator(affixData.getLanguage());
//			while(!entries.isEmpty()){
//				LineEntry affixEntry = entries.get(0);
//
//				List<LineEntry> collisions = collectEntries(affixEntry, entries);
//
//				//remove matched entries
//				collisions.forEach(entry -> entries.remove(entry));
//
//				//if there are more than one collisions
//				if(collisions.size() > 1){
//					//bucket collisions by length
//					Map<Integer, List<LineEntry>> bucket = bucketForLength(collisions, comparator);
//
//					//generate regex from input:
//					//perform a one-leap step through the buckets
//					Iterator<List<LineEntry>> itr = bucket.values().iterator();
//					List<LineEntry> startingList = itr.next();
//					while(itr.hasNext()){
//						List<LineEntry> nextList = itr.next();
//						if(!nextList.isEmpty())
//							joinCollisions(startingList, nextList, comparator);
//
//						startingList = nextList;
//					}
//					//perform a two-leaps step through the buckets
//					itr = bucket.values().iterator();
//					startingList = itr.next();
//					if(itr.hasNext()){
//						List<LineEntry> intermediateList = itr.next();
//						while(itr.hasNext()){
//							List<LineEntry> nextList = itr.next();
//							if(!nextList.isEmpty())
//								joinCollisions(startingList, nextList, comparator);
//
//							startingList = intermediateList;
//							intermediateList = nextList;
//						}
//					}
//					//three-leaps? n-leaps?
//					//TODO
//
//					//store aggregated entries
//					bucket.values()
//						.forEach(aggregatedAffixEntries::addAll);
//				}
//				//otherwise store entry and pass to the next
//				else
//					aggregatedAffixEntries.add(affixEntry);
//			}

//			AffixEntry.Type type = (originalRuleEntry.isSuffix()? AffixEntry.Type.SUFFIX: AffixEntry.Type.PREFIX);
//			LOGGER.info(Backbone.MARKER_APPLICATION, composeHeader(type, flag, originalRuleEntry.isCombineable(), aggregatedAffixEntries.size()));
//			aggregatedAffixEntries.stream()
//				.map(entry -> composeLine(type, flag, entry))
//				.forEach(entry -> LOGGER.info(Backbone.MARKER_APPLICATION, entry));
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
					newAffixEntries.get(index).originalWords.addAll(affixEntry.originalWords);
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

	private Map<String, List<LineEntry>> aggregateEntries(List<LineEntry> entries){
		//sort entries by shortest condition
		entries.sort((entry1, entry2) -> Integer.compare(entry1.condition.length(), entry2.condition.length()));

		Map<String, List<LineEntry>> bucket = new HashMap<>();
		while(!entries.isEmpty()){
			List<LineEntry> list = new ArrayList<>();

			//collect all entries that has the condition that ends with `condition`
			String condition = entries.get(0).condition;
			Iterator<LineEntry> itr = entries.iterator();
			while(itr.hasNext()){
				LineEntry entry = itr.next();
				if(entry.condition.endsWith(condition)){
					itr.remove();
					list.add(entry);
				}
			}

			bucket.put(condition, list);
		}
		return bucket;
	}

	private List<LineEntry> reduceEntriesToRules(Map<String, List<LineEntry>> aggregatedFlaggedEntries){
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

//	private void manageCollision(LineEntry affixEntry, Set<LineEntry> newAffixEntries){
//		//search newAffixEntries for collisions on condition
//		for(LineEntry entry : newAffixEntries)
//			if(entry.condition.equals(affixEntry.condition)){
//				//find last different character from entry.originalWord and affixEntry.originalWord
//				int idx;
//				for(idx = 1; idx <= Math.min(entry.originalWord.length(), affixEntry.originalWord.length()); idx ++)
//					if(entry.originalWord.charAt(entry.originalWord.length() - idx) != affixEntry.originalWord.charAt(affixEntry.originalWord.length() - idx))
//						break;
//				
//				if(entry.addition.endsWith(affixEntry.addition)){
//					//add another letter to the condition of entry
//					entry.condition = entry.originalWord.substring(entry.originalWord.length() - idx);
//					affixEntry.condition = NOT_GROUP_STARTING + entry.condition.charAt(0) + GROUP_ENDING + entry.condition.substring(1);
//				}
//				else{
//					//add another letter to the condition of affixEntry
//					affixEntry.condition = affixEntry.originalWord.substring(affixEntry.originalWord.length() - idx);
//					entry.condition = NOT_GROUP_STARTING + entry.condition.charAt(0) + GROUP_ENDING + affixEntry.condition.substring(1);
//				}
//				break;
//			}
//	}

	private List<LineEntry> collectEntries(LineEntry affixEntry, List<LineEntry> entries){
		//collect all the entries that have affixEntry as last part of the condition
		String affixEntryCondition = affixEntry.condition;
		Set<LineEntry> collisions = new HashSet<>();
		collisions.add(affixEntry);
		for(int i = 1; i < entries.size(); i ++){
			LineEntry targetAffixEntry = entries.get(i);
			String targetAffixEntryCondition = targetAffixEntry.condition;
			if(targetAffixEntryCondition.endsWith(affixEntryCondition))
				collisions.add(new LineEntry(targetAffixEntry.removal, targetAffixEntry.addition, targetAffixEntryCondition));
		}
		return new ArrayList<>(collisions);
	}

	private void joinCollisions(List<LineEntry> startingList, List<LineEntry> nextList, Comparator<String> comparator){
		//extract the prior-to-last letter
		int size = startingList.size();
		for(int i = 0; i < size; i ++){
			LineEntry affixEntry = startingList.get(i);
			String affixEntryCondition = affixEntry.condition;
			String[] startingCondition = RegExpSequencer.splitSequence(affixEntryCondition);
			int discriminatorIndex = startingCondition.length;
			String affixEntryRemoval = affixEntry.removal;
			String affixEntryAddition = affixEntry.addition;
			//strip affixEntry's condition and collect
			List<String> otherConditions = nextList.stream()
				.map(entry -> entry.condition)
				.map(RegExpSequencer::splitSequence)
				.filter(condition -> SEQUENCER.endsWith(condition, startingCondition))
				.map(condition -> condition[condition.length - discriminatorIndex])
				.distinct()
				.map(String::valueOf)
				.collect(Collectors.toList());
			if(!otherConditions.isEmpty()){
				Stream<String> other;
				//if this condition.length > startingCondition.length + 1, then add in-between rules
				if(discriminatorIndex > 0 && discriminatorIndex + 1 == SEQUENCER.length(startingCondition)){
					//collect intermediate letters
					Collection<String> letterBucket = bucketForLetter(nextList, discriminatorIndex, comparator);

					for(String letter : letterBucket)
						startingList.add(new LineEntry(affixEntryRemoval, affixEntryAddition, letter));

					//merge conditions
					Stream<String> startingConditionStream = Arrays.stream(startingCondition[discriminatorIndex - 1].substring(2,
						startingCondition[discriminatorIndex - 1].length() - 1).split(StringUtils.EMPTY));
					other = Stream.concat(startingConditionStream, otherConditions.stream())
						.distinct();
					affixEntryCondition = StringUtils.join(ArrayUtils.remove(startingCondition, 0));
				}
				else if(discriminatorIndex + 1 > SEQUENCER.length(startingCondition)){
					//collect intermediate letters
					Collection<String> letterBucket = bucketForLetter(nextList, discriminatorIndex, comparator);

					for(String letter : letterBucket)
						startingList.add(new LineEntry(affixEntryRemoval, affixEntryAddition, letter));

					other = otherConditions.stream();
				}
				else{
					other = otherConditions.stream();
				}

				String otherCondition = other
					.sorted(comparator)
					.collect(Collectors.joining());
				startingList.set(i, new LineEntry(affixEntryRemoval, affixEntryAddition, NOT_GROUP_STARTING + otherCondition + GROUP_ENDING
					+ affixEntryCondition));
			}
		}
	}

	private Map<Integer, List<LineEntry>> bucketForLength(List<LineEntry> entries, Comparator<String> comparator){
		Map<Integer, List<LineEntry>> bucket = new HashMap<>();
		for(LineEntry entry : entries){
			int entryLength = entry.condition.length() - (entry.condition.startsWith(NOT_GROUP_STARTING)? 3:
				(entry.condition.startsWith(GROUP_STARTING)? 2: 0));
			bucket.computeIfAbsent(entryLength, k -> new ArrayList<>())
				.add(entry);
		}
		//order lists
		Comparator<LineEntry> comp = (pair1, pair2) -> comparator.compare(pair1.condition, pair2.condition);
		for(Map.Entry<Integer, List<LineEntry>> bag : bucket.entrySet())
			bag.getValue().sort(comp);
		return bucket;
	}

	private Collection<String> bucketForLetter(List<LineEntry> entries, int index, Comparator<String> comparator){
		//collect by letter at given index
		Map<String, String> bucket = new HashMap<>();
		for(LineEntry entry : entries){
			String condition = entry.condition;
			String key = String.valueOf(condition.charAt(index));
			String bag = bucket.get(key);
			if(bag != null)
				condition = Arrays.asList(bag.charAt(index - 1), condition.charAt(index - 1)).stream()
					.map(String::valueOf)
					.sorted(comparator)
					.collect(Collectors.joining(StringUtils.EMPTY, NOT_GROUP_STARTING, GROUP_ENDING))
					+ condition.substring(index);
			bucket.put(key, condition);
		}

		//convert non-merged conditions
		List<String> valuesBucket = bucket.values().stream()
			.map(condition -> (condition.charAt(0) == '['? condition: NOT_GROUP_STARTING + condition.charAt(0) + GROUP_ENDING + condition.substring(index)))
			.collect(Collectors.toList());
		bucket.clear();

		//merge non-merged conditions
		for(String condition : valuesBucket){
			int idx = condition.indexOf(GROUP_ENDING);
			String key = condition.substring(0, idx + 1);
			String bag = bucket.get(key);
			if(bag != null){
				String[] letters = (bag.charAt(idx + 1) == '['?
					bag.substring(idx + 2, bag.indexOf(']', idx + 2)).split(StringUtils.EMPTY):
					new String[]{String.valueOf(bag.charAt(idx + 1))});
				letters = ArrayUtils.add(letters, String.valueOf(condition.charAt(idx + 1)));
				condition = key
					+ Arrays.asList(letters).stream()
					.sorted(comparator)
					.collect(Collectors.joining(StringUtils.EMPTY, GROUP_STARTING, GROUP_ENDING))
					+ condition.substring(idx + 2);
			}
			bucket.put(key, condition);
		}
		return bucket.values();
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
			.append(partialLine.removal);
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
