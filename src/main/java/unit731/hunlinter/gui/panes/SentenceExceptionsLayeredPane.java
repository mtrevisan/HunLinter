package unit731.hunlinter.gui.panes;

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
import unit731.hunlinter.MainFrame;
import unit731.hunlinter.actions.OpenFileAction;
import unit731.hunlinter.gui.FontHelper;
import unit731.hunlinter.gui.GUIHelper;
import unit731.hunlinter.gui.components.JTagPanel;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.exceptions.ExceptionsParser;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.eventbus.EventBusService;
import unit731.hunlinter.services.eventbus.EventHandler;
import unit731.hunlinter.services.system.Debouncer;


public class SentenceExceptionsLayeredPane extends JLayeredPane{

	private static final long serialVersionUID = -4277472579904204046L;

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
		FontHelper.addFontableProperty(textField, tagPanel);

		GUIHelper.addUndoManager(textField);

		EventBusService.subscribe(SentenceExceptionsLayeredPane.this);
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      inputLabel = new javax.swing.JLabel();
      textField = new javax.swing.JTextField();
      addButton = new javax.swing.JButton();
      scrollPane = new javax.swing.JScrollPane();
      scrollPane.getVerticalScrollBar().setUnitIncrement(16);
      tagPanel = new JTagPanel((changeType, tags) -> {
         final ExceptionsParser sexParser = parserManager.getSexParser();
         sexParser.modify(changeType, tags);
         try{
            sexParser.save(packager.getSentenceExceptionsFile());
         }
         catch(final TransformerException e){
            LOGGER.info(ParserManager.MARKER_APPLICATION, e.getMessage());
         }
      });
      correctionsRecordedLabel = new javax.swing.JLabel();
      correctionsRecordedValueLabel = new javax.swing.JLabel();
      openSexButton = new javax.swing.JButton();

      inputLabel.setText("Exception:");

      textField.setFont(FontHelper.getCurrentFont());
      textField.setToolTipText("hit `enter` to add");
      textField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            textFieldKeyReleased(evt);
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

      scrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      scrollPane.setViewportView(tagPanel);

      correctionsRecordedLabel.setText("Exceptions recorded:");

      correctionsRecordedValueLabel.setText("…");

      openSexButton.setAction(new OpenFileAction(Packager.KEY_FILE_SENTENCE_EXCEPTIONS, packager));
      openSexButton.setText("Open Sentence Exceptions");
      openSexButton.setEnabled(false);

      setLayer(inputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(textField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(addButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(scrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(correctionsRecordedLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(correctionsRecordedValueLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(openSexButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(scrollPane)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(correctionsRecordedLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(correctionsRecordedValueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 636, Short.MAX_VALUE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(openSexButton))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addComponent(inputLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(textField)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(addButton)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(textField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(inputLabel)
               .addComponent(addButton))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(correctionsRecordedLabel)
               .addComponent(correctionsRecordedValueLabel)
               .addComponent(openSexButton))
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents

   private void textFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textFieldKeyReleased
		debouncer.call(this);
   }//GEN-LAST:event_textFieldKeyReleased

   private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
      try{
         final String exception = textField.getText().trim();
         if(!parserManager.getSexParser().contains(exception)){
            parserManager.getSexParser().modify(ExceptionsParser.TagChangeType.ADD, Collections.singletonList(exception));
            tagPanel.addTag(exception);

            //reset input
            textField.setText(null);
            tagPanel.applyFilter(null);

            updateSentenceExceptionsCounter();

            parserManager.storeSentenceExceptionFile();
         }
         else{
            textField.requestFocusInWindow();

            JOptionPane.showOptionDialog(this,
               "A duplicate is already present", "Warning!", JOptionPane.DEFAULT_OPTION,
               JOptionPane.WARNING_MESSAGE, null, null, null);
         }
      }
      catch(final Exception e){
         LOGGER.info(ParserManager.MARKER_APPLICATION, "Insertion error: {}", e.getMessage());
      }
   }//GEN-LAST:event_addButtonActionPerformed

	@EventHandler
	public void initialize(final Integer actionCommand){
		//noinspection NumberEquality
		if(actionCommand != MainFrame.ACTION_COMMAND_INITIALIZE)
			return;

		if(parserManager.getSexParser().getExceptionsCounter() > 0){
			updateSentenceExceptionsCounter();

			final List<String> sentenceExceptions = parserManager.getSexParser().getExceptionsDictionary();
			tagPanel.initializeTags(sentenceExceptions);
		}
		openSexButton.setEnabled(packager.getSentenceExceptionsFile() != null);
	}

	@EventHandler
	public void clear(final Integer actionCommand){
		//noinspection NumberEquality
		if(actionCommand != MainFrame.ACTION_COMMAND_GUI_CLEAR_ALL && actionCommand != MainFrame.ACTION_COMMAND_GUI_CLEAR_SENTENCE_EXCEPTIONS)
			return;

		formerFilterSentenceException = null;
		textField.setText(null);

		openSexButton.setEnabled(false);
		tagPanel.applyFilter(null);
		tagPanel.initializeTags(null);
	}

	private void filterSentenceExceptions(){
		final String unmodifiedException = textField.getText().trim();
		if(formerFilterSentenceException != null && formerFilterSentenceException.equals(unmodifiedException))
			return;

		formerFilterSentenceException = unmodifiedException;

		//if text to be inserted is already fully contained into the thesaurus, do not enable the button
		final boolean alreadyContained = parserManager.getSexParser().contains(unmodifiedException);
		addButton.setEnabled(StringUtils.isNotBlank(unmodifiedException) && unmodifiedException.endsWith(".")
			&& !alreadyContained);


		tagPanel.applyFilter(StringUtils.isNotBlank(unmodifiedException)? unmodifiedException: null);
	}

	private void updateSentenceExceptionsCounter(){
		correctionsRecordedValueLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(parserManager.getSexParser().getExceptionsCounter()));
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
   private javax.swing.JLabel correctionsRecordedLabel;
   private javax.swing.JLabel correctionsRecordedValueLabel;
   private javax.swing.JLabel inputLabel;
   private javax.swing.JButton openSexButton;
   private javax.swing.JScrollPane scrollPane;
   private unit731.hunlinter.gui.components.JTagPanel tagPanel;
   private javax.swing.JTextField textField;
   // End of variables declaration//GEN-END:variables
}
