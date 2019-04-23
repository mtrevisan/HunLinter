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
import java.util.StringJoiner;
import java.util.function.BiConsumer;
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

	private static final String TAB = "\t";
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
			if(word != null)
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
			.thenComparing(Comparator.comparing(entry -> entry.condition))
			.thenComparing(Comparator.comparingInt(entry -> entry.removal.length()))
			.thenComparing(Comparator.comparing(entry -> entry.removal))
			.thenComparing(Comparator.comparingInt(entry -> entry.addition.length()))
			.thenComparing(Comparator.comparing(entry -> entry.addition));

//String flag = "%0"; //mi[lƚ]e
String flag = "<2";
//NOTE: if the rules are from a closed group, then `keepLongestCommonAffix` should be true
//flag name for input should be "Optimize for closed group"?
boolean keepLongestCommonAffix = false;
		RuleEntry originalRuleEntry = (RuleEntry)affixData.getData(flag);
		if(originalRuleEntry == null)
			throw new IllegalArgumentException("Non-existent rule " + flag + ", cannot reduce");

		Map<String, LineEntry> equivalenceTable = new HashMap<>();
		BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			List<Production> productions = wordGenerator.applyAffixRules(line);

			collectProductionsByFlag(productions, flag, equivalenceTable);
		};
		Runnable completed = () -> {
			//extract rules with missing conditions
			List<LineEntry> missingConditions = new ArrayList<>();
			Iterator<Map.Entry<String, LineEntry>> itr = equivalenceTable.entrySet().iterator();
			while(itr.hasNext()){
				Map.Entry<String, LineEntry> elem = itr.next();
				if(elem.getValue().condition.isEmpty()){
					missingConditions.add(elem.getValue());

					itr.remove();
				}
			}
			//reinsert rules with missing conditions with a condition
			for(LineEntry entry : missingConditions){
				String lcs = longestCommonSuffix(entry.from, null);
				if(lcs.isEmpty()){
					//no common suffix present:
					//bucket by last character
					Map<Character, Set<String>> lastCharacterBucket = new HashMap<>();
					for(String from : entry.from)
						lastCharacterBucket.computeIfAbsent(from.charAt(from.length() - 1), k -> new HashSet<>())
							.add(from);
					//insert new rules
					for(Map.Entry<Character, Set<String>> conditions : lastCharacterBucket.entrySet()){
						lcs = (conditions.getValue().size() == 1?
							String.valueOf(conditions.getKey()):
							longestCommonSuffix(conditions.getValue(), null));
						LineEntry e = new LineEntry(entry.removal, entry.addition, lcs);
						e.from.addAll(conditions.getValue());
						collectIntoEquivalenceClasses(e, equivalenceTable);
					}
				}
				else{
					//common suffix present:
					entry.condition = lcs;
					collectIntoEquivalenceClasses(entry, equivalenceTable);
				}
			}

//System.out.println("entries:");
//for(Map.Entry<String, LineEntry> e : equivalenceTable.entrySet())
//	System.out.println(e.getKey() + ": " + StringUtils.join(e.getValue(), ","));
			AffixEntry.Type type = (originalRuleEntry.isSuffix()? AffixEntry.Type.SUFFIX: AffixEntry.Type.PREFIX);
			try{
//				reduceEquivalenceClasses(type, entriesTable);
//System.out.println("cleaned entries:");
//for(Map.Entry<String, List<LineEntry>> e : entriesTable.entrySet())
//	System.out.println(e.getKey() + ": " + StringUtils.join(e.getValue(), ","));

				List<String> rules = convertEntriesToRules(originalRuleEntry, keepLongestCommonAffix, equivalenceTable);

				LOGGER.info(Backbone.MARKER_APPLICATION, composeHeader(type, flag, originalRuleEntry.isCombineable(), rules.size()));
				rules.stream()
					.forEach(rule -> LOGGER.info(Backbone.MARKER_APPLICATION, rule));
			}
			catch(Exception e){
				LOGGER.info(Backbone.MARKER_APPLICATION, e.getMessage());
			}



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
//		WorkerData data = WorkerData.createParallel(WORKER_NAME, dicParser);
		WorkerData data = WorkerData.create(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		createReadWorker(data, lineProcessor);
	}

	private void collectProductionsByFlag(List<Production> productions, String flag, Map<String, LineEntry> equivalenceTable){
		Iterator<Production> itr = productions.iterator();
		//skip base production
		itr.next();
		while(itr.hasNext()){
			Production production = itr.next();

			AffixEntry lastAppliedRule = production.getLastAppliedRule();
			if(lastAppliedRule != null && lastAppliedRule.getFlag().equals(flag)){
				String word = lastAppliedRule.undoRule(production.getWord());
				LineEntry affixEntry = (lastAppliedRule.isSuffix()? createSuffixEntry(production, word): createPrefixEntry(production, word));

				collectIntoEquivalenceClasses(affixEntry, equivalenceTable);
			}
		}
	}

	private void collectIntoEquivalenceClasses(LineEntry affixEntry, Map<String, LineEntry> equivalenceTable){
		String key = affixEntry.removal + TAB + affixEntry.addition + TAB + affixEntry.condition;
		LineEntry ruleSet = equivalenceTable.putIfAbsent(key, affixEntry);
		if(ruleSet != null)
			ruleSet.from.addAll(affixEntry.from);
	}

	private void collectIntoEquivalenceClasses(LineEntry affixEntry, Set<LineEntry> equivalenceTable){
		LineEntry foundClass = null;
		for(LineEntry eq : equivalenceTable)
			if(affixEntry.condition.isEmpty() && eq.condition.isEmpty()
					|| !affixEntry.condition.isEmpty() && !eq.condition.isEmpty()
						&& (eq.condition.endsWith(affixEntry.condition) || affixEntry.condition.endsWith(eq.condition))){
				foundClass = eq;
				break;
			}
		if(foundClass != null)
			foundClass.from.addAll(affixEntry.from);
		else
			equivalenceTable.add(affixEntry);
	}

	private void reduceEquivalenceClasses(AffixEntry.Type type, Map<String, List<LineEntry>> entriesTable){
		while(true){
			Set<List<String>> equivalenceClasses = calculateEquivalenceClasses(entriesTable);
			if(equivalenceClasses.stream().map(List::size).allMatch(size -> size == 1))
				break;
			
			removeCollidingClasses(equivalenceClasses, type, entriesTable);
		}
	}

	private Set<List<String>> calculateEquivalenceClasses(Map<String, List<LineEntry>> entriesTable){
		Set<List<String>> equivalenceClasses = new HashSet<>();
		for(String cond : entriesTable.keySet()){
			List<String> foundClass = null;
			for(List<String> cl : equivalenceClasses)
				if(cl.stream().anyMatch(eq -> eq.endsWith(cond) || cond.endsWith(eq))){
					foundClass = cl;
					break;
				}
			if(foundClass != null)
				foundClass.add(cond);
			else
				equivalenceClasses.add(new ArrayList<>(Arrays.asList(cond)));
		}
		return equivalenceClasses;
	}

	private void removeCollidingClasses(Set<List<String>> equivalenceClasses, AffixEntry.Type type, Map<String, List<LineEntry>> entriesTable){
		//order equivalence classes
		equivalenceClasses.forEach(classes -> classes.sort(Comparator.comparing(String::length).reversed()));

		for(List<String> equivalenceClass : equivalenceClasses){
			int classes = equivalenceClass.size();
			if(classes > 1){
				Iterator<String> itr = equivalenceClass.iterator();
				String baseKey = itr.next();
				while(itr.hasNext()){
					String currentKey = itr.next();

					//prepend/append characters to currentKey until it is no longer contained into baseKey
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
				collectFlagProduction(splittedEntry, splittedEntry.from, entriesTable);
		}
	}

	private void collectFlagProduction(LineEntry affixEntry, Set<String> from, Map<String, List<LineEntry>> entries){
		List<LineEntry> list = entries.computeIfAbsent(affixEntry.condition, k -> new ArrayList<>());
		int idx = list.indexOf(affixEntry);
		if(idx >= 0)
			list.get(idx).from.addAll(from != null? from: affixEntry.from);
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

	private List<String> convertEntriesToRules(RuleEntry originalRuleEntry, boolean keepLongestCommonAffix,
			Map<String, LineEntry> equivalenceTable){
		List<LineEntry> entries = equivalenceTable.values().stream()
			.sorted(lineEntryComparator)
			.collect(Collectors.toList());

		entries = compressRules(entries, keepLongestCommonAffix);

		return composeAffixRules(originalRuleEntry, entries);
	}

	private List<LineEntry> compressRules(List<LineEntry> entries, boolean keepLongestCommonAffix){
		Map<String, Set<LineEntry>> bucket = bucketByRemovalAndAddingAndCondition(entries);

		List<LineEntry> compressedRules = compressBucketedRules(bucket);

		//FIXME
		if(!keepLongestCommonAffix){
			List<LineEntry> additionalRules = reduceNotConditions(compressedRules);
			compressedRules.addAll(additionalRules);
		}
		else{
			//TODO
			//check for collisions between conditions
		}

		//extract compressed rules
		return prepareRules(keepLongestCommonAffix, compressedRules);
	}

	private Map<String, Set<LineEntry>> bucketByRemovalAndAddingAndCondition(Collection<LineEntry> entries){
		Map<String, Set<LineEntry>> bucket = new HashMap<>();
		for(LineEntry entry : entries){
			String key = entry.removal + TAB + entry.addition + TAB + entry.condition;
			bucket.computeIfAbsent(key, k -> new HashSet<>())
				.add(entry);
		}
		return bucket;
	}

	private List<LineEntry> compressBucketedRules(Map<String, Set<LineEntry>> bucket){
		List<LineEntry> compressedRules = new ArrayList<>();
		for(Set<LineEntry> rules : bucket.values()){
			Iterator<LineEntry> itr = rules.iterator();
			if(rules.size() == 1)
				compressedRules.add(itr.next());
			else{
				//collect conditions
				Set<String> conditions = rules.stream()
					.map(le -> le.condition)
					.collect(Collectors.toSet());
				String commonCondition = mergeConditions(conditions);

				LineEntry onlyEntry = itr.next();
				itr.remove();
				List<String> froms = rules.stream()
					.flatMap(le -> le.from.stream())
					.collect(Collectors.toList());
				onlyEntry.from.addAll(froms);
				onlyEntry.condition = commonCondition;
				compressedRules.add(onlyEntry);
			}
		}
		return compressedRules;
	}

	private String mergeConditions(Set<String> conditions){
		//TODO cope with suffix/affix
		String lcs = longestCommonSuffix(conditions, null);
		if(lcs.length() + 1 != conditions.iterator().next().length())
			return null;

		return conditions.stream()
			//TODO cope with suffix/affix
			.map(cond -> String.valueOf(cond.charAt(0)))
			.sorted(comparator)
			.collect(Collectors.joining(StringUtils.EMPTY, GROUP_START, GROUP_END + lcs));
	}

	private List<LineEntry> reduceNotConditions(List<LineEntry> compressedRules){
		//stage 0: original list
		//SFX <2 0 aso/M0 [nr]
		//SFX <2 e aso/M0 e
		//SFX <2 0 sa/F0 [gnñortu]a
		//SFX <2 ía iasa/F0 ía
		//SFX <2 0 sa/F0 [aiou]ƚa
		//SFX <2 èƚa eƚasa/F0 èƚa
		//SFX <2 òƚa oƚasa/F0 òƚa
		//SFX <2 o aso/M0 [giñptv]o
		//SFX <2 o aso/M0 [aiou]ƚo
		//SFX <2 èƚo eƚaso/M0 èƚo
		//SFX <2 òjo ojaso/M0 òjo
		//SFX <2 òɉo oɉaso/M0 òɉo

		//stage 1: collect the rules with a fixed part in the conditions (ex. [aiou]la > 'la')
		//SFX <2 0 sa/F0 [aiou]ƚa
		//SFX <2 o aso/M0 [aiou]ƚo
		//SFX <2 0 sa/F0 [gnñortu]a
		//SFX <2 o aso/M0 [giñptv]o
		Map<LineEntry, List<LineEntry>> fixedPartsBucket = new HashMap<>();
		for(LineEntry elem : compressedRules){
			int closedBraketIndex = elem.condition.indexOf(']') + 1;
			if(closedBraketIndex > 0 && closedBraketIndex < elem.condition.length())
				fixedPartsBucket.put(elem, new ArrayList<>());
		}

		//stage 2: check for collisions between fixed parts
		Map<String, List<LineEntry>> conditionBucket = new HashMap<>();
		for(LineEntry key : fixedPartsBucket.keySet())
			conditionBucket.computeIfAbsent(key.condition, k -> new ArrayList<>())
				.add(key);
		Map<String, List<LineEntry>> collisions = new HashMap<>();
		for(Map.Entry<String, List<LineEntry>> e : conditionBucket.entrySet()){
			String fixedCondition = e.getKey().substring(e.getKey().indexOf(']') + 1);
			if(collisions.containsKey(fixedCondition))
				throw new IllegalArgumentException("Collision occurred between fixed parts of some conditions: " + e.getValue()
					+ " and " + collisions.get(fixedCondition));

			collisions.put(fixedCondition, e.getValue());
		}

		//stage 3: for each fixed-part rule, bucket all the non-fixed parts of the remaining rules that ends with a fixed part,
		//		bucket also from the bucketed bucket
		//SFX <2 0 sa/F0 [aiou]ƚa:		[SFX <2 èƚa eƚasa/F0 èƚa, SFX <2 òƚa oƚasa/F0 òƚa]
		//SFX <2 o aso/M0 [aiou]ƚo:	[SFX <2 èƚo eƚaso/M0 èƚo]
		//SFX <2 0 sa/F0 [gnñortu]a:	[SFX <2 èƚa eƚasa/F0 èƚa, SFX <2 òƚa oƚasa/F0 òƚa, SFX <2 ía iasa/F0 ía, SFX <2 0 sa/F0 [aiou]ƚa]
		//SFX <2 o aso/M0 [giñptv]o:	[SFX <2 òjo ojaso/M0 òjo, SFX <2 òɉo oɉaso/M0 òɉo, SFX <2 èƚo eƚaso/M0 èƚo, SFX <2 o aso/M0 [aiou]ƚo]
		for(Map.Entry<LineEntry, List<LineEntry>> entry : fixedPartsBucket.entrySet()){
			LineEntry key = entry.getKey();
			List<LineEntry> value = entry.getValue();

			String fixedCondition = key.condition.substring(key.condition.indexOf(']') + 1);
			for(LineEntry compressedRule : compressedRules)
				if(key != compressedRule && compressedRule.condition.endsWith(fixedCondition))
					value.add(compressedRule);
		}

		//stage 4: collect the letters prior to the common part and use them as the not part
		for(Map.Entry<LineEntry, List<LineEntry>> entry : fixedPartsBucket.entrySet()){
			LineEntry key = entry.getKey();
			List<LineEntry> value = entry.getValue();

			int keyConditionLength = key.condition.length();
			int fixedPartConditionLength = keyConditionLength - key.condition.indexOf(']');
			int variablePartConditionLength = keyConditionLength - fixedPartConditionLength;
			Set<String> notSet = value.stream()
				.map(rule -> String.valueOf(rule.condition.charAt(rule.condition.length() - fixedPartConditionLength)))
				.collect(Collectors.toSet());
			//NOTE: the `notSet` should not be contained into the group of key.condition!! if not, raise exception!
			String variablePart = key.condition.substring(1, variablePartConditionLength);
			boolean overlaps = StringUtils.containsAny(variablePart, notSet.toArray(new String[notSet.size()]));
			if(overlaps)
				throw new IllegalArgumentException("Collision occurred between variable parts of a condition and not-set: " + key.toString()
					+ ", not-set is " + String.join(StringUtils.EMPTY, notSet));

			//if everything's ok, continue and substitute the variable part with the negated set
			List<String> notPart = new ArrayList<>(notSet);
			notPart.sort(comparator);
			StringJoiner sj = new StringJoiner(StringUtils.EMPTY);
			sj.add(NOT_GROUP_START);
			notPart.forEach(sj::add);
			sj.add(GROUP_END)
				.add(key.condition.substring(variablePartConditionLength + 1));
			key.condition = sj.toString();
		}

		//stage 5: expand missing rules
		Map<LineEntry, List<LineEntry>> missingRules = new HashMap<>();
		for(Map.Entry<LineEntry, List<LineEntry>> entry : fixedPartsBucket.entrySet()){
			LineEntry key = entry.getKey();
			List<LineEntry> value = entry.getValue();

			int fixedPartConditionLength = key.condition.length() - key.condition.indexOf(']');
			for(LineEntry rule : value){
				int fixedPartIndex = rule.condition.length() - fixedPartConditionLength;
				if(rule.condition.indexOf(']') < 0 && fixedPartIndex > 0){
					//check if fixed part is not already contained into the bucket
					String fixedPart = rule.condition.substring(fixedPartIndex);
					if(!collisions.containsKey(fixedPart))
						missingRules.computeIfAbsent(key, k -> new ArrayList<>())
							.add(rule);
				}
			}
		}
		List<LineEntry> additionalRules = new ArrayList<>();
		//collect all letters from key.condition.length - 2 for each group key.condition.length - fixedPartConditionLength to key.condition.length
		for(Map.Entry<LineEntry, List<LineEntry>> rule : missingRules.entrySet()){
			LineEntry key = rule.getKey();
			List<LineEntry> value = rule.getValue();

			Map<String, LineEntry> rules = new HashMap<>();
			int fixedPartConditionLength = key.condition.length() - key.condition.indexOf(']');
			Map<String, List<LineEntry>> expansionBucket = new HashMap<>();
			for(LineEntry v : value){
				int index = v.condition.length() - fixedPartConditionLength;
				if(index < 0)
					throw new IllegalArgumentException("Cannot create additional rules from " + key);

				while(index > 0){
					String subkey = v.condition.substring(index);
					rules.put(subkey, key);
					expansionBucket.computeIfAbsent(subkey, k -> new ArrayList<>())
						.add(v);

					index --;
				}
			}
			for(Map.Entry<String, List<LineEntry>> expansionPoint : expansionBucket.entrySet()){
				String k = expansionPoint.getKey();
				List<LineEntry> v = expansionPoint.getValue();

				int keyConditionLength = k.length();
				Set<String> notSet = v.stream()
					.map(r -> String.valueOf(r.condition.charAt(r.condition.length() - keyConditionLength - 1)))
					.collect(Collectors.toSet());

				LineEntry r = rules.get(k);
				List<String> notPart = new ArrayList<>(notSet);
				notPart.sort(comparator);
				StringJoiner sj = new StringJoiner(StringUtils.EMPTY);
				sj.add(NOT_GROUP_START);
				notPart.forEach(sj::add);
				sj.add(GROUP_END)
					.add(k);
				additionalRules.add(new LineEntry(r.removal, r.addition, sj.toString()));
			}
		}

	return additionalRules;
	}

	private List<LineEntry> prepareRules(boolean keepLongestCommonAffix, List<LineEntry> compressedRules){
		if(keepLongestCommonAffix)
			//TODO cope with suffix/prefix
			compressedRules.forEach(entry -> entry.condition = longestCommonSuffix(entry.from, RegExpSequencer.splitSequence(entry.condition)));
		compressedRules.sort(lineEntryComparator);
		return compressedRules;
	}

	private List<String> composeAffixRules(RuleEntry originalRuleEntry, List<LineEntry> entries){
		AffixEntry.Type type = (originalRuleEntry.isSuffix()? AffixEntry.Type.SUFFIX: AffixEntry.Type.PREFIX);

		int size = entries.size();
		String flag = originalRuleEntry.getEntries().get(0).getFlag();
		List<String> rules = new ArrayList<>(size);
		for(LineEntry entry : entries)
			rules.add(composeLine(type, flag, entry));
		return rules;
	}

	//FIXME optimize!
	private String longestCommonSuffix(Set<String> texts, String[] baseCondition){
		String firstText = null;
		int lastCommonLetter = (baseCondition != null? baseCondition.length: 0);
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
				if(index < 0 || text.charAt(index) != commonLetter){
					text = text.substring(index + 1);
					if(baseCondition != null)
						text = text.substring(0, text.length() - baseCondition.length) + String.join(StringUtils.EMPTY, baseCondition);
					return text;
				}
			}

			lastCommonLetter ++;
		}
		if(baseCondition != null)
			firstText = firstText.substring(0, firstText.length() - baseCondition.length) + String.join(StringUtils.EMPTY, baseCondition);
		return firstText;
	}




	private Set<Character> calculateIntersection(List<Set<Character>> letters){
		Iterator<Set<Character>> itr = letters.iterator();
		Set<Character> intersection = new HashSet<>(itr.next());
		while(itr.hasNext())
			intersection.retainAll(itr.next());
		return intersection;
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
