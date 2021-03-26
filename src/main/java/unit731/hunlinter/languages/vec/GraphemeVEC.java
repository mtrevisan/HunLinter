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

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.services.RegexHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


final class GraphemeVEC{

	public static final String PHONEME_JJH = "ʝ";
	public static final String PHONEME_FH = "ꞙ";

	public static final String GRAPHEME_D_STROKE = "đ";
	private static final String GRAPHEME_F = "f";
	public static final String GRAPHEME_H = "h";
	private static final String GRAPHEME_FH = GRAPHEME_F + GRAPHEME_H;
	private static final String GRAPHEME_J = "j";
	public static final String GRAPHEME_I = "i";
	public static final String GRAPHEME_L = "l";
	public static final String GRAPHEME_L_STROKE = "ƚ";
	private static final String GRAPHEME_U = "u";
	private static final String GRAPHEME_W = "w";
	public static final String GRAPHEME_S = "s";
	public static final String GRAPHEME_T_STROKE = "ŧ";
	public static final String GRAPHEME_X = "x";

	private static final Pattern DIPHTONG1 = RegexHelper.pattern("[àèéíòóú][aeoiu]");
	private static final Pattern DIPHTONG2 = RegexHelper.pattern("[aeo][aeo]");
	private static final Pattern HYATUS = RegexHelper.pattern("[aeïoü][aàeèéiíoòóuú]|[iu][íú]");

	private static final Pattern ETEROPHONIC_SEQUENCE = RegexHelper.pattern("(?:^|[^aeiouàèéíòóú])[iju][àèéíòóú]");
	private static final Pattern ETEROPHONIC_SEQUENCE_W = RegexHelper.pattern("((?:^|[^s])t|(?:^|[^t])[kgrs]|i)u([aeiouàèéíòóú])");
	private static final Pattern ETEROPHONIC_SEQUENCE_J = RegexHelper.pattern("([^aeiouàèéíòóúw])i([aeiouàèéíòóú])");

	private static final Pattern SINGLE_SYLLABE_IU = RegexHelper.pattern("^[^aàeéèiïíoóòuüú]+[iu][aeiou]$");
	private static final Pattern SINGLE_SYLLABE_IU_UMLAUT = RegexHelper.pattern("^[^aàeéèiïíoóòuüú]+[ïü][aeiou]$");
	private static final Map<Character, Character> IU_UMLAUT = new HashMap<>();
	static{
		IU_UMLAUT.put('i', 'ï');
		IU_UMLAUT.put('u', 'ü');
		IU_UMLAUT.put('ï', 'i');
		IU_UMLAUT.put('ü', 'u');
	}


	private GraphemeVEC(){}

	public static boolean isDiphtong(final CharSequence word){
		if(RegexHelper.find(word, DIPHTONG1))
			return true;

		final Matcher m = RegexHelper.matcher(word, DIPHTONG2);
		return (m.find() && m.start() != WordVEC.getIndexOfStress(word));
	}

	public static boolean isHyatus(final CharSequence word){
		return RegexHelper.find(word, HYATUS);
	}

	public static boolean isEterophonicSequence(final CharSequence group){
		return RegexHelper.find(group, ETEROPHONIC_SEQUENCE);
	}


	/**
	 * Handle /j/ and /w/ phonemes.
	 *
	 * NOTE: Use mostly IPA standard, non-standard IPA character is used to mark /d͡ʒ/-affine grapheme.
	 *
	 * @param word	The word to be converted
	 * @return	The converted word
	 */
	public static String handleJHJWIUmlautPhonemes(final String word){
		String phonemizedWord = correctUIJGraphemes(word);

		//phonize etherophonic sequences
		if(phonemizedWord.contains(GRAPHEME_U))
			phonemizedWord = RegexHelper.replaceAll(phonemizedWord, ETEROPHONIC_SEQUENCE_W, "$1" + GRAPHEME_W + "$2");
		if(phonemizedWord.contains(GRAPHEME_I))
			phonemizedWord = RegexHelper.replaceAll(phonemizedWord, ETEROPHONIC_SEQUENCE_J, "$1" + GRAPHEME_J + "$2");

		return phonemizedWord;
	}

	private static String correctUIJGraphemes(String word){
		final int stressIndex = WordVEC.getIndexOfStress(word);
		if(stressIndex < 0 && RegexHelper.find(word, SINGLE_SYLLABE_IU)){
			final int index = word.length() - 2;
			word = WordVEC.replaceCharAt(word, index, IU_UMLAUT.get(word.charAt(index)));
		}

		//this step is mandatory before eterophonic sequence VjV
		if(word.contains(GRAPHEME_J))
			word = StringUtils.replace(word, GRAPHEME_J, PHONEME_JJH);

		return word;
	}

	private static String correctGrapheme(String word, final CharSequence grapheme, final Iterable<Pattern> eterophonicSequenceFalsePositives, final String newPhoneme){
		if(word.contains(grapheme)){
			final String newPhonemePattern = "$1" + newPhoneme + "$2";
			for(final Pattern p : eterophonicSequenceFalsePositives)
				word = RegexHelper.replaceAll(word, p, newPhonemePattern);
		}
		return word;
	}

	/**
	 * Convert back the /j/ and /w/ phonemes into the original alphabetical characters.
	 *
	 * @param word	The "phonemized" word to be converted
	 * @return	The converted word
	 */
	public static String rollbackJHJWIUmlautPhonemes(String word){
		word = StringUtils.replace(word, PHONEME_FH, GRAPHEME_FH);
		//this step is mandatory before eterophonic sequence VjV
		word = StringUtils.replace(word, GRAPHEME_J, GRAPHEME_I);
		word = StringUtils.replace(word, GRAPHEME_W, GRAPHEME_U);
		word = StringUtils.replace(word, PHONEME_JJH, GRAPHEME_J);

		final int stressIndex = WordVEC.getIndexOfStress(word);
		if(stressIndex < 0 && RegexHelper.find(word, SINGLE_SYLLABE_IU_UMLAUT)){
			final int index = word.length() - 2;
			word = WordVEC.replaceCharAt(word, index, IU_UMLAUT.get(word.charAt(index)));
		}

		return word;
	}

}
