package unit731.hunlinter.workers.dictionary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.services.sorters.externalsorter.ExternalSorter;
import unit731.hunlinter.services.sorters.externalsorter.ExternalSorterOptions;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;

import static unit731.hunlinter.services.system.LoopHelper.forEach;


public class WordlistWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordlistWorker.class);

	public static final String WORKER_NAME = "Wordlist";

	public enum WorkerType{COMPLETE, PLAIN_WORDS, PLAIN_WORDS_NO_DUPLICATES}


	public WordlistWorker(final DictionaryParser dicParser, final WordGenerator wordGenerator, final WorkerType type,
			final File outputFile){
		super((WorkerDataParser<DictionaryParser>)new WorkerDataParser<>(WORKER_NAME, dicParser)
			.withParallelProcessing());

		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(outputFile);


		final Charset charset = dicParser.getCharset();


		BufferedWriter writer;
		try{
			writer = Files.newBufferedWriter(outputFile.toPath(), charset);
		}
		catch(final IOException e){
			throw new RuntimeException(e);
		}


		final Function<Inflection, String> toString = (type == WorkerType.COMPLETE? Inflection::toString: Inflection::getWord);
		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			final Inflection[] inflections = wordGenerator.applyAffixRules(dicEntry);

			forEach(inflections, inflection -> writeLine(writer, toString.apply(inflection), StringUtils.LF));
		};

		getWorkerData()
			.withDataCancelledCallback(e -> closeWriter(writer));

		final Function<Void, File> step1 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName());

			final File dicFile = dicParser.getDicFile();
			processLines(dicFile.toPath(), charset, lineProcessor);
			closeWriter(writer);

			return outputFile;
		};
		final Function<File, File> step2 = file -> {
			resetProcessing("Sorting");

			//sort file & remove duplicates
			final ExternalSorter sorter = new ExternalSorter();
			final ExternalSorterOptions options = ExternalSorterOptions.builder()
				.charset(charset)
				.sortInParallel()
				.comparator(dicParser.getComparator())
				.useTemporaryAsZip()
				.removeDuplicates()
				.build();
			try{
				sorter.sort(outputFile, options, outputFile);
			}
			catch(final Exception e){
				throw new RuntimeException(e);
			}

			LOGGER.info(ParserManager.MARKER_APPLICATION, "File written: {}", file.getAbsolutePath());

			finalizeProcessing("Wordlist extracted successfully");

			return file;
		};
		final Function<File, Void> step3 = WorkerManager.openFileStep(LOGGER);
		setProcessor(step1.andThen(step2).andThen(step3));
	}

}
