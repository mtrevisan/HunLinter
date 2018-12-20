package unit731.hunspeller.parsers.dictionary.workers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.collections.radixtree.sequencers.RegExpSequencer;
import unit731.hunspeller.languages.BaseBuilder;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.AffixEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class RuleReducerWorker extends WorkerDictionaryBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(RuleReducerWorker.class);

	public static final String WORKER_NAME = "Rule reducer";

	private static final String SLASH = "/";

	private static final String NOT_GROUP_STARTING = "[^";
	private static final String GROUP_STARTING = "[";
	private static final String GROUP_ENDING = "]";

	private static final RegExpSequencer SEQUENCER = new RegExpSequencer();


	/** NOTE: this works only if the rules produce only one output! ... for now */
	public RuleReducerWorker(AffixParser affParser, DictionaryParser dicParser, WordGenerator wordGenerator, ReadWriteLockable lockable){
		Objects.requireNonNull(affParser);
		Objects.requireNonNull(wordGenerator);

String flag = "v1";
		RuleEntry originalRuleEntry = (RuleEntry)affParser.getData(flag);
		if(originalRuleEntry == null)
			throw new IllegalArgumentException("Non-existent rule " + flag + ", cannot reduce");

		boolean isSuffix = originalRuleEntry.isSuffix();
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		List<String> aliasesFlag = affParser.getData(AffixTag.ALIASES_FLAG);
		List<String> aliasesMorphologicalField = affParser.getData(AffixTag.ALIASES_MORPHOLOGICAL_FIELD);
		Comparator<String> comparator = BaseBuilder.getComparator(affParser.getLanguage());
		Set<Pair<String, String>> newAffixEntries = new HashSet<>();
		BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLineWithAliases(line, strategy, aliasesFlag, aliasesMorphologicalField);
			dicEntry.applyConversionTable(affParser::applyInputConversionTable);

			if(dicEntry.hasContinuationFlag(flag)){
				String word = dicEntry.getWord();
				int wordLength = word.length();
				String lastLetter = word.substring(wordLength - 1);
				List<Production> productions = wordGenerator.applySingleAffixRule(word + SLASH + flag);

				if(productions.size() > 1)
					throw new IllegalArgumentException("Rule " + flag + " produced more than one output, cannot reduce");

				productions.forEach(production -> 
					newAffixEntries.add(isSuffix? createSuffixEntry(production, wordLength, word, lastLetter):
						createPrefixEntry(production, wordLength, word, lastLetter))
				);
			}
		};
		Runnable completed = () -> {
			//aggregate rules
			List<Pair<String, String>> aggregatedAffixEntries = new ArrayList<>();
			List<Pair<String, String>> entries = sortEntries(newAffixEntries);
			while(!entries.isEmpty()){
				Pair<String, String> affixEntry = entries.get(0);

				List<Pair<String, String>> collisions = collectEntries(affixEntry, entries);

				//remove matched entries
				collisions.forEach(entry -> entries.remove(entry));

				//if there are more than one collisions
				if(collisions.size() > 1){
					//bucket collisions by length
					Map<Integer, List<Pair<String, String>>> bucket = bucketForLength(collisions, comparator);

					//generate regex from input:
					//perform a one-leap step through the buckets
					Iterator<List<Pair<String, String>>> itr = bucket.values().iterator();
					List<Pair<String, String>> startingList = itr.next();
					while(itr.hasNext()){
						List<Pair<String, String>> nextList = itr.next();
						if(!nextList.isEmpty())
							joinCollisions(startingList, nextList, comparator);

						startingList = nextList;
					}
					//perform a two-leaps step through the buckets
					itr = bucket.values().iterator();
					startingList = itr.next();
					if(itr.hasNext()){
						List<Pair<String, String>> intermediateList = itr.next();
						while(itr.hasNext()){
							List<Pair<String, String>> nextList = itr.next();
							if(!nextList.isEmpty())
								joinCollisions(startingList, nextList, comparator);

							startingList = intermediateList;
							intermediateList = nextList;
						}
					}
					//three-leaps? n-leaps?
					//TODO

					//store aggregated entries
					bucket.values()
						.forEach(aggregatedAffixEntries::addAll);
				}
				//otherwise store entry and pass to the next
				else
					aggregatedAffixEntries.add(affixEntry);
			}

			AffixEntry.Type type = (isSuffix? AffixEntry.Type.SUFFIX: AffixEntry.Type.PREFIX);
			LOGGER.info(Backbone.MARKER_APPLICATION, composeHeader(type, flag, originalRuleEntry.isCombineable(), aggregatedAffixEntries.size()));
			aggregatedAffixEntries.stream()
				.map(entry -> composeLine(type, flag, entry))
				.forEach(entry -> LOGGER.info(Backbone.MARKER_APPLICATION, entry));
		};
		createReadParallelWorker(WORKER_NAME, dicParser, lineProcessor, completed, null, lockable);
	}

	/** Sort entries by shortest condition */
	private List<Pair<String, String>> sortEntries(Set<Pair<String, String>> set){
		List<Pair<String, String>> sortedList = new ArrayList<>(set);
		sortedList.sort((entry1, entry2) ->
			Integer.compare(entry1.getRight().length(), entry2.getRight().length())
		);
		return sortedList;
	}

	private List<Pair<String, String>> collectEntries(Pair<String, String> affixEntry, List<Pair<String, String>> entries){
		//collect all the entries that have affixEntry as last part of the condition
		String affixEntryCondition = affixEntry.getRight();
		List<Pair<String, String>> collisions = new ArrayList<>();
		collisions.add(affixEntry);
		for(int i = 1; i < entries.size(); i ++){
			Pair<String, String> targetAffixEntry = entries.get(i);
			String targetAffixEntryCondition = targetAffixEntry.getRight();
			if(targetAffixEntryCondition.endsWith(affixEntryCondition))
				collisions.add(Pair.of(targetAffixEntry.getLeft(), targetAffixEntryCondition));
		}
		return collisions;
	}

	private void joinCollisions(List<Pair<String, String>> startingList, List<Pair<String, String>> nextList, Comparator<String> comparator){
		//extract the prior-to-last letter
		int discriminatorIndex = nextList.get(0).getRight().length() - 2;
		int size = startingList.size();
		for(int i = 0; i < size; i ++){
			Pair<String, String> affixEntry = startingList.get(i);
			String affixEntryCondition = affixEntry.getRight();
			String[] startingCondition = RegExpSequencer.splitSequence(affixEntryCondition);
			String affixEntryLine = affixEntry.getLeft();
			//strip affixEntry's condition and collect
			List<String> otherConditions = nextList.stream()
				.map(Pair::getRight)
				.filter(condition -> SEQUENCER.endsWith(RegExpSequencer.splitSequence(condition), startingCondition))
				.map(condition -> condition.charAt(discriminatorIndex))
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
						startingList.add(Pair.of(affixEntryLine, letter));

					//merge conditions
					Stream<String> startingConditionStream = Arrays.stream(startingCondition[discriminatorIndex - 1].substring(2, startingCondition[discriminatorIndex - 1].length() - 1).split(StringUtils.EMPTY));
					other = Stream.concat(startingConditionStream, otherConditions.stream())
						.distinct();
					affixEntryCondition = StringUtils.join(ArrayUtils.remove(startingCondition, 0));
				}
				else if(discriminatorIndex + 1 > SEQUENCER.length(startingCondition)){
					//collect intermediate letters
					Collection<String> letterBucket = bucketForLetter(nextList, discriminatorIndex, comparator);

					for(String letter : letterBucket)
						startingList.add(Pair.of(affixEntryLine, letter));

					other = otherConditions.stream();
				}
				else{
					other = otherConditions.stream();
				}

				String otherCondition = other
					.sorted(comparator)
					.collect(Collectors.joining());
				startingList.set(i, Pair.of(affixEntryLine, NOT_GROUP_STARTING + otherCondition + GROUP_ENDING + affixEntryCondition));
			}
		}
	}

	private Pair<String, String> createSuffixEntry(Production production, int wordLength, String word, String lastLetter){
		int lastCommonLetter;
		String producedWord = production.getWord();
		for(lastCommonLetter = 0; lastCommonLetter < Math.min(wordLength, producedWord.length()); lastCommonLetter ++)
			if(word.charAt(lastCommonLetter) != producedWord.charAt(lastCommonLetter))
				break;

		String removal = (lastCommonLetter < wordLength? word.substring(lastCommonLetter): AffixEntry.ZERO);
		String addition = (lastCommonLetter < producedWord.length()? producedWord.substring(lastCommonLetter): AffixEntry.ZERO);
		String condition = (lastCommonLetter < wordLength? removal: lastLetter);
		String newAffixEntry = composeLine(removal, addition);
		return Pair.of(newAffixEntry, condition);
	}

	private Pair<String, String> createPrefixEntry(Production production, int wordLength, String word, String lastLetter){
		Pair<String, String> entry;
		int firstCommonLetter;
		String producedWord = production.getWord();
		for(firstCommonLetter = 0; firstCommonLetter < Math.min(wordLength, producedWord.length()); firstCommonLetter ++)
			if(word.charAt(firstCommonLetter) == producedWord.charAt(firstCommonLetter))
				break;
		String removal = (firstCommonLetter < wordLength? word.substring(0, firstCommonLetter): AffixEntry.ZERO);
		String addition = producedWord.substring(0, firstCommonLetter);
		String condition = (firstCommonLetter < wordLength? removal: lastLetter);
		String newAffixEntry = composeLine(removal, addition);
		entry = Pair.of(newAffixEntry, condition);
		return entry;
	}

	private String composeLine(String removal, String addition){
		StringBuilder sb = new StringBuilder();
		return sb.append(removal)
			.append(StringUtils.SPACE)
			.append(addition)
			.toString();
	}

	private Map<Integer, List<Pair<String, String>>> bucketForLength(List<Pair<String, String>> entries, Comparator<String> comparator){
		Map<Integer, List<Pair<String, String>>> bucket = new HashMap<>();
		for(Pair<String, String> entry : entries)
			bucket.computeIfAbsent(entry.getRight().length(), k -> new ArrayList<>())
				.add(entry);
		//order lists
		Comparator<Pair<String, String>> comp = (pair1, pair2) -> comparator.compare(pair1.getValue(), pair2.getValue());
		for(Map.Entry<Integer, List<Pair<String, String>>> bag : bucket.entrySet())
			bag.getValue().sort(comp);
		return bucket;
	}

	private Collection<String> bucketForLetter(List<Pair<String, String>> entries, int index, Comparator<String> comparator){
		//collect by letter at given index
		Map<String, String> bucket = new HashMap<>();
		for(Pair<String, String> entry : entries){
			String condition = entry.getRight();
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

	private String composeLine(AffixEntry.Type type, String flag, Pair<String, String> partialLine){
		StringBuilder sb = new StringBuilder();
		return sb.append(type.getFlag().getCode())
			.append(StringUtils.SPACE)
			.append(flag)
			.append(StringUtils.SPACE)
			.append(partialLine.getLeft())
			.append(StringUtils.SPACE)
			.append(partialLine.getRight())
			.toString();
	}

}
