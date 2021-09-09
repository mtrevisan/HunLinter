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
import io.github.mtrevisan.hunlinter.datastructures.fsa.lookup.DictionaryLookup;
import io.github.mtrevisan.hunlinter.datastructures.fsa.stemming.Dictionary;
import io.github.mtrevisan.hunlinter.gui.FontHelper;
import io.github.mtrevisan.hunlinter.gui.GUIHelper;
import io.github.mtrevisan.hunlinter.gui.JCopyableTable;
import io.github.mtrevisan.hunlinter.gui.dialogs.ThesaurusMergeDialog;
import io.github.mtrevisan.hunlinter.gui.models.ThesaurusTableModel;
import io.github.mtrevisan.hunlinter.gui.renderers.TableRenderer;
import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.thesaurus.DuplicationResult;
import io.github.mtrevisan.hunlinter.parsers.thesaurus.SynonymsEntry;
import io.github.mtrevisan.hunlinter.parsers.thesaurus.ThesaurusEntry;
import io.github.mtrevisan.hunlinter.parsers.thesaurus.ThesaurusParser;
import io.github.mtrevisan.hunlinter.parsers.vos.AffixEntry;
import io.github.mtrevisan.hunlinter.services.eventbus.EventHandler;
import io.github.mtrevisan.hunlinter.services.log.ExceptionHelper;
import io.github.mtrevisan.hunlinter.services.system.Debouncer;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;


public class ThesaurusLayeredPane extends JLayeredPane{

	@Serial
	private static final long serialVersionUID = -6844935166825095145L;

	private static final Logger LOGGER = LoggerFactory.getLogger(ThesaurusLayeredPane.class);

	private static final int DEBOUNCER_INTERVAL = 600;


	private final Debouncer<ThesaurusLayeredPane> debouncer = new Debouncer<>(this::filterThesaurus, DEBOUNCER_INTERVAL);

	private JMenuItem popupMergeMenuItem;

	private final ParserManager parserManager;

	private final FutureTask<JFileChooser> futureOpenDictionaryFSAFileChooser;
	private DictionaryLookup dictionaryLookup;
	private String formerFilterThesaurusText;


	public ThesaurusLayeredPane(final ParserManager parserManager){
		Objects.requireNonNull(parserManager, "Parser manager cannot be null");

		this.parserManager = parserManager;


		initComponents();


		futureOpenDictionaryFSAFileChooser = JavaHelper.createFuture(() -> {
			final JFileChooser openPoSDictionaryFileChooser = new JFileChooser();
			openPoSDictionaryFileChooser.setFileFilter(new FileNameExtensionFilter("FSA files", "dict"));
			openPoSDictionaryFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			return openPoSDictionaryFileChooser;
		});


		//add "fontable" property
		FontHelper.addFontableProperty(table, synonymsTextField);

		GUIHelper.addUndoManager(synonymsTextField);

		try{
			final JPopupMenu copyPopupMenu = new JPopupMenu();
			copyPopupMenu.add(GUIHelper.createPopupCopyMenu(copyPopupMenu, GUIHelper::copyCallback));
			GUIHelper.addPopupMenu(copyPopupMenu, synonymsRecordedValueLabel);

			final JPopupMenu mergeCopyRemovePopupMenu = new JPopupMenu();
			popupMergeMenuItem = GUIHelper.createPopupMergeMenu(mergeCopyRemovePopupMenu, this::mergeThesaurusRow);
			popupMergeMenuItem.setEnabled(false);
			mergeCopyRemovePopupMenu.add(popupMergeMenuItem);
			mergeCopyRemovePopupMenu.add(GUIHelper.createPopupCopyMenu(mergeCopyRemovePopupMenu, GUIHelper::copyCallback));
			mergeCopyRemovePopupMenu.add(GUIHelper.createPopupRemoveMenu(mergeCopyRemovePopupMenu, this::removeSelectedRows));
			final JPopupMenu copyRemovePopupMenu = new JPopupMenu();
			copyRemovePopupMenu.add(GUIHelper.createPopupCopyMenu(copyRemovePopupMenu, GUIHelper::copyCallback));
			copyRemovePopupMenu.add(GUIHelper.createPopupRemoveMenu(copyRemovePopupMenu, this::removeSelectedRows));
			GUIHelper.addPopupMenu(mergeCopyRemovePopupMenu, table);
		}
		catch(final IOException ignored){}
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      synonymsLabel = new javax.swing.JLabel();
      synonymsTextField = new javax.swing.JTextField();
      addButton = new javax.swing.JButton();
      scrollPane = new javax.swing.JScrollPane();
      table = new MyJCopyableTable();
      synonymsRecordedLabel = new javax.swing.JLabel();
      synonymsRecordedValueLabel = new javax.swing.JLabel();

      synonymsLabel.setText("New definition:");

		final Font currentFont = FontHelper.getCurrentFont();

		synonymsTextField.setFont(currentFont);
      synonymsTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            synonymsTextFieldKeyReleased(evt);
         }
      });

      addButton.setMnemonic('A');
      addButton.setText("Add");
      addButton.setEnabled(false);
      addButton.addActionListener(this::addButtonActionPerformed);

      table.setFont(currentFont);
      table.setModel(new ThesaurusTableModel());
      table.setRowHeight(24);
      table.setRowSorter(new TableRowSorter<>((ThesaurusTableModel)table.getModel()));
      table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
      table.setShowHorizontalLines(false);
      table.setShowVerticalLines(false);
      table.getTableHeader().setReorderingAllowed(false);
      //listen for row removal
      final KeyStroke cancelKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
      table.registerKeyboardAction(event -> removeSelectedRowsFromThesaurus(), cancelKeyStroke, JComponent.WHEN_FOCUSED);
      final KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, false);
      table.registerKeyboardAction(event -> GUIHelper.copyToClipboard((JCopyableTable)table), copyKeyStroke, JComponent.WHEN_FOCUSED);
      GUIHelper.addScrollToFirstRow(table);
      GUIHelper.addScrollToLastRow(table);

      final TableCellRenderer theCellRenderer = new TableRenderer();
      table.getColumnModel().getColumn(0).setMinWidth(150);
      table.getColumnModel().getColumn(0).setMaxWidth(300);
      table.getColumnModel().getColumn(1).setCellRenderer(theCellRenderer);
      scrollPane.setViewportView(table);

      synonymsRecordedLabel.setText("Synonyms recorded:");

      synonymsRecordedValueLabel.setText("…");

      setLayer(synonymsLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(synonymsTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(addButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(scrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(synonymsRecordedLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(synonymsRecordedValueLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 909, Short.MAX_VALUE)
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addComponent(synonymsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(synonymsTextField)
                  .addGap(18, 18, 18)
                  .addComponent(addButton))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(synonymsRecordedLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(synonymsRecordedValueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 805, Short.MAX_VALUE)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(synonymsLabel)
               .addComponent(synonymsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(addButton))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(synonymsRecordedLabel)
               .addComponent(synonymsRecordedValueLabel))
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents

   private void synonymsTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_synonymsTextFieldKeyReleased
      debouncer.call(this);
   }//GEN-LAST:event_synonymsTextFieldKeyReleased

   private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
      try{
         //try adding the synonyms
         final String synonyms = synonymsTextField.getText();
         final Predicate<String> duplicatesDiscriminator = message -> {
            final int responseOption = JOptionPane.showConfirmDialog(this,
               "There is some duplicates with same part-of-speech and definition(s) '" + message
               + "'.\nForce insertion?", "Duplicate detected",
               JOptionPane.YES_NO_OPTION);
            return (responseOption == JOptionPane.YES_OPTION);
         };
			final ThesaurusParser theParser = parserManager.getTheParser();
			final DuplicationResult<ThesaurusEntry> duplicationResult = theParser.insertSynonyms(synonyms, duplicatesDiscriminator);
         if(duplicationResult.isForceInsertion()){
            //if everything's ok update the table and the sorter…
            final ThesaurusTableModel dm = (ThesaurusTableModel)table.getModel();
            dm.setSynonyms(theParser.getSynonymsDictionary());
            dm.fireTableDataChanged();

            formerFilterThesaurusText = null;
            synonymsTextField.setText(null);
            synonymsTextField.requestFocusInWindow();
            popupMergeMenuItem.setEnabled(false);
            @SuppressWarnings("unchecked")
            final TableRowSorter<ThesaurusTableModel> sorter = (TableRowSorter<ThesaurusTableModel>)table.getRowSorter();
            sorter.setRowFilter(null);

            updateSynonymsCounter();

            //… and save the files
            parserManager.storeThesaurusFiles();
         }
         else{
            synonymsTextField.requestFocusInWindow();

				final StringJoiner duplicatedWords = new StringJoiner(", ");
				for(final ThesaurusEntry thesaurusEntry : duplicationResult.getDuplicates())
					duplicatedWords.add(thesaurusEntry.getDefinition());
            LOGGER.info(ParserManager.MARKER_APPLICATION, "Duplicate detected: {}", duplicatedWords);
         }
      }
      catch(final IOException ioe){
         LOGGER.info(ParserManager.MARKER_APPLICATION, "Insertion error: {}", ioe.getMessage());
      }
   }//GEN-LAST:event_addButtonActionPerformed

	@EventHandler
	@SuppressWarnings({"unused", "NumberEquality"})
	public final void initialize(final Integer actionCommand){
		if(actionCommand != MainFrame.ACTION_COMMAND_INITIALIZE)
			return;

		final String language = parserManager.getLanguage();

		final Comparator<String> comparator = Comparator.comparingInt(String::length)
			.thenComparing(BaseBuilder.getComparator(language));
		final Comparator<AffixEntry> comparatorAffix = Comparator.comparingInt((AffixEntry entry) -> entry.toString().length())
			.thenComparing((entry0, entry1) -> BaseBuilder.getComparator(language).compare(entry0.toString(), entry1.toString()));
		GUIHelper.addSorterToTable(table, comparator, comparatorAffix);

		try{
			//thesaurus file:
			final ThesaurusParser theParser = parserManager.getTheParser();
			if(theParser != null && theParser.getSynonymsCount() > 0){
				GUIHelper.addSorterToTable(table, comparator, null);

				final ThesaurusTableModel dm = (ThesaurusTableModel)table.getModel();
				dm.setSynonyms(theParser.getSynonymsDictionary());
				updateSynonymsCounter();
			}
		}
		catch(final RuntimeException re){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "A bad error occurred: {}", re.getMessage());

			LOGGER.error("A bad error occurred", re);
		}
	}

	@EventHandler
	@SuppressWarnings({"unused", "NumberEquality"})
	public final void clear(final Integer actionCommand){
		if(actionCommand != MainFrame.ACTION_COMMAND_GUI_CLEAR_ALL && actionCommand != MainFrame.ACTION_COMMAND_GUI_CLEAR_THESAURUS)
			return;

		synonymsTextField.setText(null);
		popupMergeMenuItem.setEnabled(false);

		formerFilterThesaurusText = null;
		((DefaultRowSorter<ThesaurusTableModel, Integer>)table.getRowSorter()).setRowFilter(null);
		final ThesaurusTableModel dm = (ThesaurusTableModel)table.getModel();
		dm.setSynonyms(null);
	}

	private void filterThesaurus(){
		final String unmodifiedSearchText = synonymsTextField.getText().trim();

		popupMergeMenuItem.setEnabled(StringUtils.isNotBlank(unmodifiedSearchText));

		if(unmodifiedSearchText.equals(formerFilterThesaurusText))
			return;

		formerFilterThesaurusText = unmodifiedSearchText;

		final Pair<String[], String[]> pair = ThesaurusParser.extractComponentsForFilter(unmodifiedSearchText);
		//if text to be inserted is already fully contained into the thesaurus, do not enable the button
		final boolean alreadyContained = parserManager.getTheParser().contains(pair.getLeft(), pair.getRight());
		addButton.setEnabled(!alreadyContained);

		@SuppressWarnings("unchecked")
		final TableRowSorter<ThesaurusTableModel> sorter = (TableRowSorter<ThesaurusTableModel>)table.getRowSorter();
		if(StringUtils.isNotBlank(unmodifiedSearchText)){
			final Pair<String, String> searchText = ThesaurusParser.prepareTextForFilter(pair.getLeft(), pair.getRight());
			JavaHelper.executeOnEventDispatchThread(() -> sorter.setRowFilter(RowFilter.regexFilter(searchText.getRight())));
		}
		else
			sorter.setRowFilter(null);
	}

	public final void mergeThesaurusRow(final Component invoker){
		final int selectedRow = table.convertRowIndexToModel(table.getSelectedRow());
		final ThesaurusTableModel dm = (ThesaurusTableModel)table.getModel();
		final ThesaurusEntry synonyms = dm.getSynonymsAt(selectedRow);
		final SynonymsEntry newSynonyms = new SynonymsEntry(synonymsTextField.getText());

		//filter synonyms with same part-of-speech
		final List<SynonymsEntry> filteredSynonymsEntries = new ArrayList<>(0);
		for(final SynonymsEntry syns : synonyms.getSynonyms())
			if(syns.hasSamePartOfSpeeches(newSynonyms.getPartOfSpeeches()))
				filteredSynonymsEntries.add(syns);
		if(filteredSynonymsEntries.isEmpty())
			JOptionPane.showMessageDialog(null,
				"No synonyms with same part-of-speech present.\r\nCannot merge automatically, do it manually.",
				"Warning", JOptionPane.WARNING_MESSAGE);
		else{
			//show merge dialog
			final ThesaurusMergeDialog dialog = new ThesaurusMergeDialog(synonyms.getDefinition(), newSynonyms,
				filteredSynonymsEntries, null);
			GUIHelper.addCancelByEscapeKey(dialog);
			dialog.setLocationRelativeTo(this);
			dialog.setVisible(true);

			if(dialog.isMerged())
				synonymsTextField.setText(dialog.getMerge());
		}
	}

	public final void removeSelectedRows(final Component invoker){
		removeSelectedRowsFromThesaurus();
	}

	public final void removeSelectedRowsFromThesaurus(){
		try{
			final int selectedRow = table.convertRowIndexToModel(table.getSelectedRow());
			final ThesaurusTableModel dm = (ThesaurusTableModel)table.getModel();
			final String selectedDefinition = (String)dm.getValueAt(selectedRow, 0);
			final String selectedSynonyms = (String)dm.getValueAt(selectedRow, 1);
			final ThesaurusParser theParser = parserManager.getTheParser();
			theParser.deleteDefinitionAndSynonyms(selectedDefinition, selectedSynonyms);

			dm.setSynonyms(theParser.getSynonymsDictionary());
			updateSynonymsCounter();

			//… and save the files
			parserManager.storeThesaurusFiles();


			//redo filtering, that is re-set the state of the button (it may have changed)
			final String unmodifiedSearchText = synonymsTextField.getText();
			if(StringUtils.isNotBlank(unmodifiedSearchText)){
				final Pair<String[], String[]> pair = ThesaurusParser.extractComponentsForFilter(unmodifiedSearchText);
				//if text to be inserted is already fully contained into the thesaurus, do not enable the button
				final boolean alreadyContained = theParser.contains(pair.getLeft(), pair.getRight());
				addButton.setEnabled(!alreadyContained);
			}
		}
		catch(final IOException ioe){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Deletion error: {}", ioe.getMessage());
		}
	}

	private void updateSynonymsCounter(){
		synonymsRecordedValueLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(parserManager.getTheParser().getSynonymsCount()));
	}


	public final void openDictionaryFSAActionPerformed(final ActionEvent evt){
		dictionaryLookup = null;

		final JFileChooser openPoSFSAFileChooser = JavaHelper.waitForFuture(futureOpenDictionaryFSAFileChooser);
		final int projectSelected = openPoSFSAFileChooser.showOpenDialog(this);
		if(projectSelected == JFileChooser.APPROVE_OPTION){
			final File baseFile = openPoSFSAFileChooser.getSelectedFile();
			loadFSAFile(baseFile.toPath());
		}
	}

	private void loadFSAFile(final Path basePath){
		try{
			dictionaryLookup = new DictionaryLookup(Dictionary.read(basePath));
		}
		catch(final IllegalArgumentException | IOException e){
			JOptionPane.showMessageDialog(this, "Error while loading Dictionary FSA\n\n"
				+ ExceptionHelper.getMessageNoLineNumber(e), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public final DictionaryLookup getDictionaryLookup(){
		return dictionaryLookup;
	}

	private static final class MyJCopyableTable extends JCopyableTable{
		@Override
		public String getValueAtRow(final int row){
			final TableModel model = getModel();
			final String definition = (String)model.getValueAt(row, 0);
			final String synonyms = (String)model.getValueAt(row, 1);
			final String[] synonymsByDefinition = StringUtils.splitByWholeSeparator(synonyms, ThesaurusTableModel.TAG_NEW_LINE);
			final StringJoiner sj = new StringJoiner("\r\n");
			for(final String s : synonymsByDefinition)
				sj.add(definition + ": " + GUIHelper.removeHTMLCode(s));
			return sj.toString();
		}
	}


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
   private javax.swing.JButton addButton;
   private javax.swing.JScrollPane scrollPane;
   private javax.swing.JLabel synonymsLabel;
   private javax.swing.JLabel synonymsRecordedLabel;
   private javax.swing.JLabel synonymsRecordedValueLabel;
   private javax.swing.JTextField synonymsTextField;
   private javax.swing.JTable table;
   // End of variables declaration//GEN-END:variables
}
