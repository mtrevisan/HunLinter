package unit731.hunspeller;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionListener;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class DictionarySortDialog extends javax.swing.JDialog{

	private final JList<String> list;


	public DictionarySortDialog(Frame parent, String title, String message){
		super(parent, true);

		Objects.nonNull(title);
		Objects.nonNull(message);

		list = new JList<>();
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		initComponents();

		setTitle(title);
		lblMessage.setText(message);

		addCancelByEscapeKey();

		setLocationRelativeTo(parent);
	}

	@SuppressWarnings("unchecked")
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      lblMessage = new javax.swing.JLabel();
      mainScrollPane = new javax.swing.JScrollPane(list);

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      lblMessage.setText("...");

      mainScrollPane.setBackground(java.awt.Color.white);
      mainScrollPane.setViewportBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(mainScrollPane)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(lblMessage)
                  .addGap(0, 518, Short.MAX_VALUE)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(lblMessage)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(mainScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 328, Short.MAX_VALUE)
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

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

	public void setCellRenderer(ListCellRenderer<String> renderer){
		list.setCellRenderer(renderer);
	}

	public void addListSelectionListener(ListSelectionListener listener){
		list.addListSelectionListener(listener);
	}

	public void setListData(String[] listData){
		list.setListData(listData);
	}

	public int getSelectedIndex(){
		return list.getSelectedIndex();
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
			DictionarySortDialog dialog = new DictionarySortDialog(new javax.swing.JFrame(), "title", "message");
			dialog.setListData(new String[]{"a", "b", "c"});
			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setVisible(true);
		});
	}

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JLabel lblMessage;
   private javax.swing.JScrollPane mainScrollPane;
   // End of variables declaration//GEN-END:variables
}
