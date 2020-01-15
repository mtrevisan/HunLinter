package unit731.hunlinter;

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
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.gui.JCopyableTable;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.parsers.vos.RuleEntry;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.parsers.workers.RulesReducerWorker;
import unit731.hunlinter.services.log.ApplicationLogAppender;


public class RulesReducerDialog extends JDialog implements ActionListener, PropertyChangeListener{

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesReducerDialog.class);

	private static final long serialVersionUID = -5660512112885632106L;


	private final Backbone backbone;

	private RulesReducerWorker rulesReducerWorker;


	public RulesReducerDialog(Backbone backbone, Frame parent){
		super(parent, "Rules Reducer", true);

		Objects.requireNonNull(backbone);
		Objects.requireNonNull(parent);

		this.backbone = backbone;

		initComponents();

		final Font font = GUIUtils.getCurrentFont();
		currentSetTextArea.setFont(font);
		reducedSetTextArea.setFont(font);

		init();

		try{
			final JPopupMenu popupMenu = new JPopupMenu();
			popupMenu.add(GUIUtils.createPopupCopyMenu(reducedSetLabel.getHeight(), popupMenu, GUIUtils::copyCallback));
			GUIUtils.addPopupMenu(popupMenu, reducedSetTextArea);
		}
		catch(final IOException ignored){}

		reload();

		ApplicationLogAppender.addTextArea(reducedSetTextArea, Backbone.MARKER_RULE_REDUCER);
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      lblRule = new javax.swing.JLabel();
      ruleComboBox = new javax.swing.JComboBox<>();
      optimizeClosedGroupCheckBox = new javax.swing.JCheckBox();
      reduceButton = new javax.swing.JButton();
      currentSetLabel = new javax.swing.JLabel();
      currentSetScrollPane = new javax.swing.JScrollPane();
      currentSetTextArea = new javax.swing.JTextArea();
      mainProgressBar = new javax.swing.JProgressBar();
      reducedSetLabel = new javax.swing.JLabel();
      reducedSetScrollPane = new javax.swing.JScrollPane();
      reducedSetTextArea = new javax.swing.JTextArea();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
      setMinimumSize(new java.awt.Dimension(547, 476));

      lblRule.setText("Rule:");

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
                  .addComponent(lblRule)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(ruleComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addComponent(reduceButton))
               .addComponent(reducedSetScrollPane)
               .addComponent(currentSetScrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 527, Short.MAX_VALUE)
               .addComponent(mainProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(optimizeClosedGroupCheckBox)
                     .addComponent(reducedSetLabel)
                     .addComponent(currentSetLabel))
                  .addGap(0, 0, Short.MAX_VALUE)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(lblRule)
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
            .addGap(18, 18, 18)
            .addComponent(reducedSetLabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(reducedSetScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

	private void init(){
		KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		getRootPane().registerKeyboardAction(this, escapeKeyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	public final void reload(){
		mainProgressBar.setValue(0);
		currentSetTextArea.setText(null);

		AffixData affixData = backbone.getAffixData();
		List<RuleEntry> affixes = affixData.getRuleEntries();
		List<String> affixEntries = affixes.stream()
			.map(affix -> (affix.isSuffix()? AffixOption.SUFFIX: AffixOption.PREFIX) + StringUtils.SPACE + affix.getEntries().get(0).getFlag())
			.sorted()
			.collect(Collectors.toList());

		javax.swing.SwingUtilities.invokeLater(() -> {
			ruleComboBox.removeAllItems();
			affixEntries.forEach(ruleComboBox::addItem);
		});
	}

   private void ruleComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ruleComboBoxActionPerformed
		String flag = getSelectedFlag();
		if(flag != null){
			RuleEntry rule = backbone.getAffixData().getData(flag);
			StringJoiner sj = new StringJoiner(StringUtils.SPACE);
			String header = sj.add(rule.getType().getOption().getCode())
				.add(flag)
				.add(Character.toString(rule.combinableChar()))
				.add(Integer.toString(rule.getEntries().size()))
				.toString();
			String rules = rule.getEntries().stream()
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
	public void actionPerformed(ActionEvent event){
		if(rulesReducerWorker != null && rulesReducerWorker.getState() == SwingWorker.StateValue.STARTED){
			Runnable cancelTask = () -> {
				ruleComboBox.setEnabled(true);
				optimizeClosedGroupCheckBox.setEnabled(true);
				reduceButton.setEnabled(true);
			};
			Runnable resumeTask = () -> setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			rulesReducerWorker.askUserToAbort(this, cancelTask, resumeTask);
		}
		else
			dispose();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt){
		String propertyName = evt.getPropertyName();
		if("progress".equals(propertyName)){
			int progress = (int)evt.getNewValue();
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
				rulesReducerWorker = new RulesReducerWorker(flag, keepLongestCommonAffix, backbone.getAffixData(), backbone.getDicParser(),
					backbone.getWordGenerator());
				rulesReducerWorker.addPropertyChangeListener(this);
				rulesReducerWorker.execute();
			}
			catch(final Exception e){
				ruleComboBox.setEnabled(true);
				optimizeClosedGroupCheckBox.setEnabled(true);
				reduceButton.setEnabled(true);

				LOGGER.info(Backbone.MARKER_RULE_REDUCER, e.getMessage());
			}
		}
	}

	private String getSelectedFlag(){
		Object item = ruleComboBox.getSelectedItem();
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
   private javax.swing.JLabel lblRule;
   private javax.swing.JProgressBar mainProgressBar;
   private javax.swing.JCheckBox optimizeClosedGroupCheckBox;
   private javax.swing.JButton reduceButton;
   private javax.swing.JLabel reducedSetLabel;
   private javax.swing.JScrollPane reducedSetScrollPane;
   private javax.swing.JTextArea reducedSetTextArea;
   private javax.swing.JComboBox<String> ruleComboBox;
   // End of variables declaration//GEN-END:variables

}
