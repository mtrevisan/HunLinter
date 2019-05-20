package unit731.hunspeller.parsers.thesaurus.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.EOFException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
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

	public List<MeaningEntry> getMeanings(){
		return meanings;
	}

	public void setMeanings(final List<MeaningEntry> meanings){
		this.meanings = meanings;
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
