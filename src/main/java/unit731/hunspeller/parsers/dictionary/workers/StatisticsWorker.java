package unit731.hunspeller.parsers.dictionary.workers;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.List;
import java.util.Objects;
import javax.swing.SwingWorker;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.TimeWatch;


@AllArgsConstructor
public class StatisticsWorker extends SwingWorker<Void, String>{

	private final AffixParser affParser;
	private final DictionaryParser dicParser;
	private final Resultable resultable;


	@Override
	protected Void doInBackground() throws Exception{
		boolean stopped = false;
		try{
			publish("Opening Dictionary file for statistics extraction: " + affParser.getLanguage() + ".dic");

			TimeWatch watch = TimeWatch.start();

			FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();

			setProgress(0);
			try(BufferedReader br = Files.newBufferedReader(dicParser.getDicFile().toPath(), dicParser.getCharset())){
				String line = br.readLine();
				if(!NumberUtils.isCreatable(line))
					throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

				int lineIndex = 1;
				long readSoFar = line.length();
				long totalSize = dicParser.getDicFile().length();
				while(Objects.nonNull(line = br.readLine())){
					lineIndex ++;
					readSoFar += line.length();

					line = DictionaryParser.cleanLine(line);
					if(!line.isEmpty()){
						DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);
						try{
							List<RuleProductionEntry> productions = dicParser.getWordGenerator().applyRules(dictionaryWord);

							for(RuleProductionEntry production : productions){
								//collect statistics
								String word = production.getWord();
								int length = Normalizer.normalize(word, Normalizer.Form.NFKC).length();
								int syllabes = dicParser.getHyphenator().hyphenate(word).countSyllabes();
								//TODO
//								writer.write(production.getWord());
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

			publish("Statistics extracted successfully (it takes " + watch.toStringMinuteSeconds() + ")");
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

}
