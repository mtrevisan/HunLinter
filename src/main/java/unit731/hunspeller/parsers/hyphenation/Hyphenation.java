package unit731.hunspeller.parsers.hyphenation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;


@AllArgsConstructor
@Getter
public class Hyphenation{

	@NonNull
	private final List<String> syllabes;
	@NonNull
	private final boolean[] errors;


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
		boolean response = false;
		for(boolean error : errors)
			if(error){
				response = true;
				break;
			}
		return response;
	}

}
