package unit731.hunspeller.parsers.hyphenation;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;


public interface HyphenationInterface{

	boolean isHyphenated();

	boolean hasErrors();

	List<String> getSyllabes();

	int countSyllabes();

	List<String> getRules();

	StringJoiner formatHyphenation(StringJoiner sj, Function<String, String> errorFormatter);

}
