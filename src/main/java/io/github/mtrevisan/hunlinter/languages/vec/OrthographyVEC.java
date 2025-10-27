/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.languages.vec;

import io.github.mtrevisan.hunlinter.languages.Orthography;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenationParser;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;


public final class OrthographyVEC extends Orthography{

	private static final String[] STRESS_CODES = {"a\\", "e\\", "o\\", "e/", "i/", "i\\", "ì", "i:", "o/", "u/", "u\\", "ù", "u:"};
	private static final String[] TRUE_STRESS = {"à", "è", "ò", "é", "í", "í", "í", "ï", "ó", "ú", "ú", "ú", "ü"};

	private static final String[] EXTENDED_CHARS = {"dh", "jh", "lh", "nh", "th"};
	private static final String[] TRUE_CHARS = {"đ", "ɉ", "ƚ", "ñ", "ŧ"};

	private static final String[] MB_MP = {"mb", "mp"};
	private static final String[] NB_NP = {"nb", "np"};

	//here `ï` and `ü` are really consonants, but are treated as vowels, in order for `argüio` to be valid
	private static final Pattern PATTERN_IUMLAUT_C = RegexHelper.pattern("ï([^aeiouàèéíïòóúü])");
	private static final Pattern PATTERN_UUMLAUT_C = RegexHelper.pattern("ü([^aeiouàèéíïòóúü])");
	private static final Pattern PATTERN_V_IUMLAUT = RegexHelper.pattern("([aeiouàèéíòóú])ï");
	private static final Pattern PATTERN_V_UUMLAUT = RegexHelper.pattern("([aeiouàèéíòóú])ü");

	private static final Pattern PATTERN_REMOVE_H_FROM_NOT_FH = RegexHelper.pattern("(?<!f)h(?!aeiouàèéíòóú)");

	private static final Pattern PATTERN_J_INTO_I = RegexHelper.pattern("^j(?=[^aeiouàèéíïòóúüh])");
	private static final Pattern PATTERN_I_INITIAL_INTO_J = RegexHelper.pattern("^i(?=[aeiouàèéíïòóúü])");
	private static final Pattern PATTERN_LH_INITIAL_INTO_L = RegexHelper.pattern("^ƚ(?=[^ʼaeiouàèéíïòóúüjw])");
	private static final Pattern PATTERN_LH_INSIDE_INTO_L = RegexHelper.pattern("([aeiouàèéíïòóúü])ƚ(?=[^aeiouàèéíïòóúüjw])|([^ ʼaeiouàèéíïòóúü–-])ƚ(?=[aeiouàèéíïòóúüjw])");
	private static final Pattern PATTERN_X_INTO_S = RegexHelper.pattern(GraphemeVEC.GRAPHEME_X + "(?=[cfkpstŧ])");
	private static final Pattern PATTERN_S_INTO_X = RegexHelper.pattern(GraphemeVEC.GRAPHEME_S + "(?=([mnñbdgɉvrl]))");
	private static final String FALSE_S_INTO_X = "èsre";

	private static class SingletonHelper{
		private static final Orthography INSTANCE = new OrthographyVEC();
	}


	private OrthographyVEC(){}

	@SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
	public static Orthography getInstance(){
		return SingletonHelper.INSTANCE;
	}

	/**
	 * Corrects the orthography of a given word.
	 *
	 * @param word	The word to be corrected.
	 * @return	The corrected word.
	 */
	@Override
	public String correctOrthography(final String word){
		//correct stress
		String correctedWord = StringUtils.replaceEach(word, STRESS_CODES, TRUE_STRESS);

		correctedWord = WordVEC.markDefaultStress(correctedWord);

		//correct h occurrences after d, j, l, n, t
		correctedWord = StringUtils.replaceEach(correctedWord, EXTENDED_CHARS, TRUE_CHARS);

		//remove other occurrences of h not into fhV
		if(correctedWord.length() > 1 && correctedWord.contains(GraphemeVEC.GRAPHEME_H))
			correctedWord = RegexHelper.replaceAll(correctedWord, PATTERN_REMOVE_H_FROM_NOT_FH, StringUtils.EMPTY);

		//correct mb/mp occurrences into nb/np
		correctedWord = StringUtils.replaceEach(correctedWord, MB_MP, NB_NP);

		//correct ïC/üC occurrences into iC/uC
		correctedWord = RegexHelper.replaceAll(correctedWord, PATTERN_IUMLAUT_C, "i$1");
		correctedWord = RegexHelper.replaceAll(correctedWord, PATTERN_UUMLAUT_C, "u$1");
		//correct Vï/Vü occurrences into Vi/Vu
		correctedWord = RegexHelper.replaceAll(correctedWord, PATTERN_V_IUMLAUT, "$1i");
		correctedWord = RegexHelper.replaceAll(correctedWord, PATTERN_V_UUMLAUT, "$1u");

		correctedWord = correctIJOccurrences(correctedWord);

		//correct lh occurrences into l not at the beginning of a word and not between vowels
		correctedWord = RegexHelper.replaceAll(correctedWord, PATTERN_LH_INITIAL_INTO_L, GraphemeVEC.GRAPHEME_L);
		correctedWord = RegexHelper.replaceAll(correctedWord, PATTERN_LH_INSIDE_INTO_L, "$1l");
		//correct x occurrences into s prior to c, f, k, p, s, t, ŧ
		//correct s occurrences into x prior to m, n, ñ, b, d, g, j, ɉ, v, r, l
		correctedWord = RegexHelper.replaceAll(correctedWord, PATTERN_X_INTO_S, GraphemeVEC.GRAPHEME_S);
		if(!correctedWord.endsWith(FALSE_S_INTO_X))
			correctedWord = RegexHelper.replaceAll(correctedWord, PATTERN_S_INTO_X, GraphemeVEC.GRAPHEME_X);

		//eliminate consonant geminates
		correctedWord = reduceGeminates(correctedWord);

		return correctedWord;
	}

	/**
	 * Corrects occurrences of 'i' into 'j' at the beginning of a word followed by a vowel and between vowels, correcting also the converse.
	 *
	 * @param word	The word to be corrected.
	 * @return	The corrected word.
	 */
	private static String correctIJOccurrences(String word){
		//correct i occurrences into j at the beginning of a word followed by a vowel and between vowels,
		//correcting also the converse
		word = RegexHelper.replaceAll(word, PATTERN_J_INTO_I, GraphemeVEC.GRAPHEME_I);
		word = RegexHelper.replaceAll(word, PATTERN_I_INITIAL_INTO_J, GraphemeVEC.GRAPHEME_J);
		return word;
	}

	/**
	 * Reduces geminates in a given word, excluding cases such as /^[ie]nn/, /^d[ei]ss/, or /[eo]nne$/.
	 *
	 * @param word	The word to reduce geminates in.
	 * @return	The word with geminates reduced.
	 */
	//FIXME speed up
	private static String reduceGeminates(final CharSequence word){
		final StringBuilder sb = new StringBuilder(word);
		for(int i = 1; i < sb.length(); i ++){
			char chr = sb.charAt(i);
			if(chr == sb.charAt(i - 1) && Arrays.binarySearch(WordVEC.VOWELS_ARRAY, chr) < 0){
				final boolean starting1With = (i == 2 && ((chr = word.charAt(0)) == 'i' || chr == 'e'));
				final boolean starting2With = (i == 3 && word.charAt(0) == 'd' && ((chr = word.charAt(1)) == 'e' || chr == 'i'));
				final boolean endingWith = (i == sb.length() - 2 && ((chr = word.charAt(word.length() - 4)) == 'o' || chr == 'e')
					&& word.charAt(word.length() - 1) == 'e');
				if(!starting1With && !starting2With && !endingWith){
					sb.replace(i - 1, i + 1, String.valueOf(sb.charAt(i)));
					i --;
				}
			}
		}
		return sb.toString();
	}

	/**
	 * This method checks for syllabation errors in a given list of syllabes.
	 *
	 * @param syllabes	The list of syllabes to check for errors.
	 * @return	An array of booleans representing the error status for each syllabe.
	 */
	@Override
	public boolean[] getSyllabationErrors(final List<String> syllabes){
		final boolean[] errors = new boolean[syllabes.size()];
		for(int i = 0; i < syllabes.size(); i ++){
			final String syllabe = syllabes.get(i);
			errors[i] = (!syllabe.contains(HyphenationParser.APOSTROPHE)
				&& !StringUtils.contains(syllabe, HyphenationParser.MODIFIER_LETTER_APOSTROPHE)
				&& !(syllabe.equals(HyphenationParser.MINUS_SIGN) || syllabe.equals(HyphenationParser.EN_DASH) || syllabe.equals(HyphenationParser.SOFT_HYPHEN))
				&& !StringUtils.containsAny(syllabe, WordVEC.VOWELS_ARRAY));
		}
		return errors;
	}

	/**
	 * Returns the index of the stressed syllable in the given list of syllables, counting from the last syllable.
	 *
	 * @param syllabes	The list of syllables.
	 * @return	The index of the stressed syllable counting from the last syllable, or {@code -1} if no stressed syllable is found.
	 */
	@Override
	public int getStressedSyllabeIndexFromLast(final List<String> syllabes){
		for(int i = syllabes.size() - 1; i >= 0; i --)
			if(hasStressedGrapheme(syllabes.get(i)))
				return i;
		return -1;
	}

	/**
	 * Counts the number of graphemes in a given word.
	 *
	 * @param word	The word to count the graphemes in.
	 * @return	The number of graphemes in the word.
	 */
	@Override
	public int countGraphemes(final String word){
		return WordVEC.countGraphemes(word);
	}

	/**
	 * Marks the default stress in a given word.
	 *
	 * @param word	The word to mark the default stress in.
	 * @return	The word with the default stress marked.
	 */
	@Override
	public String markDefaultStress(final String word){
		return WordVEC.markDefaultStress(word);
	}

	/**
	 * Checks if the given word has a stressed grapheme.
	 *
	 * @param word	The word to check.
	 * @return	Whether the word has a stressed grapheme.
	 */
	@Override
	public boolean hasStressedGrapheme(final String word){
		return WordVEC.hasStressedGrapheme(word);
	}

}
