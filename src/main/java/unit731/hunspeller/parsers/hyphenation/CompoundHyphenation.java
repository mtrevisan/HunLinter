package unit731.hunspeller.parsers.hyphenation;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;


@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class CompoundHyphenation implements HyphenationInterface{

	private final List<HyphenationInterface> subHyphenations;
	private final List<String> breakCharacters;
	private final boolean startsWithBreak;


	@Override
	public boolean isHyphenated(){
		return subHyphenations.stream()
			.anyMatch(Hyphenation::isHyphenated);
	}

	@Override
	public boolean hasErrors(){
		return subHyphenations.stream()
			.anyMatch(Hyphenation::hasErrors);
	}

	@Override
	public List<String> getSyllabes(){
		return subHyphenations.stream()
			.map(Hyphenation::getSyllabes)
			.flatMap(List::stream)
			.collect(Collectors.toList());
	}

	@Override
	public int countSyllabes(){
		return subHyphenations.stream()
			.mapToInt(Hyphenation::countSyllabes)
			.sum();
	}

	@Override
	public List<String> getRules(){
		return subHyphenations.stream()
			.map(Hyphenation::getRules)
			.flatMap(List::stream)
			.collect(Collectors.toList());
	}

	@Override
	public StringJoiner formatHyphenation(StringJoiner sj, Function<String, String> errorFormatter){
		int j = 0;
		if(startsWithBreak)
			sj.add(breakCharacters.get(j ++));
		int breaks = breakCharacters.size();
		for(HyphenationInterface sub : subHyphenations){
			sj = sub.formatHyphenation(sj, errorFormatter);
			if(j < breaks)
				sj.add(breakCharacters.get(j ++));
		}
		return sj;
	}

}
