package unit731.hunspeller.parsers.hyphenation;

import java.util.List;


public interface HyphenatorInterface{

	Hyphenation hyphenate(final String word);

	Hyphenation hyphenate(final String word, final String addedRule, final HyphenationParser.Level level);

	List<String> splitIntoCompounds(final String word);

}
