package unit731.hunspeller;

import java.awt.Desktop;
import unit731.hunspeller.interfaces.Hunspellable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.zip.Deflater;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import unit731.hunspeller.interfaces.Undoable;
import unit731.hunspeller.languages.DictionaryCorrectnessChecker;
import unit731.hunspeller.languages.BaseBuilder;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.aid.AidParser;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.hyphenators.Hyphenator;
import unit731.hunspeller.parsers.hyphenation.hyphenators.HyphenatorInterface;
import unit731.hunspeller.parsers.thesaurus.ThesaurusParser;
import unit731.hunspeller.services.ZipManager;
import unit731.hunspeller.services.filelistener.FileChangeListener;
import unit731.hunspeller.services.filelistener.FileListenerManager;


public class Backbone implements FileChangeListener{

	private static final Logger LOGGER = LoggerFactory.getLogger(Backbone.class);

	public static final Marker MARKER_APPLICATION = MarkerFactory.getMarker("application");
	public static final Marker MARKER_RULE_REDUCER = MarkerFactory.getMarker("rule-reducer");

	private static final ZipManager ZIPPER = new ZipManager();

	private static final String EXTENSION_AFF = ".aff";
	private static final String EXTENSION_DIC = ".dic";
	private static final String EXTENSION_AID = ".aid";
	private static final String EXTENSION_THESAURUS_INDEX = ".idx";
	private static final String EXTENSION_THESAURUS_DATA = ".dat";
	private static final String PREFIX_THESAURUS = "th_";
	private static final String SUFFIX_THESAURUS = "_v2";
	private static final String PREFIX_HYPHENATION = "hyph_";
	private static final String FOLDER_AID = "aids/";

	private static final String TAB = "\t";
	private static final String TAB_SPACES = StringUtils.repeat(' ', 3);


	private File affFile;

	private final AffixParser affParser;
	private final AidParser aidParser;
	private DictionaryParser dicParser;
	private final ThesaurusParser theParser;
	private HyphenationParser hypParser;

	private HyphenatorInterface hyphenator;
	private DictionaryCorrectnessChecker checker;
	private WordGenerator wordGenerator;

	private final Hunspellable hunspellable;
	private final FileListenerManager flm;


	public Backbone(Hunspellable hunspellable, Undoable undoable){
		affParser = new AffixParser();
		aidParser = new AidParser();
		theParser = new ThesaurusParser(undoable);

		this.hunspellable = hunspellable;
		flm = new FileListenerManager();
	}

	public AffixParser getAffParser(){
		return affParser;
	}

	public AffixData getAffixData(){
		return affParser.getAffixData();
	}

	public AidParser getAidParser(){
		return aidParser;
	}

	public DictionaryParser getDicParser(){
		return dicParser;
	}

	public ThesaurusParser getTheParser(){
		return theParser;
	}

	public HyphenatorInterface getHyphenator(){
		return hyphenator;
	}

	public DictionaryCorrectnessChecker getChecker(){
		return checker;
	}

	public WordGenerator getWordGenerator(){
		return wordGenerator;
	}

	public void loadFile(String affixFilePath) throws FileNotFoundException, IOException{
		clear();

		openAffixFile(affixFilePath);

		File hypFile = getHyphenationFile();
		openHyphenationFile(hypFile);

		checker = BaseBuilder.getCorrectnessChecker(affParser.getAffixData(), hyphenator);

		File dicFile = getDictionaryFile();
		prepareDictionaryFile(dicFile);

		File aidFile = getAidFile();
		openAidFile(aidFile);

		File theDataFile = getThesaurusDataFile();
		openThesaurusFile(theDataFile);
	}

	/* NOTE: used for testing purposes */
	public void loadFile(String affixFilePath, String dictionaryFilePath) throws FileNotFoundException, IOException{
		openAffixFile(affixFilePath);

		File hypFile = getHyphenationFile();
		openHyphenationFile(hypFile);

		checker = BaseBuilder.getCorrectnessChecker(affParser.getAffixData(), hyphenator);

		File dicFile = new File(dictionaryFilePath);
		prepareDictionaryFile(dicFile);

		File aidFile = getAidFile();
		openAidFile(aidFile);

		File theDataFile = getThesaurusDataFile();
		openThesaurusFile(theDataFile);
	}

	public void clear(){
		hyphenator = null;
		checker = null;
		wordGenerator = null;
	}

	public void registerFileListener(){
		File hypFile = getHyphenationFile();
		File aidFile = getAidFile();
		flm.register(this, affFile.getAbsolutePath(), hypFile.getAbsolutePath(), aidFile.getAbsolutePath());
	}

	public void startFileListener(){
		flm.start();
	}

	public void stopFileListener(){
		flm.stop();
	}

	public void openAffixFile(String affixFilePath) throws IOException{
		affFile = new File(affixFilePath);

		if(!affFile.exists()){
			affParser.clear();

			if(hunspellable != null)
				hunspellable.clearAffixParser();

			throw new FileNotFoundException("The file '" + affixFilePath + "' does not exists");
		}

		LOGGER.info(MARKER_APPLICATION, "Opening Affix file: {}", affFile.getName());

		affParser.parse(affFile);

		LOGGER.info(MARKER_APPLICATION, "Finished reading Affix file");
	}

	public void openHyphenationFile(File hypFile){
		if(hypFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening Hyphenation file: {}", hypFile.getName());

			hypParser = new HyphenationParser(affParser.getAffixData().getLanguage());
			hypParser.parse(hypFile);

			hyphenator = new Hyphenator(hypParser, HyphenationParser.BREAK_CHARACTER);

			if(hunspellable != null)
				hunspellable.clearHyphenationParser();

			LOGGER.info(MARKER_APPLICATION, "Finished reading Hyphenation file");
		}
		else if(hypParser != null)
			hypParser.clear();
	}

	public void obtainCorrectnessChecker() throws IOException{
		Objects.requireNonNull(affParser);

		checker = BaseBuilder.getCorrectnessChecker(affParser.getAffixData(), hyphenator);
	}

	public void prepareDictionaryFile(File dicFile){
		final AffixData affixData = affParser.getAffixData();
		if(dicFile.exists()){
			final String language = affixData.getLanguage();
			final Charset charset = affixData.getCharset();
			dicParser = new DictionaryParser(dicFile, language, charset);

			if(hunspellable != null)
				hunspellable.clearDictionaryParser();
		}
		else if(dicParser != null)
			dicParser.clear();

		wordGenerator = new WordGenerator(affixData, dicParser);
	}

	public void openAidFile(File aidFile) throws IOException{
		if(aidFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening Aid file: {}", aidFile.getName());

			aidParser.parse(aidFile);

			if(hunspellable != null)
				hunspellable.clearAidParser();

			LOGGER.info(MARKER_APPLICATION, "Finished reading Aid file");
		}
		else
			aidParser.clear();
	}

	public void openThesaurusFile(File theDataFile) throws IOException{
		if(theDataFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening Thesaurus file: {}", theDataFile.getName());

			theParser.parse(theDataFile);

			if(hunspellable != null)
				hunspellable.clearThesaurusParser();

			LOGGER.info(MARKER_APPLICATION, "Finished reading Thesaurus file");
		}
		else
			theParser.clear();
	}


	public void storeHyphenationFile() throws IOException{
		File hypFile = getHyphenationFile();
		hypParser.save(hypFile);
	}

	public void storeThesaurusFiles() throws IOException{
		File thesIndexFile = getThesaurusIndexFile();
		File thesDataFile = getThesaurusDataFile();
		theParser.save(thesIndexFile, thesDataFile);
	}


	private File getFile(String filename){
		return new File(affFile.toPath().getParent().toString() + File.separator + filename);
	}

	/* FIXME should be private!? */
	public File getDictionaryFile(){
		return getFile(FilenameUtils.removeExtension(affFile.getName()) + EXTENSION_DIC);
	}

	public File getAidFile(){
		return new File(getCurrentWorkingDirectory() + FOLDER_AID + affParser.getAffixData().getLanguage() + EXTENSION_AID);
	}

	private String getCurrentWorkingDirectory(){
		String codePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		return codePath
			.replaceFirst("(classes/)?[^/]*$", StringUtils.EMPTY)
			.replaceAll("%20", StringUtils.SPACE);
	}

	private File getThesaurusIndexFile(){
		return getFile(PREFIX_THESAURUS + affParser.getAffixData().getLanguage() + SUFFIX_THESAURUS + EXTENSION_THESAURUS_INDEX);
	}

	public File getThesaurusDataFile(){
		return getFile(PREFIX_THESAURUS + affParser.getAffixData().getLanguage() + SUFFIX_THESAURUS + EXTENSION_THESAURUS_DATA);
	}

	public File getHyphenationFile(){
		return getFile(PREFIX_HYPHENATION + affParser.getAffixData().getLanguage() + EXTENSION_DIC);
	}


	@Override
	public void fileDeleted(Path path){
		LOGGER.info(MARKER_APPLICATION, "File {} deleted", path.toFile().getName());

		String absolutePath = affFile.getParent() + File.separator + path.toString();
		if(hasAFFExtension(absolutePath)){
			affParser.clear();

			if(hunspellable != null)
				hunspellable.clearAffixParser();
		}
		else if(hasAIDExtension(absolutePath)){
			aidParser.clear();

			if(hunspellable != null)
				hunspellable.clearAidParser();
		}
	}

	@Override
	public void fileModified(Path path){
		LOGGER.info(MARKER_APPLICATION, "File {} modified, reloading", path.toString());

		if(hunspellable != null)
			hunspellable.loadFileInternal(affFile.getAbsolutePath());
	}

	private boolean hasAFFExtension(String path){
		return path.endsWith(EXTENSION_AFF);
	}

	private boolean hasDICExtension(String path){
		return path.endsWith(EXTENSION_DIC);
	}

	private boolean isHyphenationFile(String path){
		String baseName = FilenameUtils.getBaseName(path);
		return (baseName.startsWith(PREFIX_HYPHENATION) && path.endsWith(EXTENSION_DIC));
	}

	private boolean hasAIDExtension(String path){
		return path.endsWith(EXTENSION_AID);
	}


	public void createPackage(){
		Path basePath = getPackageBaseDirectory();

		//package entire folder with ZIP
		if(basePath != null){
			LOGGER.info(Backbone.MARKER_APPLICATION, "Found base path on {}", basePath.toString());

			try{
				String outputFilename = basePath.toString() + File.separator + basePath.getName(basePath.getNameCount() - 1) + ".zip";
				ZIPPER.zipDirectory(basePath.toFile(), Deflater.BEST_COMPRESSION, outputFilename);

				LOGGER.info(Backbone.MARKER_APPLICATION, "Package created");

				//open directory
				if(Desktop.isDesktopSupported())
					Desktop.getDesktop().open(new File(basePath.toString()));
			}
			catch(IOException e){
				LOGGER.info(Backbone.MARKER_APPLICATION, "Package error: {}", e.getMessage());

				LOGGER.error("Something very bad happened while creating package", e);
			}
		}
	}

	/** Go up directories until description.xml or install.rdf is found */
	private Path getPackageBaseDirectory(){
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
		return (found? parentPath: null);
	}


	public String[] getDictionaryLines() throws IOException{
		File dicFile = getDictionaryFile();
		return Files.lines(dicFile.toPath(), affParser.getAffixData().getCharset())
			.map(line -> StringUtils.replace(line, TAB, TAB_SPACES))
			.toArray(String[]::new);
	}

	public void mergeSectionsToDictionary(List<File> files) throws IOException{
		OpenOption option = StandardOpenOption.TRUNCATE_EXISTING;
		for(File file : files){
			Files.write(getDictionaryFile().toPath(), Files.readAllBytes(file.toPath()), option);

			option = StandardOpenOption.APPEND;
		}
	}

	public boolean hasHyphenationRule(String addedRule, HyphenationParser.Level level){
		return hypParser.hasRule(addedRule, level);
	}

	public String addHyphenationRule(String newRule, HyphenationParser.Level level){
		return hypParser.addRule(newRule, level);
	}

	public boolean restorePreviousThesaurusSnapshot() throws IOException{
		boolean restored = theParser.restorePreviousSnapshot();
		if(restored)
			storeThesaurusFiles();

		return restored;
	}

	public boolean restoreNextThesaurusSnapshot() throws IOException{
		boolean restored = theParser.restoreNextSnapshot();
		if(restored)
			storeThesaurusFiles();

		return restored;
	}

}
