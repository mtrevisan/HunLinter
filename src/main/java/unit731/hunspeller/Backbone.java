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
import java.util.Map;
import java.util.function.Supplier;
import java.util.zip.Deflater;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import unit731.hunspeller.interfaces.Undoable;
import unit731.hunspeller.languages.builders.DictionaryParserBuilder;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.aid.AidParser;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.hyphenators.AbstractHyphenator;
import unit731.hunspeller.parsers.hyphenation.hyphenators.Hyphenator;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.thesaurus.ThesaurusParser;
import unit731.hunspeller.parsers.thesaurus.dtos.DuplicationResult;
import unit731.hunspeller.parsers.thesaurus.dtos.MeaningEntry;
import unit731.hunspeller.parsers.thesaurus.dtos.ThesaurusEntry;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.ZipManager;
import unit731.hunspeller.services.externalsorter.ExternalSorter;
import unit731.hunspeller.services.filelistener.FileChangeListener;
import unit731.hunspeller.services.filelistener.FileListenerManager;


@Slf4j
public class Backbone implements FileChangeListener{

	public static final Marker MARKER_APPLICATION = MarkerFactory.getMarker("application");

	private static final ZipManager ZIPPER = new ZipManager();

	private static final String ASTERISK = "*";
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

	private final WordGenerator wordGenerator;
	private final AbstractHyphenator hyphenator;

	private final Hunspellable hunspellable;
	private final FileListenerManager flm;


	public Backbone(Hunspellable hunspellable, Undoable undoable){
		affParser = new AffixParser();
		aidParser = new AidParser();
		theParser = new ThesaurusParser(undoable);
		wordGenerator = new WordGenerator(affParser);
		hyphenator = new Hyphenator(hypParser, HyphenationParser.BREAK_CHARACTER);

		this.hunspellable = hunspellable;
		flm = new FileListenerManager();
	}

	public void loadFile(String filePath) throws FileNotFoundException, IOException{
		stopFileListener();


		openAffixFile(filePath);

		File hypFile = getHyphenationFile();
		openHyphenationFile(hypFile);

		File dicFile = getDictionaryFile();
		prepareDictionaryFile(dicFile);

		File aidFile = getAidFile();
		openAidFile(aidFile);

		File theFile = getThesaurusDataFile();
		openThesaurusFile(theFile);


		flm.register(this, affFile.getParent(), ASTERISK + EXTENSION_AFF, ASTERISK + EXTENSION_DIC, ASTERISK + EXTENSION_AID);

		startFileListener();
	}

	public void stopFileListener(){
		flm.stop();
	}

	public void startFileListener() throws IOException{
		flm.start();
	}

	private void openAffixFile(String filePath) throws IOException{
		affFile = new File(filePath);

		log.info(Backbone.MARKER_APPLICATION, "Loading file {}", affFile.getName());

		if(!affFile.exists()){
			affParser.clear();

			hunspellable.clearAffixParser();

			throw new FileNotFoundException("The file does not exists");
		}

		log.info(MARKER_APPLICATION, "Opening Affix file for parsing: {}", affFile.getName());

		affParser.parse(affFile);

		log.info(MARKER_APPLICATION, "Finished reading Affix file");
	}

	private void openHyphenationFile(File hypFile) throws IOException{
		if(hypFile.exists()){
			log.info(MARKER_APPLICATION, "Opening Hyphenation file for parsing: {}", hypFile.getName());

			String language = getLanguage();
			hypParser = new HyphenationParser(language);
			hypParser.parse(hypFile);

			hunspellable.clearHyphenationParser();

			log.info(MARKER_APPLICATION, "Finished reading Hyphenation file");
		}
		else
			hypParser.clear();
	}

	private void prepareDictionaryFile(File dicFile){
		if(dicFile.exists()){
			String language = getLanguage();
			Charset charset = getCharset();
			dicParser = DictionaryParserBuilder.getParser(language, dicFile, wordGenerator, hyphenator, charset);

			hunspellable.clearDictionaryParser();
		}
		else
			dicParser.clear();
	}

	private void openAidFile(File aidFile) throws IOException{
		if(aidFile.exists()){
			log.info(MARKER_APPLICATION, "Opening Aid file for parsing: {}", aidFile.getName());

			aidParser.parse(aidFile);

			hunspellable.clearAidParser();

			log.info(MARKER_APPLICATION, "Finished reading Aid file");
		}
		else
			aidParser.clear();
	}

	private void openThesaurusFile(File theFile) throws IOException{
		if(theFile.exists()){
			log.info(MARKER_APPLICATION, "Opening Thesaurus file for parsing: {}", theFile.getName());

			theParser.parse(theFile);

			hunspellable.clearThesaurusParser();

			log.info(MARKER_APPLICATION, "Finished reading Thesaurus file");
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

	/** FIXME should be private! */
	public File getDictionaryFile(){
		return getFile(getLanguage() + EXTENSION_DIC);
	}

	private File getAidFile(){
		return getFile(getCurrentWorkingDirectory() + FOLDER_AID + getLanguage() + EXTENSION_AID);
	}

	private String getCurrentWorkingDirectory(){
		String codePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		return codePath
			.replaceFirst("(classes/)?[^/]*$", StringUtils.EMPTY)
			.replaceAll("%20", StringUtils.SPACE);
	}

	private File getThesaurusIndexFile(){
		return getFile(PREFIX_THESAURUS + getLanguage() + SUFFIX_THESAURUS + EXTENSION_THESAURUS_INDEX);
	}

	private File getThesaurusDataFile(){
		return getFile(PREFIX_THESAURUS + getLanguage() + SUFFIX_THESAURUS + EXTENSION_THESAURUS_DATA);
	}

	private File getHyphenationFile(){
		return getFile(PREFIX_HYPHENATION + getLanguage() + EXTENSION_DIC);
	}


	@Override
	public void fileDeleted(Path path){
		log.info(MARKER_APPLICATION, "File {} deleted", path.toFile().getName());

		String absolutePath = path.toString().toLowerCase();
		if(hasAFFExtension(absolutePath)){
			affParser.clear();

			hunspellable.clearAffixParser();
		}
		else if(hasAIDExtension(absolutePath)){
			aidParser.clear();

			hunspellable.clearAidParser();
		}
	}

	@Override
	public void fileModified(Path path){
		log.info(MARKER_APPLICATION, "File {} modified", path.toFile().getName());

		try{
			String absolutePath = path.toString().toLowerCase();
			if(hasAFFExtension(absolutePath))
				openAffixFile(absolutePath);
			else if(isHyphenationFile(absolutePath)){
				File hypFile = getHyphenationFile();
				openHyphenationFile(hypFile);
			}
			else if(hasAIDExtension(absolutePath)){
				File aidFile = getAidFile();
				openAidFile(aidFile);
			}
		}
		catch(IOException e){
			String message = ExceptionService.getMessage(e);
			log.error(MARKER_APPLICATION, e.getClass().getSimpleName() + ": " + message, e);
		}
	}

	private boolean hasAFFExtension(String path){
		return path.endsWith(EXTENSION_AFF);
	}

	private boolean isHyphenationFile(String path){
		String baseName = FilenameUtils.getBaseName(path);
		return (baseName.startsWith("hyph_") && path.endsWith(EXTENSION_DIC));
	}

	private boolean hasAIDExtension(String path){
		return path.endsWith(EXTENSION_AID);
	}


	public void createPackage(){
		Path basePath = getPackageBaseDirectory();

		//package entire folder with ZIP
		if(basePath != null){
			log.info(Backbone.MARKER_APPLICATION, "Found base path on " + basePath.toString());

			try{
				String outputFilename = basePath.toString() + File.separator + basePath.getName(basePath.getNameCount() - 1) + ".zip";
				ZIPPER.zipDirectory(basePath.toFile(), Deflater.BEST_COMPRESSION, outputFilename);

				log.info(Backbone.MARKER_APPLICATION, "Package created");

				//open directory
				if(Desktop.isDesktopSupported())
					Desktop.getDesktop().open(new File(basePath.toString()));
			}
			catch(IOException e){
				log.info(Backbone.MARKER_APPLICATION, "Package error: " + e.getMessage());

				log.error("Something very bad happend while creating package", e);
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


	public Charset getCharset(){
		return affParser.getCharset();
	}

	public String getLanguage(){
		return affParser.getLanguage();
	}

	public long getDictionaryFileLength(){
		return dicParser.getDicFile().length();
	}

	public String[] getDictionaryLines() throws IOException{
		File dicFile = getDictionaryFile();
		String[] lines = Files.lines(dicFile.toPath(), getCharset())
			.map(line -> StringUtils.replace(line, TAB, TAB_SPACES))
			.toArray(String[]::new);
		return lines;
	}

	public int getExpectedNumberOfDictionaryElements(){
		return dicParser.getExpectedNumberOfElements();
	}

	public double getFalsePositiveDictionaryProbability(){
		return dicParser.getFalsePositiveProbability();
	}

	public double getGrowRatioWhenDictionaryFull(){
		return dicParser.getGrowRatioWhenFull();
	}

	public void checkDictionaryProduction(RuleProductionEntry production){
		dicParser.checkProduction(production);
	}

	public ExternalSorter getDictionarySorter(){
		return dicParser.getSorter();
	}

	public int getDictionaryBoundaryIndex(int row){
		return dicParser.getBoundaryIndex(row);
	}

	public int getDictionaryNextBoundaryIndex(int row){
		return dicParser.getNextBoundaryIndex(row);
	}

	public int getDictionaryPreviousBoundaryIndex(int row){
		return dicParser.getPreviousBoundaryIndex(row);
	}

	public boolean isDictionaryLineInBoundary(int lineIndex){
		return dicParser.isInBoundary(lineIndex);
	}

	public boolean shouldBeProcessedForMinimalPair(RuleProductionEntry production){
		return dicParser.shouldBeProcessedForMinimalPair(production);
	}

	public boolean isConsonant(char chr){
		return dicParser.isConsonant(chr);
	}

	public void calculateDictionaryBoundaries(){
		dicParser.calculateDictionaryBoundaries();
	}

	public Map.Entry<Integer, Integer> getDictionaryBoundary(int row){
		return dicParser.getBoundary(row);
	}

	public void clearDictionaryBoundaries(){
		dicParser.getBoundaries().clear();
	}

	public boolean isDictionaryModified(){
		return theParser.isDictionaryModified();
	}

	public void mergeSectionsToDictionary(List<File> files) throws IOException{
		OpenOption option = StandardOpenOption.TRUNCATE_EXISTING;
		for(File file : files){
			Files.write(getDictionaryFile().toPath(), Files.readAllBytes(file.toPath()), option);

			option = StandardOpenOption.APPEND;
		}
	}

	public List<RuleProductionEntry> applyRules(String line){
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		DictionaryEntry dicEntry = new DictionaryEntry(line, strategy);
		return wordGenerator.applyRules(dicEntry);
	}

	public String correctOrthography(String word){
		return dicParser.correctOrthography(word);
	}

	public List<String> splitWordIntoCompounds(String word){
		return hyphenator.splitIntoCompounds(word);
	}

	public boolean hasHyphenationRule(String addedRule, HyphenationParser.Level level){
		return hypParser.hasRule(addedRule, level);
	}

	public String addHyphenationRule(String newRule, HyphenationParser.Level level){
		return hypParser.addRule(newRule, level);
	}

	public Hyphenation hyphenate(String word){
		return hyphenator.hyphenate(word);
	}

	public Hyphenation hyphenate(String word, String addedRule, HyphenationParser.Level level){
		return hyphenator.hyphenate(word, addedRule, level);
	}

	public List<String> getAidLines(){
		return aidParser.getLines();
	}

	public List<ThesaurusEntry> getSynonymsDictionary(){
		return theParser.getSynonymsDictionary();
	}

	public int getSynonymsCounter(){
		return theParser.getSynonymsCounter();
	}

	public ThesaurusEntry getThesaurusSynonym(int row){
		return theParser.getSynonymsDictionary().get(row);
	}

	public void setThesaurusSynonym(int row, List<MeaningEntry> meanings, String text){
		theParser.setMeanings(row, meanings, text);
	}

	public List<String> extractThesaurusDuplicates(){
		return theParser.extractDuplicates();
	}

	public String prepareTextForThesaurusFilter(String text){
		return dicParser.prepareTextForFilter(text);
	}

	public DuplicationResult insertThesaurusMeanings(String synonyms, Supplier<Boolean> duplicatesDiscriminator){
		return theParser.insertMeanings(synonyms, duplicatesDiscriminator);
	}

	public void deleteThesaurusMeanings(int[] selectedRows){
		theParser.deleteMeanings(selectedRows);
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
