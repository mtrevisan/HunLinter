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
package io.github.mtrevisan.hunlinter.parsers;

import io.github.mtrevisan.hunlinter.MainFrame;
import io.github.mtrevisan.hunlinter.gui.events.PreLoadProjectEvent;
import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.languages.DictionaryCorrectnessChecker;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixParser;
import io.github.mtrevisan.hunlinter.parsers.aid.AidParser;
import io.github.mtrevisan.hunlinter.parsers.autocorrect.AutoCorrectParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.parsers.exceptions.ExceptionsParser;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenationParser;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.Hyphenator;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenatorInterface;
import io.github.mtrevisan.hunlinter.parsers.thesaurus.ThesaurusParser;
import io.github.mtrevisan.hunlinter.services.Packager;
import io.github.mtrevisan.hunlinter.services.eventbus.EventBusService;
import io.github.mtrevisan.hunlinter.services.filelistener.FileChangeListener;
import io.github.mtrevisan.hunlinter.services.filelistener.FileListenerManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ParserManager implements FileChangeListener{

	private static final Logger LOGGER = LoggerFactory.getLogger(ParserManager.class);

	public static final Marker MARKER_APPLICATION = MarkerFactory.getMarker("application");
	public static final Marker MARKER_RULE_REDUCER = MarkerFactory.getMarker("rule-reducer");
	public static final Marker MARKER_RULE_REDUCER_STATUS = MarkerFactory.getMarker("rule-reducer-status");
	public static final Marker MARKER_AFFIX_EXTRACTOR = MarkerFactory.getMarker("affix-extractor");
	public static final Marker MARKER_AFFIX_EXTRACTOR_STATUS = MarkerFactory.getMarker("affix-extractor-status");

	private static final String EXTENSION_AID = ".aid";
	private static final String FOLDER_AID = "aids";

	private static final String TAB = "\t";
	private static final String TAB_SPACES = StringUtils.repeat(' ', 3);


	private final AffixParser affParser;
	private final AidParser aidParser;
	private DictionaryParser dicParser;
	private ThesaurusParser theParser;
	private HyphenationParser hypParser;

	private HyphenatorInterface hyphenator;
	private DictionaryCorrectnessChecker checker;
	private WordGenerator wordGenerator;

	private final AutoCorrectParser acoParser;
	private final ExceptionsParser sexParser;
	private final ExceptionsParser wexParser;

	private final FileListenerManager flm;

	private final Packager packager;


	public ParserManager(final Packager packager){
		Objects.requireNonNull(packager, "Packager cannot be null");

		affParser = new AffixParser();
		aidParser = new AidParser();
		acoParser = new AutoCorrectParser();
		sexParser = new ExceptionsParser(Packager.FILENAME_SENTENCE_EXCEPTIONS);
		wexParser = new ExceptionsParser(Packager.FILENAME_WORD_EXCEPTIONS);

		flm = new FileListenerManager();

		this.packager = packager;
	}

	public final AffixParser getAffParser(){
		return affParser;
	}

	public final AffixData getAffixData(){
		return affParser.getAffixData();
	}

	public final String getLanguage(){
		return affParser.getLanguage();
	}

	public final AidParser getAidParser(){
		return aidParser;
	}

	public final DictionaryParser getDicParser(){
		return dicParser;
	}

	public final ThesaurusParser getTheParser(){
		return theParser;
	}

	public final HyphenationParser getHypParser(){
		return hypParser;
	}

	public final HyphenatorInterface getHyphenator(){
		return hyphenator;
	}

	public final AutoCorrectParser getAcoParser(){
		return acoParser;
	}

	public final ExceptionsParser getSexParser(){
		return sexParser;
	}

	public final ExceptionsParser getWexParser(){
		return wexParser;
	}

	public final DictionaryCorrectnessChecker getChecker(){
		return checker;
	}

	public final WordGenerator getWordGenerator(){
		return wordGenerator;
	}

	public final void startFileListener(){
		registerFileListener();

		flm.start();
	}

	private void registerFileListener(){
		flm.unregisterAll();

		final File affFile = packager.getAffixFile();
		final File dicFile = packager.getDictionaryFile();
		final File hypFile = packager.getHyphenationFile();
		final File aidFile = getAidFile();
		final File sexFile = packager.getSentenceExceptionsFile();
		final File wexFile = packager.getWordExceptionsFile();
		final File[] files = {affFile, dicFile, hypFile, aidFile, sexFile, wexFile};
		for(final File file : files)
			if(file != null)
				flm.register(this, file.getAbsolutePath());
	}

	public final void stopFileListener(){
		flm.unregisterAll();

		flm.stop();
	}

	@SuppressWarnings("OverlyBroadThrowsClause")
	public final void openAffixFile(final File affFile) throws IOException{
		if(!affFile.exists()){
			EventBusService.publish(MainFrame.ACTION_COMMAND_PARSER_CLEAR_AFFIX);

			throw new FileNotFoundException("The file '" + affFile.getCanonicalPath() + "' doesn't exists");
		}

		LOGGER.info(MARKER_APPLICATION, "Opening Affix file: {}", affFile.getName());

		affParser.parse(affFile, packager.getLanguage());

		LOGGER.info(MARKER_APPLICATION, "Finished reading Affix file");
	}

	public final void openHyphenationFile(final File hypFile){
		if(hypFile != null && hypFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening Hyphenation file: {}", hypFile.getName());

			final String language = affParser.getLanguage();
			final Comparator<String> comparator = BaseBuilder.getComparator(language);
			hypParser = new HyphenationParser(comparator);
			hypParser.parse(hypFile);

			hyphenator = new Hyphenator(hypParser, HyphenationParser.BREAK_CHARACTER);

			LOGGER.info(MARKER_APPLICATION, "Finished reading Hyphenation file");
		}
		else if(hypParser != null)
			EventBusService.publish(MainFrame.ACTION_COMMAND_PARSER_CLEAR_HYPHENATION);
	}

	public final void getCorrectnessChecker(){
		Objects.requireNonNull(affParser, "Affix parser cannot be null");

		checker = BaseBuilder.getCorrectnessChecker(affParser.getAffixData(), hyphenator);
	}

	public final void prepareDictionaryFile(final File dicFile){
		final AffixData affixData = affParser.getAffixData();
		if(dicFile.exists()){
			final String language = affixData.getLanguage();
			final Charset charset = affixData.getCharset();
			dicParser = new DictionaryParser(dicFile, language, charset);
		}
		else if(dicParser != null)
			EventBusService.publish(MainFrame.ACTION_COMMAND_PARSER_CLEAR_DICTIONARY);

		wordGenerator = new WordGenerator(affixData, dicParser, checker);
	}

	public final void openAidFile(final File aidFile) throws IOException{
		if(aidFile != null && aidFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening Aid file: {}", aidFile.getName());

			aidParser.parse(aidFile);

			LOGGER.info(MARKER_APPLICATION, "Finished reading Aid file");
		}
		else
			EventBusService.publish(MainFrame.ACTION_COMMAND_PARSER_CLEAR_AID);
	}

	public final void openThesaurusFile(final File theDataFile) throws IOException{
		if(theDataFile != null && theDataFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening Thesaurus file: {}", theDataFile.getName());

			theParser = new ThesaurusParser(packager.getLanguage());
			theParser.parse(theDataFile);

			LOGGER.info(MARKER_APPLICATION, "Finished reading Thesaurus file");
		}
		else
			EventBusService.publish(MainFrame.ACTION_COMMAND_PARSER_CLEAR_THESAURUS);
	}

	public final void openAutoCorrectFile(final File acoFile) throws IOException, SAXException{
		if(acoFile != null && acoFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening Auto-Correct file: {}", acoFile.getName());

			acoParser.parse(acoFile);

			LOGGER.info(MARKER_APPLICATION, "Finished reading Auto-Correct file");
		}
		else
			EventBusService.publish(MainFrame.ACTION_COMMAND_PARSER_CLEAR_AUTO_CORRECT);
	}

	public final void openSentenceExceptionsFile(final File sexFile) throws IOException, SAXException{
		if(sexFile != null && sexFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening Sentence Exceptions file: {}", sexFile.getName());

			final String language = affParser.getLanguage();
			sexParser.parse(sexFile, language);

			LOGGER.info(MARKER_APPLICATION, "Finished reading Sentence Exceptions file");
		}
		else
			EventBusService.publish(MainFrame.ACTION_COMMAND_PARSER_CLEAR_SENTENCE_EXCEPTION);
	}

	public final void openWordExceptionsFile(final File wexFile) throws IOException, SAXException{
		if(wexFile != null && wexFile.exists()){
			LOGGER.info(MARKER_APPLICATION, "Opening Word Exceptions file: {}", wexFile.getName());

			final String language = affParser.getLanguage();
			wexParser.parse(wexFile, language);

			LOGGER.info(MARKER_APPLICATION, "Finished reading Word Exceptions file");
		}
		else
			EventBusService.publish(MainFrame.ACTION_COMMAND_PARSER_CLEAR_WORD_EXCEPTION);
	}


	public final void storeHyphenationFile() throws IOException{
		final File hypFile = packager.getHyphenationFile();
		hypParser.save(hypFile);
	}

	public final void storeThesaurusFiles() throws IOException{
		final File theIndexFile = packager.getThesaurusIndexFile();
		final File theDataFile = packager.getThesaurusDataFile();
		theParser.save(theIndexFile, theDataFile);
	}

	public final void storeSentenceExceptionFile() throws TransformerException{
		final File sexFile = packager.getSentenceExceptionsFile();
		sexParser.save(sexFile);
	}

	public final void storeWordExceptionFile() throws TransformerException{
		final File wexFile = packager.getWordExceptionsFile();
		wexParser.save(wexFile);
	}

	public final void storeAutoCorrectFile() throws TransformerException{
		final File acoFile = packager.getAutoCorrectFile();
		acoParser.save(acoFile);
	}


	public final File getAidFile(){
		return Path.of(FOLDER_AID, affParser.getLanguage() + EXTENSION_AID)
			.toFile();
	}


	@Override
	public final void fileDeleted(final Path path){
		LOGGER.info(MARKER_APPLICATION, "File {} deleted", path.getFileName());

		final File file = path.toFile();
		if(file.equals(packager.getAffixFile()))
			EventBusService.publish(MainFrame.ACTION_COMMAND_PARSER_CLEAR_AFFIX);
		else if(file.equals(packager.getDictionaryFile()))
			EventBusService.publish(MainFrame.ACTION_COMMAND_PARSER_CLEAR_DICTIONARY);
		else if(path.toString().endsWith(EXTENSION_AID))
			EventBusService.publish(MainFrame.ACTION_COMMAND_PARSER_CLEAR_AID);
		else if(file.equals(packager.getAutoCorrectFile()))
			EventBusService.publish(MainFrame.ACTION_COMMAND_PARSER_CLEAR_AUTO_CORRECT);
		else if(file.equals(packager.getSentenceExceptionsFile()))
			EventBusService.publish(MainFrame.ACTION_COMMAND_PARSER_CLEAR_SENTENCE_EXCEPTION);
		else if(file.equals(packager.getWordExceptionsFile()))
			EventBusService.publish(MainFrame.ACTION_COMMAND_PARSER_CLEAR_WORD_EXCEPTION);
//		else if(filePath.equals(packager.getAutoTextFile()))
//			EventBusService.publish(MainFrame.ACTION_COMMAND_PARSER_CLEAR_AUTO_TEXT);
	}

	@Override
	public final void fileModified(final Path path){
		if(path.toFile().toString().equals(packager.getDictionaryFile().toString()))
			//cannot refresh file because on sorting the dictionary is changed, leading to a useless reloading
			EventBusService.publish(MainFrame.ACTION_COMMAND_PARSER_RELOAD_DICTIONARY);
		else{
			LOGGER.info(MARKER_APPLICATION, "File {} modified, reloading", path.getFileName());

			EventBusService.publish(new PreLoadProjectEvent(packager.getProjectPath()));
		}
	}


	public final List<String> getDictionaryLines() throws IOException{
		final File dicFile = packager.getDictionaryFile();
		try(final Stream<String> lines = Files.lines(dicFile.toPath(), affParser.getAffixData().getCharset())){
			return lines
				.map(line -> StringUtils.replace(line, TAB, TAB_SPACES))
				.collect(Collectors.toList());
		}
	}

	public final boolean hasHyphenationRule(final String addedRule, final HyphenationParser.Level level){
		return hypParser.hasRule(addedRule, level);
	}

	public final String addHyphenationRule(final String newRule, final HyphenationParser.Level level){
		return hypParser.addRule(newRule, level);
	}

	public final void createPackage(){
		packager.createPackage(packager.getProjectPath(), getLanguage());
	}

}
