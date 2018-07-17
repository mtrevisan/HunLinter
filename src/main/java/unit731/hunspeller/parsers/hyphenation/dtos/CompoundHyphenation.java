package unit731.hunspeller.parsers.hyphenation.dtos;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;


@AllArgsConstructor(staticName = "build")
@Getter
@EqualsAndHashCode
public class CompoundHyphenation implements HyphenationInterface{

	private final List<HyphenationInterface> subHyphenations;


	@Override
	public boolean isHyphenated(){
		return subHyphenations.stream()
			.anyMatch(HyphenationInterface::isHyphenated);
	}

	@Override
	public List<String> getSyllabes(){
		return subHyphenations.stream()
			.map(HyphenationInterface::getSyllabes)
			.flatMap(List::stream)
			.collect(Collectors.toList());
	}

	@Override
	public int countSyllabes(){
		return subHyphenations.stream()
			.mapToInt(HyphenationInterface::countSyllabes)
			.sum();
	}

	@Override
	public List<String> getRules(){
		return subHyphenations.stream()
			.map(HyphenationInterface::getRules)
			.flatMap(List::stream)
			.collect(Collectors.toList());
	}

	@Override
	public boolean[] getErrors(){
		return ArrayUtils.toPrimitive(subHyphenations.stream()
			.map(HyphenationInterface::getErrors)
			.map(ArrayUtils::toObject)
			.flatMap(Arrays::stream)
			.toArray(Boolean[]::new));
	}

	@Override
	public boolean hasErrors(){
		return subHyphenations.stream()
			.anyMatch(HyphenationInterface::hasErrors);
	}

	@Override
	public StringJoiner formatHyphenation(StringJoiner sj, Function<String, String> errorFormatter){
		for(HyphenationInterface sub : subHyphenations)
			sj = sub.formatHyphenation(sj, errorFormatter);
		return sj;
	}

	@Override
	public String toString(){
		return formatHyphenation(new StringJoiner(HyphenationParser.SOFT_HYPHEN), Function.identity())
			.toString();
	}

}
