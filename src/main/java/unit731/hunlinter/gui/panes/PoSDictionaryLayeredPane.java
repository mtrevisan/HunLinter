package unit731.hunlinter.gui.panes;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.Objects;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import unit731.hunlinter.MainFrame;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.eventbus.EventBusService;
import unit731.hunlinter.services.eventbus.EventHandler;
import unit731.hunlinter.services.system.Debouncer;


public class PoSDictionaryLayeredPane extends JLayeredPane{

	private static final long serialVersionUID = 1011325870107687156L;

	private static final int DEBOUNCER_INTERVAL = 600;


	private final Debouncer<PoSDictionaryLayeredPane> debouncer = new Debouncer<>(this::processSentence, DEBOUNCER_INTERVAL);

	private final Packager packager;
	private final ParserManager parserManager;

	private JFileChooser openPoSDictionaryFileChooser;
	private String formerFilterInputText;


	public PoSDictionaryLayeredPane(final Packager packager, final ParserManager parserManager){
		Objects.requireNonNull(packager);
		Objects.requireNonNull(parserManager);

		this.packager = packager;
		this.parserManager = parserManager;


		openPoSDictionaryFileChooser = new JFileChooser();
		openPoSDictionaryFileChooser.setFileFilter(new FileNameExtensionFilter("FSA files", "dict"));
		openPoSDictionaryFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);


		initComponents();


		//add "fontable" property
		GUIUtils.addFontableProperty(textField, resultTextArea);

		GUIUtils.addUndoManager(textField);

		EventBusService.subscribe(PoSDictionaryLayeredPane.this);
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      inputLabel = new javax.swing.JLabel();
      textField = new javax.swing.JTextField();
      resultScrollPane = new javax.swing.JScrollPane();
      resultTextArea = new javax.swing.JTextArea();
      posRecordedLabel = new javax.swing.JLabel();
      posRecordedValueLabel = new javax.swing.JLabel();
      openPoSFSAButton = new javax.swing.JButton();

      inputLabel.setText("Sentence:");

      textField.setToolTipText("hit `enter` to add");
      textField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            textFieldKeyReleased(evt);
         }
      });

      resultTextArea.setEditable(false);
      resultTextArea.setColumns(20);
      resultTextArea.setRows(5);
      resultScrollPane.setViewportView(resultTextArea);

      posRecordedLabel.setText("PoS recorded:");

      posRecordedValueLabel.setText("…");

      openPoSFSAButton.setText("Load PoS FSA");
      openPoSFSAButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            openPoSFSAButtonActionPerformed(evt);
         }
      });

      setLayer(inputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(textField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(resultScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(posRecordedLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(posRecordedValueLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(openPoSFSAButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(resultScrollPane)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(posRecordedLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(posRecordedValueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 732, Short.MAX_VALUE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(openPoSFSAButton))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addComponent(inputLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(textField)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(textField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(inputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(resultScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(posRecordedLabel)
               .addComponent(posRecordedValueLabel)
               .addComponent(openPoSFSAButton))
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents

   private void textFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textFieldKeyReleased
		debouncer.call(this);
   }//GEN-LAST:event_textFieldKeyReleased

   private void openPoSFSAButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openPoSFSAButtonActionPerformed
		final int projectSelected = openPoSDictionaryFileChooser.showOpenDialog(this);
		if(projectSelected == JFileChooser.APPROVE_OPTION){
			final File baseFile = openPoSDictionaryFileChooser.getSelectedFile();
			loadFile(baseFile.toPath());
		}
   }//GEN-LAST:event_openPoSFSAButtonActionPerformed

	private void loadFile(final Path basePath){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		//TODO
	}

	@EventHandler
	public void setCurrentFont(final Integer actionCommand){
		//noinspection NumberEquality
		if(actionCommand != MainFrame.ACTION_COMMAND_SET_CURRENT_FONT)
			return;

		final Font currentFont = GUIUtils.getCurrentFont();
		resultTextArea.setFont(currentFont);
	}

	@EventHandler
	public void clear(final Integer actionCommand){
		//noinspection NumberEquality
		if(actionCommand != MainFrame.ACTION_COMMAND_GUI_CLEAR_ALL && actionCommand != MainFrame.ACTION_COMMAND_GUI_CLEAR_POS_DICTIONARY)
			return;

		formerFilterInputText = null;
		textField.setText(null);
	}

	private void processSentence(){
		final String unmodifiedException = textField.getText().trim();
		if(formerFilterInputText != null && formerFilterInputText.equals(unmodifiedException))
			return;

		formerFilterInputText = unmodifiedException;

		//TODO
	}

	private void updatePoSCounter(){
		//TODO
		posRecordedValueLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(parserManager.getSexParser().getExceptionsCounter()));
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
   private javax.swing.JLabel inputLabel;
   private javax.swing.JButton openPoSFSAButton;
   private javax.swing.JLabel posRecordedLabel;
   private javax.swing.JLabel posRecordedValueLabel;
   private javax.swing.JScrollPane resultScrollPane;
   private javax.swing.JTextArea resultTextArea;
   private javax.swing.JTextField textField;
   // End of variables declaration//GEN-END:variables
}
