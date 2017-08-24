package unit731.hunspeller;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.thesaurus.MeaningEntry;


@Slf4j
public class ThesaurusDuplicatesDialog extends JDialog{

	private static final long serialVersionUID = 5718588727397261977L;

	private static final String CANCEL_ACTION_KEY = "CANCEL_ACTION_KEY";


	public ThesaurusDuplicatesDialog(Frame parent, List<String> duplicates){
		super(parent, "Thesaurus duplicates", true);

		Objects.nonNull(parent);
		Objects.nonNull(duplicates);

		initComponents();

		addCancelByEscapeKey();

		String content = String.join(StringUtils.LF, duplicates);
		duplicatesTextArea.setText(content);

		setLocationRelativeTo(parent);
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      mainScrollPane = new javax.swing.JScrollPane();
      duplicatesTextArea = new javax.swing.JTextArea();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      duplicatesTextArea.setEditable(false);
      duplicatesTextArea.setColumns(20);
      duplicatesTextArea.setLineWrap(true);
      duplicatesTextArea.setRows(5);
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

	/** Force the escape key to call the same action as pressing the Cancel button. */
	private void addCancelByEscapeKey(){
		KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
		InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		inputMap.put(escapeKey, CANCEL_ACTION_KEY);
		AbstractAction cancelAction = new AbstractAction(){
			private static final long serialVersionUID = 3435241037784957190L;

			@Override
			public void actionPerformed(ActionEvent e){
				dispose();
			}
		};
		getRootPane().getActionMap().put(CANCEL_ACTION_KEY, cancelAction);
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

		/* Create and display the dialog */
		java.awt.EventQueue.invokeLater(() -> {
			try{
				List<MeaningEntry> meanings = new ArrayList<>();
				meanings.add(new MeaningEntry("(noun)|bla|bli|blo"));
				meanings.add(new MeaningEntry("(art)|el|la"));
				ThesaurusDuplicatesDialog dialog = new ThesaurusDuplicatesDialog(new javax.swing.JFrame(), Arrays.asList("a", "b", "c"));
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
   private javax.swing.JTextArea duplicatesTextArea;
   private javax.swing.JScrollPane mainScrollPane;
   // End of variables declaration//GEN-END:variables
}
