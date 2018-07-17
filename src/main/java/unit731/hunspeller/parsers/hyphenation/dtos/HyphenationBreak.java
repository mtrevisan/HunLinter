package unit731.hunspeller.parsers.hyphenation.dtos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;


@AllArgsConstructor
public class HyphenationBreak{

	private static final Pair<Integer, String> EMPTY_PAIR = Pair.of(0, null);


	private Map<Integer, Pair<Integer, String>> indexesAndRules = new HashMap<>();
	@Getter
	private int size;


	public static HyphenationBreak merge(HyphenationBreak parentHyphBreak, List<HyphenationBreak> hyphBreaks, String breakCharacter){
		Map<Integer, Pair<Integer, String>> indexesAndRules = new HashMap<>();
		int accumulator = 0;
		int size = hyphBreaks.size();
		for(int i = 0; i < size; i ++){
			Set<Map.Entry<Integer, Pair<Integer, String>>> entrySet = hyphBreaks.get(i).indexesAndRules.entrySet();
			for(Map.Entry<Integer, Pair<Integer, String>> entry : entrySet){
				int newKey = entry.getKey() + accumulator;
				Pair<Integer, String> oldPair = indexesAndRules.get(newKey);
				indexesAndRules.put(newKey, (oldPair == null || entry.getValue().getKey() > oldPair.getKey()? entry.getValue(): oldPair));
			}
			accumulator += hyphBreaks.get(i).getSize();
		}


		//TODO
		//add parent hyph break
//		int accumulator = 0;
//		int size = hyphBreaks.size();
//		for(int i = 0; i < size; i ++){
//			HyphenationBreak hyphBreak = hyphBreaks.get(i);
//			String[] parentRules = parentHyphBreak.getRules();
//			if(parentHyphBreak.indexes[i] > indexes[i])
//				indexes[i] = parentHyphBreak.indexes[i];
//			if(parentHyphBreak.rules[i] != null)
//				rules[i] = parentHyphBreak.rules[i];
//
//			accumulator += hyphBreak.indexes.length;
//		}

		return new HyphenationBreak(indexesAndRules, accumulator);
	}

	public HyphenationBreak(int[] indexes){
		this(indexes, new String[indexes.length]);
	}

	public HyphenationBreak(int size){
		this(new int[size], new String[size]);
	}

	public HyphenationBreak(int[] indexes, String[] rules){
		Objects.requireNonNull(indexes);
		Objects.requireNonNull(rules);

		for(int i = 0; i < indexes.length; i ++)
			if(indexes[i] > 0)
				indexesAndRules.put(i, Pair.of(indexes[i], rules[i]));
		size = indexes.length;
	}

	public boolean isBreakpoint(int index){
		return (indexesAndRules.getOrDefault(index, EMPTY_PAIR).getKey() % 2 != 0);
	}

	public String getRule(int index){
		return indexesAndRules.getOrDefault(index, EMPTY_PAIR).getValue();
	}

	public List<String> getRules(){
		return indexesAndRules.values().stream()
			.map(Pair::getValue)
			.collect(Collectors.toList());
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
				for(int i = 0; i < size; i ++)
					if(nohyp.equals(syllabes.get(i))){
						resetBreakpoint(i);
						resetBreakpoint(i + nohypLength);

						modified = true;
					}
		}
		return modified;
	}

	private void resetBreakpoint(int index){
		if(index < indexesAndRules.size())
			indexesAndRules.put(index, EMPTY_PAIR);
	}

}
