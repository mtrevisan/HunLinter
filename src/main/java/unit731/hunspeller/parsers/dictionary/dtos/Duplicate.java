package unit731.hunspeller.parsers.dictionary.dtos;

import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;


@AllArgsConstructor
@EqualsAndHashCode(of = "lineIndex")
@Getter
public class Duplicate{

	@NonNull
	private final RuleProductionEntry production;
	@NonNull
	private final DictionaryEntry dictionaryWord;
	private final int lineIndex;

}
