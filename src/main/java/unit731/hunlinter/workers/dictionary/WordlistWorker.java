package unit731.hunlinter.workers.dictionary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.services.system.JavaHelper;
import unit731.hunlinter.services.system.LoopHelper;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import unit731.hunlinter.workers.exceptions.LinterException;


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


		final List<IndexDataPair<String>> collectedLines = new ArrayList<>();
		final Consumer<IndexDataPair<String>> lineReadProcessor = collectedLines::add;
		final Set<String> words = new HashSet<>();
		final Function<Production, String> toString = (type == WorkerType.COMPLETE? Production::toString: Production::getWord);
		final BiConsumer<BufferedWriter, IndexDataPair<String>> lineProcessor = (writer, indexData) -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			final Production[] productions = wordGenerator.applyAffixRules(dicEntry);

			if(type == WorkerType.PLAIN_WORDS_NO_DUPLICATES)
				LoopHelper.forEach(productions, production -> words.add(production.toString()));
			else{
				try{
					for(final Production production : productions){
						writer.write(toString.apply(production));
						writer.newLine();
					}
				}
				catch(final IOException e){
					throw new LinterException(e.getMessage());
				}
			}
		};
		final Runnable completed = () -> {
			if(type == WorkerType.PLAIN_WORDS_NO_DUPLICATES){
				LOGGER.info(ParserManager.MARKER_APPLICATION, "Write file: {}", outputFile.getAbsolutePath());

				writeProcess(outputFile, words);
			}

			finalizeProcessing("File written: " + outputFile.getAbsolutePath());

			WorkerManager.openFileStep(LOGGER).apply(outputFile);
		};

		getWorkerData()
			.withDataCompletedCallback(completed);

		final Function<Void, List<IndexDataPair<String>>> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/2)");

			final Path dicPath = dicParser.getDicFile().toPath();
			processLines(dicPath, charset, lineReadProcessor);

			return collectedLines;
		};
		final Function<List<IndexDataPair<String>>, Void> step2 = lines -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Execute " + workerData.getWorkerName() + " (step 2/2)");

			executeWriteProcess(lineProcessor, lines, outputFile, charset);

			return null;
		};
		setProcessor(step1.andThen(step2));
	}

	private void writeProcess(final File outputFile, final Set<String> words){
		int writtenSoFar = 0;
		final int totalLines = words.size();
		final DictionaryParser dicParser = workerData.getParser();
		final Charset charset = dicParser.getCharset();
		try(final BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), charset)){
			final List<String> sortedWords = new ArrayList<>(words);
			final Comparator<String> comparator = workerData.getParser().getComparator();
			sortedWords.sort(comparator);
			for(final String word : words){
				try{
					writtenSoFar ++;

					writer.write(word);
					writer.write(StringUtils.LF);

					setProgress(writtenSoFar, totalLines);

					sleepOnPause();
				}
				catch(final Exception e){
					if(!JavaHelper.isInterruptedException(e))
						LOGGER.info(ParserManager.MARKER_APPLICATION, "{}: {}", e.getMessage(), word);

					throw e;
				}
			}
		}
		catch(final Exception e){
			cancel(e);
		}
	}

}
