package unit731.hunlinter.languages.vec;

import unit731.hunlinter.languages.WordTokenizer;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;

import java.util.List;
import java.util.regex.Pattern;


public class WordTokenizerVEC extends WordTokenizer{

	private static final String UNICODE_APOSTROPHE = "'";
	private static final String UNICODE_APOSTROPHES = UNICODE_APOSTROPHE + HyphenationParser.RIGHT_MODIFIER_LETTER_APOSTROPHE;

	private static final Pattern TOKENIZING_CHARACTERS;

	static{
		final String quotedTokenizingChars = Pattern.quote(DEFAULT_TOKENIZING_CHARACTERS);
		TOKENIZING_CHARACTERS = Pattern.compile("(?i)"
				+ "([dglƚnsv]|(a|[ai\u2019]n)dó|[kps]o|pu?ò|st|tan|kuan|tut|([n\u2019]|in)t|tèr[sŧ]|k[uo]art|kuint|sèst|[kp]a|sen[sŧ]|komò|fra|nu|re|intor)[" + UNICODE_APOSTROPHES + "](?=[" + quotedTokenizingChars + "])"
				+ "|"
				+ "(?<=\\s)[" + UNICODE_APOSTROPHES + "][^" + quotedTokenizingChars + "]+"
			);
	}


	@Override
	public List<String> tokenize(final String text){
		final List<String> list = super.tokenize(text);
		return joinApostrophes(list);
	}

	protected List<String> joinApostrophes(final List<String> list){
		return join(list, TOKENIZING_CHARACTERS, UNICODE_APOSTROPHES,
			group -> group.replaceAll(UNICODE_APOSTROPHE, HyphenationParser.RIGHT_MODIFIER_LETTER_APOSTROPHE));
	}

}
