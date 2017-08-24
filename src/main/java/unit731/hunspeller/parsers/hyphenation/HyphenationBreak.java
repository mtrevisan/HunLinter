package unit731.hunspeller.parsers.hyphenation;

import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public class HyphenationBreak{

	private final int[] indexes;
	private final String[] augmentedPatternData;

}
