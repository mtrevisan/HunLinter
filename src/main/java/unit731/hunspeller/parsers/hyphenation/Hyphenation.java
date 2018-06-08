package unit731.hunspeller.parsers.hyphenation;

import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;


@AllArgsConstructor
@Getter
public class Hyphenation{

	@NonNull
	private final List<String> syllabes;
	@NonNull
	private final List<String> rules;
	@NonNull
	private final boolean[] errors;


	public static List<String> getSyllabes(List<Hyphenation> hyphenation){
		return hyphenation.stream()
			.map(Hyphenation::getSyllabes)
			.flatMap(List::stream)
			.collect(Collectors.toList());
	}

	public static long countSyllabes(List<Hyphenation> hyphenation){
		return hyphenation.stream()
			.map(Hyphenation::getSyllabes)
			.mapToInt(List::size)
			.sum();
	}

	public static List<String> getRules(List<Hyphenation> hyphenation){
		return hyphenation.stream()
			.map(Hyphenation::getRules)
			.flatMap(List::stream)
			.collect(Collectors.toList());
	}

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

	public static boolean hasErrors(List<Hyphenation> hyphenation){
		for(Hyphenation hyph : hyphenation)
			for(boolean error : hyph.errors)
				if(error)
					return true;
		return false;
	}

	public static String formatHyphenation(List<Hyphenation> hyphenation, StringJoiner sj, Function<String, String> errorFormatter){
		Iterator<Hyphenation> itr = hyphenation.iterator();
		while(itr.hasNext()){
			Hyphenation hyph = itr.next();

			int size = hyph.syllabes.size();
			for(int i = 0; i < size; i ++){
				Function<String, String> fun = (hyph.errors[i]? errorFormatter: Function.identity());
				sj.add(fun.apply(hyph.syllabes.get(i)));
			}

			if(itr.hasNext())
				sj.add(HyphenationParser.HYPHEN);
		}
		return sj.toString();
	}

}
