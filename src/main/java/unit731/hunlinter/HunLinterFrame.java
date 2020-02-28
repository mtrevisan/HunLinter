package unit731.hunlinter;

import java.awt.*;

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
import unit731.hunlinter.actions.SelectFontAction;
import unit731.hunlinter.actions.ThesaurusLinterAction;
import unit731.hunlinter.actions.UpdateAction;
import unit731.hunlinter.gui.ProjectFolderFilter;
import unit731.hunlinter.interfaces.HunLintable;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import javax.swing.text.DefaultCaret;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.actions.DictionaryLinterAction;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.gui.RecentFilesMenu;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.exceptions.LanguageNotChosenException;
import unit731.hunlinter.workers.exceptions.ProjectNotFoundException;
import unit731.hunlinter.workers.ProjectLoaderWorker;
import unit731.hunlinter.workers.dictionary.WordlistWorker;
import unit731.hunlinter.workers.core.WorkerAbstract;
import unit731.hunlinter.services.downloader.DownloaderHelper;
import unit731.hunlinter.services.system.JavaHelper;
import unit731.hunlinter.services.log.ApplicationLogAppender;
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

	private final JFileChooser openProjectPathFileChooser;

	private final Preferences preferences = Preferences.userNodeForPackage(getClass());
	private final ParserManager parserManager;
	private final Packager packager;

	private RecentFilesMenu recentProjectsMenu;

	private ProjectLoaderWorker prjLoaderWorker;
	private final WorkerManager workerManager;


	public HunLinterFrame(){
		packager = new Packager();
		parserManager = new ParserManager(packager, this);
		workerManager = new WorkerManager(packager, parserManager, this);


		initComponents();


		recentProjectsMenu.setEnabled(recentProjectsMenu.hasEntries());
		filEmptyRecentProjectsMenuItem.setEnabled(recentProjectsMenu.hasEntries());

		//add "fontable" property
		GUIUtils.addFontableProperty(
			parsingResultTextArea);

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
      hypLayeredPane = new HyphenationLayeredPane(packager, parserManager, this);
      acoLayeredPane = new AutoCorrectLayeredPane(packager, parserManager, this);
      sexLayeredPane = new SentenceExceptionsLayeredPane(packager, parserManager);
      wexLayeredPane = new WordExceptionsLayeredPane(packager, parserManager);
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
      mainTabbedPane.addTab("Hyphenation", hypLayeredPane);
      mainTabbedPane.addTab("AutoCorrect", acoLayeredPane);
      mainTabbedPane.addTab("Sentence Exceptions", sexLayeredPane);
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
		((HyphenationLayeredPane)hypLayeredPane).setCurrentFont();
		((AutoCorrectLayeredPane)acoLayeredPane).setCurrentFont();
		((SentenceExceptionsLayeredPane)sexLayeredPane).setCurrentFont();
		((WordExceptionsLayeredPane)wexLayeredPane).setCurrentFont();
	}

	private void loadFileCompleted(){
		//restore default font (changed for reporting reading errors)
		setCurrentFont();

		parserManager.registerFileListener();
		parserManager.startFileListener();

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
			((HyphenationLayeredPane)hypLayeredPane).initialize();
			((AutoCorrectLayeredPane)acoLayeredPane).initialize();
			((SentenceExceptionsLayeredPane)sexLayeredPane).initialize();
			((WordExceptionsLayeredPane)wexLayeredPane).initialize();


			//thesaurus file:
			if(parserManager.getTheParser().getSynonymsCount() > 0){
				theMenu.setEnabled(true);
				theLinterMenuItem.setEnabled(true);
				GUIUtils.setTabbedPaneEnable(mainTabbedPane, theLayeredPane, true);
			}


			//hyphenation file:
			if(parserManager.getHyphenator() != null){
				hypLinterMenuItem.setEnabled(true);

				hypMenu.setEnabled(true);
				hypStatisticsMenuItem.setEnabled(true);
				GUIUtils.setTabbedPaneEnable(mainTabbedPane, hypLayeredPane, true);
			}


			//auto–correct file:
			if(parserManager.getAcoParser().getCorrectionsCounter() > 0)
				GUIUtils.setTabbedPaneEnable(mainTabbedPane, acoLayeredPane, true);


			//sentence exceptions file:
			if(parserManager.getSexParser().getExceptionsCounter() > 0)
				GUIUtils.setTabbedPaneEnable(mainTabbedPane, sexLayeredPane, true);


			//word exceptions file:
			if(parserManager.getWexParser().getExceptionsCounter() > 0)
				GUIUtils.setTabbedPaneEnable(mainTabbedPane, wexLayeredPane, true);


			if(!mainTabbedPane.getComponentAt(mainTabbedPane.getSelectedIndex()).isEnabled())
				mainTabbedPane.setSelectedIndex(0);


			final String language = parserManager.getAffixData().getLanguage();
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
		((HyphenationLayeredPane)hypLayeredPane).clear();
		((AutoCorrectLayeredPane)acoLayeredPane).clear();
		((SentenceExceptionsLayeredPane)sexLayeredPane).clear();
		((WordExceptionsLayeredPane)wexLayeredPane).clear();


		//hyphenation file:
		hypMenu.setEnabled(false);
		hypStatisticsMenuItem.setEnabled(false);
		GUIUtils.setTabbedPaneEnable(mainTabbedPane, hypLayeredPane, false);


		//aid file:
		clearAidParser();


		//auto–correct file:
		GUIUtils.setTabbedPaneEnable(mainTabbedPane, acoLayeredPane, false);


		//sentence exceptions file:
		GUIUtils.setTabbedPaneEnable(mainTabbedPane, sexLayeredPane, false);


		//word exceptions file:
		GUIUtils.setTabbedPaneEnable(mainTabbedPane, wexLayeredPane, false);
	}


	@Override
	public void clearAffixParser(){
		clearDictionaryParser();
	}

	@Override
	public void clearHyphenationParser(){
		((HyphenationLayeredPane)hypLayeredPane).clear();

		hypMenu.setEnabled(false);
		hypStatisticsMenuItem.setEnabled(false);
		GUIUtils.setTabbedPaneEnable(mainTabbedPane, hypLayeredPane, false);
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
		((AutoCorrectLayeredPane)acoLayeredPane).clear();

		GUIUtils.setTabbedPaneEnable(mainTabbedPane, acoLayeredPane, false);
	}

	@Override
	public void clearSentenceExceptionsParser(){
		((SentenceExceptionsLayeredPane)sexLayeredPane).clear();

		GUIUtils.setTabbedPaneEnable(mainTabbedPane, sexLayeredPane, false);
	}

	@Override
	public void clearWordExceptionsParser(){
		((WordExceptionsLayeredPane)wexLayeredPane).clear();

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
   private javax.swing.JLayeredPane acoLayeredPane;
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
   private javax.swing.JPopupMenu.Separator hypDuplicatesSeparator;
   private javax.swing.JLayeredPane hypLayeredPane;
   private javax.swing.JMenuItem hypLinterMenuItem;
   private javax.swing.JMenu hypMenu;
   private javax.swing.JMenuItem hypStatisticsMenuItem;
   private javax.swing.JMenuBar mainMenuBar;
   private javax.swing.JProgressBar mainProgressBar;
   private javax.swing.JTabbedPane mainTabbedPane;
   private javax.swing.JScrollPane parsingResultScrollPane;
   private javax.swing.JTextArea parsingResultTextArea;
   private javax.swing.JLayeredPane sexLayeredPane;
   private javax.swing.JLayeredPane theLayeredPane;
   private javax.swing.JMenuItem theLinterMenuItem;
   private javax.swing.JMenu theMenu;
   private javax.swing.JLayeredPane wexLayeredPane;
   // End of variables declaration//GEN-END:variables

}
