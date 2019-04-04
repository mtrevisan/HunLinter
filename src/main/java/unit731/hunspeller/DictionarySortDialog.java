package unit731.hunspeller;

import java.awt.Frame;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.gui.DictionarySortCellRenderer;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.services.GUIHelper;


public class DictionarySortDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionarySortDialog.class);

	private static final long serialVersionUID = -4815599935456195094L;


	private final DictionaryParser dicParser;

	private final JList<String> list = new JList<>();


	public DictionarySortDialog(DictionaryParser dicParser, String title, String message, Frame parent){
		super(parent, title, true);

		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(title);
		Objects.requireNonNull(message);

		this.dicParser = dicParser;
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		initComponents();

		init();

		lblMessage.setText(message);

		GUIHelper.addCancelByEscapeKey(this);
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      lblMessage = new javax.swing.JLabel();
      mainScrollPane = new javax.swing.JScrollPane(list);
      btnNextUnsortedArea = new javax.swing.JButton();
      btnPreviousUnsortedArea = new javax.swing.JButton();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      lblMessage.setText("...");

      mainScrollPane.setBackground(java.awt.Color.white);
      mainScrollPane.setViewportBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

      btnNextUnsortedArea.setText("▼");
      btnNextUnsortedArea.setToolTipText("Next unsorted area");
      btnNextUnsortedArea.addActionListener(new java.awt.event.ActionListener() {
         @Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
            btnNextUnsortedAreaActionPerformed(evt);
         }
      });

      btnPreviousUnsortedArea.setText("▲");
      btnPreviousUnsortedArea.setToolTipText("Previous unsorted area");
      btnPreviousUnsortedArea.addActionListener(new java.awt.event.ActionListener() {
         @Override
			public void actionPerformed(java.awt.event.ActionEvent evt) {
            btnPreviousUnsortedAreaActionPerformed(evt);
         }
      });

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
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 422, Short.MAX_VALUE)
                  .addComponent(btnNextUnsortedArea)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(btnPreviousUnsortedArea)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(btnNextUnsortedArea)
               .addComponent(btnPreviousUnsortedArea)
               .addComponent(lblMessage))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(mainScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE)
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

	private void init(){
		ListCellRenderer<String> dicCellRenderer = new DictionarySortCellRenderer(dicParser::getBoundaryIndex);
		setCellRenderer(dicCellRenderer);
	}

   private void btnNextUnsortedAreaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNextUnsortedAreaActionPerformed
		int lineIndex = list.getFirstVisibleIndex();
		int boundaryIndex = dicParser.getNextBoundaryIndex(lineIndex);
		if(boundaryIndex >= 0){
			int visibleLines = list.getLastVisibleIndex() - list.getFirstVisibleIndex();
			boundaryIndex = Math.min(boundaryIndex + visibleLines, list.getModel().getSize() - 1);
		}
		else
			boundaryIndex = 0;
		list.ensureIndexIsVisible(boundaryIndex);
   }//GEN-LAST:event_btnNextUnsortedAreaActionPerformed

   private void btnPreviousUnsortedAreaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPreviousUnsortedAreaActionPerformed
		int lineIndex = list.getFirstVisibleIndex();
		int boundaryIndex = dicParser.getPreviousBoundaryIndex(lineIndex);
		if(boundaryIndex < 0){
			boundaryIndex = dicParser.getPreviousBoundaryIndex(list.getModel().getSize());
			int visibleLines = list.getLastVisibleIndex() - list.getFirstVisibleIndex();
			boundaryIndex = Math.min(boundaryIndex + visibleLines, list.getModel().getSize() - 1);
		}
		list.ensureIndexIsVisible(boundaryIndex);
   }//GEN-LAST:event_btnPreviousUnsortedAreaActionPerformed

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

	private void writeObject(ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(DictionarySortDialog.class.getName());
	}

	private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException{
		throw new NotSerializableException(DictionarySortDialog.class.getName());
	}


	public static void main(String args[]){
		//<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
		try{
			String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex){
			LOGGER.error(null, ex);
		}
		//</editor-fold>

		/* Create and display the dialog */
		java.awt.EventQueue.invokeLater(() -> {
			javax.swing.JFrame parent = new javax.swing.JFrame();
			DictionarySortDialog dialog = new DictionarySortDialog(null, "title", "message", parent);
			dialog.setLocationRelativeTo(parent);
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
   private javax.swing.JButton btnNextUnsortedArea;
   private javax.swing.JButton btnPreviousUnsortedArea;
   private javax.swing.JLabel lblMessage;
   private javax.swing.JScrollPane mainScrollPane;
   // End of variables declaration//GEN-END:variables
}
