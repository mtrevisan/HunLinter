package unit731.hunspeller.parsers.hyphenation;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.ArrayUtils;


@AllArgsConstructor
@Getter
@EqualsAndHashCode(of = {"syllabes", "breakCharacter"})
public class Hyphenation implements HyphenationInterface{

	@NonNull
	private final List<String> syllabes;
	@NonNull
	private final List<String> rules;
	@NonNull
	private final boolean[] errors;
	@NonNull
	private final String breakCharacter;


	public static Hyphenation merge(List<Hyphenation> hyphs, String breakCharacter){
		List<String> syllabes = hyphs.stream()
			.map(Hyphenation::getSyllabes)
			.flatMap(List::stream)
			.collect(Collectors.toList());
		List<String> rules = hyphs.stream()
			.map(Hyphenation::getRules)
			.flatMap(List::stream)
			.collect(Collectors.toList());
		Boolean[] errors = hyphs.stream()
			.map(Hyphenation::getErrors)
			.map(ArrayUtils::toObject)
			.flatMap(Arrays::stream)
			.toArray(Boolean[]::new);
		return new Hyphenation(syllabes, rules, ArrayUtils.toPrimitive(errors), breakCharacter);
	}

	/**
	 * @param idx	Index with respect to the word from which to extract the index of the corresponding syllabe
	 * @return the (relative) index of the syllabe at the given (global) index
	 */
	public int getSyllabeIndex(int idx){
		int k = -1;
		int size = countSyllabes();
		for(int i = 0; i < size; i ++){
			String syllabe = syllabes.get(i);
			idx -= syllabe.length();
			if(idx < 0){
				k = i;
				break;
			}
		}
		return k;
	}

	/**
	 * @param idx	Index with respect to the word from which to extract the index of the corresponding syllabe
	 * @return the syllabe at the given (global) index
	 */
	public String getSyllabe(int idx){
		return syllabes.get(getSyllabeIndex(idx));
	}

	@Override
	public int countSyllabes(){
		return syllabes.size();
	}

	/**
	 * @param idx	Index of syllabe to extract, if negative then it's relative to the last syllabe
	 * @return the syllabe at the given (relative) index
	 */
	public String getAt(int idx){
		return syllabes.get(restoreRelativeIndex(idx));
	}

	private int restoreRelativeIndex(int idx){
		return (idx + countSyllabes()) % countSyllabes();
	}

	@Override
	public boolean isHyphenated(){
		return !rules.isEmpty();
	}

	@Override
	public boolean hasErrors(){
		boolean result = false;
		for(boolean error : errors)
			if(error){
				result = true;
				break;
			}
		return result;
	}

	@Override
	public StringJoiner formatHyphenation(StringJoiner sj, Function<String, String> errorFormatter){
		int size = countSyllabes();
		for(int i = 0; i < size; i ++){
			Function<String, String> fun = (errors[i]? errorFormatter: Function.identity());
			sj.add(fun.apply(syllabes.get(i)));
		}
		return sj;
	}

	@Override
	public String toString(){
		return formatHyphenation(new StringJoiner(HyphenationParser.SOFT_HYPHEN), Function.identity())
			.toString();
	}

}
