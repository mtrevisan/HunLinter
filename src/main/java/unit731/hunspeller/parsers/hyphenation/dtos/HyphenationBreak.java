package unit731.hunspeller.parsers.hyphenation.dtos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;


@AllArgsConstructor
public class HyphenationBreak{

	public static final Pair<Integer, String> EMPTY_PAIR = Pair.of(0, null);


	@NonNull
	private Map<Integer, Pair<Integer, String>> indexesAndRules = new HashMap<>();
	@Getter
	private final int size;


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
		int wordLength = syllabes.stream()
			.map(String::length)
			.mapToInt(x -> x)
			.sum();
		int syllabesCount = syllabes.size();
		for(String nohyp : noHyphen){
			int nohypLength = nohyp.length();
			if(nohyp.charAt(0) == '^'){
				if(syllabes.get(0).startsWith(nohyp.substring(1))){
					indexesAndRules.remove(1);
					indexesAndRules.remove(nohypLength);

					modified = true;
				}
			}
			else if(nohyp.charAt(nohypLength - 1) == '$'){
				if(syllabes.get(syllabes.size() - 1).endsWith(nohyp.substring(0, nohypLength - 1))){
					indexesAndRules.remove(wordLength - nohypLength - 1);
					indexesAndRules.remove(wordLength - 1);

					modified = true;
				}
			}
			else{
				int index = 0;
				for(int i = 0; i < syllabesCount; i ++){
					String syllabe = syllabes.get(i);
					if(syllabe.equals(nohyp)){
						indexesAndRules.remove(index);
						indexesAndRules.remove(index + nohypLength);

						modified = true;
					}

					index += syllabe.length();
				}
			}
		}
		return modified;
	}

}
