package unit731.hunspeller.parsers.thesaurus.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
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
import unit731.hunspeller.services.FileHelper;


public class ThesaurusEntry implements Comparable<ThesaurusEntry>{

	public static final String PIPE = "|";
	public static final String POS_MEANS = PIPE + ":";
	public static final String MEANS = PIPE + ",";


	@JsonProperty
	private String synonym;
	@JsonProperty
	private List<MeaningEntry> meanings;


	private ThesaurusEntry(){}

	public ThesaurusEntry(String synonym, List<MeaningEntry> meanings){
		Objects.requireNonNull(synonym);
		Objects.requireNonNull(meanings);

		this.synonym = synonym;
		this.meanings = meanings;
	}

	public String getSynonym(){
		return synonym;
	}

	public List<MeaningEntry> getMeanings(){
		return meanings;
	}

	public void setMeanings(List<MeaningEntry> meanings){
		this.meanings = meanings;
	}

	public ThesaurusEntry(String line, LineNumberReader br) throws IOException{
		Objects.requireNonNull(line);
		Objects.requireNonNull(br);

		String[] data = StringUtils.split(line, POS_MEANS);

		synonym = data[0];
		int numEntries = Integer.parseInt(data[1]);
		meanings = new ArrayList<>(numEntries);
		for(int i = 0; i < numEntries; i ++){
			String meaning = br.readLine();
			if(meaning == null)
				throw new IllegalArgumentException("Unexpected EOF while reading Thesaurus file");

			//ignore any BOM marker on first line
			if(i == 0)
				meaning = FileHelper.clearBOMMarker(meaning);

			meanings.add(new MeaningEntry(meaning));
		}
	}

	@Override
	public String toString(){
		StringJoiner sj = new StringJoiner(": ");
		sj.add(synonym);
		meanings.forEach(meaning -> sj.add(StringUtils.join(meaning, ", ")));
		return sj.toString();
	}

	@Override
	public int compareTo(ThesaurusEntry other){
		return new CompareToBuilder()
			.append(synonym, other.getSynonym())
			.toComparison();
	}

	@Override
	public boolean equals(Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		ThesaurusEntry rhs = (ThesaurusEntry)obj;
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
