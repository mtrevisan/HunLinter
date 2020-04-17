package unit731.hunlinter;

import java.awt.*;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Objects;
import javax.swing.*;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.parsers.thesaurus.SynonymsEntry;

import static unit731.hunlinter.services.system.LoopHelper.forEach;


public class ThesaurusMergeDialog extends JDialog{

	private static final long serialVersionUID = 8375487773986703688L;

	private final SynonymsEntry baseSynonyms;
	private final String definition;
	private final List<SynonymsEntry> synonymsEntries;

	private boolean merged;


	public ThesaurusMergeDialog(final String definition, final SynonymsEntry baseSynonyms, final List<SynonymsEntry> synonymsEntries, final Frame parent){
		super(parent, "Thesaurus merger", true);

		Objects.requireNonNull(synonymsEntries);

		this.baseSynonyms = baseSynonyms;
		this.definition = definition;
		this.synonymsEntries = synonymsEntries;

		initComponents();

		GUIUtils.addUndoManager(mergerTextArea);

		forEach(synonymsEntries, entry -> lineComboBox.addItem(entry.toString()));
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      lblMessage = new javax.swing.JLabel();
      lineComboBox = new javax.swing.JComboBox<>();
      mergerScrollPane = new javax.swing.JScrollPane();
      mergerTextArea = new javax.swing.JTextArea();
      mergeButton = new javax.swing.JButton();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      lblMessage.setLabelFor(lineComboBox);
      lblMessage.setText("Select a line from the list:");

      lineComboBox.setFont(GUIUtils.getCurrentFont());
      lineComboBox.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            lineComboBoxActionPerformed(evt);
         }
      });

      mergerScrollPane.setBackground(java.awt.Color.white);
      mergerScrollPane.setViewportBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

      mergerTextArea.setColumns(20);
      mergerTextArea.setFont(GUIUtils.getCurrentFont());
      mergerTextArea.setLineWrap(true);
      mergerTextArea.setRows(5);
      mergerTextArea.setWrapStyleWord(true);
      mergerScrollPane.setViewportView(mergerTextArea);

      mergeButton.setText("Merge");
      mergeButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            mergeButtonActionPerformed(evt);
         }
      });

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(mergerScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 527, Short.MAX_VALUE)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(lblMessage)
                  .addGap(0, 0, Short.MAX_VALUE))
               .addComponent(lineComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addGap(0, 0, Short.MAX_VALUE)
                  .addComponent(mergeButton)
                  .addGap(0, 0, Short.MAX_VALUE)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(lblMessage)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(lineComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addComponent(mergerScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
            .addGap(18, 18, 18)
            .addComponent(mergeButton)
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

   private void lineComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lineComboBoxActionPerformed
		final int synonymsIndex = lineComboBox.getSelectedIndex();

		final String def = (baseSynonyms.containsSynonym(definition)? StringUtils.EMPTY: definition);
		final SynonymsEntry selectedSynonyms = synonymsEntries.get(synonymsIndex);
		final SynonymsEntry mergedEntry = baseSynonyms.merge(def, selectedSynonyms);

		mergerTextArea.setText(mergedEntry.toString());
		mergerTextArea.setCaretPosition(0);
   }//GEN-LAST:event_lineComboBoxActionPerformed

   private void mergeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mergeButtonActionPerformed
		merged = true;

		dispose();
   }//GEN-LAST:event_mergeButtonActionPerformed

	public String getMerge(){
		return mergerTextArea.getText();
	}

	public boolean isMerged(){
		return merged;
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
   private javax.swing.JLabel lblMessage;
   private javax.swing.JComboBox<String> lineComboBox;
   private javax.swing.JButton mergeButton;
   private javax.swing.JScrollPane mergerScrollPane;
   private javax.swing.JTextArea mergerTextArea;
   // End of variables declaration//GEN-END:variables

}
