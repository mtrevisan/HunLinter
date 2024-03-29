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
package io.github.mtrevisan.hunlinter.parsers.thesaurus;

import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;


public class ThesaurusDictionary{

	private static final Pattern PATTERN_SYNONYM_USE = RegexHelper.pattern("\\s*\\([^)]+\\)");

	private static final String LIST_SEPARATOR = ", ";
	private static final String PART_OF_SPEECH_START = "(";
	private static final String PART_OF_SPEECH_END = ")";


	private final Map<String, ThesaurusEntry> dictionary;

	private final Comparator<String> comparator;


	public ThesaurusDictionary(final String language){
		comparator = BaseBuilder.getComparator(language);

		//sort the definitions in language-specific order
		dictionary = new HashMap<>();
	}

	public final boolean add(final ThesaurusEntry entry){
		return (dictionary.put(entry.getDefinition(), entry) == null);
	}

	public final void addAll(final Map<String, ThesaurusEntry> entries){
		dictionary.putAll(entries);
	}

	/**
	 * @param partOfSpeeches	Part-of-speech.
	 * @param synonyms	Unique list of synonyms.
	 * @return	Whether the row was added.
	 */
	public final boolean add(final String[] partOfSpeeches, final List<String> synonyms){
		final StringJoiner sj = new StringJoiner(LIST_SEPARATOR, PART_OF_SPEECH_START, PART_OF_SPEECH_END);
		final int size = (partOfSpeeches != null? partOfSpeeches.length: 0);
		for(int i = 0; i < size; i ++)
			sj.add(partOfSpeeches[i]);
		final String wholePartOfSpeeches = sj.toString();

		boolean result = false;
		for(int i = 0; i < synonyms.size(); i ++){
			String currentDefinition = synonyms.get(i);
			final SynonymsEntry synonymsEntry = extractPartOfSpeechAndSynonyms(wholePartOfSpeeches, synonyms, currentDefinition);

			currentDefinition = removeSynonymUse(currentDefinition);
			final ThesaurusEntry foundDefinition = dictionary.get(currentDefinition);
			if(foundDefinition != null)
				//add definition and synonyms if definition does exist
				foundDefinition.addSynonym(synonymsEntry);
			else{
				//add to list if definition doesn't exist
				final ThesaurusEntry entry = ThesaurusEntry.createFromDefinitionAndSynonyms(currentDefinition, synonymsEntry);
				dictionary.put(currentDefinition, entry);

				result = true;
			}
		}

		return result;
	}

	private static SynonymsEntry extractPartOfSpeechAndSynonyms(final CharSequence partOfSpeeches, final List<String> synonyms,
			final String definition){
		final StringJoiner sj = new StringJoiner(ThesaurusEntry.PIPE);
		sj.add(partOfSpeeches);
		for(int i = 0; i < synonyms.size(); i ++){
			final String synonym = synonyms.get(i);
			if(!synonym.equals(definition))
				sj.add(synonym);
		}
		return new SynonymsEntry(sj.toString());
	}

	/** Find if there is a duplicate with the same definition and same Part-of-Speech. */
	public final boolean contains(final String definition, final List<String> partOfSpeeches, final String synonym){
		final ThesaurusEntry def = dictionary.get(definition);
		return (def != null && def.containsPartOfSpeechesAndSynonym(partOfSpeeches, synonym));
	}

	/** Find if there is a duplicate with the same Part-of-Speech and same synonyms. */
	public final boolean contains(final String[] partOfSpeeches, final String[] synonyms){
		final List<String> pos = (partOfSpeeches != null? Arrays.asList(partOfSpeeches): null);
		final List<String> syns = Arrays.asList(synonyms);
		for(final ThesaurusEntry entry : dictionary.values())
			if(entry.contains(pos, syns))
				return true;
		return false;
	}

	//FIXME? remove only one entry?
	public final void deleteDefinition(final String definition, final String synonyms){
		//recover all words (definition and synonyms) from given definition
		final ThesaurusEntry entryToBeDeleted = dictionary.get(definition);
		final List<String> definitions = new ArrayList<>(entryToBeDeleted.getSynonymsSet());
		definitions.add(definition);
		for(int i = 0; i < definitions.size(); i ++){
			final int subTypeIndex = StringUtils.indexOfAny(definitions.get(i), '(');
			if(subTypeIndex >= 0)
				//NOTE: remove also de space before the open parenthesis (this assumes the subtype be the last thing of the synonym)
				definitions.set(i, definitions.get(i).substring(0, subTypeIndex - 1));
		}

		//remove all
		dictionary.entrySet()
			.removeIf(entry -> definitions.contains(entry.getKey()));

//		//recover definition and synonyms pairs (to be deleted)
//		final String[] synonymsByDefinition = StringUtils.splitByWholeSeparator(synonyms, ThesaurusTableModel.TAG_NEW_LINE);
//		final List<SynonymsEntry> entries = Arrays.stream(synonymsByDefinition)
//			.map(syns -> new SynonymsEntry(GUIHelper.removeHTMLCode(syns) + ThesaurusEntry.PIPE + definition))
//			.collect(Collectors.toList());
//
//		//delete each occurrence of the definition-synonyms pair
//		dictionary.entrySet()
//			.removeIf(entry -> {
//				final String def = entry.getKey();
//				final List<SynonymsEntry> syns = entry.getValue().getSynonyms().stream()
//					.map(s -> new SynonymsEntry(s + ThesaurusEntry.PIPE + def))
//					.collect(Collectors.toList());
//				return entries.stream()
//					.anyMatch(e -> syns.stream().anyMatch(s -> e.isSame(s)));
//			});
	}

	public final List<ThesaurusEntry> getSynonymsDictionary(){
		//sort for GUI
		return getSortedEntries(comparator);
	}

	public final List<ThesaurusEntry> getSortedSynonyms(){
		//sort for package
		return getSortedEntries(Comparator.naturalOrder());
	}

	private List<ThesaurusEntry> getSortedEntries(final Comparator<String> comparator){
		final List<ThesaurusEntry> synonyms = new ArrayList<>(dictionary.values());
		synonyms.sort((ThesaurusEntry entry1, ThesaurusEntry entry2) -> comparator.compare(entry1.getDefinition(), entry2.getDefinition()));
		return synonyms;
	}

	public final void clear(){
		dictionary.clear();
	}

	public final int size(){
		return dictionary.size();
	}

	/** Find all the entries that have Part-of-Speech and synonyms contained into the given ones. */
	public final List<ThesaurusEntry> extractDuplicates(final String[] partOfSpeeches, final List<String> synonyms){
		final List<String> pos = Arrays.asList(partOfSpeeches);
		final List<ThesaurusEntry> list = new ArrayList<>(dictionary.size());
		for(final ThesaurusEntry entry : dictionary.values())
			if(entry.intersects(pos, synonyms))
				list.add(entry);
		return list;
	}

	public static String removeSynonymUse(final CharSequence synonym){
		return RegexHelper.replaceAll(synonym, PATTERN_SYNONYM_USE, StringUtils.EMPTY);
	}

}
