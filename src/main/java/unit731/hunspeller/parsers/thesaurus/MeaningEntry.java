package unit731.hunspeller.parsers.thesaurus;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import unit731.hunspeller.services.PatternService;


@AllArgsConstructor
@Getter
public class MeaningEntry implements Comparable<MeaningEntry>{

	@NonNull
	private final String partOfSpeech;
	@NonNull
	private final List<String> meanings;


	public MeaningEntry(String synonymAndMeanings){
		Objects.requireNonNull(synonymAndMeanings);

		try{
			String[] partOfSpeechAndMeanings = PatternService.split(synonymAndMeanings, ThesaurusEntry.REGEX_PATTERN_ESCAPED_PIPE, 2);

			partOfSpeech = StringUtils.strip(partOfSpeechAndMeanings[0]);
			if(!partOfSpeech.startsWith("(") || !partOfSpeech.endsWith(")"))
				throw new IllegalArgumentException("Part of speech is not in parenthesis: " + synonymAndMeanings);

			this.meanings = Arrays.stream(PatternService.split(partOfSpeechAndMeanings[1], ThesaurusEntry.REGEX_PATTERN_ESCAPED_PIPE))
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

}
