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

import io.github.mtrevisan.hunlinter.MainFrame;
import io.github.mtrevisan.hunlinter.gui.FontHelper;
import io.github.mtrevisan.hunlinter.gui.models.SortableListModel;
import io.github.mtrevisan.hunlinter.gui.renderers.DictionarySortCellRenderer;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.services.eventbus.EventBusService;
import io.github.mtrevisan.hunlinter.services.eventbus.EventHandler;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Collection;
import java.util.Objects;


public class DictionarySortDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = -4815599935456195094L;

	private static final double FONT_SIZE_REDUCTION = 0.85;


	private final ParserManager parserManager;
	private final DictionaryParser dicParser;


	public DictionarySortDialog(final ParserManager parserManager, final Frame parent){
		super(parent, "Dictionary sorter", true);

		Objects.requireNonNull(parserManager);

		this.parserManager = parserManager;
		dicParser = parserManager.getDicParser();

		initComponents();

		setCurrentFont();

		reloadDictionaryParser(MainFrame.ACTION_COMMAND_PARSER_RELOAD_DICTIONARY);

		lblMessage.setText("Select a section from the list:");

		EventBusService.subscribe(this);
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      lblMessage = new javax.swing.JLabel();
      entriesScrollPane = new javax.swing.JScrollPane();
      entriesList = new javax.swing.JList<>();
      btnNextUnsortedArea = new javax.swing.JButton();
      btnPreviousUnsortedArea = new javax.swing.JButton();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      lblMessage.setText("…");

      entriesScrollPane.setViewportBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

      entriesList.setFont(FontHelper.getCurrentFont());
      entriesList.setModel(new SortableListModel());
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

	public void setDictionaryEnabled(final boolean enabled){
		entriesList.setEnabled(enabled);
	}

	@EventHandler
	public void reloadDictionaryParser(final Integer actionCommand){
		if(actionCommand != MainFrame.ACTION_COMMAND_PARSER_RELOAD_DICTIONARY)
			return;

		try{
			parserManager.getDicParser().clearBoundaries();

			//reload text
			final int lastVisibleIndex = getFirstVisibleIndex();
			loadLines(parserManager.getDictionaryLines(), lastVisibleIndex);
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}

	private void loadLines(final Collection<String> listData, final int firstVisibleItemIndex){
		final SortableListModel model = (SortableListModel)entriesList.getModel();
		model.replaceAll(listData, 0);

		entriesList.ensureIndexIsVisible(firstVisibleItemIndex);

		//re-render sections
		entriesList.repaint();
	}

	private void setCurrentFont(){
		final Font currentFont = FontHelper.getCurrentFont();
		final Font font = currentFont.deriveFont((float)(currentFont.getSize() * FONT_SIZE_REDUCTION));
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

		final int lastItemIndex = entriesList.getModel().getSize() - 1;
		int boundaryIndex = dicParser.getPreviousBoundaryIndex(lineIndex);
		if(boundaryIndex < 0)
			boundaryIndex = dicParser.getPreviousBoundaryIndex(lastItemIndex);
		final int visibleLines = entriesList.getLastVisibleIndex() - entriesList.getFirstVisibleIndex();
		final int newIndex = Math.max(boundaryIndex + visibleLines, lastItemIndex);
		entriesList.ensureIndexIsVisible(newIndex);

		//correct first item
		entriesList.ensureIndexIsVisible(boundaryIndex);
   }//GEN-LAST:event_btnPreviousUnsortedAreaActionPerformed

	private void setCellRenderer(final ListCellRenderer<String> renderer){
		entriesList.setCellRenderer(renderer);
	}

	public void addListSelectionListener(final ListSelectionListener listener){
		entriesList.addListSelectionListener(listener);
	}

	public int getFirstVisibleIndex(){
		return entriesList.getFirstVisibleIndex();
	}

	public int getSelectedIndex(){
		return entriesList.getSelectedIndex();
	}


	@SuppressWarnings("unused")
	@Serial
	private void writeObject(final ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	@Serial
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
