package unit731.hunspeller;

import org.xml.sax.SAXException;
import unit731.hunspeller.interfaces.Hunspellable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

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
import unit731.hunspeller.parsers.autocorrect.AutoCorrectParser;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.Hyphenator;
import unit731.hunspeller.parsers.hyphenation.HyphenatorInterface;
import unit731.hunspeller.parsers.thesaurus.ThesaurusParser;
import unit731.hunspeller.parsers.wordexceptions.ExceptionsParser;
import unit731.hunspeller.services.Packager;
import unit731.hunspeller.services.filelistener.FileChangeListener;
import unit731.hunspeller.services.filelistener.FileListenerManager;

import javax.xml.transform.TransformerException;


public class Backbone implements FileChangeListener{

	private static final Logger LOGGER = LoggerFactory.getLogger(Backbone.class);

	public static final Marker MARKER_APPLICATION = MarkerFactory.getMarker("application");
	public static final Marker MARKER_RULE_REDUCER = MarkerFactory.getMarker("rule-reducer");

	private static final String EXTENSION_AFF = ".aff";
	private static final String EXTENSION_DIC = ".dic";
	private static final String EXTENSION_AID = ".aid";
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

	private final AutoCorrectParser acoParser;
	private final ExceptionsParser sexParser;
	private final ExceptionsParser wexParser;

	private Packager packager;

	private final Hunspellable hunspellable;
	private final FileListenerManager flm;


	public Backbone(final Hunspellable hunspellable, final Undoable undoable){
		affParser = new AffixParser();
		aidParser = new AidParser();
		theParser = new ThesaurusParser(undoable);
		acoParser = new AutoCorrectParser();
		sexParser = new ExceptionsParser(Packager.FILENAME_SENTENCE_EXCEPTIONS);
		wexParser = new ExceptionsParser(Packager.FILENAME_WORD_EXCEPTIONS);

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

	public AutoCorrectParser getAcoParser(){
		return acoParser;
	}

	public ExceptionsParser getSexParser(){
		return sexParser;
	}

	public ExceptionsParser getWexParser(){
		return wexParser;
	}

	public DictionaryCorrectnessChecker getChecker(){
		return checker;
	}

	public WordGenerator getWordGenerator(){
		return wordGenerator;
	}

	public void loadFile(final String affixFilePath) throws IOException, SAXException{
		clear();

		openAffixFile(affixFilePath);

		final File hypFile = getHyphenationFile();
		openHyphenationFile(hypFile);

		checker = BaseBuilder.getCorrectnessChecker(affParser.getAffixData(), hyphenator);

		final File dicFile = getDictionaryFile();
		prepareDictionaryFile(dicFile);

		final File aidFile = getAidFile();
		openAidFile(aidFile);

		final File theDataFile = getThesaurusDataFile();
		openThesaurusFile(theDataFile);

		final File acoFile = getAutoCorrectFile();
		openAutoCorrectFile(acoFile);

		final File sexFile = getSentenceExceptionsFile();
		openSentenceExceptionsFile(sexFile);

		final File wexFile = getWordExceptionsFile();
		openWordExceptionsFile(wexFile);
	}

	/* NOTE: used for testing purposes */
	public void loadFile(final String affixFilePath, final String dictionaryFilePath) throws IOException, SAXException{
		openAffixFile(affixFilePath);

		final File hypFile = getHyphenationFile();
		openHyphenationFile(hypFile);

		checker = BaseBuilder.getCorrectnessChecker(affParser.getAffixData(), hyphenator);

		final File dicFile = new File(dictionaryFilePath);
		prepareDictionaryFile(dicFile);

		final File aidFile = getAidFile();
		openAidFile(aidFile);

		final File theDataFile = getThesaurusDataFile();
		openThesaurusFile(theDataFile);

		final File acoFile = getAutoCorrectFile();
		openAutoCorrectFile(acoFile);

		final File sexFile = getSentenceExceptionsFile();
		openSentenceExceptionsFile(sexFile);

		final File wexFile = getWordExceptionsFile();
		openWordExceptionsFile(wexFile);
	}

	public void clear(){
		hyphenator = null;
		checker = null;
		wordGenerator = null;
	}

	public void registerFileListener(){
		final File hypFile = getHyphenationFile();
		final File aidFile = getAidFile();
		final File acoFile = getAutoCorrectFile();

		final String[] uris = Stream.of(affFile, hypFile, aidFile, acoFile)
			.filter(Objects::nonNull)
			.map(File::getAbsolutePath)
			.toArray(String[]::new);
		flm.register(this, uris);
	}

	public void startFileListener(){
		flm.start();
	}

	public void stopFileListener(){
		flm.stop();
	}

	public void openAffixFile(final String affixFilePath) throws IOException{
		affFile = new File(affixFilePath);

		if(!affFile.exists()){
			affParser.clear();

			if(hunspellable != null)
				hunspellable.clearAffixParser();

			throw new FileNotFoundException("The file '" + affixFilePath + "' does not exists");
		}

		LOGGER.info(MARKER_APPLICATION, "Opening Affix file: {}", affFile.getName());

		affParser.parse(affFile);

		packager = new Packager(affFile);

		LOGGER.info(MARKER_APPLICATION, "Finished reading Affix file");
	}

	public void openHyphenationFile(final File hypFile){
		if(hypFile != null && hypFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening Hyphenation file: {}", hypFile.getName());

			final String language = affParser.getAffixData().getLanguage();
			final Comparator<String> comparator = BaseBuilder.getComparator(language);
			hypParser = new HyphenationParser(comparator);
			hypParser.parse(hypFile);

			hyphenator = new Hyphenator(hypParser, HyphenationParser.BREAK_CHARACTER);

			if(hunspellable != null)
				hunspellable.clearHyphenationParser();

			LOGGER.info(MARKER_APPLICATION, "Finished reading Hyphenation file");
		}
		else if(hypParser != null)
			hypParser.clear();
	}

	public void getCorrectnessChecker(){
		Objects.requireNonNull(affParser);

		checker = BaseBuilder.getCorrectnessChecker(affParser.getAffixData(), hyphenator);
	}

	public void prepareDictionaryFile(final File dicFile){
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

	public void openAidFile(final File aidFile) throws IOException{
		if(aidFile != null && aidFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening Aid file: {}", aidFile.getName());

			aidParser.parse(aidFile);

			if(hunspellable != null)
				hunspellable.clearAidParser();

			LOGGER.info(MARKER_APPLICATION, "Finished reading Aid file");
		}
		else
			aidParser.clear();
	}

	public void openThesaurusFile(final File theDataFile) throws IOException{
		if(theDataFile != null && theDataFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening Thesaurus file: {}", theDataFile.getName());

			theParser.parse(theDataFile);

			if(hunspellable != null)
				hunspellable.clearThesaurusParser();

			LOGGER.info(MARKER_APPLICATION, "Finished reading Thesaurus file");
		}
		else
			theParser.clear();
	}

	public void openAutoCorrectFile(final File acoFile) throws IOException, SAXException{
		if(acoFile != null && acoFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening AutoCorrect file: {}", acoFile.getName());

			acoParser.parse(acoFile);

			if(hunspellable != null)
				hunspellable.clearAutoCorrectParser();

			LOGGER.info(MARKER_APPLICATION, "Finished reading AutoCorrect file");
		}
		else
			acoParser.clear();
	}

	public void openSentenceExceptionsFile(final File sexFile) throws IOException, SAXException{
		if(sexFile != null && sexFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening SentenceExceptions file: {}", sexFile.getName());

			sexParser.parse(sexFile);

			if(hunspellable != null)
				hunspellable.clearSentenceExceptionsParser();

			LOGGER.info(MARKER_APPLICATION, "Finished reading SentenceExceptions file");
		}
		else
			sexParser.clear();
	}

	public void openWordExceptionsFile(final File wexFile) throws IOException, SAXException{
		if(wexFile != null && wexFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening WordExceptions file: {}", wexFile.getName());

			wexParser.parse(wexFile);

			if(hunspellable != null)
				hunspellable.clearWordExceptionsParser();

			LOGGER.info(MARKER_APPLICATION, "Finished reading WordExceptions file");
		}
		else
			wexParser.clear();
	}


	public void storeHyphenationFile() throws IOException{
		final File hypFile = getHyphenationFile();
		hypParser.save(hypFile);
	}

	public void storeThesaurusFiles() throws IOException{
		final File theIndexFile = getThesaurusIndexFile();
		final File theDataFile = getThesaurusDataFile();
		theParser.save(theIndexFile, theDataFile);
	}

	public void storeAutoCorrectFile() throws TransformerException{
		final File acoFile = getAutoCorrectFile();
		acoParser.save(acoFile);
	}


	private File getFile(String filename){
		return new File(affFile.toPath().getParent().toString() + File.separator + filename);
	}

	public File getAffFile(){
		return affFile;
	}

	//FIXME should this be private?!
	public File getDictionaryFile(){
		return getFile(FilenameUtils.removeExtension(affFile.getName()) + EXTENSION_DIC);
	}

	public File getAidFile(){
		return new File(getCurrentWorkingDirectory() + FOLDER_AID + affParser.getAffixData().getLanguage() + EXTENSION_AID);
	}

	private String getCurrentWorkingDirectory(){
		final String codePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		return codePath
			.replaceFirst("(classes/)?[^/]*$", StringUtils.EMPTY)
			.replaceAll("%20", StringUtils.SPACE);
	}

	private File getThesaurusIndexFile(){
		return packager.getThesaurusIndexFile();
	}

	public File getThesaurusDataFile(){
		return packager.getThesaurusDataFile();
	}

	public File getHyphenationFile(){
		return packager.getHyphenationFile();
	}

	public File getAutoCorrectFile(){
		return packager.getAutoCorrectFile();
	}

	public File getSentenceExceptionsFile(){
		return packager.getSentenceExceptionsFile();
	}

	public File getWordExceptionsFile(){
		return packager.getWordExceptionsFile();
	}

	public File getAutoTextFile(){
		return packager.getAutoTextFile();
	}


	@Override
	public void fileDeleted(final Path path){
		LOGGER.info(MARKER_APPLICATION, "File {} deleted", path.toFile().getName());

		final String absolutePath = affFile.getParent() + File.separator + path.toString();
		if(hasAFFExtension(absolutePath)){
			affParser.clear();

			Optional.ofNullable(hunspellable)
				.ifPresent(Hunspellable::clearAffixParser);
		}
		else if(hasAIDExtension(absolutePath)){
			aidParser.clear();

			Optional.ofNullable(hunspellable)
				.ifPresent(Hunspellable::clearAidParser);
		}
		else if(isAutoCorrectFile(absolutePath)){
			acoParser.clear();

			Optional.ofNullable(hunspellable)
				.ifPresent(Hunspellable::clearAutoCorrectParser);
		}
		else if(isSentenceExceptionsFile(absolutePath)){
			sexParser.clear();

			Optional.ofNullable(hunspellable)
				.ifPresent(Hunspellable::clearSentenceExceptionsParser);
		}
		else if(isWordExceptionsFile(absolutePath)){
			wexParser.clear();

			Optional.ofNullable(hunspellable)
				.ifPresent(Hunspellable::clearWordExceptionsParser);
		}
//		else if(isAutoTextFile(absolutePath)){
//			atxParser.clear();
//
//			Optional.ofNullable(hunspellable)
//				.ifPresent(Hunspellable::clearAutoTextParser);
//		}
	}

	@Override
	public void fileModified(final Path path){
		LOGGER.info(MARKER_APPLICATION, "File {} modified, reloading", path.toString());

		if(hunspellable != null)
			hunspellable.loadFileInternal(affFile.getAbsolutePath());
	}

	private boolean hasAFFExtension(final String path){
		return path.endsWith(EXTENSION_AFF);
	}

	private boolean hasAIDExtension(final String path){
		return path.endsWith(EXTENSION_AID);
	}

	private boolean isAutoCorrectFile(final String path){
		return path.equals(packager.getAutoCorrectFile().getAbsolutePath());
	}

	private boolean isSentenceExceptionsFile(final String path){
		return path.equals(packager.getSentenceExceptionsFile().getAbsolutePath());
	}

	private boolean isWordExceptionsFile(final String path){
		return path.equals(packager.getWordExceptionsFile().getAbsolutePath());
	}

	private boolean isAutoTextFile(final String path){
		return path.equals(packager.getAutoTextFile().getAbsolutePath());
	}


	public String[] getDictionaryLines() throws IOException{
		final File dicFile = getDictionaryFile();
		return Files.lines(dicFile.toPath(), affParser.getAffixData().getCharset())
			.map(line -> StringUtils.replace(line, TAB, TAB_SPACES))
			.toArray(String[]::new);
	}

	public void mergeSectionsToDictionary(final List<File> files) throws IOException{
		OpenOption option = StandardOpenOption.TRUNCATE_EXISTING;
		for(File file : files){
			Files.write(getDictionaryFile().toPath(), Files.readAllBytes(file.toPath()), option);

			option = StandardOpenOption.APPEND;
		}
	}

	public boolean hasHyphenationRule(final String addedRule, final HyphenationParser.Level level){
		return hypParser.hasRule(addedRule, level);
	}

	public String addHyphenationRule(final String newRule, final HyphenationParser.Level level){
		return hypParser.addRule(newRule, level);
	}

	public boolean restorePreviousThesaurusSnapshot() throws IOException{
		final boolean restored = theParser.restorePreviousSnapshot();
		if(restored)
			storeThesaurusFiles();

		return restored;
	}

	public boolean restoreNextThesaurusSnapshot() throws IOException{
		final boolean restored = theParser.restoreNextSnapshot();
		if(restored)
			storeThesaurusFiles();

		return restored;
	}

	public void createPackage(){
		packager.createPackage(affFile, getAffixData().getLanguage());
	}

}
