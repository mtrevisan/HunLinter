package unit731.hunspeller;

import java.awt.*;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.JDialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.gui.GUIUtils;
import unit731.hunspeller.parsers.autocorrect.CorrectionEntry;


public class CorrectionDialog extends JDialog{

	private static final long serialVersionUID = 7630665680331864500L;

	private static final Logger LOGGER = LoggerFactory.getLogger(CorrectionDialog.class);


	private final CorrectionEntry correction;
	private final BiConsumer<String, String> okButtonAction;


	public CorrectionDialog(CorrectionEntry correction, BiConsumer<String, String> okButtonAction, Frame parent){
		super(parent, "Change auto correction for \"" + correction + "\"", true);

		Objects.requireNonNull(parent);
		Objects.requireNonNull(correction);
		Objects.requireNonNull(okButtonAction);

		initComponents();

		incorrectTextField.setFont(GUIUtils.getCurrentFont());
		correctTextField.setFont(GUIUtils.getCurrentFont());

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

      incorrectLabel.setLabelFor(incorrectTextField);
      incorrectLabel.setText("Incorrect form:");

      correctLabel.setLabelFor(correctTextField);
      correctLabel.setText("Correct form:");

      buttonPanel.setPreferredSize(new java.awt.Dimension(600, 45));

      btnOk.setText("Ok");
      btnOk.setMaximumSize(new java.awt.Dimension(65, 23));
      btnOk.setMinimumSize(new java.awt.Dimension(65, 23));
      btnOk.setPreferredSize(new java.awt.Dimension(65, 23));
      btnOk.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            btnOkActionPerformed(evt);
         }
      });

      btnCancel.setText("Cancel");
      btnCancel.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            btnCancelActionPerformed(evt);
         }
      });

      javax.swing.GroupLayout buttonPanelLayout = new javax.swing.GroupLayout(buttonPanel);
      buttonPanel.setLayout(buttonPanelLayout);
      buttonPanelLayout.setHorizontalGroup(
         buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(buttonPanelLayout.createSequentialGroup()
            .addGap(150, 150, 150)
            .addComponent(btnOk, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGap(30, 30, 30)
            .addComponent(btnCancel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGap(150, 150, 150))
      );
      buttonPanelLayout.setVerticalGroup(
         buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(buttonPanelLayout.createSequentialGroup()
            .addGroup(buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(btnOk, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(btnCancel))
            .addContainerGap())
      );

      buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(incorrectLabel)
               .addComponent(correctLabel))
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
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(incorrectLabel)
               .addComponent(incorrectTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(9, 9, 9)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(correctLabel)
               .addComponent(correctTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(17, 17, 17)
            .addComponent(buttonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

   private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
      dispose();
   }//GEN-LAST:event_btnCancelActionPerformed

   private void btnOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOkActionPerformed
      try{
			final String incorrect = incorrectTextField.getText();
			final String correct = correctTextField.getText();
         okButtonAction.accept(incorrect, correct);
      }
      catch(final IllegalArgumentException e){
         LOGGER.info(Backbone.MARKER_APPLICATION, "Error while changing the auto correction for word {}: {}", correction, e.getMessage());
      }

      dispose();
   }//GEN-LAST:event_btnOkActionPerformed

	@SuppressWarnings("unused")
	private void writeObject(final ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	private void readObject(final ObjectInputStream is) throws IOException{
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
