package unit731.hunspeller.parsers.thesaurus;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
public class ThesaurusEntry implements Comparable<ThesaurusEntry>{

	public static final String PIPE = "|";
	public static final String POS_MEANS = PIPE + ":";
	public static final String MEANS = PIPE + ",";


	@NonNull
	@JsonProperty
	private String synonym;
	@NonNull
	@JsonProperty
	@Setter
	private List<MeaningEntry> meanings;


	public ThesaurusEntry(String line, BufferedReader br) throws IOException{
		Objects.requireNonNull(line);
		Objects.requireNonNull(br);

		String[] data = StringUtils.split(line, POS_MEANS);

		synonym = data[0];
		int numEntries = Integer.parseInt(data[1]);
		meanings = new ArrayList<>(numEntries);
		for(int i = 0; i < numEntries; i ++){
			String meaning = br.readLine();
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

}
