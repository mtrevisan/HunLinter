package unit731.hunspeller.parsers.hyphenation.dtos;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;


@AllArgsConstructor
public class HyphenationBreak{

	public static final Pair<Integer, String> EMPTY_PAIR = Pair.of(0, null);


	@NonNull
	private final Map<Integer, Pair<Integer, String>> indexesAndRules;
	@Getter
	private final int size;


	public static HyphenationBreak getEmptyInstance(){
		return new HyphenationBreak(Collections.<Integer, Pair<Integer, String>>emptyMap(), 0);
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

	public void enforceNoHyphens(List<String> syllabes, Set<String> noHyphen){
		int syllabesCount = syllabes.size();
		if(syllabesCount > 1){
			int wordLength = syllabes.stream()
				.map(String::length)
				.mapToInt(x -> x)
				.sum();
			for(String nohyp : noHyphen){
				if(syllabesCount <= 1)
					break;

				int nohypLength = nohyp.length();
				if(nohyp.charAt(0) == '^'){
					if(syllabes.get(0).equals(nohyp.substring(1))){
						indexesAndRules.remove(1);
						indexesAndRules.remove(nohypLength);

						if(syllabesCount > 1){
							//merge syllabe with following
							String removedSyllabe = syllabes.remove(0);
							syllabes.set(0, removedSyllabe + syllabes.get(0));

							syllabesCount --;
						}
					}
				}
				else if(nohyp.charAt(nohypLength - 1) == '$'){
					if(syllabes.get(syllabesCount - 1).equals(nohyp.substring(0, nohypLength - 1))){
						indexesAndRules.remove(wordLength - nohypLength - 1);
						indexesAndRules.remove(wordLength - 1);

						if(syllabesCount > 1){
							//merge syllabe with previous
							syllabesCount --;
							String removedSyllabe = syllabes.remove(syllabesCount);
							syllabes.set(syllabesCount - 1, syllabes.get(syllabesCount - 1) + removedSyllabe);
						}
					}
				}
				else{
					int index = 0;
					for(int i = 0; syllabesCount > 1 && i < syllabesCount; i ++){
						String syllabe = syllabes.get(i);

						if(syllabe.equals(nohyp)){
							indexesAndRules.remove(index);
							indexesAndRules.remove(index + nohypLength);

							if(i == 0){
								//merge syllabe with following
								String removedSyllabe = syllabes.remove(0);
								syllabes.set(0, removedSyllabe + syllabes.get(0));

								syllabesCount --;
							}
							else if(i == syllabesCount - 1){
								//merge syllabe with previous
								syllabesCount --;
								String removedSyllabe = syllabes.remove(syllabesCount);
								syllabes.set(syllabesCount - 1, syllabes.get(syllabesCount - 1) + removedSyllabe);
							}
							else{
								//merge syllabe with previous
								String removedSyllabe1 = (i >= 0? syllabes.remove(i): StringUtils.EMPTY);
								String removedSyllabe0 = (i >= 0? syllabes.remove(i): StringUtils.EMPTY);
								if(syllabes.isEmpty())
									syllabes.add(removedSyllabe1 + removedSyllabe0);
								else
									syllabes.set(i - 1, syllabes.get(i - 1) + removedSyllabe1 + removedSyllabe0);

								syllabesCount -= 2;
							}

							i --;
						}

						index += syllabe.length();
					}
				}
			}
		}
	}

}
