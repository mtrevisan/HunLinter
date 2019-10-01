package unit731.hunspeller;

import java.awt.*;

import unit731.hunspeller.gui.AscendingDescendingUnsortedTableRowSorter;
import unit731.hunspeller.interfaces.Hunspellable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.MenuSelectionManager;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.DefaultCaret;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.gui.CompoundTableModel;
import unit731.hunspeller.interfaces.Undoable;
import unit731.hunspeller.gui.GUIUtils;
import unit731.hunspeller.gui.HunspellerTableModel;
import unit731.hunspeller.gui.ProductionTableModel;
import unit731.hunspeller.gui.RecentFilesMenu;
import unit731.hunspeller.gui.ThesaurusTableModel;
import unit731.hunspeller.gui.ThesaurusTableRenderer;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.languages.BaseBuilder;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;
import unit731.hunspeller.parsers.vos.AffixEntry;
import unit731.hunspeller.parsers.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.exceptions.ProjectFileNotFoundException;
import unit731.hunspeller.parsers.dictionary.workers.CompoundRulesWorker;
import unit731.hunspeller.parsers.dictionary.workers.CorrectnessWorker;
import unit731.hunspeller.parsers.dictionary.workers.DuplicatesWorker;
import unit731.hunspeller.parsers.dictionary.workers.HyphenationCorrectnessWorker;
import unit731.hunspeller.parsers.dictionary.workers.MinimalPairsWorker;
import unit731.hunspeller.parsers.dictionary.workers.ProjectLoaderWorker;
import unit731.hunspeller.parsers.dictionary.workers.SorterWorker;
import unit731.hunspeller.parsers.dictionary.workers.StatisticsWorker;
import unit731.hunspeller.parsers.dictionary.workers.WordCountWorker;
import unit731.hunspeller.parsers.dictionary.workers.WordlistWorker;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerBase;
import unit731.hunspeller.parsers.thesaurus.DuplicationResult;
import unit731.hunspeller.parsers.hyphenation.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.thesaurus.ThesaurusParser;
import unit731.hunspeller.parsers.thesaurus.ThesaurusEntry;
import unit731.hunspeller.services.ApplicationLogAppender;
import unit731.hunspeller.services.Debouncer;
import unit731.hunspeller.services.ExceptionHelper;
import unit731.hunspeller.services.PatternHelper;
import unit731.hunspeller.services.RecentItems;


/**
 * @see <a href="http://manpages.ubuntu.com/manpages/trusty/man4/hunspell.4.html">Hunspell 4</a>
 * @see <a href="https://github.com/lopusz/hunspell-stemmer">Hunspell stemmer on github</a>
 * @see <a href="https://github.com/nuspell/nuspell">Nuspell on github</a>
 * @see <a href="https://github.com/hunspell/hyphen">Hyphen on github</a>
 *
 * @see <a href="https://www.shareicon.net/">Share icon</a>
 * @see <a href="https://www.iloveimg.com/resize-image/resize-png">PNG resizer</a>
 * @see <a href="https://compresspng.com/">PNG compresser</a>
 * @see <a href="https://www.icoconverter.com/index.php">ICO converter</a>
 */
public class HunspellerFrame extends JFrame implements ActionListener, PropertyChangeListener, Hunspellable, Undoable{

	private static final long serialVersionUID = 6772959670167531135L;

	private static final Logger LOGGER = LoggerFactory.getLogger(HunspellerFrame.class);

	private static final String URL_ONLINE_HELP = "https://github.com/mtrevisan/Hunspeller";

	private final static String FONT_FAMILY_NAME_PREFIX = "font.familyName.";
	private final static String FONT_SIZE_PREFIX = "font.size.";

	private static final int DEBOUNCER_INTERVAL = 600;
	private static final Pattern PATTERN_POINTS_AND_NUMBERS_AND_EQUALS_AND_MINUS = PatternHelper.pattern("[.\\d=-]");

	private String formerInputText;
	private String formerCompoundInputText;
	private String formerFilterThesaurusText;
	private String formerHyphenationText;
	private final JFileChooser openAffixFileFileChooser;
	private final JFileChooser saveTextFileFileChooser;
	private DictionarySortDialog dicSortDialog;
	private RulesReducerDialog rulesReducerDialog;

	private final Preferences preferences = Preferences.userNodeForPackage(getClass());
	private final Backbone backbone;

	private RecentFilesMenu recentFilesMenu;
	private final Debouncer<HunspellerFrame> productionDebouncer = new Debouncer<>(this::calculateProductions, DEBOUNCER_INTERVAL);
	private final Debouncer<HunspellerFrame> compoundProductionDebouncer = new Debouncer<>(this::calculateCompoundProductions, DEBOUNCER_INTERVAL);
	private final Debouncer<HunspellerFrame> theFilterDebouncer = new Debouncer<>(this::filterThesaurus, DEBOUNCER_INTERVAL);
	private final Debouncer<HunspellerFrame> hypDebouncer = new Debouncer<>(this::hyphenate, DEBOUNCER_INTERVAL);
	private final Debouncer<HunspellerFrame> hypAddRuleDebouncer = new Debouncer<>(this::hyphenateAddRule, DEBOUNCER_INTERVAL);

	private ProjectLoaderWorker prjLoaderWorker;
	private CorrectnessWorker dicCorrectnessWorker;
	private DuplicatesWorker dicDuplicatesWorker;
	private SorterWorker dicSorterWorker;
	private WordCountWorker dicWordCountWorker;
	private StatisticsWorker dicStatisticsWorker;
	private WordlistWorker dicWordlistWorker;
	private MinimalPairsWorker dicMinimalPairsWorker;
	private CompoundRulesWorker compoundRulesExtractorWorker;
	private HyphenationCorrectnessWorker hypCorrectnessWorker;
	private final Map<String, Runnable> enableComponentFromWorker = new HashMap<>();


	public HunspellerFrame(){
		backbone = new Backbone(this, this);

		initComponents();

		recentFilesMenu.setEnabled(recentFilesMenu.hasEntries());
		filEmptyRecentFilesMenuItem.setEnabled(recentFilesMenu.hasEntries());

		try{
			JPopupMenu copyingPopupMenu = GUIUtils.createCopyingPopupMenu(hypRulesOutputLabel.getHeight());
			GUIUtils.addPopupMenu(copyingPopupMenu, hypSyllabationOutputLabel, hypRulesOutputLabel, hypAddRuleSyllabationOutputLabel);
		}
		catch(IOException ignored){}

		ApplicationLogAppender.addTextArea(parsingResultTextArea, Backbone.MARKER_APPLICATION);


		File currentDir = new File(".");
		openAffixFileFileChooser = new JFileChooser();
		openAffixFileFileChooser.setFileFilter(new FileNameExtensionFilter("AFF files", "aff"));
		openAffixFileFileChooser.setCurrentDirectory(currentDir);

		saveTextFileFileChooser = new JFileChooser();
		saveTextFileFileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
		saveTextFileFileChooser.setCurrentDirectory(currentDir);

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
		enableComponentFromWorker.put(CompoundRulesWorker.WORKER_NAME, () -> {
			cmpInputComboBox.setEnabled(true);
			cmpLimitComboBox.setEnabled(true);
			cmpInputTextArea.setEnabled(true);
			if(compoundRulesExtractorWorker.isCancelled())
				cmpLoadInputButton.setEnabled(true);
		});
		enableComponentFromWorker.put(HyphenationCorrectnessWorker.WORKER_NAME, () -> hypCheckCorrectnessMenuItem.setEnabled(true));
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
      dicTable = new javax.swing.JTable();
      dicTotalProductionsLabel = new javax.swing.JLabel();
      dicTotalProductionsOutputLabel = new javax.swing.JLabel();
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
      theMeaningsLabel = new javax.swing.JLabel();
      theMeaningsTextField = new javax.swing.JTextField();
      theAddButton = new javax.swing.JButton();
      theScrollPane = new javax.swing.JScrollPane();
      theTable = new javax.swing.JTable();
      theSynonymsRecordedLabel = new javax.swing.JLabel();
      theSynonymsRecordedOutputLabel = new javax.swing.JLabel();
      theUndoButton = new javax.swing.JButton();
      theRedoButton = new javax.swing.JButton();
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
      mainMenuBar = new javax.swing.JMenuBar();
      filMenu = new javax.swing.JMenu();
      filOpenAFFMenuItem = new javax.swing.JMenuItem();
      filCreatePackageMenuItem = new javax.swing.JMenuItem();
      filFontSeparator = new javax.swing.JPopupMenu.Separator();
      filFontMenuItem = new javax.swing.JMenuItem();
      filRecentFilesSeparator = new javax.swing.JPopupMenu.Separator();
      filEmptyRecentFilesMenuItem = new javax.swing.JMenuItem();
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
      dicExtractMinimalPairsMenuItem = new javax.swing.JMenuItem();
      theMenu = new javax.swing.JMenu();
      theFindDuplicatesMenuItem = new javax.swing.JMenuItem();
      hypMenu = new javax.swing.JMenu();
      hypCheckCorrectnessMenuItem = new javax.swing.JMenuItem();
      hypDuplicatesSeparator = new javax.swing.JPopupMenu.Separator();
      hypStatisticsMenuItem = new javax.swing.JMenuItem();
      hlpMenu = new javax.swing.JMenu();
      hlpOnlineHelpMenuItem = new javax.swing.JMenuItem();
      hlpAboutMenuItem = new javax.swing.JMenuItem();

      setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
      setTitle("Hunspeller");
      setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/favicon.jpg")));
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
      dicScrollPane.setViewportView(dicTable);

      dicTotalProductionsLabel.setLabelFor(dicTotalProductionsOutputLabel);
      dicTotalProductionsLabel.setText("Total productions:");

      dicTotalProductionsOutputLabel.setText("...");

      dicLayeredPane.setLayer(dicInputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(dicInputTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(dicRuleFlagsAidLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(dicRuleFlagsAidComboBox, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(dicScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(dicTotalProductionsLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(dicTotalProductionsOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);

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
               .addComponent(dicScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 904, Short.MAX_VALUE)
               .addGroup(dicLayeredPaneLayout.createSequentialGroup()
                  .addComponent(dicTotalProductionsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(dicTotalProductionsOutputLabel)
                  .addGap(0, 0, Short.MAX_VALUE)))
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
            .addComponent(dicScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(dicLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(dicTotalProductionsLabel)
               .addComponent(dicTotalProductionsOutputLabel))
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
            limitComboBoxActionPerformed(evt);
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

      cmpInputTextArea.setColumns(20);
      cmpInputTextArea.setRows(1);
      cmpInputScrollPane.setViewportView(cmpInputTextArea);

      cmpLoadInputButton.setText("Load input from dictionary");
      cmpLoadInputButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            loadInputButtonActionPerformed(evt);
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

      theMeaningsLabel.setLabelFor(theMeaningsTextField);
      theMeaningsLabel.setText("New synonym:");

      theMeaningsTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            theMeaningsTextFieldKeyReleased(evt);
         }
      });

      theAddButton.setMnemonic('A');
      theAddButton.setText("Add");
      theAddButton.setToolTipText("");
      theAddButton.setEnabled(false);
      theAddButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            theAddButtonActionPerformed(evt);
         }
      });

      theTable.setModel(new ThesaurusTableModel());
      theTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
      theTable.setRowSorter(new TableRowSorter<>((ThesaurusTableModel)theTable.getModel()));
      theTable.setShowHorizontalLines(false);
      theTable.setShowVerticalLines(false);
      theTable.setRowSelectionAllowed(true);
      theTable.getColumnModel().getColumn(0).setMinWidth(200);
      theTable.getColumnModel().getColumn(0).setMaxWidth(500);

      theTable.registerKeyboardAction(this, cancelKeyStroke, JComponent.WHEN_FOCUSED);

      JFrame parent = this;
      theTable.addMouseListener(new MouseAdapter(){
         public void mouseClicked(MouseEvent e){
            if(e.getClickCount() == 1){
               JTable target = (JTable)e.getSource();
               int col = target.getSelectedColumn();
               if(col == 1){
                  int row = theTable.convertRowIndexToModel(target.getSelectedRow());
                  Consumer<String> okButtonAction = (text) -> {
                     try{
                        backbone.getTheParser().setMeanings(row, text);

                        // ... and save the files
                        backbone.storeThesaurusFiles();
                     }
                     catch(IllegalArgumentException | IOException ex){
                        LOGGER.info(Backbone.MARKER_APPLICATION, unit731.hunspeller.services.ExceptionHelper.getMessage(ex));
                     }
                  };
                  ThesaurusEntry synonym = backbone.getTheParser().getSynonymsDictionary().get(row);
                  ThesaurusMeaningsDialog dialog = new ThesaurusMeaningsDialog(synonym, okButtonAction, parent);
                  GUIUtils.addCancelByEscapeKey(dialog);
                  dialog.setLocationRelativeTo(parent);
                  dialog.setVisible(true);
               }
            }
         }
      });

      ThesaurusTableRenderer cellRenderer = new ThesaurusTableRenderer();
      theTable.getColumnModel().getColumn(1).setCellRenderer(cellRenderer);
      theScrollPane.setViewportView(theTable);

      theSynonymsRecordedLabel.setLabelFor(theSynonymsRecordedOutputLabel);
      theSynonymsRecordedLabel.setText("Synonyms recorded:");

      theSynonymsRecordedOutputLabel.setText("...");

      theUndoButton.setMnemonic('U');
      theUndoButton.setText("Undo");
      theUndoButton.setToolTipText("");
      theUndoButton.setEnabled(false);
      theUndoButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            theUndoButtonActionPerformed(evt);
         }
      });

      theRedoButton.setMnemonic('R');
      theRedoButton.setText("Redo");
      theRedoButton.setEnabled(false);
      theRedoButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            theRedoButtonActionPerformed(evt);
         }
      });

      theLayeredPane.setLayer(theMeaningsLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      theLayeredPane.setLayer(theMeaningsTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      theLayeredPane.setLayer(theAddButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      theLayeredPane.setLayer(theScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
      theLayeredPane.setLayer(theSynonymsRecordedLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      theLayeredPane.setLayer(theSynonymsRecordedOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      theLayeredPane.setLayer(theUndoButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      theLayeredPane.setLayer(theRedoButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout theLayeredPaneLayout = new javax.swing.GroupLayout(theLayeredPane);
      theLayeredPane.setLayout(theLayeredPaneLayout);
      theLayeredPaneLayout.setHorizontalGroup(
         theLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(theLayeredPaneLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(theLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(theScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 904, Short.MAX_VALUE)
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, theLayeredPaneLayout.createSequentialGroup()
                  .addComponent(theMeaningsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(theMeaningsTextField)
                  .addGap(18, 18, 18)
                  .addComponent(theAddButton)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(theUndoButton)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(theRedoButton))
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
               .addComponent(theMeaningsLabel)
               .addComponent(theMeaningsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(theAddButton)
               .addComponent(theUndoButton)
               .addComponent(theRedoButton))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(theScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 173, Short.MAX_VALUE)
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

      hypSyllabationOutputLabel.setText("...");

      hypSyllabesCountLabel.setLabelFor(hypSyllabesCountOutputLabel);
      hypSyllabesCountLabel.setText("Syllabes:");

      hypSyllabesCountOutputLabel.setText("...");

      hypRulesLabel.setLabelFor(hypRulesOutputLabel);
      hypRulesLabel.setText("Rules:");

      hypRulesOutputLabel.setText("...");

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

      hypAddRuleSyllabationOutputLabel.setText("...");

      hypAddRuleSyllabesCountLabel.setLabelFor(hypAddRuleSyllabesCountOutputLabel);
      hypAddRuleSyllabesCountLabel.setText("New syllabes:");

      hypAddRuleSyllabesCountOutputLabel.setText("...");

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
                  .addComponent(hypAddRuleSyllabationLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypAddRuleSyllabationOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, hypLayeredPaneLayout.createSequentialGroup()
                  .addComponent(hypRulesLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypRulesOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
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
            .addContainerGap(69, Short.MAX_VALUE))
      );

      mainTabbedPane.addTab("Hyphenation", hypLayeredPane);

      addWindowListener(new WindowAdapter(){
         @Override
         public void windowClosing(WindowEvent e){
            exit();
         }
      });

      filMenu.setMnemonic('F');
      filMenu.setText("File");
      filMenu.setToolTipText("");

      filOpenAFFMenuItem.setMnemonic('a');
      filOpenAFFMenuItem.setText("Open AFF file...");
      filOpenAFFMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            filOpenAFFMenuItemActionPerformed(evt);
         }
      });
      filMenu.add(filOpenAFFMenuItem);

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

      filFontMenuItem.setMnemonic('f');
      filFontMenuItem.setText("Select font...");
      filFontMenuItem.setEnabled(false);
      filFontMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            filFontMenuItemActionPerformed(evt);
         }
      });
      filMenu.add(filFontMenuItem);
      filMenu.add(filRecentFilesSeparator);

      filEmptyRecentFilesMenuItem.setMnemonic('e');
      filEmptyRecentFilesMenuItem.setText("Empty Recent Files list");
      filEmptyRecentFilesMenuItem.setEnabled(false);
      filEmptyRecentFilesMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            filEmptyRecentFilesMenuItemActionPerformed(evt);
         }
      });
      filMenu.add(filEmptyRecentFilesMenuItem);
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
      recentFilesMenu = new unit731.hunspeller.gui.RecentFilesMenu(recentItems, this::loadFile);
      recentFilesMenu.setText("Recent files");
      recentFilesMenu.setMnemonic('R');
      filMenu.add(recentFilesMenu, 3);

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
      dicSortDictionaryMenuItem.setText("Sort dictionary...");
      dicSortDictionaryMenuItem.setToolTipText("");
      dicSortDictionaryMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            dicSortDictionaryMenuItemActionPerformed(evt);
         }
      });
      dicMenu.add(dicSortDictionaryMenuItem);

      dicRulesReducerMenuItem.setText("Rules reducer...");
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
      dicExtractDuplicatesMenuItem.setText("Extract duplicates...");
      dicExtractDuplicatesMenuItem.setToolTipText("");
      dicExtractDuplicatesMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            dicExtractDuplicatesMenuItemActionPerformed(evt);
         }
      });
      dicMenu.add(dicExtractDuplicatesMenuItem);

      dicExtractWordlistMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/dictionary_wordlist.png"))); // NOI18N
      dicExtractWordlistMenuItem.setText("Extract wordlist...");
      dicExtractWordlistMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            dicExtractWordlistMenuItemActionPerformed(evt);
         }
      });
      dicMenu.add(dicExtractWordlistMenuItem);

      dicExtractWordlistPlainTextMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/dictionary_wordlist.png"))); // NOI18N
      dicExtractWordlistPlainTextMenuItem.setText("Extract wordlist (plain words)...");
      dicExtractWordlistPlainTextMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            dicExtractWordlistPlainTextMenuItemActionPerformed(evt);
         }
      });
      dicMenu.add(dicExtractWordlistPlainTextMenuItem);

      dicExtractMinimalPairsMenuItem.setMnemonic('m');
      dicExtractMinimalPairsMenuItem.setText("Extract minimal pairs...");
      dicExtractMinimalPairsMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            dicExtractMinimalPairsMenuItemActionPerformed(evt);
         }
      });
      dicMenu.add(dicExtractMinimalPairsMenuItem);

      mainMenuBar.add(dicMenu);

      theMenu.setMnemonic('T');
      theMenu.setText("Thesaurus tools");
      theMenu.setToolTipText("");
      theMenu.setEnabled(false);

      theFindDuplicatesMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/dictionary_duplicates.png"))); // NOI18N
      theFindDuplicatesMenuItem.setMnemonic('d');
      theFindDuplicatesMenuItem.setText("Find duplicates");
      theFindDuplicatesMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            theFindDuplicatesMenuItemActionPerformed(evt);
         }
      });
      theMenu.add(theFindDuplicatesMenuItem);

      mainMenuBar.add(theMenu);

      hypMenu.setMnemonic('H');
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

      hlpOnlineHelpMenuItem.setMnemonic('h');
      hlpOnlineHelpMenuItem.setText("Online help");
      hlpOnlineHelpMenuItem.setToolTipText("");
      hlpOnlineHelpMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            hlpOnlineHelpMenuItemActionPerformed(evt);
         }
      });
      hlpMenu.add(hlpOnlineHelpMenuItem);

      hlpAboutMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/about.png"))); // NOI18N
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

      mainTabbedPane.setEnabledAt(0, false);
      mainTabbedPane.setEnabledAt(1, false);
      mainTabbedPane.setEnabledAt(2, false);
      mainTabbedPane.setEnabledAt(3, false);
      KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
      mainTabbedPane.registerKeyboardAction(this, escapeKeyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

      pack();
      setLocationRelativeTo(null);
   }// </editor-fold>//GEN-END:initComponents

	public void setCurrentFont(){
		final Font font = GUIUtils.getCurrentFont();
		parsingResultTextArea.setFont(font);

		dicInputTextField.setFont(font);
		dicRuleFlagsAidComboBox.setFont(font);
		dicTable.setFont(font);

		cmpInputComboBox.setFont(font);
		cmpRuleFlagsAidComboBox.setFont(font);
		cmpInputTextArea.setFont(font);
		cmpTable.setFont(font);

		theMeaningsTextField.setFont(font);
		theTable.setFont(font);

		hypWordTextField.setFont(font);
		hypSyllabationOutputLabel.setFont(font);
		hypRulesOutputLabel.setFont(font);
		hypAddRuleTextField.setFont(font);
		hypAddRuleSyllabationOutputLabel.setFont(font);
	}

   private void filOpenAFFMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filOpenAFFMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		int fileSelected = openAffixFileFileChooser.showOpenDialog(this);
		if(fileSelected == JFileChooser.APPROVE_OPTION){
			recentFilesMenu.addEntry(openAffixFileFileChooser.getSelectedFile().getAbsolutePath());

			recentFilesMenu.setEnabled(true);
			filEmptyRecentFilesMenuItem.setEnabled(true);

			File affFile = openAffixFileFileChooser.getSelectedFile();
			loadFile(affFile.getAbsolutePath());
		}
   }//GEN-LAST:event_filOpenAFFMenuItemActionPerformed

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

		HelpDialog dialog = new HelpDialog(this);
		GUIUtils.addCancelByEscapeKey(dialog);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
   }//GEN-LAST:event_hlpAboutMenuItemActionPerformed


	private void calculateProductions(HunspellerFrame frame){
		String inputText = StringUtils.strip(frame.dicInputTextField.getText());

		if(formerInputText != null && formerInputText.equals(inputText))
			return;
		formerInputText = inputText;

		if(StringUtils.isNotBlank(inputText)){
			try{
				List<Production> productions = frame.backbone.getWordGenerator().applyAffixRules(inputText);

				ProductionTableModel dm = (ProductionTableModel)frame.dicTable.getModel();
				dm.setProductions(productions);

				//show first row
				Rectangle cellRect = frame.dicTable.getCellRect(0, 0, true);
				frame.dicTable.scrollRectToVisible(cellRect);

				frame.dicTotalProductionsOutputLabel.setText(Integer.toString(productions.size()));
			}
			catch(IllegalArgumentException e){
				LOGGER.info(Backbone.MARKER_APPLICATION, "{} for input {}", e.getMessage(), inputText);
			}
		}
		else{
			frame.clearOutputTable(frame.dicTable);
			frame.dicTotalProductionsOutputLabel.setText(StringUtils.EMPTY);
		}
	}

	private void calculateCompoundProductions(HunspellerFrame frame){
		String inputText = StringUtils.strip((String)frame.cmpInputComboBox.getEditor().getItem());

		cmpLimitComboBox.setEnabled(StringUtils.isNotBlank(inputText));

		if(formerCompoundInputText != null && formerCompoundInputText.equals(inputText))
			return;
		formerCompoundInputText = inputText;

		frame.limitComboBoxActionPerformed(null);
	}


   private void dicCheckCorrectnessMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicCheckCorrectnessMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		checkDictionaryCorrectness();
   }//GEN-LAST:event_dicCheckCorrectnessMenuItemActionPerformed

   private void dicSortDictionaryMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicSortDictionaryMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		try{
			String[] lines = backbone.getDictionaryLines();
			dicSortDialog.setListData(lines);
			dicSortDialog.setVisible(true);
		}
		catch(IOException e){
			LOGGER.error("Something very bad happend while sorting the dictionary", e);
		}
   }//GEN-LAST:event_dicSortDictionaryMenuItemActionPerformed

   private void dicExtractDuplicatesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicExtractDuplicatesMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		extractDictionaryDuplicates();
   }//GEN-LAST:event_dicExtractDuplicatesMenuItemActionPerformed

   private void dicExtractWordlistMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicExtractWordlistMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		extractDictionaryWordlist(false);
   }//GEN-LAST:event_dicExtractWordlistMenuItemActionPerformed

   private void dicExtractMinimalPairsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicExtractMinimalPairsMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		extractMinimalPairs();
   }//GEN-LAST:event_dicExtractMinimalPairsMenuItemActionPerformed

	private void cmpInputComboBoxKeyReleased(){
		compoundProductionDebouncer.call(this);
	}

   private void theFindDuplicatesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_theFindDuplicatesMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		ThesaurusDuplicatesDialog dialog = new ThesaurusDuplicatesDialog(backbone.getTheParser().extractDuplicates(), this);
		dialog.setCurrentFont();
		GUIUtils.addCancelByEscapeKey(dialog);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
   }//GEN-LAST:event_theFindDuplicatesMenuItemActionPerformed

	private void filterThesaurus(HunspellerFrame frame){
		final String[] searchText = ThesaurusParser.prepareTextForThesaurusFilter(frame.theMeaningsTextField.getText());

		if(formerFilterThesaurusText != null && formerFilterThesaurusText.equals(searchText[1]))
			return;

		//TODO if text to be inserted is already contained into the thesaurus, do nothing

		formerFilterThesaurusText = searchText[1];

		theAddButton.setEnabled(StringUtils.isNotBlank(searchText[1]));

		@SuppressWarnings("unchecked")
		TableRowSorter<ThesaurusTableModel> sorter = (TableRowSorter<ThesaurusTableModel>)frame.theTable.getRowSorter();
		if(StringUtils.isNotBlank(searchText[1]))
			EventQueue.invokeLater(() -> {
				List<RowFilter<Object, Object>> filters = Arrays.asList(RowFilter.regexFilter(searchText[0], 0), RowFilter.regexFilter(searchText[1], 1));
				sorter.setRowFilter(RowFilter.orFilter(filters));
			});
		else
			sorter.setRowFilter(null);
	}

	public void removeSelectedRowsFromThesaurus(){
		try{
			int[] selectedRows = Arrays.stream(theTable.getSelectedRows())
				.map(theTable::convertRowIndexToModel)
				.toArray();
			backbone.getTheParser().deleteMeanings(selectedRows);

			ThesaurusTableModel dm = (ThesaurusTableModel)theTable.getModel();
			dm.fireTableDataChanged();

			updateSynonymsCounter();

			//... and save the files
			backbone.storeThesaurusFiles();
		}
		catch(Exception e){
			String message = ExceptionHelper.getMessage(e);
			LOGGER.info(Backbone.MARKER_APPLICATION, "Deletion error: {}", message);
		}
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
		catch(Exception e){
			LOGGER.error(Backbone.MARKER_APPLICATION, ExceptionHelper.getMessage(e));
		}
   }//GEN-LAST:event_dicWordCountMenuItemActionPerformed

   private void hypCheckCorrectnessMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hypCheckCorrectnessMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		checkHyphenationCorrectness();
   }//GEN-LAST:event_hypCheckCorrectnessMenuItemActionPerformed

   private void filEmptyRecentFilesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filEmptyRecentFilesMenuItemActionPerformed
		recentFilesMenu.clear();

		recentFilesMenu.setEnabled(false);
		filEmptyRecentFilesMenuItem.setEnabled(false);
		filOpenAFFMenuItem.setEnabled(true);
   }//GEN-LAST:event_filEmptyRecentFilesMenuItemActionPerformed

   private void dicRulesReducerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicRulesReducerMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		dicRulesReducerMenuItem.setEnabled(false);
		rulesReducerDialog.setVisible(true);
   }//GEN-LAST:event_dicRulesReducerMenuItemActionPerformed

   private void hypStatisticsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hypStatisticsMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		extractDictionaryStatistics(true);
   }//GEN-LAST:event_hypStatisticsMenuItemActionPerformed

   private void dicExtractWordlistPlainTextMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicExtractWordlistPlainTextMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		extractDictionaryWordlist(true);
   }//GEN-LAST:event_dicExtractWordlistPlainTextMenuItemActionPerformed

   private void hypAddRuleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hypAddRuleButtonActionPerformed
      String newRule = hypAddRuleTextField.getText();
      HyphenationParser.Level level = HyphenationParser.Level.values()[hypAddRuleLevelComboBox.getSelectedIndex()];
      String foundRule = backbone.addHyphenationRule(newRule.toLowerCase(Locale.ROOT), level);
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
         catch(IOException e){
            LOGGER.error("Something very bad happend while adding a rule to the hyphenation file", e);
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

   private void theRedoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_theRedoButtonActionPerformed
      try{
         if(backbone.restoreNextThesaurusSnapshot()){
            updateSynonyms();

            updateSynonymsCounter();
         }
      }
      catch(IOException e){
         LOGGER.error("Something very bad happend while redoing changes to the thesaurus file", e);
      }
   }//GEN-LAST:event_theRedoButtonActionPerformed

   private void theUndoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_theUndoButtonActionPerformed
      try{
         if(backbone.restorePreviousThesaurusSnapshot()){
            updateSynonyms();

            updateSynonymsCounter();
         }
      }
      catch(IOException e){
         LOGGER.error("Something very bad happend while undoing changes to the thesaurus file", e);
      }
   }//GEN-LAST:event_theUndoButtonActionPerformed

   private void theAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_theAddButtonActionPerformed
      try{
         //try adding the meanings
         String synonyms = theMeaningsTextField.getText();
         Supplier<Boolean> duplicatesDiscriminator = () -> {
            int responseOption = JOptionPane.showConfirmDialog(this, "There is a duplicate with same part of speech.\nForce insertion?",
               "Select one", JOptionPane.YES_NO_OPTION);
            return (responseOption == JOptionPane.YES_OPTION);
         };
         DuplicationResult duplicationResult = backbone.getTheParser().insertMeanings(synonyms, duplicatesDiscriminator);
         List<ThesaurusEntry> duplicates = duplicationResult.getDuplicates();

         if(duplicates.isEmpty() || duplicationResult.isForceInsertion()){
            //if everything's ok update the table and the sorter...
            ThesaurusTableModel dm = (ThesaurusTableModel)theTable.getModel();
            dm.fireTableDataChanged();

            formerFilterThesaurusText = null;
            theMeaningsTextField.setText(null);
            theMeaningsTextField.requestFocusInWindow();
            @SuppressWarnings("unchecked")
            TableRowSorter<ThesaurusTableModel> sorter = (TableRowSorter<ThesaurusTableModel>)theTable.getRowSorter();
            sorter.setRowFilter(null);

            updateSynonymsCounter();

            //... and save the files
            backbone.storeThesaurusFiles();
         }
         else{
				theMeaningsTextField.requestFocusInWindow();

				String duplicatedWords = duplicates.stream()
					.map(ThesaurusEntry::getSynonym)
					.collect(Collectors.joining(", "));
				JOptionPane.showOptionDialog(this, "Some duplicates are present, namely:\n   " + duplicatedWords + "\n\nSynonyms was NOT inserted!",
					"Warning!", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
         }
      }
      catch(IllegalArgumentException e){
         LOGGER.info(Backbone.MARKER_APPLICATION, "Insertion error: {}", e.getMessage());
      }
      catch(Exception t){
         String message = ExceptionHelper.getMessage(t);
         LOGGER.info(Backbone.MARKER_APPLICATION, "Insertion error: {}", message);
      }
   }//GEN-LAST:event_theAddButtonActionPerformed

   private void theMeaningsTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_theMeaningsTextFieldKeyReleased
      theFilterDebouncer.call(this);
   }//GEN-LAST:event_theMeaningsTextFieldKeyReleased

   private void loadInputButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmpLoadInputButtonActionPerformed
      extractCompoundRulesInputs();
   }//GEN-LAST:event_cmpLoadInputButtonActionPerformed

   private void limitComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmpLimitComboBoxActionPerformed
      String inputText = StringUtils.strip((String)cmpInputComboBox.getEditor().getItem());
      int limit = Integer.parseInt(cmpLimitComboBox.getItemAt(cmpLimitComboBox.getSelectedIndex()));
      String inputCompounds = cmpInputTextArea.getText();

      if(StringUtils.isNotBlank(inputText)){
         try{
         	//FIXME transfer into backbone
            final List<Production> words;
				final WordGenerator wordGenerator = backbone.getWordGenerator();
            AffixData affixData = backbone.getAffixData();
				if(affixData.getCompoundFlag().equals(inputText)){
               int maxCompounds = affixData.getCompoundMaxWordCount();
               words = wordGenerator.applyCompoundFlag(StringUtils.split(inputCompounds, '\n'), limit, maxCompounds);
            }
            else
            	words = wordGenerator.applyCompoundRules(StringUtils.split(inputCompounds, '\n'), inputText, limit);

            CompoundTableModel dm = (CompoundTableModel)cmpTable.getModel();
            dm.setProductions(words);
         }
         catch(IllegalArgumentException e){
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
		JFontChooserDialog dialog = new JFontChooserDialog(backbone.getAffixData(), GUIUtils.getCurrentFont(), onSelection,
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


	@Override
	public void actionPerformed(ActionEvent event){
		if(event.getSource() == theTable)
			removeSelectedRowsFromThesaurus();
		else{
			//FIXME
			if(prjLoaderWorker != null && prjLoaderWorker.getState() == SwingWorker.StateValue.STARTED){
				prjLoaderWorker.pause();

				final Object[] options = {"Abort", "Cancel"};
				final int answer = JOptionPane.showOptionDialog(this, "Do you really want to abort the project loader task?", "Warning!",
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

			//FIXME
			if(dicDuplicatesWorker != null && dicDuplicatesWorker.getState() == SwingWorker.StateValue.STARTED){
//				dicDuplicatesWorker.pause();

				final Object[] options = {"Abort", "Cancel"};
				final int answer = JOptionPane.showOptionDialog(this, "Do you really want to abort the dictionary duplicates task?", "Warning!",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
				if(answer == JOptionPane.YES_OPTION){
					dicDuplicatesWorker.cancel(true);

					dicExtractDuplicatesMenuItem.setEnabled(true);
					LOGGER.info(Backbone.MARKER_APPLICATION, "Dictionary duplicate extraction aborted");

					dicDuplicatesWorker = null;
				}
				else if(answer == JOptionPane.NO_OPTION || answer == JOptionPane.CLOSED_OPTION){
//					dicDuplicatesWorker.resume();
				}
			}

			checkAbortion(dicCorrectnessWorker, dicCheckCorrectnessMenuItem);

			checkAbortion(dicWordCountWorker, dicWordCountMenuItem);

			checkAbortion(dicStatisticsWorker, dicStatisticsMenuItem, hypStatisticsMenuItem);

			checkAbortion(dicWordlistWorker, dicExtractWordlistMenuItem, dicExtractWordlistPlainTextMenuItem);

			checkAbortion(compoundRulesExtractorWorker, cmpInputComboBox, cmpLimitComboBox, cmpInputTextArea, cmpLoadInputButton);

			checkAbortion(hypCorrectnessWorker, hypCheckCorrectnessMenuItem);
		}
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

	private void loadFile(final String filePath){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		clearResultTextArea();

		backbone.stopFileListener();

		loadFileInternal(filePath);
	}

	@Override
	public void loadFileInternal(final String filePath){
		if(prjLoaderWorker == null || prjLoaderWorker.isDone()){
			dicCheckCorrectnessMenuItem.setEnabled(false);

			prjLoaderWorker = new ProjectLoaderWorker(filePath, backbone, this::loadFileCompleted, this::loadFileCancelled);
			prjLoaderWorker.addPropertyChangeListener(this);
			prjLoaderWorker.execute();

			filOpenAFFMenuItem.setEnabled(false);
		}
	}

	private void loadFileCompleted(){
		backbone.registerFileListener();
		backbone.startFileListener();

		final String language = backbone.getAffixData().getLanguage();
		final String fontFamilyName = preferences.get(FONT_FAMILY_NAME_PREFIX + language, null);
		final String fontSize = preferences.get(FONT_SIZE_PREFIX + language, null);
		final Font lastUsedFont = (fontFamilyName != null && fontSize != null?
			new Font(fontFamilyName, Font.PLAIN, Integer.parseInt(fontSize)):
			JFontChooserDialog.getDefaultFont());
		GUIUtils.setCurrentFont(lastUsedFont, this);

		final Comparator<String> comparator = Comparator.comparingInt(String::length)
			.thenComparing(BaseBuilder.getComparator(language));
		final Comparator<AffixEntry> comparatorAffix = Comparator.comparingInt((AffixEntry entry) -> entry.toString().length())
			.thenComparing((entry0, entry1) -> BaseBuilder.getComparator(language).compare(entry0.toString(), entry1.toString()));
		addSorterToTable(dicTable, comparator, comparatorAffix);

		try{
			filOpenAFFMenuItem.setEnabled(true);
			dicCheckCorrectnessMenuItem.setEnabled(true);
			dicSortDictionaryMenuItem.setEnabled(true);


			//affix file:
			final AffixData affixData = backbone.getAffixData();
			final Set<String> compoundRules = affixData.getCompoundRules();
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


			//hyphenation file:
			if(backbone.getHyphenator() != null){
				hypCheckCorrectnessMenuItem.setEnabled(true);

				hypMenu.setEnabled(true);
				hypStatisticsMenuItem.setEnabled(true);
				setTabbedPaneEnable(mainTabbedPane, hypLayeredPane, true);
			}


			//dictionary file:
			dicSortDialog = new DictionarySortDialog(backbone.getDicParser(), "Select a section from the list:", this);
			GUIUtils.addCancelByEscapeKey(dicSortDialog);
			dicSortDialog.setLocationRelativeTo(this);
			dicSortDialog.addListSelectionListener(e -> {
				if(e.getValueIsAdjusting() && (dicSorterWorker == null || dicSorterWorker.isDone())){
					final int selectedRow = dicSortDialog.getSelectedIndex();
					if(backbone.getDicParser().isInBoundary(selectedRow)){
						dicSortDialog.setVisible(false);

						dicSortDictionaryMenuItem.setEnabled(false);


						dicSorterWorker = new SorterWorker(backbone, selectedRow);
						dicSorterWorker.addPropertyChangeListener(this);
						dicSorterWorker.execute();
					}
				}
			});

			filCreatePackageMenuItem.setEnabled(true);
			filFontMenuItem.setEnabled(true);
			dicMenu.setEnabled(true);
			final int index = setTabbedPaneEnable(mainTabbedPane, dicLayeredPane, true);
			setTabbedPaneEnable(mainTabbedPane, cmpLayeredPane, !compoundRules.isEmpty());
			mainTabbedPane.setSelectedIndex(index);


			//update rule reduced dialog:
			if(rulesReducerDialog == null){
				rulesReducerDialog = new RulesReducerDialog(backbone, this);
				rulesReducerDialog.setLocationRelativeTo(this);
				rulesReducerDialog.addWindowListener(new WindowAdapter(){
					@Override
					public void windowClosed(WindowEvent e){
						dicRulesReducerMenuItem.setEnabled(true);
					}
				});
			}
			else
				rulesReducerDialog.reload();
			rulesReducerDialog.setCurrentFont();


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


			//thesaurus file:
			if(backbone.getTheParser().getSynonymsCounter() > 0){
				addSorterToTable(theTable, comparator, null);

				final ThesaurusTableModel dm = (ThesaurusTableModel) theTable.getModel();
				dm.setSynonyms(backbone.getTheParser().getSynonymsDictionary());
				updateSynonymsCounter();
				theMenu.setEnabled(true);
				setTabbedPaneEnable(mainTabbedPane, theLayeredPane, true);
			}
		}
		catch(final IllegalArgumentException e){
			LOGGER.info(Backbone.MARKER_APPLICATION, e.getMessage());
		}
		catch(final Exception e){
			LOGGER.info(Backbone.MARKER_APPLICATION, "A bad error occurred: {}", ExceptionHelper.getMessage(e));

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

	private void loadFileCancelled(final Exception exc){
		if(exc instanceof ProjectFileNotFoundException)
			//remove the file from the recent files menu
			recentFilesMenu.removeEntry(((ProjectFileNotFoundException)exc).getPath());


		dicCheckCorrectnessMenuItem.setEnabled(false);
		dicSortDictionaryMenuItem.setEnabled(false);
		hypCheckCorrectnessMenuItem.setEnabled(false);


		//affix file:
		cmpInputComboBox.removeAllItems();
		cmpInputComboBox.setEnabled(false);


		//hyphenation file:
		hypMenu.setEnabled(false);
		hypStatisticsMenuItem.setEnabled(false);
		setTabbedPaneEnable(mainTabbedPane, hypLayeredPane, false);


		filCreatePackageMenuItem.setEnabled(false);
		filFontMenuItem.setEnabled(false);
		dicMenu.setEnabled(false);
		setTabbedPaneEnable(mainTabbedPane, cmpLayeredPane, false);


		//aid file:
		clearAidParser();
		//enable combo-box only if an AID file exists
		dicRuleFlagsAidComboBox.setEnabled(false);
		cmpRuleFlagsAidComboBox.setEnabled(false);


		//thesaurus file:
		theMenu.setEnabled(false);
		setTabbedPaneEnable(mainTabbedPane, theLayeredPane, false);
	}

	private void updateSynonymsCounter(){
		theSynonymsRecordedOutputLabel.setText(DictionaryParser.COUNTER_FORMATTER.format(backbone.getTheParser().getSynonymsCounter()));
	}

	private int setTabbedPaneEnable(final JTabbedPane tabbedPane, final Component component, final boolean enabled){
		final int index = tabbedPane.indexOfComponent(component);
		tabbedPane.setEnabledAt(index, enabled);
		return index;
	}


	private void hyphenate(final HunspellerFrame frame){
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

			final Supplier<StringJoiner> sj = () -> new StringJoiner(HyphenationParser.SOFT_HYPHEN, "<html>", "</html>");
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
		frame.hypRulesOutputLabel.setText(String.join(StringUtils.SPACE, rules));

		frame.hypAddRuleTextField.setText(null);
		frame.hypAddRuleLevelComboBox.setEnabled(false);
		frame.hypAddRuleButton.setEnabled(false);
		frame.hypAddRuleSyllabationOutputLabel.setText(null);
		frame.hypAddRuleSyllabesCountOutputLabel.setText(null);
	}

	private void hyphenateAddRule(final HunspellerFrame frame){
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
				ruleMatchesText = addedRuleText.contains(PatternHelper.clear(addedRule, PATTERN_POINTS_AND_NUMBERS_AND_EQUALS_AND_MINUS));

				if(ruleMatchesText){
					final Hyphenation hyphenation = frame.backbone.getHyphenator().hyphenate(addedRuleText);
					final Hyphenation addedRuleHyphenation = frame.backbone.getHyphenator().hyphenate(addedRuleText, addedRule, level);

					final Supplier<StringJoiner> sj = () -> new StringJoiner(HyphenationParser.SOFT_HYPHEN, "<html>", "</html>");
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

			dicCorrectnessWorker = new CorrectnessWorker(backbone.getDicParser(), backbone.getChecker(), backbone.getWordGenerator());
			dicCorrectnessWorker.addPropertyChangeListener(this);
			dicCorrectnessWorker.execute();
		}
	}

	private void extractDictionaryDuplicates(){
		if(dicDuplicatesWorker == null || dicDuplicatesWorker.isDone()){
			final int fileChosen = saveTextFileFileChooser.showSaveDialog(this);
			if(fileChosen == JFileChooser.APPROVE_OPTION){
				dicExtractDuplicatesMenuItem.setEnabled(false);

				final File outputFile = saveTextFileFileChooser.getSelectedFile();
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

			dicWordCountWorker = new WordCountWorker(backbone.getAffParser().getAffixData().getLanguage(), backbone.getDicParser(), backbone.getWordGenerator());
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

			dicStatisticsWorker = new StatisticsWorker(backbone.getAffParser(), backbone.getDicParser(), (performHyphenationStatistics? backbone.getHyphenator(): null),
				backbone.getWordGenerator(), this);
			dicStatisticsWorker.addPropertyChangeListener(this);
			dicStatisticsWorker.execute();
		}
	}

	private void extractDictionaryWordlist(final boolean plainWords){
		if(dicWordlistWorker == null || dicWordlistWorker.isDone()){
			final int fileChosen = saveTextFileFileChooser.showSaveDialog(this);
			if(fileChosen == JFileChooser.APPROVE_OPTION){
				dicExtractWordlistMenuItem.setEnabled(false);
				dicExtractWordlistPlainTextMenuItem.setEnabled(false);

				final File outputFile = saveTextFileFileChooser.getSelectedFile();
				dicWordlistWorker = new WordlistWorker(backbone.getDicParser(), backbone.getWordGenerator(), plainWords, outputFile);
				dicWordlistWorker.addPropertyChangeListener(this);
				dicWordlistWorker.execute();
			}
		}
	}

	private void extractMinimalPairs(){
		if(dicMinimalPairsWorker == null || dicMinimalPairsWorker.isDone()){
			final int fileChosen = saveTextFileFileChooser.showSaveDialog(this);
			if(fileChosen == JFileChooser.APPROVE_OPTION){
				dicExtractMinimalPairsMenuItem.setEnabled(false);

				final File outputFile = saveTextFileFileChooser.getSelectedFile();
				dicMinimalPairsWorker = new MinimalPairsWorker(backbone.getAffixData().getLanguage(), backbone.getDicParser(), backbone.getChecker(),
					backbone.getWordGenerator(), outputFile);
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
			compoundRulesExtractorWorker = new CompoundRulesWorker(backbone.getDicParser(), backbone.getWordGenerator(), productionReader, completed);
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


	private void updateSynonyms(){
		final ThesaurusTableModel dm = (ThesaurusTableModel)theTable.getModel();
		dm.fireTableDataChanged();
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
	}

	private void clearDictionaryCompoundFields(){
		cmpInputComboBox.setEnabled(true);
		cmpInputTextArea.setText(null);
		cmpInputTextArea.setEnabled(true);
		cmpLoadInputButton.setEnabled(true);
	}

	public void clearOutputTable(JTable table){
		final HunspellerTableModel<?> dm = (HunspellerTableModel<?>)table.getModel();
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
		setTabbedPaneEnable(mainTabbedPane, theLayeredPane, false);
	}


	private void clearResultTextArea(){
		parsingResultTextArea.setText(null);
	}

	@Override
	public void onUndoChange(boolean canUndo){
		theUndoButton.setEnabled(canUndo);
	}

	@Override
	public void onRedoChange(boolean canRedo){
		theRedoButton.setEnabled(canRedo);
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

	private void writeObject(ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

	private void readObject(ObjectInputStream is) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}


	public static void main(String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e){
			LOGGER.error(null, e);
		}

		//create and display the form
		EventQueue.invokeLater(() -> (new HunspellerFrame()).setVisible(true));
	}

   // Variables declaration - do not modify//GEN-BEGIN:variables
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
   private javax.swing.JMenuItem filEmptyRecentFilesMenuItem;
   private javax.swing.JMenuItem filExitMenuItem;
   private javax.swing.JMenuItem filFontMenuItem;
   private javax.swing.JPopupMenu.Separator filFontSeparator;
   private javax.swing.JMenu filMenu;
   private javax.swing.JMenuItem filOpenAFFMenuItem;
   private javax.swing.JPopupMenu.Separator filRecentFilesSeparator;
   private javax.swing.JPopupMenu.Separator filSeparator;
   private javax.swing.JMenuItem hlpAboutMenuItem;
   private javax.swing.JMenu hlpMenu;
   private javax.swing.JMenuItem hlpOnlineHelpMenuItem;
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
   private javax.swing.JScrollPane parsingResultScrollPane;
   private javax.swing.JTextArea parsingResultTextArea;
   private javax.swing.JButton theAddButton;
   private javax.swing.JMenuItem theFindDuplicatesMenuItem;
   private javax.swing.JLayeredPane theLayeredPane;
   private javax.swing.JLabel theMeaningsLabel;
   private javax.swing.JTextField theMeaningsTextField;
   private javax.swing.JMenu theMenu;
   private javax.swing.JButton theRedoButton;
   private javax.swing.JScrollPane theScrollPane;
   private javax.swing.JLabel theSynonymsRecordedLabel;
   private javax.swing.JLabel theSynonymsRecordedOutputLabel;
   private javax.swing.JTable theTable;
   private javax.swing.JButton theUndoButton;
   // End of variables declaration//GEN-END:variables

}
