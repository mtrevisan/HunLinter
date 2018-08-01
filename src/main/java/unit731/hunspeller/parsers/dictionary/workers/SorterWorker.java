package unit731.hunspeller.parsers.dictionary.workers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.languages.builders.ComparatorBuilder;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerBase;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.externalsorter.ExternalSorterOptions;


@Slf4j
public class SorterWorker extends WorkerBase<Void, Void>{

	public static final String WORKER_NAME = "Sorting";

	private final Backbone backbone;
	private final DictionaryParser dicParser;
	private final int lineIndex;


	public SorterWorker(Backbone backbone, int lineIndex){
		Objects.requireNonNull(backbone);
		Objects.requireNonNull(backbone.getDicParser());

		this.backbone = backbone;
		dicParser = backbone.getDicParser();
		this.lineIndex = lineIndex;
		workerName = WORKER_NAME;
	}

	@Override
	protected Void doInBackground() throws Exception{
		boolean stopped = false;
		try{
			log.info(Backbone.MARKER_APPLICATION, "Sorting Dictionary file");
			setProgress(0);

			//extract boundaries from the file (from comment to comment, or blank line)
			dicParser.calculateDictionaryBoundaries();

			setProgress(20);

			Map.Entry<Integer, Integer> boundary = dicParser.getBoundary(lineIndex);
			if(boundary != null){
				backbone.stopFileListener();

				//split dictionary isolating the sorted section
				List<File> chunks = splitDictionary(boundary);

				setProgress(40);

				sortSection(chunks);

				setProgress(60);

				//re-merge sections
				backbone.mergeSectionsToDictionary(chunks);

				setProgress(80);

				//remove temporary files
				chunks.forEach(File::delete);

				log.info(Backbone.MARKER_APPLICATION, "File sorted");

				dicParser.clear();

				backbone.startFileListener();
			}
			else
				log.info(Backbone.MARKER_APPLICATION, "File NOT sorted");

			setProgress(100);
		}
		catch(Exception e){
			stopped = true;

			if(e instanceof ClosedChannelException)
				log.warn(Backbone.MARKER_APPLICATION, "Duplicates thread interrupted");
			else{
				String message = ExceptionService.getMessage(e);
				log.error(Backbone.MARKER_APPLICATION, e.getClass().getSimpleName() + ": " + message);
			}
		}
		if(stopped)
			log.info(Backbone.MARKER_APPLICATION, "Stopped reading Dictionary file");

		return null;
	}

	private List<File> splitDictionary(Map.Entry<Integer, Integer> boundary) throws IOException{
		int index = 0;
		List<File> files = new ArrayList<>();
		File file = File.createTempFile("split", ".out");
		try(BufferedReader br = Files.newBufferedReader(dicParser.getDicFile().toPath(), dicParser.getCharset())){
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
			}

			writer.close();

			files.add(file);
		}
		return files;
	}

	private void sortSection(List<File> chunks) throws IOException{
		//sort the chosen section
		File sortSection = chunks.get(1);
		ExternalSorterOptions options = ExternalSorterOptions.builder()
			.charset(dicParser.getCharset())
			.comparator(ComparatorBuilder.getComparator(dicParser.getLanguage()))
			.useZip(true)
			.removeDuplicates(true)
			.build();
		dicParser.getSorter().sort(sortSection, options, sortSection);
	}

}

