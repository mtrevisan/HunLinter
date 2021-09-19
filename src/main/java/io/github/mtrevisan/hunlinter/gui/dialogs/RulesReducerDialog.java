/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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

import io.github.mtrevisan.hunlinter.MainFrame;
import io.github.mtrevisan.hunlinter.gui.FontHelper;
import io.github.mtrevisan.hunlinter.gui.GUIHelper;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixOption;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixType;
import io.github.mtrevisan.hunlinter.parsers.vos.AffixEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.RuleEntry;
import io.github.mtrevisan.hunlinter.services.eventbus.EventHandler;
import io.github.mtrevisan.hunlinter.services.log.ApplicationLogAppender;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.workers.WorkerManager;
import io.github.mtrevisan.hunlinter.workers.affix.RulesReducerWorker;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Consumer;


public class RulesReducerDialog extends JDialog implements ActionListener, PropertyChangeListener{

	@Serial
	private static final long serialVersionUID = -5660512112885632106L;

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesReducerDialog.class);


//	private final WorkerManager workerManager;
	private final ParserManager parserManager;

	private RulesReducerWorker rulesReducerWorker;
	private final ActionListener actionListener;


	public RulesReducerDialog(/*final WorkerManager workerManager,*/ final ParserManager parserManager, final Frame parent){
		super(parent, "Rules Reducer", true);

//		Objects.requireNonNull(workerManager, "Worker manager cannot be null");
		Objects.requireNonNull(parserManager, "Parser manager cannot be null");

//		this.workerManager = workerManager;
		this.parserManager = parserManager;
		actionListener = this::ruleComboBoxActionPerformed;

		initComponents();

		statusLabel.setText(null);

		init();

		try{
			final JPopupMenu copyPopupMenu = new JPopupMenu();
			copyPopupMenu.add(GUIHelper.createPopupCopyMenu(copyPopupMenu, GUIHelper::copyCallback));
			GUIHelper.addPopupMenu(copyPopupMenu, reducedSetTextArea);
		}
		catch(final IOException ignored){}

		reload();

		ApplicationLogAppender.addLabel(statusLabel, ParserManager.MARKER_RULE_REDUCER_STATUS);
		ApplicationLogAppender.addTextArea(reducedSetTextArea, ParserManager.MARKER_RULE_REDUCER);
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      ruleLabel = new javax.swing.JLabel();
      ruleComboBox = new javax.swing.JComboBox<>();
      optimizeClosedGroupCheckBox = new javax.swing.JCheckBox();
      reduceButton = new javax.swing.JButton();
      currentSetLabel = new javax.swing.JLabel();
      currentSetScrollPane = new javax.swing.JScrollPane();
      currentSetTextArea = new javax.swing.JTextArea();
      mainProgressBar = new javax.swing.JProgressBar();
      statusLabel = new javax.swing.JLabel();
      reducedSetLabel = new javax.swing.JLabel();
      reducedSetScrollPane = new javax.swing.JScrollPane();
      reducedSetTextArea = new javax.swing.JTextArea();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
      setMinimumSize(new java.awt.Dimension(547, 476));

      ruleLabel.setText("Rule:");
      ruleLabel.setPreferredSize(new java.awt.Dimension(26, 17));

		final Font currentFont = FontHelper.getCurrentFont();

		ruleComboBox.setFont(currentFont);
      ruleComboBox.addActionListener(actionListener);

      optimizeClosedGroupCheckBox.setText("Optimize for closed group");
      optimizeClosedGroupCheckBox.addActionListener(this::optimizeClosedGroupCheckBoxActionPerformed);
		optimizeClosedGroupCheckBox.setEnabled(false);

      reduceButton.setText("Reduce");
      reduceButton.addActionListener(this::reduceButtonActionPerformed);
		reduceButton.setEnabled(false);

      currentSetLabel.setText("Current set:");

      currentSetScrollPane.setBackground(java.awt.Color.white);
      currentSetScrollPane.setViewportBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

      currentSetTextArea.setEditable(false);
      currentSetTextArea.setColumns(20);
      currentSetTextArea.setFont(currentFont);
      currentSetTextArea.setRows(1);
      currentSetTextArea.setTabSize(3);
      currentSetScrollPane.setViewportView(currentSetTextArea);

      statusLabel.setText("…");

      reducedSetLabel.setText("Reduced set:");

      reducedSetTextArea.setEditable(false);
      reducedSetTextArea.setColumns(20);
      reducedSetTextArea.setFont(currentFont);
      reducedSetTextArea.setRows(1);
      reducedSetTextArea.setTabSize(3);
      reducedSetScrollPane.setViewportView(reducedSetTextArea);

      final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(ruleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(ruleComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addComponent(reduceButton))
               .addComponent(currentSetScrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 527, Short.MAX_VALUE)
               .addComponent(mainProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
               .addComponent(reducedSetScrollPane)
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(optimizeClosedGroupCheckBox)
                     .addComponent(currentSetLabel)
                     .addComponent(reducedSetLabel))
                  .addGap(0, 0, Short.MAX_VALUE))
               .addComponent(statusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(ruleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(ruleComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(reduceButton))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(optimizeClosedGroupCheckBox)
            .addGap(18, 18, 18)
            .addComponent(currentSetLabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(currentSetScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(mainProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(statusLabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 22, Short.MAX_VALUE)
            .addComponent(reducedSetLabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(reducedSetScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

	private void init(){
		final KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		getRootPane().registerKeyboardAction(this, escapeKeyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}


	@EventHandler
	@SuppressWarnings({"unused", "NumberEquality"})
	public final void initialize(final Integer actionCommand){
		if(actionCommand != MainFrame.ACTION_COMMAND_INITIALIZE)
			return;

		reload();
	}

	public final void reload(){
		mainProgressBar.setValue(0);
		currentSetTextArea.setText(null);
		ruleComboBoxActionPerformed(null);

		final AffixData affixData = parserManager.getAffixData();
		final List<RuleEntry> affixes = affixData.getRuleEntries();
		final List<String> affixEntries = new ArrayList<>(affixes.size());
		final StringBuilder sb = new StringBuilder(6);
		for(final RuleEntry affix : affixes){
			sb.setLength(0);
			sb.append((affix.getType() == AffixType.SUFFIX? AffixOption.SUFFIX: AffixOption.PREFIX).getCode())
				.append(StringUtils.SPACE)
				.append(affix.getFlag());
			affixEntries.add(sb.toString());
		}
		affixEntries.sort(null);

		JavaHelper.executeOnEventDispatchThread(() -> {
			final Object selectedItem = ruleComboBox.getSelectedItem();
			ruleComboBox.removeActionListener(actionListener);

			ruleComboBox.removeAllItems();
			for(final String elem : affixEntries)
				ruleComboBox.addItem(elem);

			//restore previous selection
			ruleComboBox.addActionListener(actionListener);
			ruleComboBox.setSelectedItem(selectedItem);
		});

		if(rulesReducerWorker != null && !rulesReducerWorker.isDone())
			rulesReducerWorker.cancel();
	}

   private void ruleComboBoxActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ruleComboBoxActionPerformed
		final String flag = getSelectedFlag();

		final boolean selected = (flag != null);
		optimizeClosedGroupCheckBox.setEnabled(selected);
		reduceButton.setEnabled(selected);
		if(selected){
			mainProgressBar.setValue(0);

			final RuleEntry rule = parserManager.getAffixData().getData(flag);
			final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
			final String header = sj.add(rule.getType().getOption().getCode())
				.add(flag)
				.add(Character.toString(rule.combinableChar()))
				.add(Integer.toString(rule.getEntries().size()))
				.toString();
			final StringJoiner rules = new StringJoiner(StringUtils.LF);
			for(final AffixEntry affixEntry : rule.getEntries())
				rules.add(affixEntry.toString());
			currentSetTextArea.setText(header + StringUtils.LF + rules);
			currentSetTextArea.setCaretPosition(0);
			reducedSetTextArea.setText(null);
		}
   }//GEN-LAST:event_ruleComboBoxActionPerformed

   private void reduceButtonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reduceButtonActionPerformed
		mainProgressBar.setValue(0);
		reducedSetTextArea.setText(null);
		ruleComboBox.setEnabled(false);
		optimizeClosedGroupCheckBox.setEnabled(false);
		reduceButton.setEnabled(false);

		reduceRules();
   }//GEN-LAST:event_reduceButtonActionPerformed

   private void optimizeClosedGroupCheckBoxActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optimizeClosedGroupCheckBoxActionPerformed
		reducedSetTextArea.setText(null);
   }//GEN-LAST:event_optimizeClosedGroupCheckBoxActionPerformed

	@Override
	public final void actionPerformed(final ActionEvent event){
		if(rulesReducerWorker != null && rulesReducerWorker.getState() == SwingWorker.StateValue.STARTED){
			final Runnable cancelTask = () -> {
				ruleComboBox.setEnabled(true);
				optimizeClosedGroupCheckBox.setEnabled(true);
				reduceButton.setEnabled(true);
			};
			final Runnable resumeTask = () -> setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			GUIHelper.askUserToAbort(rulesReducerWorker, this, cancelTask, resumeTask);
		}
		else
			dispose();
	}

	@Override
	public final void propertyChange(final PropertyChangeEvent evt){
		final String propertyName = evt.getPropertyName();
		if(MainFrame.PROPERTY_NAME_PROGRESS.equals(propertyName)){
			final int progress = (Integer)evt.getNewValue();
			mainProgressBar.setValue(progress);
		}
	}

	private void reduceRules(){
		if(rulesReducerWorker == null || rulesReducerWorker.isDone()){
			mainProgressBar.setValue(0);

			final String flag = getSelectedFlag();
			final boolean keepLongestCommonAffix = isKeepLongestCommonAffix();
			final Runnable onEnd = () -> {
				reducedSetTextArea.setCaretPosition(0);

				ruleComboBox.setEnabled(true);
				optimizeClosedGroupCheckBox.setEnabled(true);
				reduceButton.setEnabled(true);
			};
			rulesReducerWorker = new RulesReducerWorker(flag, keepLongestCommonAffix, parserManager.getAffixData(),
				parserManager.getDicParser(), parserManager.getWordGenerator(), onEnd);
			rulesReducerWorker.addPropertyChangeListener(this);
			rulesReducerWorker.execute();
		}
	}

	private String getSelectedFlag(){
		final Object item = ruleComboBox.getSelectedItem();
		return (item != null? StringUtils.split(item.toString())[1]: null);
	}

	private boolean isKeepLongestCommonAffix(){
		return optimizeClosedGroupCheckBox.isSelected();
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
   private javax.swing.JLabel currentSetLabel;
   private javax.swing.JScrollPane currentSetScrollPane;
   private javax.swing.JTextArea currentSetTextArea;
   private javax.swing.JProgressBar mainProgressBar;
   private javax.swing.JCheckBox optimizeClosedGroupCheckBox;
   private javax.swing.JButton reduceButton;
   private javax.swing.JLabel reducedSetLabel;
   private javax.swing.JScrollPane reducedSetScrollPane;
   private javax.swing.JTextArea reducedSetTextArea;
   private javax.swing.JComboBox<String> ruleComboBox;
   private javax.swing.JLabel ruleLabel;
   private javax.swing.JLabel statusLabel;
   // End of variables declaration//GEN-END:variables

}
