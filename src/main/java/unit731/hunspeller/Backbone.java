package unit731.hunspeller;

import unit731.hunspeller.interfaces.Hunspellable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
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
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.thesaurus.ThesaurusParser;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.filelistener.FileChangeListener;
import unit731.hunspeller.services.filelistener.FileListenerManager;


@Slf4j
public class Backbone implements FileChangeListener{

	public static final Marker MARKER_APPLICATION = MarkerFactory.getMarker("application");

	private static final String STAR = "*";
	private static final String EXTENSION_AFF = ".aff";
	private static final String EXTENSION_DIC = ".dic";
	private static final String EXTENSION_AID = ".aid";
	private static final String EXTENSION_THESAURUS_INDEX = ".idx";
	private static final String EXTENSION_THESAURUS_DATA = ".dat";
	private static final String PREFIX_THESAURUS = "th_";
	private static final String SUFFIX_THESAURUS = "_v2";
	private static final String PREFIX_HYPHENATION = "hyph_";
	private static final String FOLDER_AID = "aids/";


	private File affFile;

	private final AffixParser affParser;
	private final AidParser aidParser;
	private DictionaryParser dicParser;
	private final ThesaurusParser theParser;
	private HyphenationParser hypParser;

	private final WordGenerator wordGenerator;

	private final Hunspellable hunspellable;
	private final FileListenerManager flm;


	public Backbone(Hunspellable hunspellable, Undoable undoable){
		affParser = new AffixParser();
		aidParser = new AidParser();
		theParser = new ThesaurusParser(undoable);
		wordGenerator = new WordGenerator(affParser);

		this.hunspellable = hunspellable;
		flm = new FileListenerManager();
	}

	public void loadFile(String filePath) throws FileNotFoundException, IOException{
		stopFileListener();


		openAffixFile(filePath);

		File hypFile = getHyphenationFile();
		String language = affParser.getLanguage();
		openHyphenationFile(hypFile, language);

		File dicFile = getDictionaryFile();
		Charset charset = affParser.getCharset();
		prepareDictionaryFile(dicFile, language, charset);

		File aidFile = getAidFile();
		openAidFile(aidFile);

		File theFile = getThesaurusDataFile();
		openThesaurusFile(theFile);


		startFileListener();
	}

	private void stopFileListener(){
		flm.stop();
	}

	private void startFileListener() throws IOException{
		flm.register(this, affFile.getParent(), STAR + EXTENSION_AFF, STAR + EXTENSION_DIC, STAR + EXTENSION_AID);
		flm.start();
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

	private void openHyphenationFile(File hypFile, String language) throws IOException{
		if(hypFile.exists()){
			log.info(MARKER_APPLICATION, "Opening Hyphenation file for parsing: {}", hypFile.getName());

			hypParser = new HyphenationParser(language);
			hypParser.parse(hypFile);

			hunspellable.clearHyphenationParser();

			log.info(MARKER_APPLICATION, "Finished reading Hyphenation file");
		}
		else
			hypParser.clear();
	}

	private void prepareDictionaryFile(File dicFile, String language, Charset charset){
		if(dicFile.exists()){
			dicParser = DictionaryParserBuilder.getParser(language, dicFile, wordGenerator, charset);
			if(hypParser != null)
				dicParser.setHyphenator(hypParser.getHyphenator());

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


	private File getFile(String filename){
		return new File(affFile.toPath().getParent().toString() + File.separator + filename);
	}

	private File getDictionaryFile(){
		return getFile(affParser.getLanguage() + EXTENSION_DIC);
	}

	private File getAidFile(){
		return getFile(getCurrentWorkingDirectory() + FOLDER_AID + affParser.getLanguage() + EXTENSION_AID);
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
				String language = affParser.getLanguage();
				openHyphenationFile(hypFile, language);
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

}
