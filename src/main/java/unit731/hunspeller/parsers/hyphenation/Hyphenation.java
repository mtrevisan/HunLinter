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
	private final List<SubwordHyphenation> subwordHyphenations;


	public List<String> getSyllabes(){
		return subwordHyphenations.stream()
			.map(SubwordHyphenation::getSyllabes)
			.flatMap(List::stream)
			.collect(Collectors.toList());
	}

	public int countSyllabes(){
		return subwordHyphenations.stream()
			.mapToInt(SubwordHyphenation::countSyllabes)
			.sum();
	}

	public List<String> getRules(){
		return subwordHyphenations.stream()
			.map(SubwordHyphenation::getRules)
			.flatMap(List::stream)
			.collect(Collectors.toList());
	}

	public boolean hasErrors(){
		boolean result = false;
		for(SubwordHyphenation hyph : subwordHyphenations)
			if(hyph.hasErrors()){
				result = true;
				break;
			}
		return result;
	}

	public String formatHyphenation(StringJoiner sj, Function<String, String> errorFormatter){
		Iterator<SubwordHyphenation> itr = subwordHyphenations.iterator();
		while(itr.hasNext()){
			SubwordHyphenation hyph = itr.next();

			sj.add(hyph.formatHyphenation(sj, errorFormatter));

			if(itr.hasNext())
				sj.add(HyphenationParser.HYPHEN);
		}
		return sj.toString();
	}

}
