package unit731.hunspeller;

import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.MenuSelectionManager;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableRowSorter;
import javax.swing.text.DefaultCaret;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.interfaces.Undoable;
import unit731.hunspeller.languages.builders.DictionaryParserBuilder;
import unit731.hunspeller.gui.DictionarySortCellRenderer;
import unit731.hunspeller.gui.ProductionTableModel;
import unit731.hunspeller.gui.RecentFileMenu;
import unit731.hunspeller.gui.ThesaurusTableModel;
import unit731.hunspeller.gui.ThesaurusTableRenderer;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.aid.AidParser;
import unit731.hunspeller.parsers.dictionary.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.RuleProductionEntry;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.workers.CorrectnessWorker;
import unit731.hunspeller.parsers.dictionary.workers.DuplicatesWorker;
import unit731.hunspeller.parsers.dictionary.workers.MinimalPairsWorker;
import unit731.hunspeller.parsers.dictionary.workers.SorterWorker;
import unit731.hunspeller.parsers.dictionary.workers.WordlistWorker;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.thesaurus.ThesaurusParser;
import unit731.hunspeller.parsers.thesaurus.DuplicationResult;
import unit731.hunspeller.parsers.hyphenation.Hyphenation;
import unit731.hunspeller.parsers.thesaurus.MeaningEntry;
import unit731.hunspeller.parsers.thesaurus.ThesaurusEntry;
import unit731.hunspeller.services.Debouncer;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.filelistener.FileListenerManager;
import unit731.hunspeller.services.PatternService;
import unit731.hunspeller.services.RecentItems;
import unit731.hunspeller.services.ZipManager;
import unit731.hunspeller.services.filelistener.FileChangeListener;


/**
 * @see <a href="http://manpages.ubuntu.com/manpages/trusty/man4/hunspell.4.html">Hunspell 4</a>
 * @see <a href="https://github.com/lopusz/hunspell-stemmer">Hunspell stemmer on github</a>
 */
@Slf4j
public class HunspellerFrame extends JFrame implements ActionListener, FileChangeListener, PropertyChangeListener, Resultable, Undoable{

	private static final long serialVersionUID = 6772959670167531135L;

	private static final String EXTENSION_AFF = ".aff";
	private static final String EXTENSION_DIC = ".dic";
	private static final String EXTENSION_AID = ".aid";

	private static final Matcher REGEX_POINTS_AND_NUMBERS = PatternService.matcher("[.\\d]");

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static final DecimalFormat COUNTER_FORMATTER = (DecimalFormat)NumberFormat.getInstance(Locale.US);
	static{
		DecimalFormatSymbols symbols = COUNTER_FORMATTER.getDecimalFormatSymbols();
		symbols.setGroupingSeparator(' ');
		COUNTER_FORMATTER.setDecimalFormatSymbols(symbols);
	}

	private static final ZipManager ZIPPER = new ZipManager();

	private File affFile;

	private static String formerInputText;
	private static String formerFilterThesaurusText;
	private static String formerHyphenationText;
	private final JFileChooser openAffixFileFileChooser;
	private final JFileChooser saveTextFileFileChooser;
	private DictionarySortDialog dicDialog;

	private final AffixParser affParser;
	private final AidParser aidParser;
	private DictionaryParser dicParser;
	private final ThesaurusParser theParser;
	private HyphenationParser hypParser;

	private final WordGenerator wordGenerator;
	private final Debouncer<HunspellerFrame> productionDebouncer = new Debouncer<>(HunspellerFrame::calculateProductions, 400);
	private final Debouncer<HunspellerFrame> theFilterDebouncer = new Debouncer<>(HunspellerFrame::filterThesaurus, 400);
	private final Debouncer<HunspellerFrame> hypDebouncer = new Debouncer<>(HunspellerFrame::hyphenate, 400);
	private final Debouncer<HunspellerFrame> hypAddRuleDebouncer = new Debouncer<>(HunspellerFrame::hyphenateAddRule, 400);
	private final FileListenerManager flm;

	private CorrectnessWorker dicCorrectnessWorker;
	private DuplicatesWorker dicDuplicatesWorker;
	private SorterWorker dicSorterWorker;
	private WordlistWorker dicWordlistWorker;
	private MinimalPairsWorker dicMinimalPairsWorker;
	private SwingWorker<List<ThesaurusEntry>, String> theParserWorker;
	private SwingWorker<Void, String> hypParserWorker;
	private final Map<Class<?>, Runnable> enableMenuItemFromWorker = new HashMap<>();

	private static RecentItems recentItems;


	public HunspellerFrame(){
		Preferences preferences = Preferences.userNodeForPackage(HunspellerFrame.class);
		recentItems = new RecentItems(5, preferences);

		affParser = new AffixParser();
		aidParser = new AidParser();
		theParser = new ThesaurusParser(this);
		wordGenerator = new WordGenerator(affParser);
		flm = new FileListenerManager();

		initComponents();

//File currentDir = new File("C:\\Users\\mauro\\Projects\\VenetanHTML5App\\public_html\\app\\tools\\lang\\data\\dict-vec-oxt\\dictionaries");
//File currentDir = new File("C:\\ApacheHTDocs\\VenetanHTML5App\\public_html\\app\\tools\\lang\\data\\dict-vec-oxt\\dictionaries");
		File currentDir = new File(".");
		openAffixFileFileChooser = new JFileChooser();
		openAffixFileFileChooser.setFileFilter(new FileNameExtensionFilter("AFF files", "aff"));
		openAffixFileFileChooser.setCurrentDirectory(currentDir);

		saveTextFileFileChooser = new JFileChooser();
		saveTextFileFileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
		saveTextFileFileChooser.setCurrentDirectory(currentDir);

		enableMenuItemFromWorker.put(ThesaurusParser.ParserWorker.class, () -> theMenu.setEnabled(true));
		enableMenuItemFromWorker.put(CorrectnessWorker.class, () -> {
			dicCheckCorrectnessMenuItem.setEnabled(true);
			dicSortDictionaryMenuItem.setEnabled(true);
		});
		enableMenuItemFromWorker.put(DuplicatesWorker.class, () -> {
			dicExtractDuplicatesMenuItem.setEnabled(true);
			dicSortDictionaryMenuItem.setEnabled(true);
		});
		enableMenuItemFromWorker.put(SorterWorker.class, () -> dicSortDictionaryMenuItem.setEnabled(true));
		enableMenuItemFromWorker.put(WordlistWorker.class, () -> {
			dicExtractWordlistMenuItem.setEnabled(true);
			dicSortDictionaryMenuItem.setEnabled(true);
		});
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
      dicRuleTagsAidLabel = new javax.swing.JLabel();
      dicRuleTagsAidComboBox = new javax.swing.JComboBox<>();
      dicScrollPane = new javax.swing.JScrollPane();
      dicTable = new javax.swing.JTable();
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
      hypAddRuleLabel = new javax.swing.JLabel();
      hypAddRuleTextField = new javax.swing.JTextField();
      hypAddRuleButton = new javax.swing.JButton();
      hypAddRuleSyllabationLabel = new javax.swing.JLabel();
      hypAddRuleSyllabationOutputLabel = new javax.swing.JLabel();
      hypAddRuleSyllabesCountLabel = new javax.swing.JLabel();
      hypAddRuleSyllabesCountOutputLabel = new javax.swing.JLabel();
      mainMenuBar = new javax.swing.JMenuBar();
      fileMenu = new javax.swing.JMenu();
      fileOpenAFFMenuItem = new javax.swing.JMenuItem();
      fileCreatePackageMenuItem = new javax.swing.JMenuItem();
      recentFilesFileSeparator = new javax.swing.JPopupMenu.Separator();
      fileSeparator = new javax.swing.JPopupMenu.Separator();
      fileExitMenuItem = new javax.swing.JMenuItem();
      dicMenu = new javax.swing.JMenu();
      dicCheckCorrectnessMenuItem = new javax.swing.JMenuItem();
      dicSortDictionaryMenuItem = new javax.swing.JMenuItem();
      dicExtractDuplicatesMenuItem = new javax.swing.JMenuItem();
      dicSeparator = new javax.swing.JPopupMenu.Separator();
      dicExtractWordlistMenuItem = new javax.swing.JMenuItem();
      dicExtractMinimalPairsMenuItem = new javax.swing.JMenuItem();
      theMenu = new javax.swing.JMenu();
      theFindDuplicatesMenuItem = new javax.swing.JMenuItem();
      hlpMenu = new javax.swing.JMenu();
      hlpAboutMenuItem = new javax.swing.JMenuItem();

      setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
      setTitle("Hunspeller");
      setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/favicon.jpg")));

      parsingResultTextArea.setEditable(false);
      parsingResultTextArea.setColumns(20);
      parsingResultTextArea.setRows(5);
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

      dicRuleTagsAidLabel.setLabelFor(dicRuleTagsAidComboBox);
      dicRuleTagsAidLabel.setText("Rule tags aid:");

      dicTable.setModel(new ProductionTableModel());
      dicTable.setShowHorizontalLines(false);
      dicTable.setShowVerticalLines(false);
      dicTable.setRowSelectionAllowed(true);
      dicScrollPane.setViewportView(dicTable);

      dicLayeredPane.setLayer(dicInputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(dicInputTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(dicRuleTagsAidLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(dicRuleTagsAidComboBox, javax.swing.JLayeredPane.DEFAULT_LAYER);
      dicLayeredPane.setLayer(dicScrollPane, javax.swing.JLayeredPane.DEFAULT_LAYER);

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
                     .addComponent(dicRuleTagsAidLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(dicLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(dicRuleTagsAidComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                     .addComponent(dicInputTextField)))
               .addComponent(dicScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 904, Short.MAX_VALUE))
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
               .addComponent(dicRuleTagsAidComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(dicRuleTagsAidLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(dicScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)
            .addContainerGap())
      );

      mainTabbedPane.addTab("Dictionary", dicLayeredPane);

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
      theTable.setRowSorter(new TableRowSorter<ThesaurusTableModel>((ThesaurusTableModel)theTable.getModel()));
      theTable.setShowHorizontalLines(false);
      theTable.setShowVerticalLines(false);
      theTable.setRowSelectionAllowed(true);
      theTable.getColumnModel().getColumn(0).setMinWidth(200);
      theTable.getColumnModel().getColumn(0).setMaxWidth(500);

      KeyStroke cancelKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
      theTable.registerKeyboardAction(this, cancelKeyStroke, JComponent.WHEN_FOCUSED);

      JFrame parent = this;
      theTable.addMouseListener(new MouseAdapter(){
         public void mouseClicked(MouseEvent e){
            if(e.getClickCount() == 1){
               JTable target = (JTable)e.getSource();
               int col = target.getSelectedColumn();
               if(col == 1){
                  int row = theTable.convertRowIndexToModel(target.getSelectedRow());
                  BiConsumer<List<MeaningEntry>, String> okButtonAction = (meanings, text) -> {
                     try{
                        theParser.setMeanings(row, meanings, text);

                        // ... and save the files
                        saveThesaurusFiles();
                     }
                     catch(IllegalArgumentException | IOException ex){
                        printResultLine(ex.getMessage());
                     }
                  };
                  ThesaurusEntry synonym = theParser.getSynonymsDictionary().get(row);
                  ThesaurusMeaningsDialog dialog = new ThesaurusMeaningsDialog(parent, synonym, okButtonAction, (Resultable)parent);
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

      hypWordTextField.setNextFocusableComponent(hypAddRuleTextField);
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

      hypAddRuleLabel.setLabelFor(hypAddRuleTextField);
      hypAddRuleLabel.setText("Add rule:");

      hypAddRuleTextField.setEnabled(false);
      hypAddRuleTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            hypAddRuleTextFieldKeyReleased(evt);
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
      hypLayeredPane.setLayer(hypAddRuleLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      hypLayeredPane.setLayer(hypAddRuleTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
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
                  .addComponent(hypAddRuleSyllabationOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
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
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(hypSyllabationLabel)
               .addComponent(hypSyllabationOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypSyllabesCountLabel)
               .addComponent(hypSyllabesCountOutputLabel))
            .addGap(18, 18, 18)
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypAddRuleLabel)
               .addComponent(hypAddRuleTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(hypAddRuleButton))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypAddRuleSyllabationLabel)
               .addComponent(hypAddRuleSyllabationOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(hypLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypAddRuleSyllabesCountLabel)
               .addComponent(hypAddRuleSyllabesCountOutputLabel))
            .addContainerGap(87, Short.MAX_VALUE))
      );

      mainTabbedPane.addTab("Hyphenation", hypLayeredPane);

      addWindowListener(new WindowAdapter(){
         @Override
         public void windowClosing(WindowEvent e){
            exit();
         }
      });

      fileMenu.setMnemonic('F');
      fileMenu.setText("File");
      fileMenu.setToolTipText("");

      fileOpenAFFMenuItem.setMnemonic('A');
      fileOpenAFFMenuItem.setText("Open AFF file...");
      fileOpenAFFMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            fileOpenAFFMenuItemActionPerformed(evt);
         }
      });
      fileMenu.add(fileOpenAFFMenuItem);

      fileCreatePackageMenuItem.setMnemonic('p');
      fileCreatePackageMenuItem.setText("Create package");
      fileCreatePackageMenuItem.setEnabled(false);
      fileCreatePackageMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            fileCreatePackageMenuItemActionPerformed(evt);
         }
      });
      fileMenu.add(fileCreatePackageMenuItem);
      fileMenu.add(recentFilesFileSeparator);
      fileMenu.add(fileSeparator);

      fileExitMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/file_exit.png"))); // NOI18N
      fileExitMenuItem.setMnemonic('x');
      fileExitMenuItem.setText("Exit");
      fileExitMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            fileExitMenuItemActionPerformed(evt);
         }
      });
      fileMenu.add(fileExitMenuItem);

      mainMenuBar.add(fileMenu);
      RecentFileMenu rfm = new RecentFileMenu(recentItems, this::loadFile);
      rfm.setText("Recent files");
      rfm.setMnemonic('R');
      fileMenu.add(rfm, 3);

      dicMenu.setMnemonic('D');
      dicMenu.setText("Dictionary tools");
      dicMenu.setToolTipText("");
      dicMenu.setEnabled(false);

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

      dicExtractDuplicatesMenuItem.setMnemonic('d');
      dicExtractDuplicatesMenuItem.setText("Extract duplicates...");
      dicExtractDuplicatesMenuItem.setToolTipText("");
      dicExtractDuplicatesMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            dicExtractDuplicatesMenuItemActionPerformed(evt);
         }
      });
      dicMenu.add(dicExtractDuplicatesMenuItem);
      dicMenu.add(dicSeparator);

      dicExtractWordlistMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/dictionary_extractWordlist.png"))); // NOI18N
      dicExtractWordlistMenuItem.setMnemonic('w');
      dicExtractWordlistMenuItem.setText("Extract wordlist...");
      dicExtractWordlistMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            dicExtractWordlistMenuItemActionPerformed(evt);
         }
      });
      dicMenu.add(dicExtractWordlistMenuItem);

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

      theFindDuplicatesMenuItem.setText("Find duplicates");
      theFindDuplicatesMenuItem.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            theFindDuplicatesMenuItemActionPerformed(evt);
         }
      });
      theMenu.add(theFindDuplicatesMenuItem);

      mainMenuBar.add(theMenu);

      hlpMenu.setMnemonic('H');
      hlpMenu.setText("Help");

      hlpAboutMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/about.png"))); // NOI18N
      hlpAboutMenuItem.setMnemonic('A');
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
               .addComponent(mainProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
               .addComponent(mainTabbedPane)
               .addComponent(parsingResultScrollPane, javax.swing.GroupLayout.Alignment.TRAILING))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(parsingResultScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(mainProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addComponent(mainTabbedPane)
            .addContainerGap())
      );

      mainTabbedPane.setEnabledAt(0, false);
      mainTabbedPane.setEnabledAt(1, false);
      mainTabbedPane.setEnabledAt(2, false);
      KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
      mainTabbedPane.registerKeyboardAction(this, escapeKeyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

      pack();
      setLocationRelativeTo(null);
   }// </editor-fold>//GEN-END:initComponents


   private void fileOpenAFFMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileOpenAFFMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		int fileSelected = openAffixFileFileChooser.showOpenDialog(this);
		if(fileSelected == JFileChooser.APPROVE_OPTION){
			recentItems.push(openAffixFileFileChooser.getSelectedFile().getAbsolutePath());

			affFile = openAffixFileFileChooser.getSelectedFile();

			clearResultTextArea();

			openAffixFile();

			openAidFile();

			openThesaurusFile();

			hypParser = new HyphenationParser(affParser.getLanguage());

			openHyphenationFile();
		}
   }//GEN-LAST:event_fileOpenAFFMenuItemActionPerformed

   private void fileCreatePackageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileCreatePackageMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		//go up directories until description.xml or install.rdf is found
		boolean found = false;
		Path parentPath = affFile.toPath().getParent();
		while(true){
			File[] files = parentPath.toFile().listFiles();
			if(files == null)
				break;

			found = Arrays.stream(files)
				.map(File::getName)
				.anyMatch(name -> "description.xml".equals(name) || "install.rdf".equals(name));
			if(found)
				break;

			parentPath = parentPath.getParent();
		}

		//package entire folder with ZIP
		if(found){
			printResultLine("Found base path on " + parentPath.toString());

			try{
				String outputFilename = parentPath.toString() + File.separator + parentPath.getName(parentPath.getNameCount() - 1) + ".zip";
				ZIPPER.zipDirectory(parentPath.toFile(), Deflater.BEST_COMPRESSION, outputFilename);

				printResultLine("Compression done");

				//open directory
				if(Desktop.isDesktopSupported())
					Desktop.getDesktop().open(new File(parentPath.toString()));
			}
			catch(IOException e){
				log.error("Something very bad happend while creating package", e);

				printResultLine("Compression error: " + e.getMessage());
			}
		}
   }//GEN-LAST:event_fileCreatePackageMenuItemActionPerformed

   private void fileExitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileExitMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		exit();
   }//GEN-LAST:event_fileExitMenuItemActionPerformed

	private void exit(){
		if(theParser.isDictionaryModified()){
			//there are unsaved synonyms, ask the user if he really want to quit
			Object[] options ={"Quit", "Cancel"};
			int answer = JOptionPane.showOptionDialog(this, "There are unsaved synonyms in the thesaurus.\nWhat would you like to do?", "Warning!", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
			if(answer == JOptionPane.YES_OPTION)
				dispose();
			else if(answer == JOptionPane.NO_OPTION || answer == JOptionPane.CLOSED_OPTION)
				setDefaultCloseOperation(HunspellerFrame.DO_NOTHING_ON_CLOSE);
		}
		else
			dispose();
	}

   private void hlpAboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hlpAboutMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		HelpDialog dialog = new HelpDialog(this);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
   }//GEN-LAST:event_hlpAboutMenuItemActionPerformed


	private static void calculateProductions(HunspellerFrame frame){
		String inputText = frame.dicInputTextField.getText();
		inputText = StringUtils.strip(inputText);
		if(formerInputText != null && formerInputText.equals(inputText))
			return;
		formerInputText = inputText;

		if(StringUtils.isNotBlank(inputText)){
			FlagParsingStrategy strategy = frame.affParser.getFlagParsingStrategy();
			DictionaryEntry dicEntry = new DictionaryEntry(inputText, strategy);
			try{
				List<RuleProductionEntry> productions = frame.wordGenerator.applyRules(dicEntry);

				ProductionTableModel dm = (ProductionTableModel)frame.dicTable.getModel();
				dm.setProductions(productions);
			}
			catch(IllegalArgumentException e){
				frame.printResultLine(e.getMessage() + " for input " + inputText);
			}
		}
		else
			frame.clearOutputTable();
	}


   private void dicCheckCorrectnessMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicCheckCorrectnessMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		checkDictionaryCorrectness(affParser);

		dicCheckCorrectnessMenuItem.setEnabled(false);
		dicSortDictionaryMenuItem.setEnabled(false);
   }//GEN-LAST:event_dicCheckCorrectnessMenuItemActionPerformed

   private void dicSortDictionaryMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicSortDictionaryMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();
		dicSortDictionaryMenuItem.setEnabled(false);

		try{
			File dicFile = getDictionaryFile();
			String[] lines = Files.lines(dicFile.toPath(), affParser.getCharset())
				.map(line -> StringUtils.replace(line, "\t", "   "))
				.toArray(String[]::new);
			dicDialog.setListData(lines);
			dicDialog.setVisible(true);
		}
		catch(IOException e){
			log.error("Something very bad happend while sorting the dictionary", e);
		}

		dicSortDictionaryMenuItem.setEnabled(true);
   }//GEN-LAST:event_dicSortDictionaryMenuItemActionPerformed

   private void dicExtractDuplicatesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicExtractDuplicatesMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		extractDictionaryDuplicates(affParser);
   }//GEN-LAST:event_dicExtractDuplicatesMenuItemActionPerformed

   private void dicExtractWordlistMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicExtractWordlistMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		extractDictionaryWordlist();
   }//GEN-LAST:event_dicExtractWordlistMenuItemActionPerformed

   private void dicExtractMinimalPairsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dicExtractMinimalPairsMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		extractMinimalPairs();
   }//GEN-LAST:event_dicExtractMinimalPairsMenuItemActionPerformed

	private void dicInputTextFieldKeyReleased(java.awt.event.KeyEvent evt){//GEN-FIRST:event_dicInputTextFieldKeyReleased
		productionDebouncer.call(this);
	}//GEN-LAST:event_dicInputTextFieldKeyReleased


   private void theFindDuplicatesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_theFindDuplicatesMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		ThesaurusDuplicatesDialog dialog = new ThesaurusDuplicatesDialog(this, theParser.extractDuplicates());
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
   }//GEN-LAST:event_theFindDuplicatesMenuItemActionPerformed

	private void theAddButtonActionPerformed(java.awt.event.ActionEvent evt){//GEN-FIRST:event_theAddButtonActionPerformed
		try{
			//try adding the meanings
			String synonyms = theMeaningsTextField.getText();
			Supplier<Boolean> duplicatesDiscriminator = () -> {
				int responseOption = JOptionPane.showConfirmDialog(this, "There is a duplicate with the same part of speech.\nForce insertion?",
					"Please select one", JOptionPane.YES_NO_OPTION);
				return (responseOption == JOptionPane.YES_OPTION);
			};
			DuplicationResult duplicationResult = theParser.insertMeanings(synonyms, duplicatesDiscriminator);
			List<ThesaurusEntry> duplicates = duplicationResult.getDuplicates();

			if(duplicationResult.isForcedInsertion() || duplicates.isEmpty()){
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
				saveThesaurusFiles();
			}
			else{
				theMeaningsTextField.requestFocusInWindow();

				String duplicatedWords = String.join(", ", duplicates.stream().map(ThesaurusEntry::getSynonym).collect(Collectors.toList()));
				JOptionPane.showOptionDialog(this, "Some duplicates are present, namely:\n   " + duplicatedWords + "\n\nSynonyms was NOT inserted!", "Warning!", -1,
					JOptionPane.WARNING_MESSAGE, null, null, null);
			}
		}
		catch(ArrayIndexOutOfBoundsException | IllegalArgumentException | IOException e){
			printResultLine("Insertion error: " + e.getMessage());
		}
		catch(Exception e){
			String message = ExceptionService.getMessage(e, getClass());
			printResultLine("Insertion error: " + message);
		}
	}//GEN-LAST:event_theAddButtonActionPerformed

	private void theMeaningsTextFieldKeyReleased(java.awt.event.KeyEvent evt){//GEN-FIRST:event_theMeaningsTextFieldKeyReleased
		String text = theMeaningsTextField.getText();
		theAddButton.setEnabled(text != null && !text.isEmpty());

		theFilterDebouncer.call(this);
	}//GEN-LAST:event_theMeaningsTextFieldKeyReleased

	private static void filterThesaurus(HunspellerFrame frame){
		String text = StringUtils.strip(frame.theMeaningsTextField.getText());
		if(formerFilterThesaurusText != null && formerFilterThesaurusText.equals(text))
			return;
		formerFilterThesaurusText = text;

		@SuppressWarnings("unchecked")
		TableRowSorter<ThesaurusTableModel> sorter = (TableRowSorter<ThesaurusTableModel>)frame.theTable.getRowSorter();
		if(StringUtils.isNotBlank(text))
			EventQueue.invokeLater(() -> {
				String filterText = frame.dicParser.prepareTextForFilter(text);
				sorter.setRowFilter(RowFilter.regexFilter(filterText));
			});
		else
			sorter.setRowFilter(null);
	}

	public void removeSelectedRowsFromThesaurus(){
		try{
			int[] selectedRows = Arrays.stream(theTable.getSelectedRows())
				.map(theTable::convertRowIndexToModel)
				.toArray();
			theParser.deleteMeanings(selectedRows);

			ThesaurusTableModel dm = (ThesaurusTableModel)theTable.getModel();
			dm.fireTableDataChanged();

			updateSynonymsCounter();

			//... and save the files
			saveThesaurusFiles();
		}
		catch(ArrayIndexOutOfBoundsException | IllegalArgumentException | IOException e){
			printResultLine("Deletion error: " + e.getMessage());
		}
		catch(Exception e){
			String message = ExceptionService.getMessage(e, getClass());
			printResultLine("Deletion error: " + message);
		}
	}

   private void theUndoButtonActionPerformed(java.awt.event.ActionEvent evt){//GEN-FIRST:event_theUndoButtonActionPerformed
		try{
			if(theParser.restorePreviousSnapshot()){
				updateSynonyms();

				updateSynonymsCounter();

				//save the files
				saveThesaurusFiles();
			}
		}
		catch(IOException e){
			log.error("Something very bad happend while undoing changes to the thesaurus file", e);
		}
   }//GEN-LAST:event_theUndoButtonActionPerformed

   private void theRedoButtonActionPerformed(java.awt.event.ActionEvent evt){//GEN-FIRST:event_theRedoButtonActionPerformed
		try{
			if(theParser.restoreNextSnapshot()){
				updateSynonyms();

				updateSynonymsCounter();

				//save the files
				saveThesaurusFiles();
			}
		}
		catch(IOException e){
			log.error("Something very bad happend while redoing changes to the thesaurus file", e);
		}
   }//GEN-LAST:event_theRedoButtonActionPerformed

	private void saveThesaurusFiles() throws IOException{
		String language = affParser.getLanguage();
		File thesIndexFile = getFile("th_" + language + "_v2.idx");
		File thesDataFile = getFile("th_" + language + "_v2.dat");
		theParser.save(thesIndexFile, thesDataFile);
	}


	private void hypWordTextFieldKeyReleased(java.awt.event.KeyEvent evt){//GEN-FIRST:event_hypWordTextFieldKeyReleased
		hypDebouncer.call(this);
	}//GEN-LAST:event_hypWordTextFieldKeyReleased

   private void hypAddRuleTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_hypAddRuleTextFieldKeyReleased
		hypAddRuleDebouncer.call(this);
   }//GEN-LAST:event_hypAddRuleTextFieldKeyReleased

   private void hypAddRuleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hypAddRuleButtonActionPerformed
		String newRule = hypAddRuleTextField.getText();
		String foundRule = hypParser.addRule(newRule.toLowerCase(Locale.ROOT));
		if(foundRule == null){
			try{
				File hypFile = getHyphenationFile();
				hypParser.save(hypFile);

				if(hypWordTextField.getText() != null){
					formerHyphenationText = null;
					hyphenate(this);
				}

				hypAddRuleButton.setEnabled(false);
				hypAddRuleTextField.setText(null);
				hypAddRuleSyllabationOutputLabel.setText(null);
				hypAddRuleSyllabesCountOutputLabel.setText(null);
			}
			catch(IOException e){
				log.error("Something very bad happend while adding a rule to the hyphenation file", e);
			}
		}
		else{
			hypAddRuleTextField.requestFocusInWindow();

			printResultLine("Duplicated rule found (" + foundRule + "), cannot insert " + newRule);
		}
   }//GEN-LAST:event_hypAddRuleButtonActionPerformed


	@Override
	public void actionPerformed(ActionEvent event){
		if(event.getSource() == theTable)
			removeSelectedRowsFromThesaurus();
		else{
			if(dicCorrectnessWorker != null && dicCorrectnessWorker.getState() == SwingWorker.StateValue.STARTED){
				Object[] options ={"Abort", "Cancel"};
				int answer = JOptionPane.showOptionDialog(this, "Do you really want to abort the dictionary correctness task?", "Warning!", JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
				if(answer == JOptionPane.YES_OPTION){
					dicCorrectnessWorker.cancel(true);
					dicCorrectnessWorker = null;

					dicCheckCorrectnessMenuItem.setEnabled(true);
					dicSortDictionaryMenuItem.setEnabled(true);
					printResultLine("Dictionary correctness check aborted");
				}
				else if(answer == JOptionPane.NO_OPTION || answer == JOptionPane.CLOSED_OPTION)
					setDefaultCloseOperation(HunspellerFrame.DO_NOTHING_ON_CLOSE);
			}
			if(dicDuplicatesWorker != null && dicDuplicatesWorker.getState() == SwingWorker.StateValue.STARTED){
				Object[] options ={"Abort", "Cancel"};
				int answer = JOptionPane.showOptionDialog(this, "Do you really want to abort the dictionary correctness task?", "Warning!", JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
				if(answer == JOptionPane.YES_OPTION){
					dicDuplicatesWorker.cancel(true);
					dicDuplicatesWorker = null;

					dicExtractDuplicatesMenuItem.setEnabled(true);
					dicSortDictionaryMenuItem.setEnabled(true);
					printResultLine("Dictionary duplicate extraction aborted");
				}
				else if(answer == JOptionPane.NO_OPTION || answer == JOptionPane.CLOSED_OPTION)
					setDefaultCloseOperation(HunspellerFrame.DO_NOTHING_ON_CLOSE);
			}
			if(dicWordlistWorker != null && dicWordlistWorker.getState() == SwingWorker.StateValue.STARTED){
				Object[] options ={"Abort", "Cancel"};
				int answer = JOptionPane.showOptionDialog(this, "Do you really want to abort the wordlist extraction task?", "Warning!", JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
				if(answer == JOptionPane.YES_OPTION){
					dicWordlistWorker.cancel(true);
					dicWordlistWorker = null;

					dicExtractWordlistMenuItem.setEnabled(true);
					dicSortDictionaryMenuItem.setEnabled(true);
					printResultLine("Dictionary wordlist extraction aborted");
				}
				else if(answer == JOptionPane.NO_OPTION || answer == JOptionPane.CLOSED_OPTION)
					setDefaultCloseOperation(HunspellerFrame.DO_NOTHING_ON_CLOSE);
			}
		}
	}

	private void loadFile(String filePath){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		affFile = new File(filePath);

		clearResultTextArea();

		openAffixFile();

		openAidFile();

		openThesaurusFile();

		hypParser = new HyphenationParser(affParser.getLanguage());

		openHyphenationFile();
	}

	public void clearOutputTable(){
		ProductionTableModel dm = (ProductionTableModel)dicTable.getModel();
		dm.setProductions(null);
	}


	@Override
	public void fileDeleted(Path path){
		printResultLine("File " + path.toFile().getName() + " deleted");

		String absolutePath = path.toString().toLowerCase();
		if(hasAFFExtension(absolutePath))
			clearAffixFile();
		else if(hasAIDExtension(absolutePath))
			clearAidFile();
	}

	@Override
	public void fileModified(Path path){
//		printResultLine("File " + path.toFile().getName() + " modified");

		String absolutePath = path.toString().toLowerCase();
		if(hasAFFExtension(absolutePath))
			openAffixFile();
		else if(hasAIDExtension(absolutePath))
			openAidFile();
		else if(isHyphenationFile(absolutePath)){
			openHyphenationFile();

			//clear hyphenation
			formerHyphenationText = null;
			clearHyphenation();
		}
	}

	private boolean hasAFFExtension(String path){
		return path.endsWith(EXTENSION_AFF);
	}

	private boolean hasAIDExtension(String path){
		return path.endsWith(EXTENSION_AID);
	}

	private boolean isHyphenationFile(String path){
		String baseName = FilenameUtils.getBaseName(path);
		return (baseName.startsWith("hyph_") && path.endsWith(EXTENSION_DIC));
	}


	private void openAffixFile(){
		try{
			flm.stop();

			clearAffixFile();

			printResultLine("Opening Affix file for parsing: " + affFile.getName());
			affParser.parse(affFile);

			printResultLine("Finished reading Affix file");

			String language = affParser.getLanguage();
			File dicFile = getFile(language + EXTENSION_DIC);
			dicParser = DictionaryParserBuilder.getParser(language, dicFile, wordGenerator, affParser.getCharset());

			dicDialog = new DictionarySortDialog(dicParser, this, "Sorter", "Please select a section from the list:");
			dicDialog.setLocationRelativeTo(this);
			ListCellRenderer<String> dicCellRenderer = new DictionarySortCellRenderer(dicParser);
			dicDialog.setCellRenderer(dicCellRenderer);
			dicDialog.addListSelectionListener(e -> {
				if(e.getValueIsAdjusting() && (dicSorterWorker == null || dicSorterWorker.isDone())){
					int selectedRow = dicDialog.getSelectedIndex();
					try{
						if(dicParser.isInBoundary(selectedRow)){
							dicDialog.setVisible(false);

							dicSortDictionaryMenuItem.setEnabled(false);
							mainProgressBar.setValue(0);


							dicSorterWorker = new SorterWorker(dicParser, selectedRow, this);
							dicSorterWorker.addPropertyChangeListener(this);
							dicSorterWorker.execute();
						}
					}
					catch(IOException exc){
						log.error(null, exc);
					}
				}
			});

			mainTabbedPane.setSelectedIndex(0);
			mainTabbedPane.setEnabledAt(0, true);
			dicInputTextField.requestFocusInWindow();


			//enable menu
			dicMenu.setEnabled(true);
			fileCreatePackageMenuItem.setEnabled(true);

			try{
				flm.register(this, affFile.getParent(), "*.aff", "*.dic", "*.aid");
				flm.start();
			}
			catch(IOException e){
				printResultLine("Unable to register file change listener");
			}
		}
		catch(IOException | IllegalArgumentException e){
			printResultLine(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		catch(Exception e){
			String message = ExceptionService.getMessage(e, getClass());
			printResultLine(e.getClass().getSimpleName() + ": " + message);
		}
	}

	private void clearAffixFile(){
		clearOutputTable();

		formerInputText = null;
		dicInputTextField.setText(null);
		mainTabbedPane.setEnabledAt(0, false);

		//disable menu
		dicMenu.setEnabled(false);
		fileCreatePackageMenuItem.setEnabled(false);

		affParser.clear();
		if(dicParser != null)
			dicParser.clear();
	}


	private void checkDictionaryCorrectness(AffixParser affParser){
		if(dicCorrectnessWorker == null || dicCorrectnessWorker.isDone()){
			dicCheckCorrectnessMenuItem.setEnabled(false);
			dicSortDictionaryMenuItem.setEnabled(false);
			mainProgressBar.setValue(0);

			dicCorrectnessWorker = new CorrectnessWorker(affParser, dicParser, this);
			dicCorrectnessWorker.addPropertyChangeListener(this);
			dicCorrectnessWorker.execute();
		}
	}

	private void extractDictionaryDuplicates(AffixParser affParser){
		if(dicDuplicatesWorker == null || dicDuplicatesWorker.isDone()){
			dicExtractDuplicatesMenuItem.setEnabled(false);
			dicSortDictionaryMenuItem.setEnabled(false);

			int fileChoosen = saveTextFileFileChooser.showSaveDialog(this);
			if(fileChoosen == JFileChooser.APPROVE_OPTION){
				mainProgressBar.setValue(0);

				File outputFile = saveTextFileFileChooser.getSelectedFile();
				dicDuplicatesWorker = new DuplicatesWorker(affParser, dicParser, outputFile, this);
				dicDuplicatesWorker.addPropertyChangeListener(this);
				dicDuplicatesWorker.execute();
			}
			else{
				dicExtractDuplicatesMenuItem.setEnabled(true);
				dicSortDictionaryMenuItem.setEnabled(true);
			}
		}
	}

	private void extractDictionaryWordlist(){
		if(dicWordlistWorker == null || dicWordlistWorker.isDone()){
			dicExtractWordlistMenuItem.setEnabled(false);
			dicSortDictionaryMenuItem.setEnabled(false);

			int fileChoosen = saveTextFileFileChooser.showSaveDialog(this);
			if(fileChoosen == JFileChooser.APPROVE_OPTION){
				mainProgressBar.setValue(0);

				File outputFile = saveTextFileFileChooser.getSelectedFile();
				dicWordlistWorker = new WordlistWorker(affParser, dicParser, outputFile, this);
				dicWordlistWorker.addPropertyChangeListener(this);
				dicWordlistWorker.execute();
			}
			else{
				dicExtractWordlistMenuItem.setEnabled(true);
				dicSortDictionaryMenuItem.setEnabled(true);
			}
		}
	}

	private void extractMinimalPairs(){
		if(dicMinimalPairsWorker == null || dicMinimalPairsWorker.isDone()){
			dicExtractMinimalPairsMenuItem.setEnabled(false);
			dicSortDictionaryMenuItem.setEnabled(false);

			int fileChoosen = saveTextFileFileChooser.showSaveDialog(this);
			if(fileChoosen == JFileChooser.APPROVE_OPTION){
				mainProgressBar.setValue(0);

				File outputFile = saveTextFileFileChooser.getSelectedFile();
				dicMinimalPairsWorker = new MinimalPairsWorker(affParser, dicParser, outputFile, this);
				dicMinimalPairsWorker.addPropertyChangeListener(this);
				dicMinimalPairsWorker.execute();
			}
			else{
				dicExtractMinimalPairsMenuItem.setEnabled(true);
				dicSortDictionaryMenuItem.setEnabled(true);
			}
		}
	}


	private void openAidFile(){
		try{
			clearAidFile();

			String codePath = getCurrentWorkingDirectory();
			codePath += "aids/" + affParser.getLanguage() + EXTENSION_AID;
			File aidFile = new File(codePath);
			if(aidFile.exists()){
				printResultLine("Opening AID file for parsing: " + aidFile.getName());

				aidParser.parse(aidFile);
				List<String> lines = aidParser.getLines();

				if(!lines.isEmpty()){
					lines.stream()
						.forEach(dicRuleTagsAidComboBox::addItem);

					//enable combo-box only if an AID file exists
					dicRuleTagsAidComboBox.setEnabled(true);
				}

				printResultLine("Finished reading Aid file");
			}
		}
		catch(IOException e){
			printResultLine(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		catch(Exception e){
			String message = ExceptionService.getMessage(e, getClass());
			printResultLine(e.getClass().getSimpleName() + ": " + message);
		}
	}

	private String getCurrentWorkingDirectory(){
		String codePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		return codePath
			.replaceFirst("(classes/)?[^/]*$", StringUtils.EMPTY)
			.replaceAll("%20", StringUtils.SPACE);
	}

	private void clearAidFile(){
		if(dicRuleTagsAidComboBox.getItemCount() > 0)
			dicRuleTagsAidComboBox.removeAllItems();

		aidParser.clear();
	}


	private void openThesaurusFile(){
		if(theParserWorker == null || theParserWorker.isDone()){
			clearThesaurusFile();

			File theFile = getThesaurusFile();
			if(theFile.exists()){
				Runnable postExecution = () -> {
					if(dicParser != null)
						dicParser.setHyphenationParser(hypParser);

					ThesaurusTableModel dm = (ThesaurusTableModel)theTable.getModel();
					dm.setSynonyms(theParser.getSynonymsDictionary());

					updateSynonymsCounter();

					mainTabbedPane.setEnabledAt(1, true);
				};
				theParserWorker = new ThesaurusParser.ParserWorker(theFile, theParser, postExecution, this);
				theParserWorker.addPropertyChangeListener(this);

				theParserWorker.execute();
			}
		}
	}

	private void clearThesaurusFile(){
		ThesaurusTableModel dm = (ThesaurusTableModel)theTable.getModel();
		dm.setSynonyms(null);

		theParser.clear();

		theMenu.setEnabled(false);

		mainTabbedPane.setEnabledAt(1, false);
	}

	private void updateSynonyms(){
		ThesaurusTableModel dm = (ThesaurusTableModel)theTable.getModel();
		dm.fireTableDataChanged();
	}

	private void updateSynonymsCounter(){
		theSynonymsRecordedOutputLabel.setText(COUNTER_FORMATTER.format(theParser.getSynonymsCounter()));
	}


	private void openHyphenationFile(){
		if(hypParserWorker == null || hypParserWorker.isDone()){
			clearHyphenationFile();

			File hypFile = getHyphenationFile();
			if(hypFile.exists()){
				hypParserWorker = new HyphenationParser.ParserWorker(hypFile, hypParser, () -> mainTabbedPane.setEnabledAt(2, true), this);
				hypParserWorker.addPropertyChangeListener(this);

				hypParserWorker.execute();
			}
		}
	}

	private void clearHyphenationFile(){
		hypParser.clear();
		
		clearHyphenation();

		mainTabbedPane.setEnabledAt(2, false);
	}

	private static void hyphenate(HunspellerFrame frame){
		String text = frame.hypWordTextField.getText();
		if(formerHyphenationText != null && formerHyphenationText.equals(text))
			return;
		formerHyphenationText = text;

		String count = null;
		if(StringUtils.isNotBlank(text)){
			text = frame.hypParser.correctOrthography(text);
			Hyphenation hyphenation = frame.hypParser.hyphenate(text);

			text = frame.hypParser.formatHyphenation(hyphenation, new StringJoiner(HyphenationParser.HYPHEN, "<html>", "</html>"),
				syllabe -> "<b style=\"color:red\">" + syllabe + "</b>");
			count = Long.toString(frame.hypParser.countSyllabes(hyphenation));

			frame.hypAddRuleTextField.setEnabled(true);
		}
		else{
			text = null;

			frame.hypAddRuleTextField.setEnabled(false);
		}

		frame.hypSyllabationOutputLabel.setText(text);
		frame.hypSyllabesCountOutputLabel.setText(count);

		frame.hypAddRuleTextField.setText(null);
		frame.hypAddRuleButton.setEnabled(false);
		frame.hypAddRuleSyllabationOutputLabel.setText(null);
		frame.hypAddRuleSyllabesCountOutputLabel.setText(null);
	}

	private static void hyphenateAddRule(HunspellerFrame frame){
		try{
			String addedRuleText = frame.hypParser.correctOrthography(frame.hypWordTextField.getText());
			String addedRule = frame.hypAddRuleTextField.getText().toLowerCase(Locale.ROOT);
			String addedRuleCount = null;
			if(StringUtils.isNotBlank(addedRule)){
				boolean alreadyHasRule = frame.hypParser.hasRule(addedRule);
				boolean ruleMatchesText = false;
				boolean hyphenationChanged = false;
				boolean correctHyphenation = false;
				if(!alreadyHasRule){
					ruleMatchesText = addedRuleText.contains(PatternService.clear(addedRule, REGEX_POINTS_AND_NUMBERS));

					if(ruleMatchesText){
						Hyphenation hyphenation = frame.hypParser.hyphenate(addedRuleText);
						Hyphenation addedRuleHyphenation = frame.hypParser.hyphenate(addedRuleText, addedRule);

						Supplier<StringJoiner> baseStringJoiner = () -> new StringJoiner(HyphenationParser.HYPHEN, "<html>", "</html>");
						Function<String, String> errorFormatter = syllabe -> "<b style=\"color:red\">" + syllabe + "</b>";
						String text = frame.hypParser.formatHyphenation(hyphenation, baseStringJoiner.get(), errorFormatter);
						addedRuleText = frame.hypParser.formatHyphenation(addedRuleHyphenation, baseStringJoiner.get(), errorFormatter);
						addedRuleCount = Long.toString(frame.hypParser.countSyllabes(addedRuleHyphenation));

						hyphenationChanged = !text.equals(addedRuleText);
						correctHyphenation = !addedRuleHyphenation.hasErrors();
					}
				}

				if(alreadyHasRule || !ruleMatchesText)
					addedRuleText = null;
				frame.hypAddRuleButton.setEnabled(ruleMatchesText && hyphenationChanged && correctHyphenation);
			}
			else{
				addedRuleText = null;

				frame.hypAddRuleTextField.setText(null);
				frame.hypAddRuleButton.setEnabled(false);
				frame.hypAddRuleSyllabationOutputLabel.setText(null);
				frame.hypAddRuleSyllabesCountOutputLabel.setText(null);
			}

			frame.hypAddRuleSyllabationOutputLabel.setText(addedRuleText);
			frame.hypAddRuleSyllabesCountOutputLabel.setText(addedRuleCount);
		}
		catch(CloneNotSupportedException e){
			log.error("Unexpected exception", e);
		}
	}

	private void clearHyphenation(){
		hypWordTextField.setText(null);
		hypSyllabationOutputLabel.setText(null);
		hypSyllabesCountOutputLabel.setText(null);
		hypAddRuleTextField.setText(null);
		hypAddRuleButton.setEnabled(false);
		hypAddRuleSyllabationOutputLabel.setText(null);
		hypAddRuleSyllabesCountOutputLabel.setText(null);
	}


	private File getFile(String filename){
		return new File(affFile.toPath().getParent().toString() + File.separator + filename);
	}

	private File getDictionaryFile(){
		return getFile(affParser.getLanguage() + EXTENSION_DIC);
	}

	private File getThesaurusFile(){
		return getFile("th_" + affParser.getLanguage() + "_v2.dat");
	}

	private File getHyphenationFile(){
		return getFile("hyph_" + affParser.getLanguage() + EXTENSION_DIC);
	}


	private void clearResultTextArea(){
		parsingResultTextArea.setText(null);
	}

	@Override
	public void printResultLine(String text){
		String timeOfDay = LocalTime.now().format(TIME_FORMATTER);
		parsingResultTextArea.append(timeOfDay + StringUtils.SPACE + text + StringUtils.LF);
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
				int progress = (int)evt.getNewValue();
				mainProgressBar.setValue(progress);
				break;

			case "state":
				SwingWorker.StateValue stateValue = (SwingWorker.StateValue)evt.getNewValue();
				if(stateValue == SwingWorker.StateValue.DONE){
					Runnable menuItemEnabler = enableMenuItemFromWorker.get(evt.getSource().getClass());
					if(menuItemEnabler != null)
						menuItemEnabler.run();
				}
				break;
		}
	}


	public static void main(String[] args){
		try{
			String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e){
			log.error(null, e);
		}

		//create and display the form
		EventQueue.invokeLater(() -> {
			(new HunspellerFrame()).setVisible(true);
		});
	}

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JMenuItem dicCheckCorrectnessMenuItem;
   private javax.swing.JMenuItem dicExtractDuplicatesMenuItem;
   private javax.swing.JMenuItem dicExtractMinimalPairsMenuItem;
   private javax.swing.JMenuItem dicExtractWordlistMenuItem;
   private javax.swing.JLabel dicInputLabel;
   private javax.swing.JTextField dicInputTextField;
   private javax.swing.JLayeredPane dicLayeredPane;
   private javax.swing.JMenu dicMenu;
   private javax.swing.JComboBox<String> dicRuleTagsAidComboBox;
   private javax.swing.JLabel dicRuleTagsAidLabel;
   private javax.swing.JScrollPane dicScrollPane;
   private javax.swing.JPopupMenu.Separator dicSeparator;
   private javax.swing.JMenuItem dicSortDictionaryMenuItem;
   private javax.swing.JTable dicTable;
   private javax.swing.JMenuItem fileCreatePackageMenuItem;
   private javax.swing.JMenuItem fileExitMenuItem;
   private javax.swing.JMenu fileMenu;
   private javax.swing.JMenuItem fileOpenAFFMenuItem;
   private javax.swing.JPopupMenu.Separator fileSeparator;
   private javax.swing.JMenuItem hlpAboutMenuItem;
   private javax.swing.JMenu hlpMenu;
   private javax.swing.JButton hypAddRuleButton;
   private javax.swing.JLabel hypAddRuleLabel;
   private javax.swing.JLabel hypAddRuleSyllabationLabel;
   private javax.swing.JLabel hypAddRuleSyllabationOutputLabel;
   private javax.swing.JLabel hypAddRuleSyllabesCountLabel;
   private javax.swing.JLabel hypAddRuleSyllabesCountOutputLabel;
   private javax.swing.JTextField hypAddRuleTextField;
   private javax.swing.JLayeredPane hypLayeredPane;
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
   private javax.swing.JPopupMenu.Separator recentFilesFileSeparator;
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
