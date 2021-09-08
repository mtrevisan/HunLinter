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
package io.github.mtrevisan.hunlinter.gui.panes;

import io.github.mtrevisan.hunlinter.MainFrame;
import io.github.mtrevisan.hunlinter.actions.OpenFileAction;
import io.github.mtrevisan.hunlinter.gui.FontHelper;
import io.github.mtrevisan.hunlinter.gui.GUIHelper;
import io.github.mtrevisan.hunlinter.gui.JCopyableTable;
import io.github.mtrevisan.hunlinter.gui.dialogs.CorrectionDialog;
import io.github.mtrevisan.hunlinter.gui.models.AutoCorrectTableModel;
import io.github.mtrevisan.hunlinter.gui.renderers.TableRenderer;
import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.autocorrect.AutoCorrectParser;
import io.github.mtrevisan.hunlinter.parsers.autocorrect.CorrectionEntry;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.thesaurus.DuplicationResult;
import io.github.mtrevisan.hunlinter.services.Packager;
import io.github.mtrevisan.hunlinter.services.eventbus.EventHandler;
import io.github.mtrevisan.hunlinter.services.system.Debouncer;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Supplier;


public class AutoCorrectLayeredPane extends JLayeredPane{

	@Serial
	private static final long serialVersionUID = -5833945357934298046L;

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoCorrectLayeredPane.class);

	private static final int DEBOUNCER_INTERVAL = 600;


	private final Debouncer<AutoCorrectLayeredPane> debouncer = new Debouncer<>(this::filterAutoCorrect, DEBOUNCER_INTERVAL);

	private final Packager packager;
	private final ParserManager parserManager;

	private String formerFilterIncorrectText;
	private String formerFilterCorrectText;
	private final JFrame parentFrame;


	public AutoCorrectLayeredPane(final Packager packager, final ParserManager parserManager, final JFrame parentFrame){
		Objects.requireNonNull(packager, "Packager cannot be null");
		Objects.requireNonNull(parserManager, "Parser manager cannot be null");
		Objects.requireNonNull(parentFrame, "Parent frame cannot be null");

		this.packager = packager;
		this.parserManager = parserManager;
		this.parentFrame = parentFrame;


		initComponents();


		//add "fontable" property
		FontHelper.addFontableProperty(table, incorrectTextField, correctTextField);

		GUIHelper.addUndoManager(incorrectTextField, correctTextField);
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      incorrectLabel = new javax.swing.JLabel();
      incorrectTextField = new javax.swing.JTextField();
      toLabel = new javax.swing.JLabel();
      correctLabel = new javax.swing.JLabel();
      correctTextField = new javax.swing.JTextField();
      addButton = new javax.swing.JButton();
      scrollPane = new javax.swing.JScrollPane();
      table = new MyJCopyableTable();
      correctionsRecordedLabel = new javax.swing.JLabel();
      correctionsRecordedValueLabel = new javax.swing.JLabel();
      openAcoButton = new javax.swing.JButton();

      incorrectLabel.setText("Incorrect form:");

		final Font currentFont = FontHelper.getCurrentFont();

		incorrectTextField.setFont(currentFont);
      incorrectTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            incorrectTextFieldKeyReleased(evt);
         }
      });

      toLabel.setText("→");

      correctLabel.setText("Correct form:");

      correctTextField.setFont(currentFont);
      correctTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            correctTextFieldKeyReleased(evt);
         }
      });

      addButton.setMnemonic('A');
      addButton.setText("Add");
      addButton.setEnabled(false);
      addButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            addButtonActionPerformed(evt);
         }
      });

      table.setFont(currentFont);
      table.setModel(new AutoCorrectTableModel());
      table.setRowHeight(24);
      table.setRowSorter(new TableRowSorter<>((AutoCorrectTableModel)table.getModel()));
      table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
      table.setShowHorizontalLines(false);
      table.setShowVerticalLines(false);
      table.getTableHeader().setReorderingAllowed(false);
      //listen for row removal
      final KeyStroke cancelKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
      table.registerKeyboardAction(event -> removeSelectedRowsFromAutoCorrect(), cancelKeyStroke, JComponent.WHEN_FOCUSED);
      final KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, false);
      table.registerKeyboardAction(event -> GUIHelper.copyToClipboard((JCopyableTable)table), copyKeyStroke, JComponent.WHEN_FOCUSED);
      GUIHelper.addScrollToFirstRow(table);
      GUIHelper.addScrollToLastRow(table);

      table.addMouseListener(new MouseAdapter(){
         public void mouseClicked(final MouseEvent e){
            if(e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1){
               final int selectedRow = table.rowAtPoint(e.getPoint());
               table.setRowSelectionInterval(selectedRow, selectedRow);
               final int row = table.convertRowIndexToModel(selectedRow);
               final BiConsumer<String, String> okButtonAction = (incorrect, correct) -> {
                  try{
                     parserManager.getAcoParser().setCorrection(row, incorrect, correct);

                     //… and save the files
                     parserManager.storeAutoCorrectFile();
                  }
                  catch(final Exception ex){
                     LOGGER.info(ParserManager.MARKER_APPLICATION, ex.getMessage());
                  }
               };
               final CorrectionEntry definition = parserManager.getAcoParser().getCorrectionsDictionary().get(row);
               final CorrectionDialog dialog = new CorrectionDialog(definition, okButtonAction, parentFrame);
               GUIHelper.addCancelByEscapeKey(dialog);
               dialog.addWindowListener(new WindowAdapter(){
                  @Override
                  public void windowClosed(final WindowEvent we){
                     table.clearSelection();
                  }
               });
               dialog.setLocationRelativeTo(parentFrame);
               dialog.setVisible(true);
            }
         }
      });

      final TableCellRenderer acoCellRenderer = new TableRenderer();
      table.getColumnModel().getColumn(1).setCellRenderer(acoCellRenderer);
      scrollPane.setViewportView(table);

      correctionsRecordedLabel.setText("Corrections recorded:");

      correctionsRecordedValueLabel.setText("…");

      openAcoButton.setAction(new OpenFileAction(Packager.KEY_FILE_AUTO_CORRECT, packager));
      openAcoButton.setText("Open AutoCorrect");
      openAcoButton.setEnabled(false);

      setLayer(incorrectLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(incorrectTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(toLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(correctLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(correctTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(addButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(scrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(correctionsRecordedLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(correctionsRecordedValueLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(openAcoButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(scrollPane)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(incorrectLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(incorrectTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 330, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addComponent(toLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(correctLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(correctTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 330, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addGap(18, 18, 18)
                  .addComponent(addButton))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(correctionsRecordedLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(correctionsRecordedValueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(openAcoButton)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(incorrectLabel)
               .addComponent(incorrectTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(correctTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(addButton)
               .addComponent(toLabel)
               .addComponent(correctLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(correctionsRecordedLabel)
               .addComponent(correctionsRecordedValueLabel)
               .addComponent(openAcoButton))
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents

   private void incorrectTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_incorrectTextFieldKeyReleased
		debouncer.call(this);
   }//GEN-LAST:event_incorrectTextFieldKeyReleased

   private void correctTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_correctTextFieldKeyReleased
		debouncer.call(this);
   }//GEN-LAST:event_correctTextFieldKeyReleased

   private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
      try{
         //try adding the correction
         final String incorrect = incorrectTextField.getText();
         final String correct = correctTextField.getText();
         final Supplier<Boolean> duplicatesDiscriminator = () -> {
            final int responseOption = JOptionPane.showConfirmDialog(this,
               "There is a duplicate with same incorrect and correct forms.\nForce insertion?", "Duplicate detected",
               JOptionPane.YES_NO_OPTION);
            return (responseOption == JOptionPane.YES_OPTION);
         };
         final DuplicationResult<CorrectionEntry> duplicationResult = parserManager.getAcoParser()
         .insertCorrection(incorrect, correct, duplicatesDiscriminator);
         if(duplicationResult.isForceInsertion()){
            //if everything's ok update the table and the sorter…
            final AutoCorrectTableModel dm = (AutoCorrectTableModel)table.getModel();
            dm.fireTableDataChanged();

            formerFilterIncorrectText = null;
            formerFilterCorrectText = null;
            incorrectTextField.setText(null);
            correctTextField.setText(null);
            addButton.setEnabled(false);
            incorrectTextField.requestFocusInWindow();
            @SuppressWarnings("unchecked")
            final TableRowSorter<AutoCorrectTableModel> sorter = (TableRowSorter<AutoCorrectTableModel>)table.getRowSorter();
            sorter.setRowFilter(null);

            updateAutoCorrectionsCounter();

            //… and save the files
            parserManager.storeAutoCorrectFile();
         }
         else{
            incorrectTextField.requestFocusInWindow();

				final StringJoiner duplicatedWords = new StringJoiner(", ");
				for(final CorrectionEntry correctionEntry : duplicationResult.getDuplicates())
					duplicatedWords.add(correctionEntry.toString());
            LOGGER.info(ParserManager.MARKER_APPLICATION, "Duplicate detected: {}", duplicatedWords);
         }
      }
      catch(final Exception e){
         LOGGER.info(ParserManager.MARKER_APPLICATION, "Insertion error: {}", e.getMessage());
      }
   }//GEN-LAST:event_addButtonActionPerformed

	@EventHandler
	@SuppressWarnings("unused")
	public final void initialize(final Integer actionCommand){
		if(actionCommand != MainFrame.ACTION_COMMAND_INITIALIZE)
			return;

		if(parserManager.getAcoParser().getCorrectionsCounter() > 0){
			final String language = parserManager.getLanguage();
			final Comparator<String> comparator = Comparator.comparingInt(String::length)
				.thenComparing(BaseBuilder.getComparator(language));
			GUIHelper.addSorterToTable(table, comparator, null);

			final AutoCorrectTableModel dm = (AutoCorrectTableModel)table.getModel();
			dm.setCorrections(parserManager.getAcoParser().getCorrectionsDictionary());
			updateAutoCorrectionsCounter();
		}
		openAcoButton.setEnabled(packager.getAutoCorrectFile() != null);
	}

	@EventHandler
	@SuppressWarnings({"unused", "unchecked"})
	public final void clear(final Integer actionCommand){
		if(actionCommand != MainFrame.ACTION_COMMAND_GUI_CLEAR_ALL && actionCommand != MainFrame.ACTION_COMMAND_GUI_CLEAR_AUTO_CORRECT)
			return;

		formerFilterIncorrectText = null;
		formerFilterCorrectText = null;
		incorrectTextField.setText(null);
		correctLabel.setText(null);

		openAcoButton.setEnabled(false);
		((DefaultRowSorter<AutoCorrectTableModel, Integer>)table.getRowSorter()).setRowFilter(null);
		final AutoCorrectTableModel dm = (AutoCorrectTableModel)table.getModel();
		dm.setCorrections(null);
	}

	private void removeSelectedRowsFromAutoCorrect(){
		try{
			final int selectedRow = table.convertRowIndexToModel(table.getSelectedRow());
			parserManager.getAcoParser().deleteCorrection(selectedRow);

			final AutoCorrectTableModel dm = (AutoCorrectTableModel)table.getModel();
			dm.fireTableDataChanged();

			updateAutoCorrectionsCounter();

			//… and save the files
			parserManager.storeAutoCorrectFile();
		}
		catch(final Exception e){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Deletion error: {}", e.getMessage());
		}
	}

	private void filterAutoCorrect(){
		final String unmodifiedIncorrectText = incorrectTextField.getText().trim();
		final String unmodifiedCorrectText = correctTextField.getText().trim();
		if(formerFilterIncorrectText != null && formerFilterIncorrectText.equals(unmodifiedIncorrectText)
			&& formerFilterCorrectText != null && formerFilterCorrectText.equals(unmodifiedCorrectText))
			return;

		formerFilterIncorrectText = unmodifiedIncorrectText;
		formerFilterCorrectText = unmodifiedCorrectText;

		final Pair<String, String> pair = AutoCorrectParser.extractComponentsForFilter(unmodifiedIncorrectText,
			unmodifiedCorrectText);
		final String incorrect = pair.getLeft();
		final String correct = pair.getRight();
		//if text to be inserted is already fully contained into the thesaurus, do not enable the button
		final boolean alreadyContained = parserManager.getAcoParser().contains(incorrect, correct);
		addButton.setEnabled(StringUtils.isNotBlank(unmodifiedIncorrectText) && StringUtils.isNotBlank(unmodifiedCorrectText)
			&& !unmodifiedIncorrectText.equals(unmodifiedCorrectText) && !alreadyContained);

		@SuppressWarnings("unchecked")
		final TableRowSorter<AutoCorrectTableModel> sorter = (TableRowSorter<AutoCorrectTableModel>)table.getRowSorter();
		if(StringUtils.isNotBlank(unmodifiedIncorrectText) || StringUtils.isNotBlank(unmodifiedCorrectText)){
			final Pair<String, String> searchText = AutoCorrectParser.prepareTextForFilter(incorrect, correct);
			final RowFilter<AutoCorrectTableModel, Integer> filterIncorrect = RowFilter.regexFilter(searchText.getLeft(), 0);
			final RowFilter<AutoCorrectTableModel, Integer> filterCorrect = RowFilter.regexFilter(searchText.getRight(), 1);
			JavaHelper.executeOnEventDispatchThread(() -> sorter.setRowFilter(RowFilter.andFilter(Arrays.asList(filterIncorrect,
				filterCorrect))));
		}
		else
			sorter.setRowFilter(null);
	}

	private void updateAutoCorrectionsCounter(){
		correctionsRecordedValueLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(parserManager.getAcoParser().getCorrectionsCounter()));
	}

	private static final class MyJCopyableTable extends JCopyableTable{
		@Override
		public String getValueAtRow(final int row){
			final TableModel model = getModel();
			final String incorrect = (String)model.getValueAt(row, 0);
			final String correct = (String)model.getValueAt(row, 1);
			return incorrect + " > " + correct;
		}
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
   private javax.swing.JButton addButton;
   private javax.swing.JLabel correctLabel;
   private javax.swing.JTextField correctTextField;
   private javax.swing.JLabel correctionsRecordedLabel;
   private javax.swing.JLabel correctionsRecordedValueLabel;
   private javax.swing.JLabel incorrectLabel;
   private javax.swing.JTextField incorrectTextField;
   private javax.swing.JButton openAcoButton;
   private javax.swing.JScrollPane scrollPane;
   private javax.swing.JTable table;
   private javax.swing.JLabel toLabel;
   // End of variables declaration//GEN-END:variables
}
