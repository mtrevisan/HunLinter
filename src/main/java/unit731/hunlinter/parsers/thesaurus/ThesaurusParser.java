package unit731.hunlinter.parsers.thesaurus;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunlinter.parsers.workers.exceptions.LinterException;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.PatternHelper;
import unit731.hunlinter.services.text.StringHelper;


/**
 * for storing mementos:
 * https://github.com/dnaumenko/java-diff-utils
 * https://www.adictosaltrabajo.com/2012/06/05/comparar-ficheros-java-diff-utils/
 * https://github.com/java-diff-utils/java-diff-utils/wiki/Examples
 * <a href="https://github.com/java-diff-utils/java-diff-utils">java-diff-utils</a>
 * <a href="http://blog.robertelder.org/diff-algorithm/">Myers Diff Algorithm - Code &amp; Interactive Visualization</a>
 */
public class ThesaurusParser{

	private static final MessageFormat WRONG_FORMAT = new MessageFormat("Wrong format, it must be one of '(<pos1, pos2, …>)|synonym1|synonym2|…' or 'pos1, pos2, …:synonym1,synonym2,…': was ''{0}''");
	private static final MessageFormat NOT_ENOUGH_SYNONYMS = new MessageFormat("Not enough synonyms are supplied (at least one should be present): was ''{0}''");
	private static final MessageFormat DUPLICATE_DETECTED = new MessageFormat("Duplicate detected for ''{0}''");

	private static final String PIPE = "|";

	private static final String PART_OF_SPEECH_START = "(";
	private static final String PART_OF_SPEECH_END = ")";

	private static final Pattern PATTERN_PARENTHESIS = PatternHelper.pattern("\\([^)]+\\)");

	private static final Pattern PATTERN_CLEAR_SEARCH = PatternHelper.pattern("\\s+\\([^)]+\\)");
	private static final Pattern PATTERN_FILTER_EMPTY = PatternHelper.pattern("^\\(.+?\\)((?<!\\\\)\\|)?|^(?<!\\\\)\\||(?<!\\\\)\\|$|\\/.*$");
	private static final Pattern PATTERN_FILTER_OR = PatternHelper.pattern("(,|\\|)+");

	private final ThesaurusDictionary dictionary;


	public ThesaurusParser(final String language){
		dictionary = new ThesaurusDictionary(language);
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

			FileHelper.readCharset(line);

			while((line = br.readLine()) != null)
				if(!line.isEmpty()){
					final boolean added = dictionary.add(new ThesaurusEntry(line, br));
					if(!added)
						throw new IllegalArgumentException("Duplicated synonym in thesaurus: " + line);
				}
		}
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(theParser.synonyms));
//6 035 792 B
	}

	private String extractLine(final LineNumberReader br) throws IOException{
		final String line = br.readLine();
		if(line == null)
			throw new EOFException("Unexpected EOF while reading Thesaurus file");

		return line;
	}

	public int getSynonymsCount(){
		return dictionary.size();
	}

	public List<ThesaurusEntry> getSynonymsDictionary(){
		return dictionary.getSynonymsDictionary();
	}

	/**
	 * @param partOfSpeechAndSynonyms	The object representing all the synonyms of a word along with their part of speech
	 * @param duplicatesDiscriminator	Function called to ask the user what to do if duplicates are found
	 * 	(return <code>true</code> to force insertion)
	 * @return The duplication result
	 */
	public DuplicationResult<ThesaurusEntry> insertSynonyms(final String partOfSpeechAndSynonyms,
			final Function<String, Boolean> duplicatesDiscriminator){
		final String[] posAndSyns = StringUtils.split(partOfSpeechAndSynonyms, ThesaurusEntry.PART_OF_SPEECH_SEPARATOR, 2);
		if(posAndSyns.length != 2)
			throw new LinterException(WRONG_FORMAT.format(new Object[]{partOfSpeechAndSynonyms}));

		final String partOfSpeech = StringUtils.strip(posAndSyns[0]);
		final int prefix = (partOfSpeech.startsWith(PART_OF_SPEECH_START)? 1: 0);
		final int suffix = (partOfSpeech.endsWith(PART_OF_SPEECH_END)? 1: 0);
		final String[] partOfSpeeches = partOfSpeech.substring(prefix, partOfSpeech.length() - suffix)
			.split("\\s*,\\s*");

		final String[] synonyms = Arrays.stream(StringUtils.split(posAndSyns[1], ThesaurusEntry.SYNONYMS_SEPARATOR))
			.map(String::trim)
			.filter(StringUtils::isNotBlank)
			.distinct()
			.toArray(String[]::new);
		if(synonyms.length < 2)
			throw new LinterException(NOT_ENOUGH_SYNONYMS.format(new Object[]{partOfSpeechAndSynonyms}));

		boolean forceInsertion = false;
		final List<ThesaurusEntry> duplicates = extractDuplicates(partOfSpeeches, synonyms);
		if(!duplicates.isEmpty()){
			final String duplicatesMessage = duplicates.stream()
				.map(ThesaurusEntry::getDefinition)
				.collect(StringHelper.limitingJoin(", ", 5, "…"));
			forceInsertion = duplicatesDiscriminator.apply(duplicatesMessage);
			if(!forceInsertion)
				throw new LinterException(DUPLICATE_DETECTED.format(new Object[]{duplicatesMessage}));
		}

		if(duplicates.isEmpty() || forceInsertion)
			dictionary.add(partOfSpeeches, synonyms);

		return new DuplicationResult<>(duplicates, forceInsertion);
	}

	/* Find if there is a duplicate with the same part of speech */
	private List<ThesaurusEntry> extractDuplicates(final String[] partOfSpeeches, final String[] synonyms){
		return dictionary.extractDuplicates(partOfSpeeches, synonyms);
	}

	/* Find if there is a duplicate with the same definition and same part of speech (and also a synonym) */
	public boolean contains(final String definition, final String[] partOfSpeeches, final String synonym){
		return dictionary.contains(definition, partOfSpeeches, synonym);
	}

	/* Find if there is a duplicate with the same part of speech and same synonyms */
	public boolean contains(final String[] partOfSpeeches, final String[] synonyms){
		return dictionary.contains(partOfSpeeches, synonyms);
	}

	public void deleteDefinitionAndSynonyms(final String definition, final String selectedSynonyms){
		dictionary.deleteDefinition(definition, selectedSynonyms);
	}

	public static Pair<String[], String[]> extractComponentsForFilter(String text){
		text = text.toLowerCase(Locale.ROOT);

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

	public static Pair<String, String> prepareTextForFilter(final String[] partOfSpeeches, String[] synonyms){
		//extract part of speech if present
		final String posFilter = (partOfSpeeches != null && partOfSpeeches.length > 0?
			"[\\(\\s](" + StringUtils.join(partOfSpeeches, PIPE) + ")[\\),]":
			".+");
		final String synonymsFilter = (synonyms != null && synonyms.length > 0?
			"(" + StringUtils.join(synonyms, PIPE) + ")":
			".+");

		//compose filter regexp
		return Pair.of(posFilter, synonymsFilter);
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

	public void clear(){
		dictionary.clear();
	}

	public void save(final File theIndexFile, final File theDataFile) throws IOException{
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
			final List<ThesaurusEntry> synonyms = dictionary.getSortedSynonyms();
			for(final ThesaurusEntry synonym : synonyms){
				synonym.saveToIndex(indexWriter, idx);

				final int synonymsLength = synonym.saveToData(dataWriter, charset);

				idx += synonym.getDefinition().getBytes(charset).length + synonymsLength + doubleLineTerminatorLength;
			}
		}
	}

}
