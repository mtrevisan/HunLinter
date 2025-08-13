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
import java.util.function.Consumer;
import java.util.function.Function;


public class DuplicatesWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(DuplicatesWorker.class);


	private static class DuplicatesDictionaryBaseData extends BloomFilterParameters{

		private static class SingletonHelper{
			private static final DuplicatesDictionaryBaseData INSTANCE = new DuplicatesDictionaryBaseData();
		}


		public static DuplicatesDictionaryBaseData getInstance(){
			return SingletonHelper.INSTANCE;
		}

		protected DuplicatesDictionaryBaseData(){}

		@Override
		public final int getExpectedNumberOfElements(){
			return (int)(MAX_DUPLICATES * 1.5);
		}

		@Override
		public final double getFalsePositiveProbability(){
			return 0.1 / MAX_DUPLICATES;
		}

		@Override
		public final double getGrowthRateWhenFull(){
			return 1.3;
		}

	}

	public static final String WORKER_NAME = "Duplicates extraction";

	private static final int MAX_DUPLICATES = 1_000;


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

		final Function<Void, BloomFilterInterface<String>> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/3)");

			return collectDuplicates();
		};
		final Function<BloomFilterInterface<String>, Collection<List<Duplicate>>> step2 = this::extractDuplicates;
		final Function<Collection<List<Duplicate>>, File> step3 = duplicates -> {
			writeDuplicates(outputFile, duplicates);

			finalizeProcessing("Duplicates extracted successfully");

			return outputFile;
		};
		final Function<File, Void> step4 = WorkerManager.openFileStep(LOGGER);
		setProcessor(step1.andThen(step2).andThen(step3).andThen(step4));
	}

	private BloomFilterInterface<String> collectDuplicates(){
		final File dicFile = dicParser.getDicFile();
		final Charset charset = dicParser.getCharset();

		final BloomFilterInterface<String> bloomFilter = new ScalableInMemoryBloomFilter<>(charset, dictionaryBaseData);
		final BloomFilterInterface<String> duplicatesBloomFilter = new ScalableInMemoryBloomFilter<>(charset,
			DuplicatesDictionaryBaseData.getInstance());

		final BiConsumer<Integer, String> fun = (lineIndex, line) -> {
			try{
				if(duplicatesBloomFilter.getAddedElements() == MAX_DUPLICATES)
					return;

				final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
				final List<Inflection> inflections = wordGenerator.applyAffixRules(dicEntry);

				for(int i = 0; i < inflections.size(); i ++){
					final String str = inflections.get(i).toStringWithPartOfSpeech();
					if(!bloomFilter.add(str)){
						duplicatesBloomFilter.add(str);

						if(duplicatesBloomFilter.getAddedElements() == MAX_DUPLICATES)
							break;
					}
				}
			}
			catch(final LinterException e){
				LOGGER.info(ParserManager.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), lineIndex + 1, line);
			}
		};
		final ProgressCallback progressCallback = lineIndex -> {
			setWorkerProgress(lineIndex);

			sleepOnPause();
		};
		ParserHelper.forEachDictionaryLine(dicFile, charset, fun, progressCallback);

		bloomFilter.close();
		final int totalInflections = bloomFilter.getAddedElements();
		final double falsePositiveProbability = bloomFilter.getTrueFalsePositiveProbability();
		bloomFilter.clear();

		duplicatesBloomFilter.close();

		final int falsePositiveCount = (int)Math.ceil(totalInflections * falsePositiveProbability);
		if(duplicatesBloomFilter.getAddedElements() == MAX_DUPLICATES){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Maximum duplications reached");
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Total inflections processed: {}",
				DictionaryParser.COUNTER_FORMATTER.format(totalInflections));
		}
		else
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Total inflections: {}",
				DictionaryParser.COUNTER_FORMATTER.format(totalInflections));
		LOGGER.info(ParserManager.MARKER_APPLICATION, "False positive probability is {} (overall duplicates ≲ {})",
			DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability), falsePositiveCount);

		return duplicatesBloomFilter;
	}

	private Collection<List<Duplicate>> extractDuplicates(final BloomFilterInterface<String> duplicatesBloomFilter){
		final Map<String, List<Duplicate>> result = new HashMap<>(0);

		if(duplicatesBloomFilter.getAddedElements() > 0){
			resetProcessing("Extracting duplicates (step 2/3)");

			final Charset charset = dicParser.getCharset();
			final File dicFile = dicParser.getDicFile();
			final BiConsumer<Integer, String> fun = (lineIndex, line) -> {
				try{
					final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
					final List<Inflection> inflections = wordGenerator.applyAffixRules(dicEntry);

					if(!inflections.isEmpty()){
						final String word = inflections.get(WordGenerator.BASE_INFLECTION_INDEX).getWord();
						for(int i = 0; i < inflections.size(); i ++){
							final Inflection inflection = inflections.get(i);
							final String text = inflection.toStringWithPartOfSpeech();
							if(duplicatesBloomFilter.contains(text))
								result.computeIfAbsent(text, k -> new ArrayList<>(1))
									.add(new Duplicate(inflection, word, dicEntry.getContinuationFlags(), lineIndex));
						}
					}
				}
				catch(final LinterException e){
					LOGGER.info(ParserManager.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), lineIndex + 1, line);
				}
			};
			final ProgressCallback progressCallback = lineIndex -> {
				setWorkerProgress(lineIndex);

				sleepOnPause();
			};
			ParserHelper.forEachDictionaryLine(dicFile, charset, fun, progressCallback);

			final int totalDuplicates = duplicatesBloomFilter.getAddedElements();
			final double falsePositiveProbability = duplicatesBloomFilter.getTrueFalsePositiveProbability();
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Total duplicates: {}", DictionaryParser.COUNTER_FORMATTER.format(totalDuplicates));
			LOGGER.info(ParserManager.MARKER_APPLICATION, "False positive probability is {} (overall duplicates ≲ {})",
				DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability), (int)Math.ceil(totalDuplicates * falsePositiveProbability));

			duplicatesBloomFilter.clear();
		}
		else
			LOGGER.info(ParserManager.MARKER_APPLICATION, "No duplicates found, skip remaining steps");

		return result.values();
	}

	private void writeDuplicates(final File duplicatesFile, final Collection<List<Duplicate>> duplicates){
		final int totalSize = duplicates.size();
		if(totalSize > 0){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Write results to file (step 3/3)");

			final Set<String> processedLines = new HashSet<>(totalSize);

			int writtenSoFar = 0;
			try(final BufferedWriter writer = Files.newBufferedWriter(duplicatesFile.toPath(), dicParser.getCharset())){
				final StringBuilder origin = new StringBuilder();
				final Comparator<Duplicate> comparator = Comparator.comparingInt(o -> o.getInflection().getAppliedRules().length);
				for(final List<Duplicate> entries : duplicates){
					entries.sort(comparator);

					final StringJoiner lines = new StringJoiner(", ");
					for(final Duplicate duplicate : entries)
						lines.add(Integer.toString(duplicate.getLineIndex()));
					if(!processedLines.add(lines.toString()))
						//already inserted line, skip
						continue;

					final Inflection prod = entries.iterator().next().getInflection();
					origin.setLength(0);
					origin.append(prod.getWord());
					final List<String> partOfSpeechOrInflectionalAffix = prod.getMorphologicalFieldPartOfSpeechOrInflectionalAffix();
					if(!partOfSpeechOrInflectionalAffix.isEmpty())
						origin.append("(")
							.append(String.join(" ", partOfSpeechOrInflectionalAffix))
							.append(")");
					origin.append(": ");
					final StringJoiner sj = new StringJoiner(", ");
					for(final Duplicate duplicate : entries){
						final String info = StringUtils.join(Arrays.asList(duplicate.getWord(), " (", Integer.toString(duplicate.getLineIndex()),
							(duplicate.getInflection().hasInflectionRules()?
							" via " + duplicate.getInflection().getRulesSequence(): StringUtils.EMPTY), ")"), StringUtils.EMPTY);
						sj.add(info);
					}

					final Iterator<Duplicate> itr = entries.iterator();
					final Duplicate firstDuplicate = itr.next();
					final Inflection firstDuplicateInflection = firstDuplicate.getInflection();
					final String firstDuplicateRulesSequence = firstDuplicateInflection.getRulesSequence();
					final Set<String> firstDuplicateContinuationFlags = new HashSet<>(firstDuplicateInflection.getContinuationFlags());
					boolean isCandidate = false;
					while(itr.hasNext()){
						final Duplicate nextDuplicate = itr.next();
						final Inflection nextDuplicateInflection = nextDuplicate.getInflection();
						final String nextDuplicateRulesSequence = nextDuplicateInflection.getRulesSequence();
						final Set<String> nextDuplicateContinuationFlags = new HashSet<>(nextDuplicateInflection.getContinuationFlags());

						Duplicate candidate = firstDuplicate;
						String candidateRulesSequence = firstDuplicateRulesSequence;
						Set<String> candidateContinuationFlags = firstDuplicateContinuationFlags;
						Duplicate other = nextDuplicate;
						String otherRulesSequence = nextDuplicateRulesSequence;
						if(firstDuplicateRulesSequence.length() > nextDuplicateRulesSequence.length()){
							candidate = nextDuplicate;
							other = firstDuplicate;
							candidateRulesSequence = nextDuplicateRulesSequence;
							otherRulesSequence = firstDuplicateRulesSequence;
							candidateContinuationFlags = nextDuplicateContinuationFlags;
						}

						final boolean containmentCondition = (firstDuplicateContinuationFlags.isEmpty()
							|| candidateContinuationFlags.containsAll(firstDuplicateContinuationFlags));
						final String testEnd = Inflection.LEADS_TO + candidateRulesSequence;
						final String testStart = candidateRulesSequence + Inflection.LEADS_TO;
						final boolean equalProduction = (candidateRulesSequence.equals(otherRulesSequence)
							&& candidate.getWord().equals(other.getWord()));
						if(candidateRulesSequence.isEmpty()
								|| containmentCondition && (equalProduction
									|| otherRulesSequence.startsWith(testStart) || otherRulesSequence.endsWith(testEnd))){
							final Set<String> candidateAppliedRules = new HashSet<>(candidate.getContinuationFlags());
							for(final AffixEntry candidateAppliedRule : candidate.getInflection().getAppliedRules())
								candidateAppliedRules.remove(candidateAppliedRule.getFlag());

							final Set<String> otherAppliedRules = new HashSet<>(other.getContinuationFlags());
							for(final AffixEntry otherAppliedRule : other.getInflection().getAppliedRules())
								otherAppliedRules.remove(otherAppliedRule.getFlag());

							final Set<String> intersectionAppliedRules = new HashSet<>(candidateAppliedRules);
							intersectionAppliedRules.retainAll(otherAppliedRules);
							candidateAppliedRules.removeAll(intersectionAppliedRules);
							otherAppliedRules.removeAll(intersectionAppliedRules);
							if(candidateAppliedRules.isEmpty()){
								final String info = candidate.getWord()
									+ (equalProduction && ! other.getWord().equals(candidate.getWord())? " (or " + other.getWord() + ")": "")
									+ " is a candidate for removal";
								if(!sj.toString().endsWith(info))
									sj.add(info);
							}
							else
								sj.add(candidate.getWord() + " cannot be safely removed due to candidate having flags "
									+ candidateAppliedRules
									+ (!otherAppliedRules.isEmpty()? " and other having flags " + otherAppliedRules: ""));

							isCandidate = true;
						}
					}

					if(isCandidate){
						writer.write(origin.toString());
						writer.write(sj.toString());
						writer.newLine();
					}

					setWorkerProgress(++ writtenSoFar, totalSize);

					sleepOnPause();
				}
			}
			catch(final IOException ioe){
				throw new WriterException(ioe);
			}

			LOGGER.info(ParserManager.MARKER_APPLICATION, "File written: {}", duplicatesFile.getAbsolutePath());
		}
	}

}
