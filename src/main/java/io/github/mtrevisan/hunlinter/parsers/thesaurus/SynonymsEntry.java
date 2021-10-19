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

import io.github.mtrevisan.hunlinter.datastructures.SetHelper;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenationParser;
import io.github.mtrevisan.hunlinter.services.text.StringHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;


public class SynonymsEntry{

	private static final String COLUMN = ":";
	private static final String COMMA = ",";

	private static final String WRONG_FORMAT = "Wrong format for thesaurus entry: `{}`";
	private static final String POS_NOT_IN_PARENTHESIS = "Part-of-speech is not in parenthesis: `{}`";
	private static final String NOT_ENOUGH_SYNONYMS = "Not enough synonyms are supplied (at least one should be present): `{}`";


	private final List<String> partOfSpeeches;
	private final List<String> synonyms;


	public SynonymsEntry(String partOfSpeechAndSynonyms){
		Objects.requireNonNull(partOfSpeechAndSynonyms, "Part-of-speech and synonyms cannot be null");

		//all entries should be in lowercase
		if(partOfSpeechAndSynonyms.charAt(0) == ThesaurusEntry.PIPE.charAt(0))
			partOfSpeechAndSynonyms = HyphenationParser.MINUS_SIGN + partOfSpeechAndSynonyms;
		final String[] components = StringUtils.split(partOfSpeechAndSynonyms.toLowerCase(Locale.ROOT),
			ThesaurusEntry.PART_OF_SPEECH_SEPARATOR, 2);
		if(components.length == 2){
			final String partOfSpeech = components[0].trim();
			final char firstChar = partOfSpeech.charAt(0);
			final char lastChart = StringHelper.lastChar(partOfSpeech);
			if((firstChar == '(' || firstChar == '[') ^ (lastChart == ')' || lastChart == ']'))
				throw new LinterException(POS_NOT_IN_PARENTHESIS, partOfSpeechAndSynonyms);

			String pos = StringUtils.removeEnd(StringUtils.removeStart(partOfSpeech, "("), ")");
			pos = StringUtils.removeEnd(StringUtils.removeStart(pos, "["), "]");
			partOfSpeeches = Arrays.asList(StringUtils.split(pos, ','));
			for(int i = 0; i < partOfSpeeches.size(); i ++)
				partOfSpeeches.set(i, partOfSpeeches.get(i).trim());
			partOfSpeeches.sort(Comparator.naturalOrder());

			final String[] syns = StringUtils.split(components[1], ThesaurusEntry.SYNONYMS_SEPARATOR);
			final Collection<String> uniqueValues = new HashSet<>(syns.length);
			synonyms = new ArrayList<>(syns.length);
			for(final String synonym : syns){
				final String trim = synonym.trim();
				if(StringUtils.isNotBlank(trim) && uniqueValues.add(trim))
					synonyms.add(trim);
			}
			if(synonyms.isEmpty())
				throw new LinterException(NOT_ENOUGH_SYNONYMS, partOfSpeechAndSynonyms);
		}
		else
			throw new LinterException(WRONG_FORMAT, partOfSpeechAndSynonyms);
	}

	public final SynonymsEntry merge(final CharSequence definition, final SynonymsEntry entry){
		final SynonymsEntry newEntry = new SynonymsEntry(toString());

		//remove intersection
		newEntry.synonyms.removeIf(entry::containsSynonym);

		//add remaining synonyms
		newEntry.synonyms.addAll(new SynonymsEntry(entry.toLine(definition)).synonyms);

		return newEntry;
	}

	public final List<String> getPartOfSpeeches(){
		return partOfSpeeches;
	}

	public final boolean hasSamePartOfSpeeches(final List<String> partOfSpeeches){
		partOfSpeeches.sort(Comparator.naturalOrder());
		return this.partOfSpeeches.equals(partOfSpeeches);
	}

	public final List<String> getSynonyms(){
		return synonyms;
	}

	public final boolean containsPartOfSpeech(final Collection<String> partOfSpeeches){
		return !Collections.disjoint(this.partOfSpeeches, partOfSpeeches);
	}

	public final boolean containsSynonym(final String synonym){
		if(synonyms != null)
			for(int i = 0; i < synonyms.size(); i ++)
				if(ThesaurusDictionary.removeSynonymUse(synonyms.get(i)).equals(synonym))
					return true;
		return false;
	}

	public final boolean contains(final Collection<String> partOfSpeeches, final Collection<String> synonyms){
		return ((partOfSpeeches == null || this.partOfSpeeches.containsAll(partOfSpeeches)) && this.synonyms.containsAll(synonyms));
	}

	public final boolean intersects(final Collection<String> partOfSpeeches, final Collection<String> synonyms){
		return ((partOfSpeeches == null || !Collections.disjoint(this.partOfSpeeches, partOfSpeeches))
			&& !Collections.disjoint(this.synonyms, synonyms));
	}

	@Override
	public final String toString(){
		return (new StringJoiner(COLUMN))
			.add(String.join(COMMA, partOfSpeeches))
			.add(StringUtils.join(synonyms, COMMA))
			.toString();
	}

	public final String toLine(final CharSequence definition){
		final StringJoiner sj = new StringJoiner(", ", "(", ")");
		for(final String partOfSpeech : partOfSpeeches)
			sj.add(partOfSpeech);
		return (new StringJoiner(ThesaurusEntry.PIPE))
			.add(sj.toString())
			.add(StringUtils.join(synonyms, ThesaurusEntry.PIPE))
			.add(definition)
			.toString();
	}

	@Override
	public final boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final SynonymsEntry rhs = (SynonymsEntry)obj;
		return (partOfSpeeches.equals(rhs.partOfSpeeches)
			&& synonyms.equals(rhs.synonyms));
	}

	@Override
	public final int hashCode(){
		int result = synonyms.hashCode();
		result = 31 * result + partOfSpeeches.hashCode();
		return result;
	}

}
