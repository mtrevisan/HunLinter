package unit731.hunlinter.workers.dictionary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
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
		super(new WorkerDataParser<>(WORKER_NAME, dicParser)
			.withParallelProcessing(true));

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


		final Function<Production, String> toString = (type == WorkerType.COMPLETE? Production::toString: Production::getWord);
		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			final Production[] productions = wordGenerator.applyAffixRules(dicEntry);

			forEach(productions, production -> writeLine(writer, toString.apply(production)));
		};

		getWorkerData()
			.withDataCancelledCallback(e -> closeWriter(writer));

		final Function<Void, File> step1 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName());

			final File dicFile = dicParser.getDicFile();
			processLines(dicFile.toPath(), charset, lineProcessor);
			closeWriter(writer);

			LOGGER.info(ParserManager.MARKER_APPLICATION, "Sorting");

			if(type == WorkerType.PLAIN_WORDS_NO_DUPLICATES){
//FIXME
//				//sort file & remove duplicates
//				final ExternalSorter sorter = new ExternalSorter();
//				final ExternalSorterOptions options = ExternalSorterOptions.builder()
//					.charset(charset)
//					//lexical order
//					.comparator(Comparator.naturalOrder())
//					.useZip(true)
//					.removeDuplicates(true)
//					.build();
//				try{
//					sorter.sort(outputFile, options, outputFile);
//				}
//				catch(final Exception e){
//					throw new RuntimeException(e);
//				}
			}

			LOGGER.info(ParserManager.MARKER_APPLICATION, "File written: {}", outputFile.getAbsolutePath());

			finalizeProcessing("Wordlist extracted successfully");

			return outputFile;
		};
		final Function<File, Void> step2 = WorkerManager.openFileStep(LOGGER);
		setProcessor(step1.andThen(step2));
	}

}
