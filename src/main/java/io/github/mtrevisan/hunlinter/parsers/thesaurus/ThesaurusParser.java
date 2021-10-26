/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.parsers.thesaurus;

import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.languages.Orthography;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import io.github.mtrevisan.hunlinter.services.text.StringHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Predicate;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(ThesaurusParser.class);


	private static final String WRONG_FORMAT = "Wrong format, it must be one of '(<pos1, pos2, …>)|synonym1|synonym2|…' or 'pos1, pos2, …:synonym1,synonym2,…': `{}`";
	private static final String NOT_ENOUGH_SYNONYMS = "Not enough synonyms are supplied (at least one should be present): `{}`";

	private static final String PIPE = "|";

	private static final String PART_OF_SPEECH_START = "([";
	private static final String PART_OF_SPEECH_END = ")]";

	private static final Pattern PART_OF_SPEECH_SPLITTER = RegexHelper.pattern("\\s*,\\s*");
	private static final Pattern FILTER_SPLITTER = RegexHelper.pattern(", *");

	private static final Pattern PATTERN_PARENTHESIS = RegexHelper.pattern("\\([^)]+\\)");

	private static final Pattern PATTERN_CLEAR_SEARCH = RegexHelper.pattern("\\s+\\([^)]+\\)");
	private static final Pattern PATTERN_FILTER_EMPTY = RegexHelper.pattern("^\\(.+?\\)((?<!\\\\)\\|)?|^(?<!\\\\)\\||(?<!\\\\)\\|$|\\/.*$");
	private static final Pattern PATTERN_FILTER_OR = RegexHelper.pattern("(,|\\|)+");

	private static final char[] NEW_LINE = {'\n'};

	private final ThesaurusDictionary dictionary;
	private final Orthography orthography;
	private Charset charset;


	public ThesaurusParser(final String language){
		dictionary = new ThesaurusDictionary(language);
		orthography = BaseBuilder.getOrthography(language);
	}

	public final ThesaurusDictionary getDictionary(){
		return dictionary;
	}

	/**
	 * Parse the rows out from a .dic file.
	 *
	 * @param theFile	The reference to the thesaurus file
	 * @throws IOException	If an I/O error occurs
	 */
	@SuppressWarnings("OverlyBroadThrowsClause")
	public final void parse(final File theFile) throws IOException{
		clear();

		final Path thePath = theFile.toPath();
		charset = FileHelper.determineCharset(thePath, 20);
		LOGGER.info(ParserManager.MARKER_APPLICATION, "The charset of the thesaurus file is {}", charset.name());
		final Map<String, ThesaurusEntry> entries = new HashMap<>(0);
		try(final Scanner scanner = FileHelper.createScanner(thePath, charset)){
			//skip charset
			scanner.nextLine();

			while(scanner.hasNextLine()){
				final String line = scanner.nextLine();
				if(line.isEmpty())
					continue;

				final ThesaurusEntry entry = new ThesaurusEntry(line, scanner);
				if(entries.put(entry.getDefinition(), entry) != null)
					throw new IllegalArgumentException("Duplicated synonym in thesaurus: " + line);
			}
		}
		catch(final Exception e){
			throw new LinterException(e, e.getMessage());
		}
		dictionary.addAll(entries);
	}

	public final int getSynonymsCount(){
		return dictionary.size();
	}

	public final List<ThesaurusEntry> getSynonymsDictionary(){
		return dictionary.getSynonymsDictionary();
	}

	/**
	 * @param partOfSpeechAndSynonyms	The object representing all the synonyms of a word along with their Part-of-Speech
	 * @param duplicatesDiscriminator	Function called to ask the user what to do if duplicates are found
	 * 	(return {@code true} to force insertion)
	 * @return The duplication result
	 */
	public final DuplicationResult<ThesaurusEntry> insertSynonyms(final String partOfSpeechAndSynonyms,
			final Predicate<String> duplicatesDiscriminator){
		final String[] posAndSyns = StringUtils.split(partOfSpeechAndSynonyms, ThesaurusEntry.PART_OF_SPEECH_SEPARATOR, 2);
		if(posAndSyns.length != 2)
			throw new LinterException(WRONG_FORMAT, partOfSpeechAndSynonyms);

		final int prefix = (StringUtils.startsWithAny(posAndSyns[0], PART_OF_SPEECH_START)? 1: 0);
		final int suffix = (StringUtils.endsWithAny(posAndSyns[0], PART_OF_SPEECH_END)? 1: 0);
		final String[] partOfSpeeches = StringUtils.split(posAndSyns[0].substring(prefix, posAndSyns[0].length() - suffix), ',');
		for(int i = 0; i < partOfSpeeches.length; i ++)
			partOfSpeeches[i] = partOfSpeeches[i].trim();

		final String[] pas = StringUtils.split(posAndSyns[1], ThesaurusEntry.SYNONYMS_SEPARATOR);
		final List<String> list = new ArrayList<>(pas.length);
		for(int i = 0; i < pas.length; i ++){
			final String trim = orthography.correctOrthography(pas[i].trim());
			if(StringUtils.isNotBlank(trim) && !list.contains(trim))
				list.add(trim);
		}
		if(list.size() < 2)
			throw new LinterException(NOT_ENOUGH_SYNONYMS, partOfSpeechAndSynonyms);

		final List<ThesaurusEntry> duplicates = extractDuplicates(partOfSpeeches, list);
		boolean forceInsertion = duplicates.isEmpty();
		if(!forceInsertion){
			final String duplicatesMessage = duplicates.stream()
				.map(ThesaurusEntry::getDefinition)
				.collect(StringHelper.limitingJoin(", ", 5, "…"));
			forceInsertion = duplicatesDiscriminator.test(duplicatesMessage);
		}

		if(forceInsertion)
			dictionary.add(partOfSpeeches, list);

		return new DuplicationResult<>(duplicates, forceInsertion);
	}

	/** Find if there is a duplicate with the same Part-of-Speech. */
	private List<ThesaurusEntry> extractDuplicates(final String[] partOfSpeeches, final List<String> synonyms){
		return dictionary.extractDuplicates(partOfSpeeches, synonyms);
	}

	/** Find if there is a duplicate with the same definition and same Part-of-Speech (and also a synonym). */
	public final boolean contains(final String definition, final List<String> partOfSpeeches, final String synonym){
		return dictionary.contains(definition, partOfSpeeches, synonym);
	}

	/** Find if there is a duplicate with the same Part-of-Speech and same synonyms. */
	public final boolean contains(final String[] partOfSpeeches, final String[] synonyms){
		return dictionary.contains(partOfSpeeches, synonyms);
	}

	public final void deleteDefinitionAndSynonyms(final String definition, final String selectedSynonyms){
		dictionary.deleteDefinition(definition, selectedSynonyms);
	}

	public static Pair<String[], String[]> extractComponentsForFilter(String text, final Orthography orthography){
		text = text.toLowerCase(Locale.ROOT);

		//extract Part-of-Speech if present
		final String[] pos = extractPartOfSpeechFromFilter(text);

		text = clearFilter(text);

		text = RegexHelper.clear(text, PATTERN_FILTER_EMPTY);
		text = RegexHelper.replaceAll(text, PATTERN_FILTER_OR, PIPE);
		text = RegexHelper.replaceAll(text, PATTERN_PARENTHESIS, StringUtils.EMPTY);

		final String[] pas = StringUtils.split(text, PIPE);
		for(int i = 0; i < pas.length; i ++)
			pas[i] = orthography.correctOrthography(pas[i].trim());

		//compose filter regexp
		return Pair.of(pos, pas);
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
		if(idx >= 0){
			pos = StringUtils.split(text.substring(start, idx), ',');
			for(int i = 0; i < pos.length; i ++)
				pos[i] = pos[i].trim();
		}
		return pos;
	}

	public static Pair<String, String> prepareTextForFilter(final String[] partOfSpeeches, final String[] synonyms){
		//extract Part-of-Speech if present
		final String posFilter = (partOfSpeeches != null && partOfSpeeches.length > 0
			? "[\\(\\s](" + StringUtils.join(partOfSpeeches, PIPE) + ")[\\),]"
			: ".+");
		String[] quotedSynonyms = null;
		if(synonyms != null && synonyms.length > 0){
			quotedSynonyms = new String[synonyms.length];
			for(int i = 0; i < synonyms.length; i ++)
				quotedSynonyms[i] = Pattern.quote(synonyms[i]);
		}
		final String synonymsFilter = (quotedSynonyms != null? "(" + StringUtils.join(quotedSynonyms, PIPE) + ")" : ".+");

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
		text = RegexHelper.quoteReplacement(text);
		//remove all \s+([^)]+)
		return RegexHelper.clear(text, PATTERN_CLEAR_SEARCH);
	}

	public final void clear(){
		dictionary.clear();
	}

	public final void save(final File theIndexFile, final File theDataFile) throws IOException{
		//save index and data files
		final Path thePath = theDataFile.toPath();
		try(
				final BufferedWriter indexWriter = Files.newBufferedWriter(theIndexFile.toPath(), charset);
				final BufferedWriter dataWriter = Files.newBufferedWriter(thePath, charset)){
			//save charset
			final String hunspellCharsetName = FileHelper.getHunspellCharsetName(charset);
			indexWriter.write(hunspellCharsetName);
			indexWriter.write(NEW_LINE);
			//save counter
			indexWriter.write(Integer.toString(dictionary.size()));
			indexWriter.write(NEW_LINE);
			//save charset
			dataWriter.write(hunspellCharsetName);
			dataWriter.write(NEW_LINE);
			//save data
			int idx = hunspellCharsetName.length() + 1;
			final List<ThesaurusEntry> synonyms = dictionary.getSortedSynonyms();
			for(int i = 0; i < synonyms.size(); i ++){
				final ThesaurusEntry synonym = synonyms.get(i);
				synonym.saveToIndex(indexWriter, idx);

				final int synonymsLength = synonym.saveToData(dataWriter, charset);

				idx += synonym.getDefinition().getBytes(charset).length + synonymsLength + 2;
			}
		}
	}

}
