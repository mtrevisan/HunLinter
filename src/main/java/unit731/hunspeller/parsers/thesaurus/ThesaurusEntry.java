package unit731.hunspeller.parsers.thesaurus;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


public class ThesaurusEntry implements Comparable<ThesaurusEntry>{

	public static final String PIPE = "|";
	public static final String POS_AND_MEANS = PIPE + ":";
	public static final String MEANS = PIPE + ",";


	@JsonProperty
	private String synonym;
	@JsonProperty
	private List<MeaningEntry> meanings;


	private ThesaurusEntry(){}

	public ThesaurusEntry(final String synonym, final List<MeaningEntry> meanings){
		Objects.requireNonNull(synonym);
		Objects.requireNonNull(meanings);

		this.synonym = synonym;
		this.meanings = meanings;
	}

	public ThesaurusEntry(final String line, final LineNumberReader br) throws IOException{
		Objects.requireNonNull(line);
		Objects.requireNonNull(br);

		final String[] data = StringUtils.split(line, POS_AND_MEANS);

		synonym = data[0];
		final int numEntries = Integer.parseInt(data[1]);
		meanings = new ArrayList<>(numEntries);
		for(int i = 0; i < numEntries; i ++){
			final String meaning = br.readLine();
			if(meaning == null)
				throw new EOFException("Unexpected EOF while reading Thesaurus file");

			meanings.add(new MeaningEntry(meaning));
		}
	}

	public String getSynonym(){
		return synonym;
	}

//	public List<MeaningEntry> getMeanings(){
//		return meanings;
//	}

	public String joinMeanings(String separator){
		return StringUtils.join(meanings, separator);

	}
	public void setMeanings(final String[] lines){
		meanings.clear();
		Arrays.stream(lines)
			.map(MeaningEntry::new)
			.forEachOrdered(meanings::add);
	}

	public void addMeaning(MeaningEntry meaningEntry){
		meanings.add(meaningEntry);
	}

	public int getMeaningsCount(){
		return meanings.size();
	}

	public long countSamePartOfSpeech(String partOfSpeech){
		return (long)meanings.stream()
			.map(MeaningEntry::getPartOfSpeech)
			.filter(pos -> pos.equals(partOfSpeech))
			.map(m -> 1)
			.reduce(0, (accumulator, m) -> accumulator + 1);
	}

	public void saveToIndex(BufferedWriter writer, int idx) throws IOException{
		writer.write(synonym);
		writer.write(ThesaurusEntry.PIPE);
		writer.write(Integer.toString(idx));
		writer.write(StringUtils.LF);
	}

	public int saveToData(BufferedWriter dataWriter, Charset charset) throws IOException{
		final int meaningsCount = getMeaningsCount();
		saveToIndex(dataWriter, meaningsCount);
		int meaningsLength = 1;
		for(final MeaningEntry meaning : meanings){
			final String m = meaning.toString();
			dataWriter.write(m);
			dataWriter.write(StringUtils.LF);

			meaningsLength += m.getBytes(charset).length;
		}
		return meaningsLength + StringUtils.LF.length() * meaningsCount;
	}

	@Override
	public String toString(){
		final StringJoiner sj = new StringJoiner(": ");
		sj.add(synonym);
		meanings.forEach(meaning -> sj.add(StringUtils.join(meaning, ", ")));
		return sj.toString();
	}

	@Override
	public int compareTo(final ThesaurusEntry other){
		return new CompareToBuilder()
			.append(synonym, other.getSynonym())
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
			.append(synonym, rhs.synonym)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(synonym)
			.toHashCode();
	}

}
