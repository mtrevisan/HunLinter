package unit731.hunspeller.parsers.dictionary.workers;

import java.awt.Frame;
import java.io.File;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.DictionaryStatisticsDialog;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryStatistics;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.hyphenators.HyphenatorInterface;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.TimeWatch;


@Slf4j
public class StatisticsWorker extends SwingWorker<Void, String>{

	@Getter
	private final boolean performHyphenationStatistics;
	private final AffixParser affParser;
	private final DictionaryParser dicParser;
	private final Frame parent;

	private final DictionaryStatistics dicStatistics;


	public StatisticsWorker(boolean performHyphenationStatistics, AffixParser affParser, DictionaryParser dicParser, Frame parent){
		Objects.requireNonNull(affParser);
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(parent);

		this.performHyphenationStatistics = performHyphenationStatistics;
		this.affParser = affParser;
		this.dicParser = dicParser;
		this.parent = parent;

		dicStatistics = new DictionaryStatistics(dicParser);
	}

	@Override
	protected Void doInBackground() throws Exception{
		boolean stopped = false;
		try{
			publish("Opening Dictionary file for statistics extraction: " + affParser.getLanguage() + ".dic");

			TimeWatch watch = TimeWatch.start();

			FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
			WordGenerator wordGenerator = dicParser.getWordGenerator();
			HyphenatorInterface hyphenator = dicParser.getHyphenator();

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

							for(RuleProductionEntry production : productions){
								//collect statistics
								String word = production.getWord();
								if(performHyphenationStatistics){
									List<String> subwords = hyphenator.splitIntoCompounds(word);
									if(subwords.isEmpty())
										dicStatistics.addData(word);
									else
										for(String subword : subwords){
											Hyphenation hyph = hyphenator.hyphenate(dicStatistics.getOrthography().markDefaultStress(subword));
											dicStatistics.addData(word, hyph);
										}
								}
								else
									dicStatistics.addData(word);
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

	@Override
	protected void done(){
		if(!isCancelled()){
			try{
				//show statistics window
				DictionaryStatisticsDialog dialog = new DictionaryStatisticsDialog(dicStatistics, parent);
				dialog.setLocationRelativeTo(parent);
				dialog.setVisible(true);
			}
			catch(InterruptedException | InvocationTargetException e){
				Logger.getLogger(StatisticsWorker.class.getName()).log(Level.SEVERE, null, e);
			}
		}
	}

}
