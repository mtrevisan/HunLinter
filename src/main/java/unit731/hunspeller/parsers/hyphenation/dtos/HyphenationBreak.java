package unit731.hunspeller.parsers.hyphenation.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
public class HyphenationBreak{

	private final int[] indexes;
	@Getter
	private final String[] rules;
	private final String[] augmentedPatternData;


	public HyphenationBreak(int[] indexes){
		this(indexes, new String[indexes.length], null);
	}

	public HyphenationBreak(int size){
		this(null, new String[size], null);
	}

	public boolean isBreakpoint(int index){
		return (indexes != null && indexes[index] % 2 != 0);
	}

	public String getAugmentedPatternData(int index){
		return (augmentedPatternData != null? augmentedPatternData[index]: null);
	}

}
