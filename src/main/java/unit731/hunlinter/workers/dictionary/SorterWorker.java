package unit731.hunlinter.workers.dictionary;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.services.system.FileHelper;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;


public class SorterWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(SorterWorker.class);

	public static final String WORKER_NAME = "Sorting";

	private final DictionaryParser dicParser;

	private final Comparator<String> comparator;


	public SorterWorker(final File dicFile, final ParserManager parserManager, final int lineIndex){
		super(new WorkerDataParser<>(WORKER_NAME, parserManager.getDicParser()));

		Objects.requireNonNull(dicFile);

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		dicParser = parserManager.getDicParser();

		comparator = BaseBuilder.getComparator(parserManager.getLanguage());
		final Map.Entry<Integer, Integer> boundary = dicParser.getBoundary(lineIndex);
		//here `boundary` cannot be null

		final Function<Void, List<String>> step1 = ignored -> {
			prepareProcessing("Load dictionary file (step 1/3)");

			List<String> lines;
			try{
				lines = FileHelper.readAllLines(dicParser.getDicFile().toPath(), dicParser.getCharset());
			}
			catch(final Exception e){
				throw new RuntimeException(e.getMessage());
			}

			setProgress(33);

			return lines;
		};
		final Function<List<String>, List<String>> step2 = lines -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Sort selected section (step 2/3)");

			//sort the chosen section
			lines.subList(boundary.getKey(), boundary.getValue())
				.sort(comparator);

			setProgress(67);

			return lines;
		};
		final Function<List<String>, Void> step3 = lines -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Merge sections (step 3/3)");

			try{
				FileHelper.saveFile(dicParser.getDicFile().toPath(), System.lineSeparator(), dicParser.getCharset(), lines);

				dicParser.removeBoundary(boundary.getKey());

				finalizeProcessing("Successfully processed " + workerData.getWorkerName());
			}
			catch(final Exception e){
				throw new RuntimeException(e.getMessage());
			}

			return null;
		};
		setProcessor(step1.andThen(step2).andThen(step3));
	}

}

