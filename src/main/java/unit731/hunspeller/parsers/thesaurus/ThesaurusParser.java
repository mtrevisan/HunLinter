package unit731.hunspeller.parsers.thesaurus;

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
import java.util.ListIterator;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.interfaces.Undoable;
import unit731.hunspeller.parsers.workers.exceptions.HunspellException;
import unit731.hunspeller.services.FileHelper;
import unit731.hunspeller.services.PatternHelper;


/**
 * for storing mementoes:
 * https://github.com/dnaumenko/java-diff-utils
 * https://www.adictosaltrabajo.com/2012/06/05/comparar-ficheros-java-diff-utils/
 * https://github.com/java-diff-utils/java-diff-utils/wiki/Examples
 * <a href="https://github.com/java-diff-utils/java-diff-utils">java-diff-utils</a>
 * <a href="http://blog.robertelder.org/diff-algorithm/">Myers Diff Algorithm - Code &amp; Interactive Visualization</a>
 */
public class ThesaurusParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(ThesaurusParser.class);

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

	private final ThesaurusDictionary dictionary = new ThesaurusDictionary();

	private final Undoable undoable;
	private final ThesaurusCaretaker undoCaretaker = new ThesaurusCaretaker();
	private final ThesaurusCaretaker redoCaretaker = new ThesaurusCaretaker();


	public ThesaurusParser(final Undoable undoable){
		this.undoable = undoable;
	}

	public ThesaurusDictionary getDictionary(){
		return dictionary;
	}

	/**
	 * Parse the rows out from a .dic file.
	 *
	 * @param theFile	The reference to the thesaurus file
	 * @throws IOException	If an I/O error occurs
	 */
	public void parse(final File theFile) throws IOException{
		clear();

		final Path path = theFile.toPath();
		final Charset charset = FileHelper.determineCharset(path);
		try(final LineNumberReader br = FileHelper.createReader(path, charset)){
			String line = extractLine(br);

			//line should be a charset
			try{ Charsets.toCharset(line); }
			catch(final UnsupportedCharsetException e){
				throw new HunspellException(WRONG_FILE_FORMAT.format(new Object[]{line}));
			}

			while((line = br.readLine()) != null)
				if(!line.isEmpty())
					dictionary.add(new ThesaurusEntry(line, br));
		}

		validate();
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(theParser.synonyms));
//6 035 792 B
	}

	private String extractLine(final LineNumberReader br) throws IOException{
		final String line = br.readLine();
		if(line == null)
			throw new EOFException("Unexpected EOF while reading Thesaurus file");

		return line;
	}

	private void validate(){
		final List<String> duplicatedSynonyms = dictionary.extractDuplicatedSynonyms();
		for(final String duplicatedSynonym : duplicatedSynonyms)
			LOGGER.info(Backbone.MARKER_APPLICATION, "Duplicated synonym in thesaurus: '{}'", duplicatedSynonym);
	}

	public int getSynonymsCounter(){
		return dictionary.size();
	}

	public List<ThesaurusEntry> getSynonymsDictionary(){
		return dictionary.getSynonyms();
	}

	/**
	 * @param synonymAndMeanings			The object representing all the synonyms of a word along with their part of speech
	 * @param duplicatesDiscriminator	Function called to ask the user what to do if duplicates are found
	 * 	(return <code>true</code> to force insertion)
	 * @return The duplication result
	 */
	public DuplicationResult<ThesaurusEntry> insertMeanings(final String synonymAndMeanings,
			final Supplier<Boolean> duplicatesDiscriminator){
		final String[] partOfSpeechAndMeanings = StringUtils.split(synonymAndMeanings, ThesaurusEntry.POS_AND_MEANS, 2);
		if(partOfSpeechAndMeanings.length != 2)
			throw new HunspellException(WRONG_FORMAT.format(new Object[]{synonymAndMeanings}));

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
			throw new HunspellException(NOT_ENOUGH_MEANINGS.format(new Object[]{synonymAndMeanings}));

		boolean forceInsertion = false;
		final List<ThesaurusEntry> duplicates = extractDuplicates(partOfSpeeches, meanings);
		if(!duplicates.isEmpty()){
			forceInsertion = duplicatesDiscriminator.get();
			if(!forceInsertion)
				throw new HunspellException(DUPLICATE_DETECTED.format(
					new Object[]{duplicates.stream().map(ThesaurusEntry::getSynonym).collect(Collectors.joining(", "))}));
		}

		if(duplicates.isEmpty() || forceInsertion){
			try{
//FIXME
				storeMemento();
			}
			catch(final DiffException | IOException e){
				LOGGER.warn("Error while storing a memento", e);
			}

			dictionary.add(partOfSpeeches, meanings);
		}

		return new DuplicationResult<>(duplicates, forceInsertion);
	}

	/** Find if there is a duplicate with the same part of speech */
	private List<ThesaurusEntry> extractDuplicates(final String[] partOfSpeeches, final List<String> meanings){
		final List<ThesaurusEntry> duplicates = new ArrayList<>();
		final List<ThesaurusEntry> synonyms = dictionary.getSynonyms();
		for(final String meaning : meanings){
			final String mean = PatternHelper.replaceAll(meaning, ThesaurusDictionary.PATTERN_PART_OF_SPEECH, StringUtils.EMPTY);
			synonyms.stream()
				.filter(synonym -> synonym.getSynonym().equals(mean) && synonym.hasSamePartOfSpeech(partOfSpeeches))
				.forEach(duplicates::add);
		}
		return duplicates;
	}

	/** Find if there is a duplicate with the same part of speech and same meanings */
	public boolean isAlreadyContained(final List<String> partOfSpeeches, final List<String> meanings){
		final List<ThesaurusEntry> synonyms = dictionary.getSynonyms();
		return synonyms.stream()
			.anyMatch(synonym -> synonym.contains(partOfSpeeches, meanings));
	}

	public void setMeanings(final int index, final String text) throws IOException{
		try{
//FIXME
			storeMemento();

			dictionary.setMeanings(index, text);
		}
		catch(final DiffException e){
			undoCaretaker.popMemento();

			LOGGER.warn("Error while storing a memento", e);
		}
		catch(final Exception e){
			undoCaretaker.popMemento();

			LOGGER.warn("Error while modifying the meanings", e);
		}
	}

	public void deleteMeanings(final int[] selectedRowIDs){
		final int count = selectedRowIDs.length;
		if(count > 0){
			try{
//FIXME
				storeMemento();
			}
			catch(final DiffException | IOException e){
				LOGGER.warn("Error while storing a memento", e);
			}

			IntStream.range(0, count)
				.map(i -> selectedRowIDs[i] - i)
				.forEach(dictionary::remove);
		}
	}

	public static Pair<String[], String[]> extractComponentsForFilter(String text){
		//extract part of speech if present
		final String[] pos = extractPartOfSpeechFromFilter(text);

		text = clearFilter(text);

		text = PatternHelper.clear(text, PATTERN_FILTER_EMPTY);
		text = PatternHelper.replaceAll(text, PATTERN_FILTER_OR, PIPE);
		text = PatternHelper.replaceAll(text, PATTERN_PARENTHESIS, StringUtils.EMPTY);

		//compose filter regexp
		return Pair.of(pos, StringUtils.split(text, PIPE));
	}

	private static String[] extractPartOfSpeechFromFilter(String text){
		text = StringUtils.strip(text);

		int start = 0;
		String[] pos = null;
		//remove part of speech and format the search string
		int idx = text.indexOf(':');
		if(idx < 0){
			idx = text.indexOf(")|");
			start = 1;
		}
		if(idx >= 0)
			pos = text.substring(start, idx)
				.split(", *");
		return pos;
	}

	public static Pair<String, String> prepareTextForFilter(final List<String> partOfSpeeches, List<String> meanings){
		//extract part of speech if present
		final String posFilter = (!partOfSpeeches.isEmpty()?
			"[\\(\\s](" + String.join(PIPE, partOfSpeeches) + ")[\\),]":
			".+");
		final String meaningsFilter = (!meanings.isEmpty()? "(" + String.join(PIPE, meanings) + ")": ".+");

		//compose filter regexp
		return Pair.of(posFilter, meaningsFilter);
	}

	private static String clearFilter(String text){
		text = StringUtils.strip(text);

		//remove part of speech and format the search string
		int idx = text.indexOf(':');
		if(idx >= 0){
			text = text.substring(idx + 1);
			text = StringUtils.replaceChars(text, ",", ThesaurusEntry.PIPE);
		}
		else{
			idx = text.indexOf(")|");
			if(idx >= 0)
				text = text.substring(idx + 2);
		}
		//escape special characters
		text = Matcher.quoteReplacement(text);
		//remove all \s+([^)]+)
		return PatternHelper.clear(text, PATTERN_CLEAR_SEARCH);
	}

	public void save(final File theIndexFile, final File theDataFile) throws IOException{
		//sort the synonyms
		dictionary.sort();

		//save index and data files
		final Charset charset = StandardCharsets.UTF_8;
		try(
				final BufferedWriter indexWriter = Files.newBufferedWriter(theIndexFile.toPath(), charset);
				final BufferedWriter dataWriter = Files.newBufferedWriter(theDataFile.toPath(), charset);
				){
			//save charset
			indexWriter.write(charset.name());
			indexWriter.write(StringUtils.LF);
			//save counter
			indexWriter.write(Integer.toString(dictionary.size()));
			indexWriter.write(StringUtils.LF);
			//save charset
			dataWriter.write(charset.name());
			dataWriter.write(StringUtils.LF);
			//save data
			int idx = charset.name().length() + 1;
			final int doubleLineTerminatorLength = StringUtils.LF.length() * 2;
			final List<ThesaurusEntry> synonyms = dictionary.getSynonyms();
			for(final ThesaurusEntry synonym : synonyms){
				synonym.saveToIndex(indexWriter, idx);

				int meaningsLength = synonym.saveToData(dataWriter, charset);

				idx += synonym.getSynonym().getBytes(charset).length + meaningsLength + doubleLineTerminatorLength;
			}
		}
	}

	public void clear(){
		dictionary.clear();
	}

	private void storeMemento() throws DiffException, IOException{
		undoCaretaker.pushMemento(createMemento());

		warnUndoable();
	}

	public boolean restorePreviousSnapshot() throws DiffException, IOException{
		return restoreSnapshot(undoCaretaker);
	}

	public boolean restoreNextSnapshot() throws DiffException, IOException{
		return restoreSnapshot(redoCaretaker);
	}

	private boolean restoreSnapshot(final ThesaurusCaretaker fromCaretaker) throws DiffException, IOException{
		boolean restored = false;
		if(fromCaretaker.canUndo()){
			final ThesaurusCaretaker otherCaretaker = (fromCaretaker == redoCaretaker? undoCaretaker: redoCaretaker);
			otherCaretaker.pushMemento(createMemento());

			final Patch<ThesaurusEntry> memento = fromCaretaker.popMemento();
			warnUndoable();

			restoreMemento(memento);

			restored = true;
		}
		return restored;
	}

	private void warnUndoable(){
		if(undoable != null){
			undoable.onUndoChange(canUndo());
			undoable.onRedoChange(canRedo());
		}
	}

	public boolean canUndo(){
		return undoCaretaker.canUndo();
	}

	public boolean canRedo(){
		return redoCaretaker.canUndo();
	}

	private Patch<ThesaurusEntry> createMemento() throws DiffException{
//FIXME old + new
		return DiffUtils.diff(getSynonymsDictionary(), getSynonymsDictionary());
	}

	private void restoreMemento(final Patch<ThesaurusEntry> memento){
//FIXME to check
		final List<ThesaurusEntry> synonymsDictionary = getSynonymsDictionary();
		final List<AbstractDelta<ThesaurusEntry>> deltas = memento.getDeltas();
		final ListIterator itr = deltas.listIterator(deltas.size());
		while(itr.hasPrevious()){
			final AbstractDelta<ThesaurusEntry> delta = (AbstractDelta)itr.previous();
			delta.restore(synonymsDictionary);
		}
	}

}
