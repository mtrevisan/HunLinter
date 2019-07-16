package unit731.hunspeller.parsers.thesaurus;

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
	private String partOfSpeech;
	@JsonProperty
	private List<String> meanings;


	private MeaningEntry(){}

	public MeaningEntry(final String partOfSpeech, final List<String> meanings){
		Objects.requireNonNull(partOfSpeech);
		Objects.requireNonNull(meanings);

		this.partOfSpeech = partOfSpeech;
		this.meanings = meanings;
	}

	public MeaningEntry(final String partOfSpeechAndMeanings){
		Objects.requireNonNull(partOfSpeechAndMeanings);

		try{
			final String[] components = StringUtils.split(partOfSpeechAndMeanings, ThesaurusEntry.POS_AND_MEANS, 2);

			partOfSpeech = StringUtils.strip(components[0]);
			if(partOfSpeech.charAt(0) != '(' || partOfSpeech.charAt(partOfSpeech.length() - 1) != ')')
				throw new IllegalArgumentException("Part of speech is not in parenthesis: " + partOfSpeechAndMeanings);

			meanings = Arrays.stream(StringUtils.split(components[1], ThesaurusEntry.POS_AND_MEANS))
				.map(String::trim)
				.filter(StringUtils::isNotBlank)
				.distinct()
				.collect(Collectors.toList());
			if(meanings.size() < 1)
				throw new IllegalArgumentException("Not enough meanings are supplied (at least one should be present): " + partOfSpeechAndMeanings);
		}
		catch(final ArrayIndexOutOfBoundsException e){
			throw new IllegalArgumentException(e.getMessage() + " with input \"" + partOfSpeechAndMeanings + "\"");
		}
	}

	public String getPartOfSpeech(){
		return partOfSpeech;
	}

	@Override
	public String toString(){
		return (new StringJoiner(ThesaurusEntry.PIPE))
			.add(partOfSpeech)
			.add(StringUtils.join(meanings, ThesaurusEntry.PIPE))
			.toString();
	}

	@Override
	public int compareTo(final MeaningEntry other){
		return new CompareToBuilder()
			.append(partOfSpeech, other.partOfSpeech)
			.toComparison();
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final MeaningEntry rhs = (MeaningEntry)obj;
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
