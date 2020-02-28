package unit731.hunlinter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.actions.OpenFileAction;
import unit731.hunlinter.gui.AscendingDescendingUnsortedTableRowSorter;
import unit731.hunlinter.gui.CompoundTableModel;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.gui.HunLinterTableModelInterface;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.AffixParser;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.system.Debouncer;
import unit731.hunlinter.workers.WorkerManager;


public class CompoundsLayeredPane extends JLayeredPane implements ActionListener{

	private static final Logger LOGGER = LoggerFactory.getLogger(CompoundsLayeredPane.class);

	private static final int DEBOUNCER_INTERVAL = 600;


	private final Debouncer<CompoundsLayeredPane> debouncer = new Debouncer<>(this::calculateCompoundProductions, DEBOUNCER_INTERVAL);

	private final Packager packager;
	private final ParserManager parserManager;
	private final WorkerManager workerManager;
	private final PropertyChangeListener propertyChangeListener;

	private String formerCompoundInputText;


	public CompoundsLayeredPane(final Packager packager, final ParserManager parserManager, final WorkerManager workerManager,
			final PropertyChangeListener propertyChangeListener){
		Objects.requireNonNull(packager);
		Objects.requireNonNull(parserManager);
		Objects.requireNonNull(workerManager);
		Objects.requireNonNull(propertyChangeListener);

		this.packager = packager;
		this.parserManager = parserManager;
		this.workerManager = workerManager;
		this.propertyChangeListener = propertyChangeListener;


		initComponents();


		//add "fontable" property
		GUIUtils.addFontableProperty(cmpInputTextArea, cmpTable);

		GUIUtils.addUndoManager(cmpInputTextArea);
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      cmpInputLabel = new javax.swing.JLabel();
      cmpInputComboBox = new javax.swing.JComboBox<>();
      cmpLimitLabel = new javax.swing.JLabel();
      cmpLimitComboBox = new javax.swing.JComboBox<>();
      cmpRuleFlagsAidLabel = new javax.swing.JLabel();
      cmpRuleFlagsAidComboBox = new javax.swing.JComboBox<>();
      cmpScrollPane = new javax.swing.JScrollPane();
      cmpTable = new javax.swing.JTable();
      cmpInputScrollPane = new javax.swing.JScrollPane();
      cmpInputTextArea = new javax.swing.JTextArea();
      cmpLoadInputButton = new javax.swing.JButton();
      openAidButton = new javax.swing.JButton();
      openAffButton = new javax.swing.JButton();
      openDicButton = new javax.swing.JButton();

      cmpInputLabel.setText("Compound rule:");

      cmpInputComboBox.setEditable(true);
      cmpInputComboBox.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
      cmpInputComboBox.getEditor().getEditorComponent().addKeyListener(new KeyAdapter(){
         @Override
         public void keyReleased(final KeyEvent evt){
            cmpInputComboBoxKeyReleased();
         }
      });
      cmpInputComboBox.addItemListener(new ItemListener(){
         @Override
         public void itemStateChanged(final ItemEvent evt){
            cmpInputComboBoxKeyReleased();
         }
      });

      cmpLimitLabel.setText("Limit:");

      cmpLimitComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "20", "50", "100", "500", "1000" }));
      cmpLimitComboBox.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            cmpLimitComboBoxActionPerformed(evt);
         }
      });

      cmpRuleFlagsAidLabel.setText("Rule flags aid:");

      cmpRuleFlagsAidComboBox.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N

      cmpTable.setModel(new CompoundTableModel());
      cmpTable.setShowHorizontalLines(false);
      cmpTable.setShowVerticalLines(false);
      KeyStroke cancelKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
      cmpTable.registerKeyboardAction(this, cancelKeyStroke, JComponent.WHEN_FOCUSED);

      cmpTable.setRowSelectionAllowed(true);
      cmpScrollPane.setViewportView(cmpTable);

      cmpInputTextArea.setEditable(false);
      cmpInputTextArea.setColumns(20);
      cmpInputScrollPane.setViewportView(cmpInputTextArea);

      cmpLoadInputButton.setText("Load input from dictionary");
      cmpLoadInputButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            cmpLoadInputButtonActionPerformed(evt);
         }
      });

      openAidButton.setAction(new OpenFileAction(parserManager::getAidFile, packager));
      openAidButton.setText("Open Aid");
      openAidButton.setEnabled(false);

      openAffButton.setAction(new OpenFileAction(Packager.KEY_FILE_AFFIX, packager));
      openAffButton.setText("Open Affix");
      openAffButton.setEnabled(false);

      openDicButton.setAction(new OpenFileAction(Packager.KEY_FILE_DICTIONARY, packager));
      openDicButton.setText("Open Dictionary");
      openDicButton.setEnabled(false);

      setLayer(cmpInputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(cmpInputComboBox, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(cmpLimitLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(cmpLimitComboBox, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(cmpRuleFlagsAidLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(cmpRuleFlagsAidComboBox, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(cmpScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(cmpInputScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(cmpLoadInputButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(openAidButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(openAffButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(openDicButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(cmpInputLabel)
                     .addComponent(cmpRuleFlagsAidLabel))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(cmpRuleFlagsAidComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addGroup(layout.createSequentialGroup()
                        .addComponent(cmpInputComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 728, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(cmpLimitLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmpLimitComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(cmpInputScrollPane)
                     .addGroup(layout.createSequentialGroup()
                        .addComponent(cmpLoadInputButton)
                        .addGap(0, 0, Short.MAX_VALUE)))
                  .addGap(18, 18, 18)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(cmpScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 443, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(openAidButton)
                        .addGap(18, 18, 18)
                        .addComponent(openAffButton)
                        .addGap(18, 18, 18)
                        .addComponent(openDicButton)))))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(cmpInputLabel)
               .addComponent(cmpInputComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(cmpLimitComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(cmpLimitLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(cmpRuleFlagsAidComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(cmpRuleFlagsAidLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
               .addComponent(cmpScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
               .addComponent(cmpInputScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 155, Short.MAX_VALUE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(cmpLoadInputButton)
               .addComponent(openAffButton)
               .addComponent(openDicButton)
               .addComponent(openAidButton))
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents

	@Override
	public void actionPerformed(final ActionEvent event){
		workerManager.checkForAbortion();
	}

   private void cmpLimitComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmpLimitComboBoxActionPerformed
      final String inputText = StringUtils.strip((String)cmpInputComboBox.getEditor().getItem());
      final int limit = Integer.parseInt(cmpLimitComboBox.getItemAt(cmpLimitComboBox.getSelectedIndex()));
      final String inputCompounds = cmpInputTextArea.getText();

      if(StringUtils.isNotBlank(inputText) && StringUtils.isNotBlank(inputCompounds)){
         try{
            //FIXME transfer into ParserManager
            final List<Production> words;
            final WordGenerator wordGenerator = parserManager.getWordGenerator();
            final AffixData affixData = parserManager.getAffixData();
            if(inputText.equals(affixData.getCompoundFlag())){
               int maxCompounds = affixData.getCompoundMaxWordCount();
               words = wordGenerator.applyCompoundFlag(StringUtils.split(inputCompounds, '\n'), limit,
                  maxCompounds);
            }
            else
            words = wordGenerator.applyCompoundRules(StringUtils.split(inputCompounds, '\n'), inputText,
               limit);

            final CompoundTableModel dm = (CompoundTableModel)cmpTable.getModel();
            dm.setProductions(words);
         }
         catch(final Exception e){
            LOGGER.info(ParserManager.MARKER_APPLICATION, "{} for input {}", e.getMessage(), inputText);
         }
      }
      else
      clearOutputTable(cmpTable);
   }//GEN-LAST:event_cmpLimitComboBoxActionPerformed

   private void cmpLoadInputButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmpLoadInputButtonActionPerformed
      final AffixParser affParser = parserManager.getAffParser();
      final FlagParsingStrategy strategy = affParser.getAffixData()
      .getFlagParsingStrategy();
      workerManager.createCompoundRulesWorker(
         worker -> {
            cmpInputComboBox.setEnabled(false);
            cmpLimitComboBox.setEnabled(false);
            cmpInputTextArea.setEnabled(false);
            cmpInputTextArea.setText(null);
            cmpLoadInputButton.setEnabled(false);

            worker.addPropertyChangeListener(propertyChangeListener);
            worker.execute();
         },
         compounds -> {
            final StringJoiner sj = new StringJoiner("\n");
            compounds.forEach(compound -> sj.add(compound.toString(strategy)));
            cmpInputTextArea.setText(sj.toString());
            cmpInputTextArea.setCaretPosition(0);
         },
         worker -> {
            cmpInputComboBox.setEnabled(true);
            cmpLimitComboBox.setEnabled(true);
            cmpInputTextArea.setEnabled(true);
            if(worker.isCancelled())
            cmpLoadInputButton.setEnabled(true);
         }
      );
   }//GEN-LAST:event_cmpLoadInputButtonActionPerformed

	private void cmpInputComboBoxKeyReleased(){
		debouncer.call(this);
	}

	public void initialize(){
		final String language = parserManager.getAffixData().getLanguage();

		final Comparator<String> comparator = Comparator.comparingInt(String::length)
			.thenComparing(BaseBuilder.getComparator(language));
		final Comparator<AffixEntry> comparatorAffix = Comparator.comparingInt((AffixEntry entry) -> entry.toString().length())
			.thenComparing((entry0, entry1) -> BaseBuilder.getComparator(language).compare(entry0.toString(), entry1.toString()));
		addSorterToTable(cmpTable, comparator, comparatorAffix);

		try{
			final AffixData affixData = parserManager.getAffixData();
			final Set<String> compoundRules = affixData.getCompoundRules();


			//affix file:
			if(!compoundRules.isEmpty()){
				cmpInputComboBox.removeAllItems();
				compoundRules.forEach(cmpInputComboBox::addItem);
				final String compoundFlag = affixData.getCompoundFlag();
				if(compoundFlag != null)
					cmpInputComboBox.addItem(compoundFlag);
				cmpInputComboBox.setEnabled(true);
				cmpInputComboBox.setSelectedItem(null);
			}
			openAffButton.setEnabled(packager.getAffixFile() != null);
			openDicButton.setEnabled(packager.getDictionaryFile() != null);


			//aid file:
			final List<String> lines = parserManager.getAidParser().getLines();
			final boolean aidLinesPresent = !lines.isEmpty();
			cmpRuleFlagsAidComboBox.removeAllItems();
			if(aidLinesPresent)
				lines.forEach(cmpRuleFlagsAidComboBox::addItem);
			//enable combo-box only if an AID file exists
			cmpRuleFlagsAidComboBox.setEnabled(aidLinesPresent);
			openAidButton.setEnabled(aidLinesPresent);
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
		cmpInputTextArea.setFont(currentFont);
		cmpTable.setFont(currentFont);
	}

	public void clear(){
		formerCompoundInputText = null;

		cmpInputTextArea.setText(null);
		cmpInputTextArea.setEnabled(true);
		cmpLoadInputButton.setEnabled(true);

		//affix file:
		cmpInputComboBox.removeAllItems();
		cmpInputComboBox.setEnabled(true);

		clearAid();
	}

	public void clearAid(){
		cmpRuleFlagsAidComboBox.removeAllItems();
		//enable combo-box only if an AID file exists
		cmpRuleFlagsAidComboBox.setEnabled(false);
	}

	private void clearOutputTable(final JTable table){
		final HunLinterTableModelInterface<?> dm = (HunLinterTableModelInterface<?>)table.getModel();
		dm.clear();
	}

	private void calculateCompoundProductions(){
		final String inputText = StringUtils.strip((String)cmpInputComboBox.getEditor().getItem());

		cmpLimitComboBox.setEnabled(StringUtils.isNotBlank(inputText));

		if(formerCompoundInputText != null && formerCompoundInputText.equals(inputText))
			return;
		formerCompoundInputText = inputText;

		cmpLimitComboBoxActionPerformed(null);
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
   private javax.swing.JComboBox<String> cmpInputComboBox;
   private javax.swing.JLabel cmpInputLabel;
   private javax.swing.JScrollPane cmpInputScrollPane;
   private javax.swing.JTextArea cmpInputTextArea;
   private javax.swing.JComboBox<String> cmpLimitComboBox;
   private javax.swing.JLabel cmpLimitLabel;
   private javax.swing.JButton cmpLoadInputButton;
   private javax.swing.JComboBox<String> cmpRuleFlagsAidComboBox;
   private javax.swing.JLabel cmpRuleFlagsAidLabel;
   private javax.swing.JScrollPane cmpScrollPane;
   private javax.swing.JTable cmpTable;
   private javax.swing.JButton openAffButton;
   private javax.swing.JButton openAidButton;
   private javax.swing.JButton openDicButton;
   // End of variables declaration//GEN-END:variables
}
