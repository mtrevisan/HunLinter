package unit731.hunspeller.parsers.hyphenation.hyphenators;

import java.util.List;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;


public interface HyphenatorInterface{

	Hyphenation hyphenate(final String word);

	Hyphenation hyphenate(final String word, final String addedRule, final HyphenationParser.Level level);

	List<String> splitIntoCompounds(final String word);

}
