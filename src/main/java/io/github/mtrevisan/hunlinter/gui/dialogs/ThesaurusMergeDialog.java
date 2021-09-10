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

import io.github.mtrevisan.hunlinter.gui.FontHelper;
import io.github.mtrevisan.hunlinter.gui.GUIHelper;
import io.github.mtrevisan.hunlinter.parsers.thesaurus.SynonymsEntry;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.List;
import java.util.Objects;


public class ThesaurusMergeDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 8375487773986703688L;

	private final SynonymsEntry baseSynonyms;
	private final String definition;
	private final List<SynonymsEntry> synonymsEntries;

	private boolean merged;


	public ThesaurusMergeDialog(final String definition, final SynonymsEntry baseSynonyms, final List<SynonymsEntry> synonymsEntries, final Frame parent){
		super(parent, "Thesaurus merger", true);

		Objects.requireNonNull(synonymsEntries, "Synonyms entries cannot be null");

		this.baseSynonyms = baseSynonyms;
		this.definition = definition;
		this.synonymsEntries = synonymsEntries;

		initComponents();

		GUIHelper.addUndoManager(mergerTextArea);

		for(final SynonymsEntry entry : synonymsEntries)
			lineComboBox.addItem(entry.toString());
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

		final Font currentFont = FontHelper.getCurrentFont();

		lineComboBox.setFont(currentFont);
      lineComboBox.addActionListener(this::lineComboBoxActionPerformed);

      mergerScrollPane.setBackground(java.awt.Color.white);
      mergerScrollPane.setViewportBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

      mergerTextArea.setColumns(20);
      mergerTextArea.setFont(currentFont);
      mergerTextArea.setLineWrap(true);
      mergerTextArea.setRows(5);
      mergerTextArea.setWrapStyleWord(true);
      mergerScrollPane.setViewportView(mergerTextArea);

      mergeButton.setText("Merge");
      mergeButton.addActionListener(this::mergeButtonActionPerformed);

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

	public final String getMerge(){
		return mergerTextArea.getText();
	}

	public final boolean isMerged(){
		return merged;
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
   private javax.swing.JLabel lblMessage;
   private javax.swing.JComboBox<String> lineComboBox;
   private javax.swing.JButton mergeButton;
   private javax.swing.JScrollPane mergerScrollPane;
   private javax.swing.JTextArea mergerTextArea;
   // End of variables declaration//GEN-END:variables

}
