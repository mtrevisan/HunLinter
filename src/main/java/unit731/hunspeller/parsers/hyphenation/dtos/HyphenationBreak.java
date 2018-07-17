package unit731.hunspeller.parsers.hyphenation.dtos;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.ArrayUtils;


@AllArgsConstructor
public class HyphenationBreak{

	@NonNull
	private final int[] indexes;
	@NonNull
	@Getter
	private final String[] rules;


	public static HyphenationBreak merge(HyphenationBreak parentHyphBreak, List<HyphenationBreak> hyphBreaks, String breakCharacter){
		int[] indexes = ArrayUtils.toPrimitive(hyphBreaks.stream()
			.map(hyph -> hyph.indexes)
			.map(ArrayUtils::toObject)
			.flatMap(Arrays::stream)
			.toArray(Integer[]::new));
		String[] rules = hyphBreaks.stream()
			.map(hyph -> hyph.rules)
			.flatMap(Arrays::stream)
			.toArray(String[]::new);

		//TODO
		//add parent hyph break
		int accumulator = 0;
		int size = hyphBreaks.size();
		for(int i = 0; i < size; i ++){
			HyphenationBreak hyphBreak = hyphBreaks.get(i);
			String[] parentRules = parentHyphBreak.getRules();
			if(parentHyphBreak.indexes[i] > indexes[i])
				indexes[i] = parentHyphBreak.indexes[i];
			if(parentHyphBreak.rules[i] != null)
				rules[i] = parentHyphBreak.rules[i];

			accumulator += hyphBreak.indexes.length;
		}

		return new HyphenationBreak(indexes, rules);
	}

	public HyphenationBreak(int[] indexes){
		this(indexes, new String[indexes.length]);
	}

	public HyphenationBreak(int size){
		this(new int[size], new String[size]);
	}

	public boolean isBreakpoint(int index){
		return (indexes != null && indexes[index] % 2 != 0);
	}

	public boolean enforceNoHyphens(List<String> syllabes, Set<String> noHyphen){
		boolean modified = false;
		int size = syllabes.size();
		for(String nohyp : noHyphen){
			int nohypLength = nohyp.length();
			if(nohyp.charAt(0) == '^'){
				if(syllabes.get(0).startsWith(nohyp.substring(1))){
					resetBreakpoint(0);
					resetBreakpoint(nohypLength - 1);

					modified = true;
				}
			}
			else if(nohyp.charAt(nohypLength - 1) == '$'){
				if(syllabes.get(syllabes.size() - 1).endsWith(nohyp.substring(0, nohypLength - 1))){
					resetBreakpoint(size - nohypLength - 1);
					resetBreakpoint(size - 2);

					modified = true;
				}
			}
			else
				for(int i = 0; i < size; i ++){
					String syllabe = syllabes.get(i);
					int idx = -1;
					while((idx = syllabe.indexOf(nohyp, idx + 1)) >= 0){
						resetBreakpoint(idx);
						resetBreakpoint(idx + nohypLength);

						modified = true;
					}
				}
		}
		return modified;
	}

	private void resetBreakpoint(int index){
		if(index < indexes.length){
			indexes[index] = 0;
			rules[index] = null;
		}
	}

}
