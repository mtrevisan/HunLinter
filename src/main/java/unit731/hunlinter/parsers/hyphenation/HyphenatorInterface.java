package unit731.hunlinter.parsers.hyphenation;


public interface HyphenatorInterface{

	Hyphenation hyphenate(final String word);

	Hyphenation hyphenate(final String word, final String addedRule, final HyphenationParser.Level level);

	String[] splitIntoCompounds(final String word);

}
