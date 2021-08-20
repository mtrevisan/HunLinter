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
import io.github.mtrevisan.hunlinter.services.system.LoopHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;


public class SynonymsEntry implements Comparable<SynonymsEntry>{

	private static final String COLUMN = ":";
	private static final String COMMA = ",";

	private static final MessageFormat WRONG_FORMAT = new MessageFormat("Wrong format for thesaurus entry: `{0}`");
	private static final MessageFormat POS_NOT_IN_PARENTHESIS = new MessageFormat("Part-of-speech is not in parenthesis: `{0}`");
	private static final MessageFormat NOT_ENOUGH_SYNONYMS = new MessageFormat("Not enough synonyms are supplied (at least one should be present): `{0}`");


	private final String[] partOfSpeeches;
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
			final char lastChart = partOfSpeech.charAt(partOfSpeech.length() - 1);
			if((firstChar == '(' || firstChar == '[') ^ (lastChart == ')' || lastChart == ']'))
				throw new LinterException(POS_NOT_IN_PARENTHESIS.format(new Object[]{partOfSpeechAndSynonyms}));

			String pos = StringUtils.removeEnd(StringUtils.removeStart(partOfSpeech, "("), ")");
			pos = StringUtils.removeEnd(StringUtils.removeStart(pos, "["), "]");
			partOfSpeeches = StringUtils.split(pos, ',');
			for(int i = 0; i < partOfSpeeches.length; i ++)
				partOfSpeeches[i] = partOfSpeeches[i].trim();

			final String[] synonyms = StringUtils.split(components[1], ThesaurusEntry.SYNONYMS_SEPARATOR);
			final Collection<String> uniqueValues = new HashSet<>(synonyms.length);
			this.synonyms = new ArrayList<>(synonyms.length);
			for(final String synonym : synonyms){
				final String trim = synonym.trim();
				if(StringUtils.isNotBlank(trim) && uniqueValues.add(trim))
					this.synonyms.add(trim);
			}
			if(this.synonyms.isEmpty())
				throw new LinterException(NOT_ENOUGH_SYNONYMS.format(new Object[]{partOfSpeechAndSynonyms}));
		}
		else
			throw new LinterException(WRONG_FORMAT.format(new Object[]{partOfSpeechAndSynonyms}));
	}

	public SynonymsEntry merge(final CharSequence definition, final SynonymsEntry entry){
		final SynonymsEntry newEntry = new SynonymsEntry(toString());

		//remove intersection
		newEntry.synonyms.removeIf(entry::containsSynonym);

		//add remaining synonyms
		newEntry.synonyms.addAll(new SynonymsEntry(entry.toLine(definition)).synonyms);

		return newEntry;
	}

	public String[] getPartOfSpeeches(){
		return partOfSpeeches;
	}

	public boolean hasSamePartOfSpeeches(final String[] partOfSpeeches){
		return SetHelper.setOf(this.partOfSpeeches).equals(new HashSet<>(Arrays.asList(partOfSpeeches)));
	}

	public List<String> getSynonyms(){
		return synonyms;
	}

	public boolean containsPartOfSpeech(final Collection<String> partOfSpeeches){
		return !Collections.disjoint(Arrays.asList(this.partOfSpeeches), partOfSpeeches);
	}

	public boolean containsSynonym(final String synonym){
		return (LoopHelper.match(synonyms, s -> ThesaurusDictionary.removeSynonymUse(s).equals(synonym)) != null);
	}

	public boolean contains(final Collection<String> partOfSpeeches, final Collection<String> synonyms){
		return ((partOfSpeeches == null || Arrays.asList(this.partOfSpeeches).containsAll(partOfSpeeches))
			&& this.synonyms.containsAll(synonyms));
	}

	public boolean intersects(final Collection<String> partOfSpeeches, final Collection<String> synonyms){
		return ((partOfSpeeches == null || !Collections.disjoint(Arrays.asList(this.partOfSpeeches), partOfSpeeches))
			&& !Collections.disjoint(this.synonyms, synonyms));
	}

	@Override
	public String toString(){
		return (new StringJoiner(COLUMN))
			.add(String.join(COMMA, partOfSpeeches))
			.add(StringUtils.join(synonyms, COMMA))
			.toString();
	}

	public String toLine(final CharSequence definition){
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
	public int compareTo(final SynonymsEntry other){
		return new CompareToBuilder()
			.append(partOfSpeeches, other.partOfSpeeches)
			.append(synonyms, other.synonyms)
			.toComparison();
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final SynonymsEntry rhs = (SynonymsEntry)obj;
		return new EqualsBuilder()
			.append(partOfSpeeches, rhs.partOfSpeeches)
			.append(synonyms, rhs.synonyms)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(partOfSpeeches)
			.append(synonyms)
			.toHashCode();
	}

}
