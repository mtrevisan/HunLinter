package unit731.hunspeller.parsers.dictionary.workers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import javax.swing.SwingWorker;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import static unit731.hunspeller.parsers.dictionary.DictionaryParser.openFileWithChoosenEditor;
import unit731.hunspeller.parsers.dictionary.RuleProductionEntry;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.ExceptionService;


@AllArgsConstructor
public class WordlistWorker extends SwingWorker<Void, String>{

	private final AffixParser affParser;
	private final DictionaryParser dicParser;
	private final File outputFile;
	private final Resultable resultable;


	@Override
	protected Void doInBackground() throws Exception{
		try{
			publish("Opening Dictionary file for wordlist extraction: " + affParser.getLanguage() + ".dic");

			FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();

			setProgress(0);
			try(
					BufferedReader br = Files.newBufferedReader(dicParser.getDicFile().toPath(), dicParser.getCharset());
					BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), dicParser.getCharset());
					){
				String line = br.readLine();
				if(!NumberUtils.isCreatable(line))
					throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

				int lineIndex = 1;
				long readSoFar = line.length();
				long totalSize = dicParser.getDicFile().length();
				while(Objects.nonNull(line = br.readLine())){
					lineIndex ++;
					readSoFar += line.length();

					line = dicParser.cleanLine(line);
					if(!line.isEmpty()){
						DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);
						try{
							List<RuleProductionEntry> productions = dicParser.getWordGenerator().applyRules(dictionaryWord);

							for(RuleProductionEntry production : productions){
								writer.write(production.getWord());
								writer.newLine();
							}
						}
						catch(IllegalArgumentException e){
							publish(e.getMessage() + " on line " + lineIndex + ": " + dictionaryWord.toStringBasic());
						}
					}

					setProgress((int)Math.ceil((readSoFar * 100.) / totalSize));
				}
			}

			setProgress(100);

			publish("File written: " + outputFile.getAbsolutePath());

			publish("Wordlist extracted successfully");

			openFileWithChoosenEditor(outputFile);
		}
		catch(IOException | IllegalArgumentException e){
			publish(e instanceof ClosedChannelException? "Wodlist thread interrupted": e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		catch(Exception e){
			String message = ExceptionService.getMessage(e, getClass());
			publish(e.getClass().getSimpleName() + ": " + message);
			publish("Stopped reading Dictionary file");
		}
		return null;
	}

	@Override
	protected void process(List<String> chunks){
		resultable.printResultLine(chunks);
	}

}
