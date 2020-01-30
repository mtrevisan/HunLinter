package unit731.hunlinter;

import java.awt.*;

import org.apache.commons.lang3.tuple.Pair;
import org.xml.sax.SAXException;
import unit731.hunlinter.gui.AscendingDescendingUnsortedTableRowSorter;
import unit731.hunlinter.gui.AutoCorrectTableModel;
import unit731.hunlinter.gui.JCopyableTable;
import unit731.hunlinter.gui.JTagPanel;
import unit731.hunlinter.gui.JWordLabel;
import unit731.hunlinter.gui.ProjectFolderFilter;
import unit731.hunlinter.interfaces.HunLintable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.DefaultCaret;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.gui.CompoundTableModel;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.gui.HunLinterTableModel;
import unit731.hunlinter.gui.ProductionTableModel;
import unit731.hunlinter.gui.RecentFilesMenu;
import unit731.hunlinter.gui.ThesaurusTableModel;
import unit731.hunlinter.gui.TableRenderer;
import unit731.hunlinter.languages.DictionaryCorrectnessChecker;
import unit731.hunlinter.languages.Orthography;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.AffixParser;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.autocorrect.AutoCorrectParser;
import unit731.hunlinter.parsers.autocorrect.CorrectionEntry;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.exceptions.ExceptionsParser;
import unit731.hunlinter.parsers.hyphenation.HyphenationOptionsParser;
import unit731.hunlinter.parsers.thesaurus.SynonymsEntry;
import unit731.hunlinter.parsers.workers.PoSFSAWorker;
import unit731.hunlinter.parsers.workers.core.WorkerDictionaryBase;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.workers.exceptions.ProjectNotFoundException;
import unit731.hunlinter.parsers.workers.CompoundRulesWorker;
import unit731.hunlinter.parsers.workers.CorrectnessWorker;
import unit731.hunlinter.parsers.workers.DuplicatesWorker;
import unit731.hunlinter.parsers.workers.HyphenationCorrectnessWorker;
import unit731.hunlinter.parsers.workers.MinimalPairsWorker;
import unit731.hunlinter.parsers.workers.ProjectLoaderWorker;
import unit731.hunlinter.parsers.workers.SorterWorker;
import unit731.hunlinter.parsers.workers.StatisticsWorker;
import unit731.hunlinter.parsers.workers.WordCountWorker;
import unit731.hunlinter.parsers.workers.WordlistWorker;
import unit731.hunlinter.parsers.workers.core.WorkerBase;
import unit731.hunlinter.parsers.thesaurus.DuplicationResult;
import unit731.hunlinter.parsers.hyphenation.Hyphenation;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;
import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;
import unit731.hunlinter.parsers.thesaurus.ThesaurusEntry;
import unit731.hunlinter.services.text.StringHelper;
import unit731.hunlinter.services.log.ApplicationLogAppender;
import unit731.hunlinter.services.system.Debouncer;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.PatternHelper;
import unit731.hunlinter.services.RecentItems;
import unit731.hunlinter.services.log.ExceptionHelper;


/**
 * @see <a href="http://manpages.ubuntu.com/manpages/trusty/man4/hunspell.4.html">Hunspell 4</a>
 * @see <a href="https://github.com/lopusz/hunspell-stemmer">Hunspell stemmer on github</a>
 * @see <a href="https://github.com/nuspell/nuspell">Nuspell on github</a>
 * @see <a href="https://github.com/hunspell/hyphen">Hyphen on github</a>a
 *
 * @see <a href="https://www.shareicon.net/">Share icon</a>
 * @see <a href="https://www.iloveimg.com/resize-image/resize-png">PNG resizer</a>
 * @see <a href="https://compresspng.com/">PNG compresser</a>
 * @see <a href="https://www.icoconverter.com/index.php">ICO converter</a>
 * @see <a href="https://icon-icons.com/">Free icons</a>
 */
public class HunLinterFrame extends JFrame implements ActionListener, PropertyChangeListener, HunLintable{

	private static final long serialVersionUID = 6772959670167531135L;

	private static final Logger LOGGER = LoggerFactory.getLogger(HunLinterFrame.class);

	private static final String URL_ONLINE_HELP = "https://github.com/mtrevisan/HunLinter/blob/master/README.md";
	private static final String URL_REPORT_ISSUE = "https://github.com/mtrevisan/HunLinter/issues";

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
	private final JFileChooser saveResultFileChooser;
	private RulesReducerDialog rulesReducerDialog;

	private final Preferences preferences = Preferences.userNodeForPackage(getClass());
	private Backbone backbone;
	private Packager packager;
	private int lastDictionarySortVisibleIndex;

	private RecentFilesMenu recentProjectsMenu;
	private final Debouncer<HunLinterFrame> productionDebouncer = new Debouncer<>(this::calculateProductions, DEBOUNCER_INTERVAL);
	private final Debouncer<HunLinterFrame> compoundProductionDebouncer = new Debouncer<>(this::calculateCompoundProductions, DEBOUNCER_INTERVAL);
	private final Debouncer<HunLinterFrame> theFilterDebouncer = new Debouncer<>(this::filterThesaurus, DEBOUNCER_INTERVAL);
	private final Debouncer<HunLinterFrame> hypDebouncer = new Debouncer<>(this::hyphenate, DEBOUNCER_INTERVAL);
	private final Debouncer<HunLinterFrame> hypAddRuleDebouncer = new Debouncer<>(this::hyphenateAddRule, DEBOUNCER_INTERVAL);
	private final Debouncer<HunLinterFrame> acoFilterDebouncer = new Debouncer<>(this::filterAutoCorrect, DEBOUNCER_INTERVAL);
	private final Debouncer<HunLinterFrame> sexFilterDebouncer = new Debouncer<>(this::filterSentenceExceptions, DEBOUNCER_INTERVAL);
	private final Debouncer<HunLinterFrame> wexFilterDebouncer = new Debouncer<>(this::filterWordExceptions, DEBOUNCER_INTERVAL);

	private ProjectLoaderWorker prjLoaderWorker;
	private CorrectnessWorker dicCorrectnessWorker;
	private DuplicatesWorker dicDuplicatesWorker;
	private SorterWorker dicSorterWorker;
	private WordCountWorker dicWordCountWorker;
	private StatisticsWorker dicStatisticsWorker;
	private WordlistWorker dicWordlistWorker;
	private PoSFSAWorker dicPoSFSAWorker;
	private MinimalPairsWorker dicMinimalPairsWorker;
	private CompoundRulesWorker compoundRulesExtractorWorker;
	private HyphenationCorrectnessWorker hypCorrectnessWorker;
	private final Map<String, Runnable> enableComponentFromWorker = new HashMap<>();
	private JPopupMenu copyPopupMenu;
	private JPopupMenu mergeCopyRemovePopupMenu;
	private JPopupMenu copyRemovePopupMenu;


	public HunLinterFrame(){
		initComponents();

		recentProjectsMenu.setEnabled(recentProjectsMenu.hasEntries());
		filEmptyRecentProjectsMenuItem.setEnabled(recentProjectsMenu.hasEntries());

		try{
			final int iconSize = hypRulesOutputLabel.getHeight();
			copyPopupMenu = new JPopupMenu();
			copyPopupMenu.add(GUIUtils.createPopupCopyMenu(iconSize, copyPopupMenu, GUIUtils::copyCallback));
			mergeCopyRemovePopupMenu = new JPopupMenu();
			mergeCopyRemovePopupMenu.add(GUIUtils.createPopupMergeMenu(iconSize, mergeCopyRemovePopupMenu, this::mergeThesaurusRow));
			mergeCopyRemovePopupMenu.add(GUIUtils.createPopupCopyMenu(iconSize, mergeCopyRemovePopupMenu, GUIUtils::copyCallback));
			mergeCopyRemovePopupMenu.add(GUIUtils.createPopupRemoveMenu(iconSize, mergeCopyRemovePopupMenu, this::removeSelectedRows));
			copyRemovePopupMenu = new JPopupMenu();
			copyRemovePopupMenu.add(GUIUtils.createPopupCopyMenu(iconSize, copyRemovePopupMenu, GUIUtils::copyCallback));
			copyRemovePopupMenu.add(GUIUtils.createPopupRemoveMenu(iconSize, copyRemovePopupMenu, this::removeSelectedRows));
			GUIUtils.addPopupMenu(copyPopupMenu, dicTable, hypSyllabationOutputLabel, hypRulesOutputLabel,
				hypAddRuleSyllabationOutputLabel);
			GUIUtils.addPopupMenu(mergeCopyRemovePopupMenu, theTable);
			GUIUtils.addPopupMenu(copyRemovePopupMenu, acoTable);
		}
		catch(final IOException ignored){}

		ApplicationLogAppender.addTextArea(parsingResultTextArea, Backbone.MARKER_APPLICATION);


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

		saveResultFileChooser = new JFileChooser();
		saveResultFileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));

		enableComponentFromWorker.put(CorrectnessWorker.WORKER_NAME, () -> dicCheckCorrectnessMenuItem.setEnabled(true));
		enableComponentFromWorker.put(DuplicatesWorker.WORKER_NAME, () -> dicExtractDuplicatesMenuItem.setEnabled(true));
		enableComponentFromWorker.put(SorterWorker.WORKER_NAME, () -> dicSortDictionaryMenuItem.setEnabled(true));
		enableComponentFromWorker.put(WordCountWorker.WORKER_NAME, () -> dicWordCountMenuItem.setEnabled(true));
		enableComponentFromWorker.put(StatisticsWorker.WORKER_NAME, () -> {
			if(dicStatisticsWorker.isPerformHyphenationStatistics())
				hypStatisticsMenuItem.setEnabled(true);
			else
				dicStatisticsMenuItem.setEnabled(true);
		});
		enableComponentFromWorker.put(WordlistWorker.WORKER_NAME, () -> {
			dicExtractWordlistMenuItem.setEnabled(true);
			dicExtractWordlistPlainTextMenuItem.setEnabled(true);
		});
		enableComponentFromWorker.put(PoSFSAWorker.WORKER_NAME, () -> dicExtractPoSFAMenuItem.setEnabled(true));
		enableComponentFromWorker.put(CompoundRulesWorker.WORKER_NAME, () -> {
			cmpInputComboBox.setEnabled(true);
			cmpLimitComboBox.setEnabled(true);
			cmpInputTextArea.setEnabled(true);
			if(compoundRulesExtractorWorker.isCancelled())
				cmpLoadInputButton.setEnabled(true);
		});
		enableComponentFromWorker.put(HyphenationCorrectnessWorker.WORKER_NAME,
			() -> hypCheckCorrectnessMenuItem.setEnabled(true));


		//check for updates
		if(preferences.getBoolean(UPDATE_STARTUP_CHECK, true)){
			SwingUtilities.invokeLater(() -> {
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

      parsingResultScrollPane = new javax.swing.JScrollPane();
      parsingResultTextArea = new javax.swing.JTextArea();
      mainProgressBar = new javax.swing.JProgressBar();
      mainTabbedPane = new javax.swing.JTabbedPane();
      dicLayeredPane = new javax.swing.JLayeredPane();
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
            return Stream.of(production, morphologicalFields, rule1, rule2, rule3)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(TAB));
         }
      };
      dicTotalProductionsLabel = new javax.swing.JLabel();
      dicTotalProductionsOutputLabel = new javax.swing.JLabel();
      openAidButton = new javax.swing.JButton();
      openAffButton = new javax.swing.JButton();
      openDicButton = new javax.swing.JButton();
      cmpLayeredPane = new javax.swing.JLayeredPane();
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
      theLayeredPane = new javax.swing.JLayeredPane();
      theSynonymsLabel = new javax.swing.JLabel();
      theSynonymsTextField = new javax.swing.JTextField();
      theAddButton = new javax.swing.JButton();
      theScrollPane = new javax.swing.JScrollPane();
      theTable = new JCopyableTable(){
         @Override
         public String getValueAtRow(final int row){
            final TableModel model = getModel();
            final String definition = (String)model.getValueAt(row, 0);
            final String synonyms = (String)model.getValueAt(row, 1);
            final String[] synonymsByDefinition = StringUtils.splitByWholeSeparator(synonyms, ThesaurusTableModel.TAG_NEW_LINE);
            return Arrays.stream(synonymsByDefinition)
            .map(GUIUtils::removeHTMLCode)
            .map(syns -> definition + ": " + syns)
            .collect(Collectors.joining("\r\n"));
         }
      };
      theSynonymsRecordedLabel = new javax.swing.JLabel();
      theSynonymsRecordedOutputLabel = new javax.swing.JLabel();
      hypLayeredPane = new javax.swing.JLayeredPane();
      hypWordLabel = new javax.swing.JLabel();
      hypWordTextField = new javax.swing.JTextField();
      hypSyllabationLabel = new javax.swing.JLabel();
      hypSyllabationOutputLabel = new JWordLabel();
      hypSyllabesCountLabel = new javax.swing.JLabel();
      hypSyllabesCountOutputLabel = new javax.swing.JLabel();
      hypRulesLabel = new javax.swing.JLabel();
      hypRulesOutputLabel = new JWordLabel();
      hypAddRuleLabel = new javax.swing.JLabel();
      hypAddRuleTextField = new javax.swing.JTextField();
      hypAddRuleLevelComboBox = new javax.swing.JComboBox<>();
      hypAddRuleButton = new javax.swing.JButton();
      hypAddRuleSyllabationLabel = new javax.swing.JLabel();
      hypAddRuleSyllabationOutputLabel = new JWordLabel();
      hypAddRuleSyllabesCountLabel = new javax.swing.JLabel();
      hypAddRuleSyllabesCountOutputLabel = new javax.swing.JLabel();
      optionsButton = new javax.swing.JButton();
      openHypButton = new javax.swing.JButton();
      acoLayeredPane = new javax.swing.JLayeredPane();
      acoIncorrectLabel = new javax.swing.JLabel();
      acoIncorrectTextField = new javax.swing.JTextField();
      acoToLabel = new javax.swing.JLabel();
      acoCorrectLabel = new javax.swing.JLabel();
      acoCorrectTextField = new javax.swing.JTextField();
      acoAddButton = new javax.swing.JButton();
      acoScrollPane = new javax.swing.JScrollPane();
      acoTable = new JCopyableTable(){
         @Override
         public String getValueAtRow(final int row){
            final TableModel model = getModel();
            final String incorrect = (String)model.getValueAt(row, 0);
            final String correct = (String)model.getValueAt(row, 1);
            return incorrect + " > " + correct;
         }
      };
      acoCorrectionsRecordedLabel = new javax.swing.JLabel();
      acoCorrectionsRecordedOutputLabel = new javax.swing.JLabel();
      openAcoButton = new javax.swing.JButton();
      sexLayeredPane = new javax.swing.JLayeredPane();
      sexInputLabel = new javax.swing.JLabel();
      sexTextField = new javax.swing.JTextField();
      sexAddButton = new javax.swing.JButton();
      sexScrollPane = new javax.swing.JScrollPane();
      sexScrollPane.getVerticalScrollBar().setUnitIncrement(16);
      sexTagPanel = new JTagPanel((changeType, tags) -> {
         backbone.getSexParser().modify(changeType, tags);
         try{
            backbone.getSexParser().save(backbone.getSentenceExceptionsFile());
         }
         catch(final TransformerException e){
            LOGGER.info(Backbone.MARKER_APPLICATION, e.getMessage());
         }
      });
      sexCorrectionsRecordedLabel = new javax.swing.JLabel();
      sexCorrectionsRecordedOutputLabel = new javax.swing.JLabel();
      openSexButton = new javax.swing.JButton();
      wexLayeredPane = new javax.swing.JLayeredPane();
      wexInputLabel = new javax.swing.JLabel();
      wexTextField = new javax.swing.JTextField();
      wexAddButton = new javax.swing.JButton();
      wexScrollPane = new javax.swing.JScrollPane();
      wexScrollPane.getVerticalScrollBar().setUnitIncrement(16);
      wexTagPanel = new JTagPanel((changeType, tags) -> {
         backbone.getWexParser().modify(changeType, tags);
         try{
            backbone.getWexParser().save(backbone.getWordExceptionsFile());
         }
         catch(final TransformerException e){
            LOGGER.info(Backbone.MARKER_APPLICATION, e.getMessage());
         }
      });
      wexCorrectionsRecordedLabel = new javax.swing.JLabel();
      wexCorrectionsRecordedOutputLabel = new javax.swing.JLabel();
      openWexButton = new javax.swing.JButton();
      mainMenuBar = new javax.swing.JMenuBar();
      filMenu = new javax.swing.JMenu();
      filOpenProjectMenuItem = new javax.swing.JMenuItem();
      filCreatePackageMenuItem = new javax.swing.JMenuItem();
      filFontSeparator = new javax.swing.JPopupMenu.Separator();
      filFontMenuItem = new javax.swing.JMenuItem();
      filRecentProjectsSeparator = new javax.swing.JPopupMenu.Separator();
      filEmptyRecentProjectsMenuItem = new javax.swing.JMenuItem();
      filSeparator = new javax.swing.JPopupMenu.Separator();
      filExitMenuItem = new javax.swing.JMenuItem();
      dicMenu = new javax.swing.JMenu();
      dicCheckCorrectnessMenuItem = new javax.swing.JMenuItem();
      dicSortDictionaryMenuItem = new javax.swing.JMenuItem();
      dicRulesReducerMenuItem = new javax.swing.JMenuItem();
      dicDuplicatesSeparator = new javax.swing.JPopupMenu.Separator();
      dicWordCountMenuItem = new javax.swing.JMenuItem();
      dicStatisticsMenuItem = new javax.swing.JMenuItem();
      dicStatisticsSeparator = new javax.swing.JPopupMenu.Separator();
      dicExtractDuplicatesMenuItem = new javax.swing.JMenuItem();
      dicExtractWordlistMenuItem = new javax.swing.JMenuItem();
      dicExtractWordlistPlainTextMenuItem = new javax.swing.JMenuItem();
      dicExtractPoSFAMenuItem = new javax.swing.JMenuItem();
      dicExtractMinimalPairsMenuItem = new javax.swing.JMenuItem();
      hypMenu = new javax.swing.JMenu();
      hypCheckCorrectnessMenuItem = new javax.swing.JMenuItem();
      hypDuplicatesSeparator = new javax.swing.JPopupMenu.Separator();
      hypStatisticsMenuItem = new javax.swing.JMenuItem();
      hlpMenu = new javax.swing.JMenu();
      hlpOnlineHelpMenuItem = new javax.swing.JMenuItem();
      hlpIssueReporterMenuItem = new javax.swing.JMenuItem();
      hlpOnlineSeparator = new javax.swing.JPopupMenu.Separator();
      hlpUpdateMenuItem = new javax.swing.JMenuItem();
      hlpCheckUpdateOnStartupCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
      hlpUpdateSeparator = new javax.swing.JPopupMenu.Separator();
      hlpAboutMenuItem = new javax.swing.JMenuItem();

      setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
      setTitle("HunLinter");
      setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png")));
      setMinimumSize(new java.awt.Dimension(964, 534));

      parsingResultTextArea.setEditable(false);
      parsingResultTextArea.setColumns(20);
      parsingResultTextArea.setRows(1);
      parsingResultTextArea.setTabSize(3);
      DefaultCaret caret = (DefaultCaret)parsingResultTextArea.getCaret();
      caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
      parsingResultScrollPane.setViewportView(parsingResultTextArea);

      dicInputLabel.setLabelFor(dicInputTextField);
      dicInputLabel.setText("Dictionary entry:");

      dicInputTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            dicInputTextFieldKeyReleased(evt);
         }
      });

      dicRuleFlagsAidLabel.setLabelFor(dicRuleFlagsAidComboBox);
      dicRuleFlagsAidLabel.setText("Rule flags aid:");

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

      openAidButton.setText("Open Aid");
      openAidButton.setEnabled(false);
      openAidButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            openAidButtonActionPerformed(evt);
         }
      });

      openAffButton.setText("Open Affix");
      openAffButton.setEnabled(false);
      openAffButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            openAffButtonActionPerformed(evt);
         }
      });

      openDicButton.setText("Open Dictionary");
      openDicButton.setEnabled(false);
      openDicButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            openDicButtonActionPerformed(evt);
         }
      });

      dicLayeredPane.setLayer(dicInputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(dicInputTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(dicRuleFlagsAidLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(dicRuleFlagsAidComboBox, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(dicScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(dicTotalProductionsLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(dicTotalProductionsOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(openAidButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(openAffButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(openDicButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout dicLayeredPaneLayout = new javax.swing.GroupLayout(dicLayeredPane);
      dicLayeredPane.setLayout(dicLayeredPaneLayout);
      dicLayeredPaneLayout.setHorizontalGroup(
         dicLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(dicLayeredPaneLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(dicLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, dicLayeredPaneLayout.createSequentialGroup()
                  .addGroup(dicLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                     .addComponent(dicInputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addComponent(dicRuleFlagsAidLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(dicLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(dicRuleFlagsAidComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addComponent(dicInputTextField)))
               .addComponent(dicScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 910, Short.MAX_VALUE)
               .addGroup(dicLayeredPaneLayout.createSequentialGroup()
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
      dicLayeredPaneLayout.setVerticalGroup(
         dicLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(dicLayeredPaneLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(dicLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(dicInputLabel)
               .addComponent(dicInputTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(dicLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(dicRuleFlagsAidComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(dicRuleFlagsAidLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(dicScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 141, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(dicLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(dicTotalProductionsLabel)
               .addComponent(dicTotalProductionsOutputLabel)
               .addComponent(openAffButton)
               .addComponent(openDicButton)
               .addComponent(openAidButton))
            .addContainerGap())
      );

      mainTabbedPane.addTab("Dictionary", dicLayeredPane);

      cmpInputLabel.setLabelFor(cmpInputComboBox);
      cmpInputLabel.setText("Compound rule:");

      cmpInputComboBox.setEditable(true);
      cmpInputComboBox.getEditor().getEditorComponent().addKeyListener(new java.awt.event.KeyAdapter(){
         @Override
         public void keyReleased(java.awt.event.KeyEvent evt){
            cmpInputComboBoxKeyReleased();
         }
      });
      cmpInputComboBox.addItemListener(new ItemListener(){
         @Override
         public void itemStateChanged(ItemEvent evt){
            cmpInputComboBoxKeyReleased();
         }
      });

      cmpLimitLabel.setLabelFor(cmpLimitComboBox);
      cmpLimitLabel.setText("Limit:");

      cmpLimitComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "20", "50", "100", "500", "1000" }));
      cmpLimitComboBox.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            cmpLimitComboBoxActionPerformed(evt);
         }
      });

      cmpRuleFlagsAidLabel.setLabelFor(cmpRuleFlagsAidComboBox);
      cmpRuleFlagsAidLabel.setText("Rule flags aid:");

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

      cmpLayeredPane.setLayer(cmpInputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      cmpLayeredPane.setLayer(cmpInputComboBox, javax.swing.JLayeredPane.DEFAULT_LAYER);
      cmpLayeredPane.setLayer(cmpLimitLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      cmpLayeredPane.setLayer(cmpLimitComboBox, javax.swing.JLayeredPane.DEFAULT_LAYER);
      cmpLayeredPane.setLayer(cmpRuleFlagsAidLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      cmpLayeredPane.setLayer(cmpRuleFlagsAidComboBox, javax.swing.JLayeredPane.DEFAULT_LAYER);
      cmpLayeredPane.setLayer(cmpScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      cmpLayeredPane.setLayer(cmpInputScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      cmpLayeredPane.setLayer(cmpLoadInputButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout cmpLayeredPaneLayout = new javax.swing.GroupLayout(cmpLayeredPane);
      cmpLayeredPane.setLayout(cmpLayeredPaneLayout);
      cmpLayeredPaneLayout.setHorizontalGroup(
         cmpLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(cmpLayeredPaneLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(cmpLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(cmpLayeredPaneLayout.createSequentialGroup()
                  .addGroup(cmpLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(cmpInputLabel)
                     .addComponent(cmpRuleFlagsAidLabel))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(cmpLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(cmpRuleFlagsAidComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addGroup(cmpLayeredPaneLayout.createSequentialGroup()
                        .addComponent(cmpInputComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 728, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(cmpLimitLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmpLimitComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, cmpLayeredPaneLayout.createSequentialGroup()
                  .addGroup(cmpLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(cmpInputScrollPane)
                     .addGroup(cmpLayeredPaneLayout.createSequentialGroup()
                        .addComponent(cmpLoadInputButton)
                        .addGap(0, 0, Short.MAX_VALUE)))
                  .addGap(18, 18, 18)
                  .addComponent(cmpScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 443, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addContainerGap())
      );
      cmpLayeredPaneLayout.setVerticalGroup(
         cmpLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(cmpLayeredPaneLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(cmpLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(cmpInputLabel)
               .addComponent(cmpInputComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(cmpLimitComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(cmpLimitLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(cmpLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(cmpRuleFlagsAidComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(cmpRuleFlagsAidLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(cmpLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(cmpScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 146, Short.MAX_VALUE)
               .addComponent(cmpInputScrollPane))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(cmpLoadInputButton)
            .addContainerGap())
      );

      mainTabbedPane.addTab("Compound rules", cmpLayeredPane);

      theSynonymsLabel.setLabelFor(theSynonymsTextField);
      theSynonymsLabel.setText("New definition:");

      theSynonymsTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            theSynonymsTextFieldKeyReleased(evt);
         }
      });

      theAddButton.setMnemonic('A');
      theAddButton.setText("Add");
      theAddButton.setEnabled(false);
      theAddButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            theAddButtonActionPerformed(evt);
         }
      });

      theTable.setModel(new ThesaurusTableModel());
      theTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
      theTable.setRowSorter(new TableRowSorter<>((ThesaurusTableModel)theTable.getModel()));
      theTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
      theTable.setShowHorizontalLines(false);
      theTable.setShowVerticalLines(false);
      theTable.getColumnModel().getColumn(0).setMinWidth(200);
      theTable.getColumnModel().getColumn(0).setMaxWidth(500);
      //listen for row removal
      theTable.registerKeyboardAction(event -> removeSelectedRowsFromThesaurus(), cancelKeyStroke, JComponent.WHEN_FOCUSED);
      theTable.registerKeyboardAction(event -> GUIUtils.copyToClipboard((JCopyableTable)theTable), copyKeyStroke, JComponent.WHEN_FOCUSED);

      TableRenderer theCellRenderer = new TableRenderer();
      theTable.getColumnModel().getColumn(1).setCellRenderer(theCellRenderer);
      theScrollPane.setViewportView(theTable);

      theSynonymsRecordedLabel.setLabelFor(theSynonymsRecordedOutputLabel);
      theSynonymsRecordedLabel.setText("Synonyms recorded:");

      theSynonymsRecordedOutputLabel.setText("…");

      theLayeredPane.setLayer(theSynonymsLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      theLayeredPane.setLayer(theSynonymsTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      theLayeredPane.setLayer(theAddButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      theLayeredPane.setLayer(theScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      theLayeredPane.setLayer(theSynonymsRecordedLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      theLayeredPane.setLayer(theSynonymsRecordedOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout theLayeredPaneLayout = new javax.swing.GroupLayout(theLayeredPane);
      theLayeredPane.setLayout(theLayeredPaneLayout);
      theLayeredPaneLayout.setHorizontalGroup(
         theLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(theLayeredPaneLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(theLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(theScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 910, Short.MAX_VALUE)
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, theLayeredPaneLayout.createSequentialGroup()
                  .addComponent(theSynonymsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(theSynonymsTextField)
                  .addGap(18, 18, 18)
                  .addComponent(theAddButton))
               .addGroup(theLayeredPaneLayout.createSequentialGroup()
                  .addComponent(theSynonymsRecordedLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(theSynonymsRecordedOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 799, Short.MAX_VALUE)))
            .addContainerGap())
      );
      theLayeredPaneLayout.setVerticalGroup(
         theLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, theLayeredPaneLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(theLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(theSynonymsLabel)
               .addComponent(theSynonymsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(theAddButton))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(theScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(theLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(theSynonymsRecordedLabel)
               .addComponent(theSynonymsRecordedOutputLabel))
            .addContainerGap())
      );

      mainTabbedPane.addTab("Thesaurus", theLayeredPane);

      hypWordLabel.setLabelFor(hypWordTextField);
      hypWordLabel.setText("Word:");

      hypWordTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            hypWordTextFieldKeyReleased(evt);
         }
      });

      hypSyllabationLabel.setLabelFor(hypSyllabationOutputLabel);
      hypSyllabationLabel.setText("Syllabation:");

      hypSyllabationOutputLabel.setText("…");

      hypSyllabesCountLabel.setLabelFor(hypSyllabesCountOutputLabel);
      hypSyllabesCountLabel.setText("Syllabes:");

      hypSyllabesCountOutputLabel.setText("…");

      hypRulesLabel.setLabelFor(hypRulesOutputLabel);
      hypRulesLabel.setText("Rules:");

      hypRulesOutputLabel.setText("…");

      hypAddRuleLabel.setLabelFor(hypAddRuleTextField);
      hypAddRuleLabel.setText("Add rule:");

      hypAddRuleTextField.setEnabled(false);
      hypAddRuleTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            hypAddRuleTextFieldKeyReleased(evt);
         }
      });

      hypAddRuleLevelComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Non compound", "Compound" }));
      hypAddRuleLevelComboBox.setEnabled(false);
      hypAddRuleLevelComboBox.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            hypAddRuleLevelComboBoxActionPerformed(evt);
         }
      });

      hypAddRuleButton.setMnemonic('A');
      hypAddRuleButton.setText("Add rule");
      hypAddRuleButton.setEnabled(false);
      hypAddRuleButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            hypAddRuleButtonActionPerformed(evt);
         }
      });

      hypAddRuleSyllabationLabel.setLabelFor(hypAddRuleSyllabationOutputLabel);
      hypAddRuleSyllabationLabel.setText("New syllabation:");

      hypAddRuleSyllabationOutputLabel.setText("…");

      hypAddRuleSyllabesCountLabel.setLabelFor(hypAddRuleSyllabesCountOutputLabel);
      hypAddRuleSyllabesCountLabel.setText("New syllabes:");

      hypAddRuleSyllabesCountOutputLabel.setText("…");

      optionsButton.setText("Options");
      optionsButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            optionsButtonActionPerformed(evt);
         }
      });

      openHypButton.setText("Open Hyphenation");
      openHypButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            openHypButtonActionPerformed(evt);
         }
      });

      hypLayeredPane.setLayer(hypWordLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      hypLayeredPane.setLayer(hypWordTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      hypLayeredPane.setLayer(hypSyllabationLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      hypLayeredPane.setLayer(hypSyllabationOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      hypLayeredPane.setLayer(hypSyllabesCountLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      hypLayeredPane.setLayer(hypSyllabesCountOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      hypLayeredPane.setLayer(hypRulesLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      hypLayeredPane.setLayer(hypRulesOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      hypLayeredPane.setLayer(hypAddRuleLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      hypLayeredPane.setLayer(hypAddRuleTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      hypLayeredPane.setLayer(hypAddRuleLevelComboBox, javax.swing.JLayeredPane.DEFAULT_LAYER);
      hypLayeredPane.setLayer(hypAddRuleButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      hypLayeredPane.setLayer(hypAddRuleSyllabationLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      hypLayeredPane.setLayer(hypAddRuleSyllabationOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      hypLayeredPane.setLayer(hypAddRuleSyllabesCountLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      hypLayeredPane.setLayer(hypAddRuleSyllabesCountOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      hypLayeredPane.setLayer(optionsButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      hypLayeredPane.setLayer(openHypButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout hypLayeredPaneLayout = new javax.swing.GroupLayout(hypLayeredPane);
      hypLayeredPane.setLayout(hypLayeredPaneLayout);
      hypLayeredPaneLayout.setHorizontalGroup(
         hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(hypLayeredPaneLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(hypLayeredPaneLayout.createSequentialGroup()
                  .addComponent(hypWordLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypWordTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 870, Short.MAX_VALUE))
               .addGroup(hypLayeredPaneLayout.createSequentialGroup()
                  .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(hypSyllabationLabel)
                     .addComponent(hypSyllabesCountLabel))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(hypSyllabesCountOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addComponent(hypSyllabationOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
               .addGroup(hypLayeredPaneLayout.createSequentialGroup()
                  .addComponent(hypAddRuleLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypAddRuleTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 662, Short.MAX_VALUE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(hypAddRuleLevelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addGap(18, 18, 18)
                  .addComponent(hypAddRuleButton))
               .addGroup(hypLayeredPaneLayout.createSequentialGroup()
                  .addComponent(hypAddRuleSyllabesCountLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypAddRuleSyllabesCountOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addGap(13, 13, 13))
               .addGroup(hypLayeredPaneLayout.createSequentialGroup()
                  .addComponent(hypAddRuleSyllabationLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypAddRuleSyllabationOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, hypLayeredPaneLayout.createSequentialGroup()
                  .addComponent(hypRulesLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypRulesOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(hypLayeredPaneLayout.createSequentialGroup()
                  .addComponent(optionsButton)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addComponent(openHypButton)))
            .addContainerGap())
      );
      hypLayeredPaneLayout.setVerticalGroup(
         hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(hypLayeredPaneLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypWordLabel)
               .addComponent(hypWordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypSyllabationLabel)
               .addComponent(hypSyllabationOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypSyllabesCountLabel)
               .addComponent(hypSyllabesCountOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypRulesLabel)
               .addComponent(hypRulesOutputLabel))
            .addGap(18, 18, 18)
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypAddRuleLabel)
               .addComponent(hypAddRuleTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(hypAddRuleButton)
               .addComponent(hypAddRuleLevelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypAddRuleSyllabationLabel)
               .addComponent(hypAddRuleSyllabationOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypAddRuleSyllabesCountLabel)
               .addComponent(hypAddRuleSyllabesCountOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 35, Short.MAX_VALUE)
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(optionsButton)
               .addComponent(openHypButton))
            .addContainerGap())
      );

      mainTabbedPane.addTab("Hyphenation", hypLayeredPane);

      acoIncorrectLabel.setLabelFor(acoIncorrectTextField);
      acoIncorrectLabel.setText("Incorrect form:");

      acoIncorrectTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            acoIncorrectTextFieldKeyReleased(evt);
         }
      });

      acoToLabel.setText("→");

      acoCorrectLabel.setLabelFor(acoCorrectTextField);
      acoCorrectLabel.setText("Correct form:");

      acoCorrectTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            acoCorrectTextFieldKeyReleased(evt);
         }
      });

      acoAddButton.setMnemonic('A');
      acoAddButton.setText("Add");
      acoAddButton.setToolTipText("");
      acoAddButton.setEnabled(false);
      acoAddButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            acoAddButtonActionPerformed(evt);
         }
      });

      acoTable.setModel(new AutoCorrectTableModel());
      acoTable.setRowSorter(new TableRowSorter<>((AutoCorrectTableModel)acoTable.getModel()));
      acoTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
      acoTable.setShowHorizontalLines(false);
      acoTable.setShowVerticalLines(false);
      //listen for row removal
      acoTable.registerKeyboardAction(event -> removeSelectedRowsFromAutoCorrect(), cancelKeyStroke, JComponent.WHEN_FOCUSED);
      acoTable.registerKeyboardAction(event -> GUIUtils.copyToClipboard((JCopyableTable)acoTable), copyKeyStroke, JComponent.WHEN_FOCUSED);

      JFrame acoParent = this;
      acoTable.addMouseListener(new MouseAdapter(){
         public void mouseClicked(final MouseEvent e){
            if(e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1){
               final int selectedRow = acoTable.rowAtPoint(e.getPoint());
               acoTable.setRowSelectionInterval(selectedRow, selectedRow);
               final int row = acoTable.convertRowIndexToModel(selectedRow);
               final BiConsumer<String, String> okButtonAction = (incorrect, correct) -> {
                  try{
                     backbone.getAcoParser().setCorrection(row, incorrect, correct);

                     //… and save the files
                     backbone.storeAutoCorrectFile();
                  }
                  catch(Exception ex){
                     LOGGER.info(Backbone.MARKER_APPLICATION, ex.getMessage());
                  }
               };
               final CorrectionEntry definition = backbone.getAcoParser().getCorrectionsDictionary().get(row);
               final CorrectionDialog dialog = new CorrectionDialog(definition, okButtonAction, acoParent);
               GUIUtils.addCancelByEscapeKey(dialog);
               dialog.addWindowListener(new WindowAdapter(){
                  @Override
                  public void windowClosed(final WindowEvent e){
                     acoTable.clearSelection();
                  }
               });
               dialog.setLocationRelativeTo(acoParent);
               dialog.setVisible(true);
            }
         }
      });

      TableRenderer acoCellRenderer = new TableRenderer();
      acoTable.getColumnModel().getColumn(1).setCellRenderer(acoCellRenderer);
      acoScrollPane.setViewportView(acoTable);

      acoCorrectionsRecordedLabel.setLabelFor(acoCorrectionsRecordedOutputLabel);
      acoCorrectionsRecordedLabel.setText("Corrections recorded:");

      acoCorrectionsRecordedOutputLabel.setText("…");

      openAcoButton.setText("Open AutoCorrect");
      openAcoButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            openAcoButtonActionPerformed(evt);
         }
      });

      acoLayeredPane.setLayer(acoIncorrectLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      acoLayeredPane.setLayer(acoIncorrectTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      acoLayeredPane.setLayer(acoToLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      acoLayeredPane.setLayer(acoCorrectLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      acoLayeredPane.setLayer(acoCorrectTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      acoLayeredPane.setLayer(acoAddButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      acoLayeredPane.setLayer(acoScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      acoLayeredPane.setLayer(acoCorrectionsRecordedLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      acoLayeredPane.setLayer(acoCorrectionsRecordedOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      acoLayeredPane.setLayer(openAcoButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout acoLayeredPaneLayout = new javax.swing.GroupLayout(acoLayeredPane);
      acoLayeredPane.setLayout(acoLayeredPaneLayout);
      acoLayeredPaneLayout.setHorizontalGroup(
         acoLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(acoLayeredPaneLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(acoLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(acoScrollPane)
               .addGroup(acoLayeredPaneLayout.createSequentialGroup()
                  .addComponent(acoIncorrectLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(acoIncorrectTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 330, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, Short.MAX_VALUE)
                  .addComponent(acoToLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(acoCorrectLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(acoCorrectTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 330, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addGap(18, 18, 18)
                  .addComponent(acoAddButton))
               .addGroup(acoLayeredPaneLayout.createSequentialGroup()
                  .addComponent(acoCorrectionsRecordedLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(acoCorrectionsRecordedOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 673, Short.MAX_VALUE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(openAcoButton)))
            .addContainerGap())
      );
      acoLayeredPaneLayout.setVerticalGroup(
         acoLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(acoLayeredPaneLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(acoLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(acoIncorrectLabel)
               .addComponent(acoIncorrectTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(acoCorrectTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(acoAddButton)
               .addComponent(acoToLabel)
               .addComponent(acoCorrectLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(acoScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 166, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(acoLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(acoCorrectionsRecordedLabel)
               .addComponent(acoCorrectionsRecordedOutputLabel)
               .addComponent(openAcoButton))
            .addContainerGap())
      );

      mainTabbedPane.addTab("AutoCorrect", acoLayeredPane);

      sexInputLabel.setLabelFor(sexTextField);
      sexInputLabel.setText("Exception:");

      sexTextField.setToolTipText("hit `enter` to add");
      sexTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            sexTextFieldKeyReleased(evt);
         }
      });

      sexAddButton.setMnemonic('A');
      sexAddButton.setText("Add");
      sexAddButton.setToolTipText("");
      sexAddButton.setEnabled(false);
      sexAddButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            sexAddButtonActionPerformed(evt);
         }
      });

      sexScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      sexScrollPane.setViewportView(sexTagPanel);

      sexCorrectionsRecordedLabel.setLabelFor(wexCorrectionsRecordedOutputLabel);
      sexCorrectionsRecordedLabel.setText("Exceptions recorded:");

      sexCorrectionsRecordedOutputLabel.setText("…");

      openSexButton.setText("Open Sentence Exceptions");
      openSexButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            openSexButtonActionPerformed(evt);
         }
      });

      sexLayeredPane.setLayer(sexInputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      sexLayeredPane.setLayer(sexTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      sexLayeredPane.setLayer(sexAddButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      sexLayeredPane.setLayer(sexScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      sexLayeredPane.setLayer(sexCorrectionsRecordedLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      sexLayeredPane.setLayer(sexCorrectionsRecordedOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      sexLayeredPane.setLayer(openSexButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout sexLayeredPaneLayout = new javax.swing.GroupLayout(sexLayeredPane);
      sexLayeredPane.setLayout(sexLayeredPaneLayout);
      sexLayeredPaneLayout.setHorizontalGroup(
         sexLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(sexLayeredPaneLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(sexLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(sexScrollPane)
               .addGroup(sexLayeredPaneLayout.createSequentialGroup()
                  .addComponent(sexCorrectionsRecordedLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(sexCorrectionsRecordedOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(openSexButton))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, sexLayeredPaneLayout.createSequentialGroup()
                  .addComponent(sexInputLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(sexTextField)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(sexAddButton)))
            .addContainerGap())
      );
      sexLayeredPaneLayout.setVerticalGroup(
         sexLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(sexLayeredPaneLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(sexLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(sexTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(sexInputLabel)
               .addComponent(sexAddButton))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(sexScrollPane)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(sexLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(sexCorrectionsRecordedLabel)
               .addComponent(sexCorrectionsRecordedOutputLabel)
               .addComponent(openSexButton))
            .addContainerGap())
      );

      mainTabbedPane.addTab("Sentence Exceptions", sexLayeredPane);

      wexInputLabel.setLabelFor(wexTextField);
      wexInputLabel.setText("Exception:");
      wexInputLabel.setToolTipText("");

      wexTextField.setToolTipText("hit `enter` to add");
      wexTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            wexTextFieldKeyReleased(evt);
         }
      });

      wexAddButton.setMnemonic('A');
      wexAddButton.setText("Add");
      wexAddButton.setEnabled(false);
      wexAddButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            wexAddButtonActionPerformed(evt);
         }
      });

      wexScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      wexScrollPane.setViewportView(wexTagPanel);

      wexCorrectionsRecordedLabel.setLabelFor(wexCorrectionsRecordedOutputLabel);
      wexCorrectionsRecordedLabel.setText("Exceptions recorded:");

      wexCorrectionsRecordedOutputLabel.setText("…");

      openWexButton.setText("Open Word Exceptions");
      openWexButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            openWexButtonActionPerformed(evt);
         }
      });

      wexLayeredPane.setLayer(wexInputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      wexLayeredPane.setLayer(wexTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      wexLayeredPane.setLayer(wexAddButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      wexLayeredPane.setLayer(wexScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      wexLayeredPane.setLayer(wexCorrectionsRecordedLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      wexLayeredPane.setLayer(wexCorrectionsRecordedOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      wexLayeredPane.setLayer(openWexButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout wexLayeredPaneLayout = new javax.swing.GroupLayout(wexLayeredPane);
      wexLayeredPane.setLayout(wexLayeredPaneLayout);
      wexLayeredPaneLayout.setHorizontalGroup(
         wexLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(wexLayeredPaneLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(wexLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(wexScrollPane)
               .addGroup(wexLayeredPaneLayout.createSequentialGroup()
                  .addComponent(wexCorrectionsRecordedLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(wexCorrectionsRecordedOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(openWexButton))
               .addGroup(wexLayeredPaneLayout.createSequentialGroup()
                  .addComponent(wexInputLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(wexTextField)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(wexAddButton)))
            .addContainerGap())
      );
      wexLayeredPaneLayout.setVerticalGroup(
         wexLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(wexLayeredPaneLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(wexLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(wexInputLabel)
               .addComponent(wexTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(wexAddButton))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(wexScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(wexLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(wexCorrectionsRecordedLabel)
               .addComponent(wexCorrectionsRecordedOutputLabel)
               .addComponent(openWexButton))
            .addContainerGap())
      );

      mainTabbedPane.addTab("Word Exceptions", wexLayeredPane);

      addWindowListener(new WindowAdapter(){
         @Override
         public void windowClosed(final WindowEvent e){
            exit();
         }
      });

      filMenu.setMnemonic('F');
      filMenu.setText("File");
      filMenu.setToolTipText("");

      filOpenProjectMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/file_open.png"))); // NOI18N
      filOpenProjectMenuItem.setMnemonic('O');
      filOpenProjectMenuItem.setText("Open Project…");
      filOpenProjectMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            filOpenProjectMenuItemActionPerformed(evt);
         }
      });
      filMenu.add(filOpenProjectMenuItem);

      filCreatePackageMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/file_package.png"))); // NOI18N
      filCreatePackageMenuItem.setMnemonic('p');
      filCreatePackageMenuItem.setText("Create package");
      filCreatePackageMenuItem.setEnabled(false);
      filCreatePackageMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            filCreatePackageMenuItemActionPerformed(evt);
         }
      });
      filMenu.add(filCreatePackageMenuItem);
      filMenu.add(filFontSeparator);

      filFontMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/file_font.png"))); // NOI18N
      filFontMenuItem.setMnemonic('f');
      filFontMenuItem.setText("Select font…");
      filFontMenuItem.setEnabled(false);
      filFontMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            filFontMenuItemActionPerformed(evt);
         }
      });
      filMenu.add(filFontMenuItem);
      filMenu.add(filRecentProjectsSeparator);

      filEmptyRecentProjectsMenuItem.setMnemonic('e');
      filEmptyRecentProjectsMenuItem.setText("Empty Recent Projects list");
      filEmptyRecentProjectsMenuItem.setEnabled(false);
      filEmptyRecentProjectsMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            filEmptyRecentProjectsMenuItemActionPerformed(evt);
         }
      });
      filMenu.add(filEmptyRecentProjectsMenuItem);
      filMenu.add(filSeparator);

      filExitMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/file_exit.png"))); // NOI18N
      filExitMenuItem.setMnemonic('x');
      filExitMenuItem.setText("Exit");
      filExitMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            filExitMenuItemActionPerformed(evt);
         }
      });
      filMenu.add(filExitMenuItem);

      mainMenuBar.add(filMenu);
      Preferences preferences = Preferences.userNodeForPackage(getClass());
      RecentItems recentItems = new RecentItems(5, preferences);
      recentProjectsMenu = new unit731.hunlinter.gui.RecentFilesMenu(recentItems, this::loadFile);
      recentProjectsMenu.setText("Recent projects");
      recentProjectsMenu.setMnemonic('R');
      filMenu.add(recentProjectsMenu, 3);

      dicMenu.setMnemonic('D');
      dicMenu.setText("Dictionary tools");
      dicMenu.setToolTipText("");
      dicMenu.setEnabled(false);

      dicCheckCorrectnessMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/dictionary_correctness.png"))); // NOI18N
      dicCheckCorrectnessMenuItem.setMnemonic('c');
      dicCheckCorrectnessMenuItem.setText("Check correctness");
      dicCheckCorrectnessMenuItem.setToolTipText("");
      dicCheckCorrectnessMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            dicCheckCorrectnessMenuItemActionPerformed(evt);
         }
      });
      dicMenu.add(dicCheckCorrectnessMenuItem);

      dicSortDictionaryMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/dictionary_sort.png"))); // NOI18N
      dicSortDictionaryMenuItem.setMnemonic('s');
      dicSortDictionaryMenuItem.setText("Sort dictionary…");
      dicSortDictionaryMenuItem.setToolTipText("");
      dicSortDictionaryMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            dicSortDictionaryMenuItemActionPerformed(evt);
         }
      });
      dicMenu.add(dicSortDictionaryMenuItem);

      dicRulesReducerMenuItem.setText("Rules reducer…");
      dicRulesReducerMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            dicRulesReducerMenuItemActionPerformed(evt);
         }
      });
      dicMenu.add(dicRulesReducerMenuItem);
      dicMenu.add(dicDuplicatesSeparator);

      dicWordCountMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/dictionary_count.png"))); // NOI18N
      dicWordCountMenuItem.setMnemonic('w');
      dicWordCountMenuItem.setText("Word count");
      dicWordCountMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            dicWordCountMenuItemActionPerformed(evt);
         }
      });
      dicMenu.add(dicWordCountMenuItem);

      dicStatisticsMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/dictionary_statistics.png"))); // NOI18N
      dicStatisticsMenuItem.setMnemonic('t');
      dicStatisticsMenuItem.setText("Statistics");
      dicStatisticsMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            dicStatisticsMenuItemActionPerformed(evt);
         }
      });
      dicMenu.add(dicStatisticsMenuItem);
      dicMenu.add(dicStatisticsSeparator);

      dicExtractDuplicatesMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/dictionary_duplicates.png"))); // NOI18N
      dicExtractDuplicatesMenuItem.setMnemonic('d');
      dicExtractDuplicatesMenuItem.setText("Extract duplicates…");
      dicExtractDuplicatesMenuItem.setToolTipText("");
      dicExtractDuplicatesMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            dicExtractDuplicatesMenuItemActionPerformed(evt);
         }
      });
      dicMenu.add(dicExtractDuplicatesMenuItem);

      dicExtractWordlistMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/dictionary_wordlist.png"))); // NOI18N
      dicExtractWordlistMenuItem.setText("Extract wordlist…");
      dicExtractWordlistMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            dicExtractWordlistMenuItemActionPerformed(evt);
         }
      });
      dicMenu.add(dicExtractWordlistMenuItem);

      dicExtractWordlistPlainTextMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/dictionary_wordlist.png"))); // NOI18N
      dicExtractWordlistPlainTextMenuItem.setText("Extract wordlist (plain words)…");
      dicExtractWordlistPlainTextMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            dicExtractWordlistPlainTextMenuItemActionPerformed(evt);
         }
      });
      dicMenu.add(dicExtractWordlistPlainTextMenuItem);

      dicExtractPoSFAMenuItem.setText("Extract PoS FSA…");
      dicExtractPoSFAMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            dicExtractPoSFAMenuItemActionPerformed(evt);
         }
      });
      dicMenu.add(dicExtractPoSFAMenuItem);

      dicExtractMinimalPairsMenuItem.setMnemonic('m');
      dicExtractMinimalPairsMenuItem.setText("Extract minimal pairs…");
      dicExtractMinimalPairsMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            dicExtractMinimalPairsMenuItemActionPerformed(evt);
         }
      });
      dicMenu.add(dicExtractMinimalPairsMenuItem);

      mainMenuBar.add(dicMenu);

      hypMenu.setMnemonic('y');
      hypMenu.setText("Hyphenation tools");
      hypMenu.setToolTipText("");
      hypMenu.setEnabled(false);

      hypCheckCorrectnessMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/dictionary_correctness.png"))); // NOI18N
      hypCheckCorrectnessMenuItem.setMnemonic('d');
      hypCheckCorrectnessMenuItem.setText("Check correctness");
      hypCheckCorrectnessMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            hypCheckCorrectnessMenuItemActionPerformed(evt);
         }
      });
      hypMenu.add(hypCheckCorrectnessMenuItem);
      hypMenu.add(hypDuplicatesSeparator);

      hypStatisticsMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/dictionary_statistics.png"))); // NOI18N
      hypStatisticsMenuItem.setMnemonic('t');
      hypStatisticsMenuItem.setText("Statistics");
      hypStatisticsMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            hypStatisticsMenuItemActionPerformed(evt);
         }
      });
      hypMenu.add(hypStatisticsMenuItem);

      mainMenuBar.add(hypMenu);

      hlpMenu.setMnemonic('H');
      hlpMenu.setText("Help");

      hlpOnlineHelpMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/help_help.png"))); // NOI18N
      hlpOnlineHelpMenuItem.setMnemonic('h');
      hlpOnlineHelpMenuItem.setText("Online help");
      hlpOnlineHelpMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            hlpOnlineHelpMenuItemActionPerformed(evt);
         }
      });
      hlpMenu.add(hlpOnlineHelpMenuItem);

      hlpIssueReporterMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/help_issue.png"))); // NOI18N
      hlpIssueReporterMenuItem.setText("Report an issue");
      hlpIssueReporterMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            hlpIssueReporterMenuItemActionPerformed(evt);
         }
      });
      hlpMenu.add(hlpIssueReporterMenuItem);
      hlpMenu.add(hlpOnlineSeparator);

      hlpUpdateMenuItem.setText("Check for Update…");
      hlpUpdateMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            hlpUpdateMenuItemActionPerformed(evt);
         }
      });
      hlpMenu.add(hlpUpdateMenuItem);

      hlpCheckUpdateOnStartupCheckBoxMenuItem.setSelected(preferences.getBoolean(UPDATE_STARTUP_CHECK, true));
      hlpCheckUpdateOnStartupCheckBoxMenuItem.setText("Check for updates on startup");
      hlpCheckUpdateOnStartupCheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            hlpCheckUpdateOnStartupCheckBoxMenuItemActionPerformed(evt);
         }
      });
      hlpMenu.add(hlpCheckUpdateOnStartupCheckBoxMenuItem);
      hlpMenu.add(hlpUpdateSeparator);

      hlpAboutMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/help_about.png"))); // NOI18N
      hlpAboutMenuItem.setMnemonic('a');
      hlpAboutMenuItem.setText("About");
      hlpAboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            hlpAboutMenuItemActionPerformed(evt);
         }
      });
      hlpMenu.add(hlpAboutMenuItem);

      mainMenuBar.add(hlpMenu);

      setJMenuBar(mainMenuBar);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(mainTabbedPane)
               .addComponent(parsingResultScrollPane, javax.swing.GroupLayout.Alignment.TRAILING)
               .addComponent(mainProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(parsingResultScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(mainProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addComponent(mainTabbedPane)
            .addContainerGap())
      );

      for(int i = 0; i < mainTabbedPane.getTabCount(); i ++)
      mainTabbedPane.setEnabledAt(i, false);
      KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
      mainTabbedPane.registerKeyboardAction(this, escapeKeyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

      pack();
      setLocationRelativeTo(null);
   }// </editor-fold>//GEN-END:initComponents

	private void filOpenProjectMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filOpenProjectMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		final int projectSelected = openProjectPathFileChooser.showOpenDialog(this);
		if(projectSelected == JFileChooser.APPROVE_OPTION){
			recentProjectsMenu.addEntry(openProjectPathFileChooser.getSelectedFile().getAbsolutePath());

			recentProjectsMenu.setEnabled(true);
			filEmptyRecentProjectsMenuItem.setEnabled(true);

			final File baseFile = openProjectPathFileChooser.getSelectedFile();
			loadFile(baseFile.toPath());
		}
	}//GEN-LAST:event_filOpenProjectMenuItemActionPerformed

	private void filCreatePackageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filCreatePackageMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		backbone.createPackage();
	}//GEN-LAST:event_filCreatePackageMenuItemActionPerformed

	private void filExitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filExitMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		exit();
	}//GEN-LAST:event_filExitMenuItemActionPerformed

	private void exit(){
		dispose();
	}

	private void hlpAboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hlpAboutMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		final HelpDialog dialog = new HelpDialog(this);
		GUIUtils.addCancelByEscapeKey(dialog);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}//GEN-LAST:event_hlpAboutMenuItemActionPerformed


	private void calculateProductions(final HunLinterFrame frame){
		final String inputText = StringUtils.strip(frame.dicInputTextField.getText());

		if(formerInputText != null && formerInputText.equals(inputText))
			return;
		formerInputText = inputText;

		if(StringUtils.isNotBlank(inputText)){
			try{
				final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(inputText,
					frame.backbone.getAffixData());
				final List<Production> productions = frame.backbone.getWordGenerator().applyAffixRules(dicEntry);

				final ProductionTableModel dm = (ProductionTableModel)frame.dicTable.getModel();
				dm.setProductions(productions);

				//show first row
				final Rectangle cellRect = frame.dicTable.getCellRect(0, 0, true);
				frame.dicTable.scrollRectToVisible(cellRect);

				frame.dicTotalProductionsOutputLabel.setText(Integer.toString(productions.size()));

				//check for correctness
				int line = 0;
				final DictionaryCorrectnessChecker checker = backbone.getChecker();
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
						LOGGER.info(Backbone.MARKER_APPLICATION, "{}, line {}", sb.toString(), line);
					}

					line ++;
				}
			}
			catch(final Exception e){
				LOGGER.info(Backbone.MARKER_APPLICATION, "{} for input {}", e.getMessage(), inputText);
			}
		}
		else{
			frame.clearOutputTable(frame.dicTable);
			frame.dicTotalProductionsOutputLabel.setText(StringUtils.EMPTY);
		}
	}

	private void calculateCompoundProductions(final HunLinterFrame frame){
		final String inputText = StringUtils.strip((String)frame.cmpInputComboBox.getEditor().getItem());

		cmpLimitComboBox.setEnabled(StringUtils.isNotBlank(inputText));

		if(formerCompoundInputText != null && formerCompoundInputText.equals(inputText))
			return;
		formerCompoundInputText = inputText;

		frame.cmpLimitComboBoxActionPerformed(null);
	}


	private void dicCheckCorrectnessMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicCheckCorrectnessMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		checkDictionaryCorrectness();
	}//GEN-LAST:event_dicCheckCorrectnessMenuItemActionPerformed

	private void dicSortDictionaryMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicSortDictionaryMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		try{
			final String[] lines = backbone.getDictionaryLines();
			final DictionarySortDialog dialog = new DictionarySortDialog(backbone.getDicParser(), lines,
				lastDictionarySortVisibleIndex, this);
			GUIUtils.addCancelByEscapeKey(dialog);
			dialog.setLocationRelativeTo(this);
			dialog.addListSelectionListener(e -> {
				if(e.getValueIsAdjusting() && (dicSorterWorker == null || dicSorterWorker.isDone())){
					final int selectedRow = dialog.getSelectedIndex();
					if(backbone.getDicParser().isInBoundary(selectedRow)){
						dialog.setVisible(false);

						dicSortDictionaryMenuItem.setEnabled(false);


						dicSorterWorker = new SorterWorker(backbone, selectedRow);
						dicSorterWorker.addPropertyChangeListener(this);
						dicSorterWorker.execute();
					}
				}
			});
			dialog.addWindowListener(new WindowAdapter(){
				@Override
				public void windowDeactivated(final WindowEvent e){
					lastDictionarySortVisibleIndex = dialog.getFirstVisibleIndex();
				}
			});
			dialog.setVisible(true);
		}
		catch(final IOException e){
			LOGGER.error("Something very bad happened while sorting the dictionary", e);
		}
	}//GEN-LAST:event_dicSortDictionaryMenuItemActionPerformed

	private void dicExtractDuplicatesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicExtractDuplicatesMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		extractDictionaryDuplicates();
	}//GEN-LAST:event_dicExtractDuplicatesMenuItemActionPerformed

	private void dicExtractWordlistMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicExtractWordlistMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		extractDictionaryWordlist(WordlistWorker.WorkerType.COMPLETE);
	}//GEN-LAST:event_dicExtractWordlistMenuItemActionPerformed

	private void dicExtractPoSFAMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicExtractPoSFAMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		extractPoSFSA();
	}//GEN-LAST:event_dicExtractPoSFAMenuItemActionPerformed

	private void dicExtractMinimalPairsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicExtractMinimalPairsMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		extractMinimalPairs();
	}//GEN-LAST:event_dicExtractMinimalPairsMenuItemActionPerformed

	private void cmpInputComboBoxKeyReleased(){
		compoundProductionDebouncer.call(this);
	}

	private void filterThesaurus(HunLinterFrame frame){
		final String unmodifiedSearchText = StringUtils.strip(frame.theSynonymsTextField.getText());
		if(formerFilterThesaurusText != null && formerFilterThesaurusText.equals(unmodifiedSearchText))
			return;

		formerFilterThesaurusText = unmodifiedSearchText;

		final Pair<String[], String[]> pair = ThesaurusParser.extractComponentsForFilter(unmodifiedSearchText);
		//if text to be inserted is already fully contained into the thesaurus, do not enable the button
		final boolean alreadyContained = backbone.getTheParser().contains(pair.getLeft(), pair.getRight());
		theAddButton.setEnabled(!alreadyContained);

		@SuppressWarnings("unchecked")
		final TableRowSorter<ThesaurusTableModel> sorter = (TableRowSorter<ThesaurusTableModel>)frame.theTable.getRowSorter();
		if(StringUtils.isNotBlank(unmodifiedSearchText)){
			final Pair<String, String> searchText = ThesaurusParser.prepareTextForFilter(pair.getLeft(), pair.getRight());
			EventQueue.invokeLater(() -> sorter.setRowFilter(RowFilter.regexFilter(searchText.getRight())));
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
			JOptionPane.showMessageDialog(null, "No synonyms with same part-of-speech present.\r\nCannot merge automatically, do it manually.",
				"Warning", JOptionPane.WARNING_MESSAGE);
		else{
			//show merge dialog
			final ThesaurusMergeDialog dialog = new ThesaurusMergeDialog(newSynonyms, synonyms.getDefinition(), filteredSynonymsEntries, null);
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
			backbone.getTheParser()
				.deleteDefinitionAndSynonyms(selectedDefinition, selectedSynonyms);

			dm.setSynonyms(backbone.getTheParser().getSynonymsDictionary());
			updateSynonymsCounter();

			//… and save the files
			backbone.storeThesaurusFiles();


			//redo filtering, that is re-set the state of the button (it may have changed)
			final String unmodifiedSearchText = theSynonymsTextField.getText();
			if(StringUtils.isNotBlank(unmodifiedSearchText)){
				final Pair<String[], String[]> pair = ThesaurusParser.extractComponentsForFilter(unmodifiedSearchText);
				//if text to be inserted is already fully contained into the thesaurus, do not enable the button
				final boolean alreadyContained = backbone.getTheParser().contains(pair.getLeft(), pair.getRight());
				theAddButton.setEnabled(!alreadyContained);
			}
		}
		catch(final Exception e){
			LOGGER.info(Backbone.MARKER_APPLICATION, "Deletion error: {}", e.getMessage());
		}
	}

	private void filterAutoCorrect(final HunLinterFrame frame){
		final String unmodifiedIncorrectText = StringUtils.strip(frame.acoIncorrectTextField.getText());
		final String unmodifiedCorrectText = StringUtils.strip(frame.acoCorrectTextField.getText());
		if(formerFilterIncorrectText != null && formerFilterIncorrectText.equals(unmodifiedIncorrectText)
				&& formerFilterCorrectText != null && formerFilterCorrectText.equals(unmodifiedCorrectText))
			return;

		formerFilterIncorrectText = unmodifiedIncorrectText;
		formerFilterCorrectText = unmodifiedCorrectText;

		final Pair<String, String> pair = AutoCorrectParser.extractComponentsForFilter(unmodifiedIncorrectText, unmodifiedCorrectText);
		final String incorrect = pair.getLeft();
		final String correct = pair.getRight();
		//if text to be inserted is already fully contained into the thesaurus, do not enable the button
		final boolean alreadyContained = backbone.getAcoParser().contains(incorrect, correct);
		acoAddButton.setEnabled(StringUtils.isNotBlank(unmodifiedIncorrectText) && StringUtils.isNotBlank(unmodifiedCorrectText)
			&& !unmodifiedIncorrectText.equals(unmodifiedCorrectText) && !alreadyContained);

		@SuppressWarnings("unchecked")
		final TableRowSorter<AutoCorrectTableModel> sorter = (TableRowSorter<AutoCorrectTableModel>)frame.acoTable.getRowSorter();
		if(StringUtils.isNotBlank(unmodifiedIncorrectText) || StringUtils.isNotBlank(unmodifiedCorrectText)){
			final Pair<String, String> searchText = AutoCorrectParser.prepareTextForFilter(incorrect, correct);
			final RowFilter<AutoCorrectTableModel, Integer> filterIncorrect = RowFilter.regexFilter(searchText.getLeft(), 0);
			final RowFilter<AutoCorrectTableModel, Integer> filterCorrect = RowFilter.regexFilter(searchText.getRight(), 1);
			EventQueue.invokeLater(() -> sorter.setRowFilter(RowFilter.andFilter(Arrays.asList(filterIncorrect, filterCorrect))));
		}
		else
			sorter.setRowFilter(null);
	}

	public void removeSelectedRowsFromAutoCorrect(){
		try{
			final int selectedRow = acoTable.convertRowIndexToModel(acoTable.getSelectedRow());
			backbone.getAcoParser().deleteCorrection(selectedRow);

			final AutoCorrectTableModel dm = (AutoCorrectTableModel)acoTable.getModel();
			dm.fireTableDataChanged();

			updateCorrectionsCounter();

			//… and save the files
			backbone.storeAutoCorrectFile();
		}
		catch(final Exception e){
			LOGGER.info(Backbone.MARKER_APPLICATION, "Deletion error: {}", e.getMessage());
		}
	}


	private void filterSentenceExceptions(final HunLinterFrame frame){
		final String unmodifiedException = StringUtils.strip(frame.sexTextField.getText());
		if(formerFilterSentenceException != null && formerFilterSentenceException.equals(unmodifiedException))
			return;

		formerFilterSentenceException = unmodifiedException;

		//if text to be inserted is already fully contained into the thesaurus, do not enable the button
		final boolean alreadyContained = backbone.getSexParser().contains(unmodifiedException);
		sexAddButton.setEnabled(StringUtils.isNotBlank(unmodifiedException) && unmodifiedException.endsWith(".") && !alreadyContained);


		sexTagPanel.applyFilter(StringUtils.isNotBlank(unmodifiedException)? unmodifiedException: null);
	}


	private void filterWordExceptions(final HunLinterFrame frame){
		final String unmodifiedException = StringUtils.strip(frame.wexTextField.getText());
		if(formerFilterWordException != null && formerFilterWordException.equals(unmodifiedException))
			return;

		formerFilterWordException = unmodifiedException;

		//if text to be inserted is already fully contained into the thesaurus, do not enable the button
		final boolean alreadyContained = backbone.getWexParser().contains(unmodifiedException);
		wexAddButton.setEnabled(StringUtils.isNotBlank(unmodifiedException) && StringHelper.countUppercases(unmodifiedException) > 1 && !alreadyContained);


		wexTagPanel.applyFilter(StringUtils.isNotBlank(unmodifiedException)? unmodifiedException: null);
	}

	private void dicStatisticsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicStatisticsMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		extractDictionaryStatistics(false);
	}//GEN-LAST:event_dicStatisticsMenuItemActionPerformed

	private void dicWordCountMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicWordCountMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		try{
			extractWordCount();
		}
		catch(final Exception e){
			LOGGER.error(Backbone.MARKER_APPLICATION, e.getMessage());
		}
	}//GEN-LAST:event_dicWordCountMenuItemActionPerformed

	private void hypCheckCorrectnessMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hypCheckCorrectnessMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		checkHyphenationCorrectness();
	}//GEN-LAST:event_hypCheckCorrectnessMenuItemActionPerformed

	private void filEmptyRecentProjectsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filEmptyRecentProjectsMenuItemActionPerformed
		recentProjectsMenu.clear();

		recentProjectsMenu.setEnabled(false);
		filEmptyRecentProjectsMenuItem.setEnabled(false);
		filOpenProjectMenuItem.setEnabled(true);
	}//GEN-LAST:event_filEmptyRecentProjectsMenuItemActionPerformed

	private void dicRulesReducerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicRulesReducerMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		dicRulesReducerMenuItem.setEnabled(false);

		rulesReducerDialog = new RulesReducerDialog(backbone, this);
		rulesReducerDialog.setLocationRelativeTo(this);
		rulesReducerDialog.addWindowListener(new WindowAdapter(){
			@Override
			public void windowDeactivated(final WindowEvent e){
				dicRulesReducerMenuItem.setEnabled(true);
			}
		});
		rulesReducerDialog.setVisible(true);
	}//GEN-LAST:event_dicRulesReducerMenuItemActionPerformed

	private void hypStatisticsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hypStatisticsMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		extractDictionaryStatistics(true);
	}//GEN-LAST:event_hypStatisticsMenuItemActionPerformed

	private void dicExtractWordlistPlainTextMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicExtractWordlistPlainTextMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		extractDictionaryWordlist(WordlistWorker.WorkerType.PLAIN_WORDS);
	}//GEN-LAST:event_dicExtractWordlistPlainTextMenuItemActionPerformed

	private void hypAddRuleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hypAddRuleButtonActionPerformed
		final  String newRule = hypAddRuleTextField.getText();
		final HyphenationParser.Level level = HyphenationParser.Level.values()[hypAddRuleLevelComboBox.getSelectedIndex()];
		final String foundRule = backbone.addHyphenationRule(newRule.toLowerCase(Locale.ROOT), level);
		if(foundRule == null){
			try{
				backbone.storeHyphenationFile();

				if(hypWordTextField.getText() != null){
					formerHyphenationText = null;
					hyphenate(this);
				}

				hypAddRuleLevelComboBox.setEnabled(false);
				hypAddRuleButton.setEnabled(false);
				hypAddRuleTextField.setText(null);
				hypAddRuleSyllabationOutputLabel.setText(null);
				hypAddRuleSyllabesCountOutputLabel.setText(null);
			}
			catch(final IOException e){
				LOGGER.error("Something very bad happened while adding a rule to the hyphenation file", e);
			}
		}
		else{
			hypAddRuleTextField.requestFocusInWindow();

			LOGGER.info(Backbone.MARKER_APPLICATION, "Duplicated rule found ({}), cannot insert {}", foundRule, newRule);
		}
	}//GEN-LAST:event_hypAddRuleButtonActionPerformed

	private void hypAddRuleLevelComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hypAddRuleLevelComboBoxActionPerformed
		hypAddRuleDebouncer.call(this);
	}//GEN-LAST:event_hypAddRuleLevelComboBoxActionPerformed

	private void hypAddRuleTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_hypAddRuleTextFieldKeyReleased
		hypAddRuleDebouncer.call(this);
	}//GEN-LAST:event_hypAddRuleTextFieldKeyReleased

	private void hypWordTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_hypWordTextFieldKeyReleased
		hypDebouncer.call(this);
	}//GEN-LAST:event_hypWordTextFieldKeyReleased

	private void theAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_theAddButtonActionPerformed
		try{
			//try adding the synonyms
			final String synonyms = theSynonymsTextField.getText();
			final Function<String, Boolean> duplicatesDiscriminator = message -> {
				final int responseOption = JOptionPane.showConfirmDialog(this,
					"There is some duplicates with same part of speech and definition(s) '" + message + "'.\nForce insertion?", "Select one",
					JOptionPane.YES_NO_OPTION);
				return (responseOption == JOptionPane.YES_OPTION);
			};
			final DuplicationResult<ThesaurusEntry> duplicationResult = backbone.getTheParser()
				.insertSynonyms(synonyms, duplicatesDiscriminator);
			final List<ThesaurusEntry> duplicates = duplicationResult.getDuplicates();

			if(duplicates.isEmpty() || duplicationResult.isForceInsertion()){
				//if everything's ok update the table and the sorter…
				final ThesaurusTableModel dm = (ThesaurusTableModel)theTable.getModel();
				dm.setSynonyms(backbone.getTheParser().getSynonymsDictionary());
				dm.fireTableDataChanged();

				formerFilterThesaurusText = null;
				theSynonymsTextField.setText(null);
				theSynonymsTextField.requestFocusInWindow();
				@SuppressWarnings("unchecked")
				TableRowSorter<ThesaurusTableModel> sorter = (TableRowSorter<ThesaurusTableModel>)theTable.getRowSorter();
				sorter.setRowFilter(null);

				updateSynonymsCounter();

				//… and save the files
				backbone.storeThesaurusFiles();
			}
			else{
				theSynonymsTextField.requestFocusInWindow();

				final String duplicatedWords = duplicates.stream()
					.map(ThesaurusEntry::getDefinition)
					.collect(Collectors.joining(", "));
				JOptionPane.showOptionDialog(this,
					"Some duplicates are present, namely:\n   " + duplicatedWords + "\n\nSynonyms was NOT inserted!",
					"Warning!", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, null,
					null);
			}
		}
		catch(final Exception e){
			LOGGER.info(Backbone.MARKER_APPLICATION, "Insertion error: {}", e.getMessage());
		}
	}//GEN-LAST:event_theAddButtonActionPerformed

	private void theSynonymsTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_theSynonymsTextFieldKeyReleased
		theFilterDebouncer.call(this);
	}//GEN-LAST:event_theSynonymsTextFieldKeyReleased

	private void cmpLoadInputButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmpLoadInputButtonActionPerformed
		extractCompoundRulesInputs();
	}//GEN-LAST:event_cmpLoadInputButtonActionPerformed

	private void cmpLimitComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmpLimitComboBoxActionPerformed
		final String inputText = StringUtils.strip((String)cmpInputComboBox.getEditor().getItem());
		final int limit = Integer.parseInt(cmpLimitComboBox.getItemAt(cmpLimitComboBox.getSelectedIndex()));
		final String inputCompounds = cmpInputTextArea.getText();

		if(StringUtils.isNotBlank(inputText)){
			try{
				//FIXME transfer into backbone
				final List<Production> words;
				final WordGenerator wordGenerator = backbone.getWordGenerator();
				final AffixData affixData = backbone.getAffixData();
				if(affixData.getCompoundFlag().equals(inputText)){
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
				LOGGER.info(Backbone.MARKER_APPLICATION, "{} for input {}", e.getMessage(), inputText);
			}
		}
		else
			clearOutputTable(cmpTable);
	}//GEN-LAST:event_cmpLimitComboBoxActionPerformed

	private void dicInputTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_dicInputTextFieldKeyReleased
		productionDebouncer.call(this);
	}//GEN-LAST:event_dicInputTextFieldKeyReleased

	private void filFontMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filFontMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		Consumer<Font> onSelection = font -> {
			GUIUtils.setCurrentFont(font, this);

			final String language = backbone.getAffixData().getLanguage();
			preferences.put(FONT_FAMILY_NAME_PREFIX + language, font.getFamily());
			preferences.put(FONT_SIZE_PREFIX + language, Integer.toString(font.getSize()));
		};
		FontChooserDialog dialog = new FontChooserDialog(backbone.getAffixData(), GUIUtils.getCurrentFont(), onSelection,
			this);
		GUIUtils.addCancelByEscapeKey(dialog);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}//GEN-LAST:event_filFontMenuItemActionPerformed

	private void hlpOnlineHelpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hlpOnlineHelpMenuItemActionPerformed
		if(Desktop.isDesktopSupported()){
			final Desktop desktop = Desktop.getDesktop();
			try{
				desktop.browse(new URI(URL_ONLINE_HELP));
			}
			catch(final Exception e){
				LOGGER.warn(Backbone.MARKER_APPLICATION, "Cannot open help page on browser: {}", e.getMessage());
			}
		}
		else
			LOGGER.warn(Backbone.MARKER_APPLICATION, "Cannot open help page on browser");
	}//GEN-LAST:event_hlpOnlineHelpMenuItemActionPerformed

	private void hlpIssueReporterMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hlpIssueReporterMenuItemActionPerformed
		if(Desktop.isDesktopSupported()){
			final Desktop desktop = Desktop.getDesktop();
			try{
				desktop.browse(new URI(URL_REPORT_ISSUE));
			}
			catch(final Exception e){
				LOGGER.warn(Backbone.MARKER_APPLICATION, "Cannot open issue page on browser: {}", e.getMessage());
			}
		}
		else
			LOGGER.warn(Backbone.MARKER_APPLICATION, "Cannot open issue page on browser");
	}//GEN-LAST:event_hlpIssueReporterMenuItemActionPerformed

	private void acoIncorrectTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_acoIncorrectTextFieldKeyReleased
		acoFilterDebouncer.call(this);
	}//GEN-LAST:event_acoIncorrectTextFieldKeyReleased

	private void acoCorrectTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_acoCorrectTextFieldKeyReleased
		acoFilterDebouncer.call(this);
	}//GEN-LAST:event_acoCorrectTextFieldKeyReleased

	private void acoAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_acoAddButtonActionPerformed
		try{
			//try adding the correction
			final String incorrect = acoIncorrectTextField.getText();
			final String correct = acoCorrectTextField.getText();
			final Supplier<Boolean> duplicatesDiscriminator = () -> {
				final int responseOption = JOptionPane.showConfirmDialog(this,
					"There is a duplicate with same incorrect and correct forms.\nForce insertion?", "Select one",
					JOptionPane.YES_NO_OPTION);
				return (responseOption == JOptionPane.YES_OPTION);
			};
			final DuplicationResult<CorrectionEntry> duplicationResult = backbone.getAcoParser()
				.insertCorrection(incorrect, correct, duplicatesDiscriminator);
			final List<CorrectionEntry> duplicates = duplicationResult.getDuplicates();

			if(duplicates.isEmpty() || duplicationResult.isForceInsertion()){
				//if everything's ok update the table and the sorter…
				final AutoCorrectTableModel dm = (AutoCorrectTableModel)acoTable.getModel();
				dm.fireTableDataChanged();

				formerFilterIncorrectText = null;
				formerFilterCorrectText = null;
				acoIncorrectTextField.setText(null);
				acoCorrectTextField.setText(null);
				acoAddButton.setEnabled(false);
				acoIncorrectTextField.requestFocusInWindow();
				@SuppressWarnings("unchecked")
				TableRowSorter<AutoCorrectTableModel> sorter = (TableRowSorter<AutoCorrectTableModel>)acoTable.getRowSorter();
				sorter.setRowFilter(null);

				updateSynonymsCounter();

				//… and save the files
				backbone.storeAutoCorrectFile();
			}
			else{
				acoIncorrectTextField.requestFocusInWindow();

				final String duplicatedWords = duplicates.stream()
					.map(CorrectionEntry::toString)
					.collect(Collectors.joining(", "));
				JOptionPane.showOptionDialog(this, "Some duplicates are present, namely:\n   "
						+ duplicatedWords + "\n\nSynonyms was NOT inserted!", "Warning!", JOptionPane.DEFAULT_OPTION,
					JOptionPane.WARNING_MESSAGE, null, null, null);
			}
		}
		catch(final Exception e){
			LOGGER.info(Backbone.MARKER_APPLICATION, "Insertion error: {}", e.getMessage());
		}
	}//GEN-LAST:event_acoAddButtonActionPerformed

	private void optionsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optionsButtonActionPerformed
		final Consumer<HyphenationOptionsParser> acceptButtonAction = (options) -> {
			try{
				backbone.getHypParser().setOptions(options);

				backbone.storeHyphenationFile();
			}
			catch(Exception ex){
				LOGGER.info(Backbone.MARKER_APPLICATION, ex.getMessage());
			}
		};
		final HyphenationOptionsDialog dialog = new HyphenationOptionsDialog(backbone.getHypParser().getOptions(),
			acceptButtonAction, this);
		GUIUtils.addCancelByEscapeKey(dialog);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}//GEN-LAST:event_optionsButtonActionPerformed

	private void openAidButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openAidButtonActionPerformed
		try{
			FileHelper.openFileWithChosenEditor(backbone.getAidFile());
		}
		catch(final IOException | InterruptedException e){
			LOGGER.warn("Exception while opening affix file", e);
		}
	}//GEN-LAST:event_openAidButtonActionPerformed

	private void openAffButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openAffButtonActionPerformed
		try{
			FileHelper.openFileWithChosenEditor(packager.getAffixFile());
		}
		catch(final IOException | InterruptedException e){
			LOGGER.warn("Exception while opening affix file", e);
		}
	}//GEN-LAST:event_openAffButtonActionPerformed

	private void openDicButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openDicButtonActionPerformed
		try{
			FileHelper.openFileWithChosenEditor(packager.getDictionaryFile());
		}
		catch(final IOException | InterruptedException e){
			LOGGER.warn("Exception while opening dictionary file", e);
		}
	}//GEN-LAST:event_openDicButtonActionPerformed

	private void openHypButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openHypButtonActionPerformed
		try{
			FileHelper.openFileWithChosenEditor(packager.getHyphenationFile());
		}
		catch(final IOException | InterruptedException e){
			LOGGER.warn("Exception while opening hyphenation file", e);
		}
	}//GEN-LAST:event_openHypButtonActionPerformed

	private void openAcoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openAcoButtonActionPerformed
		try{
			FileHelper.openFileWithChosenEditor(packager.getAutoCorrectFile());
		}
		catch(final IOException | InterruptedException e){
			LOGGER.warn("Exception while opening auto–correct file", e);
		}
	}//GEN-LAST:event_openAcoButtonActionPerformed

	private void openSexButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openSexButtonActionPerformed
		try{
			FileHelper.openFileWithChosenEditor(packager.getSentenceExceptionsFile());
		}
		catch(final IOException | InterruptedException e){
			LOGGER.warn("Exception while opening sentence exceptions file", e);
		}
	}//GEN-LAST:event_openSexButtonActionPerformed

	private void openWexButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openWexButtonActionPerformed
		try{
			FileHelper.openFileWithChosenEditor(packager.getWordExceptionsFile());
		}
		catch(final IOException | InterruptedException e){
			LOGGER.warn("Exception while opening word exceptions file", e);
		}
	}//GEN-LAST:event_openWexButtonActionPerformed

	private void sexTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_sexTextFieldKeyReleased
		sexFilterDebouncer.call(this);
	}//GEN-LAST:event_sexTextFieldKeyReleased

	private void wexTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_wexTextFieldKeyReleased
		wexFilterDebouncer.call(this);
	}//GEN-LAST:event_wexTextFieldKeyReleased

   private void sexAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sexAddButtonActionPerformed
		try{
			final String exception = StringUtils.strip(sexTextField.getText());
			if(!backbone.getSexParser().contains(exception)){
				backbone.getSexParser().modify(ExceptionsParser.TagChangeType.ADD, Collections.singletonList(exception));
				sexTagPanel.addTag(exception);

				//reset input
				sexTextField.setText(StringUtils.EMPTY);
				sexTagPanel.applyFilter(null);

				updateSentenceExceptionsCounter();

				backbone.storeSentenceExceptionFile();
			}
			else{
				sexTextField.requestFocusInWindow();

				JOptionPane.showOptionDialog(this,
					"A duplicate is already present", "Warning!", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, null,
					null);
			}
		}
		catch(final Exception e){
			LOGGER.info(Backbone.MARKER_APPLICATION, "Insertion error: {}", e.getMessage());
		}
   }//GEN-LAST:event_sexAddButtonActionPerformed

   private void wexAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wexAddButtonActionPerformed
		try{
			final String exception = StringUtils.strip(wexTextField.getText());
			if(!backbone.getWexParser().contains(exception)){
				backbone.getWexParser().modify(ExceptionsParser.TagChangeType.ADD, Collections.singletonList(exception));
				wexTagPanel.addTag(exception);

				//reset input
				wexTextField.setText(StringUtils.EMPTY);
				wexTagPanel.applyFilter(null);

				updateWordExceptionsCounter();

				backbone.storeWordExceptionFile();
			}
			else{
				wexTextField.requestFocusInWindow();

				JOptionPane.showOptionDialog(this,
					"A duplicate is already present", "Warning!", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, null,
					null);
			}
		}
		catch(final Exception e){
			LOGGER.info(Backbone.MARKER_APPLICATION, "Insertion error: {}", e.getMessage());
		}
   }//GEN-LAST:event_wexAddButtonActionPerformed

   private void hlpUpdateMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hlpUpdateMenuItemActionPerformed
		SwingUtilities.invokeLater(() -> {
			try{
				final FileDownloaderDialog dialog = new FileDownloaderDialog(this);
				GUIUtils.addCancelByEscapeKey(dialog);
				dialog.setLocationRelativeTo(this);
				dialog.setVisible(true);
			}
			catch(final NoRouteToHostException | UnknownHostException e){
				final String message = "Connection failed.\r\nPlease check network connection and try again.";
				LOGGER.warn(message);

				JOptionPane.showMessageDialog(this, message, "Application update", JOptionPane.WARNING_MESSAGE);
			}
			catch(final Exception e){
				final String message = e.getMessage();
				LOGGER.info(message);

				JOptionPane.showMessageDialog(this, message, "Application update", JOptionPane.INFORMATION_MESSAGE);
			}
		});
	}//GEN-LAST:event_hlpUpdateMenuItemActionPerformed

   private void hlpCheckUpdateOnStartupCheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hlpCheckUpdateOnStartupCheckBoxMenuItemActionPerformed
		final boolean selected = hlpCheckUpdateOnStartupCheckBoxMenuItem.isSelected();
		preferences.putBoolean(UPDATE_STARTUP_CHECK, selected);
   }//GEN-LAST:event_hlpCheckUpdateOnStartupCheckBoxMenuItemActionPerformed


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

				dicCheckCorrectnessMenuItem.setEnabled(true);
				LOGGER.info(Backbone.MARKER_APPLICATION, "Project loader aborted");

				prjLoaderWorker = null;
			}
			else if(answer == JOptionPane.NO_OPTION || answer == JOptionPane.CLOSED_OPTION){
				prjLoaderWorker.resume();

				setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			}
		}

		//FIXME introduce a checkAbortion case?
		if(dicDuplicatesWorker != null && dicDuplicatesWorker.getState() == SwingWorker.StateValue.STARTED){
//			dicDuplicatesWorker.pause();

			final Object[] options = {"Abort", "Cancel"};
			final int answer = JOptionPane.showOptionDialog(this,
				"Do you really want to abort the dictionary duplicates task?", "Warning!",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
			if(answer == JOptionPane.YES_OPTION){
				dicDuplicatesWorker.cancel(true);

				dicExtractDuplicatesMenuItem.setEnabled(true);
				LOGGER.info(Backbone.MARKER_APPLICATION, "Dictionary duplicate extraction aborted");

				dicDuplicatesWorker = null;
			}
			else if(answer == JOptionPane.NO_OPTION || answer == JOptionPane.CLOSED_OPTION){
//				dicDuplicatesWorker.resume();
			}
		}

		checkAbortion(dicCorrectnessWorker, dicCheckCorrectnessMenuItem);

		checkAbortion(dicWordCountWorker, dicWordCountMenuItem);

		checkAbortion(dicStatisticsWorker, dicStatisticsMenuItem, hypStatisticsMenuItem);

		checkAbortion(dicWordlistWorker, dicExtractWordlistMenuItem, dicExtractWordlistPlainTextMenuItem);

		checkAbortion(dicPoSFSAWorker, dicExtractPoSFAMenuItem);

		checkAbortion(compoundRulesExtractorWorker, cmpInputComboBox, cmpLimitComboBox, cmpInputTextArea,
			cmpLoadInputButton);

		checkAbortion(hypCorrectnessWorker, hypCheckCorrectnessMenuItem);
	}

	private void checkAbortion(final WorkerDictionaryBase worker, final JComponent ... componentsToEnable){
		if(worker != null && worker.getState() == SwingWorker.StateValue.STARTED){
			final Runnable cancelTask = () -> {
				for(final JComponent component : componentsToEnable)
					component.setEnabled(true);
			};
//			final Runnable resumeTask = () -> setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			worker.askUserToAbort(this, cancelTask, null);
		}
	}

	private void loadFile(final Path basePath){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		clearResultTextArea();

		if(backbone != null)
			backbone.stopFileListener();

		loadFileInternal(basePath);
	}

	@Override
	public void loadFileInternal(final Path projectPath){
		//clear all
		loadFileCancelled(null);

		if(prjLoaderWorker == null || prjLoaderWorker.isDone()){
			dicCheckCorrectnessMenuItem.setEnabled(false);

			try{
				packager = new Packager(projectPath != null? projectPath: packager.getProjectPath());
				final List<String> availableLanguages = packager.getAvailableLanguages();
				final AtomicReference<String> language = new AtomicReference<>(availableLanguages.get(0));
				if(availableLanguages.size() > 1){
					//choose between available languages
					final Consumer<String> onSelection = language::set;
					final LanguageChooserDialog dialog = new LanguageChooserDialog(availableLanguages, onSelection, this);
					GUIUtils.addCancelByEscapeKey(dialog);
					dialog.setLocationRelativeTo(this);
					dialog.setVisible(true);
				}
				//load appropriate files based on current language
				packager.extractConfigurationFolders(language.get());

				setTitle("HunLinter : " + packager.getAffixFile().getName() + " (" + packager.getLanguage() + ")");

				temporarilyChooseAFont(packager.getAffixFile().toPath());

				backbone = new Backbone(packager, this);

				prjLoaderWorker = new ProjectLoaderWorker(packager, backbone, this::loadFileCompleted, this::loadFileCancelled);
				prjLoaderWorker.addPropertyChangeListener(this);
				prjLoaderWorker.execute();

				filOpenProjectMenuItem.setEnabled(false);
			}
			catch(final IOException | SAXException | ProjectNotFoundException e){
				loadFileCancelled(e);

				LOGGER.error(Backbone.MARKER_APPLICATION, e.getMessage());

				LOGGER.error("A bad error occurred while loading the project", e);
			}
		}
	}

	/** Chooses one font (in case of reading errors) */
	private void temporarilyChooseAFont(final Path basePath){
		try{
			final String content = new String(Files.readAllBytes(basePath));
			final String[] extractions = PatternHelper.extract(content, EXTRACTOR, 10);
			final String sample = String.join(StringUtils.EMPTY, String.join(StringUtils.EMPTY, extractions).chars().mapToObj(Character::toString).collect(Collectors.toSet()));
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
		hypSyllabesCountOutputLabel.setFont(currentFont);
		hypAddRuleTextField.setFont(currentFont);
		hypAddRuleSyllabationOutputLabel.setFont(currentFont);

		acoIncorrectTextField.setFont(currentFont);
		acoCorrectTextField.setFont(currentFont);
		acoTable.setFont(currentFont);

		sexTagPanel.setFont(currentFont);

		wexTagPanel.setFont(currentFont);
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

	private void loadFileCompleted(){
		//restore default font (changed for reporting reading errors)
		setCurrentFont();

		backbone.registerFileListener();
		backbone.startFileListener();

		final String language = backbone.getAffixData().getLanguage();

		final Comparator<String> comparator = Comparator.comparingInt(String::length)
			.thenComparing(BaseBuilder.getComparator(language));
		final Comparator<AffixEntry> comparatorAffix = Comparator.comparingInt((AffixEntry entry) -> entry.toString().length())
			.thenComparing((entry0, entry1) -> BaseBuilder.getComparator(language).compare(entry0.toString(), entry1.toString()));
		addSorterToTable(dicTable, comparator, comparatorAffix);

		try{
			filOpenProjectMenuItem.setEnabled(true);
			filCreatePackageMenuItem.setEnabled(true);
			filFontMenuItem.setEnabled(true);
			dicCheckCorrectnessMenuItem.setEnabled(true);
			dicSortDictionaryMenuItem.setEnabled(true);
			dicMenu.setEnabled(true);
			setTabbedPaneEnable(mainTabbedPane, dicLayeredPane, true);
			final AffixData affixData = backbone.getAffixData();
			final Set<String> compoundRules = affixData.getCompoundRules();
			setTabbedPaneEnable(mainTabbedPane, cmpLayeredPane, !compoundRules.isEmpty());


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
			openAffButton.setEnabled(backbone.getAffixFile() != null);
			openDicButton.setEnabled(backbone.getDictionaryFile() != null);

			if(rulesReducerDialog != null)
				//notify RulesReducerDialog
				rulesReducerDialog.reload();


			//hyphenation file:
			if(backbone.getHyphenator() != null){
				hypCheckCorrectnessMenuItem.setEnabled(true);

				hypMenu.setEnabled(true);
				hypStatisticsMenuItem.setEnabled(true);
				setTabbedPaneEnable(mainTabbedPane, hypLayeredPane, true);
			}
			openHypButton.setEnabled(backbone.getHyphenationFile() != null);


			//aid file:
			final List<String> lines = backbone.getAidParser().getLines();
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
			if(backbone.getTheParser().getSynonymsCount() > 0){
				addSorterToTable(theTable, comparator, null);

				final ThesaurusTableModel dm = (ThesaurusTableModel)theTable.getModel();
				dm.setSynonyms(backbone.getTheParser().getSynonymsDictionary());
				updateSynonymsCounter();
				setTabbedPaneEnable(mainTabbedPane, theLayeredPane, true);
			}


			//auto–correct file:
			if(backbone.getAcoParser().getCorrectionsCounter() > 0){
				addSorterToTable(acoTable, comparator, null);

				final AutoCorrectTableModel dm = (AutoCorrectTableModel)acoTable.getModel();
				dm.setCorrections(backbone.getAcoParser().getCorrectionsDictionary());
				updateCorrectionsCounter();
				setTabbedPaneEnable(mainTabbedPane, acoLayeredPane, true);
			}
			openAcoButton.setEnabled(backbone.getAutoCorrectFile() != null);


			//sentence exceptions file:
			if(backbone.getSexParser().getExceptionsCounter() > 0){
				updateSentenceExceptionsCounter();
				final List<String> sentenceExceptions = backbone.getSexParser().getExceptionsDictionary();
				sexTagPanel.initializeTags(sentenceExceptions);
				setTabbedPaneEnable(mainTabbedPane, sexLayeredPane, true);
			}
			openSexButton.setEnabled(backbone.getSentenceExceptionsFile() != null);


			//word exceptions file:
			if(backbone.getWexParser().getExceptionsCounter() > 0){
				final List<String> wordExceptions = backbone.getWexParser().getExceptionsDictionary();
				wexTagPanel.initializeTags(wordExceptions);
				updateWordExceptionsCounter();
				setTabbedPaneEnable(mainTabbedPane, wexLayeredPane, true);
			}
			openWexButton.setEnabled(backbone.getWordExceptionsFile() != null);


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
			LOGGER.info(Backbone.MARKER_APPLICATION, e.getMessage());
		}
		catch(final Exception e){
			LOGGER.info(Backbone.MARKER_APPLICATION, "A bad error occurred: {}", e.getMessage());

			LOGGER.error("A bad error occurred", e);
		}
	}

	private void loadFileCancelled(final Exception exc){
		//menu
		filOpenProjectMenuItem.setEnabled(true);
		filCreatePackageMenuItem.setEnabled(false);
		filFontMenuItem.setEnabled(false);
		dicMenu.setEnabled(false);
		if((exc instanceof ProjectNotFoundException)){
			//remove the file from the recent projects menu
			recentProjectsMenu.removeEntry(((ProjectNotFoundException) exc).getProjectPath().toString());

			recentProjectsMenu.setEnabled(recentProjectsMenu.hasEntries());
			filEmptyRecentProjectsMenuItem.setEnabled(recentProjectsMenu.hasEntries());
		}
		dicCheckCorrectnessMenuItem.setEnabled(false);
		dicSortDictionaryMenuItem.setEnabled(false);
		hypCheckCorrectnessMenuItem.setEnabled(false);


		//affix file:
		cmpInputComboBox.removeAllItems();
		cmpInputComboBox.setEnabled(false);
		openAffButton.setEnabled(false);
		openDicButton.setEnabled(false);


		//hyphenation file:
		hypMenu.setEnabled(false);
		hypStatisticsMenuItem.setEnabled(false);
		setTabbedPaneEnable(mainTabbedPane, hypLayeredPane, false);
		openHypButton.setEnabled(false);


		//aid file:
		clearAidParser();
		//enable combo-box only if an AID file exists
		dicRuleFlagsAidComboBox.setEnabled(false);
		cmpRuleFlagsAidComboBox.setEnabled(false);
		openAidButton.setEnabled(false);


		//thesaurus file:
		setTabbedPaneEnable(mainTabbedPane, theLayeredPane, false);
		formerFilterThesaurusText = null;
		//noinspection unchecked
		((TableRowSorter<ThesaurusTableModel>)theTable.getRowSorter()).setRowFilter(null);


		//auto–correct file:
		setTabbedPaneEnable(mainTabbedPane, acoLayeredPane, false);
		openAcoButton.setEnabled(false);
		formerFilterIncorrectText = null;
		formerFilterCorrectText = null;
		//noinspection unchecked
		((TableRowSorter<AutoCorrectTableModel>)acoTable.getRowSorter()).setRowFilter(null);


		//sentence exceptions file:
		setTabbedPaneEnable(mainTabbedPane, sexLayeredPane, false);
		openSexButton.setEnabled(false);
		formerFilterSentenceException = null;
		sexTagPanel.applyFilter(null);


		//word exceptions file:
		setTabbedPaneEnable(mainTabbedPane, wexLayeredPane, false);
		openWexButton.setEnabled(false);
		formerFilterWordException = null;
		wexTagPanel.applyFilter(null);
	}

	private void updateSynonymsCounter(){
		theSynonymsRecordedOutputLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(backbone.getTheParser().getSynonymsCount()));
	}

	private void updateCorrectionsCounter(){
		acoCorrectionsRecordedOutputLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(backbone.getAcoParser().getCorrectionsCounter()));
	}

	private void updateSentenceExceptionsCounter(){
		sexCorrectionsRecordedOutputLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(backbone.getSexParser().getExceptionsCounter()));
	}

	private void updateWordExceptionsCounter(){
		wexCorrectionsRecordedOutputLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(backbone.getWexParser().getExceptionsCounter()));
	}

	private int setTabbedPaneEnable(final JTabbedPane tabbedPane, final Component component, final boolean enabled){
		final int index = tabbedPane.indexOfComponent(component);
		tabbedPane.setEnabledAt(index, enabled);
		return index;
	}


	private void hyphenate(final HunLinterFrame frame){
		final String language = frame.backbone.getAffixData().getLanguage();
		final Orthography orthography = BaseBuilder.getOrthography(language);
		String text = orthography.correctOrthography(frame.hypWordTextField.getText());
		if(formerHyphenationText != null && formerHyphenationText.equals(text))
			return;
		formerHyphenationText = text;

		String count = null;
		List<String> rules = Collections.emptyList();
		if(StringUtils.isNotBlank(text)){
			final Hyphenation hyphenation = frame.backbone.getHyphenator().hyphenate(text);

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

	private void hyphenateAddRule(final HunLinterFrame frame){
		final String language = frame.backbone.getAffixData().getLanguage();
		final Orthography orthography = BaseBuilder.getOrthography(language);
		String addedRuleText = orthography.correctOrthography(frame.hypWordTextField.getText());
		final String addedRule = orthography.correctOrthography(frame.hypAddRuleTextField.getText().toLowerCase(Locale.ROOT));
		final HyphenationParser.Level level = HyphenationParser.Level.values()[frame.hypAddRuleLevelComboBox.getSelectedIndex()];
		String addedRuleCount = null;
		if(StringUtils.isNotBlank(addedRule)){
			final boolean alreadyHasRule = frame.backbone.hasHyphenationRule(addedRule, level);
			boolean ruleMatchesText = false;
			boolean hyphenationChanged = false;
			boolean correctHyphenation = false;
			if(!alreadyHasRule){
				ruleMatchesText = addedRuleText.contains(PatternHelper.clear(addedRule,
					PATTERN_POINTS_AND_NUMBERS_AND_EQUALS_AND_MINUS));

				if(ruleMatchesText){
					final Hyphenation hyphenation = frame.backbone.getHyphenator().hyphenate(addedRuleText);
					final Hyphenation addedRuleHyphenation = frame.backbone.getHyphenator().hyphenate(addedRuleText, addedRule,
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


	private void checkDictionaryCorrectness(){
		if(dicCorrectnessWorker == null || dicCorrectnessWorker.isDone()){
			dicCheckCorrectnessMenuItem.setEnabled(false);

			dicCorrectnessWorker = new CorrectnessWorker(backbone.getDicParser(), backbone.getChecker(),
				backbone.getWordGenerator());
			dicCorrectnessWorker.addPropertyChangeListener(this);
			dicCorrectnessWorker.execute();
		}
	}

	private void extractDictionaryDuplicates(){
		if(dicDuplicatesWorker == null || dicDuplicatesWorker.isDone()){
			saveResultFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			final int fileChosen = saveResultFileChooser.showSaveDialog(this);
			if(fileChosen == JFileChooser.APPROVE_OPTION){
				dicExtractDuplicatesMenuItem.setEnabled(false);

				final File outputFile = saveResultFileChooser.getSelectedFile();
				dicDuplicatesWorker = new DuplicatesWorker(backbone.getAffixData().getLanguage(), backbone.getDicParser(),
					backbone.getWordGenerator(), outputFile);
				dicDuplicatesWorker.addPropertyChangeListener(this);
				dicDuplicatesWorker.execute();
			}
		}
	}

	private void extractWordCount(){
		if(dicWordCountWorker == null || dicWordCountWorker.isDone()){
			dicWordCountMenuItem.setEnabled(false);

			dicWordCountWorker = new WordCountWorker(backbone.getAffParser().getAffixData().getLanguage(),
				backbone.getDicParser(), backbone.getWordGenerator());
			dicWordCountWorker.addPropertyChangeListener(this);
			dicWordCountWorker.execute();
		}
	}

	private void extractDictionaryStatistics(final boolean performHyphenationStatistics){
		if(dicStatisticsWorker == null || dicStatisticsWorker.isDone()){
			if(performHyphenationStatistics)
				hypStatisticsMenuItem.setEnabled(false);
			else
				dicStatisticsMenuItem.setEnabled(false);

			dicStatisticsWorker = new StatisticsWorker(backbone.getAffParser(), backbone.getDicParser(),
				(performHyphenationStatistics? backbone.getHyphenator(): null), backbone.getWordGenerator(), this);
			dicStatisticsWorker.addPropertyChangeListener(this);
			dicStatisticsWorker.execute();
		}
	}

	private void extractDictionaryWordlist(final WordlistWorker.WorkerType type){
		if(dicWordlistWorker == null || dicWordlistWorker.isDone()){
			saveResultFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			final int fileChosen = saveResultFileChooser.showSaveDialog(this);
			if(fileChosen == JFileChooser.APPROVE_OPTION){
				dicExtractWordlistMenuItem.setEnabled(false);
				dicExtractWordlistPlainTextMenuItem.setEnabled(false);

				final File outputFile = saveResultFileChooser.getSelectedFile();
				dicWordlistWorker = new WordlistWorker(backbone.getDicParser(), backbone.getWordGenerator(), type,
					outputFile);
				dicWordlistWorker.addPropertyChangeListener(this);
				dicWordlistWorker.execute();
			}
		}
	}

	private void extractPoSFSA(){
		if(dicPoSFSAWorker == null || dicPoSFSAWorker.isDone()){
			saveResultFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			final int fileChosen = saveResultFileChooser.showSaveDialog(this);
			if(fileChosen == JFileChooser.APPROVE_OPTION){
				dicExtractPoSFAMenuItem.setEnabled(false);

				File outputFile = saveResultFileChooser.getSelectedFile();
				final String temporaryFile = outputFile.getAbsolutePath() + File.separatorChar
					+ backbone.getAffixData().getLanguage() + ".txt";
				outputFile = new File(temporaryFile);
				dicPoSFSAWorker = new PoSFSAWorker(backbone.getDicParser(), backbone.getWordGenerator(),
					outputFile);
				dicPoSFSAWorker.addPropertyChangeListener(this);
				dicPoSFSAWorker.execute();
			}
		}
	}

	private void extractMinimalPairs(){
		if(dicMinimalPairsWorker == null || dicMinimalPairsWorker.isDone()){
			saveResultFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			final int fileChosen = saveResultFileChooser.showSaveDialog(this);
			if(fileChosen == JFileChooser.APPROVE_OPTION){
				dicExtractMinimalPairsMenuItem.setEnabled(false);

				final File outputFile = saveResultFileChooser.getSelectedFile();
				dicMinimalPairsWorker = new MinimalPairsWorker(backbone.getAffixData().getLanguage(), backbone.getDicParser(),
					backbone.getChecker(), backbone.getWordGenerator(), outputFile);
				dicMinimalPairsWorker.addPropertyChangeListener(this);
				dicMinimalPairsWorker.execute();
			}
		}
	}

	private void extractCompoundRulesInputs(){
		if(compoundRulesExtractorWorker == null || compoundRulesExtractorWorker.isDone()){
			cmpInputComboBox.setEnabled(false);
			cmpLimitComboBox.setEnabled(false);
			cmpInputTextArea.setEnabled(false);
			cmpLoadInputButton.setEnabled(false);

			cmpInputTextArea.setText(null);

			final AffixParser affParser = backbone.getAffParser();
			final AffixData affixData = affParser.getAffixData();
			final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
			final String compoundFlag = affixData.getCompoundFlag();
			final List<Production> compounds = new ArrayList<>();
			final BiConsumer<Production, Integer> productionReader = (production, row) -> {
				if(!production.distributeByCompoundRule(affixData).isEmpty() || production.hasContinuationFlag(compoundFlag))
					compounds.add(production);
			};
			final Runnable completed = () -> {
				final StringJoiner sj = new StringJoiner("\n");
				compounds.forEach(compound -> sj.add(compound.toString(strategy)));
				cmpInputTextArea.setText(sj.toString());
				cmpInputTextArea.setCaretPosition(0);
			};
			compoundRulesExtractorWorker = new CompoundRulesWorker(backbone.getDicParser(), backbone.getWordGenerator(),
				productionReader, completed);
			compoundRulesExtractorWorker.addPropertyChangeListener(this);
			compoundRulesExtractorWorker.execute();
		}
	}


	private void checkHyphenationCorrectness(){
		if(hypCorrectnessWorker == null || hypCorrectnessWorker.isDone()){
			hypCorrectnessWorker = new HyphenationCorrectnessWorker(backbone.getAffParser().getAffixData().getLanguage(),
				backbone.getDicParser(), backbone.getHyphenator(), backbone.getWordGenerator());
			hypCorrectnessWorker.addPropertyChangeListener(this);
			hypCorrectnessWorker.execute();

			hypCheckCorrectnessMenuItem.setEnabled(false);
		}
	}


	@Override
	public void clearAffixParser(){
		clearDictionaryParser();


		if(rulesReducerDialog != null)
			//update rule reduced dialog
			rulesReducerDialog.reload();
	}

	@Override
	public void clearHyphenationParser(){
		clearHyphenationFields();

		hypMenu.setEnabled(false);
		hypStatisticsMenuItem.setEnabled(false);
		setTabbedPaneEnable(mainTabbedPane, hypLayeredPane, false);
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

		setTabbedPaneEnable(mainTabbedPane, dicLayeredPane, false);
		setTabbedPaneEnable(mainTabbedPane, cmpLayeredPane, false);

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
	}

	private void clearDictionaryCompoundFields(){
		formerCompoundInputText = null;

		cmpInputComboBox.setEnabled(true);
		cmpInputTextArea.setText(null);
		cmpInputTextArea.setEnabled(true);
		cmpLoadInputButton.setEnabled(true);
	}

	public void clearOutputTable(JTable table){
		final HunLinterTableModel<?> dm = (HunLinterTableModel<?>)table.getModel();
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

		setTabbedPaneEnable(mainTabbedPane, theLayeredPane, false);
	}

	@Override
	public void clearAutoCorrectParser(){
		final AutoCorrectTableModel dm = (AutoCorrectTableModel)acoTable.getModel();
		dm.setCorrections(null);

		setTabbedPaneEnable(mainTabbedPane, acoLayeredPane, false);
	}

	@Override
	public void clearSentenceExceptionsParser(){
		sexTagPanel.initializeTags(null);

		setTabbedPaneEnable(mainTabbedPane, sexLayeredPane, false);
	}

	@Override
	public void clearWordExceptionsParser(){
		wexTagPanel.initializeTags(null);

		setTabbedPaneEnable(mainTabbedPane, wexLayeredPane, false);
	}

	@Override
	public void clearAutoTextParser(){
//		final AutoTextTableModel dm = (AutoTextTableModel)atxTable.getModel();
//		dm.setCorrections(null);

		//FIXME
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
					final Runnable menuItemEnabler = enableComponentFromWorker.get(((WorkerBase<?, ?>)evt.getSource()).getWorkerName());
					if(menuItemEnabler != null)
						menuItemEnabler.run();
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


	public static void main(String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e){
			LOGGER.error(null, e);
		}

		//create and display the form
		EventQueue.invokeLater(() -> (new HunLinterFrame()).setVisible(true));
	}

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JButton acoAddButton;
   private javax.swing.JLabel acoCorrectLabel;
   private javax.swing.JTextField acoCorrectTextField;
   private javax.swing.JLabel acoCorrectionsRecordedLabel;
   private javax.swing.JLabel acoCorrectionsRecordedOutputLabel;
   private javax.swing.JLabel acoIncorrectLabel;
   private javax.swing.JTextField acoIncorrectTextField;
   private javax.swing.JLayeredPane acoLayeredPane;
   private javax.swing.JScrollPane acoScrollPane;
   private javax.swing.JTable acoTable;
   private javax.swing.JLabel acoToLabel;
   private javax.swing.JComboBox<String> cmpInputComboBox;
   private javax.swing.JLabel cmpInputLabel;
   private javax.swing.JScrollPane cmpInputScrollPane;
   private javax.swing.JTextArea cmpInputTextArea;
   private javax.swing.JLayeredPane cmpLayeredPane;
   private javax.swing.JComboBox<String> cmpLimitComboBox;
   private javax.swing.JLabel cmpLimitLabel;
   private javax.swing.JButton cmpLoadInputButton;
   private javax.swing.JComboBox<String> cmpRuleFlagsAidComboBox;
   private javax.swing.JLabel cmpRuleFlagsAidLabel;
   private javax.swing.JScrollPane cmpScrollPane;
   private javax.swing.JTable cmpTable;
   private javax.swing.JMenuItem dicCheckCorrectnessMenuItem;
   private javax.swing.JPopupMenu.Separator dicDuplicatesSeparator;
   private javax.swing.JMenuItem dicExtractDuplicatesMenuItem;
   private javax.swing.JMenuItem dicExtractMinimalPairsMenuItem;
   private javax.swing.JMenuItem dicExtractPoSFAMenuItem;
   private javax.swing.JMenuItem dicExtractWordlistMenuItem;
   private javax.swing.JMenuItem dicExtractWordlistPlainTextMenuItem;
   private javax.swing.JLabel dicInputLabel;
   private javax.swing.JTextField dicInputTextField;
   private javax.swing.JLayeredPane dicLayeredPane;
   private javax.swing.JMenu dicMenu;
   private javax.swing.JComboBox<String> dicRuleFlagsAidComboBox;
   private javax.swing.JLabel dicRuleFlagsAidLabel;
   private javax.swing.JMenuItem dicRulesReducerMenuItem;
   private javax.swing.JScrollPane dicScrollPane;
   private javax.swing.JMenuItem dicSortDictionaryMenuItem;
   private javax.swing.JMenuItem dicStatisticsMenuItem;
   private javax.swing.JPopupMenu.Separator dicStatisticsSeparator;
   private javax.swing.JTable dicTable;
   private javax.swing.JLabel dicTotalProductionsLabel;
   private javax.swing.JLabel dicTotalProductionsOutputLabel;
   private javax.swing.JMenuItem dicWordCountMenuItem;
   private javax.swing.JMenuItem filCreatePackageMenuItem;
   private javax.swing.JMenuItem filEmptyRecentProjectsMenuItem;
   private javax.swing.JMenuItem filExitMenuItem;
   private javax.swing.JMenuItem filFontMenuItem;
   private javax.swing.JPopupMenu.Separator filFontSeparator;
   private javax.swing.JMenu filMenu;
   private javax.swing.JMenuItem filOpenProjectMenuItem;
   private javax.swing.JPopupMenu.Separator filRecentProjectsSeparator;
   private javax.swing.JPopupMenu.Separator filSeparator;
   private javax.swing.JMenuItem hlpAboutMenuItem;
   private javax.swing.JCheckBoxMenuItem hlpCheckUpdateOnStartupCheckBoxMenuItem;
   private javax.swing.JMenuItem hlpIssueReporterMenuItem;
   private javax.swing.JMenu hlpMenu;
   private javax.swing.JMenuItem hlpOnlineHelpMenuItem;
   private javax.swing.JPopupMenu.Separator hlpOnlineSeparator;
   private javax.swing.JMenuItem hlpUpdateMenuItem;
   private javax.swing.JPopupMenu.Separator hlpUpdateSeparator;
   private javax.swing.JButton hypAddRuleButton;
   private javax.swing.JLabel hypAddRuleLabel;
   private javax.swing.JComboBox<String> hypAddRuleLevelComboBox;
   private javax.swing.JLabel hypAddRuleSyllabationLabel;
   private javax.swing.JLabel hypAddRuleSyllabationOutputLabel;
   private javax.swing.JLabel hypAddRuleSyllabesCountLabel;
   private javax.swing.JLabel hypAddRuleSyllabesCountOutputLabel;
   private javax.swing.JTextField hypAddRuleTextField;
   private javax.swing.JMenuItem hypCheckCorrectnessMenuItem;
   private javax.swing.JPopupMenu.Separator hypDuplicatesSeparator;
   private javax.swing.JLayeredPane hypLayeredPane;
   private javax.swing.JMenu hypMenu;
   private javax.swing.JLabel hypRulesLabel;
   private javax.swing.JLabel hypRulesOutputLabel;
   private javax.swing.JMenuItem hypStatisticsMenuItem;
   private javax.swing.JLabel hypSyllabationLabel;
   private javax.swing.JLabel hypSyllabationOutputLabel;
   private javax.swing.JLabel hypSyllabesCountLabel;
   private javax.swing.JLabel hypSyllabesCountOutputLabel;
   private javax.swing.JLabel hypWordLabel;
   private javax.swing.JTextField hypWordTextField;
   private javax.swing.JMenuBar mainMenuBar;
   private javax.swing.JProgressBar mainProgressBar;
   private javax.swing.JTabbedPane mainTabbedPane;
   private javax.swing.JButton openAcoButton;
   private javax.swing.JButton openAffButton;
   private javax.swing.JButton openAidButton;
   private javax.swing.JButton openDicButton;
   private javax.swing.JButton openHypButton;
   private javax.swing.JButton openSexButton;
   private javax.swing.JButton openWexButton;
   private javax.swing.JButton optionsButton;
   private javax.swing.JScrollPane parsingResultScrollPane;
   private javax.swing.JTextArea parsingResultTextArea;
   private javax.swing.JButton sexAddButton;
   private javax.swing.JLabel sexCorrectionsRecordedLabel;
   private javax.swing.JLabel sexCorrectionsRecordedOutputLabel;
   private javax.swing.JLabel sexInputLabel;
   private javax.swing.JLayeredPane sexLayeredPane;
   private javax.swing.JScrollPane sexScrollPane;
   private unit731.hunlinter.gui.JTagPanel sexTagPanel;
   private javax.swing.JTextField sexTextField;
   private javax.swing.JButton theAddButton;
   private javax.swing.JLayeredPane theLayeredPane;
   private javax.swing.JScrollPane theScrollPane;
   private javax.swing.JLabel theSynonymsLabel;
   private javax.swing.JLabel theSynonymsRecordedLabel;
   private javax.swing.JLabel theSynonymsRecordedOutputLabel;
   private javax.swing.JTextField theSynonymsTextField;
   private javax.swing.JTable theTable;
   private javax.swing.JButton wexAddButton;
   private javax.swing.JLabel wexCorrectionsRecordedLabel;
   private javax.swing.JLabel wexCorrectionsRecordedOutputLabel;
   private javax.swing.JLabel wexInputLabel;
   private javax.swing.JLayeredPane wexLayeredPane;
   private javax.swing.JScrollPane wexScrollPane;
   private unit731.hunlinter.gui.JTagPanel wexTagPanel;
   private javax.swing.JTextField wexTextField;
   // End of variables declaration//GEN-END:variables

}
