package unit731.hunspeller.parsers.thesaurus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunspeller.parsers.workers.exceptions.HunspellException;


public class MeaningEntry implements Comparable<MeaningEntry>{

	private static final MessageFormat POS_NOT_IN_PARENTHESIS = new MessageFormat("Part of speech is not in parenthesis: ''{0}''");
	private static final MessageFormat NOT_ENOUGH_MEANINGS = new MessageFormat("Not enough meanings are supplied (at least one should be present): ''{0}''");
	private static final MessageFormat AIOOB_EXCEPTION = new MessageFormat("{0} with input ''{1}''");


	@JsonProperty
	private String[] partOfSpeeches;
	@JsonProperty
	private List<String> meanings;


	@JsonCreator
	public MeaningEntry(@JsonProperty("partOfSpeeches") final String[] partOfSpeeches, @JsonProperty("meanings") final List<String> meanings){
		Objects.requireNonNull(partOfSpeeches);
		Objects.requireNonNull(meanings);

		this.partOfSpeeches = partOfSpeeches;
		this.meanings = meanings;
	}

	public MeaningEntry(final String partOfSpeechAndMeanings){
		Objects.requireNonNull(partOfSpeechAndMeanings);

		try{
			final String[] components = StringUtils.split(partOfSpeechAndMeanings, ThesaurusEntry.POS_AND_MEANS, 2);

			final String partOfSpeech = StringUtils.strip(components[0]);
			if(partOfSpeech.charAt(0) != '(' || partOfSpeech.charAt(partOfSpeech.length() - 1) != ')')
				throw new HunspellException(POS_NOT_IN_PARENTHESIS.format(new Object[]{partOfSpeechAndMeanings}));

			partOfSpeeches = partOfSpeech.substring(1, partOfSpeech.length() - 1)
				.split(",\\s*");
			meanings = Arrays.stream(StringUtils.split(components[1], ThesaurusEntry.POS_AND_MEANS))
				.map(String::trim)
				.filter(StringUtils::isNotBlank)
				.distinct()
				.collect(Collectors.toList());
			if(meanings.size() < 1)
				throw new HunspellException(NOT_ENOUGH_MEANINGS.format(new Object[]{partOfSpeechAndMeanings}));
		}
		catch(final ArrayIndexOutOfBoundsException e){
			throw new HunspellException(AIOOB_EXCEPTION.format(new Object[]{e.getMessage(), partOfSpeechAndMeanings}));
		}
	}

	public String[] getPartOfSpeeches(){
		return partOfSpeeches;
	}

	public boolean containsAllMeanings(final List<String> partOfSpeeches, final List<String> meanings){
		return (Arrays.asList(this.partOfSpeeches).containsAll(partOfSpeeches) && this.meanings.containsAll(meanings));
	}

	@Override
	public String toString(){
		return (new StringJoiner(ThesaurusEntry.PIPE))
			.add(Arrays.stream(partOfSpeeches).collect(Collectors.joining(", ", "(", ")")))
			.add(StringUtils.join(meanings, ThesaurusEntry.PIPE))
			.toString();
	}

	@Override
	public int compareTo(final MeaningEntry other){
		return new CompareToBuilder()
			.append(partOfSpeeches, other.partOfSpeeches)
			.append(meanings, other.meanings)
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
			.append(partOfSpeeches, rhs.partOfSpeeches)
			.append(meanings, rhs.meanings)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(partOfSpeeches)
			.append(meanings)
			.toHashCode();
	}

}
