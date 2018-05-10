package unit731.hunspeller.parsers.dictionary.workers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.SwingWorker;
import lombok.AllArgsConstructor;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.languages.builders.ComparatorBuilder;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.externalsorter.ExternalSorterOptions;


@AllArgsConstructor
public class SorterWorker extends SwingWorker<Void, String>{

	private final DictionaryParser dicParser;
	private final int lineIndex;
	private final Resultable resultable;


	@Override
	protected Void doInBackground() throws Exception{
		try{
			publish("Sorting file " + dicParser.getDicFile().getName());
			setProgress(0);

			//extract boundaries from the file (from comment to comment, or blank line)
			dicParser.calculateDictionaryBoundaries();

			setProgress(20);

			Map.Entry<Integer, Integer> boundary = dicParser.getBoundary(lineIndex);
			if(boundary != null){
				//split dictionary isolating the sorted section
				List<File> chunks = splitDictionary(boundary);

				setProgress(40);

				//sort the chosen section
				File sortSection = chunks.get(1);
				ExternalSorterOptions options = ExternalSorterOptions.builder()
					.charset(dicParser.getCharset())
					.comparator(ComparatorBuilder.getComparator(dicParser.getLanguage()))
					.useZip(true)
					.removeDuplicates(true)
					.build();
				dicParser.getSorter().sort(sortSection, options, sortSection);

				setProgress(60);

				//re-merge dictionary
				mergeDictionary(chunks);

				setProgress(80);

				//remove temporary files
				chunks.forEach(File::delete);

				publish("File " + dicParser.getDicFile().getName() + " sorted");

				dicParser.getBoundaries().clear();
			}
			else
				publish("File " + dicParser.getDicFile().getName() + " NOT sorted");

			setProgress(100);
		}
		catch(IOException | IllegalArgumentException e){
			publish(e instanceof ClosedChannelException? "Duplicates thread interrupted": e.getClass().getSimpleName() + ": " + e.getMessage());
			publish("Stopped reading Dictionary file");
		}
		catch(Exception e){
			String message = ExceptionService.getMessage(e, getClass());
			publish(e.getClass().getSimpleName() + ": " + message);
			publish("Stopped reading Dictionary file");
		}
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

	private void mergeDictionary(List<File> files) throws IOException{
		boolean append = false;
		for(File file : files){
			copyFile(file, append);

			append = true;
		}
	}

	private void copyFile(File inputFile, boolean append) throws IOException{
		try(
				BufferedReader br = Files.newBufferedReader(inputFile.toPath(), dicParser.getCharset());
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dicParser.getDicFile(), append), dicParser.getCharset()));
				){
			String line;
			while((line = br.readLine()) != null){
				writer.write(line);
				writer.newLine();
			}
		}
	}

	@Override
	protected void process(List<String> chunks){
		resultable.printResultLine(chunks);
	}

}

