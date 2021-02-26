/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package unit731.hunlinter.languages.vec;

import org.apache.commons.lang3.RegExUtils;
import unit731.hunlinter.languages.WordTokenizer;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;

import java.util.List;
import java.util.regex.Pattern;


public class WordTokenizerVEC extends WordTokenizer{

	private static final String APOSTROPHES = HyphenationParser.APOSTROPHE + HyphenationParser.MODIFIER_LETTER_APOSTROPHE;

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
		text = RegExUtils.replaceAll(text, TOKENIZING_CHARACTERS, "$1" + HyphenationParser.MODIFIER_LETTER_APOSTROPHE + "$2");
		return super.tokenize(text);
	}

}
