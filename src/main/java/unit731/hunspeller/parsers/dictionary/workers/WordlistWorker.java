package unit731.hunspeller.parsers.dictionary.workers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.LineNumberReader;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.util.List;
import javax.swing.SwingWorker;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.TimeWatch;


@AllArgsConstructor
@Slf4j
public class WordlistWorker extends SwingWorker<Void, String>{

	private final Backbone backbone;
	private final File outputFile;


	@Override
	protected Void doInBackground() throws Exception{
		boolean stopped = false;
		try{
			publish("Opening Dictionary file for wordlist extraction: " + backbone.affParser.getLanguage() + ".dic");

			TimeWatch watch = TimeWatch.start();

			FlagParsingStrategy strategy = backbone.affParser.getFlagParsingStrategy();

			setProgress(0);
			try(
					LineNumberReader br = new LineNumberReader(Files.newBufferedReader(backbone.dicParser.getDicFile().toPath(), backbone.dicParser.getCharset()));
					BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), backbone.dicParser.getCharset());
					){
				String line = br.readLine();
				//ignore any BOM marker on first line
				if(br.getLineNumber() == 1)
					line = FileService.clearBOMMarker(line);
				if(!NumberUtils.isCreatable(line))
					throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

				int lineIndex = 1;
				long readSoFar = line.length();
				long totalSize = backbone.dicParser.getDicFile().length();
				while((line = br.readLine()) != null){
					lineIndex ++;
					readSoFar += line.length();

					line = DictionaryParser.cleanLine(line);
					if(!line.isEmpty()){
						DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);
						try{
							List<RuleProductionEntry> productions = backbone.dicParser.getWordGenerator().applyRules(dictionaryWord);

							for(RuleProductionEntry production : productions){
								writer.write(production.getWord());
								writer.newLine();
							}
						}
						catch(IllegalArgumentException e){
							publish(e.getMessage() + " on line " + lineIndex + ": " + dictionaryWord.toString());
						}
					}

					setProgress((int)Math.ceil((readSoFar * 100.) / totalSize));
				}
			}

			watch.stop();

			setProgress(100);

			publish("File written: " + outputFile.getAbsolutePath());
			publish("Wordlist extracted successfully (it takes " + watch.toStringMinuteSeconds() + ")");

			DictionaryParser.openFileWithChoosenEditor(outputFile);
		}
		catch(Exception e){
			stopped = true;

			if(e instanceof ClosedChannelException)
				publish("Duplicates thread interrupted");
			else{
				String message = ExceptionService.getMessage(e);
				publish(e.getClass().getSimpleName() + ": " + message);
			}
		}
		if(stopped)
			publish("Stopped reading Dictionary file");

		return null;
	}

	@Override
	protected void process(List<String> chunks){
		for(String chunk : chunks)
			log.info(Backbone.MARKER_APPLICATION, chunk);
	}

}
