package unit731.hunspeller.parsers.dictionary.workers;

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.SwingWorker;
import lombok.NonNull;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math3.stat.Frequency;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.style.CategoryStyler;
import org.knowm.xchart.style.Styler;
import unit731.hunspeller.DictionaryStatisticsDialog;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.dtos.DictionaryStatistics;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
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
								int length = Normalizer.normalize(word, Normalizer.Form.NFKC).length();
								int syllabes = dicParser.getHyphenator().hyphenate(word).countSyllabes();

								dicStatistics.addLengthAndSyllabes(length, syllabes);
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

	@Override
	protected void done(){
		//show statistics window
//		DictionaryStatisticsDialog dialog = new DictionaryStatisticsDialog(dicStatistics, (Frame)resultable);
//		dialog.setLocationRelativeTo((Frame)resultable);
//		dialog.setVisible(true);

		Thread t = new Thread(() -> {
			String wordLengthXAxisTitle = "Word length";
			CategoryChart wordLengthChart = createChart(dicStatistics.getLengthsFrequencies(), "Word length distribution", wordLengthXAxisTitle, "Frequency", 600, 300);
			String wordSyllabeXAxisTitle = "Word syllabe";
			CategoryChart wordSyllabeChart = createChart(dicStatistics.getSyllabesFrequencies(), "Word syllabe distribution", wordSyllabeXAxisTitle, "Frequency", 600, 300);
			
			SwingWrapper<CategoryChart> swingWrapper = new SwingWrapper<>(wordLengthChart);
			swingWrapper.displayChart(wordLengthXAxisTitle);

			swingWrapper = new SwingWrapper<>(wordSyllabeChart);
			swingWrapper.displayChart(wordSyllabeXAxisTitle);
		});
		t.start();
	}

	private CategoryChart createChart(Frequency freqs, String title, String xAxisTitle, String yAxisTitle, int width, int height){
		List<Integer> xData = new ArrayList<>();
		List<Integer> yData = new ArrayList<>();
		Iterator<Map.Entry<Comparable<?>, Long>> itr = freqs.entrySetIterator();
		while(itr.hasNext()){
			Map.Entry<Comparable<?>, Long> elem = itr.next();
			xData.add(((Long)elem.getKey()).intValue());
			yData.add(elem.getValue().intValue());
		}
		return buildChart(xData, yData, title, xAxisTitle, yAxisTitle, width, height);
	}

	private CategoryChart buildChart(List<Integer> xData, List<Integer> yData, String title, String xAxisTitle, String yAxisTitle, int width, int height){
		CategoryChart chart = new CategoryChartBuilder().width(width).height(height)
			.title(title)
			.xAxisTitle(xAxisTitle)
			.yAxisTitle(yAxisTitle)
			.theme(Styler.ChartTheme.Matlab)
			.build();

		CategoryStyler styler = chart.getStyler();
		styler.setAvailableSpaceFill(0.99);
		styler.setOverlapped(true);
		styler.setLegendVisible(false);

		chart.addSeries("series", xData, yData);

		return chart;
	}

}
