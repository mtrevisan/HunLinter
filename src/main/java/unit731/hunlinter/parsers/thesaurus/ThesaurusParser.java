/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package unit731.hunlinter.parsers.thesaurus;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunlinter.services.RegexHelper;
import unit731.hunlinter.services.system.FileHelper;
import unit731.hunlinter.services.text.StringHelper;
import unit731.hunlinter.workers.exceptions.LinterException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

	private static final String PIPE = "|";

	private static final String PART_OF_SPEECH_START = "(";
	private static final String PART_OF_SPEECH_END = ")";

	private static final Pattern PATTERN_PARENTHESIS = RegexHelper.pattern("\\([^)]+\\)");

	private static final Pattern PATTERN_CLEAR_SEARCH = RegexHelper.pattern("\\s+\\([^)]+\\)");
	private static final Pattern PATTERN_FILTER_EMPTY = RegexHelper.pattern("^\\(.+?\\)((?<!\\\\)\\|)?|^(?<!\\\\)\\||(?<!\\\\)\\|$|\\/.*$");
	private static final Pattern PATTERN_FILTER_OR = RegexHelper.pattern("(,|\\|)+");

	private static final char[] NEW_LINE = {'\n'};

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
		try(final Scanner scanner = FileHelper.createScanner(path, charset)){
			String line = scanner.nextLine();
			FileHelper.readCharset(line);

			while(scanner.hasNextLine()){
				line = scanner.nextLine();
				if(line.isEmpty())
					continue;

				final boolean added = dictionary.add(new ThesaurusEntry(line, scanner));
				if(!added)
					throw new IllegalArgumentException("Duplicated synonym in thesaurus: " + line);
			}
		}
	}

	public int getSynonymsCount(){
		return dictionary.size();
	}

	public List<ThesaurusEntry> getSynonymsDictionary(){
		return dictionary.getSynonymsDictionary();
	}

	/**
	 * @param partOfSpeechAndSynonyms	The object representing all the synonyms of a word along with their Part-of-Speech
	 * @param duplicatesDiscriminator	Function called to ask the user what to do if duplicates are found
	 * 	(return <code>true</code> to force insertion)
	 * @return The duplication result
	 */
	public DuplicationResult<ThesaurusEntry> insertSynonyms(final String partOfSpeechAndSynonyms,
			final Predicate<String> duplicatesDiscriminator){
		final String[] posAndSyns = StringUtils.split(partOfSpeechAndSynonyms, ThesaurusEntry.PART_OF_SPEECH_SEPARATOR, 2);
		if(posAndSyns.length != 2)
			throw new LinterException(WRONG_FORMAT.format(new Object[]{partOfSpeechAndSynonyms}));

		final String partOfSpeech = posAndSyns[0].trim();
		final int prefix = (partOfSpeech.startsWith(PART_OF_SPEECH_START)? 1: 0);
		final int suffix = (partOfSpeech.endsWith(PART_OF_SPEECH_END)? 1: 0);
		final String[] partOfSpeeches = partOfSpeech.substring(prefix, partOfSpeech.length() - suffix)
			.split("\\s*,\\s*");

		final String[] pas = StringUtils.split(posAndSyns[1], ThesaurusEntry.SYNONYMS_SEPARATOR);
		final List<String> list = new ArrayList<>(pas.length);
		for(final String s : pas){
			final String trim = s.trim();
			if(StringUtils.isNotBlank(trim) && !list.contains(trim))
				list.add(trim);
		}
		if(list.size() < 2)
			throw new LinterException(NOT_ENOUGH_SYNONYMS.format(new Object[]{partOfSpeechAndSynonyms}));

		final String[] synonyms = list.toArray(new String[0]);
		final List<ThesaurusEntry> duplicates = extractDuplicates(partOfSpeeches, synonyms);
		boolean forceInsertion = duplicates.isEmpty();
		if(!forceInsertion){
			final String duplicatesMessage = duplicates.stream()
				.map(ThesaurusEntry::getDefinition)
				.collect(StringHelper.limitingJoin(", ", 5, "…"));
			forceInsertion = duplicatesDiscriminator.test(duplicatesMessage);
		}

		if(forceInsertion)
			dictionary.add(partOfSpeeches, synonyms);

		return new DuplicationResult<>(duplicates, forceInsertion);
	}

	/* Find if there is a duplicate with the same Part-of-Speech */
	private List<ThesaurusEntry> extractDuplicates(final String[] partOfSpeeches, final String[] synonyms){
		return dictionary.extractDuplicates(partOfSpeeches, synonyms);
	}

	/* Find if there is a duplicate with the same definition and same Part-of-Speech (and also a synonym) */
	public boolean contains(final String definition, final String[] partOfSpeeches, final String synonym){
		return dictionary.contains(definition, partOfSpeeches, synonym);
	}

	/* Find if there is a duplicate with the same Part-of-Speech and same synonyms */
	public boolean contains(final String[] partOfSpeeches, final String[] synonyms){
		return dictionary.contains(partOfSpeeches, synonyms);
	}

	public void deleteDefinitionAndSynonyms(final String definition, final String selectedSynonyms){
		dictionary.deleteDefinition(definition, selectedSynonyms);
	}

	public static Pair<String[], String[]> extractComponentsForFilter(String text){
		text = text.toLowerCase(Locale.ROOT);

		//extract Part-of-Speech if present
		final String[] pos = extractPartOfSpeechFromFilter(text);

		text = clearFilter(text);

		text = RegexHelper.clear(text, PATTERN_FILTER_EMPTY);
		text = RegexHelper.replaceAll(text, PATTERN_FILTER_OR, PIPE);
		text = RegexHelper.replaceAll(text, PATTERN_PARENTHESIS, StringUtils.EMPTY);

		//compose filter regexp
		return Pair.of(pos, StringUtils.split(text, PIPE));
	}

	private static String[] extractPartOfSpeechFromFilter(String text){
		text = text.trim();

		int start = 0;
		String[] pos = null;
		//remove Part-of-Speech and format the search string
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
		//extract Part-of-Speech if present
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
		text = text.trim();

		//remove Part-of-Speech and format the search string
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
		return RegexHelper.clear(text, PATTERN_CLEAR_SEARCH);
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
			indexWriter.write(NEW_LINE);
			//save counter
			indexWriter.write(Integer.toString(dictionary.size()));
			indexWriter.write(NEW_LINE);
			//save charset
			dataWriter.write(charset.name());
			dataWriter.write(NEW_LINE);
			//save data
			int idx = charset.name().length() + 1;
			final List<ThesaurusEntry> synonyms = dictionary.getSortedSynonyms();
			for(final ThesaurusEntry synonym : synonyms){
				synonym.saveToIndex(indexWriter, idx);

				final int synonymsLength = synonym.saveToData(dataWriter, charset);

				idx += synonym.getDefinition().getBytes(charset).length + synonymsLength + 2;
			}
		}
	}

}
