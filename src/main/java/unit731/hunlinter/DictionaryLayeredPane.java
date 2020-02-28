package unit731.hunlinter;

import java.awt.*;

import org.apache.commons.lang3.tuple.Pair;
import org.xml.sax.SAXException;
import unit731.hunlinter.actions.OpenFileAction;
import unit731.hunlinter.gui.AscendingDescendingUnsortedTableRowSorter;
import unit731.hunlinter.gui.AutoCorrectTableModel;
import unit731.hunlinter.gui.JCopyableTable;
import unit731.hunlinter.gui.ProjectFolderFilter;
import unit731.hunlinter.interfaces.HunLintable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.gui.HunLinterTableModelInterface;
import unit731.hunlinter.gui.ProductionTableModel;
import unit731.hunlinter.gui.RecentFilesMenu;
import unit731.hunlinter.gui.ThesaurusTableModel;
import unit731.hunlinter.gui.TableRenderer;
import unit731.hunlinter.languages.DictionaryCorrectnessChecker;
import unit731.hunlinter.languages.Orthography;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.autocorrect.AutoCorrectParser;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.thesaurus.SynonymsEntry;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.exceptions.LanguageNotChosenException;
import unit731.hunlinter.workers.exceptions.ProjectNotFoundException;
import unit731.hunlinter.workers.ProjectLoaderWorker;
import unit731.hunlinter.workers.core.WorkerAbstract;
import unit731.hunlinter.parsers.hyphenation.Hyphenation;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;
import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;
import unit731.hunlinter.parsers.thesaurus.ThesaurusEntry;
import unit731.hunlinter.services.downloader.DownloaderHelper;
import unit731.hunlinter.services.system.JavaHelper;
import unit731.hunlinter.services.text.StringHelper;
import unit731.hunlinter.services.log.ApplicationLogAppender;
import unit731.hunlinter.services.system.Debouncer;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.PatternHelper;
import unit731.hunlinter.services.log.ExceptionHelper;


public class DictionaryLayeredPane extends JFrame implements ActionListener, PropertyChangeListener, HunLintable{

	private static final long serialVersionUID = 6772959670167531135L;

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryLayeredPane.class);

	private static final Pattern EXTRACTOR = PatternHelper.pattern("(?:TRY |FX [^ ]+ )([^\r\n\\d]+)[\r\n]+");

	private final static String FONT_FAMILY_NAME_PREFIX = "font.familyName.";
	private final static String FONT_SIZE_PREFIX = "font.size.";
	private final static String UPDATE_STARTUP_CHECK = "update.startupCheck";

	private static final String TAB = "\t";

	private static final int DEBOUNCER_INTERVAL = 600;
	private static final Pattern PATTERN_POINTS_AND_NUMBERS_AND_EQUALS_AND_MINUS = PatternHelper.pattern("[.\\d=-]");

	private String formerInputText;
	private String formerCompoundInputText;
	private String formerFilterThesaurusText;
	private String formerHyphenationText;
	private String formerFilterIncorrectText;
	private String formerFilterCorrectText;
	private String formerFilterSentenceException;
	private String formerFilterWordException;
	private final JFileChooser openProjectPathFileChooser;

	private final Preferences preferences = Preferences.userNodeForPackage(getClass());
	private final ParserManager parserManager;
	private final Packager packager;

	private RecentFilesMenu recentProjectsMenu;
	private final Debouncer<DictionaryLayeredPane> productionDebouncer = new Debouncer<>(this::calculateProductions, DEBOUNCER_INTERVAL);
	private final Debouncer<DictionaryLayeredPane> compoundProductionDebouncer = new Debouncer<>(this::calculateCompoundProductions, DEBOUNCER_INTERVAL);
	private final Debouncer<DictionaryLayeredPane> theFilterDebouncer = new Debouncer<>(this::filterThesaurus, DEBOUNCER_INTERVAL);
	private final Debouncer<DictionaryLayeredPane> hypDebouncer = new Debouncer<>(this::hyphenate, DEBOUNCER_INTERVAL);
	private final Debouncer<DictionaryLayeredPane> hypAddRuleDebouncer = new Debouncer<>(this::hyphenateAddRule, DEBOUNCER_INTERVAL);
	private final Debouncer<DictionaryLayeredPane> acoFilterDebouncer = new Debouncer<>(this::filterAutoCorrect, DEBOUNCER_INTERVAL);
	private final Debouncer<DictionaryLayeredPane> sexFilterDebouncer = new Debouncer<>(this::filterSentenceExceptions, DEBOUNCER_INTERVAL);
	private final Debouncer<DictionaryLayeredPane> wexFilterDebouncer = new Debouncer<>(this::filterWordExceptions, DEBOUNCER_INTERVAL);

	private ProjectLoaderWorker prjLoaderWorker;
	private final WorkerManager workerManager;

	private JMenuItem popupMergeMenuItem;


	public DictionaryLayeredPane(){
		packager = new Packager();
		parserManager = new ParserManager(packager, this);
		workerManager = new WorkerManager(parserManager, this);


		initComponents();


		recentProjectsMenu.setEnabled(recentProjectsMenu.hasEntries());
		filEmptyRecentProjectsMenuItem.setEnabled(recentProjectsMenu.hasEntries());

		//add "fontable" property
		GUIUtils.addFontableProperty(parsingResultTextArea,
			dicInputTextField,
			cmpInputTextArea, cmpTable,
			theTable, theSynonymsTextField,
			hypWordTextField, hypAddRuleTextField, hypSyllabationOutputLabel, hypRulesOutputLabel, hypAddRuleSyllabationOutputLabel,
			acoTable, acoIncorrectTextField, acoCorrectTextField,
			sexTextField,
			wexTextField);

		GUIUtils.addUndoManager(dicInputTextField,
			cmpInputTextArea,
			theSynonymsTextField,
			hypWordTextField, hypAddRuleTextField,
			acoIncorrectTextField, acoCorrectTextField,
			sexTextField,
			wexTextField);

		try{
			final int iconSize = hypRulesOutputLabel.getHeight();
			final JPopupMenu copyPopupMenu = new JPopupMenu();
			copyPopupMenu.add(GUIUtils.createPopupCopyMenu(iconSize, copyPopupMenu, GUIUtils::copyCallback));
			final JPopupMenu mergeCopyRemovePopupMenu = new JPopupMenu();
			popupMergeMenuItem = GUIUtils.createPopupMergeMenu(iconSize, mergeCopyRemovePopupMenu, this::mergeThesaurusRow);
			popupMergeMenuItem.setEnabled(false);
			mergeCopyRemovePopupMenu.add(popupMergeMenuItem);
			mergeCopyRemovePopupMenu.add(GUIUtils.createPopupCopyMenu(iconSize, mergeCopyRemovePopupMenu, GUIUtils::copyCallback));
			mergeCopyRemovePopupMenu.add(GUIUtils.createPopupRemoveMenu(iconSize, mergeCopyRemovePopupMenu, this::removeSelectedRows));
			final JPopupMenu copyRemovePopupMenu = new JPopupMenu();
			copyRemovePopupMenu.add(GUIUtils.createPopupCopyMenu(iconSize, copyRemovePopupMenu, GUIUtils::copyCallback));
			copyRemovePopupMenu.add(GUIUtils.createPopupRemoveMenu(iconSize, copyRemovePopupMenu, this::removeSelectedRows));
			GUIUtils.addPopupMenu(copyPopupMenu, dicTable,
				theSynonymsRecordedOutputLabel,
				hypSyllabationOutputLabel, hypRulesOutputLabel, hypAddRuleSyllabationOutputLabel);
			GUIUtils.addPopupMenu(mergeCopyRemovePopupMenu, theTable);
			GUIUtils.addPopupMenu(copyRemovePopupMenu, acoTable);
		}
		catch(final IOException ignored){}

		ApplicationLogAppender.addTextArea(parsingResultTextArea, ParserManager.MARKER_APPLICATION);


		openProjectPathFileChooser = new JFileChooser();
		openProjectPathFileChooser.setFileFilter(new ProjectFolderFilter("Project folders"));
		openProjectPathFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		//disable the "All files" option
		openProjectPathFileChooser.setAcceptAllFileFilterUsed(false);
		try{
			final BufferedImage projectFolderImg = ImageIO.read(GUIUtils.class.getResourceAsStream("/project_folder.png"));
			final ImageIcon projectFolderIcon = new ImageIcon(projectFolderImg);
			openProjectPathFileChooser.setFileView(new FileView(){
				//choose the right icon for the folder
				@Override
				public Icon getIcon(final File file){
					return (Packager.isProjectFolder(file)? projectFolderIcon:
						FileSystemView.getFileSystemView().getSystemIcon(file));
				}
			});
		}
		catch(final IOException ignored){}


		//check for updates
		if(preferences.getBoolean(UPDATE_STARTUP_CHECK, true)){
			JavaHelper.executeOnEventDispatchThread(() -> {
				try{
					final FileDownloaderDialog dialog = new FileDownloaderDialog(this);
					GUIUtils.addCancelByEscapeKey(dialog);
					dialog.setLocationRelativeTo(this);
					dialog.setVisible(true);
				}
				catch(final Exception ignored){}
			});
		}
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      dicInputLabel = new javax.swing.JLabel();
      dicInputTextField = new javax.swing.JTextField();
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
      dicTotalProductionsOutputLabel = new javax.swing.JLabel();
      openAidButton = new javax.swing.JButton();
      openAffButton = new javax.swing.JButton();
      openDicButton = new javax.swing.JButton();

      setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
      setTitle((String)DownloaderHelper.getApplicationProperties().get(DownloaderHelper.PROPERTY_KEY_ARTIFACT_ID));
      setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png")));
      setMinimumSize(new java.awt.Dimension(964, 534));

      dicInputLabel.setLabelFor(dicInputTextField);
      dicInputLabel.setText("Dictionary entry:");

      dicInputTextField.setPreferredSize(new java.awt.Dimension(7, 22));
      dicInputTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            dicInputTextFieldKeyReleased(evt);
         }
      });

      dicRuleFlagsAidLabel.setLabelFor(dicRuleFlagsAidComboBox);
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

      dicTotalProductionsLabel.setLabelFor(dicTotalProductionsOutputLabel);
      dicTotalProductionsLabel.setText("Total productions:");

      dicTotalProductionsOutputLabel.setText("…");

      openAidButton.setAction(new OpenFileAction(() -> parserManager.getAidFile(), packager));
      openAidButton.setText("Open Aid");
      openAidButton.setEnabled(false);

      openAffButton.setAction(new OpenFileAction(Packager.KEY_FILE_AFFIX, packager));
      openAffButton.setText("Open Affix");
      openAffButton.setEnabled(false);

      openDicButton.setAction(new OpenFileAction(Packager.KEY_FILE_DICTIONARY, packager));
      openDicButton.setText("Open Dictionary");
      openDicButton.setEnabled(false);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
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
                  .addComponent(dicTotalProductionsOutputLabel)
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
               .addComponent(dicTotalProductionsOutputLabel)
               .addComponent(openAffButton)
               .addComponent(openDicButton)
               .addComponent(openAidButton))
            .addContainerGap())
      );

      pack();
      setLocationRelativeTo(null);
   }// </editor-fold>//GEN-END:initComponents


	private void calculateProductions(final DictionaryLayeredPane frame){
		final String inputText = StringUtils.strip(frame.dicInputTextField.getText());

		if(formerInputText != null && formerInputText.equals(inputText))
			return;
		formerInputText = inputText;

		if(StringUtils.isNotBlank(inputText)){
			try{
				final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(inputText,
					frame.parserManager.getAffixData());
				final List<Production> productions = frame.parserManager.getWordGenerator().applyAffixRules(dicEntry);

				final ProductionTableModel dm = (ProductionTableModel)frame.dicTable.getModel();
				dm.setProductions(productions);

				//show first row
				final Rectangle cellRect = frame.dicTable.getCellRect(0, 0, true);
				frame.dicTable.scrollRectToVisible(cellRect);

				frame.dicTotalProductionsOutputLabel.setText(Integer.toString(productions.size()));

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
			frame.clearOutputTable(frame.dicTable);
			frame.dicTotalProductionsOutputLabel.setText(StringUtils.EMPTY);
		}
	}

	private void calculateCompoundProductions(final DictionaryLayeredPane frame){
		final String inputText = StringUtils.strip((String)frame.cmpInputComboBox.getEditor().getItem());

		cmpLimitComboBox.setEnabled(StringUtils.isNotBlank(inputText));

		if(formerCompoundInputText != null && formerCompoundInputText.equals(inputText))
			return;
		formerCompoundInputText = inputText;

		frame.cmpLimitComboBoxActionPerformed(null);
	}


	private void filterThesaurus(DictionaryLayeredPane frame){
		final String unmodifiedSearchText = StringUtils.strip(frame.theSynonymsTextField.getText());

		popupMergeMenuItem.setEnabled(StringUtils.isNotBlank(unmodifiedSearchText));

		if(formerFilterThesaurusText != null && formerFilterThesaurusText.equals(unmodifiedSearchText))
			return;

		formerFilterThesaurusText = unmodifiedSearchText;

		final Pair<String[], String[]> pair = ThesaurusParser.extractComponentsForFilter(unmodifiedSearchText);
		//if text to be inserted is already fully contained into the thesaurus, do not enable the button
		final boolean alreadyContained = parserManager.getTheParser().contains(pair.getLeft(), pair.getRight());
		theAddButton.setEnabled(!alreadyContained);

		@SuppressWarnings("unchecked")
		final TableRowSorter<ThesaurusTableModel> sorter = (TableRowSorter<ThesaurusTableModel>)frame.theTable.getRowSorter();
		if(StringUtils.isNotBlank(unmodifiedSearchText)){
			final Pair<String, String> searchText = ThesaurusParser.prepareTextForFilter(pair.getLeft(), pair.getRight());
			JavaHelper.executeOnEventDispatchThread(() -> sorter.setRowFilter(RowFilter.regexFilter(searchText.getRight())));
		}
		else
			sorter.setRowFilter(null);
	}

	public void mergeThesaurusRow(final Component invoker){
		final int selectedRow = theTable.convertRowIndexToModel(theTable.getSelectedRow());
		final ThesaurusTableModel dm = (ThesaurusTableModel)theTable.getModel();
		final ThesaurusEntry synonyms = dm.getSynonymsAt(selectedRow);
		final SynonymsEntry newSynonyms = new SynonymsEntry(theSynonymsTextField.getText());

		//filter synonyms with same part-of-speech
		final List<SynonymsEntry> filteredSynonymsEntries = synonyms.getSynonyms().stream()
			.filter(syns -> syns.hasSamePartOfSpeeches(newSynonyms.getPartOfSpeeches()))
			.collect(Collectors.toList());
		if(filteredSynonymsEntries.isEmpty())
			JOptionPane.showMessageDialog(null,
				"No synonyms with same part-of-speech present.\r\nCannot merge automatically, do it manually.",
				"Warning", JOptionPane.WARNING_MESSAGE);
		else{
			//show merge dialog
			final ThesaurusMergeDialog dialog = new ThesaurusMergeDialog(synonyms.getDefinition(), newSynonyms,
				filteredSynonymsEntries, null);
			GUIUtils.addCancelByEscapeKey(dialog);
			dialog.setLocationRelativeTo(this);
			dialog.setVisible(true);

			if(dialog.isMerged())
				theSynonymsTextField.setText(dialog.getMerge());
		}
	}

	public void removeSelectedRows(final Component invoker){
		if(invoker == theTable)
			removeSelectedRowsFromThesaurus();
		else if(invoker == acoTable)
			removeSelectedRowsFromAutoCorrect();
	}

	public void removeSelectedRowsFromThesaurus(){
		try{
			final int selectedRow = theTable.convertRowIndexToModel(theTable.getSelectedRow());
			final ThesaurusTableModel dm = (ThesaurusTableModel)theTable.getModel();
			final String selectedDefinition = (String)dm.getValueAt(selectedRow, 0);
			final String selectedSynonyms = (String)dm.getValueAt(selectedRow, 1);
			parserManager.getTheParser()
				.deleteDefinitionAndSynonyms(selectedDefinition, selectedSynonyms);

			dm.setSynonyms(parserManager.getTheParser().getSynonymsDictionary());
			updateSynonymsCounter();

			//… and save the files
			parserManager.storeThesaurusFiles();


			//redo filtering, that is re-set the state of the button (it may have changed)
			final String unmodifiedSearchText = theSynonymsTextField.getText();
			if(StringUtils.isNotBlank(unmodifiedSearchText)){
				final Pair<String[], String[]> pair = ThesaurusParser.extractComponentsForFilter(unmodifiedSearchText);
				//if text to be inserted is already fully contained into the thesaurus, do not enable the button
				final boolean alreadyContained = parserManager.getTheParser().contains(pair.getLeft(), pair.getRight());
				theAddButton.setEnabled(!alreadyContained);
			}
		}
		catch(final Exception e){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Deletion error: {}", e.getMessage());
		}
	}

	public void removeSelectedRowsFromAutoCorrect(){
		try{
			final int selectedRow = acoTable.convertRowIndexToModel(acoTable.getSelectedRow());
			parserManager.getAcoParser().deleteCorrection(selectedRow);

			final AutoCorrectTableModel dm = (AutoCorrectTableModel)acoTable.getModel();
			dm.fireTableDataChanged();

			updateCorrectionsCounter();

			//… and save the files
			parserManager.storeAutoCorrectFile();
		}
		catch(final Exception e){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Deletion error: {}", e.getMessage());
		}
	}

	private void filterAutoCorrect(final DictionaryLayeredPane frame){
		final String unmodifiedIncorrectText = StringUtils.strip(frame.acoIncorrectTextField.getText());
		final String unmodifiedCorrectText = StringUtils.strip(frame.acoCorrectTextField.getText());
		if(formerFilterIncorrectText != null && formerFilterIncorrectText.equals(unmodifiedIncorrectText)
				&& formerFilterCorrectText != null && formerFilterCorrectText.equals(unmodifiedCorrectText))
			return;

		formerFilterIncorrectText = unmodifiedIncorrectText;
		formerFilterCorrectText = unmodifiedCorrectText;

		final Pair<String, String> pair = AutoCorrectParser.extractComponentsForFilter(unmodifiedIncorrectText,
			unmodifiedCorrectText);
		final String incorrect = pair.getLeft();
		final String correct = pair.getRight();
		//if text to be inserted is already fully contained into the thesaurus, do not enable the button
		final boolean alreadyContained = parserManager.getAcoParser().contains(incorrect, correct);
		acoAddButton.setEnabled(StringUtils.isNotBlank(unmodifiedIncorrectText) && StringUtils.isNotBlank(unmodifiedCorrectText)
			&& !unmodifiedIncorrectText.equals(unmodifiedCorrectText) && !alreadyContained);

		@SuppressWarnings("unchecked")
		final TableRowSorter<AutoCorrectTableModel> sorter = (TableRowSorter<AutoCorrectTableModel>)frame.acoTable.getRowSorter();
		if(StringUtils.isNotBlank(unmodifiedIncorrectText) || StringUtils.isNotBlank(unmodifiedCorrectText)){
			final Pair<String, String> searchText = AutoCorrectParser.prepareTextForFilter(incorrect, correct);
			final RowFilter<AutoCorrectTableModel, Integer> filterIncorrect = RowFilter.regexFilter(searchText.getLeft(), 0);
			final RowFilter<AutoCorrectTableModel, Integer> filterCorrect = RowFilter.regexFilter(searchText.getRight(), 1);
			JavaHelper.executeOnEventDispatchThread(() -> sorter.setRowFilter(RowFilter.andFilter(Arrays.asList(filterIncorrect,
				filterCorrect))));
		}
		else
			sorter.setRowFilter(null);
	}

	private void filterSentenceExceptions(final DictionaryLayeredPane frame){
		final String unmodifiedException = StringUtils.strip(frame.sexTextField.getText());
		if(formerFilterSentenceException != null && formerFilterSentenceException.equals(unmodifiedException))
			return;

		formerFilterSentenceException = unmodifiedException;

		//if text to be inserted is already fully contained into the thesaurus, do not enable the button
		final boolean alreadyContained = parserManager.getSexParser().contains(unmodifiedException);
		sexAddButton.setEnabled(StringUtils.isNotBlank(unmodifiedException) && unmodifiedException.endsWith(".")
			&& !alreadyContained);


		sexTagPanel.applyFilter(StringUtils.isNotBlank(unmodifiedException)? unmodifiedException: null);
	}

	private void filterWordExceptions(final DictionaryLayeredPane frame){
		final String unmodifiedException = StringUtils.strip(frame.wexTextField.getText());
		if(formerFilterWordException != null && formerFilterWordException.equals(unmodifiedException))
			return;

		formerFilterWordException = unmodifiedException;

		//if text to be inserted is already fully contained into the thesaurus, do not enable the button
		final boolean alreadyContained = parserManager.getWexParser().contains(unmodifiedException);
		wexAddButton.setEnabled(StringUtils.isNotBlank(unmodifiedException) && StringHelper.countUppercases(unmodifiedException) > 1 && !alreadyContained);


		wexTagPanel.applyFilter(StringUtils.isNotBlank(unmodifiedException)? unmodifiedException: null);
	}

	private void cmpInputComboBoxKeyReleased(){
		compoundProductionDebouncer.call(this);
	}

	private void dicInputTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_dicInputTextFieldKeyReleased
		productionDebouncer.call(this);
	}//GEN-LAST:event_dicInputTextFieldKeyReleased


	@Override
	public void actionPerformed(ActionEvent event){
		//FIXME introduce a checkAbortion case?
		if(prjLoaderWorker != null && prjLoaderWorker.getState() == SwingWorker.StateValue.STARTED){
			prjLoaderWorker.pause();

			final Object[] options = {"Abort", "Cancel"};
			final int answer = JOptionPane.showOptionDialog(this,
				"Do you really want to abort the project loader task?", "Warning!",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
			if(answer == JOptionPane.YES_OPTION){
				prjLoaderWorker.cancel();

				dicLinterMenuItem.setEnabled(true);
				theLinterMenuItem.setEnabled(true);
				LOGGER.info(ParserManager.MARKER_APPLICATION, "Project loader aborted");

				prjLoaderWorker = null;
			}
			else if(answer == JOptionPane.NO_OPTION || answer == JOptionPane.CLOSED_OPTION){
				prjLoaderWorker.resume();

				setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			}
		}

		workerManager.checkForAbortion();
	}

	private void loadFile(final Path basePath){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		clearResultTextArea();

		if(parserManager != null)
			parserManager.stopFileListener();

		loadFileInternal(basePath);
	}

	@Override
	public void loadFileInternal(final Path projectPath){
		//clear all
		loadFileCancelled(null);

		clearAllParsers();

		mainTabbedPane.setSelectedIndex(0);


		if(prjLoaderWorker == null || prjLoaderWorker.isDone()){
			dicLinterMenuItem.setEnabled(false);
			theLinterMenuItem.setEnabled(false);

			try{
				packager.reload(projectPath != null? projectPath: packager.getProjectPath());

				final List<String> availableLanguages = packager.getAvailableLanguages();
				final AtomicReference<String> language = new AtomicReference<>(availableLanguages.get(0));
				if(availableLanguages.size() > 1){
					//choose between available languages
					final Consumer<String> onSelection = language::set;
					final LanguageChooserDialog dialog = new LanguageChooserDialog(availableLanguages, onSelection, this);
					GUIUtils.addCancelByEscapeKey(dialog);
					dialog.setLocationRelativeTo(this);
					dialog.setVisible(true);

					if(!dialog.languageChosen())
						throw new LanguageNotChosenException("Language not chosen loading " + projectPath);
				}
				//load appropriate files based on current language
				packager.extractConfigurationFolders(language.get());

				setTitle(DownloaderHelper.getApplicationProperties().get(DownloaderHelper.PROPERTY_KEY_ARTIFACT_ID) + " : "
					+ packager.getLanguage());

				temporarilyChooseAFont(packager.getAffixFile().toPath());

				prjLoaderWorker = new ProjectLoaderWorker(packager, parserManager, this::loadFileCompleted, this::loadFileCancelled);
				prjLoaderWorker.addPropertyChangeListener(this);
				prjLoaderWorker.execute();

				filOpenProjectMenuItem.setEnabled(false);
			}
			catch(final IOException | SAXException | ProjectNotFoundException | LanguageNotChosenException e){
				loadFileCancelled(e);

				LOGGER.error(ParserManager.MARKER_APPLICATION, e.getMessage());

				LOGGER.error("A bad error occurred while loading the project", e);
			}
		}
	}

	/** Chooses one font (in case of reading errors) */
	private void temporarilyChooseAFont(final Path basePath){
		try{
			final String content = new String(Files.readAllBytes(basePath));
			final String[] extractions = PatternHelper.extract(content, EXTRACTOR, 10);
			final String sample = String.join(StringUtils.EMPTY, String.join(StringUtils.EMPTY, extractions).chars()
				.mapToObj(Character::toString).collect(Collectors.toSet()));
			parsingResultTextArea.setFont(GUIUtils.chooseBestFont(sample));
		}
		catch(final IOException ignored){}
	}

	private void setCurrentFont(){
		final Font currentFont = GUIUtils.getCurrentFont();
		parsingResultTextArea.setFont(currentFont);

		dicInputTextField.setFont(currentFont);
		dicTable.setFont(currentFont);

		cmpInputTextArea.setFont(currentFont);
		cmpTable.setFont(currentFont);

		theSynonymsTextField.setFont(currentFont);
		theTable.setFont(currentFont);

		hypWordTextField.setFont(currentFont);
		hypSyllabationOutputLabel.setFont(currentFont);
		hypAddRuleTextField.setFont(currentFont);
		hypAddRuleSyllabationOutputLabel.setFont(currentFont);

		acoIncorrectTextField.setFont(currentFont);
		acoCorrectTextField.setFont(currentFont);
		acoTable.setFont(currentFont);
	}

	private void addSorterToTable(final JTable table, final Comparator<String> comparator,
			final Comparator<AffixEntry> comparatorAffix){
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

	private void loadFileCompleted(){
		//restore default font (changed for reporting reading errors)
		setCurrentFont();

		parserManager.registerFileListener();
		parserManager.startFileListener();

		final String language = parserManager.getAffixData().getLanguage();

		final Comparator<String> comparator = Comparator.comparingInt(String::length)
			.thenComparing(BaseBuilder.getComparator(language));
		final Comparator<AffixEntry> comparatorAffix = Comparator.comparingInt((AffixEntry entry) -> entry.toString().length())
			.thenComparing((entry0, entry1) -> BaseBuilder.getComparator(language).compare(entry0.toString(), entry1.toString()));
		addSorterToTable(dicTable, comparator, comparatorAffix);

		try{
			filOpenProjectMenuItem.setEnabled(true);
			filCreatePackageMenuItem.setEnabled(true);
			filFontMenuItem.setEnabled(true);
			dicLinterMenuItem.setEnabled(true);
			dicSortDictionaryMenuItem.setEnabled(true);
			dicMenu.setEnabled(true);
			GUIUtils.setTabbedPaneEnable(mainTabbedPane, dicLayeredPane, true);
			final AffixData affixData = parserManager.getAffixData();
			final Set<String> compoundRules = affixData.getCompoundRules();
			GUIUtils.setTabbedPaneEnable(mainTabbedPane, cmpLayeredPane, !compoundRules.isEmpty());


			//affix file:
			if(!compoundRules.isEmpty()){
				cmpInputComboBox.removeAllItems();
				compoundRules.forEach(cmpInputComboBox::addItem);
				final String compoundFlag = affixData.getCompoundFlag();
				if(compoundFlag != null)
					cmpInputComboBox.addItem(compoundFlag);
				cmpInputComboBox.setEnabled(true);
				cmpInputComboBox.setSelectedItem(null);
				dicInputTextField.requestFocusInWindow();
			}
			openAffButton.setEnabled(packager.getAffixFile() != null);
			openDicButton.setEnabled(packager.getDictionaryFile() != null);


			//hyphenation file:
			if(parserManager.getHyphenator() != null){
				hypLinterMenuItem.setEnabled(true);

				hypMenu.setEnabled(true);
				hypStatisticsMenuItem.setEnabled(true);
				GUIUtils.setTabbedPaneEnable(mainTabbedPane, hypLayeredPane, true);
			}
			openHypButton.setEnabled(packager.getHyphenationFile() != null);


			//aid file:
			final List<String> lines = parserManager.getAidParser().getLines();
			final boolean aidLinesPresent = !lines.isEmpty();
			clearAidParser();
			if(aidLinesPresent){
				lines.forEach(dicRuleFlagsAidComboBox::addItem);
				lines.forEach(cmpRuleFlagsAidComboBox::addItem);
			}
			//enable combo-box only if an AID file exists
			dicRuleFlagsAidComboBox.setEnabled(aidLinesPresent);
			cmpRuleFlagsAidComboBox.setEnabled(aidLinesPresent);
			openAidButton.setEnabled(aidLinesPresent);


			//thesaurus file:
			if(parserManager.getTheParser().getSynonymsCount() > 0){
				addSorterToTable(theTable, comparator, null);

				theMenu.setEnabled(true);
				theLinterMenuItem.setEnabled(true);
				final ThesaurusTableModel dm = (ThesaurusTableModel)theTable.getModel();
				dm.setSynonyms(parserManager.getTheParser().getSynonymsDictionary());
				updateSynonymsCounter();
				GUIUtils.setTabbedPaneEnable(mainTabbedPane, theLayeredPane, true);
			}


			//auto–correct file:
			if(parserManager.getAcoParser().getCorrectionsCounter() > 0){
				addSorterToTable(acoTable, comparator, null);

				final AutoCorrectTableModel dm = (AutoCorrectTableModel)acoTable.getModel();
				dm.setCorrections(parserManager.getAcoParser().getCorrectionsDictionary());
				updateCorrectionsCounter();
				GUIUtils.setTabbedPaneEnable(mainTabbedPane, acoLayeredPane, true);
			}
			openAcoButton.setEnabled(packager.getAutoCorrectFile() != null);


			//sentence exceptions file:
			if(parserManager.getSexParser().getExceptionsCounter() > 0){
				updateSentenceExceptionsCounter();
				final List<String> sentenceExceptions = parserManager.getSexParser().getExceptionsDictionary();
				sexTagPanel.initializeTags(sentenceExceptions);
				GUIUtils.setTabbedPaneEnable(mainTabbedPane, sexLayeredPane, true);
			}
			openSexButton.setEnabled(packager.getSentenceExceptionsFile() != null);


			//word exceptions file:
			if(parserManager.getWexParser().getExceptionsCounter() > 0){
				final List<String> wordExceptions = parserManager.getWexParser().getExceptionsDictionary();
				wexTagPanel.initializeTags(wordExceptions);
				updateWordExceptionsCounter();
				GUIUtils.setTabbedPaneEnable(mainTabbedPane, wexLayeredPane, true);
			}
			openWexButton.setEnabled(packager.getWordExceptionsFile() != null);


			if(!mainTabbedPane.getComponentAt(mainTabbedPane.getSelectedIndex()).isEnabled())
				mainTabbedPane.setSelectedIndex(0);


			final String fontFamilyName = preferences.get(FONT_FAMILY_NAME_PREFIX + language, null);
			final String fontSize = preferences.get(FONT_SIZE_PREFIX + language, null);
			final Font lastUsedFont = (fontFamilyName != null && fontSize != null?
				new Font(fontFamilyName, Font.PLAIN, Integer.parseInt(fontSize)):
				FontChooserDialog.getDefaultFont());
			GUIUtils.setCurrentFont(lastUsedFont, this);
		}
		catch(final IndexOutOfBoundsException e){
			LOGGER.info(ParserManager.MARKER_APPLICATION, e.getMessage());
		}
		catch(final Exception e){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "A bad error occurred: {}", e.getMessage());

			LOGGER.error("A bad error occurred", e);
		}
	}

	private void loadFileCancelled(final Exception exc){
		//menu
		filOpenProjectMenuItem.setEnabled(true);
		filCreatePackageMenuItem.setEnabled(false);
		filFontMenuItem.setEnabled(false);
		dicMenu.setEnabled(false);
		theMenu.setEnabled(false);
		if((exc instanceof ProjectNotFoundException)){
			//remove the file from the recent projects menu
			recentProjectsMenu.removeEntry(((ProjectNotFoundException) exc).getProjectPath().toString());

			recentProjectsMenu.setEnabled(recentProjectsMenu.hasEntries());
			filEmptyRecentProjectsMenuItem.setEnabled(recentProjectsMenu.hasEntries());
		}
		dicLinterMenuItem.setEnabled(false);
		theLinterMenuItem.setEnabled(false);
		dicSortDictionaryMenuItem.setEnabled(false);
		hypLinterMenuItem.setEnabled(false);


		//affix file:
		cmpInputComboBox.removeAllItems();
		cmpInputComboBox.setEnabled(false);
		openAffButton.setEnabled(false);
		openDicButton.setEnabled(false);


		//hyphenation file:
		hypMenu.setEnabled(false);
		hypStatisticsMenuItem.setEnabled(false);
		GUIUtils.setTabbedPaneEnable(mainTabbedPane, hypLayeredPane, false);
		openHypButton.setEnabled(false);


		//aid file:
		clearAidParser();
		//enable combo-box only if an AID file exists
		dicRuleFlagsAidComboBox.setEnabled(false);
		cmpRuleFlagsAidComboBox.setEnabled(false);
		openAidButton.setEnabled(false);


		//thesaurus file:
		GUIUtils.setTabbedPaneEnable(mainTabbedPane, theLayeredPane, false);
		formerFilterThesaurusText = null;
		//noinspection unchecked
		((TableRowSorter<ThesaurusTableModel>)theTable.getRowSorter()).setRowFilter(null);


		//auto–correct file:
		GUIUtils.setTabbedPaneEnable(mainTabbedPane, acoLayeredPane, false);
		openAcoButton.setEnabled(false);
		formerFilterIncorrectText = null;
		formerFilterCorrectText = null;
		//noinspection unchecked
		((TableRowSorter<AutoCorrectTableModel>)acoTable.getRowSorter()).setRowFilter(null);


		//sentence exceptions file:
		GUIUtils.setTabbedPaneEnable(mainTabbedPane, sexLayeredPane, false);
		openSexButton.setEnabled(false);
		formerFilterSentenceException = null;
		sexTagPanel.applyFilter(null);


		//word exceptions file:
		GUIUtils.setTabbedPaneEnable(mainTabbedPane, wexLayeredPane, false);
		openWexButton.setEnabled(false);
		formerFilterWordException = null;
		wexTagPanel.applyFilter(null);
	}

	private void updateSynonymsCounter(){
		theSynonymsRecordedOutputLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(parserManager.getTheParser().getSynonymsCount()));
	}

	private void updateCorrectionsCounter(){
		acoCorrectionsRecordedOutputLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(parserManager.getAcoParser().getCorrectionsCounter()));
	}

	private void updateSentenceExceptionsCounter(){
		sexCorrectionsRecordedOutputLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(parserManager.getSexParser().getExceptionsCounter()));
	}

	private void updateWordExceptionsCounter(){
		wexCorrectionsRecordedOutputLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(parserManager.getWexParser().getExceptionsCounter()));
	}


	private void hyphenate(final DictionaryLayeredPane frame){
		final String language = frame.parserManager.getAffixData().getLanguage();
		final Orthography orthography = BaseBuilder.getOrthography(language);
		String text = orthography.correctOrthography(frame.hypWordTextField.getText());
		if(formerHyphenationText != null && formerHyphenationText.equals(text))
			return;
		formerHyphenationText = text;

		String count = null;
		List<String> rules = Collections.emptyList();
		if(StringUtils.isNotBlank(text)){
			final Hyphenation hyphenation = frame.parserManager.getHyphenator().hyphenate(text);

			final Supplier<StringJoiner> sj = () -> new StringJoiner(HyphenationParser.SOFT_HYPHEN, "<html>",
				"</html>");
			final Function<String, String> errorFormatter = syllabe -> "<b style=\"color:red\">" + syllabe + "</b>";
			text = orthography.formatHyphenation(hyphenation.getSyllabes(), sj.get(), errorFormatter)
				.toString();
			count = Long.toString(hyphenation.countSyllabes());
			rules = hyphenation.getRules();

			frame.hypAddRuleTextField.setEnabled(true);
		}
		else{
			text = null;

			frame.hypAddRuleTextField.setEnabled(false);
		}

		frame.hypSyllabationOutputLabel.setText(text);
		frame.hypSyllabesCountOutputLabel.setText(count);
		frame.hypRulesOutputLabel.setText(StringUtils.join(rules, StringUtils.SPACE));

		frame.hypAddRuleTextField.setText(null);
		frame.hypAddRuleSyllabationOutputLabel.setText(null);
		frame.hypAddRuleSyllabesCountOutputLabel.setText(null);
	}

	private void hyphenateAddRule(final DictionaryLayeredPane frame){
		final String language = frame.parserManager.getAffixData().getLanguage();
		final Orthography orthography = BaseBuilder.getOrthography(language);
		String addedRuleText = orthography.correctOrthography(frame.hypWordTextField.getText());
		final String addedRule = orthography.correctOrthography(frame.hypAddRuleTextField.getText().toLowerCase(Locale.ROOT));
		final HyphenationParser.Level level = HyphenationParser.Level.values()[frame.hypAddRuleLevelComboBox.getSelectedIndex()];
		String addedRuleCount = null;
		if(StringUtils.isNotBlank(addedRule)){
			final boolean alreadyHasRule = frame.parserManager.hasHyphenationRule(addedRule, level);
			boolean ruleMatchesText = false;
			boolean hyphenationChanged = false;
			boolean correctHyphenation = false;
			if(!alreadyHasRule){
				ruleMatchesText = addedRuleText.contains(PatternHelper.clear(addedRule,
					PATTERN_POINTS_AND_NUMBERS_AND_EQUALS_AND_MINUS));

				if(ruleMatchesText){
					final Hyphenation hyphenation = frame.parserManager.getHyphenator().hyphenate(addedRuleText);
					final Hyphenation addedRuleHyphenation = frame.parserManager.getHyphenator().hyphenate(addedRuleText, addedRule,
						level);

					final Supplier<StringJoiner> sj = () -> new StringJoiner(HyphenationParser.SOFT_HYPHEN, "<html>",
						"</html>");
					final Function<String, String> errorFormatter = syllabe -> "<b style=\"color:red\">" + syllabe + "</b>";
					final String text = orthography.formatHyphenation(hyphenation.getSyllabes(), sj.get(), errorFormatter)
						.toString();
					addedRuleText = orthography.formatHyphenation(addedRuleHyphenation.getSyllabes(), sj.get(), errorFormatter)
						.toString();
					addedRuleCount = Long.toString(addedRuleHyphenation.countSyllabes());

					hyphenationChanged = !text.equals(addedRuleText);
					correctHyphenation = !orthography.hasSyllabationErrors(addedRuleHyphenation.getSyllabes());
				}
			}

			if(alreadyHasRule || !ruleMatchesText)
				addedRuleText = null;
			frame.hypAddRuleLevelComboBox.setEnabled(ruleMatchesText);
			frame.hypAddRuleButton.setEnabled(ruleMatchesText && hyphenationChanged && correctHyphenation);
		}
		else{
			addedRuleText = null;

			frame.hypAddRuleTextField.setText(null);
			frame.hypAddRuleLevelComboBox.setEnabled(false);
			frame.hypAddRuleButton.setEnabled(false);
			frame.hypAddRuleSyllabationOutputLabel.setText(null);
			frame.hypAddRuleSyllabesCountOutputLabel.setText(null);
		}

		frame.hypAddRuleSyllabationOutputLabel.setText(addedRuleText);
		frame.hypAddRuleSyllabesCountOutputLabel.setText(addedRuleCount);
	}


	@Override
	public void clearAffixParser(){
		clearDictionaryParser();
	}

	@Override
	public void clearHyphenationParser(){
		clearHyphenationFields();

		hypMenu.setEnabled(false);
		hypStatisticsMenuItem.setEnabled(false);
		GUIUtils.setTabbedPaneEnable(mainTabbedPane, hypLayeredPane, false);
	}

	private void clearHyphenationFields(){
		formerHyphenationText = null;

		hypWordTextField.setText(null);
		hypSyllabationOutputLabel.setText(null);
		hypSyllabesCountOutputLabel.setText(null);
		hypRulesOutputLabel.setText(null);
		hypAddRuleTextField.setText(null);
		hypAddRuleLevelComboBox.setEnabled(false);
		hypAddRuleButton.setEnabled(false);
		hypAddRuleSyllabationOutputLabel.setText(null);
		hypAddRuleSyllabesCountOutputLabel.setText(null);
	}

	@Override
	public void clearDictionaryParser(){
		clearDictionaryFields();
		clearDictionaryCompoundFields();

		GUIUtils.setTabbedPaneEnable(mainTabbedPane, dicLayeredPane, false);
		GUIUtils.setTabbedPaneEnable(mainTabbedPane, cmpLayeredPane, false);

		//disable menu
		dicMenu.setEnabled(false);
		filCreatePackageMenuItem.setEnabled(false);
		filFontMenuItem.setEnabled(false);
		dicInputTextField.requestFocusInWindow();
	}

	private void clearDictionaryFields(){
		clearOutputTable(dicTable);
		dicTotalProductionsOutputLabel.setText(StringUtils.EMPTY);
		clearOutputTable(cmpTable);

		formerInputText = null;

		dicInputTextField.setText(null);
		theSynonymsTextField.setText(null);
		popupMergeMenuItem.setEnabled(false);
	}

	private void clearDictionaryCompoundFields(){
		formerCompoundInputText = null;

		cmpInputComboBox.setEnabled(true);
		cmpInputTextArea.setText(null);
		cmpInputTextArea.setEnabled(true);
		cmpLoadInputButton.setEnabled(true);
	}

	public void clearOutputTable(JTable table){
		final HunLinterTableModelInterface<?> dm = (HunLinterTableModelInterface<?>)table.getModel();
		dm.clear();
	}

	@Override
	public void clearAidParser(){
		dicRuleFlagsAidComboBox.removeAllItems();
		cmpRuleFlagsAidComboBox.removeAllItems();
	}

	@Override
	public void clearThesaurusParser(){
		final ThesaurusTableModel dm = (ThesaurusTableModel)theTable.getModel();
		dm.setSynonyms(null);

		theMenu.setEnabled(false);
		GUIUtils.setTabbedPaneEnable(mainTabbedPane, theLayeredPane, false);
	}

	@Override
	public void clearAutoCorrectParser(){
		final AutoCorrectTableModel dm = (AutoCorrectTableModel)acoTable.getModel();
		dm.setCorrections(null);

		GUIUtils.setTabbedPaneEnable(mainTabbedPane, acoLayeredPane, false);
	}

	@Override
	public void clearSentenceExceptionsParser(){
		sexTagPanel.initializeTags(null);

		GUIUtils.setTabbedPaneEnable(mainTabbedPane, sexLayeredPane, false);
	}

	@Override
	public void clearWordExceptionsParser(){
		wexTagPanel.initializeTags(null);

		GUIUtils.setTabbedPaneEnable(mainTabbedPane, wexLayeredPane, false);
	}

	@Override
	public void clearAutoTextParser(){
		//TODO
//		final AutoTextTableModel dm = (AutoTextTableModel)atxTable.getModel();
//		dm.setCorrections(null);

//		atxMenu.setEnabled(false);
//		setTabbedPaneEnable(mainTabbedPane, atxLayeredPane, false);
	}


	private void clearResultTextArea(){
		parsingResultTextArea.setText(null);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt){
		switch(evt.getPropertyName()){
			case "progress":
				final int progress = (int)evt.getNewValue();
				mainProgressBar.setValue(progress);
				break;

			case "state":
				final SwingWorker.StateValue stateValue = (SwingWorker.StateValue)evt.getNewValue();
				if(stateValue == SwingWorker.StateValue.DONE){
					final String workerName = ((WorkerAbstract<?, ?>)evt.getSource()).getWorkerData().getWorkerName();
					workerManager.callOnEnd(workerName);
				}
				break;

			default:
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
   private javax.swing.JLabel dicTotalProductionsOutputLabel;
   private javax.swing.JButton openAffButton;
   private javax.swing.JButton openAidButton;
   private javax.swing.JButton openDicButton;
   // End of variables declaration//GEN-END:variables

}
