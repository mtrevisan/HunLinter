/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter;

import io.github.mtrevisan.hunlinter.actions.*;
import io.github.mtrevisan.hunlinter.gui.FontHelper;
import io.github.mtrevisan.hunlinter.gui.GUIHelper;
import io.github.mtrevisan.hunlinter.gui.MultiProgressBarUI;
import io.github.mtrevisan.hunlinter.gui.ProjectFolderFilter;
import io.github.mtrevisan.hunlinter.gui.components.RecentFilesMenu;
import io.github.mtrevisan.hunlinter.gui.dialogs.FileDownloaderDialog;
import io.github.mtrevisan.hunlinter.gui.dialogs.FontChooserDialog;
import io.github.mtrevisan.hunlinter.gui.events.LoadProjectEvent;
import io.github.mtrevisan.hunlinter.gui.events.PreLoadProjectEvent;
import io.github.mtrevisan.hunlinter.gui.events.TabbedPaneEnableEvent;
import io.github.mtrevisan.hunlinter.gui.panes.AutoCorrectLayeredPane;
import io.github.mtrevisan.hunlinter.gui.panes.CompoundsLayeredPane;
import io.github.mtrevisan.hunlinter.gui.panes.DictionaryLayeredPane;
import io.github.mtrevisan.hunlinter.gui.panes.HyphenationLayeredPane;
import io.github.mtrevisan.hunlinter.gui.panes.PoSFSALayeredPane;
import io.github.mtrevisan.hunlinter.gui.panes.SentenceExceptionsLayeredPane;
import io.github.mtrevisan.hunlinter.gui.panes.ThesaurusLayeredPane;
import io.github.mtrevisan.hunlinter.gui.panes.WordExceptionsLayeredPane;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixParser;
import io.github.mtrevisan.hunlinter.parsers.aid.AidParser;
import io.github.mtrevisan.hunlinter.parsers.autocorrect.AutoCorrectParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.exceptions.ExceptionsParser;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenationParser;
import io.github.mtrevisan.hunlinter.parsers.thesaurus.ThesaurusParser;
import io.github.mtrevisan.hunlinter.services.Packager;
import io.github.mtrevisan.hunlinter.services.RecentItems;
import io.github.mtrevisan.hunlinter.services.downloader.DownloaderHelper;
import io.github.mtrevisan.hunlinter.services.downloader.VersionException;
import io.github.mtrevisan.hunlinter.services.eventbus.EventBusService;
import io.github.mtrevisan.hunlinter.services.eventbus.EventHandler;
import io.github.mtrevisan.hunlinter.services.eventbus.events.BusExceptionEvent;
import io.github.mtrevisan.hunlinter.services.log.ApplicationLogAppender;
import io.github.mtrevisan.hunlinter.services.log.ExceptionHelper;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.workers.WorkerManager;
import io.github.mtrevisan.hunlinter.workers.core.WorkerAbstract;
import io.github.mtrevisan.hunlinter.workers.dictionary.WordlistWorker;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterWarning;
import io.github.mtrevisan.hunlinter.workers.exceptions.ProjectNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.KeyStroke;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
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
import java.io.Serial;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.prefs.Preferences;


/**
 * Swing alternative: @see <a href="https://pivot.apache.org/">Apache Pivot</a>
 *
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
 *
 * TODO? progress bar on taskbar: https://github.com/Dansoftowner/FXTaskbarProgressBar/tree/11/src/main/java/com/nativejavafx/taskbar
 */
public class MainFrame extends JFrame implements ActionListener, PropertyChangeListener{

	@Serial
	private static final long serialVersionUID = 6772959670167531135L;

	private static final Logger LOGGER = LoggerFactory.getLogger(MainFrame.class);

	public static final Integer ACTION_COMMAND_INITIALIZE = 0;
	public static final Integer ACTION_COMMAND_GUI_CLEAR_ALL = 10;
	public static final Integer ACTION_COMMAND_GUI_CLEAR_DICTIONARY = 11;
	public static final Integer ACTION_COMMAND_GUI_CLEAR_AID = 12;
	public static final Integer ACTION_COMMAND_GUI_CLEAR_COMPOUNDS = 13;
	public static final Integer ACTION_COMMAND_GUI_CLEAR_THESAURUS = 14;
	public static final Integer ACTION_COMMAND_GUI_CLEAR_HYPHENATION = 15;
	public static final Integer ACTION_COMMAND_GUI_CLEAR_AUTO_CORRECT = 16;
	public static final Integer ACTION_COMMAND_GUI_CLEAR_SENTENCE_EXCEPTIONS = 17;
	public static final Integer ACTION_COMMAND_GUI_CLEAR_WORD_EXCEPTIONS = 18;
	public static final Integer ACTION_COMMAND_GUI_CLEAR_POS_DICTIONARY = 19;
	public static final Integer ACTION_COMMAND_PARSER_CLEAR_ALL = 20;
	public static final Integer ACTION_COMMAND_PARSER_CLEAR_AFFIX = 21;
	public static final Integer ACTION_COMMAND_PARSER_CLEAR_DICTIONARY = 22;
	public static final Integer ACTION_COMMAND_PARSER_CLEAR_AID = 23;
	public static final Integer ACTION_COMMAND_PARSER_CLEAR_THESAURUS = 24;
	public static final Integer ACTION_COMMAND_PARSER_CLEAR_HYPHENATION = 25;
	public static final Integer ACTION_COMMAND_PARSER_CLEAR_AUTO_CORRECT = 26;
	public static final Integer ACTION_COMMAND_PARSER_CLEAR_SENTENCE_EXCEPTION = 27;
	public static final Integer ACTION_COMMAND_PARSER_CLEAR_WORD_EXCEPTION = 28;
	public static final Integer ACTION_COMMAND_PARSER_CLEAR_AUTO_TEXT = 29;
	public static final Integer ACTION_COMMAND_PARSER_RELOAD_DICTIONARY = 30;

	public static final String FONT_FAMILY_NAME_PREFIX = "font.familyName.";
	private static final String FONT_SIZE_PREFIX = "font.size.";

	public static final String PROPERTY_NAME_PROGRESS = "progress";
	public static final String PROPERTY_NAME_STATE = "state";
	public static final String PROPERTY_NAME_PAUSED = "paused";


	private final Future<JFileChooser> futureOpenProjectPathFileChooser;

	private final Preferences preferences = Preferences.userNodeForPackage(MainFrame.class);
	private final ParserManager parserManager;
	private final Packager packager;

	private RecentFilesMenu recentProjectsMenu;

	private final WorkerManager workerManager;


	public MainFrame(){
		Runtime.getRuntime().addShutdownHook(new Thread(JavaHelper::shutdownExecutor));

		packager = new Packager();
		parserManager = new ParserManager(packager);
		workerManager = new WorkerManager(packager, parserManager);


		initComponents();


		recentProjectsMenu.setEnabled(recentProjectsMenu.hasEntries());
		filEmptyRecentProjectsMenuItem.setEnabled(recentProjectsMenu.isEnabled());

		FontHelper.addFontableProperty(parsingResultTextPane);
		FontHelper.addLoggableProperty(parsingResultTextPane);

		ApplicationLogAppender.addTextPane(parsingResultTextPane, ParserManager.MARKER_APPLICATION);


		futureOpenProjectPathFileChooser = JavaHelper.executeFuture(() -> {
			final JFileChooser openProjectPathFileChooser = new JFileChooser();
			openProjectPathFileChooser.setFileFilter(new ProjectFolderFilter("Project folders"));
			openProjectPathFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			//disable the "All files" option
			openProjectPathFileChooser.setAcceptAllFileFilterUsed(false);
			try{
				@SuppressWarnings("ConstantConditions")
				final BufferedImage projectFolderImg = ImageIO.read(GUIHelper.class.getResourceAsStream("/project_folder.png"));
				final Icon projectFolderIcon = new ImageIcon(projectFolderImg);
				openProjectPathFileChooser.setFileView(new MyFileView(projectFolderIcon));
			}
			catch(final IOException ignored){}
			return openProjectPathFileChooser;
		});


		//check for updates
		if(preferences.getBoolean(CheckUpdateOnStartupAction.UPDATE_STARTUP_CHECK, true))
			JavaHelper.executeOnEventDispatchThread(() -> {
				try{
					final FileDownloaderDialog dialog = new FileDownloaderDialog(this);
					GUIHelper.addCancelByEscapeKey(dialog);
					dialog.setLocationRelativeTo(this);
					dialog.setVisible(true);
				}
				catch(final IOException | ParseException | URISyntaxException | VersionException ignored){}
			});
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   @SuppressWarnings("ConstantConditions")
	private void initComponents() {

      parsingResultScrollPane = new javax.swing.JScrollPane();
      parsingResultTextPane = new javax.swing.JTextPane();
      mainProgressBar = new javax.swing.JProgressBar();
		mainProgressBar.setForeground(MultiProgressBarUI.MAIN_COLOR);
		mainProgressBar.setUI(new MultiProgressBarUI());
      mainTabbedPane = new javax.swing.JTabbedPane();
      dicLayeredPane = new DictionaryLayeredPane(packager, parserManager);
		EventBusService.subscribe(dicLayeredPane);
      cmpLayeredPane = new CompoundsLayeredPane(packager, parserManager, workerManager, this, this);
		EventBusService.subscribe(cmpLayeredPane);
      theLayeredPane = new ThesaurusLayeredPane(parserManager);
		EventBusService.subscribe(theLayeredPane);
      hypLayeredPane = new HyphenationLayeredPane(packager, parserManager, this);
		EventBusService.subscribe(hypLayeredPane);
      acoLayeredPane = new AutoCorrectLayeredPane(packager, parserManager, this);
		EventBusService.subscribe(acoLayeredPane);
      sexLayeredPane = new SentenceExceptionsLayeredPane(packager, parserManager);
		EventBusService.subscribe(sexLayeredPane);
      wexLayeredPane = new WordExceptionsLayeredPane(packager, parserManager);
		EventBusService.subscribe(wexLayeredPane);
      pdcLayeredPane = new PoSFSALayeredPane(parserManager);
		EventBusService.subscribe(pdcLayeredPane);
      mainMenuBar = new javax.swing.JMenuBar();
      filMenu = new javax.swing.JMenu();
      filOpenProjectMenuItem = new javax.swing.JMenuItem();
      filCreatePackageMenuItem = new javax.swing.JMenuItem();
      setFontMenuItem = new javax.swing.JMenuItem();
      filRecentProjectsSeparator = new javax.swing.JPopupMenu.Separator();
      filEmptyRecentProjectsMenuItem = new javax.swing.JMenuItem();
      filSeparator = new javax.swing.JPopupMenu.Separator();
      filExitMenuItem = new javax.swing.JMenuItem();
      dicMenu = new javax.swing.JMenu();
      dicLinterMenuItem = new javax.swing.JMenuItem();
      dicSortDictionaryMenuItem = new javax.swing.JMenuItem();
      dicRulesReducerMenuItem = new javax.swing.JMenuItem();
		dicAffixExtractionMenuItem = new javax.swing.JMenuItem();
      dicDuplicatesSeparator = new javax.swing.JPopupMenu.Separator();
      dicWordCountMenuItem = new javax.swing.JMenuItem();
      dicStatisticsMenuItem = new javax.swing.JMenuItem();
      dicStatisticsSeparator = new javax.swing.JPopupMenu.Separator();
      dicExtractDuplicatesMenuItem = new javax.swing.JMenuItem();
      dicExtractWordlistMenuItem = new javax.swing.JMenuItem();
      dicExtractWordlistPlainTextMenuItem = new javax.swing.JMenuItem();
      dicExtractFullstripWordlistMenuItem = new javax.swing.JMenuItem();
      dicExtractMinimalPairsMenuItem = new javax.swing.JMenuItem();
      dicFSASeparator = new javax.swing.JPopupMenu.Separator();
      dicExtractDictionaryFSAMenuItem = new javax.swing.JMenuItem();
      dicExtractPoSFSAMenuItem = new javax.swing.JMenuItem();
      theMenu = new javax.swing.JMenu();
      theLinterMenuItem = new javax.swing.JMenuItem();
      theLinterFSAMenuItem = new javax.swing.JMenuItem();
      hypMenu = new javax.swing.JMenu();
      hypLinterMenuItem = new javax.swing.JMenuItem();
      hypDuplicatesSeparator = new javax.swing.JPopupMenu.Separator();
      hypStatisticsMenuItem = new javax.swing.JMenuItem();
      acoMenu = new javax.swing.JMenu();
      acoLinterMenuItem = new javax.swing.JMenuItem();
      acoLinterFSAMenuItem = new javax.swing.JMenuItem();
      setMenu = new javax.swing.JMenu();
      setCheckUpdateOnStartupCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
      setReportWarningsCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
      hlpMenu = new javax.swing.JMenu();
      hlpOnlineHelpMenuItem = new javax.swing.JMenuItem();
      hlpIssueReporterMenuItem = new javax.swing.JMenuItem();
      hlpOnlineSeparator = new javax.swing.JPopupMenu.Separator();
      hlpUpdateMenuItem = new javax.swing.JMenuItem();
      hlpUpdateSeparator = new javax.swing.JPopupMenu.Separator();
      hlpAboutMenuItem = new javax.swing.JMenuItem();

      setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
      setTitle(DownloaderHelper.ARTIFACT_ID);
      setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png")));
      setMinimumSize(new java.awt.Dimension(964, 534));

      parsingResultTextPane.setEditable(false);
      parsingResultScrollPane.setViewportView(parsingResultTextPane);

      mainTabbedPane.addTab("Inflections", dicLayeredPane);
      mainTabbedPane.addTab("Compounds", cmpLayeredPane);
      mainTabbedPane.addTab("Thesaurus", theLayeredPane);
      mainTabbedPane.addTab("Hyphenation", hypLayeredPane);
      mainTabbedPane.addTab("AutoCorrect", acoLayeredPane);
      mainTabbedPane.addTab("Sentence Exceptions", sexLayeredPane);
      mainTabbedPane.addTab("Word Exceptions", wexLayeredPane);
      mainTabbedPane.addTab("PoS FSA", pdcLayeredPane);

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
      filOpenProjectMenuItem.setText("Open project…");
      filOpenProjectMenuItem.addActionListener(this::filOpenProjectMenuItemActionPerformed);
      filMenu.add(filOpenProjectMenuItem);

      filCreatePackageMenuItem.setAction(new CreatePackageAction(parserManager));
      filCreatePackageMenuItem.setMnemonic('p');
      filCreatePackageMenuItem.setText("Create package");
      filCreatePackageMenuItem.setEnabled(false);
      filMenu.add(filCreatePackageMenuItem);
      filMenu.add(filRecentProjectsSeparator);

      filEmptyRecentProjectsMenuItem.setMnemonic('e');
      filEmptyRecentProjectsMenuItem.setText("Empty recent projects list");
      filEmptyRecentProjectsMenuItem.setEnabled(false);
      filEmptyRecentProjectsMenuItem.addActionListener(this::filEmptyRecentProjectsMenuItemActionPerformed);
      filMenu.add(filEmptyRecentProjectsMenuItem);
      filMenu.add(filSeparator);

      filExitMenuItem.setAction(new ExitAction());
      filExitMenuItem.setMnemonic('x');
      filExitMenuItem.setText("Exit");
      filMenu.add(filExitMenuItem);

      mainMenuBar.add(filMenu);
      final RecentItems recentItems = new RecentItems(5, preferences);
      recentProjectsMenu = new io.github.mtrevisan.hunlinter.gui.components.RecentFilesMenu(recentItems, MainFrame::loadFile);
      recentProjectsMenu.setText("Recent projects");
      recentProjectsMenu.setMnemonic('R');
      filMenu.add(recentProjectsMenu, 3);

      dicMenu.setMnemonic('D');
      dicMenu.setText("Dictionary tools");
      dicMenu.setEnabled(false);

		final Consumer<Exception> onDicLinterCancelled = exc -> dicLinterMenuItem.setEnabled(true);
      dicLinterMenuItem.setAction(new DictionaryLinterAction(workerManager, this, onDicLinterCancelled));
      dicLinterMenuItem.setMnemonic('c');
      dicLinterMenuItem.setText("Correctness check");
      dicLinterMenuItem.setToolTipText("");
      dicMenu.add(dicLinterMenuItem);

      dicSortDictionaryMenuItem.setAction(new DictionarySorterAction(parserManager, workerManager, this));
      dicSortDictionaryMenuItem.setMnemonic('o');
      dicSortDictionaryMenuItem.setText("Sort dictionary…");
      dicMenu.add(dicSortDictionaryMenuItem);

      dicRulesReducerMenuItem.setAction(new AffixRulesReducerAction(parserManager));
      dicRulesReducerMenuItem.setMnemonic('r');
      dicRulesReducerMenuItem.setText("Rules reducer…");
      dicRulesReducerMenuItem.setToolTipText("");
      dicMenu.add(dicRulesReducerMenuItem);

      dicAffixExtractionMenuItem.setAction(new AffixExtractionAction(parserManager));
		dicAffixExtractionMenuItem.setText("Affix extraction…");
		dicAffixExtractionMenuItem.setToolTipText("");
      dicMenu.add(dicAffixExtractionMenuItem);
      dicMenu.add(dicDuplicatesSeparator);

		final Consumer<Exception> onDicWordCountCancelled = exc -> dicWordCountMenuItem.setEnabled(true);
      dicWordCountMenuItem.setAction(new DictionaryWordCountAction(workerManager, this, onDicWordCountCancelled));
      dicWordCountMenuItem.setMnemonic('w');
      dicWordCountMenuItem.setText("Word count");
      dicMenu.add(dicWordCountMenuItem);

		final Consumer<Exception> onDicStatisticsCancelled = exc -> dicStatisticsMenuItem.setEnabled(true);
      dicStatisticsMenuItem.setAction(new DictionaryHyphenationStatisticsAction(false, workerManager,
			this, this, onDicStatisticsCancelled));
      dicStatisticsMenuItem.setMnemonic('S');
      dicStatisticsMenuItem.setText("Statistics");
      dicMenu.add(dicStatisticsMenuItem);
      dicMenu.add(dicStatisticsSeparator);

		final Consumer<Exception> onDicExtractDuplicatesCancelled = ex -> dicExtractDuplicatesMenuItem.setEnabled(true);
      dicExtractDuplicatesMenuItem.setAction(new DictionaryExtractDuplicatesAction(workerManager, this,
			onDicExtractDuplicatesCancelled));
      dicExtractDuplicatesMenuItem.setMnemonic('d');
      dicExtractDuplicatesMenuItem.setText("Extract duplicates…");
      dicMenu.add(dicExtractDuplicatesMenuItem);

		final Consumer<Exception> onDicExtractWordlistCancelled = exc -> dicExtractWordlistMenuItem.setEnabled(true);
      dicExtractWordlistMenuItem.setAction(new DictionaryExtractWordlistAction(WordlistWorker.WorkerType.COMPLETE, workerManager,
			this, onDicExtractWordlistCancelled));
      dicExtractWordlistMenuItem.setText("Extract wordlist…");
      dicMenu.add(dicExtractWordlistMenuItem);

		final Consumer<Exception> onDicExtractWordlistPlainTextCancelled = exp -> dicExtractWordlistPlainTextMenuItem.setEnabled(true);
		dicExtractWordlistPlainTextMenuItem.setAction(new DictionaryExtractWordlistAction(WordlistWorker.WorkerType.PLAIN_WORDS_NO_DUPLICATES,
			workerManager, this, onDicExtractWordlistPlainTextCancelled));
      dicExtractWordlistPlainTextMenuItem.setText("Extract wordlist (plain words)…");
      dicMenu.add(dicExtractWordlistPlainTextMenuItem);

		final Consumer<Exception> onDicExtractFullstripWordlistCancelled = exc -> dicExtractFullstripWordlistMenuItem.setEnabled(true);
		dicExtractFullstripWordlistMenuItem.setAction(new DictionaryExtractWordlistAction(WordlistWorker.WorkerType.FULLSTRIP_WORDS,
			workerManager, this, onDicExtractFullstripWordlistCancelled));
		dicExtractFullstripWordlistMenuItem.setText("Extract fullstrip wordlist…");
		dicMenu.add(dicExtractFullstripWordlistMenuItem);

		final Consumer<Exception> onDicExtractMinimalPairsCancelled = exc -> dicExtractMinimalPairsMenuItem.setEnabled(true);
		dicExtractMinimalPairsMenuItem.setAction(new DictionaryExtractMinimalPairsAction(workerManager, this,
			onDicExtractMinimalPairsCancelled));
      dicExtractMinimalPairsMenuItem.setText("Extract minimal pairs…");
      dicMenu.add(dicExtractMinimalPairsMenuItem);
      dicMenu.add(dicFSASeparator);

		final Consumer<Exception> onDicExtractDictionaryFSACancelled = exc -> dicExtractDictionaryFSAMenuItem.setEnabled(true);
      dicExtractDictionaryFSAMenuItem.setAction(new DictionaryExtractWordlistFSAAction(parserManager, workerManager,
			this, onDicExtractDictionaryFSACancelled));
      dicExtractDictionaryFSAMenuItem.setText("Extract dictionary FSA…");
      dicMenu.add(dicExtractDictionaryFSAMenuItem);

		final Consumer<Exception> onDicExtractPoSFSACancelled = exc -> dicExtractPoSFSAMenuItem.setEnabled(true);
      dicExtractPoSFSAMenuItem.setAction(new DictionaryExtractPoSFSAAction(parserManager, workerManager, this,
			onDicExtractPoSFSACancelled));
      dicExtractPoSFSAMenuItem.setText("Extract PoS FSA…");
      dicMenu.add(dicExtractPoSFSAMenuItem);

      mainMenuBar.add(dicMenu);

      theMenu.setMnemonic('T');
      theMenu.setText("Thesaurus tools");
      theMenu.setEnabled(false);

		final Consumer<Exception> onTheLinterCancelled = exc -> theLinterMenuItem.setEnabled(true);
      theLinterMenuItem.setAction(new ThesaurusLinterAction(workerManager, this, onTheLinterCancelled));
      theLinterMenuItem.setMnemonic('c');
      theLinterMenuItem.setText("Correctness check");
      theMenu.add(theLinterMenuItem);

		final Consumer<Exception> onTheLinterFSACancelled = exc -> theLinterFSAMenuItem.setEnabled(true);
		theLinterFSAMenuItem.setAction(new ThesaurusLinterFSAAction(workerManager, (ThesaurusLayeredPane)theLayeredPane,
			this, onTheLinterFSACancelled));
		theLinterFSAMenuItem.setMnemonic('d');
      theLinterFSAMenuItem.setText("Correctness check using dictionary FSA…");
		theMenu.add(theLinterFSAMenuItem);

      mainMenuBar.add(theMenu);

      hypMenu.setMnemonic('y');
      hypMenu.setText("Hyphenation tools");
      hypMenu.setEnabled(false);

		final Consumer<Exception> onHypLinterCancelled = exc -> hypLinterMenuItem.setEnabled(true);
      hypLinterMenuItem.setAction(new HyphenationLinterAction(workerManager, this, onHypLinterCancelled));
      hypLinterMenuItem.setMnemonic('c');
      hypLinterMenuItem.setText("Correctness check");
      hypMenu.add(hypLinterMenuItem);
      hypMenu.add(hypDuplicatesSeparator);

		final Consumer<Exception> onHypStatisticsCancelled = exc -> hypStatisticsMenuItem.setEnabled(true);
      hypStatisticsMenuItem.setAction(new DictionaryHyphenationStatisticsAction(true, workerManager,
			this, this, onHypStatisticsCancelled));
      hypStatisticsMenuItem.setMnemonic('S');
      hypStatisticsMenuItem.setText("Statistics");
      hypMenu.add(hypStatisticsMenuItem);

      mainMenuBar.add(hypMenu);

      acoMenu.setMnemonic('A');
      acoMenu.setText("AutoCorrect tools");
      acoMenu.setEnabled(false);

		final Consumer<Exception> onAcoLinterCancelled = exc -> acoLinterMenuItem.setEnabled(true);
      acoLinterMenuItem.setAction(new AutoCorrectLinterAction(workerManager, this, onAcoLinterCancelled));
      acoLinterMenuItem.setMnemonic('c');
      acoLinterMenuItem.setText("Correctness check");
      acoMenu.add(acoLinterMenuItem);

		final Consumer<Exception> onAcoLinterFSACancelled = exc -> acoLinterFSAMenuItem.setEnabled(true);
		acoLinterFSAMenuItem.setAction(new AutoCorrectLinterFSAAction(workerManager, (ThesaurusLayeredPane)theLayeredPane,
			this, onAcoLinterFSACancelled));
		acoLinterFSAMenuItem.setMnemonic('d');
		acoLinterFSAMenuItem.setText("Correctness check using dictionary FSA…");
		acoMenu.add(acoLinterFSAMenuItem);

      mainMenuBar.add(acoMenu);

      setMenu.setMnemonic('S');
      setMenu.setText("Settings");

      setCheckUpdateOnStartupCheckBoxMenuItem.setAction(new CheckUpdateOnStartupAction(preferences));
      setCheckUpdateOnStartupCheckBoxMenuItem.setSelected(preferences.getBoolean(CheckUpdateOnStartupAction.UPDATE_STARTUP_CHECK, true));
      setCheckUpdateOnStartupCheckBoxMenuItem.setText("Check for updates on startup");
      setMenu.add(setCheckUpdateOnStartupCheckBoxMenuItem);

		setFontMenuItem.setAction(new SelectFontAction(packager, parserManager, preferences));
		setFontMenuItem.setMnemonic('f');
		setFontMenuItem.setText("Select font…");
		setMenu.add(setFontMenuItem);

      setReportWarningsCheckBoxMenuItem.setAction(new ReportWarningsAction(preferences));
      setReportWarningsCheckBoxMenuItem.setSelected(preferences.getBoolean(ReportWarningsAction.REPORT_WARNINGS, true));
      setReportWarningsCheckBoxMenuItem.setText("Report warnings");
      setMenu.add(setReportWarningsCheckBoxMenuItem);

      mainMenuBar.add(setMenu);

      hlpMenu.setMnemonic('H');
      hlpMenu.setText("Help");

      hlpOnlineHelpMenuItem.setAction(new OnlineHelpAction());
      hlpOnlineHelpMenuItem.setMnemonic('h');
      hlpOnlineHelpMenuItem.setText("Online help");
      hlpMenu.add(hlpOnlineHelpMenuItem);

      hlpIssueReporterMenuItem.setAction(new IssueReporterAction());
      hlpIssueReporterMenuItem.setText("Report issue");
      hlpMenu.add(hlpIssueReporterMenuItem);
      hlpMenu.add(hlpOnlineSeparator);

      hlpUpdateMenuItem.setAction(new UpdateAction());
      hlpUpdateMenuItem.setText("Check for update…");
      hlpMenu.add(hlpUpdateMenuItem);
      hlpMenu.add(hlpUpdateSeparator);

      hlpAboutMenuItem.setAction(new AboutAction());
      hlpAboutMenuItem.setMnemonic('a');
      hlpAboutMenuItem.setText("About");
      hlpMenu.add(hlpAboutMenuItem);

      mainMenuBar.add(hlpMenu);

      setJMenuBar(mainMenuBar);

      final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
               .addComponent(mainTabbedPane)
               .addComponent(parsingResultScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 919, Short.MAX_VALUE)
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
            .addComponent(mainTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 206, Short.MAX_VALUE)
            .addContainerGap())
      );

      tabbedPaneEnableEvent(new TabbedPaneEnableEvent(false));

      final KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
      mainTabbedPane.registerKeyboardAction(this, escapeKeyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

      pack();
      setLocationRelativeTo(null);
   }// </editor-fold>//GEN-END:initComponents

	private void filOpenProjectMenuItemActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filOpenProjectMenuItemActionPerformed
		MenuSelectionManager.defaultManager().clearSelectedPath();

		final JFileChooser openProjectPathFileChooser = JavaHelper.waitForFuture(futureOpenProjectPathFileChooser);
		final int projectSelected = openProjectPathFileChooser.showOpenDialog(this);
		if(projectSelected == JFileChooser.APPROVE_OPTION){
			final File baseFile = openProjectPathFileChooser.getSelectedFile();
			recentProjectsMenu.addEntry(baseFile.getAbsolutePath());

			recentProjectsMenu.setEnabled(true);
			filEmptyRecentProjectsMenuItem.setEnabled(true);

			loadFile(baseFile.toPath());
		}
	}//GEN-LAST:event_filOpenProjectMenuItemActionPerformed

	private void filEmptyRecentProjectsMenuItemActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filEmptyRecentProjectsMenuItemActionPerformed
		recentProjectsMenu.clear();

		recentProjectsMenu.setEnabled(false);
		filEmptyRecentProjectsMenuItem.setEnabled(false);
	}//GEN-LAST:event_filEmptyRecentProjectsMenuItemActionPerformed


	@Override
	public final void actionPerformed(final ActionEvent event){
		WorkerManager.checkForAbortion(this);
	}

	private static void loadFile(final Path basePath){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		EventBusService.publish(new PreLoadProjectEvent(basePath));
	}

	@EventHandler
	@SuppressWarnings("unused")
	public final void loadFileInternal(final PreLoadProjectEvent preLoadProjectEvent){
		parsingResultTextPane.setText(null);
		filOpenProjectMenuItem.setEnabled(false);
		recentProjectsMenu.setEnabled(false);

		if(parserManager != null)
			parserManager.stopFileListener();

		//clear all
		loadFileCancelled(null);

		mainTabbedPane.setSelectedIndex(0);

		//NOTE: in order to avoid concurrency problems it is necessary to transform the loader into an event for the bus
		//so to happen after all the clear events
		EventBusService.publish(preLoadProjectEvent.convertToLoadEvent());
	}

	@EventHandler
	@SuppressWarnings("unused")
	public final void loadFileInternal(final LoadProjectEvent loadProjectEvent){
		final Path projectPath = loadProjectEvent.getProject();
		final Consumer<Font> initialize = temporaryFont -> {
			FontHelper.setCurrentFont(temporaryFont, this);

			filOpenProjectMenuItem.setEnabled(false);
			recentProjectsMenu.setEnabled(false);
		};
		final ActionListener projectLoaderAction = new ProjectLoaderAction(projectPath, packager, workerManager,
			initialize, this::loadFileCompleted, this::loadFileCancelled, this);
		final ActionEvent event = new ActionEvent(filEmptyRecentProjectsMenuItem, -1, "openProject");
		projectLoaderAction.actionPerformed(event);
	}

	@EventHandler
	@SuppressWarnings("unused")
	public final void loadFileInternal(final BusExceptionEvent exceptionEvent){
		final Throwable cause = exceptionEvent.getCause();

		//FIXME sometimes happens...
		if(cause.getMessage() == null)
			cause.printStackTrace();

		LOGGER.error(ParserManager.MARKER_APPLICATION, cause.getMessage());

		if(cause instanceof ProjectNotFoundException pnfe){
			//remove the file from the recent projects menu
			recentProjectsMenu.removeEntry(pnfe.getProjectPath().toString());

			recentProjectsMenu.setEnabled(recentProjectsMenu.hasEntries());
			filEmptyRecentProjectsMenuItem.setEnabled(recentProjectsMenu.hasEntries());
		}
	}

	@EventHandler
	@SuppressWarnings("unused")
	public static void parsingWarnings(final LinterWarning warningEvent){
		final String errorMessage = ExceptionHelper.getMessage(warningEvent);
		final Object eventData = warningEvent.getData();
		if(eventData != null){
			final int eventIndex = warningEvent.getIndex();
			final String lineText = (eventIndex >= 0? ", line " + (eventIndex + 1): StringUtils.EMPTY);
			LOGGER.trace("WARN: {}{}: {}", errorMessage, lineText, eventData);
			LOGGER.warn(ParserManager.MARKER_APPLICATION, "WARN: {}{}: {}", warningEvent.getMessage(), lineText, eventData);
		}
		else{
			LOGGER.trace("WARN: {}", errorMessage);
			LOGGER.warn(ParserManager.MARKER_APPLICATION, "WARN: {}", warningEvent.getMessage());
		}
	}

	private void loadFileCompleted(){
		try{
			filOpenProjectMenuItem.setEnabled(true);
			recentProjectsMenu.setEnabled(recentProjectsMenu.hasEntries());
			filCreatePackageMenuItem.setEnabled(true);
//			filFontMenuItem.setEnabled(true);
			dicLinterMenuItem.setEnabled(true);
			dicSortDictionaryMenuItem.setEnabled(true);
			dicMenu.setEnabled(true);


			EventBusService.publish(ACTION_COMMAND_INITIALIZE);

			//dictionary file:
			EventBusService.publish(new TabbedPaneEnableEvent(dicLayeredPane, true));

			//dictionary file (compound):
			final AffixData affixData = parserManager.getAffixData();
			if(affixData != null)
				EventBusService.publish(new TabbedPaneEnableEvent(cmpLayeredPane, !affixData.getCompoundRules().isEmpty()));

			//thesaurus file:
			final ThesaurusParser theParser = parserManager.getTheParser();
			if(theParser != null){
				theMenu.setEnabled(theParser.getSynonymsCount() > 0);
				EventBusService.publish(new TabbedPaneEnableEvent(theLayeredPane, theMenu.isEnabled()));
			}

			//hyphenation file:
			hypMenu.setEnabled(parserManager.getHyphenator() != null);
			EventBusService.publish(new TabbedPaneEnableEvent(hypLayeredPane, hypMenu.isEnabled()));

			//autocorrection file:
			final AutoCorrectParser acoParser = parserManager.getAcoParser();
			if(acoParser != null){
				acoMenu.setEnabled(acoParser.getCorrectionsCounter() > 0);
				EventBusService.publish(new TabbedPaneEnableEvent(acoLayeredPane, acoMenu.isEnabled()));
			}

			//sentence exceptions file:
			final ExceptionsParser sexParser = parserManager.getSexParser();
			if(sexParser != null)
				EventBusService.publish(new TabbedPaneEnableEvent(sexLayeredPane, (sexParser.getExceptionsCounter() > 0)));

			//word exceptions file:
			final ExceptionsParser wexParser = parserManager.getWexParser();
			if(wexParser != null)
				EventBusService.publish(new TabbedPaneEnableEvent(wexLayeredPane, (wexParser.getExceptionsCounter() > 0)));

			//Part-of-Speech dictionary file:
			EventBusService.publish(new TabbedPaneEnableEvent(pdcLayeredPane, true));


			//enable the first tab if the current one was disabled
			if(!mainTabbedPane.getComponentAt(mainTabbedPane.getSelectedIndex()).isEnabled())
				mainTabbedPane.setSelectedIndex(0);


			//load font for this language
			loadLanguageDependentFont();
		}
		catch(final RuntimeException e){
			LOGGER.error(ParserManager.MARKER_APPLICATION, "A bad error occurred: {}", e.getMessage());

			LOGGER.error("A bad error occurred", e);
		}

		parserManager.startFileListener();
	}

	private void loadLanguageDependentFont(){
		final String language = parserManager.getLanguage();
		final String fontFamilyName = preferences.get(FONT_FAMILY_NAME_PREFIX + language, null);
		final String fontSize = preferences.get(FONT_SIZE_PREFIX + language, null);
		final Font lastUsedFont = (fontFamilyName != null && fontSize != null
			? new Font(fontFamilyName, Font.PLAIN, Integer.parseInt(fontSize))
			: FontChooserDialog.getDefaultFont());
		FontHelper.setCurrentFont(lastUsedFont, this);
	}

	@EventHandler
	@SuppressWarnings("unused")
	public final void tabbedPaneEnableEvent(final TabbedPaneEnableEvent evt){
		final JLayeredPane pane = evt.getPane();
		final boolean enable = evt.isEnable();
		if(pane != null){
			final int index = mainTabbedPane.indexOfComponent(pane);
			mainTabbedPane.setEnabledAt(index, enable);
		}
		else
			for(int index = 0; index < mainTabbedPane.getComponentCount(); index ++)
				mainTabbedPane.setEnabledAt(index, enable);
	}

	private void loadFileCancelled(final Throwable exc){
		//menu:
		filOpenProjectMenuItem.setEnabled(true);
		filCreatePackageMenuItem.setEnabled(false);
//		filFontMenuItem.setEnabled(false);

		EventBusService.publish(ACTION_COMMAND_GUI_CLEAR_ALL);

		EventBusService.publish(ACTION_COMMAND_PARSER_CLEAR_ALL);

		//dictionary file:
		dicMenu.setEnabled(false);
		//thesaurus file:
		theMenu.setEnabled(false);
		//autocorrection file:
		acoMenu.setEnabled(false);
		//hyphenation file:
		hypMenu.setEnabled(false);

		EventBusService.publish(new TabbedPaneEnableEvent(false));

		//NOTE: wait for clearing before proceed (this is done by instantiating the event bus with waitForHandler `true`)
	}


	@EventHandler
	@SuppressWarnings({"unused", "NumberEquality"})
	public final void clearAffixParser(final Integer actionCommand){
		if(actionCommand != ACTION_COMMAND_PARSER_CLEAR_ALL && actionCommand != ACTION_COMMAND_PARSER_CLEAR_AFFIX)
			return;

		EventBusService.publish(ACTION_COMMAND_GUI_CLEAR_DICTIONARY);
		EventBusService.publish(ACTION_COMMAND_GUI_CLEAR_COMPOUNDS);

		EventBusService.publish(new TabbedPaneEnableEvent(dicLayeredPane, false));
		EventBusService.publish(new TabbedPaneEnableEvent(cmpLayeredPane, false));

		//disable menu
		dicMenu.setEnabled(false);
		filCreatePackageMenuItem.setEnabled(false);
//		filFontMenuItem.setEnabled(false);

		final AffixParser affParser = parserManager.getAffParser();
		if(affParser != null)
			affParser.clear();
	}

	@EventHandler
	@SuppressWarnings({"unused", "NumberEquality"})
	public final void clearDictionaryParser(final Integer actionCommand){
		if(actionCommand != ACTION_COMMAND_PARSER_CLEAR_ALL && actionCommand != ACTION_COMMAND_PARSER_CLEAR_DICTIONARY)
			return;

		final DictionaryParser dicParser = parserManager.getDicParser();
		if(dicParser != null)
			dicParser.clear();
	}

	@EventHandler
	@SuppressWarnings({"unused", "NumberEquality"})
	public final void clearAidParser(final Integer actionCommand){
		if(actionCommand != ACTION_COMMAND_PARSER_CLEAR_ALL && actionCommand != ACTION_COMMAND_PARSER_CLEAR_AID)
			return;

		EventBusService.publish(ACTION_COMMAND_GUI_CLEAR_AID);

		final AidParser aidParser = parserManager.getAidParser();
		if(aidParser != null)
			aidParser.clear();
	}

	@EventHandler
	@SuppressWarnings({"unused", "NumberEquality"})
	public final void clearThesaurusParser(final Integer actionCommand){
		if(actionCommand != ACTION_COMMAND_PARSER_CLEAR_ALL && actionCommand != ACTION_COMMAND_PARSER_CLEAR_THESAURUS)
			return;

		EventBusService.publish(ACTION_COMMAND_GUI_CLEAR_THESAURUS);

		theMenu.setEnabled(false);
		EventBusService.publish(new TabbedPaneEnableEvent(theLayeredPane, false));

		final ThesaurusParser theParser = parserManager.getTheParser();
		if(theParser != null)
			theParser.clear();
	}

	@EventHandler
	@SuppressWarnings({"unused", "NumberEquality"})
	public final void clearHyphenationParser(final Integer actionCommand){
		if(actionCommand != ACTION_COMMAND_PARSER_CLEAR_ALL && actionCommand != ACTION_COMMAND_PARSER_CLEAR_HYPHENATION)
			return;

		EventBusService.publish(ACTION_COMMAND_GUI_CLEAR_HYPHENATION);

		hypMenu.setEnabled(false);
		EventBusService.publish(new TabbedPaneEnableEvent(hypLayeredPane, false));

		final HyphenationParser hypParser = parserManager.getHypParser();
		if(hypParser != null)
			hypParser.clear();
	}

	@EventHandler
	@SuppressWarnings({"unused", "NumberEquality"})
	public final void clearAutoCorrectParser(final Integer actionCommand){
		if(actionCommand != ACTION_COMMAND_PARSER_CLEAR_ALL && actionCommand != ACTION_COMMAND_PARSER_CLEAR_AUTO_CORRECT)
			return;

		EventBusService.publish(ACTION_COMMAND_GUI_CLEAR_AUTO_CORRECT);

		acoMenu.setEnabled(false);
		EventBusService.publish(new TabbedPaneEnableEvent(acoLayeredPane, false));

		final AutoCorrectParser acoParser = parserManager.getAcoParser();
		if(acoParser != null)
			acoParser.clear();
	}

	@EventHandler
	@SuppressWarnings({"unused", "NumberEquality"})
	public final void clearSentenceExceptionsParser(final Integer actionCommand){
		if(actionCommand != ACTION_COMMAND_PARSER_CLEAR_ALL && actionCommand != ACTION_COMMAND_PARSER_CLEAR_SENTENCE_EXCEPTION)
			return;

		EventBusService.publish(ACTION_COMMAND_GUI_CLEAR_SENTENCE_EXCEPTIONS);

		EventBusService.publish(new TabbedPaneEnableEvent(sexLayeredPane, false));

		final ExceptionsParser sexParser = parserManager.getSexParser();
		if(sexParser != null)
			sexParser.clear();
	}

	@EventHandler
	@SuppressWarnings({"unused", "NumberEquality"})
	public final void clearWordExceptionsParser(final Integer actionCommand){
		if(actionCommand != ACTION_COMMAND_PARSER_CLEAR_ALL && actionCommand != ACTION_COMMAND_PARSER_CLEAR_WORD_EXCEPTION)
			return;

		EventBusService.publish(ACTION_COMMAND_GUI_CLEAR_WORD_EXCEPTIONS);

		EventBusService.publish(new TabbedPaneEnableEvent(wexLayeredPane, false));

		final ExceptionsParser wexParser = parserManager.getWexParser();
		if(wexParser != null)
			wexParser.clear();
	}


	@Override
	public final void propertyChange(final PropertyChangeEvent evt){
		switch(evt.getPropertyName()){
			case PROPERTY_NAME_PROGRESS -> {
				final int progress = (Integer)evt.getNewValue();
				mainProgressBar.setValue(progress);
			}
			case PROPERTY_NAME_STATE -> {
				final SwingWorker.StateValue stateValue = (SwingWorker.StateValue)evt.getNewValue();
				mainProgressBar.setForeground(MultiProgressBarUI.MAIN_COLOR);
				if(stateValue == SwingWorker.StateValue.DONE){
					final String workerName = ((WorkerAbstract<?>)evt.getSource()).getWorkerName();
					WorkerManager.callOnEnd(workerName);
				}
			}
			case WorkerAbstract.PROPERTY_WORKER_CANCELLED -> mainProgressBar.setForeground(MultiProgressBarUI.ERROR_COLOR);
		}
	}

	private static final class MyFileView extends FileView{
		private final Icon projectFolderIcon;

		private MyFileView(final Icon projectFolderIcon){
			this.projectFolderIcon = projectFolderIcon;
		}

		//choose the right icon for the folder
		@Override
		public Icon getIcon(final File file){
			return (Packager.isProjectFolder(file)
				? projectFolderIcon
				: FileSystemView.getFileSystemView().getSystemIcon(file));
		}
	}


	@SuppressWarnings("unused")
	@Serial
	private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	@Serial
	private void readObject(final ObjectInputStream is) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e){
			LOGGER.error(null, e);
		}

		//create and display the form
		JavaHelper.executeOnEventDispatchThread(() -> {
			final MainFrame mainFrame = new MainFrame();
			EventBusService.subscribe(mainFrame);
			mainFrame.setVisible(true);

			FontHelper.loadAllFonts();
		});
	}

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JLayeredPane acoLayeredPane;
   private javax.swing.JMenuItem acoLinterMenuItem;
   private javax.swing.JMenu acoMenu;
   private javax.swing.JLayeredPane cmpLayeredPane;
   private javax.swing.JPopupMenu.Separator dicDuplicatesSeparator;
   private javax.swing.JMenuItem dicExtractDictionaryFSAMenuItem;
   private javax.swing.JMenuItem dicExtractDuplicatesMenuItem;
   private javax.swing.JMenuItem dicExtractMinimalPairsMenuItem;
   private javax.swing.JMenuItem dicExtractPoSFSAMenuItem;
   private javax.swing.JMenuItem dicExtractWordlistMenuItem;
   private javax.swing.JMenuItem dicExtractWordlistPlainTextMenuItem;
   private javax.swing.JMenuItem dicExtractFullstripWordlistMenuItem;
   private javax.swing.JPopupMenu.Separator dicFSASeparator;
   private javax.swing.JLayeredPane dicLayeredPane;
   private javax.swing.JMenuItem dicLinterMenuItem;
   private javax.swing.JMenu dicMenu;
   private javax.swing.JMenuItem dicRulesReducerMenuItem;
   private javax.swing.JMenuItem dicAffixExtractionMenuItem;
   private javax.swing.JMenuItem dicSortDictionaryMenuItem;
   private javax.swing.JMenuItem dicStatisticsMenuItem;
   private javax.swing.JPopupMenu.Separator dicStatisticsSeparator;
   private javax.swing.JMenuItem dicWordCountMenuItem;
   private javax.swing.JMenuItem filCreatePackageMenuItem;
   private javax.swing.JMenuItem filEmptyRecentProjectsMenuItem;
   private javax.swing.JMenuItem filExitMenuItem;
   private javax.swing.JMenuItem setFontMenuItem;
   private javax.swing.JMenu filMenu;
   private javax.swing.JMenuItem filOpenProjectMenuItem;
   private javax.swing.JPopupMenu.Separator filRecentProjectsSeparator;
   private javax.swing.JPopupMenu.Separator filSeparator;
   private javax.swing.JMenuItem hlpAboutMenuItem;
   private javax.swing.JMenuItem hlpIssueReporterMenuItem;
   private javax.swing.JMenu hlpMenu;
   private javax.swing.JMenuItem hlpOnlineHelpMenuItem;
   private javax.swing.JPopupMenu.Separator hlpOnlineSeparator;
   private javax.swing.JMenuItem hlpUpdateMenuItem;
   private javax.swing.JPopupMenu.Separator hlpUpdateSeparator;
   private javax.swing.JPopupMenu.Separator hypDuplicatesSeparator;
   private javax.swing.JLayeredPane hypLayeredPane;
   private javax.swing.JMenuItem hypLinterMenuItem;
   private javax.swing.JMenu hypMenu;
   private javax.swing.JMenuItem hypStatisticsMenuItem;
   private javax.swing.JMenuBar mainMenuBar;
   private javax.swing.JProgressBar mainProgressBar;
   private javax.swing.JTabbedPane mainTabbedPane;
   private javax.swing.JScrollPane parsingResultScrollPane;
   private javax.swing.JTextPane parsingResultTextPane;
   private javax.swing.JLayeredPane pdcLayeredPane;
   private javax.swing.JCheckBoxMenuItem setCheckUpdateOnStartupCheckBoxMenuItem;
   private javax.swing.JMenu setMenu;
   private javax.swing.JCheckBoxMenuItem setReportWarningsCheckBoxMenuItem;
   private javax.swing.JLayeredPane sexLayeredPane;
   private javax.swing.JLayeredPane theLayeredPane;
   private javax.swing.JMenuItem theLinterMenuItem;
   private javax.swing.JMenuItem theLinterFSAMenuItem;
   private javax.swing.JMenuItem acoLinterFSAMenuItem;
   private javax.swing.JMenu theMenu;
   private javax.swing.JLayeredPane wexLayeredPane;
	// End of variables declaration//GEN-END:variables

}
