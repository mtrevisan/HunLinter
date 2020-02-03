package unit731.hunlinter;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.googlecode.charts4j.AxisLabels;
import com.googlecode.charts4j.AxisLabelsFactory;
import com.googlecode.charts4j.AxisStyle;
import com.googlecode.charts4j.AxisTextAlignment;
import com.googlecode.charts4j.BarChart;
import com.googlecode.charts4j.BarChartPlot;
import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.Data;
import com.googlecode.charts4j.DataUtil;
import com.googlecode.charts4j.Fills;
import com.googlecode.charts4j.GChart;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.Plots;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.DictionaryStatistics;
import unit731.hunlinter.parsers.dictionary.Frequency;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;
import unit731.hunlinter.parsers.hyphenation.Hyphenation;


public class DictionaryStatisticsDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryStatisticsDialog.class);

	private static final long serialVersionUID = 5762751368059394067l;

	private static final String SERIES_NAME = "series";
	private static final String LIST_SEPARATOR = ", ";
	private static final String TAB = "\t";

	private final DictionaryStatistics statistics;

	private final JFileChooser saveTextFileFileChooser;


	public DictionaryStatisticsDialog(final DictionaryStatistics statistics, final Frame parent){
		super(parent, "Dictionary statistics", false);

		Objects.requireNonNull(statistics);
		Objects.requireNonNull(parent);

		this.statistics = statistics;

		initComponents();

		final Font currentFont = GUIUtils.getCurrentFont();
		mostCommonSyllabesOutputLabel.setFont(currentFont);
		longestWordCharactersOutputLabel.setFont(currentFont);
		longestWordSyllabesOutputLabel.setFont(currentFont);

		try{
			final JPopupMenu popupMenu = new JPopupMenu();
			popupMenu.add(GUIUtils.createPopupCopyMenu(compoundWordsOutputLabel.getHeight(), popupMenu, GUIUtils::copyCallback));
			GUIUtils.addPopupMenu(popupMenu, compoundWordsOutputLabel, contractedWordsOutputLabel, lengthsModeOutputLabel, longestWordCharactersOutputLabel,
				longestWordSyllabesOutputLabel, mostCommonSyllabesOutputLabel, syllabeLengthsModeOutputLabel, totalWordsOutputLabel, uniqueWordsOutputLabel);
		}
		catch(final IOException ignored){}

		addListenerOnClose();

		saveTextFileFileChooser = new JFileChooser();
		saveTextFileFileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
		final File currentDir = new File(".");
		saveTextFileFileChooser.setCurrentDirectory(currentDir);


		fillStatisticData();
	}

	/**
	 * This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      totalWordsLabel = new javax.swing.JLabel();
      totalWordsOutputLabel = new javax.swing.JLabel();
      uniqueWordsLabel = new javax.swing.JLabel();
      uniqueWordsOutputLabel = new javax.swing.JLabel();
      compoundWordsLabel = new javax.swing.JLabel();
      compoundWordsOutputLabel = new javax.swing.JLabel();
      contractedWordsLabel = new javax.swing.JLabel();
      contractedWordsOutputLabel = new javax.swing.JLabel();
      lengthsModeLabel = new javax.swing.JLabel();
      lengthsModeOutputLabel = new javax.swing.JLabel();
      syllabeLengthsModeLabel = new javax.swing.JLabel();
      syllabeLengthsModeOutputLabel = new javax.swing.JLabel();
      mostCommonSyllabesLabel = new javax.swing.JLabel();
      mostCommonSyllabesOutputLabel = new javax.swing.JLabel();
      longestWordCharactersLabel = new javax.swing.JLabel();
      longestWordCharactersOutputLabel = new javax.swing.JLabel();
      longestWordSyllabesLabel = new javax.swing.JLabel();
      longestWordSyllabesOutputLabel = new javax.swing.JLabel();
      mainTabbedPane = new javax.swing.JTabbedPane();
      lengthsPanel = createChartPanel("Word length distribution", "Word length", "Frequency");
      syllabesPanel = createChartPanel("Word syllabe distribution", "Word syllabe", "Frequency");
      stressesPanel = createChartPanel("Word stress distribution", "Word stressed syllabe index (from last)", "Frequency");
      exportButton = new javax.swing.JButton();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      totalWordsLabel.setLabelFor(totalWordsOutputLabel);
      totalWordsLabel.setText("Total words:");

      totalWordsOutputLabel.setText("…");

      uniqueWordsLabel.setLabelFor(uniqueWordsOutputLabel);
      uniqueWordsLabel.setText("Unique words:");

      uniqueWordsOutputLabel.setText("…");

      compoundWordsLabel.setText("Compound words:");

      compoundWordsOutputLabel.setText("…");

      contractedWordsLabel.setText("Contracted words:");

      contractedWordsOutputLabel.setText("…");

      lengthsModeLabel.setLabelFor(lengthsModeOutputLabel);
      lengthsModeLabel.setText("Mode of wordsʼ length:");

      lengthsModeOutputLabel.setText("…");

      syllabeLengthsModeLabel.setLabelFor(syllabeLengthsModeOutputLabel);
      syllabeLengthsModeLabel.setText("Mode of wordsʼ syllabe:");

      syllabeLengthsModeOutputLabel.setText("…");

      mostCommonSyllabesLabel.setLabelFor(mostCommonSyllabesOutputLabel);
      mostCommonSyllabesLabel.setText("Most common syllabes:");

      mostCommonSyllabesOutputLabel.setText("…");

      longestWordCharactersLabel.setLabelFor(longestWordCharactersOutputLabel);
      longestWordCharactersLabel.setText("Longest word(s) (by characters):");

      longestWordCharactersOutputLabel.setText("…");

      longestWordSyllabesLabel.setLabelFor(longestWordSyllabesOutputLabel);
      longestWordSyllabesLabel.setText("Longest word(s) (by syllabes):");

      longestWordSyllabesOutputLabel.setText("…");

      javax.swing.GroupLayout lengthsPanelLayout = new javax.swing.GroupLayout(lengthsPanel);
      lengthsPanel.setLayout(lengthsPanelLayout);
      lengthsPanelLayout.setHorizontalGroup(
         lengthsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 655, Short.MAX_VALUE)
      );
      lengthsPanelLayout.setVerticalGroup(
         lengthsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 301, Short.MAX_VALUE)
      );

      mainTabbedPane.addTab("Word lengths", lengthsPanel);

      javax.swing.GroupLayout syllabesPanelLayout = new javax.swing.GroupLayout(syllabesPanel);
      syllabesPanel.setLayout(syllabesPanelLayout);
      syllabesPanelLayout.setHorizontalGroup(
         syllabesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 655, Short.MAX_VALUE)
      );
      syllabesPanelLayout.setVerticalGroup(
         syllabesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 301, Short.MAX_VALUE)
      );

      mainTabbedPane.addTab("Word syllabes", syllabesPanel);

      javax.swing.GroupLayout stressesPanelLayout = new javax.swing.GroupLayout(stressesPanel);
      stressesPanel.setLayout(stressesPanelLayout);
      stressesPanelLayout.setHorizontalGroup(
         stressesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 655, Short.MAX_VALUE)
      );
      stressesPanelLayout.setVerticalGroup(
         stressesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 301, Short.MAX_VALUE)
      );

      mainTabbedPane.addTab("Word stresses", stressesPanel);

      exportButton.setText("Export");
      exportButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            exportButtonActionPerformed(evt);
         }
      });

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(totalWordsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(totalWordsOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addComponent(mainTabbedPane)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(lengthsModeLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(lengthsModeOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(syllabeLengthsModeLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(syllabeLengthsModeOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(mostCommonSyllabesLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(mostCommonSyllabesOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(longestWordSyllabesLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(longestWordSyllabesOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(uniqueWordsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(uniqueWordsOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(compoundWordsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(compoundWordsOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(longestWordCharactersLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(longestWordCharactersOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(contractedWordsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(contractedWordsOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addGap(0, 0, Short.MAX_VALUE)
                  .addComponent(exportButton)
                  .addGap(0, 0, Short.MAX_VALUE)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(totalWordsLabel)
               .addComponent(totalWordsOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(uniqueWordsLabel)
               .addComponent(uniqueWordsOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(compoundWordsLabel)
               .addComponent(compoundWordsOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(contractedWordsLabel)
               .addComponent(contractedWordsOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(lengthsModeLabel)
               .addComponent(lengthsModeOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(syllabeLengthsModeLabel)
               .addComponent(syllabeLengthsModeOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(mostCommonSyllabesLabel)
               .addComponent(mostCommonSyllabesOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(longestWordCharactersLabel)
               .addComponent(longestWordCharactersOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(longestWordSyllabesLabel)
               .addComponent(longestWordSyllabesOutputLabel))
            .addGap(18, 18, 18)
            .addComponent(mainTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 329, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, Short.MAX_VALUE)
            .addComponent(exportButton)
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

   private void exportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportButtonActionPerformed
		final int fileChosen = saveTextFileFileChooser.showSaveDialog(this);
		if(fileChosen == JFileChooser.APPROVE_OPTION){
			exportButton.setEnabled(false);

			try{
				final File outputFile = saveTextFileFileChooser.getSelectedFile();
				exportToFile(outputFile);
			}
			catch(final Exception e){
				LOGGER.error("Cannot export statistics", e);
			}

			exportButton.setEnabled(true);
		}
   }//GEN-LAST:event_exportButtonActionPerformed

	private void addListenerOnClose(){
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowDeactivated(final WindowEvent e){
				statistics.clear();
			}
		});
	}

	private void fillStatisticData(){
		final long totalWords = statistics.getTotalProductions();
		if(totalWords > 0){
			fillBaseStatistics();
			if(statistics.hasSyllabeStatistics())
				fillSyllabeStatistics();
			else
				cleanupSyllabeStatistics();

			fillLengthsFrequencies(statistics.getLengthsFrequencies(), totalWords, lengthsPanel);

			fillLengthsFrequencies(statistics.getSyllabeLengthsFrequencies(), totalWords, syllabesPanel);

			fillLengthsFrequencies(statistics.getStressFromLastFrequencies(), totalWords, stressesPanel);
		}
	}

	private void fillBaseStatistics(){
		final long totalWords = statistics.getTotalProductions();
		final int uniqueWords = statistics.getUniqueWords();
		final int contractedWords = statistics.getContractedWords();
		final Frequency<Integer> lengthsFrequencies = statistics.getLengthsFrequencies();
		final int longestWordCharsCount = statistics.getLongestWordCountByCharacters();
		List<String> longestWords = statistics.getLongestWordsByCharacters();
		longestWords = DictionaryStatistics.extractRepresentatives(longestWords, 4);

		final String formattedTotalWords = DictionaryParser.COUNTER_FORMATTER.format(totalWords);
		final String formattedUniqueWords = DictionaryParser.COUNTER_FORMATTER.format(uniqueWords)
			+ " (" + DictionaryParser.PERCENT_FORMATTER_1.format((double)uniqueWords / totalWords) + ")";
		final String formattedContractedWords = DictionaryParser.COUNTER_FORMATTER.format(contractedWords)
			+ " (" + DictionaryParser.PERCENT_FORMATTER_1.format((double)contractedWords / uniqueWords) + ")";
		final String formattedLengthsMode = lengthsFrequencies.getMode().stream().map(String::valueOf).collect(Collectors.joining(LIST_SEPARATOR));
		final String formattedLongestWords = StringUtils.join(longestWords, LIST_SEPARATOR)
			+ " (" + longestWordCharsCount + ")";

		totalWordsOutputLabel.setText(formattedTotalWords);
		uniqueWordsOutputLabel.setText(formattedUniqueWords);
		contractedWordsOutputLabel.setText(formattedContractedWords);
		lengthsModeOutputLabel.setText(formattedLengthsMode);
		longestWordCharactersOutputLabel.setText(formattedLongestWords);
	}

	private void fillSyllabeStatistics(){
		final int compoundWords = statistics.getCompoundWords();
		final int uniqueWords = statistics.getUniqueWords();
		final Frequency<Integer> syllabeLengthsFrequencies = statistics.getSyllabeLengthsFrequencies();
		final List<String> mostCommonSyllabes = statistics.getMostCommonSyllabes(7);
		List<String> longestWordSyllabes = statistics.getLongestWordsBySyllabes().stream()
			.map(Hyphenation::getSyllabes)
			.map(syllabes -> StringUtils.join(syllabes, HyphenationParser.SOFT_HYPHEN))
			.collect(Collectors.toList());
		longestWordSyllabes = DictionaryStatistics.extractRepresentatives(longestWordSyllabes, 4);
		final int longestWordSyllabesCount = statistics.getLongestWordCountBySyllabes();

		final String formattedCompoundWords = DictionaryParser.COUNTER_FORMATTER.format(compoundWords)
			+ " (" + DictionaryParser.PERCENT_FORMATTER_1.format((double)compoundWords / uniqueWords) + ")";
		final String formattedSyllabeLengthsMode = syllabeLengthsFrequencies.getMode().stream()
			.map(String::valueOf)
			.collect(Collectors.joining(LIST_SEPARATOR));
		final String formattedMostCommonSyllabes = StringUtils.join(mostCommonSyllabes, LIST_SEPARATOR);
		final String formattedLongestWordSyllabes = StringUtils.join(longestWordSyllabes, LIST_SEPARATOR)
			+ " (" + longestWordSyllabesCount + ")";

		compoundWordsOutputLabel.setText(formattedCompoundWords);
		syllabeLengthsModeOutputLabel.setText(formattedSyllabeLengthsMode);
		mostCommonSyllabesOutputLabel.setText(formattedMostCommonSyllabes);
		longestWordSyllabesOutputLabel.setText(formattedLongestWordSyllabes);

		compoundWordsLabel.setEnabled(true);
		compoundWordsOutputLabel.setEnabled(true);
		syllabeLengthsModeLabel.setEnabled(true);
		syllabeLengthsModeOutputLabel.setEnabled(true);
		mostCommonSyllabesLabel.setEnabled(true);
		mostCommonSyllabesOutputLabel.setEnabled(true);
		longestWordSyllabesLabel.setEnabled(true);
		longestWordSyllabesOutputLabel.setEnabled(true);
	}

	private void cleanupSyllabeStatistics(){
		compoundWordsOutputLabel.setText(StringUtils.EMPTY);
		syllabeLengthsModeOutputLabel.setText(StringUtils.EMPTY);
		mostCommonSyllabesOutputLabel.setText(StringUtils.EMPTY);
		longestWordSyllabesOutputLabel.setText(StringUtils.EMPTY);

		compoundWordsLabel.setEnabled(false);
		compoundWordsOutputLabel.setEnabled(false);
		syllabeLengthsModeLabel.setEnabled(false);
		syllabeLengthsModeOutputLabel.setEnabled(false);
		mostCommonSyllabesLabel.setEnabled(false);
		mostCommonSyllabesOutputLabel.setEnabled(false);
		longestWordSyllabesLabel.setEnabled(false);
		longestWordSyllabesOutputLabel.setEnabled(false);
	}

	private void fillLengthsFrequencies(final Frequency<Integer> frequencies, final long totalSamples, final JPanel panel){
		final boolean hasData = frequencies.entrySetIterator().hasNext();

		mainTabbedPane.setEnabledAt(mainTabbedPane.indexOfComponent(panel), hasData);
		if(hasData){
			final List<String> xData = new ArrayList<>();
			final List<Double> yData = new ArrayList<>();
			final Iterator<Map.Entry<Integer, Long>> itr = frequencies.entrySetIterator();
			while(itr.hasNext()){
				final Map.Entry<Integer, Long> elem = itr.next();
				xData.add(Integer.toString(elem.getKey()));
				yData.add(elem.getValue().doubleValue() / totalSamples);
			}

			final GChart chartFactions = buildData(xData, yData);
			final JLabel graphFactions = new JLabel(new ImageIcon(ImageIO.read(new URL(chartFactions.toURLString()))));

			//---

			final Data data = Data.newData(yData);
			final BarChartPlot plot = Plots.newBarChartPlot(data, Color.BLUE);
			final BarChart chart = GCharts.newBarChart(plot);
			final AxisStyle axisStyle = AxisStyle.newAxisStyle(Color.BLACK, 13, AxisTextAlignment.CENTER);
			final AxisLabels score = AxisLabelsFactory.newAxisLabels("Score", 50.0);
			score.setAxisStyle(axisStyle);
			final AxisLabels year = AxisLabelsFactory.newAxisLabels("Year", 50.0);
			year.setAxisStyle(axisStyle);

			chart.addXAxisLabels(AxisLabelsFactory.newAxisLabels(xData));
			chart.addYAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, 100));
			chart.addYAxisLabels(score);
			chart.addXAxisLabels(year);

			chart.setSize(600, 450);
			chart.setBarWidth(100);
			chart.setSpaceWithinGroupsOfBars(0);
			chart.setDataStacked(true);
			chart.setTitle("Team Scores", Color.BLACK, 16);
		}
	}

	//https://github.com/SR-G/theadmiral/blob/master/src/main/java/net/coljac/pirates/gui/StatisticsPanel.java
	private JPanel createChartPanel(final String title, final String xAxisTitle, final String yAxisTitle){
		final Data data = Data.newData(yData);
		final BarChartPlot plot = Plots.newBarChartPlot(data, Color.BLUE);
		final BarChart chart = GCharts.newBarChart(plot);
		final AxisStyle axisStyle = AxisStyle.newAxisStyle(Color.BLACK, 13, AxisTextAlignment.CENTER);
		final AxisLabels xAxis = AxisLabelsFactory.newNumericRangeAxisLabels(0., 100.);
		xAxis.setAxisStyle(axisStyle);
		chart.addXAxisLabels(xAxis);
		final AxisLabels yAxis = AxisLabelsFactory.newNumericRangeAxisLabels(0., 100.);
		yAxis.setAxisStyle(axisStyle);
		chart.addYAxisLabels(yAxis);

		chart.setSize(600, 450);
//		chart.setBarWidth(100);
		chart.setSpaceWithinGroupsOfBars(0);
		chart.setDataStacked(true);
		chart.setTitle(title, Color.BLACK, 16);
		return chart;


//		final CategoryStyler styler = chart.getStyler();
//		styler.setAvailableSpaceFill(0.98);
//		styler.setOverlapped(true);
//		styler.setLegendVisible(false);
//		styler.setXAxisMin(0.);
//		styler.setYAxisMin(0.);
//		styler.setYAxisDecimalPattern("#%");
//		styler.setYAxisTitleVisible(false);
//		styler.setChartBackgroundColor(getBackground());
//		styler.setToolTipsEnabled(true);
//
//		return new XChartPanel<>(chart);
	}

	private GChart buildData(final List<String> labels, final List<Number> values){
		final long max = values.stream()
			.mapToLong(v -> v.longValue())
			.max()
			.orElse(0l);

		// final Plot plot = Plots.newPlot(Data.newData(0, 66.6, 33.3, 100));
		final AxisStyle axisStyle = AxisStyle.newAxisStyle(Color.BLACK, 14, AxisTextAlignment.RIGHT);
		final AxisLabels axisValues = AxisLabelsFactory.newNumericRangeAxisLabels(0, max + 1);
		axisValues.setAxisStyle(axisStyle);
		final AxisLabels axisLabels = AxisLabelsFactory.newAxisLabels(labels);
		axisLabels.setAxisStyle(axisStyle);

		final BarChartPlot plot = Plots.newBarChartPlot(DataUtil.scaleWithinRange(0, max + 1, values));
		final BarChart chart = GCharts.newBarChart(plot);
//		chart.setTitle("Stats", BLACK, 16);
//		chart.addHorizontalRangeMarker(40, 60, Color.newColor(RED, 30));
//		chart.setDataStacked(true);
		chart.addXAxisLabels(axisValues);
		chart.addYAxisLabels(axisLabels);
		chart.setHorizontal(true);
		chart.setBarWidth(19);
		chart.setSpaceWithinGroupsOfBars(2);
		chart.setGrid((50. / max) * 20, 600, 3, 2);
		chart.setBackgroundFill(Fills.newSolidFill(Color.LIGHTGREY));
//		final LinearGradientFill fill = Fills.newLinearGradientFill(0, Color.newColor("E37600"), 100);
//		fill.addColorAndOffset(Color.newColor("DC4800"), 0);
//		chart.setAreaFill(fill);
		chart.setSize(700, (values.size() + 1) * 28);
		return chart;
	}

	private void exportToFile(final File outputFile) throws IOException{
		try(final BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)){
			final boolean hasSyllabeStatistics = syllabeLengthsModeLabel.isEnabled();

			writer.write(totalWordsLabel.getText() + TAB + StringUtils.replaceChars(totalWordsOutputLabel.getText(), DictionaryParser.COUNTER_GROUPING_SEPARATOR, ' '));
			writer.newLine();
			writer.write(uniqueWordsLabel.getText() + TAB + StringUtils.replaceChars(uniqueWordsOutputLabel.getText(), DictionaryParser.COUNTER_GROUPING_SEPARATOR, ' '));
			writer.newLine();
			writer.write(compoundWordsLabel.getText() + TAB + StringUtils.replaceChars(compoundWordsOutputLabel.getText(), DictionaryParser.COUNTER_GROUPING_SEPARATOR, ' '));
			writer.newLine();
			writer.write(contractedWordsLabel.getText() + TAB + StringUtils.replaceChars(contractedWordsOutputLabel.getText(), DictionaryParser.COUNTER_GROUPING_SEPARATOR, ' '));
			writer.newLine();
			writer.write(lengthsModeLabel.getText() + TAB + lengthsModeOutputLabel.getText());
			writer.newLine();
			if(hasSyllabeStatistics){
				writer.write(syllabeLengthsModeLabel.getText() + TAB + syllabeLengthsModeOutputLabel.getText());
				writer.newLine();
				writer.write(mostCommonSyllabesLabel.getText() + TAB + mostCommonSyllabesOutputLabel.getText());
				writer.newLine();
			}
			writer.write(longestWordCharactersLabel.getText() + TAB + longestWordCharactersOutputLabel.getText());
			writer.newLine();
			if(hasSyllabeStatistics){
				writer.write(longestWordSyllabesLabel.getText() + TAB + longestWordSyllabesOutputLabel.getText());
				writer.newLine();
			}

			exportGraph(writer, lengthsPanel);
			exportGraph(writer, syllabesPanel);
			exportGraph(writer, stressesPanel);
		}
	}

	private void exportGraph(final BufferedWriter writer, final Component comp) throws IOException{
		final int index = mainTabbedPane.indexOfComponent(comp);
		final boolean hasData = mainTabbedPane.isEnabledAt(index);
		if(hasData){
			final String name = mainTabbedPane.getTitleAt(index);
			final CategorySeries series = ((CategoryChart)((XChartPanel<?>)comp).getChart()).getSeriesMap().get(SERIES_NAME);
			final Iterator<?> xItr = series.getXData().iterator();
			final Iterator<? extends Number> yItr = series.getYData().iterator();
			writer.newLine();
			writer.write(name);
			writer.newLine();
			while(xItr.hasNext()){
				writer.write(xItr.next() + ":" + TAB + DictionaryParser.PERCENT_FORMATTER_1.format(yItr.next()));
				writer.newLine();
			}
		}
	}

	@SuppressWarnings("unused")
	private void writeObject(final ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	private void readObject(final ObjectInputStream is) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}


   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JLabel compoundWordsLabel;
   private javax.swing.JLabel compoundWordsOutputLabel;
   private javax.swing.JLabel contractedWordsLabel;
   private javax.swing.JLabel contractedWordsOutputLabel;
   private javax.swing.JButton exportButton;
   private javax.swing.JLabel lengthsModeLabel;
   private javax.swing.JLabel lengthsModeOutputLabel;
   private javax.swing.JPanel lengthsPanel;
   private javax.swing.JLabel longestWordCharactersLabel;
   private javax.swing.JLabel longestWordCharactersOutputLabel;
   private javax.swing.JLabel longestWordSyllabesLabel;
   private javax.swing.JLabel longestWordSyllabesOutputLabel;
   private javax.swing.JTabbedPane mainTabbedPane;
   private javax.swing.JLabel mostCommonSyllabesLabel;
   private javax.swing.JLabel mostCommonSyllabesOutputLabel;
   private javax.swing.JPanel stressesPanel;
   private javax.swing.JLabel syllabeLengthsModeLabel;
   private javax.swing.JLabel syllabeLengthsModeOutputLabel;
   private javax.swing.JPanel syllabesPanel;
   private javax.swing.JLabel totalWordsLabel;
   private javax.swing.JLabel totalWordsOutputLabel;
   private javax.swing.JLabel uniqueWordsLabel;
   private javax.swing.JLabel uniqueWordsOutputLabel;
   // End of variables declaration//GEN-END:variables

}
