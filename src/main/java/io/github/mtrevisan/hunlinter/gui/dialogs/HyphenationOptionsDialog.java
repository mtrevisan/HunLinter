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
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenationOptions;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenationOptionsParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class HyphenationOptionsDialog extends javax.swing.JDialog implements ActionListener{

	@Serial
	private static final long serialVersionUID = 2441720505407258235L;

	private static final Logger LOGGER = LoggerFactory.getLogger(HyphenationOptionsDialog.class);

	private final Consumer<HyphenationOptionsParser> acceptButtonAction;


	public HyphenationOptionsDialog(final HyphenationOptionsParser options, final Consumer<HyphenationOptionsParser> acceptButtonAction, final java.awt.Frame parent){
		super(parent, true);

		Objects.requireNonNull(options, "Options cannot be null");
		Objects.requireNonNull(acceptButtonAction, "Accept button action cannot be null");

		this.acceptButtonAction = acceptButtonAction;

		initComponents();

		setCurrentFont();

		minLeftNonCompoundTextField.setText(Integer.toString(options.getNonCompoundOptions().getLeftMin()));
		minRightNonCompoundTextField.setText(Integer.toString(options.getNonCompoundOptions().getRightMin()));
		minLeftCompoundTextField.setText(Integer.toString(options.getCompoundOptions().getLeftMin()));
		minRightCompoundTextField.setText(Integer.toString(options.getCompoundOptions().getRightMin()));

		final List<String> list = new ArrayList<>(options.getNoHyphen());
		list.sort(Comparator.naturalOrder());
		final DefaultListModel<String> model = new DefaultListModel<>();
		for(final String elem : list)
			model.addElement(elem);
		noHyphenationList.setModel(model);
	}

	private void setCurrentFont(){
		final Font currentFont = FontHelper.getCurrentFont();
		noHyphenationTextField.setFont(currentFont);
		noHyphenationList.setFont(currentFont);
	}

	/**
	 * This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      minLeftNonCompoundLabel = new javax.swing.JLabel();
      minLeftNonCompoundTextField = new javax.swing.JTextField();
      minRightNonCompoundLabel = new javax.swing.JLabel();
      minRightNonCompoundTextField = new javax.swing.JTextField();
      minLeftCompoundLabel = new javax.swing.JLabel();
      minLeftCompoundTextField = new javax.swing.JTextField();
      minRightCompoundLabel = new javax.swing.JLabel();
      minRightCompoundTextField = new javax.swing.JTextField();
      noHyphenationLabel = new javax.swing.JLabel();
      noHyphenationTextField = new javax.swing.JTextField();
      addButton = new javax.swing.JButton();
      acceptButton = new javax.swing.JButton();
      noHyphenationScrollPane = new javax.swing.JScrollPane();
      noHyphenationList = new javax.swing.JList<>();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      minLeftNonCompoundLabel.setLabelFor(minLeftNonCompoundTextField);
      minLeftNonCompoundLabel.setText("Minimal left distance:");

      final DocumentFilter integerFilter = new IntegerFilter();
      ((AbstractDocument)minLeftNonCompoundTextField.getDocument()).setDocumentFilter(integerFilter);

      minRightNonCompoundLabel.setLabelFor(minRightNonCompoundTextField);
      minRightNonCompoundLabel.setText("Minimal right distance:");

      ((AbstractDocument)minRightNonCompoundTextField.getDocument()).setDocumentFilter(integerFilter);

      minLeftCompoundLabel.setLabelFor(minLeftCompoundTextField);
      minLeftCompoundLabel.setText("Minimal left compound distance:");

      ((AbstractDocument)minLeftCompoundTextField.getDocument()).setDocumentFilter(integerFilter);

      minRightCompoundLabel.setLabelFor(minRightCompoundTextField);
      minRightCompoundLabel.setText("Minimal right compound distance:");

      ((AbstractDocument)minRightCompoundTextField.getDocument()).setDocumentFilter(integerFilter);

      noHyphenationLabel.setLabelFor(noHyphenationTextField);
      noHyphenationLabel.setText("No hyph character:");

      addButton.setText("Add");
      addButton.addActionListener(this::addButtonActionPerformed);

      acceptButton.setText("Accept");
      acceptButton.addActionListener(this::acceptButtonActionPerformed);

		final Font currentFont = FontHelper.getCurrentFont();

		noHyphenationList.setFont(currentFont);
      //listen for row removal
      final KeyStroke cancelKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
      noHyphenationList.registerKeyboardAction(this, cancelKeyStroke, JComponent.WHEN_FOCUSED);
      noHyphenationScrollPane.setViewportView(noHyphenationList);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addGap(10, 10, 10)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(layout.createSequentialGroup()
                        .addComponent(minLeftNonCompoundLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(minLeftNonCompoundTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                     .addGroup(layout.createSequentialGroup()
                        .addComponent(minRightNonCompoundLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(minRightNonCompoundTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                     .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                           .addGroup(layout.createSequentialGroup()
                              .addComponent(minLeftCompoundLabel)
                              .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                              .addComponent(minLeftCompoundTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                           .addGroup(layout.createSequentialGroup()
                              .addComponent(minRightCompoundLabel)
                              .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                              .addComponent(minRightCompoundTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))))
               .addGroup(layout.createSequentialGroup()
                  .addContainerGap()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                     .addComponent(noHyphenationScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                     .addComponent(acceptButton)))
               .addGroup(layout.createSequentialGroup()
                  .addGap(10, 10, 10)
                  .addComponent(noHyphenationLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(noHyphenationTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(addButton)))
            .addGap(10, 10, 10))
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(minLeftNonCompoundLabel)
               .addComponent(minLeftNonCompoundTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(minRightNonCompoundLabel)
               .addComponent(minRightNonCompoundTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
               .addComponent(minLeftCompoundLabel)
               .addComponent(minLeftCompoundTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(minRightCompoundLabel)
               .addComponent(minRightCompoundTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(noHyphenationLabel)
               .addComponent(noHyphenationTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(addButton))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(noHyphenationScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addComponent(acceptButton)
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

	@Override
	public final void actionPerformed(final ActionEvent event){
		if(event.getSource() == noHyphenationList)
			removeSelectedRows();
	}

	private void removeSelectedRows(){
		try{
			final int[] selectedRows = noHyphenationList.getSelectedIndices();
			deleteRows(selectedRows);
		}
		catch(final RuntimeException re){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Deletion error: {}", re.getMessage());
		}
	}

	private void deleteRows(final int[] selectedRowIDs){
		final DefaultListModel<String> model = (DefaultListModel<String>)(noHyphenationList.getModel());
		for(int i = 0; i < selectedRowIDs.length; i ++)
			model.remove(selectedRowIDs[i] - i);
	}

	private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
		final DefaultListModel<String> model = (DefaultListModel<String>)(noHyphenationList.getModel());
		model.addElement(noHyphenationTextField.getText());
	}//GEN-LAST:event_addButtonActionPerformed

   private void acceptButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_acceptButtonActionPerformed
		final int minLeftNonCompound = (!minLeftNonCompoundTextField.getText().isEmpty()? Integer.parseInt(minLeftNonCompoundTextField.getText()): -1);
		final int minRightNonCompound = (!minRightNonCompoundTextField.getText().isEmpty()? Integer.parseInt(minRightNonCompoundTextField.getText()): -1);
		final int minLeftCompound = (!minLeftCompoundTextField.getText().isEmpty()? Integer.parseInt(minLeftCompoundTextField.getText()): -1);
		final int minRightCompound = (!minRightCompoundTextField.getText().isEmpty()? Integer.parseInt(minRightCompoundTextField.getText()): -1);
		final ListModel<String> model = noHyphenationList.getModel();
		final List<String> noHyphen = IntStream.range(0, model.getSize())
			.mapToObj(model::getElementAt)
			.collect(Collectors.toList());

		final HyphenationOptionsParser options = new HyphenationOptionsParser();
		final HyphenationOptions nonCompoundOptions = options.getNonCompoundOptions();
		nonCompoundOptions.setLeftMin(minLeftNonCompound);
		nonCompoundOptions.setRightMin(minRightNonCompound);
		final HyphenationOptions compoundOptions = options.getCompoundOptions();
		compoundOptions.setLeftMin(minLeftCompound);
		compoundOptions.setRightMin(minRightCompound);
		options.getNoHyphen().clear();
		options.getNoHyphen().addAll(noHyphen);
		acceptButtonAction.accept(options);

		dispose();
   }//GEN-LAST:event_acceptButtonActionPerformed


   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JButton acceptButton;
   private javax.swing.JButton addButton;
   private javax.swing.JLabel minLeftCompoundLabel;
   private javax.swing.JTextField minLeftCompoundTextField;
   private javax.swing.JLabel minLeftNonCompoundLabel;
   private javax.swing.JTextField minLeftNonCompoundTextField;
   private javax.swing.JLabel minRightCompoundLabel;
   private javax.swing.JTextField minRightCompoundTextField;
   private javax.swing.JLabel minRightNonCompoundLabel;
   private javax.swing.JTextField minRightNonCompoundTextField;
   private javax.swing.JLabel noHyphenationLabel;
   private javax.swing.JList<String> noHyphenationList;
   private javax.swing.JScrollPane noHyphenationScrollPane;
   private javax.swing.JTextField noHyphenationTextField;
   // End of variables declaration//GEN-END:variables

}
