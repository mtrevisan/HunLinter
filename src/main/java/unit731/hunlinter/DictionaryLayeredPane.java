package unit731.hunlinter;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import javax.swing.*;
import javax.swing.table.TableModel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.actions.OpenFileAction;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.gui.HunLinterTableModelInterface;
import unit731.hunlinter.gui.JCopyableTable;
import unit731.hunlinter.gui.ProductionTableModel;
import unit731.hunlinter.gui.TableRenderer;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.languages.DictionaryCorrectnessChecker;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.log.ExceptionHelper;
import unit731.hunlinter.services.system.Debouncer;
import unit731.hunlinter.services.system.LoopHelper;


public class DictionaryLayeredPane extends JLayeredPane{

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryLayeredPane.class);

	private static final String TAB = "\t";

	private static final int DEBOUNCER_INTERVAL = 600;


	private final Debouncer<DictionaryLayeredPane> debouncer = new Debouncer<>(this::calculateProductions, DEBOUNCER_INTERVAL);

	private final Packager packager;
	private final ParserManager parserManager;

	private String formerInputText;


	public DictionaryLayeredPane(final Packager packager, final ParserManager parserManager){
		Objects.requireNonNull(packager);
		Objects.requireNonNull(parserManager);

		this.packager = packager;
		this.parserManager = parserManager;


		initComponents();


		//add "fontable" property
		GUIUtils.addFontableProperty(inputTextField);

		GUIUtils.addUndoManager(inputTextField);

		try{
			//FIXME
//			final int iconSize = hypRulesValueLabel.getHeight();
//			final int iconSize = dicTotalProductionsValueLabel.getHeight();
final int iconSize = 17;
			final JPopupMenu copyPopupMenu = new JPopupMenu();
			copyPopupMenu.add(GUIUtils.createPopupCopyMenu(iconSize, copyPopupMenu, GUIUtils::copyCallback));
			GUIUtils.addPopupMenu(copyPopupMenu, table);
		}
		catch(final IOException ignored){}
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      inputLabel = new javax.swing.JLabel();
      inputTextField = new javax.swing.JTextField();
      ruleFlagsAidLabel = new javax.swing.JLabel();
      ruleFlagsAidComboBox = new javax.swing.JComboBox<>();
      scrollPane = new javax.swing.JScrollPane();
      table = new JCopyableTable(){
         @Override
         public String getValueAtRow(final int row){
            final TableModel model = getModel();
            final String production = (String) model.getValueAt(row, 0);
            final String morphologicalFields = (String) model.getValueAt(row, 1);
            final String rule1 = Optional.ofNullable((AffixEntry)model.getValueAt(row, 2))
            .map(AffixEntry::toString)
            .orElse(null);
            final String rule2 = Optional.ofNullable((AffixEntry)model.getValueAt(row, 3))
            .map(AffixEntry::toString)
            .orElse(null);
            final String rule3 = Optional.ofNullable((AffixEntry)model.getValueAt(row, 4))
            .map(AffixEntry::toString)
            .orElse(null);
            final StringJoiner sj = new StringJoiner(TAB);
            LoopHelper.applyIf(new String[]{production, morphologicalFields, rule1, rule2, rule3}, Objects::nonNull, sj::add);
            return sj.toString();
         }
      };
      totalProductionsLabel = new javax.swing.JLabel();
      totalProductionsValueLabel = new javax.swing.JLabel();
      openAidButton = new javax.swing.JButton();
      openAffButton = new javax.swing.JButton();
      openDicButton = new javax.swing.JButton();

      inputLabel.setText("Dictionary entry:");

      inputTextField.setPreferredSize(new java.awt.Dimension(7, 22));
      inputTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            inputTextFieldKeyReleased(evt);
         }
      });

      ruleFlagsAidLabel.setText("Rule flags aid:");

      ruleFlagsAidComboBox.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N

      table.setModel(new ProductionTableModel());
      table.setShowHorizontalLines(false);
      table.setShowVerticalLines(false);
      table.setRowSelectionAllowed(true);
      TableRenderer dicCellRenderer = new TableRenderer();
      for(int i = 0; i < table.getColumnCount(); i ++)
      table.getColumnModel().getColumn(i).setCellRenderer(dicCellRenderer);
      KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, false);
      table.registerKeyboardAction(event -> GUIUtils.copyToClipboard((JCopyableTable)table), copyKeyStroke, JComponent.WHEN_FOCUSED);
      scrollPane.setViewportView(table);

      totalProductionsLabel.setText("Total productions:");

      totalProductionsValueLabel.setText("…");

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
      setLayer(inputTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(ruleFlagsAidLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(ruleFlagsAidComboBox, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(scrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(totalProductionsLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(totalProductionsValueLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
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
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                     .addComponent(inputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addComponent(ruleFlagsAidLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(ruleFlagsAidComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addComponent(inputTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
               .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 909, Short.MAX_VALUE)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(totalProductionsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(totalProductionsValueLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addComponent(openAidButton)
                  .addGap(18, 18, 18)
                  .addComponent(openAffButton)
                  .addGap(18, 18, 18)
                  .addComponent(openDicButton)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(inputLabel)
               .addComponent(inputTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(ruleFlagsAidComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(ruleFlagsAidLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(totalProductionsLabel)
               .addComponent(totalProductionsValueLabel)
               .addComponent(openAffButton)
               .addComponent(openDicButton)
               .addComponent(openAidButton))
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents

	public void initialize(){
		final String language = parserManager.getAffixData().getLanguage();

		final Comparator<String> comparator = Comparator.comparingInt(String::length)
			.thenComparing(BaseBuilder.getComparator(language));
		final Comparator<AffixEntry> comparatorAffix = Comparator.comparingInt((AffixEntry entry) -> entry.toString().length())
			.thenComparing((entry0, entry1) -> BaseBuilder.getComparator(language).compare(entry0.toString(), entry1.toString()));
		GUIUtils.addSorterToTable(table, comparator, comparatorAffix);

		try{
			final AffixData affixData = parserManager.getAffixData();
			final Set<String> compoundRules = affixData.getCompoundRules();


			//affix file:
			if(!compoundRules.isEmpty())
				inputTextField.requestFocusInWindow();
			openAffButton.setEnabled(packager.getAffixFile() != null);
			openDicButton.setEnabled(packager.getDictionaryFile() != null);


			//aid file:
			final List<String> lines = parserManager.getAidParser().getLines();
			ruleFlagsAidComboBox.removeAllItems();
			LoopHelper.forEach(lines, ruleFlagsAidComboBox::addItem);
			//enable combo-box only if an AID file exists
			final boolean aidLinesPresent = !lines.isEmpty();
			ruleFlagsAidComboBox.setEnabled(aidLinesPresent);
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

	public void setCurrentFont(){
		final Font currentFont = GUIUtils.getCurrentFont();
		inputTextField.setFont(currentFont);
		table.setFont(currentFont);
	}

	public void clear(){
		//affix file:
		openAffButton.setEnabled(false);
		openDicButton.setEnabled(false);


		clearAid();

		clearOutputTable(table);

		formerInputText = null;

		//disable menu
		totalProductionsValueLabel.setText(StringUtils.EMPTY);
		inputTextField.setText(null);
		inputTextField.requestFocusInWindow();
		openAidButton.setEnabled(false);
	}

	public void clearAid(){
		ruleFlagsAidComboBox.removeAllItems();
		//enable combo-box only if an AID file exists
		ruleFlagsAidComboBox.setEnabled(false);
	}

	private void inputTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_inputTextFieldKeyReleased
		debouncer.call(this);
	}//GEN-LAST:event_inputTextFieldKeyReleased

	private void calculateProductions(){
		final String inputText = inputTextField.getText().trim();

		if(formerInputText != null && formerInputText.equals(inputText))
			return;
		formerInputText = inputText;

		clearOutputTable(table);

		if(StringUtils.isNotBlank(inputText)){
			try{
				final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(inputText,
					parserManager.getAffixData());
				final Production[] productions = parserManager.getWordGenerator().applyAffixRules(dicEntry);

				final ProductionTableModel dm = (ProductionTableModel)table.getModel();
				dm.setProductions(Arrays.asList(productions));

				//show first row
				final Rectangle cellRect = table.getCellRect(0, 0, true);
				table.scrollRectToVisible(cellRect);

				totalProductionsValueLabel.setText(Integer.toString(productions.length));

				//check for correctness
				int line = 0;
				final DictionaryCorrectnessChecker checker = parserManager.getChecker();
				final TableRenderer dicCellRenderer = (TableRenderer)table.getColumnModel().getColumn(0).getCellRenderer();
				dicCellRenderer.clearErrors();
				for(final Production production : productions){
					try{
						checker.checkProduction(production);
					}
					catch(final Exception e){
						dicCellRenderer.setErrorOnRow(line);

						final StringBuffer sb = new StringBuffer(e.getMessage());
						if(production.hasProductionRules())
							sb.append(" (via ").append(production.getRulesSequence()).append(")");
						String errorMessage = ExceptionHelper.getMessage(e);
						LOGGER.trace("{}, line {}", errorMessage, line);
						LOGGER.info(ParserManager.MARKER_APPLICATION, "{}, line {}", sb.toString(), line);
					}

					line ++;
				}
			}
			catch(final Exception e){
				LOGGER.info(ParserManager.MARKER_APPLICATION, "{} for input {}", e.getMessage(), inputText);
			}
		}
		else
			totalProductionsValueLabel.setText(StringUtils.EMPTY);
	}

	private void clearOutputTable(final JTable table){
		final HunLinterTableModelInterface<?> dm = (HunLinterTableModelInterface<?>)table.getModel();
		dm.clear();
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
   private javax.swing.JTextField inputTextField;
   private javax.swing.JButton openAffButton;
   private javax.swing.JButton openAidButton;
   private javax.swing.JButton openDicButton;
   private javax.swing.JComboBox<String> ruleFlagsAidComboBox;
   private javax.swing.JLabel ruleFlagsAidLabel;
   private javax.swing.JScrollPane scrollPane;
   private javax.swing.JTable table;
   private javax.swing.JLabel totalProductionsLabel;
   private javax.swing.JLabel totalProductionsValueLabel;
   // End of variables declaration//GEN-END:variables
}
