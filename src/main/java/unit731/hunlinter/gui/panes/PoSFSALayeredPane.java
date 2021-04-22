/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package unit731.hunlinter.gui.panes;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.MainFrame;
import unit731.hunlinter.datastructures.fsa.lookup.DictionaryLookup;
import unit731.hunlinter.datastructures.fsa.lookup.WordData;
import unit731.hunlinter.datastructures.fsa.stemming.Dictionary;
import unit731.hunlinter.gui.FontHelper;
import unit731.hunlinter.gui.GUIHelper;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.languages.WordTokenizer;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.services.eventbus.EventBusService;
import unit731.hunlinter.services.eventbus.EventHandler;
import unit731.hunlinter.services.log.ExceptionHelper;
import unit731.hunlinter.services.system.Debouncer;
import unit731.hunlinter.services.text.ArrayHelper;
import unit731.hunlinter.services.text.StringHelper;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;


public class PoSFSALayeredPane extends JLayeredPane{

	@Serial
	private static final long serialVersionUID = 1011325870107687156L;

	private static final String LEMMA_START = "/[";
	private static final String LEMMA_END = "]";
	private static final String READINGS_DELIMITER = "|";

	private static final int DEBOUNCER_INTERVAL = 600;

	private final Debouncer<PoSFSALayeredPane> debouncer = new Debouncer<>(this::processSentence, DEBOUNCER_INTERVAL);

	private final ParserManager parserManager;

	private final JFileChooser openPoSDictionaryFileChooser;
	private String formerFilterInputText;
	private DictionaryLookup dictionaryLookup;

	private WordTokenizer wordTokenizer;
	private Charset charset;


	public PoSFSALayeredPane(final ParserManager parserManager){
		Objects.requireNonNull(parserManager, "Parser manager cannot be null");

		this.parserManager = parserManager;


		openPoSDictionaryFileChooser = new JFileChooser();
		openPoSDictionaryFileChooser.setFileFilter(new FileNameExtensionFilter("FSA files", "dict"));
		openPoSDictionaryFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);


		initComponents();


		//add "fontable" property
		FontHelper.addFontableProperty(textField, resultTextArea);

		GUIHelper.addUndoManager(textField);

		EventBusService.subscribe(this);
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      inputLabel = new javax.swing.JLabel();
      textField = new javax.swing.JTextField();
      resultScrollPane = new javax.swing.JScrollPane();
      resultTextArea = new javax.swing.JTextArea();
      openPoSFSAButton = new javax.swing.JButton();

      inputLabel.setText("Sentence:");

      textField.setFont(FontHelper.getCurrentFont());
      textField.setToolTipText("hit `enter` to add");
      textField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            textFieldKeyReleased(evt);
         }
      });

      resultTextArea.setEditable(false);
      resultTextArea.setColumns(20);
      resultTextArea.setFont(FontHelper.getCurrentFont());
      resultTextArea.setRows(5);
      resultScrollPane.setViewportView(resultTextArea);

      openPoSFSAButton.setText("Load PoS FSA");
      openPoSFSAButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            openPoSFSAButtonActionPerformed(evt);
         }
      });

      setLayer(inputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(textField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(resultScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(openPoSFSAButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(resultScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 909, Short.MAX_VALUE)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(inputLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(textField)
                  .addGap(18, 18, 18)
                  .addComponent(openPoSFSAButton)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(textField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(inputLabel)
               .addComponent(openPoSFSAButton))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(resultScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 222, Short.MAX_VALUE)
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents

   private void textFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textFieldKeyReleased
		debouncer.call(this);
   }//GEN-LAST:event_textFieldKeyReleased

   private void openPoSFSAButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openPoSFSAButtonActionPerformed
		openPoSFSAButton.setEnabled(false);

		final int projectSelected = openPoSDictionaryFileChooser.showOpenDialog(this);
		if(projectSelected == JFileChooser.APPROVE_OPTION){
			final File baseFile = openPoSDictionaryFileChooser.getSelectedFile();
			loadFSAFile(baseFile.toPath());
		}
   }//GEN-LAST:event_openPoSFSAButtonActionPerformed

	private void loadFSAFile(final Path basePath){
		try{
			dictionaryLookup = new DictionaryLookup(Dictionary.read(basePath));


			textField.requestFocusInWindow();

			formerFilterInputText = null;
			if(StringUtils.isNotBlank(textField.getText()))
				processSentence();
		}
		catch(final Exception e){
			JOptionPane.showMessageDialog(this, "Error while loading Part-of-Speech FSA\n\n"
				+ ExceptionHelper.getMessageNoLineNumber(e), "Error", JOptionPane.ERROR_MESSAGE);
		}

		openPoSFSAButton.setEnabled(true);
	}

	@EventHandler
	public void clear(final Integer actionCommand){
		if(actionCommand != MainFrame.ACTION_COMMAND_GUI_CLEAR_ALL && actionCommand != MainFrame.ACTION_COMMAND_GUI_CLEAR_POS_DICTIONARY)
			return;

		formerFilterInputText = null;
		textField.setText(null);
	}

	private void processSentence(){
		if(wordTokenizer == null){
			final AffixData affixData = parserManager.getAffixData();
			wordTokenizer = BaseBuilder.getWordTokenizer(affixData.getLanguage());
			charset = affixData.getCharset();
		}

		final String inputText = textField.getText().trim();
		if(formerFilterInputText != null && formerFilterInputText.equals(inputText))
			return;

		formerFilterInputText = inputText;


		resultTextArea.setText(null);

		final StringJoiner sj = new StringJoiner(StringUtils.LF);
		final List<String> tokens = extractTrueWords(wordTokenizer.tokenize(inputText));
		if(dictionaryLookup != null)
			for(final String token : tokens){
				final StringJoiner readings = new StringJoiner(READINGS_DELIMITER);
				final String lowercaseToken = token.toLowerCase(Locale.ROOT);
				final WordData[] datas = dictionaryLookup.lookup(lowercaseToken);
				for(final WordData data : datas){
					final byte[] wholeArray = ArrayHelper.concatenate(data.getStem(), LEMMA_START.getBytes(), data.getWord(), LEMMA_END.getBytes(), data.getTag());
					readings.add(new String(wholeArray, charset));
				}
				sj.add(readings.toString());
			}
		else
			for(final String token : tokens)
				sj.add(token);
		resultTextArea.setText(sj.toString());
	}

	private List<String> extractTrueWords(final Collection<String> tokens){
		final List<String> noWhitespaceTokens = new ArrayList<>(tokens.size());
		for(final String token : tokens)
			if(StringHelper.isWord(token))
				noWhitespaceTokens.add(token);
		return noWhitespaceTokens;
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
   private javax.swing.JLabel inputLabel;
   private javax.swing.JButton openPoSFSAButton;
   private javax.swing.JScrollPane resultScrollPane;
   private javax.swing.JTextArea resultTextArea;
   private javax.swing.JTextField textField;
   // End of variables declaration//GEN-END:variables
}
