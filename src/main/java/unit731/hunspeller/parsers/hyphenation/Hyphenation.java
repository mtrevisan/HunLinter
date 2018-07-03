package unit731.hunspeller.parsers.hyphenation;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;


@AllArgsConstructor
@Getter
@EqualsAndHashCode(of = {"syllabes", "breakCharacter"})
public class Hyphenation{

	@NonNull
	private final List<String> syllabes;
	@NonNull
	private final List<String> rules;
	@NonNull
	private final boolean[] errors;
	@NonNull
	private final String breakCharacter;


	/**
	 * @param idx	Index with respect to the word from which to extract the index of the corresponding syllabe
	 * @return the (relative) index of the syllabe at the given (global) index
	 */
	public int getSyllabeIndex(int idx){
		int k = -1;
		int size = syllabes.size();
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
		return (idx + syllabes.size()) % syllabes.size();
	}

	public boolean hasErrors(){
		boolean result = false;
		for(boolean error : errors)
			if(error){
				result = true;
				break;
			}
		return result;
	}

	public StringJoiner formatHyphenation(StringJoiner sj, Function<String, String> errorFormatter){
		int size = syllabes.size();
		for(int i = 0; i < size; i ++){
			Function<String, String> fun = (errors[i]? errorFormatter: Function.identity());
			sj.add(fun.apply(syllabes.get(i)));
		}
		return sj;
	}

	@Override
	public String toString(){
		return formatHyphenation(new StringJoiner(breakCharacter), Function.identity())
			.toString();
	}

}
