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
import unit731.hunlinter.gui.AutoCorrectTableModel;
import unit731.hunlinter.gui.PanableInterface;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.gui.JCopyableTable;
import unit731.hunlinter.gui.TableRenderer;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.autocorrect.AutoCorrectParser;
import unit731.hunlinter.parsers.autocorrect.CorrectionEntry;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.thesaurus.DuplicationResult;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.system.Debouncer;
import unit731.hunlinter.services.system.JavaHelper;


public class AutoCorrectLayeredPane extends JLayeredPane implements PanableInterface{

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
		GUIUtils.addFontableProperty(table, incorrectTextField, correctTextField);

		GUIUtils.addUndoManager(incorrectTextField, correctTextField);
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
      table = new JCopyableTable(){
         @Override
         public String getValueAtRow(final int row){
            final TableModel model = getModel();
            final String incorrect = (String)model.getValueAt(row, 0);
            final String correct = (String)model.getValueAt(row, 1);
            return incorrect + " > " + correct;
         }
      };
      correctionsRecordedLabel = new javax.swing.JLabel();
      correctionsRecordedValueLabel = new javax.swing.JLabel();
      openAcoButton = new javax.swing.JButton();

      incorrectLabel.setText("Incorrect form:");

      incorrectTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            incorrectTextFieldKeyReleased(evt);
         }
      });

      toLabel.setText("→");

      correctLabel.setText("Correct form:");

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

      table.setModel(new AutoCorrectTableModel());
      table.setRowSorter(new TableRowSorter<>((AutoCorrectTableModel)table.getModel()));
      table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
      table.setShowHorizontalLines(false);
      table.setShowVerticalLines(false);
      //listen for row removal
      KeyStroke cancelKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
      table.registerKeyboardAction(event -> removeSelectedRowsFromAutoCorrect(), cancelKeyStroke, JComponent.WHEN_FOCUSED);
      KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, false);
      table.registerKeyboardAction(event -> GUIUtils.copyToClipboard((JCopyableTable)table), copyKeyStroke, JComponent.WHEN_FOCUSED);

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
                     table.clearSelection();
                  }
               });
               dialog.setLocationRelativeTo(parentFrame);
               dialog.setVisible(true);
            }
         }
      });

      TableRenderer acoCellRenderer = new TableRenderer();
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
            TableRowSorter<AutoCorrectTableModel> sorter = (TableRowSorter<AutoCorrectTableModel>)table.getRowSorter();
            sorter.setRowFilter(null);

            updateAutoCorrectionsCounter();

            //… and save the files
            parserManager.storeAutoCorrectFile();
         }
         else{
            incorrectTextField.requestFocusInWindow();

            final String duplicatedWords = duplicationResult.getDuplicates().stream()
            .map(CorrectionEntry::toString)
            .collect(Collectors.joining(", "));
            LOGGER.info(ParserManager.MARKER_APPLICATION, "Duplicate detected: {}", duplicatedWords);
         }
      }
      catch(final Exception e){
         LOGGER.info(ParserManager.MARKER_APPLICATION, "Insertion error: {}", e.getMessage());
      }
   }//GEN-LAST:event_addButtonActionPerformed

	@Override
	public void initialize(){
		if(parserManager.getAcoParser().getCorrectionsCounter() > 0){
			final String language = parserManager.getLanguage();
			final Comparator<String> comparator = Comparator.comparingInt(String::length)
				.thenComparing(BaseBuilder.getComparator(language));
			GUIUtils.addSorterToTable(table, comparator, null);

			final AutoCorrectTableModel dm = (AutoCorrectTableModel)table.getModel();
			dm.setCorrections(parserManager.getAcoParser().getCorrectionsDictionary());
			updateAutoCorrectionsCounter();
		}
		openAcoButton.setEnabled(packager.getAutoCorrectFile() != null);
	}

	@Override
	public void setCurrentFont(){
		final Font currentFont = GUIUtils.getCurrentFont();
		incorrectTextField.setFont(currentFont);
		correctTextField.setFont(currentFont);
		table.setFont(currentFont);
	}

	@Override
	public void clear(){
		formerFilterIncorrectText = null;
		formerFilterCorrectText = null;

		openAcoButton.setEnabled(false);
		//noinspection unchecked
		((TableRowSorter<AutoCorrectTableModel>)table.getRowSorter()).setRowFilter(null);
		final AutoCorrectTableModel dm = (AutoCorrectTableModel)table.getModel();
		dm.setCorrections(null);
	}

	public void removeSelectedRowsFromAutoCorrect(){
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


	@SuppressWarnings("unused")
	private void writeObject(final ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
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
