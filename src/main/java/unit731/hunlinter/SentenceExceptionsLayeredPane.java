package unit731.hunlinter;

import java.awt.*;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.actions.OpenFileAction;
import unit731.hunlinter.gui.AscendingDescendingUnsortedTableRowSorter;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.gui.JTagPanel;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.exceptions.ExceptionsParser;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.system.Debouncer;


public class SentenceExceptionsLayeredPane extends JLayeredPane{

	private static final Logger LOGGER = LoggerFactory.getLogger(SentenceExceptionsLayeredPane.class);

	private static final int DEBOUNCER_INTERVAL = 600;


	private final Debouncer<SentenceExceptionsLayeredPane> debouncer = new Debouncer<>(this::filterSentenceExceptions, DEBOUNCER_INTERVAL);

	private final Packager packager;
	private final ParserManager parserManager;

	private String formerFilterSentenceException;


	public SentenceExceptionsLayeredPane(final Packager packager, final ParserManager parserManager){
		Objects.requireNonNull(packager);
		Objects.requireNonNull(parserManager);

		this.packager = packager;
		this.parserManager = parserManager;


		initComponents();


		//add "fontable" property
		GUIUtils.addFontableProperty(sexTextField);

		GUIUtils.addUndoManager(sexTextField);
	}

	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      sexInputLabel = new javax.swing.JLabel();
      sexTextField = new javax.swing.JTextField();
      sexAddButton = new javax.swing.JButton();
      sexScrollPane = new javax.swing.JScrollPane();
      sexScrollPane.getVerticalScrollBar().setUnitIncrement(16);
      sexTagPanel = new JTagPanel((changeType, tags) -> {
         final ExceptionsParser sexParser = parserManager.getSexParser();
         sexParser.modify(changeType, tags);
         try{
            sexParser.save(packager.getSentenceExceptionsFile());
         }
         catch(final TransformerException e){
            LOGGER.info(ParserManager.MARKER_APPLICATION, e.getMessage());
         }
      });
      sexCorrectionsRecordedLabel = new javax.swing.JLabel();
      sexCorrectionsRecordedOutputLabel = new javax.swing.JLabel();
      openSexButton = new javax.swing.JButton();

      sexInputLabel.setText("Exception:");

      sexTextField.setToolTipText("hit `enter` to add");
      sexTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            sexTextFieldKeyReleased(evt);
         }
      });

      sexAddButton.setMnemonic('A');
      sexAddButton.setText("Add");
      sexAddButton.setEnabled(false);
      sexAddButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            sexAddButtonActionPerformed(evt);
         }
      });

      sexScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      sexScrollPane.setViewportView(sexTagPanel);

      sexCorrectionsRecordedLabel.setText("Exceptions recorded:");

      sexCorrectionsRecordedOutputLabel.setText("…");

      openSexButton.setAction(new OpenFileAction(Packager.KEY_FILE_SENTENCE_EXCEPTIONS, packager));
      openSexButton.setText("Open Sentence Exceptions");
      openSexButton.setEnabled(false);

      setLayer(sexInputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(sexTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(sexAddButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(sexScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(sexCorrectionsRecordedLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(sexCorrectionsRecordedOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(openSexButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(sexScrollPane)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(sexCorrectionsRecordedLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(sexCorrectionsRecordedOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 636, Short.MAX_VALUE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(openSexButton))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addComponent(sexInputLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(sexTextField)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(sexAddButton)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(sexTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(sexInputLabel)
               .addComponent(sexAddButton))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(sexScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(sexCorrectionsRecordedLabel)
               .addComponent(sexCorrectionsRecordedOutputLabel)
               .addComponent(openSexButton))
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents

   private void sexTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_sexTextFieldKeyReleased
		debouncer.call(this);
   }//GEN-LAST:event_sexTextFieldKeyReleased

   private void sexAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sexAddButtonActionPerformed
      try{
         final String exception = StringUtils.strip(sexTextField.getText());
         if(!parserManager.getSexParser().contains(exception)){
            parserManager.getSexParser().modify(ExceptionsParser.TagChangeType.ADD, Collections.singletonList(exception));
            sexTagPanel.addTag(exception);

            //reset input
            sexTextField.setText(StringUtils.EMPTY);
            sexTagPanel.applyFilter(null);

            updateSentenceExceptionsCounter();

            parserManager.storeSentenceExceptionFile();
         }
         else{
            sexTextField.requestFocusInWindow();

            JOptionPane.showOptionDialog(this,
               "A duplicate is already present", "Warning!", JOptionPane.DEFAULT_OPTION,
               JOptionPane.WARNING_MESSAGE, null, null, null);
         }
      }
      catch(final Exception e){
         LOGGER.info(ParserManager.MARKER_APPLICATION, "Insertion error: {}", e.getMessage());
      }
   }//GEN-LAST:event_sexAddButtonActionPerformed

	public void initialize(){
		if(parserManager.getSexParser().getExceptionsCounter() > 0){
			updateSentenceExceptionsCounter();

			final List<String> sentenceExceptions = parserManager.getSexParser().getExceptionsDictionary();
			sexTagPanel.initializeTags(sentenceExceptions);
		}
		openSexButton.setEnabled(packager.getSentenceExceptionsFile() != null);
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
		sexTagPanel.setFont(currentFont);
	}

	public void clear(){
		openSexButton.setEnabled(false);
		formerFilterSentenceException = null;
		sexTagPanel.applyFilter(null);
		sexTagPanel.initializeTags(null);
	}

	private void filterSentenceExceptions(){
		final String unmodifiedException = StringUtils.strip(sexTextField.getText());
		if(formerFilterSentenceException != null && formerFilterSentenceException.equals(unmodifiedException))
			return;

		formerFilterSentenceException = unmodifiedException;

		//if text to be inserted is already fully contained into the thesaurus, do not enable the button
		final boolean alreadyContained = parserManager.getSexParser().contains(unmodifiedException);
		sexAddButton.setEnabled(StringUtils.isNotBlank(unmodifiedException) && unmodifiedException.endsWith(".")
			&& !alreadyContained);


		sexTagPanel.applyFilter(StringUtils.isNotBlank(unmodifiedException)? unmodifiedException: null);
	}

	private void updateSentenceExceptionsCounter(){
		sexCorrectionsRecordedOutputLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(parserManager.getSexParser().getExceptionsCounter()));
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
   private javax.swing.JButton openSexButton;
   private javax.swing.JButton sexAddButton;
   private javax.swing.JLabel sexCorrectionsRecordedLabel;
   private javax.swing.JLabel sexCorrectionsRecordedOutputLabel;
   private javax.swing.JLabel sexInputLabel;
   private javax.swing.JScrollPane sexScrollPane;
   private unit731.hunlinter.gui.JTagPanel sexTagPanel;
   private javax.swing.JTextField sexTextField;
   // End of variables declaration//GEN-END:variables
}
