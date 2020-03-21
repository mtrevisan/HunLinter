package unit731.hunlinter.workers.dictionary;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.TimSort;
import unit731.hunlinter.services.text.StringHelper;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;


public class SorterWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(SorterWorker.class);

	public static final String WORKER_NAME = "Sorting";

	private final DictionaryParser dicParser;
	private final File dicFile;

	private final Comparator<String> comparator;


	public SorterWorker(final Packager packager, final ParserManager parserManager, final int lineIndex){
		super(new WorkerDataParser<>(WORKER_NAME, parserManager.getDicParser())
			.withParallelProcessing(true)
			.withRelaunchException(false));

		dicFile = packager.getDictionaryFile();
		dicParser = parserManager.getDicParser();
		final Charset charset = dicParser.getCharset();

		//FIXME start by counting how many line terminators there are (CR+LF)
		final AtomicInteger start = new AtomicInteger(0);
		comparator = BaseBuilder.getComparator(parserManager.getAffixData().getLanguage());

		final Function<Void, String[]> step1 = ignored -> {
			prepareProcessing("Splitting dictionary file (step 1/3)");

			String[] chunk = null;
			final Map.Entry<Integer, Integer> boundary = dicParser.getBoundary(lineIndex);
			start.set(boundary.getKey() * 2);
			if(boundary != null){
				parserManager.stopFileListener();

				//split dictionary isolating the sorted section
				chunk = extractSection(boundary, start);

				setProgress(33);
			}
			return chunk;
		};
		final Function<String[], String[]> step2 = chunk -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Sort selected section (step 2/3)");

			//sort the chosen section
			TimSort.sort(chunk, comparator);

			setProgress(67);

			return chunk;
		};
		final Function<String[], Void> step3 = chunk -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Merge sections (step 3/3)");

			//re-merge section
			mergeSectionsToDictionary(dicFile, charset, chunk, start.get());

			dicParser.clear();

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		setProcessor(step1.andThen(step2).andThen(step3));
	}

	private String[] extractSection(final Map.Entry<Integer, Integer> boundary, final AtomicInteger start){
		try(final Scanner scanner = FileHelper.createScanner(dicParser.getDicFile().toPath(), dicParser.getCharset())){
			//skip to begin of chunk
			int lineIndex = 0;
			while(lineIndex ++ < boundary.getKey())
				start.addAndGet(StringHelper.getRawBytes(scanner.nextLine()).length);

			//read lines
			lineIndex --;
			int index = 0;
			final String[] chunk = new String[boundary.getValue() - boundary.getKey() + 1];
			while(lineIndex ++ <= boundary.getValue())
				chunk[index ++] = scanner.nextLine();
			return chunk;
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}

	private void mergeSectionsToDictionary(final File dicFile, final Charset charset, final String[] chunk, final int startIndex){
		try{
			final RandomAccessFile accessor = new RandomAccessFile(dicFile, "rwd");
			accessor.seek(startIndex);
			for(final String line : chunk){
				accessor.write(line.getBytes(charset));
				accessor.writeBytes(StringUtils.CR);
				accessor.writeBytes(StringUtils.LF);
			}
			accessor.close();
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}

}

