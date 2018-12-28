package unit731.hunspeller.parsers.dictionary.workers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;
import unit731.hunspeller.services.FileHelper;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class WordlistWorker extends WorkerDictionaryBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordCountWorker.class);

	public static final String WORKER_NAME = "Wordlist";


	public WordlistWorker(DictionaryParser dicParser, WordGenerator wordGenerator, File outputFile, ReadWriteLockable lockable){
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(outputFile);
		Objects.requireNonNull(lockable);


		BiConsumer<BufferedWriter, Pair<Integer, String>> lineProcessor = (writer, line) -> {
			List<Production> productions = wordGenerator.applyAffixRules(line.getValue());

			try{
				for(Production production : productions){
					writer.write(production.getWord());
					writer.newLine();
				}
			}
			catch(IOException e){
				throw new IllegalArgumentException(e);
			}
		};
		Runnable completed = () -> {
			LOGGER.info(Backbone.MARKER_APPLICATION, "File written: {}", outputFile.getAbsolutePath());

			try{
				FileHelper.openFileWithChoosenEditor(outputFile);
			}
			catch(IOException | InterruptedException e){
				LOGGER.warn("Exception while opening the resulting file", e);
			}
		};
		createReadWriteWorker(WORKER_NAME, dicParser, outputFile, lineProcessor, completed, null, lockable);
	}

}
