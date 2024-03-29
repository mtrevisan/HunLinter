/**
 * Copyright (c) 2019-2022 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.gui.dialogs;

import io.github.mtrevisan.hunlinter.gui.FontHelper;
import io.github.mtrevisan.hunlinter.gui.GUIHelper;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryStatistics;
import io.github.mtrevisan.hunlinter.parsers.dictionary.Frequency;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.Hyphenation;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenationParser;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.Future;


public class DictionaryStatisticsDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 5762751368059394067L;

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryStatisticsDialog.class);

	private static final String LIST_SEPARATOR = ", ";
	private static final String TAB = "\t";

	private final DictionaryStatistics statistics;

	private final Future<JFileChooser> futureSaveTextFileFileChooser;


	public DictionaryStatisticsDialog(final DictionaryStatistics statistics, final Frame parent){
		super(parent, "Dictionary statistics", false);

		Objects.requireNonNull(statistics, "Statistics cannot be null");

		this.statistics = statistics;

		initComponents();

		try{
			final JPopupMenu popupMenu = new JPopupMenu();
			popupMenu.add(GUIHelper.createPopupCopyMenu(popupMenu, GUIHelper::copyCallback));
			GUIHelper.addPopupMenu(popupMenu, compoundWordsValueLabel, contractedWordsValueLabel, lengthsModeValueLabel,
				longestWordCharactersValueLabel, longestWordSyllabesValueLabel, mostCommonSyllabesValueLabel,
				syllabeLengthsModeValueLabel, totalWordsValueLabel, uniqueWordsValueLabel);
		}
		catch(final IOException ignored){}

		addListenerOnClose();


		futureSaveTextFileFileChooser = JavaHelper.executeFuture(() -> {
			final JFileChooser saveTextFileFileChooser = new JFileChooser();
			saveTextFileFileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
			final File currentDir = new File(".");
			saveTextFileFileChooser.setCurrentDirectory(currentDir);
			return saveTextFileFileChooser;
		});


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
      totalWordsValueLabel = new javax.swing.JLabel();
      uniqueWordsLabel = new javax.swing.JLabel();
      uniqueWordsValueLabel = new javax.swing.JLabel();
      compoundWordsLabel = new javax.swing.JLabel();
      compoundWordsValueLabel = new javax.swing.JLabel();
      contractedWordsLabel = new javax.swing.JLabel();
      contractedWordsValueLabel = new javax.swing.JLabel();
      lengthsModeLabel = new javax.swing.JLabel();
      lengthsModeValueLabel = new javax.swing.JLabel();
      syllabeLengthsModeLabel = new javax.swing.JLabel();
      syllabeLengthsModeValueLabel = new javax.swing.JLabel();
      mostCommonSyllabesLabel = new javax.swing.JLabel();
      mostCommonSyllabesValueLabel = new javax.swing.JLabel();
      longestWordCharactersLabel = new javax.swing.JLabel();
      longestWordCharactersValueLabel = new javax.swing.JLabel();
      longestWordSyllabesLabel = new javax.swing.JLabel();
      longestWordSyllabesValueLabel = new javax.swing.JLabel();
      mainTabbedPane = new javax.swing.JTabbedPane();
      lengthsPanel = createChartPanel("Word length distribution", "Word length", "Frequency");
      syllabesPanel = createChartPanel("Word syllabe distribution", "Word syllabe", "Frequency");
      stressesPanel = createChartPanel("Word stress distribution", "Word stressed syllabe index (from last)", "Frequency");
      exportButton = new javax.swing.JButton();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      totalWordsLabel.setLabelFor(totalWordsValueLabel);
      totalWordsLabel.setText("Total words:");

      totalWordsValueLabel.setText("…");

      uniqueWordsLabel.setLabelFor(uniqueWordsValueLabel);
      uniqueWordsLabel.setText("Unique words:");

      uniqueWordsValueLabel.setText("…");

      compoundWordsLabel.setText("Compound words:");

      compoundWordsValueLabel.setText("…");

      contractedWordsLabel.setText("Contracted words:");

      contractedWordsValueLabel.setText("…");

      lengthsModeLabel.setLabelFor(lengthsModeValueLabel);
      lengthsModeLabel.setText("Mode of words‘ length:");

      lengthsModeValueLabel.setText("…");

      syllabeLengthsModeLabel.setLabelFor(syllabeLengthsModeValueLabel);
      syllabeLengthsModeLabel.setText("Mode of words‘ syllabe:");

      syllabeLengthsModeValueLabel.setText("…");

      mostCommonSyllabesLabel.setLabelFor(mostCommonSyllabesValueLabel);
      mostCommonSyllabesLabel.setText("Most common syllabes:");
      mostCommonSyllabesLabel.setPreferredSize(new java.awt.Dimension(113, 17));

		final Font currentFont = FontHelper.getCurrentFont();

		mostCommonSyllabesValueLabel.setFont(currentFont);
      mostCommonSyllabesValueLabel.setText("…");
      mostCommonSyllabesValueLabel.setPreferredSize(new java.awt.Dimension(9, 17));

      longestWordCharactersLabel.setLabelFor(longestWordCharactersValueLabel);
      longestWordCharactersLabel.setText("Longest word(s) (by characters):");
      longestWordCharactersLabel.setPreferredSize(new java.awt.Dimension(158, 17));

      longestWordCharactersValueLabel.setFont(currentFont);
      longestWordCharactersValueLabel.setText("…");
      longestWordCharactersValueLabel.setPreferredSize(new java.awt.Dimension(9, 17));

      longestWordSyllabesLabel.setLabelFor(longestWordSyllabesValueLabel);
      longestWordSyllabesLabel.setText("Longest word(s) (by syllabes):");
      longestWordSyllabesLabel.setPreferredSize(new java.awt.Dimension(146, 17));
      longestWordSyllabesLabel.setRequestFocusEnabled(false);

      longestWordSyllabesValueLabel.setFont(currentFont);
      longestWordSyllabesValueLabel.setText("…");
      longestWordSyllabesValueLabel.setPreferredSize(new java.awt.Dimension(9, 17));

      final javax.swing.GroupLayout lengthsPanelLayout = new javax.swing.GroupLayout(lengthsPanel);
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

      final javax.swing.GroupLayout syllabesPanelLayout = new javax.swing.GroupLayout(syllabesPanel);
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

      final javax.swing.GroupLayout stressesPanelLayout = new javax.swing.GroupLayout(stressesPanel);
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
      exportButton.addActionListener(this::exportButtonActionPerformed);

      final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(totalWordsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(totalWordsValueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addComponent(mainTabbedPane)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(lengthsModeLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(lengthsModeValueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(syllabeLengthsModeLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(syllabeLengthsModeValueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(mostCommonSyllabesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(mostCommonSyllabesValueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(longestWordSyllabesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(longestWordSyllabesValueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(uniqueWordsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(uniqueWordsValueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(compoundWordsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(compoundWordsValueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(longestWordCharactersLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(longestWordCharactersValueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(contractedWordsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(contractedWordsValueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
               .addComponent(totalWordsValueLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(uniqueWordsLabel)
               .addComponent(uniqueWordsValueLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(compoundWordsLabel)
               .addComponent(compoundWordsValueLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(contractedWordsLabel)
               .addComponent(contractedWordsValueLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(lengthsModeLabel)
               .addComponent(lengthsModeValueLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(syllabeLengthsModeLabel)
               .addComponent(syllabeLengthsModeValueLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(mostCommonSyllabesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(mostCommonSyllabesValueLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(longestWordCharactersLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(longestWordCharactersValueLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(longestWordSyllabesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(longestWordSyllabesValueLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(18, 18, 18)
            .addComponent(mainTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 329, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addComponent(exportButton)
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

   private void exportButtonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportButtonActionPerformed
		final JFileChooser saveTextFileFileChooser = JavaHelper.waitForFuture(futureSaveTextFileFileChooser);
		final int fileChosen = saveTextFileFileChooser.showSaveDialog(this);
		if(fileChosen == JFileChooser.APPROVE_OPTION){
			exportButton.setEnabled(false);

			try{
				final File outputFile = saveTextFileFileChooser.getSelectedFile();
				exportToFile(outputFile);

				FileHelper.browse(outputFile);
			}
			catch(final IOException | InterruptedException e){
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
		final long totalWords = statistics.getTotalInflections();
		if(totalWords > 0){
			fillBaseStatistics();
			if(statistics.hasSyllabeStatistics())
				fillSyllabeStatistics();
			else
				cleanupSyllabeStatistics();

			fillLengthsFrequencies(statistics.getLengthsFrequencies(), totalWords, (ChartPanel)lengthsPanel);

			fillLengthsFrequencies(statistics.getSyllabeLengthsFrequencies(), totalWords, (ChartPanel)syllabesPanel);

			fillLengthsFrequencies(statistics.getStressFromLastFrequencies(), totalWords, (ChartPanel)stressesPanel);
		}
	}

	private void fillBaseStatistics(){
		final long totalWords = statistics.getTotalInflections();
		final int uniqueWords = statistics.getUniqueWords();
		final int contractedWords = statistics.getContractedWords();
		final Frequency<Integer> lengthsFrequencies = statistics.getLengthsFrequencies();
		final int longestWordCharsCount = statistics.getLongestWordCountByCharacters();
		List<String> longestWords = statistics.getLongestWordsByCharacters();
		longestWords = DictionaryStatistics.extractRepresentatives(longestWords, 4);

		final String formattedTotalWords = DictionaryParser.COUNTER_FORMATTER.format(totalWords);
		double x = (double)uniqueWords / totalWords;
		final String formattedUniqueWords = DictionaryParser.COUNTER_FORMATTER.format(uniqueWords)
			+ formatFrequencyVariableDecimals(x);
		x = (double)contractedWords / totalWords;
		final String formattedContractedWords = DictionaryParser.COUNTER_FORMATTER.format(contractedWords)
			+ formatFrequencyVariableDecimals(x);
		final StringJoiner formattedLengthsMode = new StringJoiner(LIST_SEPARATOR);
		for(final Integer integer : lengthsFrequencies.getMode())
			formattedLengthsMode.add(String.valueOf(integer));
		final String formattedLongestWords = StringUtils.join(longestWords, LIST_SEPARATOR)
			+ " (" + longestWordCharsCount + ")";

		totalWordsValueLabel.setText(formattedTotalWords);
		uniqueWordsValueLabel.setText(formattedUniqueWords);
		contractedWordsValueLabel.setText(formattedContractedWords);
		lengthsModeValueLabel.setText(formattedLengthsMode.toString());
		longestWordCharactersValueLabel.setText(formattedLongestWords);
	}

	private void fillSyllabeStatistics(){
		final int compoundWords = statistics.getCompoundWords();
		final int uniqueWords = statistics.getUniqueWords();
		final Frequency<Integer> syllabeLengthsFrequencies = statistics.getSyllabeLengthsFrequencies();
		final List<String> mostCommonSyllabes = statistics.getMostCommonSyllabes(7);
		List<String> longestWordSyllabes = new ArrayList<>(0);
		for(final Hyphenation hyphenation : statistics.getLongestWordsBySyllabes())
			longestWordSyllabes.add(StringUtils.join(hyphenation.getSyllabes(), HyphenationParser.SOFT_HYPHEN));
		longestWordSyllabes = DictionaryStatistics.extractRepresentatives(longestWordSyllabes, 4);
		final int longestWordSyllabesCount = statistics.getLongestWordCountBySyllabes();

		final double x = (double)compoundWords / uniqueWords;
		final String formattedCompoundWords = DictionaryParser.COUNTER_FORMATTER.format(compoundWords)
			+ formatFrequencyVariableDecimals(x);
		final StringJoiner formattedSyllabeLengthsMode = new StringJoiner(LIST_SEPARATOR);
		for(final Integer integer : syllabeLengthsFrequencies.getMode())
			formattedSyllabeLengthsMode.add(String.valueOf(integer));
		final String formattedMostCommonSyllabes = StringUtils.join(mostCommonSyllabes, LIST_SEPARATOR);
		final String formattedLongestWordSyllabes = StringUtils.join(longestWordSyllabes, LIST_SEPARATOR)
			+ " (" + longestWordSyllabesCount + ")";

		compoundWordsValueLabel.setText(formattedCompoundWords);
		syllabeLengthsModeValueLabel.setText(formattedSyllabeLengthsMode.toString());
		mostCommonSyllabesValueLabel.setText(formattedMostCommonSyllabes);
		longestWordSyllabesValueLabel.setText(formattedLongestWordSyllabes);

		compoundWordsLabel.setEnabled(true);
		compoundWordsValueLabel.setEnabled(true);
		syllabeLengthsModeLabel.setEnabled(true);
		syllabeLengthsModeValueLabel.setEnabled(true);
		mostCommonSyllabesLabel.setEnabled(true);
		mostCommonSyllabesValueLabel.setEnabled(true);
		longestWordSyllabesLabel.setEnabled(true);
		longestWordSyllabesValueLabel.setEnabled(true);
	}

	@SuppressWarnings("StringConcatenationInFormatCall")
	private static String formatFrequencyVariableDecimals(final double x){
		return String.format(Locale.ROOT, " (%." + Frequency.getDecimals(x) + "f%%)", x * 100.);
	}

	private void cleanupSyllabeStatistics(){
		compoundWordsValueLabel.setText(null);
		syllabeLengthsModeValueLabel.setText(null);
		mostCommonSyllabesValueLabel.setText(null);
		longestWordSyllabesValueLabel.setText(null);

		compoundWordsLabel.setEnabled(false);
		compoundWordsValueLabel.setEnabled(false);
		syllabeLengthsModeLabel.setEnabled(false);
		syllabeLengthsModeValueLabel.setEnabled(false);
		mostCommonSyllabesLabel.setEnabled(false);
		mostCommonSyllabesValueLabel.setEnabled(false);
		longestWordSyllabesLabel.setEnabled(false);
		longestWordSyllabesValueLabel.setEnabled(false);
	}

	private void fillLengthsFrequencies(final Frequency<Integer> frequencies, final long totalSamples, final ChartPanel panel){
		final boolean hasData = frequencies.entrySetIterator().hasNext();

		final int index = mainTabbedPane.indexOfComponent(panel);
		mainTabbedPane.setEnabledAt(index, hasData);
		if(hasData){
			//extract data set
			final XYSeries series = new XYSeries("frequencies");
			final Iterator<Map.Entry<Integer, Long>> itr = frequencies.entrySetIterator();
			while(itr.hasNext()){
				final Map.Entry<Integer, Long> elem = itr.next();
				series.add(elem.getKey().doubleValue(), elem.getValue().doubleValue() / totalSamples);
			}
			final XYDataset dataset = new XYSeriesCollection(series);

			panel.getChart().getXYPlot().setDataset(dataset);
		}
	}

	private static JPanel createChartPanel(final String title, final String xAxisTitle, final String yAxisTitle){
		final JFreeChart chart = createChart(title, xAxisTitle, yAxisTitle);
		return new MyChartPanel(chart);
	}

	private static JFreeChart createChart(final String title, final String xAxisTitle, final String yAxisTitle){
		final XYPlot plot = createChartPlot(xAxisTitle, yAxisTitle);
		return new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
	}

	private static XYPlot createChartPlot(final String xAxisTitle, final String yAxisTitle){
		final XYBarRenderer renderer = createChartRenderer();
		final NumberAxis xAxis = createChartXAxis(xAxisTitle);
		final NumberAxis yAxis = createChartYAxis(yAxisTitle);

		final XYPlot plot = new XYPlot(null, xAxis, yAxis, renderer);
		plot.setOrientation(PlotOrientation.VERTICAL);
		//background color
		plot.setBackgroundPaint(Color.WHITE);
		//gridlines
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinesVisible(true);
		plot.setRangeGridlinePaint(Color.BLACK);
		return plot;
	}

	private static XYBarRenderer createChartRenderer(){
		final XYBarRenderer renderer = new XYBarRenderer();
		renderer.setSeriesStroke(0, new BasicStroke(1.f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
			1.f, new float[]{10.f, 6.f}, 0.f));
		//bar color
		renderer.setSeriesPaint(0, Color.BLUE);
		//solid bar color
		renderer.setBarPainter(new StandardXYBarPainter());
		//shadow
		renderer.setShadowVisible(false);
		//tooltip
		final XYToolTipGenerator xyToolTipGenerator = createChartTooltip();
		renderer.setDefaultToolTipGenerator(xyToolTipGenerator);
		return renderer;
	}

	private static XYToolTipGenerator createChartTooltip(){
		return (dataset, series, item) -> {
			final Number x = dataset.getX(series, item);
			final Number y = dataset.getY(series, item);
			return String.format(Locale.ROOT, "(%d, %.1f%%)", x.intValue(), y.doubleValue() * 100.);
		};
	}

	private static NumberAxis createChartXAxis(final String xAxisTitle){
		//x-axis as integer starting from zero
		final NumberAxis xAxis = new NumberAxis(xAxisTitle);
		xAxis.setAutoRangeIncludesZero(false);
		xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		xAxis.setLowerBound(0.);
		xAxis.setAutoRange(true);
		return xAxis;
	}

	private static NumberAxis createChartYAxis(final String yAxisTitle){
		//y-axis as percent starting from zero
		final NumberAxis yAxis = new NumberAxis(yAxisTitle);
		yAxis.setAutoRangeIncludesZero(true);
		yAxis.setNumberFormatOverride(new DecimalFormat("#%"));
		yAxis.setLowerBound(0.);
		yAxis.setAutoRange(true);
		return yAxis;
	}

	private void exportToFile(final File outputFile) throws IOException{
		try(final BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)){
			final boolean hasSyllabeStatistics = syllabeLengthsModeLabel.isEnabled();

			writer.write(totalWordsLabel.getText() + TAB
				+ StringUtils.replaceChars(totalWordsValueLabel.getText(), DictionaryParser.COUNTER_GROUPING_SEPARATOR, ' '));
			writer.newLine();
			writer.write(uniqueWordsLabel.getText() + TAB
				+ StringUtils.replaceChars(uniqueWordsValueLabel.getText(), DictionaryParser.COUNTER_GROUPING_SEPARATOR, ' '));
			writer.newLine();
			writer.write(compoundWordsLabel.getText() + TAB
				+ StringUtils.replaceChars(compoundWordsValueLabel.getText(), DictionaryParser.COUNTER_GROUPING_SEPARATOR, ' '));
			writer.newLine();
			writer.write(contractedWordsLabel.getText() + TAB
				+ StringUtils.replaceChars(contractedWordsValueLabel.getText(), DictionaryParser.COUNTER_GROUPING_SEPARATOR, ' '));
			writer.newLine();
			writer.write(lengthsModeLabel.getText() + TAB
				+ lengthsModeValueLabel.getText());
			writer.newLine();
			if(hasSyllabeStatistics){
				writer.write(syllabeLengthsModeLabel.getText() + TAB
					+ syllabeLengthsModeValueLabel.getText());
				writer.newLine();
				writer.write(mostCommonSyllabesLabel.getText() + TAB
					+ mostCommonSyllabesValueLabel.getText());
				writer.newLine();
			}
			writer.write(longestWordCharactersLabel.getText() + TAB
				+ longestWordCharactersValueLabel.getText());
			writer.newLine();
			if(hasSyllabeStatistics){
				writer.write(longestWordSyllabesLabel.getText() + TAB
					+ longestWordSyllabesValueLabel.getText());
				writer.newLine();
			}

			exportGraph(writer, lengthsPanel);
			exportGraph(writer, syllabesPanel);
			exportGraph(writer, stressesPanel);
		}
	}

	@SuppressWarnings("StringConcatenationInFormatCall")
	private void exportGraph(final BufferedWriter writer, final Component comp) throws IOException{
		final int index = mainTabbedPane.indexOfComponent(comp);
		final boolean hasData = mainTabbedPane.isEnabledAt(index);
		if(hasData){
			final String name = mainTabbedPane.getTitleAt(index);
			final XYDataset dataset = ((ChartPanel) comp).getChart().getXYPlot().getDataset(0);
			final Iterator<?> xItr = ((XYSeriesCollection)dataset).getSeries(0).getItems().iterator();
			writer.newLine();
			writer.write(name);
			writer.newLine();
			while(xItr.hasNext()){
				final XYDataItem xy = (XYDataItem)xItr.next();
				final double y = xy.getY().doubleValue();
				final int decimals = Frequency.getDecimals(y);
				final String line = String.format(Locale.ROOT, "%d:\t%." + decimals + "f%%", xy.getX().intValue(), y * 100.);
				writer.write(line);
				writer.newLine();
			}
		}
	}

	private static final class MyChartPanel extends ChartPanel{
		private MyChartPanel(final JFreeChart chart){
			super(chart);
		}

		@Override
		protected JPopupMenu createPopupMenu(final boolean properties, final boolean copy, final boolean save, final boolean print,
				final boolean zoom){
			final JPopupMenu result = new JPopupMenu("Chart:");
			final JMenuItem propertiesItem = new JMenuItem("Properties…");
			propertiesItem.setActionCommand("PROPERTIES");
			propertiesItem.addActionListener(this);
			result.add(propertiesItem);

			result.addSeparator();

			final JMenuItem saveAsPNGItem = new JMenuItem("Save as PNG…");
			saveAsPNGItem.setActionCommand("SAVE_AS_PNG");
			saveAsPNGItem.addActionListener(this);
			result.add(saveAsPNGItem);

			result.addSeparator();

			final JMenuItem printItem = new JMenuItem("Print…");
			printItem.setActionCommand("PRINT");
			printItem.addActionListener(this);
			result.add(printItem);

			return result;
		}
	}


	@SuppressWarnings("unused")
	@Serial
	private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	@Serial
	private void readObject(final ObjectInputStream is) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}


   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JLabel compoundWordsLabel;
   private javax.swing.JLabel compoundWordsValueLabel;
   private javax.swing.JLabel contractedWordsLabel;
   private javax.swing.JLabel contractedWordsValueLabel;
   private javax.swing.JButton exportButton;
   private javax.swing.JLabel lengthsModeLabel;
   private javax.swing.JLabel lengthsModeValueLabel;
   private javax.swing.JPanel lengthsPanel;
   private javax.swing.JLabel longestWordCharactersLabel;
   private javax.swing.JLabel longestWordCharactersValueLabel;
   private javax.swing.JLabel longestWordSyllabesLabel;
   private javax.swing.JLabel longestWordSyllabesValueLabel;
   private javax.swing.JTabbedPane mainTabbedPane;
   private javax.swing.JLabel mostCommonSyllabesLabel;
   private javax.swing.JLabel mostCommonSyllabesValueLabel;
   private javax.swing.JPanel stressesPanel;
   private javax.swing.JLabel syllabeLengthsModeLabel;
   private javax.swing.JLabel syllabeLengthsModeValueLabel;
   private javax.swing.JPanel syllabesPanel;
   private javax.swing.JLabel totalWordsLabel;
   private javax.swing.JLabel totalWordsValueLabel;
   private javax.swing.JLabel uniqueWordsLabel;
   private javax.swing.JLabel uniqueWordsValueLabel;
   // End of variables declaration//GEN-END:variables
}
