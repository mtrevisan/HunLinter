package unit731.hunlinter;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.gui.AscendingDescendingUnsortedTableRowSorter;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.gui.JCopyableTable;
import unit731.hunlinter.gui.TableRenderer;
import unit731.hunlinter.gui.ThesaurusTableModel;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.thesaurus.DuplicationResult;
import unit731.hunlinter.parsers.thesaurus.SynonymsEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.services.system.Debouncer;
import unit731.hunlinter.services.system.JavaHelper;


public class ThesaurusLayeredPane extends JLayeredPane{

	private static final Logger LOGGER = LoggerFactory.getLogger(ThesaurusLayeredPane.class);

	private static final int DEBOUNCER_INTERVAL = 600;


	private final Debouncer<ThesaurusLayeredPane> debouncer = new Debouncer<>(this::filterThesaurus, DEBOUNCER_INTERVAL);

	private JMenuItem popupMergeMenuItem;

	private final ParserManager parserManager;

	private String formerFilterThesaurusText;


	public ThesaurusLayeredPane(final ParserManager parserManager){
		Objects.requireNonNull(parserManager);

		this.parserManager = parserManager;


		initComponents();


		//add "fontable" property
		GUIUtils.addFontableProperty(theTable, theSynonymsTextField);

		GUIUtils.addUndoManager(theSynonymsTextField);

		try{
			//FIXME
//			final int iconSize = hypRulesOutputLabel.getHeight();
//			final int iconSize = dicTotalProductionsValueLabel.getHeight();
			final int iconSize = 17;
			final JPopupMenu copyPopupMenu = new JPopupMenu();
			copyPopupMenu.add(GUIUtils.createPopupCopyMenu(iconSize, copyPopupMenu, GUIUtils::copyCallback));
			final JPopupMenu mergeCopyRemovePopupMenu = new JPopupMenu();
			popupMergeMenuItem = GUIUtils.createPopupMergeMenu(iconSize, mergeCopyRemovePopupMenu, this::mergeThesaurusRow);
			popupMergeMenuItem.setEnabled(false);
			mergeCopyRemovePopupMenu.add(popupMergeMenuItem);
			mergeCopyRemovePopupMenu.add(GUIUtils.createPopupCopyMenu(iconSize, mergeCopyRemovePopupMenu, GUIUtils::copyCallback));
			mergeCopyRemovePopupMenu.add(GUIUtils.createPopupRemoveMenu(iconSize, mergeCopyRemovePopupMenu, this::removeSelectedRows));
			final JPopupMenu copyRemovePopupMenu = new JPopupMenu();
			copyRemovePopupMenu.add(GUIUtils.createPopupCopyMenu(iconSize, copyRemovePopupMenu, GUIUtils::copyCallback));
			copyRemovePopupMenu.add(GUIUtils.createPopupRemoveMenu(iconSize, copyRemovePopupMenu, this::removeSelectedRows));
			GUIUtils.addPopupMenu(copyPopupMenu, theSynonymsRecordedOutputLabel);
			GUIUtils.addPopupMenu(mergeCopyRemovePopupMenu, theTable);
		}
		catch(final IOException ignored){}
	}

	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      theSynonymsLabel = new javax.swing.JLabel();
      theSynonymsTextField = new javax.swing.JTextField();
      theAddButton = new javax.swing.JButton();
      theScrollPane = new javax.swing.JScrollPane();
      theTable = new JCopyableTable(){
         @Override
         public String getValueAtRow(final int row){
            final TableModel model = getModel();
            final String definition = (String)model.getValueAt(row, 0);
            final String synonyms = (String)model.getValueAt(row, 1);
            final String[] synonymsByDefinition = StringUtils.splitByWholeSeparator(synonyms, ThesaurusTableModel.TAG_NEW_LINE);
            return Arrays.stream(synonymsByDefinition)
            .map(GUIUtils::removeHTMLCode)
            .map(syns -> definition + ": " + syns)
            .collect(Collectors.joining("\r\n"));
         }
      };
      theSynonymsRecordedLabel = new javax.swing.JLabel();
      theSynonymsRecordedOutputLabel = new javax.swing.JLabel();

      theSynonymsLabel.setText("New definition:");

      theSynonymsTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            theSynonymsTextFieldKeyReleased(evt);
         }
      });

      theAddButton.setMnemonic('A');
      theAddButton.setText("Add");
      theAddButton.setEnabled(false);
      theAddButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            theAddButtonActionPerformed(evt);
         }
      });

      theTable.setModel(new ThesaurusTableModel());
      theTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
      theTable.setRowSorter(new TableRowSorter<>((ThesaurusTableModel)theTable.getModel()));
      theTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
      theTable.setShowHorizontalLines(false);
      theTable.setShowVerticalLines(false);
      theTable.getColumnModel().getColumn(0).setMinWidth(200);
      theTable.getColumnModel().getColumn(0).setMaxWidth(500);
      //listen for row removal
      KeyStroke cancelKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
      theTable.registerKeyboardAction(event -> removeSelectedRowsFromThesaurus(), cancelKeyStroke, JComponent.WHEN_FOCUSED);
      KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, false);
      theTable.registerKeyboardAction(event -> GUIUtils.copyToClipboard((JCopyableTable)theTable), copyKeyStroke, JComponent.WHEN_FOCUSED);

      TableRenderer theCellRenderer = new TableRenderer();
      theTable.getColumnModel().getColumn(1).setCellRenderer(theCellRenderer);
      theScrollPane.setViewportView(theTable);

      theSynonymsRecordedLabel.setText("Synonyms recorded:");

      theSynonymsRecordedOutputLabel.setText("…");

      setLayer(theSynonymsLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(theSynonymsTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(theAddButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(theScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(theSynonymsRecordedLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(theSynonymsRecordedOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(theScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 909, Short.MAX_VALUE)
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addComponent(theSynonymsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(theSynonymsTextField)
                  .addGap(18, 18, 18)
                  .addComponent(theAddButton))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(theSynonymsRecordedLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(theSynonymsRecordedOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 805, Short.MAX_VALUE)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(theSynonymsLabel)
               .addComponent(theSynonymsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(theAddButton))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(theScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(theSynonymsRecordedLabel)
               .addComponent(theSynonymsRecordedOutputLabel))
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents

   private void theSynonymsTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_theSynonymsTextFieldKeyReleased
      debouncer.call(this);
   }//GEN-LAST:event_theSynonymsTextFieldKeyReleased

   private void theAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_theAddButtonActionPerformed
      try{
         //try adding the synonyms
         final String synonyms = theSynonymsTextField.getText();
         final Function<String, Boolean> duplicatesDiscriminator = message -> {
            final int responseOption = JOptionPane.showConfirmDialog(this,
               "There is some duplicates with same part–of–speech and definition(s) '" + message
               + "'.\nForce insertion?", "Duplicate detected",
               JOptionPane.YES_NO_OPTION);
            return (responseOption == JOptionPane.YES_OPTION);
         };
         final DuplicationResult<ThesaurusEntry> duplicationResult = parserManager.getTheParser()
         .insertSynonyms(synonyms, duplicatesDiscriminator);
         if(duplicationResult.isForceInsertion()){
            //if everything's ok update the table and the sorter…
            final ThesaurusTableModel dm = (ThesaurusTableModel)theTable.getModel();
            dm.setSynonyms(parserManager.getTheParser().getSynonymsDictionary());
            dm.fireTableDataChanged();

            formerFilterThesaurusText = null;
            theSynonymsTextField.setText(null);
            theSynonymsTextField.requestFocusInWindow();
            popupMergeMenuItem.setEnabled(false);
            @SuppressWarnings("unchecked")
            TableRowSorter<ThesaurusTableModel> sorter = (TableRowSorter<ThesaurusTableModel>)theTable.getRowSorter();
            sorter.setRowFilter(null);

            updateSynonymsCounter();

            //… and save the files
            parserManager.storeThesaurusFiles();
         }
         else{
            theSynonymsTextField.requestFocusInWindow();

            final String duplicatedWords = duplicationResult.getDuplicates().stream()
            .map(ThesaurusEntry::getDefinition)
            .collect(Collectors.joining(", "));
            LOGGER.info(ParserManager.MARKER_APPLICATION, "Duplicate detected: {}", duplicatedWords);
         }
      }
      catch(final Exception e){
         LOGGER.info(ParserManager.MARKER_APPLICATION, "Insertion error: {}", e.getMessage());
      }
   }//GEN-LAST:event_theAddButtonActionPerformed

	public void initialize(){
		final String language = parserManager.getAffixData().getLanguage();

		final Comparator<String> comparator = Comparator.comparingInt(String::length)
			.thenComparing(BaseBuilder.getComparator(language));
		final Comparator<AffixEntry> comparatorAffix = Comparator.comparingInt((AffixEntry entry) -> entry.toString().length())
			.thenComparing((entry0, entry1) -> BaseBuilder.getComparator(language).compare(entry0.toString(), entry1.toString()));
		addSorterToTable(theTable, comparator, comparatorAffix);

		try{
			final AffixData affixData = parserManager.getAffixData();
			final Set<String> compoundRules = affixData.getCompoundRules();


			//thesaurus file:
			if(parserManager.getTheParser().getSynonymsCount() > 0){
				addSorterToTable(theTable, comparator, null);

				final ThesaurusTableModel dm = (ThesaurusTableModel)theTable.getModel();
				dm.setSynonyms(parserManager.getTheParser().getSynonymsDictionary());
				updateSynonymsCounter();
			}
		}
		catch(final IndexOutOfBoundsException e){
			LOGGER.info(ParserManager.MARKER_APPLICATION, e.getMessage());
		}
		catch(final Exception e){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "A bad error occurred: {}", e.getMessage());

			LOGGER.error("A bad error occurred", e);
		}
	}

	private void addSorterToTable(final JTable table, final Comparator<String> comparator, final Comparator<AffixEntry> comparatorAffix){
		final TableRowSorter<TableModel> dicSorter = new AscendingDescendingUnsortedTableRowSorter<>(table.getModel());
		dicSorter.setComparator(0, comparator);
		dicSorter.setComparator(1, comparator);
		if(table.getColumnModel().getColumnCount() > 2){
			dicSorter.setComparator(2, comparatorAffix);
			dicSorter.setComparator(3, comparatorAffix);
			dicSorter.setComparator(4, comparatorAffix);
		}
		table.setRowSorter(dicSorter);
	}

	public void setCurrentFont(){
		final Font currentFont = GUIUtils.getCurrentFont();
		theSynonymsTextField.setFont(currentFont);
		theTable.setFont(currentFont);
	}

	public void clear(){
		theSynonymsTextField.setText(null);
		popupMergeMenuItem.setEnabled(false);

		formerFilterThesaurusText = null;
		//noinspection unchecked
		((TableRowSorter<ThesaurusTableModel>)theTable.getRowSorter()).setRowFilter(null);
		final ThesaurusTableModel dm = (ThesaurusTableModel)theTable.getModel();
		dm.setSynonyms(null);
	}

	private void filterThesaurus(){
		final String unmodifiedSearchText = StringUtils.strip(theSynonymsTextField.getText());

		popupMergeMenuItem.setEnabled(StringUtils.isNotBlank(unmodifiedSearchText));

		if(formerFilterThesaurusText != null && formerFilterThesaurusText.equals(unmodifiedSearchText))
			return;

		formerFilterThesaurusText = unmodifiedSearchText;

		final Pair<String[], String[]> pair = ThesaurusParser.extractComponentsForFilter(unmodifiedSearchText);
		//if text to be inserted is already fully contained into the thesaurus, do not enable the button
		final boolean alreadyContained = parserManager.getTheParser().contains(pair.getLeft(), pair.getRight());
		theAddButton.setEnabled(!alreadyContained);

		@SuppressWarnings("unchecked")
		final TableRowSorter<ThesaurusTableModel> sorter = (TableRowSorter<ThesaurusTableModel>)theTable.getRowSorter();
		if(StringUtils.isNotBlank(unmodifiedSearchText)){
			final Pair<String, String> searchText = ThesaurusParser.prepareTextForFilter(pair.getLeft(), pair.getRight());
			JavaHelper.executeOnEventDispatchThread(() -> sorter.setRowFilter(RowFilter.regexFilter(searchText.getRight())));
		}
		else
			sorter.setRowFilter(null);
	}

	public void mergeThesaurusRow(final Component invoker){
		final int selectedRow = theTable.convertRowIndexToModel(theTable.getSelectedRow());
		final ThesaurusTableModel dm = (ThesaurusTableModel)theTable.getModel();
		final ThesaurusEntry synonyms = dm.getSynonymsAt(selectedRow);
		final SynonymsEntry newSynonyms = new SynonymsEntry(theSynonymsTextField.getText());

		//filter synonyms with same part-of-speech
		final List<SynonymsEntry> filteredSynonymsEntries = synonyms.getSynonyms().stream()
			.filter(syns -> syns.hasSamePartOfSpeeches(newSynonyms.getPartOfSpeeches()))
			.collect(Collectors.toList());
		if(filteredSynonymsEntries.isEmpty())
			JOptionPane.showMessageDialog(null,
				"No synonyms with same part-of-speech present.\r\nCannot merge automatically, do it manually.",
				"Warning", JOptionPane.WARNING_MESSAGE);
		else{
			//show merge dialog
			final ThesaurusMergeDialog dialog = new ThesaurusMergeDialog(synonyms.getDefinition(), newSynonyms,
				filteredSynonymsEntries, null);
			GUIUtils.addCancelByEscapeKey(dialog);
			dialog.setLocationRelativeTo(this);
			dialog.setVisible(true);

			if(dialog.isMerged())
				theSynonymsTextField.setText(dialog.getMerge());
		}
	}

	public void removeSelectedRows(final Component invoker){
		removeSelectedRowsFromThesaurus();
	}

	public void removeSelectedRowsFromThesaurus(){
		try{
			final int selectedRow = theTable.convertRowIndexToModel(theTable.getSelectedRow());
			final ThesaurusTableModel dm = (ThesaurusTableModel)theTable.getModel();
			final String selectedDefinition = (String)dm.getValueAt(selectedRow, 0);
			final String selectedSynonyms = (String)dm.getValueAt(selectedRow, 1);
			parserManager.getTheParser()
				.deleteDefinitionAndSynonyms(selectedDefinition, selectedSynonyms);

			dm.setSynonyms(parserManager.getTheParser().getSynonymsDictionary());
			updateSynonymsCounter();

			//… and save the files
			parserManager.storeThesaurusFiles();


			//redo filtering, that is re-set the state of the button (it may have changed)
			final String unmodifiedSearchText = theSynonymsTextField.getText();
			if(StringUtils.isNotBlank(unmodifiedSearchText)){
				final Pair<String[], String[]> pair = ThesaurusParser.extractComponentsForFilter(unmodifiedSearchText);
				//if text to be inserted is already fully contained into the thesaurus, do not enable the button
				final boolean alreadyContained = parserManager.getTheParser().contains(pair.getLeft(), pair.getRight());
				theAddButton.setEnabled(!alreadyContained);
			}
		}
		catch(final Exception e){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Deletion error: {}", e.getMessage());
		}
	}

	private void updateSynonymsCounter(){
		theSynonymsRecordedOutputLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(parserManager.getTheParser().getSynonymsCount()));
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
   private javax.swing.JButton theAddButton;
   private javax.swing.JScrollPane theScrollPane;
   private javax.swing.JLabel theSynonymsLabel;
   private javax.swing.JLabel theSynonymsRecordedLabel;
   private javax.swing.JLabel theSynonymsRecordedOutputLabel;
   private javax.swing.JTextField theSynonymsTextField;
   private javax.swing.JTable theTable;
   // End of variables declaration//GEN-END:variables
}
