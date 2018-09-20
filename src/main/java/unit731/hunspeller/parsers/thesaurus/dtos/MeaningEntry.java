package unit731.hunspeller.parsers.thesaurus.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


public class MeaningEntry implements Comparable<MeaningEntry>{

	@JsonProperty
	private final String partOfSpeech;
	@JsonProperty
	private final List<String> meanings;


	public MeaningEntry(String partOfSpeech, List<String> meanings){
		Objects.requireNonNull(partOfSpeech);
		Objects.requireNonNull(meanings);

		this.partOfSpeech = partOfSpeech;
		this.meanings = meanings;
	}

	public MeaningEntry(String synonymAndMeanings){
		Objects.requireNonNull(synonymAndMeanings);

		try{
			String[] partOfSpeechAndMeanings = StringUtils.split(synonymAndMeanings, ThesaurusEntry.POS_MEANS, 2);

			partOfSpeech = StringUtils.strip(partOfSpeechAndMeanings[0]);
			if(!partOfSpeech.startsWith("(") || !partOfSpeech.endsWith(")"))
				throw new IllegalArgumentException("Part of speech is not in parenthesis: " + synonymAndMeanings);

			this.meanings = Arrays.stream(StringUtils.split(partOfSpeechAndMeanings[1], ThesaurusEntry.POS_MEANS))
				.map(String::trim)
				.filter(StringUtils::isNotBlank)
				.distinct()
				.collect(Collectors.toList());
			if(this.meanings.size() < 1)
				throw new IllegalArgumentException("Not enough meanings are supplied (at least one should be present): " + synonymAndMeanings);
		}
		catch(ArrayIndexOutOfBoundsException e){
			throw new IllegalArgumentException(e.getMessage() + " with input \"" + synonymAndMeanings + "\"");
		}
	}

	public String getPartOfSpeech(){
		return partOfSpeech;
	}

	public List<String> getMeanings(){
		return meanings;
	}

	@Override
	public String toString(){
		return (new StringJoiner(ThesaurusEntry.PIPE))
			.add(partOfSpeech)
			.add(StringUtils.join(meanings, ThesaurusEntry.PIPE))
			.toString();
	}

	@Override
	public int compareTo(MeaningEntry other){
		return new CompareToBuilder()
			.append(partOfSpeech, other.partOfSpeech)
			.toComparison();
	}

	@Override
	public boolean equals(Object obj){
		if(obj == null)
			return false;
		if(obj == this)
			return true;
		if(obj.getClass() != getClass())
			return false;

		MeaningEntry rhs = (MeaningEntry)obj;
		return new EqualsBuilder()
			.append(partOfSpeech, rhs.partOfSpeech)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(partOfSpeech)
			.toHashCode();
	}

}
