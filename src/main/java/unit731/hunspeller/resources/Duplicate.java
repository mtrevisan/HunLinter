package unit731.hunspeller.resources;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;


@AllArgsConstructor
@EqualsAndHashCode
@Getter
public class Duplicate{

	@NonNull
	private final RuleProductionEntry production;
	@NonNull
	private final DictionaryEntry dictionaryWord;
	private final int lineIndex;

}
