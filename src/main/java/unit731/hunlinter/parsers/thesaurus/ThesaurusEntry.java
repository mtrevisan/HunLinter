package unit731.hunlinter.parsers.thesaurus;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


public class ThesaurusEntry implements Comparable<ThesaurusEntry>{

	public static final String PIPE = "|";
	public static final String PART_OF_SPEECH_AND_SYNONYMS_SEPARATOR = PIPE + ":";
	public static final String SYNONYMS_SEPARATOR = PIPE + ",";


	private final String definition;
	private final List<SynonymsEntry> synonyms;


	public static ThesaurusEntry createFromDefinitionAndSynonyms(final String definition, final SynonymsEntry synonyms){
		final List<SynonymsEntry> entries = new ArrayList<>(1);
		entries.add(synonyms);
		return new ThesaurusEntry(definition, entries);
	}

	public ThesaurusEntry(final String definition, final List<SynonymsEntry> synonyms){
		Objects.requireNonNull(definition);
		Objects.requireNonNull(synonyms);

		this.definition = definition;
		this.synonyms = synonyms;
	}

	public ThesaurusEntry(final String line, final LineNumberReader br) throws IOException{
		Objects.requireNonNull(line);
		Objects.requireNonNull(br);

		//all entries should be in lowercase
		final String[] components = StringUtils.split(line.toLowerCase(Locale.ROOT), PART_OF_SPEECH_AND_SYNONYMS_SEPARATOR);

		definition = components[0];
		final int numEntries = Integer.parseInt(components[1]);
		synonyms = new ArrayList<>(numEntries);
		for(int i = 0; i < numEntries; i ++){
			final String definitionAndSynonyms = br.readLine();
			if(definitionAndSynonyms == null)
				throw new EOFException("Unexpected EOF while reading Thesaurus file");

			synonyms.add(new SynonymsEntry(definitionAndSynonyms));
		}
	}

	public String getDefinition(){
		return definition;
	}

	public String joinSynonyms(final String separator){
		return StringUtils.join(synonyms, separator);
	}

	public void addSynonym(SynonymsEntry synonymsEntry){
		synonyms.add(synonymsEntry);
	}

	public Set<String> getSynonyms(){
		return synonyms.stream()
			.map(SynonymsEntry::getSynonyms)
			.flatMap(List::stream)
			.collect(Collectors.toSet());
	}

	public int getSynonymsEntries(){
		return synonyms.size();
	}

	public int getSynonymsCount(){
		return synonyms.stream()
			.mapToInt(s -> s.getSynonyms().size())
			.sum();
	}

	@SuppressWarnings("unchecked")
	public boolean contains(final List<String> partOfSpeeches, final List<String> synonyms){
		final List<String> ss = new ArrayList<>(synonyms);
		return (ss.remove(definition) && this.synonyms.stream().anyMatch(entry -> entry.contains(partOfSpeeches, ss)));
	}

	@SuppressWarnings("unchecked")
	public boolean intersects(final List<String> partOfSpeeches, final List<String> synonyms){
		final List<String> ss = new ArrayList<>(synonyms);
		return (ss.remove(definition) || this.synonyms.stream().anyMatch(entry -> entry.intersects(partOfSpeeches, ss)));
	}

	public void saveToIndex(BufferedWriter writer, int idx) throws IOException{
		writer.write(definition);
		writer.write(ThesaurusEntry.PIPE);
		writer.write(Integer.toString(idx));
		writer.write(StringUtils.LF);
	}

	public int saveToData(final BufferedWriter dataWriter, final Charset charset) throws IOException{
		final int synonymsEntries = getSynonymsEntries();
		saveToIndex(dataWriter, synonymsEntries);
		int synonymsLength = 1;
		for(final SynonymsEntry synonym : synonyms){
			final String s = synonym.toString();
			dataWriter.write(s);
			dataWriter.write(StringUtils.LF);

			synonymsLength += s.getBytes(charset).length;
		}
		return synonymsLength + StringUtils.LF.length() * synonymsEntries;
	}

	@Override
	public String toString(){
		return synonyms.stream()
			.map(SynonymsEntry::toString)
			.map(s -> definition + ": " + String.join(", ", s))
			.collect(Collectors.joining("\\r\\n"));
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
