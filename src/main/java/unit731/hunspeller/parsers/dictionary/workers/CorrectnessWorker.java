package unit731.hunspeller.parsers.dictionary.workers;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.util.List;
import javax.swing.SwingWorker;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.RuleProductionEntry;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.FileService;


@AllArgsConstructor
public class CorrectnessWorker extends SwingWorker<Void, String>{

	private final AffixParser affParser;
	private final DictionaryParser dicParser;
	private final Resultable resultable;


	@Override
	protected Void doInBackground() throws Exception{
		int lineIndex = 1;
		try{
			publish("Opening Dictionary file for correctness checking: " + affParser.getLanguage() + ".dic");

			FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();

			setProgress(0);
			try(BufferedReader br = Files.newBufferedReader(dicParser.getDicFile().toPath(), dicParser.getCharset())){
				String line = br.readLine();
				//ignore any BOM marker on first line
				if(line.startsWith(FileService.BOM_MARKER))
					line = line.substring(1);
				if(!NumberUtils.isCreatable(line))
					throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

				long readSoFar = line.length();
				long totalSize = dicParser.getDicFile().length();
				while((line = br.readLine()) != null){
					lineIndex ++;
					readSoFar += line.length();
					line = dicParser.cleanLine(line);
					if(!line.isEmpty()){
						DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);

						try{
							List<RuleProductionEntry> productions = dicParser.getWordGenerator().applyRules(dictionaryWord);

							productions.forEach(production -> dicParser.checkProduction(production, strategy));
						}
						catch(IllegalArgumentException e){
							publish(e.getMessage() + " on line " + lineIndex + ": " + dictionaryWord.toWordAndFlagString());
						}
					}

					setProgress((int)Math.ceil((readSoFar * 100.) / totalSize));
				}
			}
			setProgress(100);

			publish("Finished reading Dictionary file");
		}
		catch(IOException | IllegalArgumentException e){
			publish(e instanceof ClosedChannelException? "Correctness thread interrupted": e.getClass().getSimpleName() + ": " + e.getMessage());
			publish("Stopped reading Dictionary file");
		}
		catch(Exception e){
			String message = ExceptionService.getMessage(e, getClass());
			publish(e.getClass().getSimpleName() + (lineIndex >= 0? " on line " + lineIndex: StringUtils.EMPTY) + ": " + message);
			publish("Stopped reading Dictionary file");
		}
		return null;
	}

	@Override
	protected void process(List<String> chunks){
		resultable.printResultLine(chunks);
	}

}
