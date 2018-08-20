package unit731.hunspeller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.zip.Deflater;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import unit731.hunspeller.interfaces.Undoable;
import unit731.hunspeller.languages.CorrectnessChecker;
import unit731.hunspeller.languages.builders.CorrectnessCheckerBuilder;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.aid.AidParser;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.hyphenators.AbstractHyphenator;
import unit731.hunspeller.parsers.hyphenation.hyphenators.Hyphenator;
import unit731.hunspeller.parsers.thesaurus.ThesaurusParser;
import unit731.hunspeller.services.ZipManager;
import unit731.hunspeller.services.filelistener.FileChangeListener;
import unit731.hunspeller.services.filelistener.FileListenerManager;


@Slf4j
public class Backbone implements FileChangeListener{

	public static final Marker MARKER_APPLICATION = MarkerFactory.getMarker("application");

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

	@Getter
	private final AffixParser affParser;
	@Getter
	private final AidParser aidParser;
	@Getter
	private DictionaryParser dicParser;
	@Getter
	private final ThesaurusParser theParser;
	private HyphenationParser hypParser;

	@Getter
	private WordGenerator wordGenerator;
	@Getter
	private CorrectnessChecker checker;
	@Getter
	private AbstractHyphenator hyphenator;

	private final Hunspellable hunspellable;
	private final FileListenerManager flm;


	public Backbone(Hunspellable hunspellable, Undoable undoable){
		affParser = new AffixParser();
		aidParser = new AidParser();
		theParser = new ThesaurusParser(undoable);

		this.hunspellable = hunspellable;
		flm = new FileListenerManager();
	}

	public void loadFile(String filePath) throws FileNotFoundException, IOException{
		openAffixFile(filePath);

		File hypFile = getHyphenationFile();
		openHyphenationFile(hypFile);

		File dicFile = getDictionaryFile();
		prepareDictionaryFile(dicFile);

		File aidFile = getAidFile();
		openAidFile(aidFile);

		File theFile = getThesaurusDataFile();
		openThesaurusFile(theFile);
	}

	public void registerFileListener() throws IOException{
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

	private void openAffixFile(String filePath) throws IOException{
		affFile = new File(filePath);

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

			String language = affParser.getLanguage();
			hypParser = new HyphenationParser(language);
			hypParser.parse(hypFile);

			wordGenerator = new WordGenerator(affParser);
			hyphenator = new Hyphenator(hypParser, HyphenationParser.BREAK_CHARACTER);
			checker = CorrectnessCheckerBuilder.getParser(language, affParser, hyphenator);

			hunspellable.clearHyphenationParser();

			log.info(MARKER_APPLICATION, "Finished reading Hyphenation file");
		}
		else if(hypParser != null)
			hypParser.clear();
	}

	private void prepareDictionaryFile(File dicFile){
		if(dicFile.exists()){
			String language = affParser.getLanguage();
			Charset charset = affParser.getCharset();
			dicParser = new DictionaryParser(dicFile, language, charset);

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

	/* FIXME should be private!? */
	public File getDictionaryFile(){
		return getFile(affParser.getLanguage() + EXTENSION_DIC);
	}

	private File getAidFile(){
		return new File(getCurrentWorkingDirectory() + FOLDER_AID + affParser.getLanguage() + EXTENSION_AID);
	}

	private String getCurrentWorkingDirectory(){
		String codePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
		return codePath
			.replaceFirst("(classes/)?[^/]*$", StringUtils.EMPTY)
			.replaceAll("%20", StringUtils.SPACE);
	}

	private File getThesaurusIndexFile(){
		return getFile(PREFIX_THESAURUS + affParser.getLanguage() + SUFFIX_THESAURUS + EXTENSION_THESAURUS_INDEX);
	}

	private File getThesaurusDataFile(){
		return getFile(PREFIX_THESAURUS + affParser.getLanguage() + SUFFIX_THESAURUS + EXTENSION_THESAURUS_DATA);
	}

	private File getHyphenationFile(){
		return getFile(PREFIX_HYPHENATION + affParser.getLanguage() + EXTENSION_DIC);
	}


	@Override
	public void fileDeleted(Path path){
		log.info(MARKER_APPLICATION, "File {} deleted", path.toFile().getName());

		String absolutePath = affFile.getParent() + File.separator + path.toString();
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
		log.info(MARKER_APPLICATION, "File {} modified, reloading", path.toString());

		hunspellable.loadFileInternal(affFile.getAbsolutePath());
	}

	private boolean hasAFFExtension(String path){
		return path.endsWith(EXTENSION_AFF);
	}

	@SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "Deliberate")
	private boolean hasDICExtension(String path){
		return path.endsWith(EXTENSION_DIC);
	}

	@SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD", justification = "Deliberate")
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
			log.info(Backbone.MARKER_APPLICATION, "Found base path on {}", basePath.toString());

			try{
				String outputFilename = basePath.toString() + File.separator + basePath.getName(basePath.getNameCount() - 1) + ".zip";
				ZIPPER.zipDirectory(basePath.toFile(), Deflater.BEST_COMPRESSION, outputFilename);

				log.info(Backbone.MARKER_APPLICATION, "Package created");

				//open directory
				if(Desktop.isDesktopSupported())
					Desktop.getDesktop().open(new File(basePath.toString()));
			}
			catch(IOException e){
				log.info(Backbone.MARKER_APPLICATION, "Package error: {}", e.getMessage());

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


	public String[] getDictionaryLines() throws IOException{
		File dicFile = getDictionaryFile();
		String[] lines = Files.lines(dicFile.toPath(), affParser.getCharset())
			.map(line -> StringUtils.replace(line, TAB, TAB_SPACES))
			.toArray(String[]::new);
		return lines;
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
