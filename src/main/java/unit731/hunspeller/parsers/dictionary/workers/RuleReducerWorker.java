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
import unit731.hunspeller.collections.radixtree.RadixTree;
import unit731.hunspeller.collections.radixtree.StringRadixTree;
import unit731.hunspeller.collections.radixtree.sequencers.RegExpSequencer;
import unit731.hunspeller.collections.radixtree.utils.RadixTreeTraverser;
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


	private class LineEntry implements Serializable{

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
			.thenComparing(Comparator.comparing(entry -> StringUtils.reverse(entry.condition)))
			.thenComparing(Comparator.comparingInt(entry -> entry.removal.length()))
			.thenComparing(Comparator.comparing(entry -> entry.removal))
			.thenComparing(Comparator.comparingInt(entry -> entry.addition.length()))
			.thenComparing(Comparator.comparing(entry -> entry.addition));

//String flag = "%1";
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
				String lcs = longestCommonSuffix(entry.from);
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
							longestCommonSuffix(conditions.getValue()));
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

			//constuct the suffix tree
			StringRadixTree<LineEntry> tree = StringRadixTree.createTree();
			for(LineEntry entry : equivalenceTable.values()){
				LineEntry oldEntry = tree.put(StringUtils.reverse(entry.condition), entry);

				if(oldEntry != null){
					LOGGER.info(Backbone.MARKER_APPLICATION, "case to be managed: duplicated entry");
throw new RuntimeException("case to be managed: duplicated entry");
					//TODO
				}
			}
			//all the conditions that are not a leaf must be augmented
			RadixTreeTraverser<String, LineEntry> traverser = (wholeKey, node, parent) -> {
				LineEntry nodeEntry = node.getValue();
				if(nodeEntry != null && !node.isEmpty()){
					//retrieve all the conditions spanned by the current condition
					List<LineEntry> list = tree.getValues(wholeKey, RadixTree.PrefixType.PREFIXED_BY);
					//sort by shortes condition
					list.sort(shortestConditionComparator);

					//extract prior to condition characters
					String feasibleGroup = extractGroup(nodeEntry.from, nodeEntry.condition.length());
System.out.println("feasible group: " + feasibleGroup);

					//extract not part
					int size = list.size();
					for(int i = 0; i < size; i ++){
						LineEntry base = list.get(i);
						Set<String> notSet = new HashSet<>();
						for(int j = i + 1; j < size; j ++){
							LineEntry collision = list.get(j);
							if(collision.condition.endsWith(base.condition)){
								int index = collision.condition.length() - base.condition.length() - 1;
								if(index == -1)
									//same length condition
									break;

								notSet.add(String.valueOf(collision.condition.charAt(index)));

								while(index > 0){
									//add additional rules
									String conditionSuffix = collision.condition.substring(index);
									Set<String> set = list.stream()
										.map(entry -> entry.condition)
										.filter(condition -> condition.endsWith(conditionSuffix))
										.collect(Collectors.toSet());
									String conditionSuffixGroup = extractGroup(set, conditionSuffix.length());
									if(conditionSuffixGroup != null){
										String condition = NOT_GROUP_START + conditionSuffixGroup + GROUP_END + conditionSuffix;
										LineEntry newEntry = new LineEntry(base.removal, base.addition, condition);
										collectIntoEquivalenceClasses(newEntry, equivalenceTable);
									}

									index --;
								}
							}
						}
						if(!notSet.isEmpty()){
							List<String> notPart = new ArrayList<>(notSet);
							notPart.sort(comparator);
							String notGroup = StringUtils.join(notPart, null);
System.out.println("not group: " + notGroup);
//							if(StringUtils.containsAny(notGroup, feasibleGroup))
//								throw new IllegalArgumentException("Invalid not group [^" + notGroup + "] into [" + feasibleGroup + "]");

							base.condition = NOT_GROUP_START + notGroup + GROUP_END + base.condition;

							//TODO check feasibility of solution
						}
					}
//System.out.println("augment the condition for " + node.getValue() + " from set\r\n\t" + StringUtils.join(list, "\r\n\t"));
				}
			};
			tree.traverseBFS(traverser);

//System.out.println("entries:");
//for(Map.Entry<String, LineEntry> e : equivalenceTable.entrySet())
//	System.out.println(e.getKey() + ": " + StringUtils.join(e.getValue(), ","));
			AffixEntry.Type type = (originalRuleEntry.isSuffix()? AffixEntry.Type.SUFFIX: AffixEntry.Type.PREFIX);
			try{
//				reduceEquivalenceClasses(type, entriesTable);
//System.out.println("cleaned entries:");
//for(Map.Entry<String, List<LineEntry>> e : entriesTable.entrySet())
//	System.out.println(e.getKey() + ": " + StringUtils.join(e.getValue(), ","));

				List<LineEntry> entries = equivalenceTable.values().stream()
					.collect(Collectors.toList());

				//merge similar rules
				Map<String, Set<LineEntry>> mergeBucket = new HashMap<>();
				Iterator<LineEntry> itr2 = entries.iterator();
				while(itr2.hasNext()){
					LineEntry entry = itr2.next();
					if(!entry.condition.contains("]")){
						String key = entry.removal + TAB + entry.addition + TAB + entry.condition.length();
						mergeBucket.computeIfAbsent(key, k -> new HashSet<>())
							.add(entry);

						itr2.remove();
					}
				}
				Iterator<Set<LineEntry>> itr3 = mergeBucket.values()
					.iterator();
				while(itr3.hasNext()){
					Set<LineEntry> set = itr3.next();
					if(set.size() == 1)
						entries.add(set.iterator().next());
					else{
						Map<String, Set<LineEntry>> similarConditionBucket = new HashMap<>();
						for(LineEntry e : set){
							String key = e.condition.substring(1);
							similarConditionBucket.computeIfAbsent(key, k -> new HashSet<>())
								.add(e);
						}
						//collect initial letters into a group
						for(Set<LineEntry> v : similarConditionBucket.values()){
							LineEntry firstEntry = v.iterator().next();
							Set<String> words = v.stream()
								.map(vv -> vv.condition)
								.collect(Collectors.toSet());
							String commonGroup = extractGroup(words, firstEntry.condition.length() - 1);
							String condition = GROUP_START + commonGroup + GROUP_END + firstEntry.condition.substring(1);
							LineEntry newEntry = new LineEntry(firstEntry.removal, firstEntry.addition, condition);
							newEntry.from.addAll(v.stream().flatMap(vv -> vv.from.stream()).collect(Collectors.toSet()));
							entries.add(newEntry);
						}
					}
				}

				List<String> rules = convertEntriesToRules(originalRuleEntry, keepLongestCommonAffix, entries);

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

	private String extractGroup(Collection<String> words, int indexFromLast){
		Set<String> group = new HashSet<>();
		for(String word : words){
			int index = word.length() - indexFromLast - 1;
			if(index < 0)
				throw new IllegalArgumentException("Cannot extract group for " + this + " because of the word " + word + " that is too short");

			group.add(String.valueOf(word.charAt(index)));
		}
		String result = null;
		if(!group.isEmpty()){
			List<String> uniqueGroup = new ArrayList<>(group);
			uniqueGroup.sort(comparator);
			result = StringUtils.join(uniqueGroup, null);
		}
		return result;
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
			List<LineEntry> entries){
		entries.sort(lineEntryComparator);

//		entries = compressRules(entries, keepLongestCommonAffix);
		entries = prepareRules(keepLongestCommonAffix, entries);

		return composeAffixRules(originalRuleEntry, entries);
	}

	private List<LineEntry> compressRules(List<LineEntry> entries, boolean keepLongestCommonAffix){
		Map<String, Set<LineEntry>> bucket = bucketByRemovalAndAddingAndCondition(entries);

		List<LineEntry> compressedRules = compressBucketedRules(bucket);

		//FIXME
//		if(!keepLongestCommonAffix){
//			List<LineEntry> additionalRules = reduceNotConditions(compressedRules);
//			compressedRules.addAll(additionalRules);
//		}
//		else{
//			//TODO
//			//check for collisions between conditions
//		}

		//extract compressed rules
		return prepareRules(keepLongestCommonAffix, compressedRules);
	}

	private Map<String, Set<LineEntry>> bucketByRemovalAndAddingAndCondition(Collection<LineEntry> entries){
		Map<String, Set<LineEntry>> bucket = new HashMap<>();
		for(LineEntry entry : entries){
			String key = entry.removal + TAB + entry.addition + TAB + entry.condition.length();
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
		String lcs = longestCommonSuffix(conditions);
		if(lcs.length() + 1 != conditions.iterator().next().length())
			return null;

		return conditions.stream()
			//TODO cope with suffix/affix
			.map(cond -> String.valueOf(cond.charAt(0)))
			.sorted(comparator)
			.collect(Collectors.joining(StringUtils.EMPTY, GROUP_START, GROUP_END + lcs));
	}

	private List<LineEntry> prepareRules(boolean keepLongestCommonAffix, List<LineEntry> compressedRules){
		if(keepLongestCommonAffix)
			//TODO cope with suffix/prefix
			compressedRules.forEach(entry -> {
				String lcs = longestCommonSuffix(entry.from);
				if(lcs == null)
					lcs = entry.condition;
				else if(entry.condition.contains("]")){
					int tailCharactersToExclude = RegExpSequencer.splitSequence(entry.condition).length;
					if(tailCharactersToExclude <= lcs.length())
						lcs = lcs.substring(0, lcs.length() - tailCharactersToExclude) + entry.condition;
					else
						lcs = entry.condition;
				}
				//NOTE: could it ever be that lcs.length < entry.condition.length?
				if(lcs.length() < entry.condition.length())
					throw new IllegalArgumentException("cannot be?");

				entry.condition = lcs;
			});
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

	//TODO longest common prefix
	private String longestCommonSuffix(Collection<String> texts){
		String lcs = null;
		if(!texts.isEmpty()){
			Iterator<String> itr = texts.iterator();
			lcs = itr.next();
			while(!lcs.isEmpty() && itr.hasNext())
				lcs = longestCommonSuffix(lcs, itr.next());
		}
		return lcs;
	}

	/**
	 * Returns the longest string {@code suffix} such that {@code a.toString().endsWith(suffix) &&
	 * b.toString().endsWith(suffix)}, taking care not to split surrogate pairs. If {@code a} and
	 * {@code b} have no common suffix, returns the empty string.
	 */
	private String longestCommonSuffix(String a, String b){
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
	 * True when a valid surrogate pair starts at the given {@code index} in the given {@code string}.
	 * Out-of-range indexes return false.
	 */
	private boolean validSurrogatePairAt(CharSequence string, int index){
		return (index >= 0 && index <= (string.length() - 2)
			&& Character.isHighSurrogate(string.charAt(index))
			&& Character.isLowSurrogate(string.charAt(index + 1)));
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
