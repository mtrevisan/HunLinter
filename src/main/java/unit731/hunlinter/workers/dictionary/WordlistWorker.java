package unit731.hunlinter.workers.dictionary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.services.system.JavaHelper;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.FileHelper;


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


		final Set<String> words = new HashSet<>();
		final Function<Production, String> toString = (type == WorkerType.COMPLETE? Production::toString: Production::getWord);
		final BiConsumer<BufferedWriter, Pair<Integer, String>> lineProcessor = (writer, line) -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line.getValue());
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);

			if(type == WorkerType.PLAIN_WORDS_NO_DUPLICATES)
				productions.stream()
					.map(toString::apply)
					.forEach(words::add);
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

				writeProcess(words);
			}

			try{
				finalizeProcessing("File written: " + outputFile.getAbsolutePath());

				FileHelper.openFileWithChosenEditor(outputFile);
			}
			catch(final IOException | InterruptedException e){
				LOGGER.warn("Exception while opening the resulting file", e);
			}
		};

		setWriteDataProcessor(lineProcessor, outputFile);
		getWorkerData()
			.withDataCompletedCallback(completed);
	}

	private void writeProcess(final Set<String> words){
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
