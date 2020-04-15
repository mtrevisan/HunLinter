package unit731.hunlinter.parsers;

import org.apache.commons.lang3.ArrayUtils;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import unit731.hunlinter.languages.DictionaryCorrectnessChecker;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.AffixParser;
import unit731.hunlinter.parsers.aid.AidParser;
import unit731.hunlinter.parsers.autocorrect.AutoCorrectParser;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;
import unit731.hunlinter.parsers.hyphenation.Hyphenator;
import unit731.hunlinter.parsers.hyphenation.HyphenatorInterface;
import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;
import unit731.hunlinter.parsers.exceptions.ExceptionsParser;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.filelistener.FileChangeListener;
import unit731.hunlinter.services.filelistener.FileListenerManager;

import javax.xml.transform.TransformerException;

import static unit731.hunlinter.services.system.LoopHelper.forEach;


public class ParserManager implements FileChangeListener{

	private static final Logger LOGGER = LoggerFactory.getLogger(ParserManager.class);

	public static final Marker MARKER_APPLICATION = MarkerFactory.getMarker("application");
	public static final Marker MARKER_RULE_REDUCER = MarkerFactory.getMarker("rule-reducer");

	private static final String EXTENSION_AID = ".aid";
	private static final String FOLDER_AID = "aids";

	private static final String TAB = "\t";
	private static final String TAB_SPACES = StringUtils.repeat(' ', 3);


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

	private final HunLintable hunLintable;
	private final FileListenerManager flm;

	private final Packager packager;


	public ParserManager(final Packager packager, final HunLintable hunLintable){
		Objects.requireNonNull(packager);

		affParser = new AffixParser();
		aidParser = new AidParser();
		theParser = new ThesaurusParser(packager.getLanguage());
		acoParser = new AutoCorrectParser();
		sexParser = new ExceptionsParser(Packager.FILENAME_SENTENCE_EXCEPTIONS);
		wexParser = new ExceptionsParser(Packager.FILENAME_WORD_EXCEPTIONS);

		this.hunLintable = hunLintable;
		flm = new FileListenerManager();

		this.packager = packager;
	}

	public AffixParser getAffParser(){
		return affParser;
	}

	public AffixData getAffixData(){
		return affParser.getAffixData();
	}

	public String getLanguage(){
		return affParser.getLanguage();
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

	public HyphenationParser getHypParser(){
		return hypParser;
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

	public void registerFileListener(){
		flm.unregisterAll();

		final File affFile = packager.getAffixFile();
		final File hypFile = packager.getHyphenationFile();
		final File aidFile = getAidFile();
		final File sexFile = packager.getSentenceExceptionsFile();
		final File wexFile = packager.getWordExceptionsFile();
		final File[] files = ArrayUtils.removeAllOccurences(new File[]{affFile, hypFile, aidFile, sexFile, wexFile},
			null);
		final List<String> uris = new ArrayList<>(files.length);
		forEach(files, file -> uris.add(file.getAbsolutePath()));
		flm.register(this, uris.toArray(String[]::new));
	}

	public void startFileListener(){
		flm.start();
	}

	public void stopFileListener(){
		flm.stop();
	}

	public void openAffixFile(final File affFile) throws IOException{
		if(!affFile.exists()){
			affParser.clear();

			if(hunLintable != null)
				hunLintable.clearAffixParser();

			throw new FileNotFoundException("The file '" + affFile.getCanonicalPath() + "' doesn't exists");
		}

		LOGGER.info(MARKER_APPLICATION, "Opening Affix file: {}", affFile.getName());

		affParser.parse(affFile, packager.getLanguage());

		LOGGER.info(MARKER_APPLICATION, "Finished reading Affix file");
	}

	public void openHyphenationFile(final File hypFile){
		if(hypFile != null && hypFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening Hyphenation file: {}", hypFile.getName());

			final String language = affParser.getLanguage();
			final Comparator<String> comparator = BaseBuilder.getComparator(language);
			hypParser = new HyphenationParser(comparator);
			hypParser.parse(hypFile);

			hyphenator = new Hyphenator(hypParser, HyphenationParser.BREAK_CHARACTER);

			if(hunLintable != null)
				hunLintable.clearHyphenationParser();

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

			if(hunLintable != null)
				hunLintable.clearDictionaryParser();
		}
		else if(dicParser != null)
			dicParser.clear();

		wordGenerator = new WordGenerator(affixData, dicParser);
	}

	public void openAidFile(final File aidFile) throws IOException{
		if(aidFile != null && aidFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening Aid file: {}", aidFile.getName());

			aidParser.parse(aidFile);

			if(hunLintable != null)
				hunLintable.clearAidParser();

			LOGGER.info(MARKER_APPLICATION, "Finished reading Aid file");
		}
		else
			aidParser.clear();
	}

	public void openThesaurusFile(final File theDataFile) throws IOException{
		if(theDataFile != null && theDataFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening Thesaurus file: {}", theDataFile.getName());

			theParser.parse(theDataFile);

			if(hunLintable != null)
				hunLintable.clearThesaurusParser();

			LOGGER.info(MARKER_APPLICATION, "Finished reading Thesaurus file");
		}
		else
			theParser.clear();
	}

	public void openAutoCorrectFile(final File acoFile) throws IOException, SAXException{
		if(acoFile != null && acoFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening Auto–Correct file: {}", acoFile.getName());

			acoParser.parse(acoFile);

			if(hunLintable != null)
				hunLintable.clearAutoCorrectParser();

			LOGGER.info(MARKER_APPLICATION, "Finished reading Auto–Correct file");
		}
		else
			acoParser.clear();
	}

	public void openSentenceExceptionsFile(final File sexFile) throws IOException, SAXException{
		if(sexFile != null && sexFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening Sentence Exceptions file: {}", sexFile.getName());

			final String language = affParser.getLanguage();
			sexParser.parse(sexFile, language);

			if(hunLintable != null)
				hunLintable.clearSentenceExceptionsParser();

			LOGGER.info(MARKER_APPLICATION, "Finished reading Sentence Exceptions file");
		}
		else
			sexParser.clear();
	}

	public void openWordExceptionsFile(final File wexFile) throws IOException, SAXException{
		if(wexFile != null && wexFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening Word Exceptions file: {}", wexFile.getName());

			final String language = affParser.getLanguage();
			wexParser.parse(wexFile, language);

			if(hunLintable != null)
				hunLintable.clearWordExceptionsParser();

			LOGGER.info(MARKER_APPLICATION, "Finished reading Word Exceptions file");
		}
		else
			wexParser.clear();
	}


	public void storeHyphenationFile() throws IOException{
		final File hypFile = packager.getHyphenationFile();
		hypParser.save(hypFile);
	}

	public void storeThesaurusFiles() throws IOException{
		final File theIndexFile = packager.getThesaurusIndexFile();
		final File theDataFile = packager.getThesaurusDataFile();
		theParser.save(theIndexFile, theDataFile);
	}

	public void storeSentenceExceptionFile() throws TransformerException{
		final File sexFile = packager.getSentenceExceptionsFile();
		sexParser.save(sexFile);
	}

	public void storeWordExceptionFile() throws TransformerException{
		final File wexFile = packager.getWordExceptionsFile();
		wexParser.save(wexFile);
	}

	public void storeAutoCorrectFile() throws TransformerException{
		final File acoFile = packager.getAutoCorrectFile();
		acoParser.save(acoFile);
	}


	public File getAidFile(){
		return Path.of(FOLDER_AID, affParser.getLanguage() + EXTENSION_AID)
			.toFile();
	}


	@Override
	public void fileDeleted(final Path path){
		//NOTE: `path` it's only the filename
		LOGGER.info(MARKER_APPLICATION, "File {} deleted", path.toFile().getName());

		final Path filePath = Path.of(packager.getProjectPath().toString(), path.toString());
		if(filePath.toFile().equals(packager.getAffixFile())){
			affParser.clear();

			Optional.ofNullable(hunLintable)
				.ifPresent(HunLintable::clearAffixParser);
		}
		else if(path.toString().endsWith(EXTENSION_AID)){
			aidParser.clear();

			Optional.ofNullable(hunLintable)
				.ifPresent(HunLintable::clearAidParser);
		}
		else if(filePath.toFile().equals(packager.getAutoCorrectFile())){
			acoParser.clear();

			Optional.ofNullable(hunLintable)
				.ifPresent(HunLintable::clearAutoCorrectParser);
		}
		else if(filePath.toFile().equals(packager.getSentenceExceptionsFile())){
			sexParser.clear();

			Optional.ofNullable(hunLintable)
				.ifPresent(HunLintable::clearSentenceExceptionsParser);
		}
		else if(filePath.toFile().equals(packager.getWordExceptionsFile())){
			wexParser.clear();

			Optional.ofNullable(hunLintable)
				.ifPresent(HunLintable::clearWordExceptionsParser);
		}
//		else if(filePath.equals(packager.getAutoTextFile())){
//			atxParser.clear();
//
//			Optional.ofNullable(hunlintable)
//				.ifPresent(HunLintable::clearAutoTextParser);
//		}
	}

	@Override
	public void fileModified(final Path path){
		LOGGER.info(MARKER_APPLICATION, "File {} modified, reloading", path.toString());

		if(hunLintable != null)
			hunLintable.loadFileInternal(null);
	}


	public String[] getDictionaryLines() throws IOException{
		final File dicFile = packager.getDictionaryFile();
		return Files.lines(dicFile.toPath(), affParser.getAffixData().getCharset())
			.map(line -> StringUtils.replace(line, TAB, TAB_SPACES))
			.toArray(String[]::new);
	}

	public boolean hasHyphenationRule(final String addedRule, final HyphenationParser.Level level){
		return hypParser.hasRule(addedRule, level);
	}

	public String addHyphenationRule(final String newRule, final HyphenationParser.Level level){
		return hypParser.addRule(newRule, level);
	}

	public void createPackage(){
		packager.createPackage(packager.getProjectPath(), getLanguage());
	}

}
