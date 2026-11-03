/**
 * Copyright (c) 2019-2022 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.workers.dictionary;

import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.BloomFilterInterface;
import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.BloomFilterParameters;
import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.ScalableInMemoryBloomFilter;
import io.github.mtrevisan.hunlinter.gui.ProgressCallback;
import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.Duplicate;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.parsers.exceptions.WriterException;
import io.github.mtrevisan.hunlinter.parsers.vos.AffixEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.services.ParserHelper;
import io.github.mtrevisan.hunlinter.workers.WorkerManager;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDataParser;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDictionary;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;


public class DuplicatesWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(DuplicatesWorker.class);


	public static final String WORKER_NAME = "Duplicates extraction";

	/**
	 * If `true`, write to file ONLY the entries (lemmas) that can be removed without losing any inflection (same word, same
	 * morphological fields), and nothing else; if `false`, write all the duplicates with the removal analysis.
	 */
	private static final boolean WRITE_ONLY_SAFELY_REMOVABLE_ENTRIES = true;

	/** Text of the analysis of an entry that can be safely removed (see {@link RemovalAnalyzer#analyzeEntry(int, String, Collection)}). */
	private static final String ENTRY_SAFELY_REMOVABLE_MARKER = "is safely removable (all its inflections";


	private final DictionaryParser dicParser;
	private final WordGenerator wordGenerator;

	private final BloomFilterParameters dictionaryBaseData;


	public DuplicatesWorker(final ParserManager parserManager, final Consumer<Exception> onCancelled, final File outputFile){
		this(parserManager.getLanguage(), parserManager.getDicParser(), parserManager.getWordGenerator(), onCancelled, outputFile);
	}

	public DuplicatesWorker(final String language, final DictionaryParser dicParser, final WordGenerator wordGenerator,
			final Consumer<Exception> onCancelled, final File outputFile){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withParallelProcessing()
			.withDataCancelledCallback(onCancelled)
			.withCancelOnException();

		Objects.requireNonNull(language, "Language cannot be null");
		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");
		Objects.requireNonNull(outputFile, "Output file cannot be null");


		this.dicParser = dicParser;
		this.wordGenerator = wordGenerator;

		dictionaryBaseData = BaseBuilder.getDictionaryBaseData(language);

		final Function<Void, Set<String>> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/3)");

			return collectDuplicates();
		};
		final Function<Set<String>, Collection<List<Duplicate>>> step2 = this::extractDuplicates;
		final Function<Collection<List<Duplicate>>, File> step3 = duplicates -> {
			writeDuplicates(outputFile, duplicates);

			finalizeProcessing("Duplicates extracted successfully");

			return outputFile;
		};
		final Function<File, Void> step4 = WorkerManager.openFileStep(LOGGER);
		setProcessor(step1.andThen(step2).andThen(step3).andThen(step4));
	}

	private Set<String> collectDuplicates(){
		final File dicFile = dicParser.getDicFile();
		final Charset charset = dicParser.getCharset();

		final BloomFilterInterface<String> bloomFilter = new ScalableInMemoryBloomFilter<>(charset, dictionaryBaseData);
		//exact set of the texts seen more than once, NOT capped: every lemma of the dictionary has to be checked, not only the first ones
		final Set<String> duplicateTexts = ConcurrentHashMap.newKeySet();

		//NOTE: the bloom filter (thread-safe bit array) and the set are shared among the threads
		final BiConsumer<Integer, String> fun = (lineIndex, line) -> {
			try{
				final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
				final List<Inflection> inflections = wordGenerator.applyAffixRulesWithoutOutputConversion(dicEntry);

				for(int i = 0, length = inflections.size(); i < length; i ++){
					final String str = inflections.get(i).toStringWithAllMorphologicalFields();
					if(!bloomFilter.add(str))
						duplicateTexts.add(str);
				}
			}
			catch(final LinterException e){
				LOGGER.info(ParserManager.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), fileLine(lineIndex), line);
			}
		};
		final ProgressCallback progressCallback = lineIndex -> {
			setWorkerProgress(lineIndex);

			sleepOnPause();
		};
		ParserHelper.forEachDictionaryLineParallel(dicFile, charset, fun, progressCallback);

		bloomFilter.close();
		final int totalInflections = bloomFilter.getAddedElements();
		final double falsePositiveProbability = bloomFilter.getTrueFalsePositiveProbability();
		bloomFilter.clear();

		final int falsePositiveCount = (int)Math.ceil(totalInflections * falsePositiveProbability);
		LOGGER.info(ParserManager.MARKER_APPLICATION, "Total inflections: {}",
			DictionaryParser.COUNTER_FORMATTER.format(totalInflections));
		LOGGER.info(ParserManager.MARKER_APPLICATION, "False positive probability is {} (overall duplicates ≲ {})",
			DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability), falsePositiveCount);

		return duplicateTexts;
	}

	private Collection<List<Duplicate>> extractDuplicates(final Set<String> duplicateTexts){
		final Map<String, List<Duplicate>> result = new ConcurrentHashMap<>(0);

		if(!duplicateTexts.isEmpty()){
			resetProcessing("Extracting duplicates (step 2/3)");

			final Charset charset = dicParser.getCharset();
			final File dicFile = dicParser.getDicFile();
			final BiConsumer<Integer, String> fun = (lineIndex, line) -> {
				try{
					final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
					final List<Inflection> inflections = wordGenerator.applyAffixRulesWithoutOutputConversion(dicEntry);

					if(!inflections.isEmpty()){
						final String word = inflections.get(WordGenerator.BASE_INFLECTION_INDEX).getWord();
						for(int i = 0, length = inflections.size(); i < length; i ++){
							final Inflection inflection = inflections.get(i);
							final String text = inflection.toStringWithAllMorphologicalFields();
							if(duplicateTexts.contains(text)){
								final Duplicate duplicate = new Duplicate(inflection, word, dicEntry.getContinuationFlags(), lineIndex);
								result.computeIfAbsent(text, k -> Collections.synchronizedList(new ArrayList<>(2)))
									.add(duplicate);
							}
						}
					}
				}
				catch(final LinterException e){
					LOGGER.info(ParserManager.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), fileLine(lineIndex), line);
				}
			};
			final ProgressCallback progressCallback = lineIndex -> {
				setWorkerProgress(lineIndex);

				sleepOnPause();
			};
			ParserHelper.forEachDictionaryLineParallel(dicFile, charset, fun, progressCallback);

			//a text seen only once is a bloom filter false positive, not a duplicate
			result.values().removeIf(group -> group.size() < 2);

			LOGGER.info(ParserManager.MARKER_APPLICATION, "Total duplicates: {}", DictionaryParser.COUNTER_FORMATTER.format(result.size()));

			duplicateTexts.clear();
		}
		else
			LOGGER.info(ParserManager.MARKER_APPLICATION, "No duplicates found, skip remaining steps");

		return result.values();
	}

	/**
	 * Write ALL the real duplicates (groups of at least two entries producing the same inflection), one group per line, ordered by
	 * the first line of the dictionary involved; each group is annotated with the removal hints.
	 * The output file is always created (empty if there are no duplicates).
	 */
	private void writeDuplicates(final File duplicatesFile, final Collection<List<Duplicate>> duplicates){
		LOGGER.info(ParserManager.MARKER_APPLICATION, "Write results to file (step 3/3)");

		//sort inside each group, then sort the groups, so that the output is deterministic
		//(the line index is the tie-breaker because the entries are collected by many threads, in no particular order)
		final Comparator<Duplicate> byAppliedRules = Comparator.<Duplicate>comparingInt(o -> o.getInflection().getAppliedRules().length)
			.thenComparingInt(Duplicate::getLineIndex);
		final List<List<Duplicate>> groups = new ArrayList<>(duplicates.size());
		for(final List<Duplicate> group : duplicates){
			final List<Duplicate> sortedGroup = new ArrayList<>(group);
			sortedGroup.sort(byAppliedRules);
			groups.add(sortedGroup);
		}
		groups.sort(Comparator.comparingInt(DuplicatesWorker::getFirstLineIndex));

		//load only the lines of the entries involved, they are needed to really generate their inflections
		final Set<Integer> involvedLineIndexes = new HashSet<>(0);
		for(final List<Duplicate> group : groups)
			for(final Duplicate duplicate : group)
				involvedLineIndexes.add(duplicate.getLineIndex());
		final RemovalAnalyzer analyzer = new RemovalAnalyzer(
			ParserHelper.loadDictionaryLines(dicParser.getDicFile(), dicParser.getCharset(), involvedLineIndexes));

		final int totalSize = groups.size();
		final Set<String> processedGroups = new HashSet<>(totalSize);
		int writtenSoFar = 0;
		int writtenGroups = 0;
		try(final BufferedWriter writer = Files.newBufferedWriter(duplicatesFile.toPath(), dicParser.getCharset())){
			final StringBuilder sb = new StringBuilder();
			for(final List<Duplicate> group : groups){
				//skip groups made of the very same lines AND rules (they would give the very same advices)
				if(processedGroups.add(getGroupKey(group, sb))){
					if(describeGroup(group, analyzer, sb)){
						writer.write(sb.toString());
						writer.newLine();
						writtenGroups ++;
					}
				}

				setWorkerProgress(++ writtenSoFar, totalSize);

				sleepOnPause();
			}
		}
		catch(final IOException ioe){
			throw new WriterException(ioe);
		}

		LOGGER.info(ParserManager.MARKER_APPLICATION, "File written: {} ({} duplicates)", duplicatesFile.getAbsolutePath(),
			writtenGroups);
	}

	private static int getFirstLineIndex(final List<Duplicate> group){
		int min = Integer.MAX_VALUE;
		for(int i = 0, length = group.size(); i < length; i ++)
			min = Math.min(min, group.get(i).getLineIndex());
		return min;
	}

	private static String getGroupKey(final List<Duplicate> group, final StringBuilder sb){
		sb.setLength(0);
		for(int i = 0, length = group.size(); i < length; i ++){
			if(i > 0)
				sb.append(", ");
			final Duplicate duplicate = group.get(i);
			sb.append(duplicate.getLineIndex())
				.append(':')
				.append(duplicate.getInflection().getRulesSequence());
		}
		return sb.toString();
	}

	/**
	 * Exact removal analysis: instead of guessing from the flags, the inflections of the entries involved are really generated
	 * and compared, so that a removal is reported as safe only if NO inflection (word + morphological fields) is lost.
	 */
	private final class RemovalAnalyzer{

		private static final int MAX_LOST_SAMPLES = 3;

		private final Map<Integer, String> lines;
		private final Map<Integer, Set<String>> inflectionsByLine = new HashMap<>(0);
		private final Map<String, Set<String>> inflectionsByLineWithoutFlag = new HashMap<>(0);


		private RemovalAnalyzer(final Map<Integer, String> lines){
			this.lines = lines;
		}

		/** @return	The inflections of the entry, or `null` if they cannot be generated. */
		private Set<String> inflections(final int lineIndex){
			if(inflectionsByLine.containsKey(lineIndex))
				return inflectionsByLine.get(lineIndex);

			final Set<String> inflections = generate(lineIndex, null);
			inflectionsByLine.put(lineIndex, inflections);
			return inflections;
		}

		/** @return	The inflections of the entry once the flag has been removed from it, or `null` if they cannot be generated. */
		private Set<String> inflections(final int lineIndex, final String flagToRemove){
			final String key = lineIndex + "|" + flagToRemove;
			if(inflectionsByLineWithoutFlag.containsKey(key))
				return inflectionsByLineWithoutFlag.get(key);

			final Set<String> inflections = generate(lineIndex, flagToRemove);
			inflectionsByLineWithoutFlag.put(key, inflections);
			return inflections;
		}

		private Set<String> generate(final int lineIndex, final String flagToRemove){
			final String line = lines.get(lineIndex);
			if(line == null)
				return null;

			try{
				final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
				if(flagToRemove != null)
					dicEntry.removeContinuationFlag(flagToRemove);
				final List<Inflection> inflections = wordGenerator.applyAffixRulesWithoutOutputConversion(dicEntry);

				final Set<String> keys = new HashSet<>(inflections.size() * 2);
				for(int i = 0, length = inflections.size(); i < length; i ++)
					keys.add(inflections.get(i).toStringWithAllMorphologicalFields());
				return keys;
			}
			catch(final RuntimeException ignored){
				return null;
			}
		}

		/** @return	The elements of `inflections` that are NOT in `covering`, as a readable string (empty if there are none). */
		private String lost(final Set<String> inflections, final Collection<String> covering){
			final List<String> lost = new ArrayList<>(0);
			int count = 0;
			for(final String inflection : inflections)
				if(!covering.contains(inflection)){
					count ++;
					if(lost.size() < MAX_LOST_SAMPLES)
						lost.add(inflection);
				}
			if(count == 0)
				return StringUtils.EMPTY;

			Collections.sort(lost);
			return count + " inflection" + (count > 1? "s": StringUtils.EMPTY);
		}

		/** Is the flag removable from the entry without losing any inflection? */
		private String analyzeFlag(final int lineIndex, final String flag){
			final Set<String> full = inflections(lineIndex);
			final Set<String> reduced = inflections(lineIndex, flag);
			final String where = "flag " + flag + " of line " + fileLine(lineIndex);
			if(full == null || reduced == null)
				return where + " cannot be verified";

			final String lost = lost(full, reduced);
			return (lost.isEmpty()
				? where + " is safely removable (no inflection lost)"
				: where + " is NOT removable, it would lose " + lost);
		}

		/** Is the whole entry removable, being all its inflections generated by the other entries? */
		private String analyzeEntry(final int lineIndex, final String lemma, final Collection<Integer> otherLineIndexes,
				final Map<Integer, String> lemmas){
			final Set<String> full = inflections(lineIndex);
			final String where = "entry " + lemma + " (line " + fileLine(lineIndex) + ")";
			if(full == null || full.isEmpty())
				//nothing to compare with (a vacuous truth would wrongly say it's removable)
				return where + " cannot be verified";

			final Set<String> covering = new HashSet<>(0);
			final StringJoiner coveringEntries = new StringJoiner(", ");
			for(final Integer otherLineIndex : otherLineIndexes){
				final Set<String> other = inflections(otherLineIndex);
				if(other == null)
					return where + " cannot be verified";

				covering.addAll(other);

				//the entries that really contribute to cover this one
				int covered = 0;
				for(final String inflection : full)
					if(other.contains(inflection))
						covered ++;
				if(covered > 0)
					coveringEntries.add(lemmas.get(otherLineIndex) + " (line " + fileLine(otherLineIndex) + ", " + covered + "/"
						+ full.size() + ")");
			}

			final String lost = lost(full, covering);
			return (lost.isEmpty()
				? where + " " + ENTRY_SAFELY_REMOVABLE_MARKER + " are generated by the other entries: " + coveringEntries + ")"
				: where + " is NOT removable, it would lose " + lost);
		}

	}

	/**
	 * Build the line to be written for the given group (sorted by applied rules count) into `sb`.
	 *
	 * @return	Whether the line has to be written (always, unless {@link #WRITE_ONLY_SAFELY_REMOVABLE_ENTRIES} is set and the group
	 * 	has no entry that can be safely removed).
	 */
	private static boolean describeGroup(final List<Duplicate> group, final RemovalAnalyzer analyzer, final StringBuilder sb){
		final Inflection prod = group.getFirst().getInflection();
		sb.setLength(0);
		sb.append(prod.getWord());
		//all the morphological fields, since they are what makes two inflections true duplicates
		final List<String> morphologicalFields = prod.getMorphologicalFieldsAsList();
		if(morphologicalFields != null && !morphologicalFields.isEmpty())
			sb.append('(')
				.append(String.join(" ", morphologicalFields))
				.append(')');
		sb.append(": ");

		final StringJoiner details = new StringJoiner(", ");
		//removal units: an entry (line) with the dictionary flag that generates the duplicate
		final Map<String, Duplicate> units = new LinkedHashMap<>(0);
		final Set<Integer> lineIndexes = new LinkedHashSet<>(0);
		final Map<Integer, String> lemmas = new HashMap<>(0);
		for(final Duplicate duplicate : group){
			final Inflection inflection = duplicate.getInflection();
			details.add(duplicate.getWord() + " (" + fileLine(duplicate.getLineIndex())
				+ (inflection.hasInflectionRules()? " via " + inflection.getRulesSequence(): StringUtils.EMPTY) + ")");

			units.putIfAbsent(duplicate.getLineIndex() + "|" + getRootFlag(inflection), duplicate);
			lineIndexes.add(duplicate.getLineIndex());
			lemmas.putIfAbsent(duplicate.getLineIndex(), duplicate.getWord());
		}

		final Set<String> advices = new LinkedHashSet<>(0);
		final List<String> removableEntries = new ArrayList<>(0);
		if(units.size() == 1){
			//all the duplicates come from the very same flag of the very same entry: some rules of the flag overlap
			final Duplicate unit = units.values().iterator().next();
			final Set<String> ruleChains = new LinkedHashSet<>(0);
			for(final Duplicate duplicate : group)
				ruleChains.add(getRuleChain(duplicate.getInflection()));
			advices.add("redundant rules inside flag " + getRootFlag(unit.getInflection()) + " of line " + fileLine(unit.getLineIndex())
				+ ": " + String.join(" | ", ruleChains));
		}
		else{
			for(final Duplicate unit : units.values()){
				final String rootFlag = getRootFlag(unit.getInflection());
				if(!rootFlag.isEmpty())
					advices.add(analyzer.analyzeFlag(unit.getLineIndex(), rootFlag));
			}

			if(lineIndexes.size() > 1){
				//NOTE: two entries could cover each other (ie. identical entries): removing both would lose everything, so the
				//entries are examined one at a time (the last first) against the ones that would still remain, and an entry reported
				//as removable is considered removed for the following ones
				final List<Integer> remaining = new ArrayList<>(lineIndexes);
				final List<Integer> examinationOrder = new ArrayList<>(lineIndexes);
				examinationOrder.sort(Comparator.reverseOrder());
				for(final Integer lineIndex : examinationOrder){
					final List<Integer> others = new ArrayList<>(remaining);
					others.remove(lineIndex);
					final String advice = analyzer.analyzeEntry(lineIndex, lemmas.get(lineIndex), others, lemmas);
					advices.add(advice);
					if(advice.contains(ENTRY_SAFELY_REMOVABLE_MARKER)){
						remaining.remove(lineIndex);
						removableEntries.add(advice);
					}
				}
			}
		}

		if(WRITE_ONLY_SAFELY_REMOVABLE_ENTRIES){
			//only the entries that can be removed without losing any inflection
			if(removableEntries.isEmpty())
				return false;

			sb.append(String.join(", ", removableEntries));
			return true;
		}

		for(final String advice : advices)
			details.add(advice);
		sb.append(details);
		return true;
	}

	/**
	 * @param lineIndex	The index used internally (zero-based, header excluded).
	 * @return	The line number as seen in a text editor (one-based, header included).
	 */
	private static int fileLine(final int lineIndex){
		return lineIndex + 2;
	}

	/** @return	The flag, in the dictionary entry, that generates the inflection (empty if the inflection is the entry itself). */
	private static String getRootFlag(final Inflection inflection){
		final AffixEntry[] appliedRules = inflection.getAppliedRules();
		return (appliedRules != null && appliedRules.length > 0? appliedRules[0].getFlag(): StringUtils.EMPTY);
	}

	private static String getRuleChain(final Inflection inflection){
		final AffixEntry[] appliedRules = inflection.getAppliedRules();
		final StringJoiner sj = new StringJoiner(Inflection.LEADS_TO);
		if(appliedRules != null)
			for(final AffixEntry appliedRule : appliedRules)
				sj.add("[" + appliedRule + "]");
		return sj.toString();
	}

}
