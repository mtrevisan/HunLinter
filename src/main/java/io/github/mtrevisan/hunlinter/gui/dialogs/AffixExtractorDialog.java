/**
 * Copyright (c) 2025 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
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

import io.github.mtrevisan.hunlinter.MainFrame;
import io.github.mtrevisan.hunlinter.gui.FontHelper;
import io.github.mtrevisan.hunlinter.gui.GUIHelper;
import io.github.mtrevisan.hunlinter.gui.MultiProgressBarUI;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.enums.MorphologicalTag;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.services.eventbus.EventHandler;
import io.github.mtrevisan.hunlinter.services.log.ApplicationLogAppender;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.workers.core.WorkerAbstract;
import io.github.mtrevisan.hunlinter.workers.dictionary.AffixExtractorWorker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;


public class AffixExtractorDialog extends JDialog implements ActionListener, PropertyChangeListener{

	@Serial
	private static final long serialVersionUID = -119971552572420876L;

	private static final Pattern POS_PATTERN = RegexHelper.pattern(MorphologicalTag.PART_OF_SPEECH.getCode() + "([^\\s]+)\\s*");


	private javax.swing.JLabel partOfSpeechLabel;
	private javax.swing.JComboBox<String> partOfSpeechComboBox;
	private javax.swing.JLabel minimumSuffixLengthLabel;
	private javax.swing.JTextField minimumSuffixLengthTextField;
	private javax.swing.JButton extractButton;
	private javax.swing.JProgressBar mainProgressBar;
	private javax.swing.JLabel statusLabel;
	private javax.swing.JScrollPane reducedSetScrollPane;
	private javax.swing.JTextArea affixesTextArea;


	private final ParserManager parserManager;

	private AffixExtractorWorker affixExtractorWorker;
	private final ActionListener actionListener;


	public AffixExtractorDialog(final ParserManager parserManager, final Frame parent) throws IOException{
		super(parent, "Affix Extractor", true);

		Objects.requireNonNull(parserManager, "Parser manager cannot be null");

		this.parserManager = parserManager;
		actionListener = this::partOfSpeechComboBoxActionPerformed;

		initComponents();

		statusLabel.setText(null);

		init();

		try{
			final JPopupMenu copyPopupMenu = new JPopupMenu();
			copyPopupMenu.add(GUIHelper.createPopupCopyMenu(copyPopupMenu, GUIHelper::copyCallback));
			GUIHelper.addPopupMenu(copyPopupMenu, affixesTextArea);
		}
		catch(final IOException ignored){
		}

		reload();

		ApplicationLogAppender.addLabel(statusLabel, ParserManager.MARKER_AFFIX_EXTRACTOR_STATUS);
		ApplicationLogAppender.addTextArea(affixesTextArea, ParserManager.MARKER_AFFIX_EXTRACTOR);
	}

	private void initComponents(){
		partOfSpeechLabel = new javax.swing.JLabel();
		partOfSpeechComboBox = new javax.swing.JComboBox<>();
		minimumSuffixLengthLabel = new javax.swing.JLabel();
		minimumSuffixLengthTextField = new javax.swing.JTextField();
		extractButton = new javax.swing.JButton();
		mainProgressBar = new javax.swing.JProgressBar();
		mainProgressBar.setForeground(MultiProgressBarUI.MAIN_COLOR);
		mainProgressBar.setUI(new MultiProgressBarUI());
		statusLabel = new javax.swing.JLabel();
		reducedSetScrollPane = new javax.swing.JScrollPane();
		affixesTextArea = new javax.swing.JTextArea();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setMinimumSize(new java.awt.Dimension(547, 476));

		partOfSpeechLabel.setText("Part of Speech:");
		partOfSpeechLabel.setPreferredSize(new java.awt.Dimension(78, 17));

		final Font currentFont = FontHelper.getCurrentFont();

		partOfSpeechComboBox.setFont(currentFont);
		partOfSpeechComboBox.addActionListener(actionListener);

		minimumSuffixLengthLabel.setLabelFor(minimumSuffixLengthTextField);
		minimumSuffixLengthLabel.setText("Minimum suffix length:");
		minimumSuffixLengthTextField.setText("5");

		extractButton.setText("Extract");
		extractButton.addActionListener(this::extractButtonActionPerformed);
		extractButton.setEnabled(false);

		affixesTextArea.setEditable(false);
		affixesTextArea.setColumns(20);
		affixesTextArea.setFont(currentFont);
		affixesTextArea.setRows(1);
		affixesTextArea.setTabSize(3);
		affixesTextArea.setLineWrap(true);
		affixesTextArea.setWrapStyleWord(true);
		reducedSetScrollPane.setViewportView(affixesTextArea);

		statusLabel.setText("…");

		final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addContainerGap()
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(layout.createSequentialGroup()
							.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(layout.createSequentialGroup()
									.addComponent(partOfSpeechLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
									.addComponent(partOfSpeechComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
								.addGroup(layout.createSequentialGroup()
									.addComponent(minimumSuffixLengthLabel)
									.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
									.addComponent(minimumSuffixLengthTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
									.addComponent(extractButton))
							.addGap(0, 0, Short.MAX_VALUE)))
						.addComponent(mainProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(reducedSetScrollPane)
					.addComponent(statusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
					.addContainerGap())
		);
		layout.setVerticalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addContainerGap()
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
						.addComponent(partOfSpeechLabel)
						.addComponent(partOfSpeechComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
						.addComponent(minimumSuffixLengthLabel)
						.addComponent(minimumSuffixLengthTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(extractButton))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
					.addGap(18, 18, 18)
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
					.addComponent(mainProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(statusLabel)
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 22, Short.MAX_VALUE)
					.addComponent(reducedSetScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
					.addContainerGap())
		);

		pack();
	}

	private void init(){
		final KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		getRootPane().registerKeyboardAction(this, escapeKeyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}


	@EventHandler
	@SuppressWarnings({"unused", "NumberEquality"})
	public final void initialize(final Integer actionCommand) throws IOException{
		if(actionCommand != MainFrame.ACTION_COMMAND_INITIALIZE)
			return;

		reload();
	}

	public final void reload() throws IOException{
		mainProgressBar.setValue(0);
		partOfSpeechComboBoxActionPerformed(null);

		final TreeSet<String> posEntries = new TreeSet<>();
		final List<String> dicLines = parserManager.getDictionaryLines();
		for(final String dicLine : dicLines){
			final List<String> extractions = RegexHelper.extract(dicLine, POS_PATTERN, 1);
			if(!extractions.isEmpty())
				posEntries.add(extractions.getFirst());
		}

		JavaHelper.executeOnEventDispatchThread(() -> {
			final Object selectedItem = partOfSpeechComboBox.getSelectedItem();
			partOfSpeechComboBox.removeActionListener(actionListener);

			partOfSpeechComboBox.removeAllItems();
			for(final String elem : posEntries)
				partOfSpeechComboBox.addItem(elem);

			//restore previous selection
			partOfSpeechComboBox.addActionListener(actionListener);
			partOfSpeechComboBox.setSelectedItem(selectedItem);
		});

		if(affixExtractorWorker != null && !affixExtractorWorker.isDone())
			affixExtractorWorker.cancel();
	}

	private void partOfSpeechComboBoxActionPerformed(final ActionEvent evt){
		extractButton.setEnabled(true);
		mainProgressBar.setValue(0);

		affixesTextArea.setText(null);
	}

	private void extractButtonActionPerformed(final ActionEvent evt){
		mainProgressBar.setValue(0);
		affixesTextArea.setText(null);
		partOfSpeechComboBox.setEnabled(false);
		minimumSuffixLengthTextField.setEnabled(false);
		extractButton.setEnabled(false);

		extractAffixes();
	}

	@Override
	public final void actionPerformed(final ActionEvent event){
		if(affixExtractorWorker != null && affixExtractorWorker.getState() == SwingWorker.StateValue.STARTED){
			final Runnable cancelTask = () -> {
				partOfSpeechComboBox.setEnabled(true);
				minimumSuffixLengthTextField.setEnabled(true);
				extractButton.setEnabled(true);
			};
			final Runnable resumeTask = () -> setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			GUIHelper.askUserToAbort(affixExtractorWorker, this, cancelTask, resumeTask);
		}
		else
			dispose();
	}

	@Override
	public final void propertyChange(final PropertyChangeEvent evt){
		switch(evt.getPropertyName()){
			case MainFrame.PROPERTY_NAME_PROGRESS -> {
				final int progress = (Integer) evt.getNewValue();
				mainProgressBar.setValue(progress);
			}
			case MainFrame.PROPERTY_NAME_STATE -> {
				final SwingWorker.StateValue stateValue = (SwingWorker.StateValue) evt.getNewValue();
				if(stateValue == SwingWorker.StateValue.STARTED)
					mainProgressBar.setForeground(MultiProgressBarUI.MAIN_COLOR);
			}
			case WorkerAbstract.PROPERTY_WORKER_CANCELLED -> mainProgressBar.setForeground(MultiProgressBarUI.ERROR_COLOR);
		}
	}

	private void extractAffixes(){
		if(affixExtractorWorker == null || affixExtractorWorker.isDone()){
			mainProgressBar.setValue(0);

			final Runnable onCompleted = () -> {
				affixesTextArea.setCaretPosition(0);

				partOfSpeechComboBox.setEnabled(true);
				minimumSuffixLengthTextField.setEnabled(true);
				extractButton.setEnabled(true);
			};
			final Consumer<Exception> onCancelled = exc -> {
				//change color of progress bar to reflect an error
				propertyChange(affixExtractorWorker.propertyChangeEventWorkerCancelled);
			};
			final String partOfSpeech = (String)partOfSpeechComboBox.getSelectedItem();
			final int minimumSuffixLength = Integer.parseInt(minimumSuffixLengthTextField.getText());
			affixExtractorWorker = new AffixExtractorWorker(partOfSpeech, minimumSuffixLength, parserManager.getDicParser(),
				onCompleted, onCancelled);
			affixExtractorWorker.addPropertyChangeListener(this);
			affixExtractorWorker.execute();
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

}
