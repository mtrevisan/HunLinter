package unit731.hunlinter.workers.dictionary;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import unit731.hunlinter.services.externalsorter.ExternalSorterOptions;


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

		comparator = BaseBuilder.getComparator(parserManager.getAffixData().getLanguage());

		final Function<Void, List<File>> step1 = ignored -> {
			prepareProcessing("Splitting dictionary file (step 1/4)");

			List<File> chunks = null;
			final Map.Entry<Integer, Integer> boundary = dicParser.getBoundary(lineIndex);
			if(boundary != null){
				parserManager.stopFileListener();

				//split dictionary isolating the sorted section
				chunks = splitDictionary(boundary);

				setProgress(25);
			}
			return chunks;
		};
		final Function<List<File>, List<File>> step2 = chunks -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Sort selected section (step 2/4)");

			sortSection(chunks);

			setProgress(50);

			return chunks;
		};
		final Function<List<File>, List<File>> step3 = chunks -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Merge sections (step 3/4)");

			//re-merge sections
			mergeSectionsToDictionary(dicFile, chunks);

			setProgress(75);

			return chunks;
		};
		final Function<List<File>, Void> step4 = chunks -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Finalize sort (step 4/4)");

			//remove temporary files
			chunks.forEach(File::delete);

			dicParser.clear();

			setProgress(100);

			return null;
		};
		setProcessor(step1.andThen(step2).andThen(step3).andThen(step4));
	}

	private List<File> splitDictionary(final Map.Entry<Integer, Integer> boundary){
		int index = 0;
		final List<File> files = new ArrayList<>();
		try(final BufferedReader br = Files.newBufferedReader(dicParser.getDicFile().toPath(), dicParser.getCharset())){
			File file = File.createTempFile("split", ".out");
			BufferedWriter writer = Files.newBufferedWriter(file.toPath(), dicParser.getCharset());
			String line;
			while((line = br.readLine()) != null){
				if(index == boundary.getKey() || index == boundary.getValue() + 1){
					writer.close();

					files.add(file);

					file = File.createTempFile("split", ".out");
					writer = Files.newBufferedWriter(file.toPath(), dicParser.getCharset());
				}

				writer.write(line);
				writer.newLine();

				index ++;

				sleepOnPause();
			}

			writer.close();

			files.add(file);
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
		return files;
	}

	private void sortSection(final List<File> chunks){
		//sort the chosen section
		final File sortSection = chunks.get(1);
		final ExternalSorterOptions options = ExternalSorterOptions.builder()
			.charset(dicParser.getCharset())
			.comparator(comparator)
			.useZip(true)
			.removeDuplicates(true)
			.build();
		try{
			dicParser.getSorter().sort(sortSection, options, sortSection);
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}

	private void mergeSectionsToDictionary(final File dicFile, final List<File> files){
		try{
			OpenOption option = StandardOpenOption.TRUNCATE_EXISTING;
			for(File file : files){
				Files.write(dicFile.toPath(), Files.readAllBytes(file.toPath()), option);

				option = StandardOpenOption.APPEND;
			}
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}

}

