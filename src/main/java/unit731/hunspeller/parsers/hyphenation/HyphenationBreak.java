package unit731.hunspeller.parsers.hyphenation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;


@AllArgsConstructor
public class HyphenationBreak{

	@NonNull
	private final int[] indexes;
	@Getter
	@NonNull
	private final String[] rules;
	@NonNull
	private final String[] augmentedPatternData;


	public boolean isBreakpoint(int index){
		return (indexes[index] % 2 != 0);
	}

	public String getAugmentedPatternData(int index){
		return augmentedPatternData[index];
	}

}
