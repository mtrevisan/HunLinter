package unit731.hunspeller.resources;

import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public class HyphenationPattern{

	private final int[] indices;
	private final String[] augmentedPatternData;

}
