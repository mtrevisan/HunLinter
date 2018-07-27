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
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryStatistics;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.TimeWatch;


@Slf4j
public class StatisticsWorker extends SwingWorker<Void, String>{

	private final Backbone backbone;
	@Getter
	private final boolean performHyphenationStatistics;
	private final Frame parent;

	private final DictionaryStatistics dicStatistics;


	public StatisticsWorker(Backbone backbone, boolean performHyphenationStatistics, Frame parent){
		Objects.requireNonNull(backbone);
		Objects.requireNonNull(parent);

		this.backbone = backbone;
		this.performHyphenationStatistics = performHyphenationStatistics;
		this.parent = parent;

		dicStatistics = new DictionaryStatistics(backbone);
	}

	@Override
	protected Void doInBackground() throws Exception{
		boolean stopped = false;
		try{
			publish("Opening Dictionary file for statistics extraction");

			TimeWatch watch = TimeWatch.start();

			setProgress(0);
			File dicFile = backbone.dicParser.getDicFile();
			long totalSize = dicFile.length();
			try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(dicFile.toPath(), backbone.getCharset()))){
				String line = br.readLine();
				//ignore any BOM marker on first line
				if(br.getLineNumber() == 1)
					line = FileService.clearBOMMarker(line);
				if(!NumberUtils.isCreatable(line))
					throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

				int lineIndex = 1;
				long readSoFar = line.length();
				while((line = br.readLine()) != null){
					lineIndex ++;
					readSoFar += line.length();

					line = DictionaryParser.cleanLine(line);
					if(!line.isEmpty()){
						try{
							List<RuleProductionEntry> productions = backbone.applyRules(line);

							for(RuleProductionEntry production : productions){
								//collect statistics
								String word = production.getWord();
								if(performHyphenationStatistics){
									List<String> subwords = backbone.splitWordIntoCompounds(word);
									if(subwords.isEmpty())
										dicStatistics.addData(word);
									else
										for(String subword : subwords){
											Hyphenation hyph = backbone.hyphenate(dicStatistics.getOrthography().markDefaultStress(subword));
											dicStatistics.addData(word, hyph);
										}
								}
								else
									dicStatistics.addData(word);
							}
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
