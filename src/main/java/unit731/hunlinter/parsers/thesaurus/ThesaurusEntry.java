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
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunlinter.services.ParserHelper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;

import static unit731.hunlinter.services.system.LoopHelper.forEach;
import static unit731.hunlinter.services.system.LoopHelper.match;


public class ThesaurusEntry implements Comparable<ThesaurusEntry>{

	public static final String PIPE = "|";
	public static final String PART_OF_SPEECH_SEPARATOR = PIPE + ":";
	public static final String SYNONYMS_SEPARATOR = PIPE + ",";

	private static final char[] NEW_LINE = {'\n'};


	private final String definition;
	private final List<SynonymsEntry> synonyms;


	public static ThesaurusEntry createFromDefinitionAndSynonyms(final String definition, final SynonymsEntry synonyms){
		final List<SynonymsEntry> entries = new ArrayList<>(1);
		entries.add(synonyms);
		return new ThesaurusEntry(definition, entries);
	}

	private ThesaurusEntry(final String definition, final List<SynonymsEntry> synonyms){
		Objects.requireNonNull(definition);
		Objects.requireNonNull(synonyms);

		this.definition = definition.toLowerCase(Locale.ROOT);
		this.synonyms = synonyms;
	}

	public ThesaurusEntry(final String line, final Scanner scanner) throws IOException{
		Objects.requireNonNull(line);
		Objects.requireNonNull(scanner);

		//all entries should be in lowercase
		final String[] components = StringUtils.split(line.toLowerCase(Locale.ROOT), PART_OF_SPEECH_SEPARATOR);

		definition = components[0];
		final int numEntries = Integer.parseInt(components[1]);
		synonyms = new ArrayList<>(numEntries);
		for(int i = 0; i < numEntries; i ++){
			ParserHelper.assertNotEOF(scanner);

			final String definitionAndSynonyms = scanner.nextLine();
			synonyms.add(new SynonymsEntry(definitionAndSynonyms));
		}
	}

	public String getDefinition(){
		return definition;
	}

	public String joinSynonyms(final String separator){
		return StringUtils.join(synonyms, separator);
	}

	public void addSynonym(final SynonymsEntry synonymsEntry){
		synonyms.add(synonymsEntry);
	}

	public List<SynonymsEntry> getSynonyms(){
		return synonyms;
	}

	public Set<String> getSynonymsSet(){
		final Set<String> set = new HashSet<>();
		final Consumer<String> add = set::add;
		for(final SynonymsEntry synonym : synonyms)
			forEach(synonym.getSynonyms(), add);
		return set;
	}

	public int getSynonymsEntries(){
		return synonyms.size();
	}

	public boolean containsPartOfSpeechesAndSynonym(final String[] partOfSpeeches, final String synonym){
//		return synonyms.stream()
//			.filter(entry -> entry.hasSamePartOfSpeeches(partOfSpeeches))
//			.anyMatch(entry -> entry.containsSynonym(synonym));
		return (match(synonyms, entry -> entry.hasSamePartOfSpeeches(partOfSpeeches) && entry.containsSynonym(synonym)) != null);
//		for(final SynonymsEntry entry : synonyms)
//			if(entry.hasSamePartOfSpeeches(partOfSpeeches))
//				return entry.containsSynonym(synonym);
//		return false;
	}

	public boolean contains(final Collection<String> partOfSpeeches, final List<String> synonyms){
		final Collection<String> ss = new ArrayList<>(synonyms);
		return (ss.remove(definition) && match(this.synonyms, entry -> entry.contains(partOfSpeeches, ss)) != null);
	}

	public boolean intersects(final Collection<String> partOfSpeeches, final List<String> synonyms){
		final Collection<String> ss = new ArrayList<>(synonyms);
		return (ss.remove(definition) && match(this.synonyms, entry -> entry.containsPartOfSpeech(partOfSpeeches)) != null
			|| match(this.synonyms, entry -> entry.intersects(partOfSpeeches, ss)) != null);
	}

	public void saveToIndex(final BufferedWriter writer, final int idx) throws IOException{
		writer.write(definition);
		writer.write(PIPE);
		writer.write(Integer.toString(idx));
		writer.write(NEW_LINE);
	}

	public int saveToData(final BufferedWriter dataWriter, final Charset charset) throws IOException{
		final int synonymsEntries = getSynonymsEntries();
		saveToIndex(dataWriter, synonymsEntries);
		int synonymsLength = 1;
		for(final SynonymsEntry synonym : synonyms){
			final String s = synonym.toString();
			dataWriter.write(s);
			dataWriter.write(NEW_LINE);

			synonymsLength += s.getBytes(charset).length;
		}
		return synonymsLength + NEW_LINE.length * synonymsEntries;
	}

	@Override
	public String toString(){
		final StringJoiner sj = new StringJoiner("\r\n");
		forEach(synonyms, synonym -> sj.add(definition + ": " + String.join(", ", synonym.toString())));
		return sj.toString();
	}

	public String toLine(final int definitionIndex){
		return synonyms.get(definitionIndex)
			.toLine(definition);
	}

	@Override
	public int compareTo(final ThesaurusEntry other){
		return new CompareToBuilder()
			.append(definition, other.definition)
			.append(synonyms, other.synonyms)
			.toComparison();
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final ThesaurusEntry rhs = (ThesaurusEntry)obj;
		return new EqualsBuilder()
			.append(definition, rhs.definition)
			.append(synonyms, rhs.synonyms)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(definition)
			.append(synonyms)
			.toHashCode();
	}

}
