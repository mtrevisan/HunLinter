package unit731.hunspeller;

import java.awt.Frame;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.swing.JDialog;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.gui.GUIUtils;


public class ThesaurusDuplicatesDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(ThesaurusDuplicatesDialog.class);

	private static final long serialVersionUID = 5718588727397261977L;


	public ThesaurusDuplicatesDialog(List<String> duplicates, Frame parent){
		super(parent, "Duplicates", true);

		Objects.requireNonNull(duplicates);
		Objects.requireNonNull(parent);

		initComponents();


		String content = String.join(StringUtils.LF, duplicates);
		duplicatesTextArea.setText(content);
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      mainScrollPane = new javax.swing.JScrollPane();
      duplicatesTextArea = new javax.swing.JTextArea();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      duplicatesTextArea.setEditable(false);
      duplicatesTextArea.setColumns(20);
      duplicatesTextArea.setLineWrap(true);
      duplicatesTextArea.setRows(1);
      duplicatesTextArea.setWrapStyleWord(true);
      mainScrollPane.setViewportView(duplicatesTextArea);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(mainScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(mainScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

	public static void main(String args[]){
		//<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
		try{
			String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e){
			LOGGER.error(null, e);
		}
		//</editor-fold>

		java.awt.EventQueue.invokeLater(() -> {
			try{
				javax.swing.JFrame parent = new javax.swing.JFrame();
				ThesaurusDuplicatesDialog dialog = new ThesaurusDuplicatesDialog(Arrays.asList("a", "b", "c"), parent);
				GUIUtils.addCancelByEscapeKey(dialog);
				dialog.setLocationRelativeTo(parent);
				dialog.addWindowListener(new java.awt.event.WindowAdapter(){
					@Override
					public void windowClosing(java.awt.event.WindowEvent e){
						System.exit(0);
					}
				});
				dialog.setVisible(true);
			}
			catch(IllegalArgumentException e){
				LOGGER.error(null, e);
			}
		});
	}

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JTextArea duplicatesTextArea;
   private javax.swing.JScrollPane mainScrollPane;
   // End of variables declaration//GEN-END:variables
}
