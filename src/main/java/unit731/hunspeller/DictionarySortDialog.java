package unit731.hunspeller;

import java.awt.*;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import unit731.hunspeller.gui.DictionarySortCellRenderer;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;


public class DictionarySortDialog extends JDialog{

	private static final long serialVersionUID = -4815599935456195094L;

	private static final double FONT_SIZE_REDUCTION = 0.85;


	private final DictionaryParser dicParser;
	private final int firstVisibleItemIndex;


	public DictionarySortDialog(final DictionaryParser dicParser, final int firstVisibleItemIndex, final Frame parent){
		super(parent, "Dictionary sorter", true);

		Objects.requireNonNull(dicParser);

		this.dicParser = dicParser;
		this.firstVisibleItemIndex = firstVisibleItemIndex;

		initComponents();

		lblMessage.setText("Select a section from the list:");
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      lblMessage = new javax.swing.JLabel();
      entriesScrollPane = new javax.swing.JScrollPane();
      entriesList = new javax.swing.JList<>();
      btnNextUnsortedArea = new javax.swing.JButton();
      btnPreviousUnsortedArea = new javax.swing.JButton();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      lblMessage.setText("...");

      entriesScrollPane.setBackground(java.awt.Color.white);
      entriesScrollPane.setViewportBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

      entriesList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
      entriesScrollPane.setViewportView(entriesList);

      btnNextUnsortedArea.setText("▼");
      btnNextUnsortedArea.setToolTipText("Next unsorted area");
      btnNextUnsortedArea.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            btnNextUnsortedAreaActionPerformed(evt);
         }
      });

      btnPreviousUnsortedArea.setText("▲");
      btnPreviousUnsortedArea.setToolTipText("Previous unsorted area");
      btnPreviousUnsortedArea.addActionListener(new java.awt.event.ActionListener() {
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
               .addComponent(entriesScrollPane)
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
            .addComponent(entriesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE)
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

	public void setCurrentFont(final Font currentFont){
		final Font font = currentFont.deriveFont(Math.round(currentFont.getSize() * FONT_SIZE_REDUCTION));
		final ListCellRenderer<String> dicCellRenderer = new DictionarySortCellRenderer(dicParser::getBoundaryIndex, font);
		setCellRenderer(dicCellRenderer);
	}

   private void btnNextUnsortedAreaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNextUnsortedAreaActionPerformed
		final int lineIndex = entriesList.getFirstVisibleIndex();
		//make line completely visible
		entriesList.ensureIndexIsVisible(lineIndex);

		int boundaryIndex = dicParser.getNextBoundaryIndex(lineIndex);
		if(boundaryIndex < 0)
			boundaryIndex = dicParser.getNextBoundaryIndex(0);
		final int visibleLines = entriesList.getLastVisibleIndex() - entriesList.getFirstVisibleIndex();
		final int newIndex = Math.min(boundaryIndex + visibleLines, entriesList.getModel().getSize() - 1);
		entriesList.ensureIndexIsVisible(newIndex);

		//correct first item
		entriesList.ensureIndexIsVisible(boundaryIndex);
   }//GEN-LAST:event_btnNextUnsortedAreaActionPerformed

   private void btnPreviousUnsortedAreaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPreviousUnsortedAreaActionPerformed
		final int lineIndex = entriesList.getFirstVisibleIndex();
		//make line completely visible
		entriesList.ensureIndexIsVisible(lineIndex);

		int boundaryIndex = dicParser.getPreviousBoundaryIndex(lineIndex);
		if(boundaryIndex < 0)
			boundaryIndex = dicParser.getPreviousBoundaryIndex(entriesList.getModel().getSize() - 1);
		final int visibleLines = entriesList.getLastVisibleIndex() - entriesList.getFirstVisibleIndex();
		final int newIndex = Math.max(boundaryIndex + visibleLines, entriesList.getModel().getSize() - 1);
		entriesList.ensureIndexIsVisible(newIndex);

		//correct first item
		entriesList.ensureIndexIsVisible(boundaryIndex);
   }//GEN-LAST:event_btnPreviousUnsortedAreaActionPerformed

	public void setCellRenderer(ListCellRenderer<String> renderer){
		entriesList.setCellRenderer(renderer);
	}

	public void addListSelectionListener(ListSelectionListener listener){
		entriesList.addListSelectionListener(listener);
	}

	public void setListData(final String[] listData){
		entriesList.setListData(listData);
		if(firstVisibleItemIndex > 0 && firstVisibleItemIndex < entriesList.getModel().getSize())
			entriesList.ensureIndexIsVisible(firstVisibleItemIndex);

		//initialize dictionary
		dicParser.calculateDictionaryBoundaries();
	}

	public int getFirstVisibleIndex(){
		return entriesList.getFirstVisibleIndex();
	}

	public int getSelectedIndex(){
		return entriesList.getSelectedIndex();
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
   private javax.swing.JButton btnNextUnsortedArea;
   private javax.swing.JButton btnPreviousUnsortedArea;
   private javax.swing.JList<String> entriesList;
   private javax.swing.JScrollPane entriesScrollPane;
   private javax.swing.JLabel lblMessage;
   // End of variables declaration//GEN-END:variables

}
