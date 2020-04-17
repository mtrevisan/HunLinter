package unit731.hunlinter.workers.dictionary;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.services.sorters.SmoothSort;
import unit731.hunlinter.services.system.FileHelper;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;


public class SorterWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(SorterWorker.class);

	public static final String WORKER_NAME = "Sorting";

	private static final byte[] NEW_LINE = {'\r', '\n'};

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

		final Function<Void, String[]> step1 = ignored -> {
			prepareProcessing("Load dictionary file (step 1/3)");

			final String[] chunk;
			try{
				//split dictionary isolating the sorted section
				chunk = extractSection(boundary);
			}
			catch(final Exception e){
				throw new RuntimeException(e.getMessage());
			}

			setProgress(33);

			return chunk;
		};
		final Function<String[], String[]> step2 = chunk -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Sort selected section (step 2/3)");

			//sort the chosen section
			SmoothSort.sort(chunk, comparator);

			setProgress(67);

			return chunk;
		};
		final Function<String[], Void> step3 = chunk -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Merge sections (step 3/3)");

			//merge section
			mergeSectionToDictionary(chunk, boundary.getValue());

			dicParser.removeBoundary(boundary.getKey());

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		setProcessor(step1.andThen(step2).andThen(step3));
	}

	private String[] extractSection(final Map.Entry<Integer, Integer> boundary) throws IOException{
		try(final Scanner scanner = FileHelper.createScanner(dicParser.getDicFile().toPath(), dicParser.getCharset())){
			//skip to begin of chunk
			int lineIndex = 0;
			while(lineIndex ++ < boundary.getKey())
				scanner.nextLine();
			lineIndex --;

			//read lines
			int index = 0;
			final String[] chunk = new String[boundary.getValue() - boundary.getKey() + 1];
			while(lineIndex ++ <= boundary.getValue())
				chunk[index ++] = scanner.nextLine();
			return chunk;
		}
	}

	private void mergeSectionToDictionary(final String[] chunk, final int startIndex){
		try(final RandomAccessFile accessor = new RandomAccessFile(dicParser.getDicFile(), "rwd")){
			accessor.seek(startIndex);
			for(final String line : chunk){
				accessor.write(line.getBytes(dicParser.getCharset()));
				accessor.write(NEW_LINE);
			}
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}

}

