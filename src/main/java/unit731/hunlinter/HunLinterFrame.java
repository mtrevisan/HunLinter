package unit731.hunlinter;

import java.awt.*;

import org.apache.commons.lang3.tuple.Pair;
import org.xml.sax.SAXException;
import unit731.hunlinter.actions.AboutAction;
import unit731.hunlinter.actions.AffixRulesReducerAction;
import unit731.hunlinter.actions.CheckUpdateOnStartupAction;
import unit731.hunlinter.actions.CreatePackageAction;
import unit731.hunlinter.actions.DictionaryExtractDuplicatesAction;
import unit731.hunlinter.actions.DictionaryExtractMinimalPairsAction;
import unit731.hunlinter.actions.DictionaryExtractPosFSAAction;
import unit731.hunlinter.actions.DictionaryExtractWordlistAction;
import unit731.hunlinter.actions.DictionaryHyphenationStatisticsAction;
import unit731.hunlinter.actions.DictionarySorterAction;
import unit731.hunlinter.actions.DictionaryWordCountAction;
import unit731.hunlinter.actions.ExitAction;
import unit731.hunlinter.actions.HyphenationLinterAction;
import unit731.hunlinter.actions.IssueReporterAction;
import unit731.hunlinter.actions.OnlineHelpAction;
import unit731.hunlinter.actions.OpenFileAction;
import unit731.hunlinter.actions.SelectFontAction;
import unit731.hunlinter.actions.ThesaurusLinterAction;
import unit731.hunlinter.actions.UpdateAction;
import unit731.hunlinter.gui.AscendingDescendingUnsortedTableRowSorter;
import unit731.hunlinter.gui.AutoCorrectTableModel;
import unit731.hunlinter.gui.JCopyableTable;
import unit731.hunlinter.gui.JTagPanel;
import unit731.hunlinter.gui.ProjectFolderFilter;
import unit731.hunlinter.interfaces.HunLintable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.DefaultCaret;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.actions.DictionaryLinterAction;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.gui.RecentFilesMenu;
import unit731.hunlinter.gui.TableRenderer;
import unit731.hunlinter.languages.Orthography;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.autocorrect.AutoCorrectParser;
import unit731.hunlinter.parsers.autocorrect.CorrectionEntry;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.exceptions.ExceptionsParser;
import unit731.hunlinter.parsers.hyphenation.HyphenationOptionsParser;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.exceptions.LanguageNotChosenException;
import unit731.hunlinter.workers.exceptions.ProjectNotFoundException;
import unit731.hunlinter.workers.ProjectLoaderWorker;
import unit731.hunlinter.workers.dictionary.WordlistWorker;
import unit731.hunlinter.workers.core.WorkerAbstract;
import unit731.hunlinter.parsers.thesaurus.DuplicationResult;
import unit731.hunlinter.parsers.hyphenation.Hyphenation;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;
import unit731.hunlinter.services.downloader.DownloaderHelper;
import unit731.hunlinter.services.system.JavaHelper;
import unit731.hunlinter.services.text.StringHelper;
import unit731.hunlinter.services.log.ApplicationLogAppender;
import unit731.hunlinter.services.system.Debouncer;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.PatternHelper;
import unit731.hunlinter.services.RecentItems;


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

	private static final Pattern EXTRACTOR = PatternHelper.pattern("(?:TRY |FX [^ ]+ )([^\r\n\\d]+)[\r\n]+");

	private final static String FONT_FAMILY_NAME_PREFIX = "font.familyName.";
	private final static String FONT_SIZE_PREFIX = "font.size.";
	private final static String UPDATE_STARTUP_CHECK = "update.startupCheck";

	private static final int DEBOUNCER_INTERVAL = 600;
	private static final Pattern PATTERN_POINTS_AND_NUMBERS_AND_EQUALS_AND_MINUS = PatternHelper.pattern("[.\\d=-]");

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
	private final Debouncer<HunLinterFrame> hypDebouncer = new Debouncer<>(this::hyphenate, DEBOUNCER_INTERVAL);
	private final Debouncer<HunLinterFrame> hypAddRuleDebouncer = new Debouncer<>(this::hyphenateAddRule, DEBOUNCER_INTERVAL);
	private final Debouncer<HunLinterFrame> acoFilterDebouncer = new Debouncer<>(this::filterAutoCorrect, DEBOUNCER_INTERVAL);
	private final Debouncer<HunLinterFrame> sexFilterDebouncer = new Debouncer<>(this::filterSentenceExceptions, DEBOUNCER_INTERVAL);
	private final Debouncer<HunLinterFrame> wexFilterDebouncer = new Debouncer<>(this::filterWordExceptions, DEBOUNCER_INTERVAL);

	private ProjectLoaderWorker prjLoaderWorker;
	private final WorkerManager workerManager;


	public HunLinterFrame(){
		packager = new Packager();
		parserManager = new ParserManager(packager, this);
		workerManager = new WorkerManager(parserManager, this);


		initComponents();


		recentProjectsMenu.setEnabled(recentProjectsMenu.hasEntries());
		filEmptyRecentProjectsMenuItem.setEnabled(recentProjectsMenu.hasEntries());

		//add "fontable" property
		GUIUtils.addFontableProperty(
			parsingResultTextArea,
			hypWordTextField, hypAddRuleTextField, hypSyllabationOutputLabel, hypRulesOutputLabel, hypAddRuleSyllabationOutputLabel,
			acoTable, acoIncorrectTextField, acoCorrectTextField,
			sexTextField,
			wexTextField);

		GUIUtils.addUndoManager(
			hypWordTextField, hypAddRuleTextField,
			acoIncorrectTextField, acoCorrectTextField,
			sexTextField,
			wexTextField);

		try{
			final int iconSize = hypRulesOutputLabel.getHeight();
			final JPopupMenu copyPopupMenu = new JPopupMenu();
			copyPopupMenu.add(GUIUtils.createPopupCopyMenu(iconSize, copyPopupMenu, GUIUtils::copyCallback));
			GUIUtils.addPopupMenu(copyPopupMenu,
				hypSyllabationOutputLabel, hypRulesOutputLabel, hypAddRuleSyllabationOutputLabel);
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

      parsingResultScrollPane = new javax.swing.JScrollPane();
      parsingResultTextArea = new javax.swing.JTextArea();
      mainProgressBar = new javax.swing.JProgressBar();
      mainTabbedPane = new javax.swing.JTabbedPane();
      dicLayeredPane = new DictionaryLayeredPane(packager, parserManager);
      cmpLayeredPane = new CompoundsLayeredPane(packager, parserManager, workerManager, this);
      theLayeredPane = new ThesaurusLayeredPane(parserManager);
      hypLayeredPane = new javax.swing.JLayeredPane();
      hypWordLabel = new javax.swing.JLabel();
      hypWordTextField = new javax.swing.JTextField();
      hypSyllabationLabel = new javax.swing.JLabel();
      hypSyllabationOutputLabel = new javax.swing.JLabel();
      hypSyllabesCountLabel = new javax.swing.JLabel();
      hypSyllabesCountOutputLabel = new javax.swing.JLabel();
      hypRulesLabel = new javax.swing.JLabel();
      hypRulesOutputLabel = new javax.swing.JLabel();
      hypAddRuleLabel = new javax.swing.JLabel();
      hypAddRuleTextField = new javax.swing.JTextField();
      hypAddRuleLevelComboBox = new javax.swing.JComboBox<>();
      hypAddRuleButton = new javax.swing.JButton();
      hypAddRuleSyllabationLabel = new javax.swing.JLabel();
      hypAddRuleSyllabationOutputLabel = new javax.swing.JLabel();
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
         final ExceptionsParser sexParser = parserManager.getSexParser();
         sexParser.modify(changeType, tags);
         try{
            sexParser.save(packager.getSentenceExceptionsFile());
         }
         catch(final TransformerException e){
            LOGGER.info(ParserManager.MARKER_APPLICATION, e.getMessage());
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
         final ExceptionsParser wexParser = parserManager.getWexParser();
         wexParser.modify(changeType, tags);
         try{
            wexParser.save(packager.getWordExceptionsFile());
         }
         catch(final TransformerException e){
            LOGGER.info(ParserManager.MARKER_APPLICATION, e.getMessage());
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
      dicLinterMenuItem = new javax.swing.JMenuItem();
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
      theMenu = new javax.swing.JMenu();
      theLinterMenuItem = new javax.swing.JMenuItem();
      hypMenu = new javax.swing.JMenu();
      hypLinterMenuItem = new javax.swing.JMenuItem();
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
      setTitle((String)DownloaderHelper.getApplicationProperties().get(DownloaderHelper.PROPERTY_KEY_ARTIFACT_ID));
      setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png")));
      setMinimumSize(new java.awt.Dimension(964, 534));

      parsingResultTextArea.setEditable(false);
      parsingResultTextArea.setColumns(20);
      parsingResultTextArea.setRows(1);
      parsingResultTextArea.setTabSize(3);
      DefaultCaret caret = (DefaultCaret)parsingResultTextArea.getCaret();
      caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
      parsingResultScrollPane.setViewportView(parsingResultTextArea);

      mainTabbedPane.addTab("Dictionary", dicLayeredPane);
      mainTabbedPane.addTab("Compounds", cmpLayeredPane);
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
      hypSyllabationLabel.setPreferredSize(new java.awt.Dimension(58, 17));

      hypSyllabationOutputLabel.setText("…");
      hypSyllabationOutputLabel.setPreferredSize(new java.awt.Dimension(9, 17));

      hypSyllabesCountLabel.setLabelFor(hypSyllabesCountOutputLabel);
      hypSyllabesCountLabel.setText("Syllabes:");

      hypSyllabesCountOutputLabel.setText("…");

      hypRulesLabel.setLabelFor(hypRulesOutputLabel);
      hypRulesLabel.setText("Rules:");
      hypRulesLabel.setPreferredSize(new java.awt.Dimension(31, 17));

      hypRulesOutputLabel.setText("…");
      hypRulesOutputLabel.setPreferredSize(new java.awt.Dimension(9, 17));

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
      hypAddRuleSyllabationLabel.setPreferredSize(new java.awt.Dimension(81, 17));

      hypAddRuleSyllabationOutputLabel.setText("…");
      hypAddRuleSyllabationOutputLabel.setPreferredSize(new java.awt.Dimension(9, 17));

      hypAddRuleSyllabesCountLabel.setLabelFor(hypAddRuleSyllabesCountOutputLabel);
      hypAddRuleSyllabesCountLabel.setText("New syllabes:");

      hypAddRuleSyllabesCountOutputLabel.setText("…");

      optionsButton.setText("Options");
      optionsButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            optionsButtonActionPerformed(evt);
         }
      });

      openHypButton.setAction(new OpenFileAction(Packager.KEY_FILE_HYPHENATION, packager));
      openHypButton.setText("Open Hyphenation");
      openHypButton.setEnabled(false);

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
                  .addComponent(hypWordTextField))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, hypLayeredPaneLayout.createSequentialGroup()
                  .addComponent(hypSyllabationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypSyllabationOutputLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 845, javax.swing.GroupLayout.PREFERRED_SIZE))
               .addGroup(hypLayeredPaneLayout.createSequentialGroup()
                  .addComponent(hypAddRuleLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypAddRuleTextField)
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
                  .addComponent(hypAddRuleSyllabationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypAddRuleSyllabationOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, hypLayeredPaneLayout.createSequentialGroup()
                  .addComponent(hypRulesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypRulesOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(hypLayeredPaneLayout.createSequentialGroup()
                  .addComponent(optionsButton)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addComponent(openHypButton))
               .addGroup(hypLayeredPaneLayout.createSequentialGroup()
                  .addComponent(hypSyllabesCountLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypSyllabesCountOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
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
               .addComponent(hypSyllabationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(hypSyllabationOutputLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(hypSyllabesCountLabel)
               .addComponent(hypSyllabesCountOutputLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypRulesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(hypRulesOutputLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(18, 18, 18)
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypAddRuleLabel)
               .addComponent(hypAddRuleTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(hypAddRuleButton)
               .addComponent(hypAddRuleLevelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypAddRuleSyllabationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(hypAddRuleSyllabationOutputLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypAddRuleSyllabesCountLabel)
               .addComponent(hypAddRuleSyllabesCountOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 26, Short.MAX_VALUE)
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
      KeyStroke cancelKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
      acoTable.registerKeyboardAction(event -> removeSelectedRowsFromAutoCorrect(), cancelKeyStroke, JComponent.WHEN_FOCUSED);
      KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, false);
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
                     parserManager.getAcoParser().setCorrection(row, incorrect, correct);

                     //… and save the files
                     parserManager.storeAutoCorrectFile();
                  }
                  catch(Exception ex){
                     LOGGER.info(ParserManager.MARKER_APPLICATION, ex.getMessage());
                  }
               };
               final CorrectionEntry definition = parserManager.getAcoParser().getCorrectionsDictionary().get(row);
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

      openAcoButton.setAction(new OpenFileAction(Packager.KEY_FILE_AUTO_CORRECT, packager));
      openAcoButton.setText("Open AutoCorrect");
      openAcoButton.setEnabled(false);

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
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
                  .addComponent(acoCorrectionsRecordedOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

      openSexButton.setAction(new OpenFileAction(Packager.KEY_FILE_SENTENCE_EXCEPTIONS, packager));
      openSexButton.setText("Open Sentence Exceptions");
      openSexButton.setEnabled(false);

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

      openWexButton.setAction(new OpenFileAction(Packager.KEY_FILE_WORD_EXCEPTIONS, packager));
      openWexButton.setText("Open Word Exceptions");
      openWexButton.setEnabled(false);

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
            filExitMenuItem.getAction().actionPerformed(null);
         }
      });

      filMenu.setMnemonic('F');
      filMenu.setText("File");

      filOpenProjectMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/file_open.png"))); // NOI18N
      filOpenProjectMenuItem.setMnemonic('O');
      filOpenProjectMenuItem.setText("Open Project…");
      filOpenProjectMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            filOpenProjectMenuItemActionPerformed(evt);
         }
      });
      filMenu.add(filOpenProjectMenuItem);

      filCreatePackageMenuItem.setAction(new CreatePackageAction(parserManager));
      filCreatePackageMenuItem.setMnemonic('p');
      filCreatePackageMenuItem.setText("Create package");
      filCreatePackageMenuItem.setEnabled(false);
      filMenu.add(filCreatePackageMenuItem);
      filMenu.add(filFontSeparator);

      filFontMenuItem.setAction(new SelectFontAction(parserManager, preferences, this));
      filFontMenuItem.setMnemonic('f');
      filFontMenuItem.setText("Select font…");
      filFontMenuItem.setEnabled(false);
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

      filExitMenuItem.setAction(new ExitAction(this));
      filExitMenuItem.setMnemonic('x');
      filExitMenuItem.setText("Exit");
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
      dicMenu.setEnabled(false);

      dicLinterMenuItem.setAction(new DictionaryLinterAction(workerManager, this));
      dicLinterMenuItem.setMnemonic('c');
      dicLinterMenuItem.setText("Check correctness");
      dicLinterMenuItem.setToolTipText("");
      dicMenu.add(dicLinterMenuItem);

      dicSortDictionaryMenuItem.setAction(new DictionarySorterAction(parserManager, workerManager, this, this));
      dicSortDictionaryMenuItem.setMnemonic('s');
      dicSortDictionaryMenuItem.setText("Sort dictionary…");
      dicMenu.add(dicSortDictionaryMenuItem);

      dicRulesReducerMenuItem.setAction(new AffixRulesReducerAction(parserManager, this));
      dicRulesReducerMenuItem.setMnemonic('r');
      dicRulesReducerMenuItem.setText("Rules reducer…");
      dicRulesReducerMenuItem.setToolTipText("");
      dicMenu.add(dicRulesReducerMenuItem);
      dicMenu.add(dicDuplicatesSeparator);

      dicWordCountMenuItem.setAction(new DictionaryWordCountAction(workerManager, this));
      dicWordCountMenuItem.setMnemonic('w');
      dicWordCountMenuItem.setText("Word count");
      dicMenu.add(dicWordCountMenuItem);

      dicStatisticsMenuItem.setAction(new DictionaryHyphenationStatisticsAction(false, workerManager, this));
      dicStatisticsMenuItem.setMnemonic('t');
      dicStatisticsMenuItem.setText("Statistics");
      dicMenu.add(dicStatisticsMenuItem);
      dicMenu.add(dicStatisticsSeparator);

      dicExtractDuplicatesMenuItem.setAction(new DictionaryExtractDuplicatesAction(workerManager, this, this));
      dicExtractDuplicatesMenuItem.setMnemonic('d');
      dicExtractDuplicatesMenuItem.setText("Extract duplicates…");
      dicMenu.add(dicExtractDuplicatesMenuItem);

      dicExtractWordlistMenuItem.setAction(new DictionaryExtractWordlistAction(WordlistWorker.WorkerType.COMPLETE, workerManager, this, this));
      dicExtractWordlistMenuItem.setText("Extract wordlist…");
      dicMenu.add(dicExtractWordlistMenuItem);

      dicExtractWordlistPlainTextMenuItem.setAction(new DictionaryExtractWordlistAction(WordlistWorker.WorkerType.PLAIN_WORDS, workerManager, this, this));
      dicExtractWordlistPlainTextMenuItem.setText("Extract wordlist (plain words)…");
      dicMenu.add(dicExtractWordlistPlainTextMenuItem);

      dicExtractPoSFAMenuItem.setAction(new DictionaryExtractPosFSAAction(parserManager, workerManager, this, this));
      dicExtractPoSFAMenuItem.setText("Extract PoS FSA…");
      dicMenu.add(dicExtractPoSFAMenuItem);

      dicExtractMinimalPairsMenuItem.setAction(new DictionaryExtractMinimalPairsAction(workerManager, this, this));
      dicExtractMinimalPairsMenuItem.setMnemonic('m');
      dicExtractMinimalPairsMenuItem.setText("Extract minimal pairs…");
      dicMenu.add(dicExtractMinimalPairsMenuItem);

      mainMenuBar.add(dicMenu);

      theMenu.setMnemonic('D');
      theMenu.setText("Thesaurus tools");
      theMenu.setEnabled(false);

      theLinterMenuItem.setAction(new ThesaurusLinterAction(workerManager, this));
      theLinterMenuItem.setMnemonic('c');
      theLinterMenuItem.setText("Check correctness");
      theMenu.add(theLinterMenuItem);

      mainMenuBar.add(theMenu);

      hypMenu.setMnemonic('y');
      hypMenu.setText("Hyphenation tools");
      hypMenu.setEnabled(false);

      hypLinterMenuItem.setAction(new HyphenationLinterAction(workerManager, this));
      hypLinterMenuItem.setMnemonic('d');
      hypLinterMenuItem.setText("Check correctness");
      hypMenu.add(hypLinterMenuItem);
      hypMenu.add(hypDuplicatesSeparator);

      hypStatisticsMenuItem.setAction(new DictionaryHyphenationStatisticsAction(true, workerManager, this));
      hypStatisticsMenuItem.setMnemonic('t');
      hypStatisticsMenuItem.setText("Statistics");
      hypMenu.add(hypStatisticsMenuItem);

      mainMenuBar.add(hypMenu);

      hlpMenu.setMnemonic('H');
      hlpMenu.setText("Help");

      hlpOnlineHelpMenuItem.setAction(new OnlineHelpAction());
      hlpOnlineHelpMenuItem.setMnemonic('h');
      hlpOnlineHelpMenuItem.setText("Online help");
      hlpMenu.add(hlpOnlineHelpMenuItem);

      hlpIssueReporterMenuItem.setAction(new IssueReporterAction());
      hlpIssueReporterMenuItem.setText("Report an issue");
      hlpMenu.add(hlpIssueReporterMenuItem);
      hlpMenu.add(hlpOnlineSeparator);

      hlpUpdateMenuItem.setAction(new UpdateAction(this));
      hlpUpdateMenuItem.setText("Check for Update…");
      hlpMenu.add(hlpUpdateMenuItem);

      hlpCheckUpdateOnStartupCheckBoxMenuItem.setAction(new CheckUpdateOnStartupAction(hlpCheckUpdateOnStartupCheckBoxMenuItem, preferences));
      hlpCheckUpdateOnStartupCheckBoxMenuItem.setSelected(preferences.getBoolean(UPDATE_STARTUP_CHECK, true));
      hlpCheckUpdateOnStartupCheckBoxMenuItem.setText("Check for updates on startup");
      hlpMenu.add(hlpCheckUpdateOnStartupCheckBoxMenuItem);
      hlpMenu.add(hlpUpdateSeparator);

      hlpAboutMenuItem.setAction(new AboutAction(this));
      hlpAboutMenuItem.setMnemonic('a');
      hlpAboutMenuItem.setText("About");
      hlpMenu.add(hlpAboutMenuItem);

      mainMenuBar.add(hlpMenu);

      setJMenuBar(mainMenuBar);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
               .addComponent(mainTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 919, Short.MAX_VALUE)
               .addComponent(parsingResultScrollPane, javax.swing.GroupLayout.Alignment.LEADING)
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

	private void filEmptyRecentProjectsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filEmptyRecentProjectsMenuItemActionPerformed
		recentProjectsMenu.clear();

		recentProjectsMenu.setEnabled(false);
		filEmptyRecentProjectsMenuItem.setEnabled(false);
	}//GEN-LAST:event_filEmptyRecentProjectsMenuItemActionPerformed


	public void removeSelectedRowsFromAutoCorrect(){
		try{
			final int selectedRow = acoTable.convertRowIndexToModel(acoTable.getSelectedRow());
			parserManager.getAcoParser().deleteCorrection(selectedRow);

			final AutoCorrectTableModel dm = (AutoCorrectTableModel)acoTable.getModel();
			dm.fireTableDataChanged();

			updateAutoCorrectionsCounter();

			//… and save the files
			parserManager.storeAutoCorrectFile();
		}
		catch(final Exception e){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Deletion error: {}", e.getMessage());
		}
	}

	private void filterAutoCorrect(){
		final String unmodifiedIncorrectText = StringUtils.strip(acoIncorrectTextField.getText());
		final String unmodifiedCorrectText = StringUtils.strip(acoCorrectTextField.getText());
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
		final TableRowSorter<AutoCorrectTableModel> sorter = (TableRowSorter<AutoCorrectTableModel>)acoTable.getRowSorter();
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

	private void filterSentenceExceptions(){
		final String unmodifiedException = StringUtils.strip(sexTextField.getText());
		if(formerFilterSentenceException != null && formerFilterSentenceException.equals(unmodifiedException))
			return;

		formerFilterSentenceException = unmodifiedException;

		//if text to be inserted is already fully contained into the thesaurus, do not enable the button
		final boolean alreadyContained = parserManager.getSexParser().contains(unmodifiedException);
		sexAddButton.setEnabled(StringUtils.isNotBlank(unmodifiedException) && unmodifiedException.endsWith(".")
			&& !alreadyContained);


		sexTagPanel.applyFilter(StringUtils.isNotBlank(unmodifiedException)? unmodifiedException: null);
	}

	private void filterWordExceptions(){
		final String unmodifiedException = StringUtils.strip(wexTextField.getText());
		if(formerFilterWordException != null && formerFilterWordException.equals(unmodifiedException))
			return;

		formerFilterWordException = unmodifiedException;

		//if text to be inserted is already fully contained into the thesaurus, do not enable the button
		final boolean alreadyContained = parserManager.getWexParser().contains(unmodifiedException);
		wexAddButton.setEnabled(StringUtils.isNotBlank(unmodifiedException) && StringHelper.countUppercases(unmodifiedException) > 1 && !alreadyContained);


		wexTagPanel.applyFilter(StringUtils.isNotBlank(unmodifiedException)? unmodifiedException: null);
	}

	private void hypAddRuleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hypAddRuleButtonActionPerformed
		final  String newRule = hypAddRuleTextField.getText();
		final HyphenationParser.Level level = HyphenationParser.Level.values()[hypAddRuleLevelComboBox.getSelectedIndex()];
		final String foundRule = parserManager.addHyphenationRule(newRule.toLowerCase(Locale.ROOT), level);
		if(foundRule == null){
			try{
				parserManager.storeHyphenationFile();

				if(hypWordTextField.getText() != null){
					formerHyphenationText = null;
					hyphenate();
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

			LOGGER.info(ParserManager.MARKER_APPLICATION, "Duplicated rule found ({}), cannot insert {}", foundRule, newRule);
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
					"There is a duplicate with same incorrect and correct forms.\nForce insertion?", "Duplicate detected",
					JOptionPane.YES_NO_OPTION);
				return (responseOption == JOptionPane.YES_OPTION);
			};
			final DuplicationResult<CorrectionEntry> duplicationResult = parserManager.getAcoParser()
				.insertCorrection(incorrect, correct, duplicatesDiscriminator);
			if(duplicationResult.isForceInsertion()){
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

				updateAutoCorrectionsCounter();

				//… and save the files
				parserManager.storeAutoCorrectFile();
			}
			else{
				acoIncorrectTextField.requestFocusInWindow();

				final String duplicatedWords = duplicationResult.getDuplicates().stream()
					.map(CorrectionEntry::toString)
					.collect(Collectors.joining(", "));
				LOGGER.info(ParserManager.MARKER_APPLICATION, "Duplicate detected: {}", duplicatedWords);
			}
		}
		catch(final Exception e){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Insertion error: {}", e.getMessage());
		}
	}//GEN-LAST:event_acoAddButtonActionPerformed

	private void optionsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optionsButtonActionPerformed
		final Consumer<HyphenationOptionsParser> acceptButtonAction = (options) -> {
			try{
				parserManager.getHypParser().setOptions(options);

				parserManager.storeHyphenationFile();
			}
			catch(Exception ex){
				LOGGER.info(ParserManager.MARKER_APPLICATION, ex.getMessage());
			}
		};
		final HyphenationOptionsDialog dialog = new HyphenationOptionsDialog(parserManager.getHypParser().getOptions(),
			acceptButtonAction, this);
		GUIUtils.addCancelByEscapeKey(dialog);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}//GEN-LAST:event_optionsButtonActionPerformed

	private void sexTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_sexTextFieldKeyReleased
		sexFilterDebouncer.call(this);
	}//GEN-LAST:event_sexTextFieldKeyReleased

	private void sexAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sexAddButtonActionPerformed
		try{
			final String exception = StringUtils.strip(sexTextField.getText());
			if(!parserManager.getSexParser().contains(exception)){
				parserManager.getSexParser().modify(ExceptionsParser.TagChangeType.ADD, Collections.singletonList(exception));
				sexTagPanel.addTag(exception);

				//reset input
				sexTextField.setText(StringUtils.EMPTY);
				sexTagPanel.applyFilter(null);

				updateSentenceExceptionsCounter();

				parserManager.storeSentenceExceptionFile();
			}
			else{
				sexTextField.requestFocusInWindow();

				JOptionPane.showOptionDialog(this,
					"A duplicate is already present", "Warning!", JOptionPane.DEFAULT_OPTION,
					JOptionPane.WARNING_MESSAGE, null, null, null);
			}
		}
		catch(final Exception e){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Insertion error: {}", e.getMessage());
		}
	}//GEN-LAST:event_sexAddButtonActionPerformed

	private void wexTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_wexTextFieldKeyReleased
		wexFilterDebouncer.call(this);
	}//GEN-LAST:event_wexTextFieldKeyReleased

   private void wexAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wexAddButtonActionPerformed
		try{
			final String exception = StringUtils.strip(wexTextField.getText());
			if(!parserManager.getWexParser().contains(exception)){
				parserManager.getWexParser().modify(ExceptionsParser.TagChangeType.ADD, Collections.singletonList(exception));
				wexTagPanel.addTag(exception);

				//reset input
				wexTextField.setText(StringUtils.EMPTY);
				wexTagPanel.applyFilter(null);

				updateWordExceptionsCounter();

				parserManager.storeWordExceptionFile();
			}
			else{
				wexTextField.requestFocusInWindow();

				JOptionPane.showOptionDialog(this,
					"A duplicate is already present", "Warning!", JOptionPane.DEFAULT_OPTION,
					JOptionPane.WARNING_MESSAGE, null, null, null);
			}
		}
		catch(final Exception e){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Insertion error: {}", e.getMessage());
		}
   }//GEN-LAST:event_wexAddButtonActionPerformed


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

		((DictionaryLayeredPane)dicLayeredPane).setCurrentFont();
		((CompoundsLayeredPane)cmpLayeredPane).setCurrentFont();
		((ThesaurusLayeredPane)theLayeredPane).setCurrentFont();

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


			((DictionaryLayeredPane)dicLayeredPane).initialize();
			((CompoundsLayeredPane)cmpLayeredPane).initialize();
			((ThesaurusLayeredPane)theLayeredPane).initialize();


			//hyphenation file:
			if(parserManager.getHyphenator() != null){
				hypLinterMenuItem.setEnabled(true);

				hypMenu.setEnabled(true);
				hypStatisticsMenuItem.setEnabled(true);
				GUIUtils.setTabbedPaneEnable(mainTabbedPane, hypLayeredPane, true);
			}
			openHypButton.setEnabled(packager.getHyphenationFile() != null);


			//thesaurus file:
			if(parserManager.getTheParser().getSynonymsCount() > 0){
				theMenu.setEnabled(true);
				theLinterMenuItem.setEnabled(true);
				GUIUtils.setTabbedPaneEnable(mainTabbedPane, theLayeredPane, true);
			}


			//auto–correct file:
			if(parserManager.getAcoParser().getCorrectionsCounter() > 0){
				addSorterToTable(acoTable, comparator, null);

				final AutoCorrectTableModel dm = (AutoCorrectTableModel)acoTable.getModel();
				dm.setCorrections(parserManager.getAcoParser().getCorrectionsDictionary());
				updateAutoCorrectionsCounter();
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


		((DictionaryLayeredPane)dicLayeredPane).clear();
		((CompoundsLayeredPane)cmpLayeredPane).clear();
		((ThesaurusLayeredPane)theLayeredPane).clear();
		GUIUtils.setTabbedPaneEnable(mainTabbedPane, theLayeredPane, false);


		//hyphenation file:
		hypMenu.setEnabled(false);
		hypStatisticsMenuItem.setEnabled(false);
		GUIUtils.setTabbedPaneEnable(mainTabbedPane, hypLayeredPane, false);
		openHypButton.setEnabled(false);


		//aid file:
		clearAidParser();


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

	private void updateAutoCorrectionsCounter(){
		acoCorrectionsRecordedOutputLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(parserManager.getAcoParser().getCorrectionsCounter()));
	}

	private void updateSentenceExceptionsCounter(){
		sexCorrectionsRecordedOutputLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(parserManager.getSexParser().getExceptionsCounter()));
	}

	private void updateWordExceptionsCounter(){
		wexCorrectionsRecordedOutputLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(parserManager.getWexParser().getExceptionsCounter()));
	}


	private void hyphenate(){
		final String language = parserManager.getAffixData().getLanguage();
		final Orthography orthography = BaseBuilder.getOrthography(language);
		String text = orthography.correctOrthography(hypWordTextField.getText());
		if(formerHyphenationText != null && formerHyphenationText.equals(text))
			return;
		formerHyphenationText = text;

		String count = null;
		List<String> rules = Collections.emptyList();
		if(StringUtils.isNotBlank(text)){
			final Hyphenation hyphenation = parserManager.getHyphenator().hyphenate(text);

			final Supplier<StringJoiner> sj = () -> new StringJoiner(HyphenationParser.SOFT_HYPHEN, "<html>",
				"</html>");
			final Function<String, String> errorFormatter = syllabe -> "<b style=\"color:red\">" + syllabe + "</b>";
			text = orthography.formatHyphenation(hyphenation.getSyllabes(), sj.get(), errorFormatter)
				.toString();
			count = Long.toString(hyphenation.countSyllabes());
			rules = hyphenation.getRules();

			hypAddRuleTextField.setEnabled(true);
		}
		else{
			text = null;

			hypAddRuleTextField.setEnabled(false);
		}

		hypSyllabationOutputLabel.setText(text);
		hypSyllabesCountOutputLabel.setText(count);
		hypRulesOutputLabel.setText(StringUtils.join(rules, StringUtils.SPACE));

		hypAddRuleTextField.setText(null);
		hypAddRuleSyllabationOutputLabel.setText(null);
		hypAddRuleSyllabesCountOutputLabel.setText(null);
	}

	private void hyphenateAddRule(){
		final String language = parserManager.getAffixData().getLanguage();
		final Orthography orthography = BaseBuilder.getOrthography(language);
		String addedRuleText = orthography.correctOrthography(hypWordTextField.getText());
		final String addedRule = orthography.correctOrthography(hypAddRuleTextField.getText().toLowerCase(Locale.ROOT));
		final HyphenationParser.Level level = HyphenationParser.Level.values()[hypAddRuleLevelComboBox.getSelectedIndex()];
		String addedRuleCount = null;
		if(StringUtils.isNotBlank(addedRule)){
			final boolean alreadyHasRule = parserManager.hasHyphenationRule(addedRule, level);
			boolean ruleMatchesText = false;
			boolean hyphenationChanged = false;
			boolean correctHyphenation = false;
			if(!alreadyHasRule){
				ruleMatchesText = addedRuleText.contains(PatternHelper.clear(addedRule,
					PATTERN_POINTS_AND_NUMBERS_AND_EQUALS_AND_MINUS));

				if(ruleMatchesText){
					final Hyphenation hyphenation = parserManager.getHyphenator().hyphenate(addedRuleText);
					final Hyphenation addedRuleHyphenation = parserManager.getHyphenator().hyphenate(addedRuleText, addedRule,
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
			hypAddRuleLevelComboBox.setEnabled(ruleMatchesText);
			hypAddRuleButton.setEnabled(ruleMatchesText && hyphenationChanged && correctHyphenation);
		}
		else{
			addedRuleText = null;

			hypAddRuleTextField.setText(null);
			hypAddRuleLevelComboBox.setEnabled(false);
			hypAddRuleButton.setEnabled(false);
			hypAddRuleSyllabationOutputLabel.setText(null);
			hypAddRuleSyllabesCountOutputLabel.setText(null);
		}

		hypAddRuleSyllabationOutputLabel.setText(addedRuleText);
		hypAddRuleSyllabesCountOutputLabel.setText(addedRuleCount);
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
		((CompoundsLayeredPane)cmpLayeredPane).clear();

		GUIUtils.setTabbedPaneEnable(mainTabbedPane, dicLayeredPane, false);
		GUIUtils.setTabbedPaneEnable(mainTabbedPane, cmpLayeredPane, false);

		//disable menu
		dicMenu.setEnabled(false);
		filCreatePackageMenuItem.setEnabled(false);
		filFontMenuItem.setEnabled(false);
	}

	private void clearDictionaryFields(){
		((DictionaryLayeredPane)dicLayeredPane).clear();
		((ThesaurusLayeredPane)theLayeredPane).clear();
	}

	@Override
	public void clearAidParser(){
		((DictionaryLayeredPane)dicLayeredPane).clearAid();
		((CompoundsLayeredPane)cmpLayeredPane).clearAid();
	}

	@Override
	public void clearThesaurusParser(){
		((ThesaurusLayeredPane)theLayeredPane).clear();

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


	public static void main(String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e){
			LOGGER.error(null, e);
		}

		//create and display the form
		JavaHelper.executeOnEventDispatchThread(() -> (new HunLinterFrame()).setVisible(true));
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
   private javax.swing.JLayeredPane cmpLayeredPane;
   private javax.swing.JPopupMenu.Separator dicDuplicatesSeparator;
   private javax.swing.JMenuItem dicExtractDuplicatesMenuItem;
   private javax.swing.JMenuItem dicExtractMinimalPairsMenuItem;
   private javax.swing.JMenuItem dicExtractPoSFAMenuItem;
   private javax.swing.JMenuItem dicExtractWordlistMenuItem;
   private javax.swing.JMenuItem dicExtractWordlistPlainTextMenuItem;
   private javax.swing.JLayeredPane dicLayeredPane;
   private javax.swing.JMenuItem dicLinterMenuItem;
   private javax.swing.JMenu dicMenu;
   private javax.swing.JMenuItem dicRulesReducerMenuItem;
   private javax.swing.JMenuItem dicSortDictionaryMenuItem;
   private javax.swing.JMenuItem dicStatisticsMenuItem;
   private javax.swing.JPopupMenu.Separator dicStatisticsSeparator;
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
   private javax.swing.JPopupMenu.Separator hypDuplicatesSeparator;
   private javax.swing.JLayeredPane hypLayeredPane;
   private javax.swing.JMenuItem hypLinterMenuItem;
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
   private javax.swing.JLayeredPane theLayeredPane;
   private javax.swing.JMenuItem theLinterMenuItem;
   private javax.swing.JMenu theMenu;
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
