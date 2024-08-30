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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	public final boolean add(final String[] partOfSpeeches, final String[] synonyms){
		final StringJoiner sj = new StringJoiner(LIST_SEPARATOR, PART_OF_SPEECH_START, PART_OF_SPEECH_END);
		final int size = (partOfSpeeches != null? partOfSpeeches.length: 0);
		for(int i = 0; i < size; i ++)
			sj.add(partOfSpeeches[i]);
		final String wholePartOfSpeeches = sj.toString();

		boolean result = false;
		for(int i = 0; i < synonyms.length; i ++){
			String currentDefinition = synonyms[i];
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

	private static SynonymsEntry extractPartOfSpeechAndSynonyms(final CharSequence partOfSpeeches, final String[] synonyms,
			final String definition){
		final StringJoiner sj = new StringJoiner(ThesaurusEntry.PIPE);
		sj.add(partOfSpeeches);
		for(int i = 0; i < synonyms.length; i ++){
			final String synonym = synonyms[i];
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
		final HashSet<String> pos = (partOfSpeeches != null? new HashSet<>(Arrays.asList(partOfSpeeches)): null);
		final Set<String> syns = new HashSet<>(Arrays.asList(synonyms));
		for(final ThesaurusEntry entry : dictionary.values())
			for(final SynonymsEntry synonymsEntry : entry.getSynonyms())
				if(pos == null || new HashSet<>(synonymsEntry.getPartOfSpeeches()).containsAll(pos)){
					final Set<String> currentSet = new HashSet<>(synonymsEntry.getSynonyms());
					currentSet.add(entry.getDefinition());
					if(syns.equals(currentSet))
						return true;
				}
		return false;
	}

	public final void deleteDefinition(final String definition){
		//collect entries to be removed
		final ThesaurusEntry entryToBeDeleted = dictionary.get(definition);
		final List<SynonymsEntry> synonymsEntries = entryToBeDeleted.getSynonyms();
		final List<Set<String>> deleteSets = new ArrayList<>(synonymsEntries.size());
		for(final SynonymsEntry entry : synonymsEntries){
			final Set<String> deleteSet = new HashSet<>(entry.getSynonyms());
			deleteSet.add(definition);
			deleteSets.add(deleteSet);
		}
		//remove all entries that have all the elements in one of `deleteSets`
		dictionary.values()
			.forEach(entry -> {
				final Iterator<SynonymsEntry> itr = entry.getSynonyms().iterator();
				while(itr.hasNext()){
					final SynonymsEntry synonymsEntry = itr.next();

					final Set<String> currentSet = new HashSet<>(synonymsEntry.getSynonyms());
					currentSet.add(entry.getDefinition());
					for(final Set<String> deleteSet : deleteSets){
						if(currentSet.equals(deleteSet))
							itr.remove();
					}
				}
			});
		//remove all empty records
		dictionary.entrySet()
			.removeIf(entry -> entry.getValue().getSynonyms().isEmpty());
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
	public final List<ThesaurusEntry> extractDuplicates(final String[] partOfSpeeches, final String[] synonyms){
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
