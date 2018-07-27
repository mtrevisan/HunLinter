package unit731.hunspeller.parsers.dictionary.workers;

import java.io.LineNumberReader;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.util.List;
import javax.swing.SwingWorker;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
public class CorrectnessWorker extends SwingWorker<Void, String>{

	private final Backbone backbone;


	@Override
	protected Void doInBackground() throws Exception{
		int lineIndex = 1;
		boolean stopped = false;
		try{
			publish("Opening Dictionary file for correctness checking: " + backbone.affParser.getLanguage() + ".dic");

			TimeWatch watch = TimeWatch.start();

			FlagParsingStrategy strategy = backbone.affParser.getFlagParsingStrategy();

			setProgress(0);
			try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(backbone.dicParser.getDicFile().toPath(), backbone.dicParser.getCharset()))){
				String line = br.readLine();
				//ignore any BOM marker on first line
				if(br.getLineNumber() == 1)
					line = FileService.clearBOMMarker(line);
				if(!NumberUtils.isCreatable(line))
					throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

				long readSoFar = line.length();
				long totalSize = backbone.dicParser.getDicFile().length();
				while((line = br.readLine()) != null){
					lineIndex ++;
					readSoFar += line.length();
					line = DictionaryParser.cleanLine(line);
					if(!line.isEmpty()){
						try{
							DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);

							List<RuleProductionEntry> productions = backbone.dicParser.getWordGenerator().applyRules(dictionaryWord);

							productions.forEach(production -> backbone.dicParser.checkProduction(production, strategy));
						}
						catch(IllegalArgumentException e){
							publish(e.getMessage() + " on line " + lineIndex + ": " + line);
						}
					}

					setProgress((int)Math.ceil((readSoFar * 100.) / totalSize));
				}
			}

			watch.stop();

			setProgress(100);

			publish("Finished processing Dictionary file (it takes " + watch.toStringMinuteSeconds() + ")");
		}
		catch(Exception e){
			stopped = true;

			if(e instanceof ClosedChannelException)
				publish("Correctness thread interrupted");
			else{
				String message = ExceptionService.getMessage(e);
				publish(e.getClass().getSimpleName() + (lineIndex >= 0? " on line " + lineIndex: StringUtils.EMPTY) + ": " + message);
			}
		}
		if(stopped)
			publish("Stopped processing Dictionary file");

		return null;
	}

	@Override
	protected void process(List<String> chunks){
		for(String chunk : chunks)
			log.info(Backbone.MARKER_APPLICATION, chunk);
	}

}
