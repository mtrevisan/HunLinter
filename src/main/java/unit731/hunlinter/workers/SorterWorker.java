package unit731.hunlinter.workers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.Backbone;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import unit731.hunlinter.services.externalsorter.ExternalSorterOptions;


public class SorterWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(SorterWorker.class);

	public static final String WORKER_NAME = "Sorting";

	private final Backbone backbone;
	private final DictionaryParser dicParser;
	private final int lineIndex;

	private final Comparator<String> comparator;


	public SorterWorker(final Backbone backbone, final int lineIndex){
		super(new WorkerDataParser<>(WORKER_NAME, backbone.getDicParser())
			.withParallelProcessing(true)
			.withRelaunchException(false));

		this.backbone = backbone;
		dicParser = backbone.getDicParser();
		this.lineIndex = lineIndex;

		comparator = BaseBuilder.getComparator(backbone.getAffixData().getLanguage());
	}

	@Override
	protected Void doInBackground(){
		try{
			prepareProcessing("Sorting Dictionary file");

			final Map.Entry<Integer, Integer> boundary = dicParser.getBoundary(lineIndex);
			if(boundary != null){
				backbone.stopFileListener();

				//split dictionary isolating the sorted section
				final List<File> chunks = splitDictionary(boundary);

				setProgress(25);

				sortSection(chunks);

				setProgress(50);

				//re-merge sections
				backbone.mergeSectionsToDictionary(chunks);

				setProgress(75);

				//remove temporary files
				chunks.forEach(File::delete);

				LOGGER.info(Backbone.MARKER_APPLICATION, "File sorted");

				dicParser.clear();

				backbone.startFileListener();
			}
			else
				LOGGER.info(Backbone.MARKER_APPLICATION, "File NOT sorted");

			setProgress(100);
		}
		catch(final ClosedChannelException e){
			cancel(e);

			LOGGER.warn(Backbone.MARKER_APPLICATION, "Duplicates thread interrupted");
		}
		catch(final Exception e){
			cancel(e);

			LOGGER.error(Backbone.MARKER_APPLICATION, e.getMessage());
		}

		return null;
	}

	private List<File> splitDictionary(final Map.Entry<Integer, Integer> boundary) throws IOException, InterruptedException{
		int index = 0;
		final List<File> files = new ArrayList<>();
		File file = File.createTempFile("split", ".out");
		try(final BufferedReader br = Files.newBufferedReader(dicParser.getDicFile().toPath(), dicParser.getCharset())){
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
		return files;
	}

	private void sortSection(final List<File> chunks) throws IOException{
		//sort the chosen section
		final File sortSection = chunks.get(1);
		final ExternalSorterOptions options = ExternalSorterOptions.builder()
			.charset(dicParser.getCharset())
			.comparator(comparator)
			.useZip(true)
			.removeDuplicates(true)
			.build();
		dicParser.getSorter().sort(sortSection, options, sortSection);
	}

}
