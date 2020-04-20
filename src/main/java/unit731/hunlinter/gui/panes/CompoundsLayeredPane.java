package unit731.hunlinter.gui.panes;

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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import javax.swing.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.MainFrame;
import unit731.hunlinter.actions.OpenFileAction;
import unit731.hunlinter.gui.FontHelper;
import unit731.hunlinter.gui.models.CompoundTableModel;
import unit731.hunlinter.gui.GUIHelper;
import unit731.hunlinter.gui.models.HunLinterTableModelInterface;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.AffixParser;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.eventbus.EventBusService;
import unit731.hunlinter.services.eventbus.EventHandler;
import unit731.hunlinter.services.system.Debouncer;
import unit731.hunlinter.workers.WorkerManager;

import static unit731.hunlinter.services.system.LoopHelper.forEach;


public class CompoundsLayeredPane extends JLayeredPane implements ActionListener{

	private static final long serialVersionUID = 4289096513559178063L;

	private static final Logger LOGGER = LoggerFactory.getLogger(CompoundsLayeredPane.class);

	private static final int DEBOUNCER_INTERVAL = 600;


	private final Debouncer<CompoundsLayeredPane> debouncer = new Debouncer<>(this::calculateCompoundInflections, DEBOUNCER_INTERVAL);

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
		FontHelper.addFontableProperty(inputTextArea, table);

		GUIHelper.addUndoManager(inputTextArea);

		EventBusService.subscribe(CompoundsLayeredPane.this);
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      inputLabel = new javax.swing.JLabel();
      inputComboBox = new javax.swing.JComboBox<>();
      limitLabel = new javax.swing.JLabel();
      limitComboBox = new javax.swing.JComboBox<>();
      ruleFlagsAidLabel = new javax.swing.JLabel();
      ruleFlagsAidComboBox = new javax.swing.JComboBox<>();
      scrollPane = new javax.swing.JScrollPane();
      table = new javax.swing.JTable();
      inputScrollPane = new javax.swing.JScrollPane();
      inputTextArea = new javax.swing.JTextArea();
      loadInputButton = new javax.swing.JButton();
      openAidButton = new javax.swing.JButton();
      openAffButton = new javax.swing.JButton();
      openDicButton = new javax.swing.JButton();

      inputLabel.setText("Compound rule:");

      inputComboBox.setEditable(true);
      inputComboBox.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
      inputComboBox.getEditor().getEditorComponent().addKeyListener(new KeyAdapter(){
         @Override
         public void keyReleased(final KeyEvent evt){
            cmpInputComboBoxKeyReleased();
         }
      });
      inputComboBox.addItemListener(new ItemListener(){
         @Override
         public void itemStateChanged(final ItemEvent evt){
            cmpInputComboBoxKeyReleased();
         }
      });

      limitLabel.setText("Limit:");

      limitComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "20", "50", "100", "500", "1000" }));
      limitComboBox.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            limitComboBoxActionPerformed(evt);
         }
      });

      ruleFlagsAidLabel.setText("Rule flags aid:");

      ruleFlagsAidComboBox.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N

      table.setModel(new CompoundTableModel());
      table.setShowHorizontalLines(false);
      table.setShowVerticalLines(false);
      KeyStroke cancelKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
      table.registerKeyboardAction(this, cancelKeyStroke, JComponent.WHEN_FOCUSED);

      table.setRowSelectionAllowed(true);
      scrollPane.setViewportView(table);

      inputTextArea.setEditable(false);
      inputTextArea.setColumns(20);
      inputScrollPane.setViewportView(inputTextArea);

      loadInputButton.setText("Load input from dictionary");
      loadInputButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            loadInputButtonActionPerformed(evt);
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

      setLayer(inputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(inputComboBox, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(limitLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(limitComboBox, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(ruleFlagsAidLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(ruleFlagsAidComboBox, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(scrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(inputScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(loadInputButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
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
                     .addComponent(inputLabel)
                     .addComponent(ruleFlagsAidLabel))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(ruleFlagsAidComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addGroup(layout.createSequentialGroup()
                        .addComponent(inputComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 728, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(limitLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(limitComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(inputScrollPane)
                     .addGroup(layout.createSequentialGroup()
                        .addComponent(loadInputButton)
                        .addGap(0, 0, Short.MAX_VALUE)))
                  .addGap(18, 18, 18)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(scrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 443, javax.swing.GroupLayout.PREFERRED_SIZE)
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
               .addComponent(inputLabel)
               .addComponent(inputComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(limitComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(limitLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(ruleFlagsAidComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(ruleFlagsAidLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
               .addComponent(scrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
               .addComponent(inputScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 155, Short.MAX_VALUE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(loadInputButton)
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

   private void limitComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_limitComboBoxActionPerformed
      final String inputText = ((String)inputComboBox.getEditor().getItem()).trim();
      final int limit = Integer.parseInt(limitComboBox.getItemAt(limitComboBox.getSelectedIndex()));
      final String inputCompounds = inputTextArea.getText();

      if(StringUtils.isNotBlank(inputText) && StringUtils.isNotBlank(inputCompounds)){
         try{
            //FIXME transfer into ParserManager
            final Inflection[] words;
            final WordGenerator wordGenerator = parserManager.getWordGenerator();
            final AffixData affixData = parserManager.getAffixData();
				final String[] input = StringUtils.split(inputCompounds, '\n');
            if(inputText.equals(affixData.getCompoundFlag())){
               final int maxCompounds = affixData.getCompoundMaxWordCount();
               words = wordGenerator.applyCompoundFlag(input, limit, maxCompounds);
            }
            else
					words = wordGenerator.applyCompoundRules(input, inputText, limit);

            final CompoundTableModel dm = (CompoundTableModel)table.getModel();
            dm.setInflections(Arrays.asList(words));
         }
         catch(final Exception e){
            LOGGER.info(ParserManager.MARKER_APPLICATION, "{} for input {}", e.getMessage(), inputText);
         }
      }
      else
      	clearOutputTable(table);
   }//GEN-LAST:event_limitComboBoxActionPerformed

   private void loadInputButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadInputButtonActionPerformed
      final AffixParser affParser = parserManager.getAffParser();
      final FlagParsingStrategy strategy = affParser.getAffixData()
      .getFlagParsingStrategy();
      workerManager.createCompoundRulesWorker(
         worker -> {
            inputComboBox.setEnabled(false);
            limitComboBox.setEnabled(false);
            inputTextArea.setEnabled(false);
            inputTextArea.setText(null);
            loadInputButton.setEnabled(false);

            worker.addPropertyChangeListener(propertyChangeListener);
            worker.execute();
         },
         compounds -> {
            final StringJoiner sj = new StringJoiner(StringUtils.LF);
				forEach(compounds, compound -> sj.add(compound.toString(strategy)));
            inputTextArea.setText(sj.toString());
            inputTextArea.setCaretPosition(0);
         },
         worker -> {
            inputComboBox.setEnabled(true);
            limitComboBox.setEnabled(true);
            inputTextArea.setEnabled(true);
            if(worker.isCancelled())
            loadInputButton.setEnabled(true);
         }
      );
   }//GEN-LAST:event_loadInputButtonActionPerformed

	private void cmpInputComboBoxKeyReleased(){
		debouncer.call(this);
	}

	@EventHandler
	public void initialize(final Integer actionCommand){
		//noinspection NumberEquality
		if(actionCommand != MainFrame.ACTION_COMMAND_INITIALIZE)
			return;

		final String language = parserManager.getLanguage();
		final Comparator<String> comparator = Comparator.comparingInt(String::length)
			.thenComparing(BaseBuilder.getComparator(language));
		final Comparator<AffixEntry> comparatorAffix = Comparator.comparingInt((AffixEntry entry) -> entry.toString().length())
			.thenComparing((entry0, entry1) -> BaseBuilder.getComparator(language).compare(entry0.toString(), entry1.toString()));
		GUIHelper.addSorterToTable(table, comparator, comparatorAffix);

		final AffixData affixData = parserManager.getAffixData();
		final Set<String> compoundRules = affixData.getCompoundRules();


		//affix file:
		if(!compoundRules.isEmpty()){
			inputComboBox.removeAllItems();
			forEach(compoundRules, inputComboBox::addItem);
			final String compoundFlag = affixData.getCompoundFlag();
			if(compoundFlag != null)
				inputComboBox.addItem(compoundFlag);
			inputComboBox.setEnabled(true);
			inputComboBox.setSelectedItem(null);
		}
		openAffButton.setEnabled(packager.getAffixFile() != null);
		openDicButton.setEnabled(packager.getDictionaryFile() != null);


		//aid file:
		final List<String> lines = parserManager.getAidParser().getLines();
		ruleFlagsAidComboBox.removeAllItems();
		forEach(lines, ruleFlagsAidComboBox::addItem);
		final boolean aidLinesPresent = !lines.isEmpty();
		//enable combo-box only if an AID file exists
		ruleFlagsAidComboBox.setEnabled(aidLinesPresent);
		openAidButton.setEnabled(aidLinesPresent);
	}

	@EventHandler
	public void clear(final Integer actionCommand){
		//noinspection NumberEquality
		if(actionCommand != MainFrame.ACTION_COMMAND_GUI_CLEAR_ALL && actionCommand != MainFrame.ACTION_COMMAND_GUI_CLEAR_COMPOUNDS)
			return;

		formerCompoundInputText = null;

		inputTextArea.setText(null);
		inputTextArea.setEnabled(true);
		loadInputButton.setEnabled(true);

		//affix file:
		inputComboBox.removeAllItems();
		inputComboBox.setEnabled(true);
	}

	@EventHandler
	public void clearAid(final Integer actionCommand){
		//noinspection NumberEquality
		if(actionCommand != MainFrame.ACTION_COMMAND_GUI_CLEAR_AID)
			return;

		ruleFlagsAidComboBox.removeAllItems();
		//enable combo-box only if an AID file exists
		ruleFlagsAidComboBox.setEnabled(false);
	}

	private void clearOutputTable(final JTable table){
		final HunLinterTableModelInterface<?> dm = (HunLinterTableModelInterface<?>)table.getModel();
		dm.clear();
	}

	private void calculateCompoundInflections(){
		final String inputText = ((String)inputComboBox.getEditor().getItem()).trim();

		limitComboBox.setEnabled(StringUtils.isNotBlank(inputText));

		if(formerCompoundInputText != null && formerCompoundInputText.equals(inputText))
			return;
		formerCompoundInputText = inputText;

		limitComboBoxActionPerformed(null);
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
   private javax.swing.JComboBox<String> inputComboBox;
   private javax.swing.JLabel inputLabel;
   private javax.swing.JScrollPane inputScrollPane;
   private javax.swing.JTextArea inputTextArea;
   private javax.swing.JComboBox<String> limitComboBox;
   private javax.swing.JLabel limitLabel;
   private javax.swing.JButton loadInputButton;
   private javax.swing.JButton openAffButton;
   private javax.swing.JButton openAidButton;
   private javax.swing.JButton openDicButton;
   private javax.swing.JComboBox<String> ruleFlagsAidComboBox;
   private javax.swing.JLabel ruleFlagsAidLabel;
   private javax.swing.JScrollPane scrollPane;
   private javax.swing.JTable table;
   // End of variables declaration//GEN-END:variables
}
