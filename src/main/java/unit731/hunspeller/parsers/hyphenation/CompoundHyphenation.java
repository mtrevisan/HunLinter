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
@EqualsAndHashCode
public class CompoundHyphenation implements HyphenationInterface{

	@NonNull
	private final Hyphenation firstSubHyphenation;
	@NonNull
	private final String breakCharacter;
	@NonNull
	private final Hyphenation lastSubHyphenation;


	public CompoundHyphenation(Hyphenation hyphenation){
		firstSubHyphenation = hyphenation;
		breakCharacter = null;
		lastSubHyphenation = null;
	}

	@Override
	public boolean isCompounded(){
		return (lastSubHyphenation != null);
	}

	@Override
	public boolean isHyphenated(){
		return (firstSubHyphenation.isHyphenated() || isCompounded() && lastSubHyphenation.isHyphenated());
	}

	@Override
	public boolean hasErrors(){
		return (firstSubHyphenation.hasErrors() || isCompounded() && lastSubHyphenation.hasErrors());
	}

	@Override
	public List<String> getSyllabes(){
		List<String> syllabes = firstSubHyphenation.getSyllabes();
		if(isCompounded())
			syllabes.addAll(lastSubHyphenation.getSyllabes());
		return syllabes;
	}

	@Override
	public int countSyllabes(){
		int syllabes = firstSubHyphenation.countSyllabes();
		if(isCompounded())
			syllabes += lastSubHyphenation.countSyllabes();
		return syllabes;
	}

	@Override
	public List<String> getRules(){
		List<String> rules = firstSubHyphenation.getRules();
		if(isCompounded())
			rules.addAll(lastSubHyphenation.getRules());
		return rules;
	}

	@Override
	public StringJoiner formatHyphenation(StringJoiner sj, Function<String, String> errorFormatter){
		sj = firstSubHyphenation.formatHyphenation(sj, errorFormatter);
		if(isCompounded()){
			sj.add(breakCharacter);
			sj = lastSubHyphenation.formatHyphenation(sj, errorFormatter);
		}
		return sj;
	}

}
