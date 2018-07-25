package unit731.hunspeller.parsers.dictionary.workers;

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.DictionaryStatisticsDialog;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryStatistics;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.TimeWatch;


public class WordCountWorker extends SwingWorker<Void, String>{

	@NonNull
	private final AffixParser affParser;
	@NonNull
	private final DictionaryParser dicParser;
	@NonNull
	private final Resultable resultable;

	@Getter
	private int wordCount;


	public WordCountWorker(AffixParser affParser, DictionaryParser dicParser, Resultable resultable){
		if(!(resultable instanceof Frame))
			throw new IllegalArgumentException("The resultable should also be a Frame");

		this.affParser = affParser;
		this.dicParser = dicParser;
		this.resultable = resultable;
	}

	@Override
	protected Void doInBackground() throws Exception{
		boolean stopped = false;
		wordCount = 0;
		try{
			publish("Opening Dictionary file for word count extraction: " + affParser.getLanguage() + ".dic");

			TimeWatch watch = TimeWatch.start();

			FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
			WordGenerator wordGenerator = dicParser.getWordGenerator();

			setProgress(0);
			File dicFile = dicParser.getDicFile();
			long totalSize = dicFile.length();
			try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(dicFile.toPath(), dicParser.getCharset()))){
				String line = br.readLine();
				//ignore any BOM marker on first line
				if(br.getLineNumber() == 1)
					line = FileService.clearBOMMarker(line);
				if(!NumberUtils.isCreatable(line))
					throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

				int lineIndex = 1;
				long readSoFar = line.length();
				while(Objects.nonNull(line = br.readLine())){
					lineIndex ++;
					readSoFar += line.length();

					line = DictionaryParser.cleanLine(line);
					if(!line.isEmpty()){
						DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);
						try{
							List<RuleProductionEntry> productions = wordGenerator.applyRules(dictionaryWord);
							wordCount += productions.size();
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

			publish("Word count extracted successfully (it takes " + watch.toStringMinuteSeconds() + ")");
		}
		catch(IOException | IllegalArgumentException e){
			stopped = true;

			publish(e instanceof ClosedChannelException? "Statistics thread interrupted": e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		catch(Exception e){
			stopped = true;

			String message = ExceptionService.getMessage(e, getClass());
			publish(e.getClass().getSimpleName() + ": " + message);
		}
		if(stopped)
			publish("Stopped reading Dictionary file");

		return null;
	}

	@Override
	protected void process(List<String> chunks){
		resultable.printResultLine(chunks);
	}

	@Override
	protected void done(){
		if(!isCancelled()){
publish(Integer.toString(wordCount));
//			try{
//				//show statistics window
//				DictionaryStatisticsDialog dialog = new DictionaryStatisticsDialog(dicStatistics, (Frame)resultable);
//				dialog.setLocationRelativeTo((Frame)resultable);
//				dialog.setVisible(true);
//			}
//			catch(InterruptedException | InvocationTargetException e){
//				Logger.getLogger(WordCountWorker.class.getName()).log(Level.SEVERE, null, e);
//			}
		}
	}

}
