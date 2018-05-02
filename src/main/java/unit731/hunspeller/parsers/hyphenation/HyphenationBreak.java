package unit731.hunspeller.parsers.hyphenation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;


@AllArgsConstructor
@Getter
public class HyphenationBreak{

	@NonNull
	private final int[] indexes;
	@NonNull
	private final String[] rules;
	@NonNull
	private final String[] augmentedPatternData;

}
