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
import io.github.mtrevisan.hunlinter.gui.components.TagPanel;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.exceptions.ExceptionsParser;
import io.github.mtrevisan.hunlinter.services.Packager;
import io.github.mtrevisan.hunlinter.services.eventbus.EventHandler;
import io.github.mtrevisan.hunlinter.services.system.Debouncer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.xml.transform.TransformerException;
import java.awt.*;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class SentenceExceptionsLayeredPane extends JLayeredPane{

	@Serial
	private static final long serialVersionUID = -4277472579904204046L;

	private static final Logger LOGGER = LoggerFactory.getLogger(SentenceExceptionsLayeredPane.class);

	private static final int DEBOUNCER_INTERVAL = 600;


	private final Debouncer<SentenceExceptionsLayeredPane> debouncer = new Debouncer<>(this::filterSentenceExceptions, DEBOUNCER_INTERVAL);

	private final Packager packager;
	private final ParserManager parserManager;

	private String formerFilterSentenceException;


	public SentenceExceptionsLayeredPane(final Packager packager, final ParserManager parserManager){
		Objects.requireNonNull(packager, "Packager cannot be null");
		Objects.requireNonNull(parserManager, "Parser manager cannot be null");

		this.packager = packager;
		this.parserManager = parserManager;


		initComponents();


		//add "fontable" property
		FontHelper.addFontableProperty(textField, tagPanel);

		GUIHelper.addUndoManager(textField);
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      inputLabel = new javax.swing.JLabel();
      textField = new javax.swing.JTextField();
      addButton = new javax.swing.JButton();
      scrollPane = new javax.swing.JScrollPane();
      scrollPane.getVerticalScrollBar().setUnitIncrement(16);
      tagPanel = new TagPanel((changeType, tags) -> {
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

		final Font currentFont = FontHelper.getCurrentFont();

		textField.setFont(currentFont);
      textField.setToolTipText("hit `enter` to add");
      textField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            textFieldKeyReleased(evt);
         }
      });

      addButton.setMnemonic('A');
      addButton.setText("Add");
      addButton.setEnabled(false);
      addButton.addActionListener(this::addButtonActionPerformed);

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
      catch(final TransformerException | HeadlessException e){
         LOGGER.info(ParserManager.MARKER_APPLICATION, "Insertion error: {}", e.getMessage());
      }
   }//GEN-LAST:event_addButtonActionPerformed

	@EventHandler
	@SuppressWarnings({"unused", "NumberEquality"})
	public final void initialize(final Integer actionCommand){
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
	@SuppressWarnings({"unused", "NumberEquality"})
	public final void clear(final Integer actionCommand){
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
		if(unmodifiedException.equals(formerFilterSentenceException))
			return;

		formerFilterSentenceException = unmodifiedException;

		//if text to be inserted is already fully contained into the thesaurus, do not enable the button
		final boolean alreadyContained = parserManager.getSexParser().contains(unmodifiedException);
		addButton.setEnabled(StringUtils.isNotBlank(unmodifiedException) && !unmodifiedException.isEmpty()
			&& unmodifiedException.charAt(unmodifiedException.length() - 1) == '.' && !alreadyContained);


		tagPanel.applyFilter(StringUtils.isNotBlank(unmodifiedException)? unmodifiedException: null);
	}

	private void updateSentenceExceptionsCounter(){
		correctionsRecordedValueLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(parserManager.getSexParser().getExceptionsCounter()));
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
   private javax.swing.JLabel correctionsRecordedLabel;
   private javax.swing.JLabel correctionsRecordedValueLabel;
   private javax.swing.JLabel inputLabel;
   private javax.swing.JButton openSexButton;
   private javax.swing.JScrollPane scrollPane;
   private TagPanel tagPanel;
   private javax.swing.JTextField textField;
   // End of variables declaration//GEN-END:variables
}
