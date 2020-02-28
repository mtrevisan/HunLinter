package unit731.hunlinter;

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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.actions.OpenFileAction;
import unit731.hunlinter.gui.AscendingDescendingUnsortedTableRowSorter;
import unit731.hunlinter.gui.AutoCorrectTableModel;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.gui.JCopyableTable;
import unit731.hunlinter.gui.TableRenderer;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.autocorrect.AutoCorrectParser;
import unit731.hunlinter.parsers.autocorrect.CorrectionEntry;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.thesaurus.DuplicationResult;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.system.Debouncer;
import unit731.hunlinter.services.system.JavaHelper;


public class AutoCorrectLayeredPane extends JLayeredPane{

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoCorrectLayeredPane.class);

	private static final int DEBOUNCER_INTERVAL = 600;


	private final Debouncer<AutoCorrectLayeredPane> debouncer = new Debouncer<>(this::filterAutoCorrect, DEBOUNCER_INTERVAL);

	private final Packager packager;
	private final ParserManager parserManager;

	private String formerFilterIncorrectText;
	private String formerFilterCorrectText;
	private final JFrame parentFrame;


	public AutoCorrectLayeredPane(final Packager packager, final ParserManager parserManager, final JFrame parentFrame){
		Objects.requireNonNull(packager);
		Objects.requireNonNull(parserManager);
		Objects.requireNonNull(parentFrame);

		this.packager = packager;
		this.parserManager = parserManager;
		this.parentFrame = parentFrame;


		initComponents();


		//add "fontable" property
		GUIUtils.addFontableProperty(acoTable, acoIncorrectTextField, acoCorrectTextField);

		GUIUtils.addUndoManager(acoIncorrectTextField, acoCorrectTextField);
	}

	@SuppressWarnings("unchecked")
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      acoIncorrectLabel = new javax.swing.JLabel();
      acoIncorrectTextField = new javax.swing.JTextField();
      acoToLabel = new javax.swing.JLabel();
      acoCorrectLabel = new javax.swing.JLabel();
      acoCorrectTextField = new javax.swing.JTextField();
      acoAddButton = new javax.swing.JButton();
      acoScrollPane = new javax.swing.JScrollPane();
      acoTable = new JCopyableTable(){
         @Override
         public String getValueAtRow(final int row){
            final TableModel model = getModel();
            final String incorrect = (String)model.getValueAt(row, 0);
            final String correct = (String)model.getValueAt(row, 1);
            return incorrect + " > " + correct;
         }
      };
      acoCorrectionsRecordedLabel = new javax.swing.JLabel();
      acoCorrectionsRecordedOutputLabel = new javax.swing.JLabel();
      openAcoButton = new javax.swing.JButton();

      acoIncorrectLabel.setText("Incorrect form:");

      acoIncorrectTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            acoIncorrectTextFieldKeyReleased(evt);
         }
      });

      acoToLabel.setText("→");

      acoCorrectLabel.setText("Correct form:");

      acoCorrectTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            acoCorrectTextFieldKeyReleased(evt);
         }
      });

      acoAddButton.setMnemonic('A');
      acoAddButton.setText("Add");
      acoAddButton.setEnabled(false);
      acoAddButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            acoAddButtonActionPerformed(evt);
         }
      });

      acoTable.setModel(new AutoCorrectTableModel());
      acoTable.setRowSorter(new TableRowSorter<>((AutoCorrectTableModel)acoTable.getModel()));
      acoTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
      acoTable.setShowHorizontalLines(false);
      acoTable.setShowVerticalLines(false);
      //listen for row removal
      KeyStroke cancelKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
      acoTable.registerKeyboardAction(event -> removeSelectedRowsFromAutoCorrect(), cancelKeyStroke, JComponent.WHEN_FOCUSED);
      KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, false);
      acoTable.registerKeyboardAction(event -> GUIUtils.copyToClipboard((JCopyableTable)acoTable), copyKeyStroke, JComponent.WHEN_FOCUSED);

      acoTable.addMouseListener(new MouseAdapter(){
         public void mouseClicked(final MouseEvent e){
            if(e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1){
               final int selectedRow = acoTable.rowAtPoint(e.getPoint());
               acoTable.setRowSelectionInterval(selectedRow, selectedRow);
               final int row = acoTable.convertRowIndexToModel(selectedRow);
               final BiConsumer<String, String> okButtonAction = (incorrect, correct) -> {
                  try{
                     parserManager.getAcoParser().setCorrection(row, incorrect, correct);

                     //… and save the files
                     parserManager.storeAutoCorrectFile();
                  }
                  catch(Exception ex){
                     LOGGER.info(ParserManager.MARKER_APPLICATION, ex.getMessage());
                  }
               };
               final CorrectionEntry definition = parserManager.getAcoParser().getCorrectionsDictionary().get(row);
               final CorrectionDialog dialog = new CorrectionDialog(definition, okButtonAction, parentFrame);
               GUIUtils.addCancelByEscapeKey(dialog);
               dialog.addWindowListener(new WindowAdapter(){
                  @Override
                  public void windowClosed(final WindowEvent e){
                     acoTable.clearSelection();
                  }
               });
               dialog.setLocationRelativeTo(parentFrame);
               dialog.setVisible(true);
            }
         }
      });

      TableRenderer acoCellRenderer = new TableRenderer();
      acoTable.getColumnModel().getColumn(1).setCellRenderer(acoCellRenderer);
      acoScrollPane.setViewportView(acoTable);

      acoCorrectionsRecordedLabel.setText("Corrections recorded:");

      acoCorrectionsRecordedOutputLabel.setText("…");

      openAcoButton.setAction(new OpenFileAction(Packager.KEY_FILE_AUTO_CORRECT, packager));
      openAcoButton.setText("Open AutoCorrect");
      openAcoButton.setEnabled(false);

      setLayer(acoIncorrectLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(acoIncorrectTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(acoToLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(acoCorrectLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(acoCorrectTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(acoAddButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(acoScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(acoCorrectionsRecordedLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(acoCorrectionsRecordedOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(openAcoButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(acoScrollPane)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(acoIncorrectLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(acoIncorrectTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 330, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addComponent(acoToLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(acoCorrectLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(acoCorrectTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 330, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addGap(18, 18, 18)
                  .addComponent(acoAddButton))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(acoCorrectionsRecordedLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(acoCorrectionsRecordedOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(openAcoButton)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(acoIncorrectLabel)
               .addComponent(acoIncorrectTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(acoCorrectTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(acoAddButton)
               .addComponent(acoToLabel)
               .addComponent(acoCorrectLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(acoScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(acoCorrectionsRecordedLabel)
               .addComponent(acoCorrectionsRecordedOutputLabel)
               .addComponent(openAcoButton))
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents

   private void acoIncorrectTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_acoIncorrectTextFieldKeyReleased
		debouncer.call(this);
   }//GEN-LAST:event_acoIncorrectTextFieldKeyReleased

   private void acoCorrectTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_acoCorrectTextFieldKeyReleased
		debouncer.call(this);
   }//GEN-LAST:event_acoCorrectTextFieldKeyReleased

   private void acoAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_acoAddButtonActionPerformed
      try{
         //try adding the correction
         final String incorrect = acoIncorrectTextField.getText();
         final String correct = acoCorrectTextField.getText();
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
            final AutoCorrectTableModel dm = (AutoCorrectTableModel)acoTable.getModel();
            dm.fireTableDataChanged();

            formerFilterIncorrectText = null;
            formerFilterCorrectText = null;
            acoIncorrectTextField.setText(null);
            acoCorrectTextField.setText(null);
            acoAddButton.setEnabled(false);
            acoIncorrectTextField.requestFocusInWindow();
            @SuppressWarnings("unchecked")
            TableRowSorter<AutoCorrectTableModel> sorter = (TableRowSorter<AutoCorrectTableModel>)acoTable.getRowSorter();
            sorter.setRowFilter(null);

            updateAutoCorrectionsCounter();

            //… and save the files
            parserManager.storeAutoCorrectFile();
         }
         else{
            acoIncorrectTextField.requestFocusInWindow();

            final String duplicatedWords = duplicationResult.getDuplicates().stream()
            .map(CorrectionEntry::toString)
            .collect(Collectors.joining(", "));
            LOGGER.info(ParserManager.MARKER_APPLICATION, "Duplicate detected: {}", duplicatedWords);
         }
      }
      catch(final Exception e){
         LOGGER.info(ParserManager.MARKER_APPLICATION, "Insertion error: {}", e.getMessage());
      }
   }//GEN-LAST:event_acoAddButtonActionPerformed

	public void initialize(){
		if(parserManager.getAcoParser().getCorrectionsCounter() > 0){
			final String language = parserManager.getAffixData().getLanguage();
			final Comparator<String> comparator = Comparator.comparingInt(String::length)
				.thenComparing(BaseBuilder.getComparator(language));
			addSorterToTable(acoTable, comparator, null);

			final AutoCorrectTableModel dm = (AutoCorrectTableModel)acoTable.getModel();
			dm.setCorrections(parserManager.getAcoParser().getCorrectionsDictionary());
			updateAutoCorrectionsCounter();
		}
		openAcoButton.setEnabled(packager.getAutoCorrectFile() != null);
	}

	private void addSorterToTable(final JTable table, final Comparator<String> comparator,
			final Comparator<AffixEntry> comparatorAffix){
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
		acoIncorrectTextField.setFont(currentFont);
		acoCorrectTextField.setFont(currentFont);
		acoTable.setFont(currentFont);
	}

	public void clear(){
		formerFilterIncorrectText = null;
		formerFilterCorrectText = null;

		openAcoButton.setEnabled(false);
		//noinspection unchecked
		((TableRowSorter<AutoCorrectTableModel>)acoTable.getRowSorter()).setRowFilter(null);
		final AutoCorrectTableModel dm = (AutoCorrectTableModel)acoTable.getModel();
		dm.setCorrections(null);
	}

	public void removeSelectedRowsFromAutoCorrect(){
		try{
			final int selectedRow = acoTable.convertRowIndexToModel(acoTable.getSelectedRow());
			parserManager.getAcoParser().deleteCorrection(selectedRow);

			final AutoCorrectTableModel dm = (AutoCorrectTableModel)acoTable.getModel();
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
		final String unmodifiedIncorrectText = StringUtils.strip(acoIncorrectTextField.getText());
		final String unmodifiedCorrectText = StringUtils.strip(acoCorrectTextField.getText());
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
		acoAddButton.setEnabled(StringUtils.isNotBlank(unmodifiedIncorrectText) && StringUtils.isNotBlank(unmodifiedCorrectText)
			&& !unmodifiedIncorrectText.equals(unmodifiedCorrectText) && !alreadyContained);

		@SuppressWarnings("unchecked")
		final TableRowSorter<AutoCorrectTableModel> sorter = (TableRowSorter<AutoCorrectTableModel>)acoTable.getRowSorter();
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
		acoCorrectionsRecordedOutputLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(parserManager.getAcoParser().getCorrectionsCounter()));
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
   private javax.swing.JButton acoAddButton;
   private javax.swing.JLabel acoCorrectLabel;
   private javax.swing.JTextField acoCorrectTextField;
   private javax.swing.JLabel acoCorrectionsRecordedLabel;
   private javax.swing.JLabel acoCorrectionsRecordedOutputLabel;
   private javax.swing.JLabel acoIncorrectLabel;
   private javax.swing.JTextField acoIncorrectTextField;
   private javax.swing.JScrollPane acoScrollPane;
   private javax.swing.JTable acoTable;
   private javax.swing.JLabel acoToLabel;
   private javax.swing.JButton openAcoButton;
   // End of variables declaration//GEN-END:variables
}
