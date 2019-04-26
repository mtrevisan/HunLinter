package unit731.hunspeller;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;
import unit731.hunspeller.parsers.dictionary.workers.RuleReducerWorker;
import unit731.hunspeller.services.ApplicationLogAppender;


public class RuleReducerDialog extends JDialog implements ActionListener, PropertyChangeListener{

	private static final Logger LOGGER = LoggerFactory.getLogger(RuleReducerDialog.class);

	private static final long serialVersionUID = -5660512112885632106L;


	private final Backbone backbone;

	private RuleReducerWorker ruleReducerWorker;


	public RuleReducerDialog(Backbone backbone, Frame parent){
		super(parent, "Rule reducer", true);

		Objects.requireNonNull(backbone);
		Objects.requireNonNull(parent);

		this.backbone = backbone;

		initComponents();


		init();

		ApplicationLogAppender.addTextArea(resultTextArea, Backbone.MARKER_RULE_REDUCER);
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      lblRule = new javax.swing.JLabel();
      ruleComboBox = new javax.swing.JComboBox<>();
      optimizeClosedGroupCheckBox = new javax.swing.JCheckBox();
      reduceButton = new javax.swing.JButton();
      ruleScrollPane = new javax.swing.JScrollPane();
      ruleTextArea = new javax.swing.JTextArea();
      mainProgressBar = new javax.swing.JProgressBar();
      resultLabel = new javax.swing.JLabel();
      resultScrollPane = new javax.swing.JScrollPane();
      resultTextArea = new javax.swing.JTextArea();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

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

      ruleScrollPane.setBackground(java.awt.Color.white);
      ruleScrollPane.setViewportBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

      ruleTextArea.setEditable(false);
      ruleTextArea.setColumns(20);
      ruleTextArea.setRows(1);
      ruleTextArea.setTabSize(3);
      ruleScrollPane.setViewportView(ruleTextArea);

      resultLabel.setText("Result:");

      resultTextArea.setEditable(false);
      resultTextArea.setColumns(20);
      resultTextArea.setRows(1);
      resultTextArea.setTabSize(3);
      resultScrollPane.setViewportView(resultTextArea);

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
               .addComponent(resultScrollPane)
               .addComponent(ruleScrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 527, Short.MAX_VALUE)
               .addComponent(mainProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(optimizeClosedGroupCheckBox)
                     .addComponent(resultLabel))
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
            .addComponent(ruleScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(mainProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addComponent(resultLabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(resultScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

	private void init(){
		ruleTextArea.setText(null);

		AffixData affixData = backbone.getAffixData();
		List<RuleEntry> affixes = affixData.getRuleEntries();
		List<String> affixEntries = affixes.stream()
			.map(affix -> (affix.isSuffix()? AffixTag.SUFFIX: AffixTag.PREFIX) + StringUtils.SPACE + affix.getEntries().get(0).getFlag())
			.sorted()
			.collect(Collectors.toList());
		ruleComboBox.removeAllItems();
		affixEntries.forEach(ruleComboBox::addItem);
	}

   private void ruleComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ruleComboBoxActionPerformed
		String flag = getSelectedFlag();
		RuleEntry rule = backbone.getAffixData().getData(flag);
		String ruleEntries = rule.getEntries().stream()
			.map(AffixEntry::toString)
			.collect(Collectors.joining(StringUtils.LF));
		ruleTextArea.setText(ruleEntries);
		ruleTextArea.setCaretPosition(0);
		resultTextArea.setText(null);
   }//GEN-LAST:event_ruleComboBoxActionPerformed

   private void reduceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reduceButtonActionPerformed
		mainProgressBar.setValue(0);
		resultTextArea.setText(null);

		reduceRules();
   }//GEN-LAST:event_reduceButtonActionPerformed

   private void optimizeClosedGroupCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optimizeClosedGroupCheckBoxActionPerformed
		resultTextArea.setText(null);
   }//GEN-LAST:event_optimizeClosedGroupCheckBoxActionPerformed

	@Override
	public void actionPerformed(ActionEvent event){
		if(ruleReducerWorker != null){
			SwingWorker.StateValue state = ruleReducerWorker.getState();
			if(state == SwingWorker.StateValue.STARTED){
				ruleReducerWorker.pause();

				Object[] options = {"Abort", "Cancel"};
				int answer = JOptionPane.showOptionDialog(this, "Do you really want to abort the rule reducer task?", "Warning!",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
				if(answer == JOptionPane.YES_OPTION){
					ruleReducerWorker.cancel();

					LOGGER.info(Backbone.MARKER_APPLICATION, "Rule reducer aborted");

					ruleReducerWorker = null;
				}
				else if(answer == JOptionPane.NO_OPTION || answer == JOptionPane.CLOSED_OPTION){
					ruleReducerWorker.resume();

					setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
				}
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt){
		String propertyName = evt.getPropertyName();
		if("progress".equals(propertyName)){
			int progress = (int)evt.getNewValue();
			mainProgressBar.setValue(progress);
		}
		else if("state".equals(propertyName) && evt.getNewValue() == SwingWorker.StateValue.DONE){
			resultTextArea.setCaretPosition(0);
		}
	}

	private void reduceRules(){
		if(ruleReducerWorker == null || ruleReducerWorker.isDone()){
			mainProgressBar.setValue(0);

			try{
				String flag = getSelectedFlag();
				boolean keepLongestCommonAffix = getKeepLongestCommonAffix();
				ruleReducerWorker = new RuleReducerWorker(flag, keepLongestCommonAffix, backbone.getAffixData(), backbone.getDicParser(),
					backbone.getWordGenerator());
				ruleReducerWorker.addPropertyChangeListener(this);
				ruleReducerWorker.execute();
			}
			catch(Exception e){
				LOGGER.info(Backbone.MARKER_RULE_REDUCER, e.getMessage());
			}
		}
	}

	private String getSelectedFlag(){
		return ruleComboBox.getSelectedItem().toString().split(StringUtils.SPACE)[1];
	}

	private boolean getKeepLongestCommonAffix(){
		return optimizeClosedGroupCheckBox.isSelected();
	}

	private void writeObject(ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(RuleReducerDialog.class.getName());
	}

	private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException{
		throw new NotSerializableException(RuleReducerDialog.class.getName());
	}


   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JLabel lblRule;
   private javax.swing.JProgressBar mainProgressBar;
   private javax.swing.JCheckBox optimizeClosedGroupCheckBox;
   private javax.swing.JButton reduceButton;
   private javax.swing.JLabel resultLabel;
   private javax.swing.JScrollPane resultScrollPane;
   private javax.swing.JTextArea resultTextArea;
   private javax.swing.JComboBox<String> ruleComboBox;
   private javax.swing.JScrollPane ruleScrollPane;
   private javax.swing.JTextArea ruleTextArea;
   // End of variables declaration//GEN-END:variables
}
