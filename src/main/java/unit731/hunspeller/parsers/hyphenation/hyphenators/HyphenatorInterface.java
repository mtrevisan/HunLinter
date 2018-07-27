package unit731.hunspeller.parsers.hyphenation.hyphenators;

import java.util.List;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;


public interface HyphenatorInterface{

	Hyphenation hyphenate(String word);

	Hyphenation hyphenate(String word, String addedRule, HyphenationParser.Level level);

	List<String> splitIntoCompounds(String word);

}
