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
import io.github.mtrevisan.hunlinter.gui.JCopyableTable;
import io.github.mtrevisan.hunlinter.gui.components.LabeledPopupMenu;
import io.github.mtrevisan.hunlinter.gui.models.HunLinterTableModelInterface;
import io.github.mtrevisan.hunlinter.gui.models.InflectionTableModel;
import io.github.mtrevisan.hunlinter.gui.renderers.TableRenderer;
import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.languages.DictionaryCorrectnessChecker;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.vos.AffixEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntryFactory;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.services.Packager;
import io.github.mtrevisan.hunlinter.services.eventbus.EventHandler;
import io.github.mtrevisan.hunlinter.services.log.ExceptionHelper;
import io.github.mtrevisan.hunlinter.services.system.Debouncer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.prefs.Preferences;


public class DictionaryLayeredPane extends JLayeredPane{

	@Serial
	private static final long serialVersionUID = 7030870103355904749L;

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryLayeredPane.class);

	private static final String TAB = "\t";

	private static final int DEBOUNCER_INTERVAL = 600;

	private static final String COLUMN_MIN_WIDTH = "column.minWidth";
	private static final String COLUMN_MAX_WIDTH = "column.maxWidth";
	private static final String COLUMN_WIDTH = "column.width";
	private static final String SHOW_MORPHOLOGICAL_FIELDS_COLUMN = "morphologicalFieldsColumn";
	private static final String SHOW_APPLIED_RULE_COLUMNS = "appliedRuleColumns";


	private final Debouncer<DictionaryLayeredPane> debouncer = new Debouncer<>(this::calculateInflections, DEBOUNCER_INTERVAL);

	private final Preferences preferences = Preferences.userNodeForPackage(getClass());


	private final Packager packager;
	private final ParserManager parserManager;
	private DictionaryEntryFactory dictionaryEntryFactory;

	private String formerInputText;


	public DictionaryLayeredPane(final Packager packager, final ParserManager parserManager){
		Objects.requireNonNull(packager, "Packager cannot be null");
		Objects.requireNonNull(parserManager, "Parser manager cannot be null");

		this.packager = packager;
		this.parserManager = parserManager;


		initComponents();


		//add "fontable" property
		FontHelper.addFontableProperty(inputTextField, table);

		GUIHelper.addUndoManager(inputTextField);

		try{
			//FIXME
//			final int iconSize = hypRulesValueLabel.getHeight();
//			final int iconSize = dicTotalInflectionsValueLabel.getHeight();
final int iconSize = 17;
			final JPopupMenu copyPopupMenu = new JPopupMenu();
			copyPopupMenu.add(GUIHelper.createPopupCopyMenu(iconSize, copyPopupMenu, GUIHelper::copyCallback));
			copyPopupMenu.add(GUIHelper.createPopupExportTableMenu(iconSize, copyPopupMenu, GUIHelper::exportTableCallback));
			GUIHelper.addPopupMenu(copyPopupMenu, table);

			//reset columns visibility
			preferences.putBoolean(SHOW_MORPHOLOGICAL_FIELDS_COLUMN, true);
			preferences.putBoolean(SHOW_APPLIED_RULE_COLUMNS, true);
			final JPopupMenu hideColumnsPopupMenu = new LabeledPopupMenu("Show/hide columns");
			hideColumnsPopupMenu.add(GUIHelper.createCheckBoxMenu("Morphological fields",
				true, hideColumnsPopupMenu,
				this::hideMorphologicalFieldsColumn));
			hideColumnsPopupMenu.add(GUIHelper.createCheckBoxMenu("Applied rules",
				true, hideColumnsPopupMenu,
				this::hideAppliedRulesColumns));
			GUIHelper.addPopupMenu(hideColumnsPopupMenu, table.getTableHeader());
		}
		catch(final IOException ignored){}
	}

	private void hideMorphologicalFieldsColumn(final Component invoker){
		final boolean showMorphologicalFieldsColumn = preferences.getBoolean(SHOW_MORPHOLOGICAL_FIELDS_COLUMN, true);

		final TableColumn column = table.getColumnModel().getColumn(1);
		storeDefaultColumnsWidth(column);

		if(showMorphologicalFieldsColumn)
			hideColumn(column);
		else
			setDefaultColumnsWidth(column);

		preferences.putBoolean(SHOW_MORPHOLOGICAL_FIELDS_COLUMN, !showMorphologicalFieldsColumn);
	}

	private void hideAppliedRulesColumns(final Component invoker){
		final boolean showAppliedRuleColumns = preferences.getBoolean(SHOW_APPLIED_RULE_COLUMNS, true);

		final TableColumnModel columnModel = table.getColumnModel();
		storeDefaultColumnsWidth(columnModel.getColumn(1));

		if(showAppliedRuleColumns)
			for(int i = 2; i < 5; i ++)
				hideColumn(columnModel.getColumn(i));
		else
			for(int i = 2; i < 5; i ++)
				setDefaultColumnsWidth(columnModel.getColumn(i));

		preferences.putBoolean(SHOW_APPLIED_RULE_COLUMNS, !showAppliedRuleColumns);
	}

	private void storeDefaultColumnsWidth(final TableColumn column){
		if(preferences.getInt(COLUMN_MIN_WIDTH, -1) < 0){
			preferences.putInt(COLUMN_MIN_WIDTH, column.getMinWidth());
			preferences.putInt(COLUMN_MAX_WIDTH, column.getMaxWidth());
			preferences.putInt(COLUMN_WIDTH, column.getWidth());
		}
	}

	private void setDefaultColumnsWidth(final TableColumn column){
		column.setMinWidth(preferences.getInt(COLUMN_MIN_WIDTH, 15));
		column.setMaxWidth(preferences.getInt(COLUMN_MAX_WIDTH, 2147483647));
		column.setWidth(preferences.getInt(COLUMN_WIDTH, 182));
	}

	private void hideColumn(final TableColumn column){
		column.setMinWidth(0);
		column.setMaxWidth(0);
		column.setWidth(0);
	}

	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      inputLabel = new javax.swing.JLabel();
      inputTextField = new javax.swing.JTextField();
      ruleFlagsAidLabel = new javax.swing.JLabel();
      ruleFlagsAidComboBox = new javax.swing.JComboBox<>();
      scrollPane = new javax.swing.JScrollPane();
      table = new MyJCopyableTable();
      totalInflectionsLabel = new javax.swing.JLabel();
      totalInflectionsValueLabel = new javax.swing.JLabel();
      openAidButton = new javax.swing.JButton();
      openAffButton = new javax.swing.JButton();
      openDicButton = new javax.swing.JButton();

      inputLabel.setText("Dictionary entry:");

		final Font currentFont = FontHelper.getCurrentFont();

		inputTextField.setFont(currentFont);
      inputTextField.setEnabled(false);
      inputTextField.setPreferredSize(new java.awt.Dimension(7, 22));
      inputTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            inputTextFieldKeyReleased(evt);
         }
      });

      ruleFlagsAidLabel.setText("Rule flags aid:");

      ruleFlagsAidComboBox.setFont(currentFont);

      table.setFont(currentFont);
      table.setModel(new InflectionTableModel());
      table.setRowHeight(24);
      table.setShowHorizontalLines(false);
      table.setShowVerticalLines(false);
      table.getTableHeader().setReorderingAllowed(false);
      final KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, false);
      table.registerKeyboardAction(event -> GUIHelper.copyToClipboard((JCopyableTable)table), copyKeyStroke, JComponent.WHEN_FOCUSED);
      GUIHelper.addScrollToFirstRow(table);
      GUIHelper.addScrollToLastRow(table);

      table.setRowSelectionAllowed(true);
      final TableCellRenderer dicCellRenderer = new TableRenderer();
      for(int i = 0; i < table.getColumnCount(); i ++)
      	table.getColumnModel().getColumn(i).setCellRenderer(dicCellRenderer);
      scrollPane.setViewportView(table);

      totalInflectionsLabel.setText("Total inflections:");

      totalInflectionsValueLabel.setText("…");

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
      setLayer(totalInflectionsLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(totalInflectionsValueLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
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
                  .addComponent(totalInflectionsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(totalInflectionsValueLabel)
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
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 161, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(totalInflectionsLabel)
               .addComponent(totalInflectionsValueLabel)
               .addComponent(openAffButton)
               .addComponent(openDicButton)
               .addComponent(openAidButton))
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents

	@EventHandler
	public void initialize(final Integer actionCommand){
		if(actionCommand != MainFrame.ACTION_COMMAND_INITIALIZE)
			return;

		final String language = parserManager.getLanguage();

		final Comparator<String> languageComparator = BaseBuilder.getComparator(language);
		final Comparator<String> comparator = Comparator.comparingInt(String::length)
			.thenComparing(languageComparator);
		final Comparator<AffixEntry> comparatorAffix = Comparator.comparingInt((AffixEntry entry) -> entry.toString().length())
			.thenComparing((entry0, entry1) -> languageComparator.compare(entry0.toString(), entry1.toString()));
		GUIHelper.addSorterToTable(table, comparator, comparatorAffix);

		try{
			dictionaryEntryFactory = new DictionaryEntryFactory(parserManager.getAffixData());

			//affix file:
			inputTextField.setEnabled(true);
			inputTextField.requestFocusInWindow();
			openAffButton.setEnabled(packager.getAffixFile() != null);
			openDicButton.setEnabled(packager.getDictionaryFile() != null);


			//aid file:
			final List<String> lines = parserManager.getAidParser().getLines();
			ruleFlagsAidComboBox.removeAllItems();
			for(final String line : lines)
				ruleFlagsAidComboBox.addItem(line);
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

	@EventHandler
	public void clear(final Integer actionCommand){
		if(actionCommand != MainFrame.ACTION_COMMAND_GUI_CLEAR_ALL && actionCommand != MainFrame.ACTION_COMMAND_GUI_CLEAR_DICTIONARY)
			return;

		//affix file:
		inputTextField.setEnabled(false);
		openAffButton.setEnabled(false);
		openDicButton.setEnabled(false);


		clearOutputTable(table);

		formerInputText = null;

		//disable menu
		totalInflectionsValueLabel.setText(null);
		inputTextField.setText(null);
		inputTextField.requestFocusInWindow();
		openAidButton.setEnabled(false);
	}

	@EventHandler
	public void clearAid(final Integer actionCommand){
		if(actionCommand != MainFrame.ACTION_COMMAND_GUI_CLEAR_AID)
			return;

		ruleFlagsAidComboBox.removeAllItems();
		//enable combo-box only if an AID file exists
		ruleFlagsAidComboBox.setEnabled(false);
	}

	private void inputTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_inputTextFieldKeyReleased
		debouncer.call(this);
	}//GEN-LAST:event_inputTextFieldKeyReleased

	private void calculateInflections(){
		final String text = inputTextField.getText().trim();

		if(text.equals(formerInputText))
			return;
		formerInputText = text;

		clearOutputTable(table);

		if(StringUtils.isNotBlank(text)){
			try{
				final DictionaryEntry dicEntry = dictionaryEntryFactory.createFromDictionaryLine(text);
				final List<Inflection> inflections = parserManager.getWordGenerator().applyAffixRules(dicEntry);

				final HunLinterTableModelInterface<Inflection> dm = (InflectionTableModel)table.getModel();
				dm.setInflections(inflections);

				//show first row
				final Rectangle cellRect = table.getCellRect(0, 0, true);
				table.scrollRectToVisible(cellRect);

				totalInflectionsValueLabel.setText(Integer.toString(inflections.size()));

				//check for correctness
				int index = 0;
				final DictionaryCorrectnessChecker checker = parserManager.getChecker();
				final TableRenderer dicCellRenderer = (TableRenderer)table.getColumnModel().getColumn(0).getCellRenderer();
				dicCellRenderer.clearErrors();
				final StringBuilder sb = new StringBuilder();
				for(final Inflection inflection : inflections){
					try{
						checker.checkInflection(inflection, index);
					}
					catch(final Exception e){
						dicCellRenderer.setErrorOnRow(index);

						sb.setLength(0);
						sb.append(e.getMessage());
						if(inflection.hasInflectionRules())
							sb.append(" (via ").append(inflection.getRulesSequence()).append(")");
						final String errorMessage = ExceptionHelper.getMessage(e);
						LOGGER.trace("{}, line {}", errorMessage, index);
						LOGGER.info(ParserManager.MARKER_APPLICATION, "{}, line {}", sb, index);
					}

					index ++;
				}
			}
			catch(final Exception e){
				LOGGER.info(ParserManager.MARKER_APPLICATION, "{} for input {}", e.getMessage(), text);
			}
		}
		else
			totalInflectionsValueLabel.setText(null);
	}

	private void clearOutputTable(final JTable table){
		final HunLinterTableModelInterface<?> dm = (HunLinterTableModelInterface<?>)table.getModel();
		dm.clear();
	}

	private static class MyJCopyableTable extends JCopyableTable{
		@Override
		public String getValueAtRow(final int row){
			final TableModel model = getModel();
			final String inflection = (String)model.getValueAt(row, 0);
			final String morphologicalFields = (String)model.getValueAt(row, 1);
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
			if(Objects.nonNull(inflection))
				sj.add(inflection);
			if(Objects.nonNull(morphologicalFields))
				sj.add(morphologicalFields);
			if(Objects.nonNull(rule1))
				sj.add(rule1);
			if(Objects.nonNull(rule2))
				sj.add(rule2);
			if(Objects.nonNull(rule3))
				sj.add(rule3);
			return sj.toString();
		}
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
   private javax.swing.JTextField inputTextField;
   private javax.swing.JButton openAffButton;
   private javax.swing.JButton openAidButton;
   private javax.swing.JButton openDicButton;
   private javax.swing.JComboBox<String> ruleFlagsAidComboBox;
   private javax.swing.JLabel ruleFlagsAidLabel;
   private javax.swing.JScrollPane scrollPane;
   private javax.swing.JTable table;
   private javax.swing.JLabel totalInflectionsLabel;
   private javax.swing.JLabel totalInflectionsValueLabel;
   // End of variables declaration//GEN-END:variables
}
