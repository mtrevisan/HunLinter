package unit731.hunlinter.languages.vec;

import org.apache.commons.lang3.RegExUtils;
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
				+ "([dglƚnsv]|(?:a|[ai\u2019]n)dó|[kps]o|pu?ò|st|tan|kuan|tut|(?:[n\u2019]|in)t|tèr[sŧ]|k[uo]art|kuint|sèst|[kp]a|sen[sŧ]|komò|fra|nu|re|intor)[" + UNICODE_APOSTROPHES + "](?=[" + quotedTokenizingChars + "])"
				+ "|"
				+ "[" + UNICODE_APOSTROPHES + "]([^" + quotedTokenizingChars + "]+)"
			);
	}


	@Override
	public List<String> tokenize(String text){
		text = RegExUtils.replaceAll(text, TOKENIZING_CHARACTERS, "$1" + HyphenationParser.RIGHT_MODIFIER_LETTER_APOSTROPHE + "$2");
		return super.tokenize(text);
	}

}
