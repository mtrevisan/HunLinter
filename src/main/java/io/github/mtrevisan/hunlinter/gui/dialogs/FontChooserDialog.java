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
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.NumberFormatter;
import javax.swing.text.Position;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * A dialog to prompt the user for a font. It has a static method to display the dialog and return a
 * new {@code Font} instance.
 */
public class FontChooserDialog extends javax.swing.JDialog{

	@Serial
	private static final long serialVersionUID = -4686780467476615109L;

	public static final Font DEFAULT_FONT = new Font("Monospaced", Font.PLAIN, 14);


	private record ListSearchTextFieldDocumentHandler(JList<String> targetList) implements DocumentListener{

		@Override
		public final void insertUpdate(final DocumentEvent e){
			update(e);
		}

		@Override
		public final void removeUpdate(final DocumentEvent e){
			update(e);
		}

		@Override
		public final void changedUpdate(final DocumentEvent e){
			update(e);
		}

		private void update(final DocumentEvent event){
			try{
				final Document doc = event.getDocument();
				final String newValue = doc.getText(0, doc.getLength());
				if(!newValue.isEmpty() && targetList.getModel().getSize() > 0){
					final int index = targetList.getNextMatch(newValue, 0, Position.Bias.Forward);
					final int foundIndex = Math.max(index, 0);

					targetList.ensureIndexIsVisible(foundIndex);

					final String matchedName = targetList.getModel().getElementAt(foundIndex);
					if(newValue.equalsIgnoreCase(matchedName) && foundIndex != targetList.getSelectedIndex())
						JavaHelper.executeOnEventDispatchThread(() -> targetList.setSelectedIndex(foundIndex));
				}
			}
			catch(final BadLocationException ignored){}
		}
	}


	private Font selectedFont;
	private final Consumer<Font> onSelection;

	private Font previousFont;
	private final String sampleText;


	public FontChooserDialog(final Supplier<String> sampleExtractor, final Consumer<Font> onSelection, final Frame parent){
		super(parent, "Font chooser", true);

		Objects.requireNonNull(sampleExtractor, "Sample extractor cannot be null");
		Objects.requireNonNull(onSelection, "On selection cannot be null");
		Objects.requireNonNull(parent, "Parent cannot be null");

		sampleText = sampleExtractor.get();
		FontHelper.extractFonts(sampleText);

		initComponents();

		final Font initialFont = FontHelper.getCurrentFont();
		selectedFont = (initialFont == null? DEFAULT_FONT: initialFont);
		this.onSelection = onSelection;
		previousFont = selectedFont;
		setSelectedFont();
	}

	/**
	 * This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      familyNameLabel = new javax.swing.JLabel();
      familyNameTextField = new javax.swing.JTextField();
      final DefaultListModel<String> model = new DefaultListModel<>();
      model.addAll(FontHelper.getFamilyNamesAll());
      familyNameList = new javax.swing.JList<>(model);
      familyNameScrollPane = new javax.swing.JScrollPane(familyNameList);
      monospacedCheckBox = new javax.swing.JCheckBox();
      sampleLabel = new javax.swing.JLabel();
      sampleScrollPane = new javax.swing.JScrollPane();
      sampleTextArea = new javax.swing.JTextArea();
      okButton = new javax.swing.JButton();
      cancelButton = new javax.swing.JButton();
      sizeLabel = new javax.swing.JLabel();
      final NumberFormat format = NumberFormat.getInstance();
      final NumberFormatter formatter = new NumberFormatter(format);
      formatter.setValueClass(Integer.class);
      formatter.setMinimum(1);
      formatter.setAllowsInvalid(false);
      //if you want the value to be committed on each keystroke instead of focus lost
      formatter.setCommitsOnValidEdit(true);
      sizeTextField = new JFormattedTextField(formatter);

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
      setResizable(false);

      familyNameLabel.setText("Family name:");

      familyNameTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            familyNameTextFieldKeyReleased(evt);
         }
      });
      familyNameTextField.getDocument()
      .addDocumentListener(new ListSearchTextFieldDocumentHandler(familyNameList));

      familyNameScrollPane.setBackground(java.awt.Color.white);
      familyNameScrollPane.setViewportBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

      familyNameList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
      familyNameList.addListSelectionListener(this::familyNameListValueChanged);
      familyNameScrollPane.setViewportView(familyNameList);

      monospacedCheckBox.setText("Show monospaced fonts only");
      monospacedCheckBox.addActionListener(this::monospacedCheckBoxActionPerformed);

      sampleLabel.setText("Sample:");

      sampleScrollPane.setBackground(java.awt.Color.white);
      sampleScrollPane.setViewportBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

      sampleTextArea.setColumns(20);
      sampleTextArea.setLineWrap(true);
      sampleTextArea.setText(sampleText);
      sampleTextArea.setWrapStyleWord(true);
      sampleTextArea.setCaretPosition(0);
      sampleScrollPane.setViewportView(sampleTextArea);

      okButton.setText("OK");
      okButton.setPreferredSize(new java.awt.Dimension(65, 23));
      okButton.addActionListener(this::okButtonActionPerformed);

      cancelButton.setText("Cancel");
      cancelButton.addActionListener(this::cancelButtonActionPerformed);

      sizeLabel.setText("Size:");

      sizeTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            sizeTextFieldKeyReleased(evt);
         }
      });

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(sampleScrollPane)
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addGap(0, 0, Short.MAX_VALUE)
                  .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(cancelButton))
               .addComponent(familyNameTextField)
               .addComponent(familyNameScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE)
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(familyNameLabel)
                     .addComponent(sampleLabel))
                  .addGap(0, 0, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(monospacedCheckBox)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addComponent(sizeLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(sizeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(familyNameLabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(familyNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(familyNameScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(monospacedCheckBox)
               .addComponent(sizeLabel)
               .addComponent(sizeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(18, 18, 18)
            .addComponent(sampleLabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(sampleScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(cancelButton)
               .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

	private void familyNameTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_familyNameTextFieldKeyReleased
		int index = familyNameList.getSelectedIndex();
		switch(evt.getKeyCode()){
			case KeyEvent.VK_UP -> {
				index --;
				familyNameList.setSelectedIndex(Math.max(index, 0));
			}
			case KeyEvent.VK_DOWN -> {
				index ++;
				final int listSize = familyNameList.getModel().getSize();
				familyNameList.setSelectedIndex(index < listSize? index: listSize - 1);
			}
		}
	}//GEN-LAST:event_familyNameTextFieldKeyReleased

	private void familyNameListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_familyNameListValueChanged
		if(storeSelectedFont())
			setSelectedFont();
	}//GEN-LAST:event_familyNameListValueChanged

	private void monospacedCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_monospacedCheckBoxActionPerformed
		final List<String> fonts = (monospacedCheckBox.isSelected()
			? FontHelper.getFamilyNamesMonospaced()
			: FontHelper.getFamilyNamesAll());

		final DefaultListModel<String> model = (DefaultListModel<String>)familyNameList.getModel();
		model.clear();
		model.addAll(fonts);

		setSelectedFont();
	}//GEN-LAST:event_monospacedCheckBoxActionPerformed

   private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
		onSelection.accept(selectedFont);

		dispose();
   }//GEN-LAST:event_okButtonActionPerformed

   private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
		dispose();
   }//GEN-LAST:event_cancelButtonActionPerformed

   private void sizeTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_sizeTextFieldKeyReleased
		final int code = evt.getKeyCode();
		if(code == KeyEvent.VK_UP){
			final int fontSize = Integer.parseInt(sizeTextField.getText()) + 1;
			sizeTextField.setText(Integer.toString(fontSize));

			selectedFont = selectedFont.deriveFont((float)fontSize);
		}
		else if(code == KeyEvent.VK_DOWN){
			final int fontSize = Integer.parseInt(sizeTextField.getText()) - 1;
			if(fontSize > 0){
				sizeTextField.setText(Integer.toString(fontSize));

				selectedFont = selectedFont.deriveFont((float)fontSize);
			}
		}

		setSampleFont();
   }//GEN-LAST:event_sizeTextFieldKeyReleased

	public static Font getDefaultFont(){
		return DEFAULT_FONT;
	}

	/** Create a new Font object to return as the selected font. */
	private boolean storeSelectedFont(){
		final int familyNameIndex = familyNameList.getSelectedIndex();
		if(familyNameIndex >= 0){
			final String fontFamily = familyNameList.getSelectedValue();
			final int fontSize = (!sizeTextField.getText().isEmpty()? Integer.parseInt(sizeTextField.getText()): 15);
			selectedFont = new Font(fontFamily, Font.PLAIN, fontSize);
		}

		final boolean fontChanged = !selectedFont.equals(previousFont);
		if(fontChanged)
			previousFont = selectedFont;
		return fontChanged;
	}

	/** Set the controls to display the initial font. */
	private void setSelectedFont(){
		setSelectedFontFamily();
		setSampleFont();
	}

	/**
	 * Set the family name of the selected font.
	 */
	private void setSelectedFontFamily(){
		SwingUtilities.invokeLater(() -> {
			sizeTextField.setText(Integer.toString(selectedFont.getSize()));
			final String name = selectedFont.getFamily();
			familyNameTextField.setText(name);
			familyNameList.setSelectedValue(name, true);
		});
	}

	private void setSampleFont(){
		SwingUtilities.invokeLater(() -> {
			final Font sampleFont = Font.decode(selectedFont.getFamily() + "-PLAIN-" + selectedFont.getSize());
			final float size = (!sizeTextField.getText().isEmpty()? Float.parseFloat(sizeTextField.getText()): 15.f);
			sampleTextArea.setFont(sampleFont.deriveFont(size));
		});
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
   private javax.swing.JButton cancelButton;
   private javax.swing.JLabel familyNameLabel;
   private javax.swing.JList<String> familyNameList;
   private javax.swing.JScrollPane familyNameScrollPane;
   private javax.swing.JTextField familyNameTextField;
   private javax.swing.JCheckBox monospacedCheckBox;
   private javax.swing.JButton okButton;
   private javax.swing.JLabel sampleLabel;
   private javax.swing.JScrollPane sampleScrollPane;
   private javax.swing.JTextArea sampleTextArea;
   private javax.swing.JLabel sizeLabel;
   private javax.swing.JTextField sizeTextField;
   // End of variables declaration//GEN-END:variables

}
