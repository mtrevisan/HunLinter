package unit731.hunspeller.parsers.dictionary.workers;

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import lombok.NonNull;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.DictionaryStatisticsDialog;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.languages.vec.WordVEC;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.dtos.DictionaryStatistics;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationInterface;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.TimeWatch;


public class StatisticsWorker extends SwingWorker<Void, String>{

	@NonNull
	private final AffixParser affParser;
	@NonNull
	private final DictionaryParser dicParser;
	@NonNull
	private final Resultable resultable;

	private final DictionaryStatistics dicStatistics = new DictionaryStatistics();


	public StatisticsWorker(AffixParser affParser, DictionaryParser dicParser, Resultable resultable){
		if(!(resultable instanceof Frame))
			throw new IllegalArgumentException("The resultable should also be a Frame");

		this.affParser = affParser;
		this.dicParser = dicParser;
		this.resultable = resultable;
	}

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
//FIXME WordVEC
								HyphenationInterface hyph = dicParser.getHyphenator().hyphenate(WordVEC.markDefaultStress(word));
								if(!hyph.hasErrors()){
									int length = Normalizer.normalize(word, Normalizer.Form.NFKC).length();
									int syllabes = hyph.countSyllabes();
									List<Integer> stressIndexFromLast = getStressIndexFromLast(hyph);

									dicStatistics.addLengthAndSyllabeLengthAndStressFromLast(length, syllabes, stressIndexFromLast.get(stressIndexFromLast.size() - 1));
									dicStatistics.addSyllabes(hyph.getSyllabes());
									dicStatistics.storeLongestWord(word);
								}
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

	private List<Integer> getStressIndexFromLast(HyphenationInterface hyph){
		List<String> syllabes = hyph.getSyllabes();
		int size = syllabes.size() - 1;
		for(int i = 0; i <= size; i ++)
//FIXME WordVEC
			if(WordVEC.isStressed(syllabes.get(size - i)))
				return Arrays.asList(i);
		return null;
	}

	@Override
	protected void process(List<String> chunks){
		resultable.printResultLine(chunks);
	}

	@Override
	protected void done(){
		try{
			//show statistics window
			DictionaryStatisticsDialog dialog = new DictionaryStatisticsDialog(dicStatistics, (Frame)resultable);
			dialog.setLocationRelativeTo((Frame)resultable);
			dialog.setVisible(true);
		}
		catch(InterruptedException | InvocationTargetException e){
			Logger.getLogger(StatisticsWorker.class.getName()).log(Level.SEVERE, null, e);
		}
	}

}
