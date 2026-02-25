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
import io.github.mtrevisan.hunlinter.services.TrieReplacer;
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

	private static final String FALSE_S_INTO_X = "èsre";

	private static class SingletonHelper{
		private static final Orthography INSTANCE = new OrthographyVEC();
	}


	private static final TrieReplacer TRIE_STRESS = new TrieReplacer(STRESS_CODES, TRUE_STRESS);
	private static final TrieReplacer TRIE_EXTENDED = new TrieReplacer(EXTENDED_CHARS, TRUE_CHARS);
	private static final TrieReplacer TRIE_NASAL = new TrieReplacer(MB_MP, NB_NP);


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
	//FIXME starts with 31 s
	@Override
	public String correctOrthography(final String word){
		//correct stress
		String correctedWord = TRIE_STRESS.replaceEach(word);

		correctedWord = WordVEC.markDefaultStress(correctedWord);

		//correct h occurrences after d, j, l, n, t
		correctedWord = TRIE_EXTENDED.replaceEach(correctedWord);

		//remove other occurrences of h not into fhV
		if(correctedWord.length() > 1 && correctedWord.contains(GraphemeVEC.GRAPHEME_H))
			correctedWord = removeHFromNotFH(correctedWord);

		//correct mb/mp occurrences into nb/np
		correctedWord = TRIE_NASAL.replaceEach(correctedWord);

		//correct ïC/üC occurrences into iC/uC
		correctedWord = normalizeDiaeresisBeforeConsonant(correctedWord);
		//correct Vï/Vü occurrences into Vi/Vu
		correctedWord = normalizeDiaeresisAfterVowel(correctedWord);

		//correct i occurrences into j at the beginning of a word followed by a vowel and between vowels,
		//correcting also the converse
		correctedWord = jIntoI(correctedWord);
		correctedWord = iInitialIntoJ(correctedWord);

		//correct lh occurrences into l not at the beginning of a word and not between vowels
		correctedWord = lhInitialIntoL(correctedWord);
		correctedWord = lhInsideIntoL(correctedWord);
		//correct x occurrences into s prior to c, f, k, p, s, t, ŧ
		//correct s occurrences into x prior to m, n, ñ, b, d, g, j, ɉ, v, r, l
		correctedWord = xIntoS(correctedWord);
		if(!correctedWord.endsWith(FALSE_S_INTO_X))
			correctedWord = sIntoX(correctedWord);

		//eliminate consonant geminates
		correctedWord = reduceGeminates(correctedWord);

		return correctedWord;
	}

	//Pattern PATTERN_REMOVE_H_FROM_NOT_FH = RegexHelper.pattern("(?<!f)h(?!aeiouàèéíòóú)")
	private static String removeHFromNotFH(final String word){
		final int length = word.length();
		final StringBuilder sb = new StringBuilder(length);
		for(int i = 0; i < length; i ++){
			final char c = word.charAt(i);
			if(c == 'h'){
				final char prev = (i > 0? word.charAt(i - 1): 0);
				final char next = (i + 1 < length ? word.charAt(i + 1): 0);

				//equivalent of (?<!f)h(?!vowel)
				if(prev != 'f' && !WordVEC.isVowel(next))
					//skip this h
					continue;
			}
			sb.append(c);
		}
		return sb.toString();
	}

	//here `ï` and `ü` are really consonants, but are treated as vowels, in order for `argüio` to be valid
	//Pattern PATTERN_I_DIAERESIS_C = RegexHelper.pattern("ï([^aeiouàèéíïòóúüʼ–-])")
	//Pattern PATTERN_U_DIAERESIS_C = RegexHelper.pattern("ü([^aeiouàèéíïòóúüʼ–-])")
	private static String normalizeDiaeresisBeforeConsonant(final String word){
		final int length = word.length();
		final StringBuilder sb = new StringBuilder(length);
		for(int i = 0; i < length; i ++){
			final char c = word.charAt(i);

			if((c == 'ï' || c == 'ü') && i + 1 < length){
				final char next = word.charAt(i + 1);
				if(!WordVEC.isVowel(next) && next != 'ʼ' && next != '–' && next != '-'){
					//C is consonant → remove diaeresis
					sb.append(c == 'ï'? 'i': 'u');
					continue;
				}
			}
			sb.append(c);
		}
		return sb.toString();
	}

	//Pattern PATTERN_V_I_DIAERESIS = RegexHelper.pattern("([aeiouàèéíòóú])ï")
	//Pattern PATTERN_V_U_DIAERESIS = RegexHelper.pattern("([aeiouàèéíòóú])ü")
	private static String normalizeDiaeresisAfterVowel(final String word){
		final int length = word.length();
		final StringBuilder sb = new StringBuilder(length);
		for(int i = 0; i < length; i ++){
			final char c = word.charAt(i);

			if((c == 'ï' || c == 'ü') && i > 0 && WordVEC.isVowel(word.charAt(i - 1))){
				sb.append(c == 'ï'? 'i': 'u');
				continue;
			}
			sb.append(c);
		}
		return sb.toString();
	}

	//Pattern PATTERN_J_INTO_I = RegexHelper.pattern("^j(?=[^aeiouàèéíïòóúüh])")
	private static String jIntoI(final String word){
		final int length = word.length();
		if(length > 1 && word.charAt(0) == 'j'){
			final char next = word.charAt(1);
			if(!WordVEC.isVowel(next) && next != 'ï' && next != 'ü' && next != 'h')
				return "i" + word.substring(1);
		}
		return word;
	}

	//Pattern PATTERN_I_INITIAL_INTO_J = RegexHelper.pattern("^i(?=[aeiouàèéíïòóúü])")
	private static String iInitialIntoJ(final String word){
		final int length = word.length();
		return (length > 1 && word.charAt(0) == 'i' && WordVEC.isVowel(word.charAt(1))
			? "j" + word.substring(1)
			: word);
	}

	//Pattern PATTERN_LH_INITIAL_INTO_L = RegexHelper.pattern("^ƚ(?=[^ʼaeiouàèéíïòóúüjw])")
	private static String lhInitialIntoL(final String word){
		final int length = word.length();
		if(length > 1 && word.charAt(0) == 'ƚ'){
			final char next = word.charAt(1);
			if(!WordVEC.isVowelOrJW(next))
				return "l" + word.substring(1);
		}
		return word;
	}

	//Pattern PATTERN_LH_INSIDE_INTO_L = RegexHelper.pattern("([aeiouàèéíïòóúü])ƚ(?=[^aeiouàèéíïòóúüjw–-])|([^ ʼaeiouàèéíïòóúü–-])ƚ(?=[aeiouàèéíïòóúüjw])")
	/**
	 * Replace 'ƚ' with 'l' only in the two "inside" cases equivalent to:
	 *  ([vowel])ƚ(?=[^vowel jw – -]) | ([^ ʼ vowel – -])ƚ(?=[vowel jw])
	 *
	 * Edge cases:
	 * - If the word length is 1 and it's just 'ƚ', keep it as 'ƚ'.
	 * - We require real neighbors for both cases; if prev or next does not exist, we do NOT replace.
	 */
	private static String lhInsideIntoL(final String word){
		// If the word is just "ƚ", we must keep it as-is.
		if(word.length() == 1)
			return word;

		final int length = word.length();
		final StringBuilder sb = new StringBuilder(length);

		for(int i = 0; i < length; i ++){
			final char c = word.charAt(i);

			if(c == 'ƚ'){
				//fetch neighbors; if they don't exist, mark as '\0'
				final char prev = (i > 0 ? word.charAt(i - 1) : '\0');
				final char next = (i + 1 < length ? word.charAt(i + 1) : '\0');

				//we need real neighbors for the two cases to match the original regex intent
				final boolean hasPrev = (prev != '\0');
				final boolean hasNext = (next != '\0');

				//case1: ([vowel])ƚ(?=[^vowel jw – -])
				//	-> previous is vowel; next exists and is NOT vowel/j/w/–/-
				final boolean case1 = (hasPrev && WordVEC.isVowel(prev) && hasNext && !WordVEC.isVowelOrJW(next)
					&& next != '–' && next != '-');

				//case2: ([^ ʼ vowel – -])ƚ(?=[vowel jw])
				//	-> previous exists and is NOT space/apostrophe/vowel/–/-; next exists and IS vowel or j/w
				final boolean case2 = (hasPrev && prev != ' ' && prev != 'ʼ' && !WordVEC.isVowel(prev)
					&& prev != '–' && prev != '-' && hasNext && WordVEC.isVowelOrJW(next));

				if(case1 || case2){
					//replace 'ƚ' with plain 'l' only in these two internal contexts
					sb.append('l');
					continue;
				}
			}

			sb.append(c);
		}

		return sb.toString();
	}

	//Pattern PATTERN_X_INTO_S = RegexHelper.pattern(GraphemeVEC.GRAPHEME_X + "(?=[cfkpstŧ])")
	private static String xIntoS(final String word){
		final int length = word.length();
		final StringBuilder sb = new StringBuilder(length);
		for(int i = 0; i < length; i ++){
			final char c = word.charAt(i);

			sb.append(c == 'x' && i + 1 < length && WordVEC.isConsonantForXtoS(word.charAt(i + 1))? 's': c);
		}
		return sb.toString();
	}

	//Pattern PATTERN_S_INTO_X = RegexHelper.pattern(GraphemeVEC.GRAPHEME_S + "(?=([mnñbdgɉvrl]))")
	private static String sIntoX(final String word){
//		if(word.endsWith("sx"))
//			return word;

		final int length = word.length();
		final StringBuilder sb = new StringBuilder(length);
		for(int i = 0; i < length; i ++){
			final char c = word.charAt(i);

			sb.append(c == 's' && i + 1 < length && WordVEC.isConsonantForStoX(word.charAt(i + 1))? 'x': c);
		}
		return sb.toString();
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
		for(int i = 0, length = syllabes.size(); i < length; i ++){
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
