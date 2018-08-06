package unit731.hunspeller.parsers.dictionary.dtos;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;


@AllArgsConstructor
@EqualsAndHashCode(of = "lineIndex")
@Getter
public class Duplicate{

	@NonNull
	private final Production production;
	@NonNull
	private final String word;
	private final int lineIndex;

}
