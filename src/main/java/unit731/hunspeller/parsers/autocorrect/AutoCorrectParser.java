package unit731.hunspeller.parsers.autocorrect;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.interfaces.Undoable;
import unit731.hunspeller.parsers.thesaurus.DuplicationResult;
import unit731.hunspeller.parsers.thesaurus.ThesaurusCaretaker;
import unit731.hunspeller.parsers.thesaurus.ThesaurusDictionary;
import unit731.hunspeller.parsers.thesaurus.ThesaurusEntry;
import unit731.hunspeller.services.FileHelper;
import unit731.hunspeller.services.PatternHelper;
import unit731.hunspeller.services.memento.CaretakerInterface;
import unit731.hunspeller.services.memento.OriginatorInterface;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * for storing mementoes:
 * https://github.com/dnaumenko/java-diff-utils
 * https://www.adictosaltrabajo.com/2012/06/05/comparar-ficheros-java-diff-utils/
 * https://github.com/java-diff-utils/java-diff-utils/wiki/Examples
 * <a href="https://github.com/java-diff-utils/java-diff-utils">java-diff-utils</a>
 * <a href="http://blog.robertelder.org/diff-algorithm/">Myers Diff Algorithm - Code & Interactive Visualization</a>
 */
public class AutoCorrectParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoCorrectParser.class);

	private static final MessageFormat WRONG_FILE_FORMAT = new MessageFormat("Thesaurus file malformed, the first line is not a charset, was ''{0}''");
	private static final MessageFormat WRONG_FORMAT = new MessageFormat("Wrong format, it must be one of '(<pos1, pos2, ...>)|meaning1|meaning2|...' or 'pos1, pos2, ...:meaning1,meaning2,...': was ''{0}''");
	private static final MessageFormat NOT_ENOUGH_MEANINGS = new MessageFormat("Not enough meanings are supplied (at least one should be present): was ''{0}''");
	private static final MessageFormat DUPLICATE_DETECTED = new MessageFormat("Duplicate detected for ''{0}''");

	private static final String PIPE = "|";

	private static final String PART_OF_SPEECH_START = "(";
	private static final String PART_OF_SPEECH_END = ")";

	private static final Pattern PATTERN_PARENTHESIS = PatternHelper.pattern("\\([^)]+\\)");

	private static final Pattern PATTERN_CLEAR_SEARCH = PatternHelper.pattern("\\s+\\([^)]+\\)");
	private static final Pattern PATTERN_FILTER_EMPTY = PatternHelper.pattern("^\\(.+?\\)((?<!\\\\)\\|)?|^(?<!\\\\)\\||(?<!\\\\)\\|$|\\/.*$");
	private static final Pattern PATTERN_FILTER_OR = PatternHelper.pattern("(,|\\|)+");

	private final AutoCorrectDictionary dictionary = new AutoCorrectDictionary();


	public AutoCorrectDictionary getDictionary(){
		return dictionary;
	}

	/**
	 * Parse the rows out from a DocumentList.xml file.
	 *
	 * @param acoFile	The content of the thesaurus file
	 * @throws IOException	If an I/O error occurs
	 */
	public void parse(final File acoFile) throws IOException{
		clear();

		final Path path = acoFile.toPath();
		final Charset charset = FileHelper.determineCharset(path);
		try(final LineNumberReader br = FileHelper.createReader(path, charset)){
			String line = extractLine(br);

			//line should be a charset
			try{ Charsets.toCharset(line); }
			catch(final UnsupportedCharsetException e){
				throw new IllegalArgumentException(WRONG_FILE_FORMAT.format(new Object[]{line}));
			}

			while((line = br.readLine()) != null)
				if(!line.isEmpty())
					dictionary.add(new ThesaurusEntry(line, br));
		}
	}

	private String extractLine(final LineNumberReader br) throws IOException{
		final String line = br.readLine();
		if(line == null)
			throw new EOFException("Unexpected EOF while reading Thesaurus file");

		return line;
	}

	public int getAutoCorrectCounter(){
		return dictionary.size();
	}

	/**
	 * @param synonymAndMeanings			The line representing all the synonyms of a word along with their part of speech
	 * @param duplicatesDiscriminator	Function called to ask the user what to do if duplicates are found (return <code>true</code> to force
	 *												insertion)
	 * @return The duplication result
	 */
	public DuplicationResult insertMeanings(final String synonymAndMeanings, final Supplier<Boolean> duplicatesDiscriminator){
		final String[] partOfSpeechAndMeanings = StringUtils.split(synonymAndMeanings, ThesaurusEntry.POS_AND_MEANS, 2);
		if(partOfSpeechAndMeanings.length != 2)
			throw new IllegalArgumentException(WRONG_FORMAT.format(new Object[]{synonymAndMeanings}));

		final String partOfSpeech = StringUtils.strip(partOfSpeechAndMeanings[0]);
		final int prefix = (partOfSpeech.startsWith(PART_OF_SPEECH_START)? 1: 0);
		final int suffix = (partOfSpeech.endsWith(PART_OF_SPEECH_END)? 1: 0);
		final String[] partOfSpeeches = partOfSpeech.substring(prefix, partOfSpeech.length() - suffix)
			.split("\\s*,\\s*");

		final List<String> meanings = Arrays.stream(StringUtils.split(partOfSpeechAndMeanings[1], ThesaurusEntry.MEANS))
			.map(String::trim)
			.filter(StringUtils::isNotBlank)
			.distinct()
			.collect(Collectors.toList());
		if(meanings.size() < 2)
			throw new IllegalArgumentException(NOT_ENOUGH_MEANINGS.format(new Object[]{synonymAndMeanings}));

		boolean forceInsertion = false;
		final List<ThesaurusEntry> duplicates = extractDuplicates(partOfSpeeches, meanings);
		if(!duplicates.isEmpty()){
			forceInsertion = duplicatesDiscriminator.get();
			if(!forceInsertion)
				throw new IllegalArgumentException(DUPLICATE_DETECTED.format(new Object[]{duplicates.stream().map(ThesaurusEntry::getSynonym).collect(Collectors.joining(", "))}));
		}

		if(duplicates.isEmpty() || forceInsertion){
			try{
				storeMemento();
			}
			catch(final IOException e){
				LOGGER.warn("Error while storing a memento", e);
			}

			dictionary.add(partOfSpeeches, meanings);
		}

		return new DuplicationResult(duplicates, forceInsertion);
	}

	/** Find if there is a duplicate with the same part of speech */
	private List<ThesaurusEntry> extractDuplicates(final String[] partOfSpeeches, final List<String> meanings){
		final List<ThesaurusEntry> duplicates = new ArrayList<>();
		final List<ThesaurusEntry> synonyms = dictionary.getSynonyms();
		for(final String meaning : meanings){
			final String mean = PatternHelper.replaceAll(meaning, ThesaurusDictionary.PATTERN_PART_OF_SPEECH, StringUtils.EMPTY);
			for(final ThesaurusEntry synonym : synonyms)
				if(synonym.getSynonym().equals(mean) && synonym.hasSamePartOfSpeech(partOfSpeeches))
					duplicates.add(synonym);
		}
		return duplicates;
	}

	/** Find if there is a duplicate with the same part of speech and same meanings */
	public boolean isAlreadyContained(final List<String> partOfSpeeches, final List<String> meanings){
		final List<ThesaurusEntry> synonyms = dictionary.getSynonyms();
		return synonyms.stream()
			.anyMatch(synonym -> synonym.contains(partOfSpeeches, meanings));
	}

	public void setCorrection(final int index, final String text){
		dictionary.setMeanings(index, text);
	}

	public void deleteMeanings(final int[] selectedRowIDs){
		final int count = selectedRowIDs.length;
		for(int i = 0; i < count; i ++)
			dictionary.remove(selectedRowIDs[i] - i);
	}

	public void save(final File acoFile) throws IOException{
		//sort the synonyms
		dictionary.sort();

		//save index and data files
		final Charset charset = StandardCharsets.UTF_8;
		try(final BufferedWriter writer = Files.newBufferedWriter(acoFile.toPath(), charset)){
			//save charset
			writer.write(charset.name());
			writer.write(StringUtils.LF);
			//save data
			int idx = charset.name().length() + 1;
			final int doubleLineTerminatorLength = StringUtils.LF.length() * 2;
			final List<ThesaurusEntry> synonyms = dictionary.getSynonyms();
			for(ThesaurusEntry synonym : synonyms){
				synonym.saveToIndex(indexWriter, idx);

				int meaningsLength = synonym.saveToData(writer, charset);

				idx += synonym.getSynonym().getBytes(charset).length + meaningsLength + doubleLineTerminatorLength;
			}
		}
	}

	public void clear(){
		dictionary.clear();
	}

}
