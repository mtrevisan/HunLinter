package unit731.hunspeller;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.gui.GUIUtils;
import unit731.hunspeller.parsers.affix.AffixData;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;


/**
 * A dialog to prompt the user for a font. It has a static method to display the dialog and return a
 * new {@code Font} instance.
 */
public class FontChooserDialog extends javax.swing.JDialog{

	private static final long serialVersionUID = -4686780467476615109L;

	private static final Logger LOGGER = LoggerFactory.getLogger(FontChooserDialog.class);

	private static final Integer[] SIZES = {10, 12, 14, 16, 18, 20, 22};

	private static final Font DEFAULT_FONT = new Font("Monospaced", Font.PLAIN, 13);


	private static class ListSearchTextFieldDocumentHandler implements DocumentListener{
		private final JList<String> targetList;

		ListSearchTextFieldDocumentHandler(final JList<String> targetList){
			this.targetList = targetList;
		}

		@Override
		public void insertUpdate(final DocumentEvent e){
			update(e);
		}

		@Override
		public void removeUpdate(final DocumentEvent e){
			update(e);
		}

		@Override
		public void changedUpdate(final DocumentEvent e){
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
						SwingUtilities.invokeLater(() -> targetList.setSelectedIndex(foundIndex));
				}
			}
			catch(final BadLocationException e){
				LOGGER.error("Error while retrieving selection from list", e);
			}
		}
	}


	private Font selectedFont;
	private final Consumer<Font> onSelection;

	private Font previousFont;
	private final String sampleText;


	public FontChooserDialog(final AffixData affixData, final Font initialFont, final Consumer<Font> onSelection,
			final java.awt.Frame parent){
		super(parent, "Font chooser", true);

		Objects.requireNonNull(onSelection);
		Objects.requireNonNull(parent);

		sampleText = affixData.getSampleText();
		GUIUtils.extractFonts(sampleText);

		initComponents();

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
      familyNameScrollPane = new javax.swing.JScrollPane(familyNameList);
      final DefaultListModel<String> model = new DefaultListModel<>();
      for(final String familyName : GUIUtils.getFamilyNamesAll())
      model.addElement(familyName);
      familyNameList = new javax.swing.JList<>(model);
      monospacedCheckBox = new javax.swing.JCheckBox();
      sizeLabel = new javax.swing.JLabel();
      sizeTextField = new javax.swing.JTextField();
      sizeScrollPane = new javax.swing.JScrollPane(sizeList);
      sizeList = new javax.swing.JList<>(SIZES);
      sampleLabel = new javax.swing.JLabel();
      sampleScrollPane = new javax.swing.JScrollPane();
      sampleTextArea = new javax.swing.JTextArea();
      okButton = new javax.swing.JButton();
      cancelButton = new javax.swing.JButton();

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
      familyNameList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
         public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
            familyNameListValueChanged(evt);
         }
      });
      familyNameScrollPane.setViewportView(familyNameList);

      monospacedCheckBox.setText("Show monospaced fonts only");
      monospacedCheckBox.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            monospacedCheckBoxActionPerformed(evt);
         }
      });

      sizeLabel.setText("Size:");

      sizeTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            sizeTextFieldKeyReleased(evt);
         }
      });

      sizeScrollPane.setBackground(java.awt.Color.white);
      sizeScrollPane.setViewportBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      sizeScrollPane.setPreferredSize(new java.awt.Dimension(258, 100));

      sizeList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
      sizeList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
         public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
            sizeListValueChanged(evt);
         }
      });
      sizeScrollPane.setViewportView(sizeList);

      sampleLabel.setText("Sample:");

      sampleScrollPane.setBackground(java.awt.Color.white);
      sampleScrollPane.setViewportBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

      sampleTextArea.setColumns(20);
      sampleTextArea.setLineWrap(true);
      sampleTextArea.setText(sampleText);
      sampleScrollPane.setViewportView(sampleTextArea);

      okButton.setText("OK");
      okButton.setPreferredSize(new java.awt.Dimension(65, 23));
      okButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            okButtonActionPerformed(evt);
         }
      });

      cancelButton.setText("Cancel");
      cancelButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            cancelButtonActionPerformed(evt);
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
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(familyNameScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                     .addComponent(familyNameTextField)
                     .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                           .addComponent(familyNameLabel)
                           .addComponent(monospacedCheckBox))
                        .addGap(0, 0, Short.MAX_VALUE)))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                     .addComponent(sizeTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
                     .addComponent(sizeScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                     .addComponent(sizeLabel)))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(sampleLabel)
                  .addGap(0, 0, Short.MAX_VALUE))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addGap(0, 0, Short.MAX_VALUE)
                  .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(cancelButton)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(familyNameLabel)
               .addComponent(sizeLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(sizeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(familyNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(sizeScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addComponent(familyNameScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(monospacedCheckBox)))
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
			case KeyEvent.VK_UP:
				index --;
				familyNameList.setSelectedIndex(Math.max(index, 0));
				break;

			case KeyEvent.VK_DOWN:
				index ++;
				final int listSize = familyNameList.getModel().getSize();
				familyNameList.setSelectedIndex(index < listSize? index: listSize - 1);
		}
	}//GEN-LAST:event_familyNameTextFieldKeyReleased

	private void familyNameListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_familyNameListValueChanged
		if(createSelectedFont())
			setSelectedFont();
	}//GEN-LAST:event_familyNameListValueChanged

	private void monospacedCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_monospacedCheckBoxActionPerformed
		final DefaultListModel<String> model = (DefaultListModel<String>)familyNameList.getModel();
		model.clear();
		model.addAll(monospacedCheckBox.isSelected()? GUIUtils.getFamilyNamesMonospaced(): GUIUtils.getFamilyNamesAll());
		setSelectedFont();
	}//GEN-LAST:event_monospacedCheckBoxActionPerformed

   private void sizeTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_sizeTextFieldKeyReleased
		if(!sizeTextField.getText().isEmpty() && createSelectedFont())
			setSelectedFont();
   }//GEN-LAST:event_sizeTextFieldKeyReleased

	private void sizeListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_sizeListValueChanged
		sizeTextField.setText(sizeList.getSelectedValue().toString());

		if(createSelectedFont())
			setSelectedFont();
	}//GEN-LAST:event_sizeListValueChanged

   private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
		onSelection.accept(selectedFont);

		dispose();
   }//GEN-LAST:event_okButtonActionPerformed

   private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
		dispose();
   }//GEN-LAST:event_cancelButtonActionPerformed

	public static Font getDefaultFont(){
		return DEFAULT_FONT;
	}

	/** Create a new Font object to return as the selected font */
	private boolean createSelectedFont(){
		final int familyNameIndex = familyNameList.getSelectedIndex();
		final String sizeIndex = sizeTextField.getText();
		if(familyNameIndex >= 0 && StringUtils.isNotEmpty(sizeIndex)){
			final String fontFamily = familyNameList.getSelectedValue();
			final int fontSize = Integer.parseInt(sizeTextField.getText());
			selectedFont = new Font(fontFamily, Font.PLAIN, fontSize);
		}

		final boolean fontChanged = !selectedFont.equals(previousFont);
		if(fontChanged)
			previousFont = selectedFont;
		return fontChanged;
	}

	/** Set the controls to display the initial font */
	private void setSelectedFont(){
		setSelectedFontSize(selectedFont.getSize());
		setSelectedFontFamily(selectedFont.getFamily());
		setSampleFont();
	}

	/**
	 * Set the family name of the selected font.
	 *
	 * @param name  the family name of the selected font.
	 */
	private void setSelectedFontFamily(final String name){
		familyNameTextField.setText(name);
		familyNameList.setSelectedValue(name, true);
	}

	/**
	 * Set the size of the selected font.
	 *
	 * @param size the size of the selected font
	 */
	private void setSelectedFontSize(final int size){
		sizeList.setSelectedValue(size, true);
		sizeTextField.setText(Integer.toString(size));
	}

	private void setSampleFont(){
		final Font sampleFont = Font.decode(selectedFont.getFamily() + "-PLAIN-" + selectedFont.getSize());
		sampleTextArea.setFont(sampleFont);
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
   private javax.swing.JList<Integer> sizeList;
   private javax.swing.JScrollPane sizeScrollPane;
   private javax.swing.JTextField sizeTextField;
   // End of variables declaration//GEN-END:variables

}