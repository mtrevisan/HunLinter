package unit731.hunlinter.workers.dictionary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.TimSort;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import unit731.hunlinter.workers.exceptions.LinterException;

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
		final Comparator<String> comparator = dicParser.getComparator();


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

			return outputFile;
		};
		final Function<File, File> step2 = file -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Sorting");

			//sort file & remove duplicates
			String[] sortedLines;
			try(final Scanner scanner = FileHelper.createScanner(file.toPath(), charset)){
				final List<String> lines = new ArrayList<>();
				while(scanner.hasNextLine())
					lines.add(scanner.nextLine());
				sortedLines = lines.toArray(String[]::new);
				TimSort.sort(sortedLines, comparator);

				TimSort.removeDuplicates(sortedLines);
			}
			catch(final Exception t){
				throw new LinterException(t.getMessage());
			}

			try(final BufferedWriter writer2 = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)){
				for(final String line : sortedLines){
					writer2.write(line);
					writer2.newLine();
				}
			}
			catch(final Exception t){
				throw new LinterException(t.getMessage());
			}

			LOGGER.info(ParserManager.MARKER_APPLICATION, "File written: {}", file.getAbsolutePath());

			finalizeProcessing("Wordlist extracted successfully");

			return file;
		};
		final Function<File, Void> step3 = WorkerManager.openFileStep(LOGGER);
		setProcessor(step1.andThen(step2).andThen(step3));
	}

}
