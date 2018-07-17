package unit731.hunspeller.parsers.hyphenation.dtos;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;


@AllArgsConstructor
public class HyphenationBreak{

	private final int[] indexes;
	@Getter
	private final String[] rules;
	private final String[] augmentedPatternData;


	public static HyphenationBreak merge(HyphenationBreak parentHyphBreak, List<HyphenationBreak> hyphBreaks, String breakCharacter){
		//TODO add parent hyph break
		int[] indexes = ArrayUtils.toPrimitive(hyphBreaks.stream()
			.map(hyph -> hyph.indexes)
			.map(ArrayUtils::toObject)
			.flatMap(Arrays::stream)
			.toArray(Integer[]::new));
		String[] rules = hyphBreaks.stream()
			.map(hyph -> hyph.rules)
			.flatMap(Arrays::stream)
			.toArray(String[]::new);
		String[] augmentedPatternData = hyphBreaks.stream()
			.map(hyph -> hyph.augmentedPatternData)
			.flatMap(Arrays::stream)
			.toArray(String[]::new);
		return new HyphenationBreak(indexes, rules, augmentedPatternData);
	}

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

	public boolean enforceNoHyphens(List<String> syllabes, Set<String> noHyphen){
		int size = syllabes.size();
		for(String nohyp : noHyphen){
			int nohypLength = nohyp.length();
			if(nohyp.charAt(0) == '^'){
				if(syllabes.get(0).startsWith(nohyp.substring(1))){
					resetBreakpoint(indexes, rules, augmentedPatternData, 0);
					resetBreakpoint(indexes, rules, augmentedPatternData, nohypLength - 1);
				}
			}
			else if(nohyp.charAt(nohypLength - 1) == '$'){
				if(syllabes.get(syllabes.size() - 1).endsWith(nohyp.substring(0, nohypLength - 1))){
					resetBreakpoint(indexes, rules, augmentedPatternData, size - nohypLength - 1);
					resetBreakpoint(indexes, rules, augmentedPatternData, size - 2);
				}
			}
			else
				for(int i = 0; i < size; i ++){
					String syllabe = syllabes.get(i);
					int idx = -1;
					while((idx = syllabe.indexOf(nohyp, idx + 1)) >= 0){
						resetBreakpoint(indexes, rules, augmentedPatternData, idx);
						resetBreakpoint(indexes, rules, augmentedPatternData, idx + nohypLength);
					}
				}
		}
		return false;
	}

	private void resetBreakpoint(int[] indexes, String[] rules, String[] augmentedPatternData, int index){
		if(index < indexes.length){
			indexes[index] = 0;
			rules[index] = null;
			augmentedPatternData[index] = null;
		}
	}

}
