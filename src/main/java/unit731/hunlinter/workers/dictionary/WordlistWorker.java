package unit731.hunlinter.workers.dictionary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.FileHelper;


public class WordlistWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordlistWorker.class);

	public static final String WORKER_NAME = "Wordlist";

	public enum WorkerType{COMPLETE, PLAIN_WORDS}


	public WordlistWorker(final DictionaryParser dicParser, final WordGenerator wordGenerator, final WorkerType type,
			final File outputFile){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser)
			.withParallelProcessing(true));

		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(outputFile);


		final Function<Production, String> toString = (type == WorkerType.COMPLETE? Production::toString: Production::getWord);
		final BiConsumer<BufferedWriter, Pair<Integer, String>> lineProcessor = (writer, line) -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line.getValue());
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);

			try{
				for(final Production production : productions){
					writer.write(toString.apply(production));
					writer.newLine();
				}
			}
			catch(final IOException e){
				throw new LinterException(e.getMessage());
			}
		};
		final Runnable completed = () -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "File written: {}", outputFile.getAbsolutePath());

			try{
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

}
