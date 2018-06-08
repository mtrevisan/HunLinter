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

	public long countSyllabes(){
		return subwordHyphenations.stream()
			.map(SubwordHyphenation::getSyllabes)
			.mapToInt(List::size)
			.sum();
	}

	public List<String> getRules(){
		return subwordHyphenations.stream()
			.map(SubwordHyphenation::getRules)
			.flatMap(List::stream)
			.collect(Collectors.toList());
	}

	public boolean hasErrors(){
		for(SubwordHyphenation hyph : subwordHyphenations)
			for(boolean error : hyph.getErrors())
				if(error)
					return true;
		return false;
	}

	public String formatHyphenation(StringJoiner sj, Function<String, String> errorFormatter){
		Iterator<SubwordHyphenation> itr = subwordHyphenations.iterator();
		while(itr.hasNext()){
			SubwordHyphenation hyph = itr.next();

			int size = hyph.getSyllabes().size();
			for(int i = 0; i < size; i ++){
				Function<String, String> fun = (hyph.getErrors()[i]? errorFormatter: Function.identity());
				sj.add(fun.apply(hyph.getSyllabes().get(i)));
			}

			if(itr.hasNext())
				sj.add(HyphenationParser.HYPHEN);
		}
		return sj.toString();
	}

}
