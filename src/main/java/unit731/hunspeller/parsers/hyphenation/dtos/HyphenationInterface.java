package unit731.hunspeller.parsers.hyphenation.dtos;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;


public interface HyphenationInterface{

	boolean isHyphenated();

	List<String> getSyllabes();

	int countSyllabes();

	List<String> getRules();

	boolean[] getErrors();

	boolean hasErrors();

	StringJoiner formatHyphenation(StringJoiner sj, Function<String, String> errorFormatter);

}
