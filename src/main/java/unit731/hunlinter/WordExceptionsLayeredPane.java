package unit731.hunlinter;

import java.awt.*;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.swing.*;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.actions.OpenFileAction;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.gui.JTagPanel;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.exceptions.ExceptionsParser;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.system.Debouncer;
import unit731.hunlinter.services.text.StringHelper;


public class WordExceptionsLayeredPane extends JLayeredPane{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordExceptionsLayeredPane.class);

	private static final int DEBOUNCER_INTERVAL = 600;


	private final Debouncer<WordExceptionsLayeredPane> debouncer = new Debouncer<>(this::filterWordExceptions, DEBOUNCER_INTERVAL);

	private final Packager packager;
	private final ParserManager parserManager;

	private String formerFilterWordException;


	public WordExceptionsLayeredPane(final Packager packager, final ParserManager parserManager){
		Objects.requireNonNull(packager);
		Objects.requireNonNull(parserManager);

		this.packager = packager;
		this.parserManager = parserManager;


		initComponents();


		//add "fontable" property
		GUIUtils.addFontableProperty(wexTextField);

		GUIUtils.addUndoManager(wexTextField);
	}

	@SuppressWarnings("unchecked")
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      wexInputLabel = new javax.swing.JLabel();
      wexTextField = new javax.swing.JTextField();
      wexAddButton = new javax.swing.JButton();
      wexScrollPane = new javax.swing.JScrollPane();
      wexScrollPane.getVerticalScrollBar().setUnitIncrement(16);
      wexTagPanel = new JTagPanel((changeType, tags) -> {
         final ExceptionsParser wexParser = parserManager.getWexParser();
         wexParser.modify(changeType, tags);
         try{
            wexParser.save(packager.getWordExceptionsFile());
         }
         catch(final TransformerException e){
            LOGGER.info(ParserManager.MARKER_APPLICATION, e.getMessage());
         }
      });
      wexCorrectionsRecordedLabel = new javax.swing.JLabel();
      wexCorrectionsRecordedOutputLabel = new javax.swing.JLabel();
      openWexButton = new javax.swing.JButton();

      setPreferredSize(new java.awt.Dimension(929, 273));

      wexInputLabel.setText("Exception:");

      wexTextField.setToolTipText("hit `enter` to add");
      wexTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            wexTextFieldKeyReleased(evt);
         }
      });

      wexAddButton.setMnemonic('A');
      wexAddButton.setText("Add");
      wexAddButton.setEnabled(false);
      wexAddButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            wexAddButtonActionPerformed(evt);
         }
      });

      wexScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      wexScrollPane.setViewportView(wexTagPanel);

      wexCorrectionsRecordedLabel.setText("Exceptions recorded:");

      wexCorrectionsRecordedOutputLabel.setText("…");

      openWexButton.setAction(new OpenFileAction(Packager.KEY_FILE_WORD_EXCEPTIONS, packager));
      openWexButton.setText("Open Word Exceptions");
      openWexButton.setEnabled(false);

      setLayer(wexInputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(wexTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(wexAddButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(wexScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(wexCorrectionsRecordedLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(wexCorrectionsRecordedOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(openWexButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(wexScrollPane)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(wexCorrectionsRecordedLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(wexCorrectionsRecordedOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(openWexButton))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(wexInputLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(wexTextField)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(wexAddButton)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(wexInputLabel)
               .addComponent(wexTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(wexAddButton))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(wexScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(wexCorrectionsRecordedLabel)
               .addComponent(wexCorrectionsRecordedOutputLabel)
               .addComponent(openWexButton))
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents

   private void wexTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_wexTextFieldKeyReleased
      debouncer.call(this);
   }//GEN-LAST:event_wexTextFieldKeyReleased

   private void wexAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wexAddButtonActionPerformed
      try{
         final String exception = StringUtils.strip(wexTextField.getText());
         if(!parserManager.getWexParser().contains(exception)){
            parserManager.getWexParser().modify(ExceptionsParser.TagChangeType.ADD, Collections.singletonList(exception));
            wexTagPanel.addTag(exception);

            //reset input
            wexTextField.setText(StringUtils.EMPTY);
            wexTagPanel.applyFilter(null);

            updateWordExceptionsCounter();

            parserManager.storeWordExceptionFile();
         }
         else{
            wexTextField.requestFocusInWindow();

            JOptionPane.showOptionDialog(this,
               "A duplicate is already present", "Warning!", JOptionPane.DEFAULT_OPTION,
               JOptionPane.WARNING_MESSAGE, null, null, null);
         }
      }
      catch(final Exception e){
         LOGGER.info(ParserManager.MARKER_APPLICATION, "Insertion error: {}", e.getMessage());
      }
   }//GEN-LAST:event_wexAddButtonActionPerformed

	public void initialize(){
		if(parserManager.getWexParser().getExceptionsCounter() > 0){
			final List<String> wordExceptions = parserManager.getWexParser().getExceptionsDictionary();
			wexTagPanel.initializeTags(wordExceptions);
			updateWordExceptionsCounter();
		}
		openWexButton.setEnabled(packager.getWordExceptionsFile() != null);
	}

	public void setCurrentFont(){
		final Font currentFont = GUIUtils.getCurrentFont();
		wexTagPanel.setFont(currentFont);
	}

	public void clear(){
		openWexButton.setEnabled(false);
		formerFilterWordException = null;
		wexTagPanel.applyFilter(null);
		wexTagPanel.initializeTags(null);
	}

	private void filterWordExceptions(){
		final String unmodifiedException = StringUtils.strip(wexTextField.getText());
		if(formerFilterWordException != null && formerFilterWordException.equals(unmodifiedException))
			return;

		formerFilterWordException = unmodifiedException;

		//if text to be inserted is already fully contained into the thesaurus, do not enable the button
		final boolean alreadyContained = parserManager.getWexParser().contains(unmodifiedException);
		wexAddButton.setEnabled(StringUtils.isNotBlank(unmodifiedException) && StringHelper.countUppercases(unmodifiedException) > 1 && !alreadyContained);


		wexTagPanel.applyFilter(StringUtils.isNotBlank(unmodifiedException)? unmodifiedException: null);
	}

	private void updateWordExceptionsCounter(){
		wexCorrectionsRecordedOutputLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(parserManager.getWexParser().getExceptionsCounter()));
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
   private javax.swing.JButton openWexButton;
   private javax.swing.JButton wexAddButton;
   private javax.swing.JLabel wexCorrectionsRecordedLabel;
   private javax.swing.JLabel wexCorrectionsRecordedOutputLabel;
   private javax.swing.JLabel wexInputLabel;
   private javax.swing.JScrollPane wexScrollPane;
   private unit731.hunlinter.gui.JTagPanel wexTagPanel;
   private javax.swing.JTextField wexTextField;
   // End of variables declaration//GEN-END:variables
}
