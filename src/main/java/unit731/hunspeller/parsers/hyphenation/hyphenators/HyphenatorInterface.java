package unit731.hunspeller.parsers.hyphenation.hyphenators;

import java.util.List;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationInterface;


public interface HyphenatorInterface{

	HyphenationInterface hyphenate(String word);

	HyphenationInterface hyphenate(String word, String addedRule, HyphenationParser.Level level) throws CloneNotSupportedException;

	List<String> splitIntoCompounds(String word);

}
