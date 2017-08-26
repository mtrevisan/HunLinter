package unit731.hunspeller;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.parsers.thesaurus.MeaningEntry;
import unit731.hunspeller.parsers.thesaurus.ThesaurusEntry;


@Slf4j
public class ThesaurusMeaningsDialog extends JDialog{

	private static final long serialVersionUID = 667526009330291911L;


	private final ThesaurusEntry synonym;
	private final BiConsumer<List<MeaningEntry>, String> okButtonAction;
	private final Resultable resultable;

	private final List<MeaningEntry> meanings;


	public ThesaurusMeaningsDialog(Frame parent, ThesaurusEntry synonym, BiConsumer<List<MeaningEntry>, String> okButtonAction, Resultable resultable){
		super(parent, "Change meanings for \"" + synonym.getSynonym() + "\"", true);

		Objects.nonNull(parent);
		Objects.nonNull(synonym);
		Objects.nonNull(okButtonAction);
		Objects.nonNull(resultable);

		initComponents();

		addCancelByEscapeKey();

		this.synonym = synonym;
		this.okButtonAction = okButtonAction;
		meanings = synonym.getMeanings();
		String content = StringUtils.join(meanings, StringUtils.LF);
		meaningsTextArea.setText(content);

		this.resultable = resultable;

		setLocationRelativeTo(parent);
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      mainScrollPane = new javax.swing.JScrollPane();
      meaningsTextArea = new javax.swing.JTextArea();
      buttonPanel = new javax.swing.JPanel();
      okButton = new javax.swing.JButton();
      cancelButton = new javax.swing.JButton();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      meaningsTextArea.setColumns(20);
      meaningsTextArea.setLineWrap(true);
      meaningsTextArea.setRows(5);
      meaningsTextArea.setWrapStyleWord(true);
      mainScrollPane.setViewportView(meaningsTextArea);

      buttonPanel.setPreferredSize(new java.awt.Dimension(600, 45));

      okButton.setText("Ok");
      okButton.setMaximumSize(new java.awt.Dimension(65, 23));
      okButton.setMinimumSize(new java.awt.Dimension(65, 23));
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

      javax.swing.GroupLayout buttonPanelLayout = new javax.swing.GroupLayout(buttonPanel);
      buttonPanel.setLayout(buttonPanelLayout);
      buttonPanelLayout.setHorizontalGroup(
         buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(buttonPanelLayout.createSequentialGroup()
            .addGap(217, 217, 217)
            .addComponent(okButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGap(30, 30, 30)
            .addComponent(cancelButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGap(223, 223, 223))
      );
      buttonPanelLayout.setVerticalGroup(
         buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, buttonPanelLayout.createSequentialGroup()
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(cancelButton))
            .addContainerGap())
      );

      buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addComponent(buttonPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(mainScrollPane)
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(mainScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(buttonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

   private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
		try{
			String text = meaningsTextArea.getText();
			okButtonAction.accept(meanings, text);
		}
		catch(IllegalArgumentException e){
			resultable.printResultLine("Error while changing the meanings for word \"" + synonym.getSynonym() + "\": " + e.getMessage());
		}

		dispose();
   }//GEN-LAST:event_okButtonActionPerformed

   private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
		dispose();
   }//GEN-LAST:event_cancelButtonActionPerformed

	/** Force the escape key to call the same action as pressing the Cancel button. */
	private void addCancelByEscapeKey(){
		AbstractAction cancelAction = new AbstractAction(){
			private static final long serialVersionUID = -5644390861803492172L;

			@Override
			public void actionPerformed(ActionEvent e){
				dispose();
			}
		};
		KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
		getRootPane().registerKeyboardAction(cancelAction, escapeKey, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	public static void main(String args[]){
		//<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
		try{
			String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex){
			log.error(null, ex);
		}
		//</editor-fold>

		java.awt.EventQueue.invokeLater(() -> {
			try{
				List<MeaningEntry> meanings = Collections.unmodifiableList(Arrays.asList(
					new MeaningEntry("(noun)|bla|bli|blo"),
					new MeaningEntry("(art)|el|la")
				));
				ThesaurusEntry synonym = new ThesaurusEntry("synonym", meanings);
				ThesaurusMeaningsDialog dialog = new ThesaurusMeaningsDialog(new javax.swing.JFrame(), synonym, (means, text) -> {}, null);
				dialog.addWindowListener(new java.awt.event.WindowAdapter(){
					@Override
					public void windowClosing(java.awt.event.WindowEvent e){
						System.exit(0);
					}
				});
				dialog.setVisible(true);
			}
			catch(IllegalArgumentException ex){
				log.error(null, ex);
			}
		});
	}

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JPanel buttonPanel;
   private javax.swing.JButton cancelButton;
   private javax.swing.JScrollPane mainScrollPane;
   private javax.swing.JTextArea meaningsTextArea;
   private javax.swing.JButton okButton;
   // End of variables declaration//GEN-END:variables
}
