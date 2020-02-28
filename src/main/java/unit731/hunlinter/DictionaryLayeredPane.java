package unit731.hunlinter;

import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.actions.OpenFileAction;
import unit731.hunlinter.gui.AscendingDescendingUnsortedTableRowSorter;
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
import unit731.hunlinter.services.system.JavaHelper;


public class DictionaryLayeredPane extends JLayeredPane{

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryLayeredPane.class);

	private static final String TAB = "\t";

	private static final int DEBOUNCER_INTERVAL = 600;


	private final Packager packager;
	private final ParserManager parserManager;

	private final Debouncer<DictionaryLayeredPane> productionDebouncer = new Debouncer<>(this::calculateProductions, DEBOUNCER_INTERVAL);

	private String formerInputText;


	public DictionaryLayeredPane(final Packager packager, final ParserManager parserManager){
		Objects.requireNonNull(packager);
		Objects.requireNonNull(parserManager);

		this.packager = packager;
		this.parserManager = parserManager;


		initComponents();


		//add "fontable" property
		GUIUtils.addFontableProperty(dicInputTextField);

		GUIUtils.addUndoManager(dicInputTextField);

		try{
			final int iconSize = dicTotalProductionsValueLabel.getHeight();
			final JPopupMenu copyPopupMenu = new JPopupMenu();
			copyPopupMenu.add(GUIUtils.createPopupCopyMenu(iconSize, copyPopupMenu, GUIUtils::copyCallback));
			GUIUtils.addPopupMenu(copyPopupMenu, dicTable);
		}
		catch(final IOException ignored){}
	}

	@SuppressWarnings("unchecked")
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      dicRuleFlagsAidLabel = new javax.swing.JLabel();
      dicRuleFlagsAidComboBox = new javax.swing.JComboBox<>();
      dicScrollPane = new javax.swing.JScrollPane();
      dicTable = new JCopyableTable(){
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
            return JavaHelper.nullableToStream(production, morphologicalFields, rule1, rule2, rule3)
            .collect(Collectors.joining(TAB));
         }
      };
      dicTotalProductionsLabel = new javax.swing.JLabel();
      dicTotalProductionsValueLabel = new javax.swing.JLabel();
      openAidButton = new javax.swing.JButton();
      openAffButton = new javax.swing.JButton();
      openDicButton = new javax.swing.JButton();
      dicInputLabel = new javax.swing.JLabel();
      dicInputTextField = new javax.swing.JTextField();

      dicRuleFlagsAidLabel.setText("Rule flags aid:");

      dicRuleFlagsAidComboBox.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N

      dicTable.setModel(new ProductionTableModel());
      dicTable.setShowHorizontalLines(false);
      dicTable.setShowVerticalLines(false);
      dicTable.setRowSelectionAllowed(true);
      TableRenderer dicCellRenderer = new TableRenderer();
      for(int i = 0; i < dicTable.getColumnCount(); i ++)
      dicTable.getColumnModel().getColumn(i).setCellRenderer(dicCellRenderer);
      KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, false);
      dicTable.registerKeyboardAction(event -> GUIUtils.copyToClipboard((JCopyableTable)dicTable), copyKeyStroke, JComponent.WHEN_FOCUSED);
      dicScrollPane.setViewportView(dicTable);

      dicTotalProductionsLabel.setText("Total productions:");

      dicTotalProductionsValueLabel.setText("…");

      openAidButton.setAction(new OpenFileAction(() -> parserManager.getAidFile(), packager));
      openAidButton.setText("Open Aid");
      openAidButton.setEnabled(false);

      openAffButton.setAction(new OpenFileAction(Packager.KEY_FILE_AFFIX, packager));
      openAffButton.setText("Open Affix");
      openAffButton.setEnabled(false);

      openDicButton.setAction(new OpenFileAction(Packager.KEY_FILE_DICTIONARY, packager));
      openDicButton.setText("Open Dictionary");
      openDicButton.setEnabled(false);

      dicInputLabel.setText("Dictionary entry:");

      dicInputTextField.setPreferredSize(new java.awt.Dimension(7, 22));
      dicInputTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            dicInputTextFieldKeyReleased(evt);
         }
      });

      setLayer(dicRuleFlagsAidLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(dicRuleFlagsAidComboBox, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(dicScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(dicTotalProductionsLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(dicTotalProductionsValueLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(openAidButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(openAffButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(openDicButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(dicInputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(dicInputTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                     .addComponent(dicInputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addComponent(dicRuleFlagsAidLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(dicRuleFlagsAidComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addComponent(dicInputTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
               .addComponent(dicScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 909, Short.MAX_VALUE)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(dicTotalProductionsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(dicTotalProductionsValueLabel)
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
               .addComponent(dicInputLabel)
               .addComponent(dicInputTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(dicRuleFlagsAidComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(dicRuleFlagsAidLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(dicScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(dicTotalProductionsLabel)
               .addComponent(dicTotalProductionsValueLabel)
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
		addSorterToTable(dicTable, comparator, comparatorAffix);

		try{
			final AffixData affixData = parserManager.getAffixData();
			final Set<String> compoundRules = affixData.getCompoundRules();


			//affix file:
			if(!compoundRules.isEmpty())
				dicInputTextField.requestFocusInWindow();
			openAffButton.setEnabled(packager.getAffixFile() != null);
			openDicButton.setEnabled(packager.getDictionaryFile() != null);


			//aid file:
			final List<String> lines = parserManager.getAidParser().getLines();
			final boolean aidLinesPresent = !lines.isEmpty();
			dicRuleFlagsAidComboBox.removeAllItems();
			if(aidLinesPresent)
				lines.forEach(dicRuleFlagsAidComboBox::addItem);
			//enable combo-box only if an AID file exists
			dicRuleFlagsAidComboBox.setEnabled(aidLinesPresent);
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

	public void clear(){
		//affix file:
		openAffButton.setEnabled(false);
		openDicButton.setEnabled(false);


		dicRuleFlagsAidComboBox.removeAllItems();

		clearOutputTable(dicTable);

		formerInputText = null;

		//disable menu
		dicTotalProductionsValueLabel.setText(StringUtils.EMPTY);
		dicInputTextField.setText(null);
		dicInputTextField.requestFocusInWindow();
		//enable combo-box only if an AID file exists
		dicRuleFlagsAidComboBox.setEnabled(false);
		openAidButton.setEnabled(false);
	}

	private void clearOutputTable(final JTable table){
		final HunLinterTableModelInterface<?> dm = (HunLinterTableModelInterface<?>)table.getModel();
		dm.clear();
	}


	private void dicInputTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_dicInputTextFieldKeyReleased
		productionDebouncer.call(this);
	}//GEN-LAST:event_dicInputTextFieldKeyReleased

	private void calculateProductions(final DictionaryLayeredPane frame){
		final String inputText = StringUtils.strip(dicInputTextField.getText());

		if(formerInputText != null && formerInputText.equals(inputText))
			return;
		formerInputText = inputText;

		if(StringUtils.isNotBlank(inputText)){
			try{
				final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(inputText,
					parserManager.getAffixData());
				final List<Production> productions = parserManager.getWordGenerator().applyAffixRules(dicEntry);

				final ProductionTableModel dm = (ProductionTableModel)dicTable.getModel();
				dm.setProductions(productions);

				//show first row
				final Rectangle cellRect = dicTable.getCellRect(0, 0, true);
				dicTable.scrollRectToVisible(cellRect);

				dicTotalProductionsValueLabel.setText(Integer.toString(productions.size()));

				//check for correctness
				int line = 0;
				final DictionaryCorrectnessChecker checker = parserManager.getChecker();
				final TableRenderer dicCellRenderer = (TableRenderer)dicTable.getColumnModel().getColumn(0).getCellRenderer();
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
		else{
			clearOutputTable(dicTable);
			dicTotalProductionsValueLabel.setText(StringUtils.EMPTY);
		}
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
   private javax.swing.JLabel dicInputLabel;
   private javax.swing.JTextField dicInputTextField;
   private javax.swing.JComboBox<String> dicRuleFlagsAidComboBox;
   private javax.swing.JLabel dicRuleFlagsAidLabel;
   private javax.swing.JScrollPane dicScrollPane;
   private javax.swing.JTable dicTable;
   private javax.swing.JLabel dicTotalProductionsLabel;
   private javax.swing.JLabel dicTotalProductionsValueLabel;
   private javax.swing.JButton openAffButton;
   private javax.swing.JButton openAidButton;
   private javax.swing.JButton openDicButton;
   // End of variables declaration//GEN-END:variables
}
