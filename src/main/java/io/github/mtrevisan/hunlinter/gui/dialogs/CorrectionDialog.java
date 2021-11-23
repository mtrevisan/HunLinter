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
import io.github.mtrevisan.hunlinter.parsers.autocorrect.CorrectionEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Objects;
import java.util.function.BiConsumer;


public class CorrectionDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 7630665680331864500L;

	private static final Logger LOGGER = LoggerFactory.getLogger(CorrectionDialog.class);


	private final CorrectionEntry correction;
	private final BiConsumer<String, String> okButtonAction;


	public CorrectionDialog(final CorrectionEntry correction, final BiConsumer<String, String> okButtonAction, final Frame parent){
		super(parent, "Change auto correction for " + correction, true);

		Objects.requireNonNull(parent, "Parent cannot be null");
		Objects.requireNonNull(correction, "Correction cannot be null");
		Objects.requireNonNull(okButtonAction, "Ok button action cannot be null");

		initComponents();

		this.correction = correction;
		this.okButtonAction = okButtonAction;
		incorrectTextField.setText(correction.getIncorrectForm());
		correctTextField.setText(correction.getCorrectForm());
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      incorrectLabel = new javax.swing.JLabel();
      incorrectTextField = new javax.swing.JTextField();
      correctLabel = new javax.swing.JLabel();
      correctTextField = new javax.swing.JTextField();
      buttonPanel = new javax.swing.JPanel();
      btnOk = new javax.swing.JButton();
      btnCancel = new javax.swing.JButton();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
      setResizable(false);

      incorrectLabel.setLabelFor(incorrectTextField);
      incorrectLabel.setText("Incorrect form:");
      incorrectLabel.setPreferredSize(new java.awt.Dimension(74, 17));

		final Font currentFont = FontHelper.getCurrentFont();

		incorrectTextField.setFont(currentFont);

      correctLabel.setLabelFor(correctTextField);
      correctLabel.setText("Correct form:");
      correctLabel.setPreferredSize(new java.awt.Dimension(66, 17));

      correctTextField.setFont(currentFont);

      buttonPanel.setPreferredSize(new java.awt.Dimension(600, 45));

      btnOk.setText("Ok");
      btnOk.setMaximumSize(new java.awt.Dimension(65, 23));
      btnOk.setMinimumSize(new java.awt.Dimension(65, 23));
      btnOk.setPreferredSize(new java.awt.Dimension(65, 23));
      btnOk.addActionListener(this::btnOkActionPerformed);

      btnCancel.setText("Cancel");
      btnCancel.addActionListener(this::btnCancelActionPerformed);

      final javax.swing.GroupLayout buttonPanelLayout = new javax.swing.GroupLayout(buttonPanel);
      buttonPanel.setLayout(buttonPanelLayout);
      buttonPanelLayout.setHorizontalGroup(
         buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(buttonPanelLayout.createSequentialGroup()
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(btnOk, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGap(31, 31, 31)
            .addComponent(btnCancel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      );
      buttonPanelLayout.setVerticalGroup(
         buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(buttonPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(btnOk, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(btnCancel))
            .addContainerGap())
      );

      buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

      final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(incorrectLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(correctLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
               .addComponent(incorrectTextField)
               .addComponent(correctTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 370, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
         .addComponent(buttonPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 474, Short.MAX_VALUE)
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(incorrectLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(incorrectTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(9, 9, 9)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(correctTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(correctLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(buttonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

   private void btnCancelActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
      dispose();
   }//GEN-LAST:event_btnCancelActionPerformed

   private void btnOkActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOkActionPerformed
      try{
			final String incorrect = incorrectTextField.getText();
			final String correct = correctTextField.getText();
         okButtonAction.accept(incorrect, correct);
      }
      catch(final RuntimeException re){
         LOGGER.error(ParserManager.MARKER_APPLICATION, "Error while changing the auto correction for word {}: {}", correction,
				re.getMessage());
      }

      dispose();
   }//GEN-LAST:event_btnOkActionPerformed


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
   private javax.swing.JButton btnCancel;
   private javax.swing.JButton btnOk;
   private javax.swing.JPanel buttonPanel;
   private javax.swing.JLabel correctLabel;
   private javax.swing.JTextField correctTextField;
   private javax.swing.JLabel incorrectLabel;
   private javax.swing.JTextField incorrectTextField;
   // End of variables declaration//GEN-END:variables
}
