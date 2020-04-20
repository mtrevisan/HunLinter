package unit731.hunlinter.gui.dialogs;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import javax.swing.*;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.gui.FontHelper;
import unit731.hunlinter.gui.GUIHelper;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.parsers.enums.AffixType;
import unit731.hunlinter.parsers.vos.RuleEntry;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.workers.affix.RulesReducerWorker;
import unit731.hunlinter.services.log.ApplicationLogAppender;
import unit731.hunlinter.services.system.JavaHelper;

import static unit731.hunlinter.services.system.LoopHelper.forEach;


public class RulesReducerDialog extends JDialog implements ActionListener, PropertyChangeListener{

	private static final long serialVersionUID = -5660512112885632106L;

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesReducerDialog.class);


	private final ParserManager parserManager;

	private RulesReducerWorker rulesReducerWorker;


	public RulesReducerDialog(final ParserManager parserManager, final Frame parent){
		super(parent, "Rules Reducer", true);

		Objects.requireNonNull(parserManager);

		this.parserManager = parserManager;

		initComponents();

		final Font font = FontHelper.getCurrentFont();
		currentSetTextArea.setFont(font);
		reducedSetTextArea.setFont(font);

		statusLabel.setText(StringUtils.EMPTY);

		init();

		try{
			final JPopupMenu popupMenu = new JPopupMenu();
			popupMenu.add(GUIHelper.createPopupCopyMenu(reducedSetLabel.getHeight(), popupMenu, GUIHelper::copyCallback));
			GUIHelper.addPopupMenu(popupMenu, reducedSetTextArea);
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

      ruleComboBox.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
      ruleComboBox.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            ruleComboBoxActionPerformed(evt);
         }
      });

      optimizeClosedGroupCheckBox.setText("Optimize for closed group");
      optimizeClosedGroupCheckBox.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            optimizeClosedGroupCheckBoxActionPerformed(evt);
         }
      });

      reduceButton.setText("Reduce");
      reduceButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            reduceButtonActionPerformed(evt);
         }
      });

      currentSetLabel.setText("Current set:");

      currentSetScrollPane.setBackground(java.awt.Color.white);
      currentSetScrollPane.setViewportBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

      currentSetTextArea.setEditable(false);
      currentSetTextArea.setColumns(20);
      currentSetTextArea.setRows(1);
      currentSetTextArea.setTabSize(3);
      currentSetScrollPane.setViewportView(currentSetTextArea);

      statusLabel.setText("…");

      reducedSetLabel.setText("Reduced set:");

      reducedSetTextArea.setEditable(false);
      reducedSetTextArea.setColumns(20);
      reducedSetTextArea.setRows(1);
      reducedSetTextArea.setTabSize(3);
      reducedSetScrollPane.setViewportView(reducedSetTextArea);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
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
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 19, Short.MAX_VALUE)
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

	public final void reload(){
		mainProgressBar.setValue(0);
		currentSetTextArea.setText(null);

		final AffixData affixData = parserManager.getAffixData();
		final List<RuleEntry> affixes = affixData.getRuleEntries();
		final List<String> affixEntries = affixes.stream()
			.map(affix -> (affix.getType() == AffixType.SUFFIX? AffixOption.SUFFIX: AffixOption.PREFIX)
				+ StringUtils.SPACE + affix.getEntries()[0].getFlag())
			.sorted()
			.collect(Collectors.toList());

		JavaHelper.executeOnEventDispatchThread(() -> {
			ruleComboBox.removeAllItems();
			forEach(affixEntries, ruleComboBox::addItem);
		});

		if(rulesReducerWorker != null && !rulesReducerWorker.isDone())
			rulesReducerWorker.cancel();
	}

   public void ruleComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ruleComboBoxActionPerformed
		final String flag = getSelectedFlag();
		if(flag != null){
			mainProgressBar.setValue(0);

			final RuleEntry rule = parserManager.getAffixData().getData(flag);
			final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
			final String header = sj.add(rule.getType().getOption().getCode())
				.add(flag)
				.add(Character.toString(rule.combinableChar()))
				.add(Integer.toString(rule.getEntries().length))
				.toString();
			final String rules = Arrays.stream(rule.getEntries())
				.map(AffixEntry::toString)
				.collect(Collectors.joining(StringUtils.LF));
			currentSetTextArea.setText(header + StringUtils.LF + rules);
			currentSetTextArea.setCaretPosition(0);
			reducedSetTextArea.setText(null);
		}
   }//GEN-LAST:event_ruleComboBoxActionPerformed

   private void reduceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reduceButtonActionPerformed
		mainProgressBar.setValue(0);
		reducedSetTextArea.setText(null);
		ruleComboBox.setEnabled(false);
		optimizeClosedGroupCheckBox.setEnabled(false);
		reduceButton.setEnabled(false);

		reduceRules();
   }//GEN-LAST:event_reduceButtonActionPerformed

   private void optimizeClosedGroupCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optimizeClosedGroupCheckBoxActionPerformed
		reducedSetTextArea.setText(null);
   }//GEN-LAST:event_optimizeClosedGroupCheckBoxActionPerformed

	@Override
	public void actionPerformed(final ActionEvent event){
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
	public void propertyChange(final PropertyChangeEvent evt){
		final String propertyName = evt.getPropertyName();
		if("progress".equals(propertyName)){
			final int progress = (int)evt.getNewValue();
			mainProgressBar.setValue(progress);
		}
		else if("state".equals(propertyName) && evt.getNewValue() == SwingWorker.StateValue.DONE){
			reducedSetTextArea.setCaretPosition(0);

			ruleComboBox.setEnabled(true);
			optimizeClosedGroupCheckBox.setEnabled(true);
			reduceButton.setEnabled(true);
		}
	}

	private void reduceRules(){
		if(rulesReducerWorker == null || rulesReducerWorker.isDone()){
			mainProgressBar.setValue(0);

			try{
				final String flag = getSelectedFlag();
				final boolean keepLongestCommonAffix = getKeepLongestCommonAffix();
				rulesReducerWorker = new RulesReducerWorker(flag, keepLongestCommonAffix, parserManager.getAffixData(), parserManager.getDicParser(),
					parserManager.getWordGenerator());
				rulesReducerWorker.addPropertyChangeListener(this);
				rulesReducerWorker.execute();
			}
			catch(final Exception e){
				ruleComboBox.setEnabled(true);
				optimizeClosedGroupCheckBox.setEnabled(true);
				reduceButton.setEnabled(true);

				LOGGER.info(ParserManager.MARKER_RULE_REDUCER, e.getMessage());
			}
		}
	}

	private String getSelectedFlag(){
		final Object item = ruleComboBox.getSelectedItem();
		return (item != null? StringUtils.split(item.toString())[1]: null);
	}

	private boolean getKeepLongestCommonAffix(){
		return optimizeClosedGroupCheckBox.isSelected();
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
