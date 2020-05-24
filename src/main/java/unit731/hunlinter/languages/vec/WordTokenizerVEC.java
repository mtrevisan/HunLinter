package unit731.hunlinter.languages.vec;

import org.apache.commons.lang3.RegExUtils;
import unit731.hunlinter.languages.WordTokenizer;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;

import java.util.List;
import java.util.regex.Pattern;


public class WordTokenizerVEC extends WordTokenizer{

	private static final String APOSTROPHES = HyphenationParser.APOSTROPHE + HyphenationParser.RIGHT_MODIFIER_LETTER_APOSTROPHE;

	private static final Pattern TOKENIZING_CHARACTERS;
	static{
		final String quotedTokenizingChars = Pattern.quote(DEFAULT_TOKENIZING_CHARACTERS);
		TOKENIZING_CHARACTERS = Pattern.compile("(?i)"
			+ "(a[lƚnv]|di|e[lƚn]|[gks][oó]|[iu]n|[lƚ][aài]|v[aàeèéiíoòóuú])[" + APOSTROPHES + "](?=[" + quotedTokenizingChars + "]|$)"
			+ "|"
			+ "[" + APOSTROPHES + "](a[nrsŧ]|b[iuú]|e[cdglƚmnrstv-]|i[eégklƚmnoóstv]|[kpsv]a|[lntuéíòóú]|o[klƚmnrsx]|s[eé]|à[nrs]|èc|[ñv][aàeèéiíoòóuú]|[lƚ]o)"
		);
	}


	@Override
	public List<String> tokenize(String text){
		text = RegExUtils.replaceAll(text, TOKENIZING_CHARACTERS, "$1" + HyphenationParser.RIGHT_MODIFIER_LETTER_APOSTROPHE + "$2");
		return super.tokenize(text);
	}

}
