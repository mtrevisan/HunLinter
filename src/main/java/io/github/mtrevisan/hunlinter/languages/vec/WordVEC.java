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

import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenationParser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.StringJoiner;


public final class WordVEC{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordVEC.class);

	private static final String VOWELS_PLAIN = "aAeEiIïÏoOuUüÜ";
	private static final String VOWELS_STRESSED = "àÀéÉèÈíÍóÓòÒúÚ";
	private static final String VOWELS_UNSTRESSED = "aAeEeEiIoOoOuU";
	private static final String CONSONANTS = "bBcCdDđĐfFgGhHjJɉɈkKlLƚȽmMnNñÑpPrRsStTŧŦvVxX";

	private static final char[] VOWELS_PLAIN_ARRAY = "aAeEiIoOuU".toCharArray();
	private static final char[] VOWELS_UNNECESSARY_STRESSED = "aAàÀeEéÉoOóÓ".toCharArray();
	private static final char[] VOWELS_UNNECESSARY_STRESS = "aAeEoO".toCharArray();
	private static final char[] SURE_VOWELS_GRAVE_STRESSED = "èÈòÒ".toCharArray();
	private static final char[] VOWELS_PLAIN_ARRAY2 = "aAeEoOíÍïÏúÚüÜ".toCharArray();
	private static final char[] VOWELS_NOT_UMLAUT_ARRAY = "iIuU".toCharArray();
	private static final char[] VOWELS_IU_ARRAY = "iIuUíÍúÚïÏüÜ".toCharArray();
	private static final char[] VOWELS_UMLAUT_ARRAY = "ïÏüÜ".toCharArray();
	private static final char[] VOWELS_STRESSED_ARRAY = VOWELS_STRESSED.toCharArray();
	private static final char[] VOWELS_UNSTRESSED_ARRAY = VOWELS_UNSTRESSED.toCharArray();
	private static final char[] VOWELS_EXTENDED_ARRAY = (VOWELS_PLAIN + VOWELS_STRESSED).toCharArray();
	static final char[] VOWELS_ARRAY = "aAàÀeEéÉèÈiIíÍïÏoOóÓòÒuUúÚüÜ".toCharArray();
	private static final char[] CONSONANTS_ARRAY = CONSONANTS.toCharArray();
	static{
		Arrays.sort(VOWELS_PLAIN_ARRAY);
		Arrays.sort(VOWELS_UNNECESSARY_STRESSED);
		Arrays.sort(VOWELS_UNNECESSARY_STRESS);
		Arrays.sort(SURE_VOWELS_GRAVE_STRESSED);
		Arrays.sort(VOWELS_PLAIN_ARRAY2);
		Arrays.sort(VOWELS_NOT_UMLAUT_ARRAY);
		Arrays.sort(VOWELS_IU_ARRAY);
		Arrays.sort(VOWELS_UMLAUT_ARRAY);
		Arrays.sort(VOWELS_STRESSED_ARRAY);
		Arrays.sort(VOWELS_UNSTRESSED_ARRAY);
		Arrays.sort(VOWELS_EXTENDED_ARRAY);
		Arrays.sort(VOWELS_ARRAY);
		Arrays.sort(CONSONANTS_ARRAY);
	}

	//NOTE: any character that appears before the first `<` is considered an ignorable character
	private static final String COLLATOR_RULE = "; '-','’' < ' '='\t' < \u00AD;‐;‑;‒;–;—;―;− < ’=''' < '/' < 0 < 1 < 2 < 3 < 4 < 5 < 6 < 7 < 8 < 9 < a,A < à,À < b,B < c,C < d,D < đ=dh,Đ=Dh < e,E < é,É < è,È < f,F < g,G < h,H < i,I < ï,Ï < í,Í < j,J < ɉ=jh,Ɉ=Jh < k,K < l,L < ƚ=lh,Ƚ=Lh < m,M < n,N < ñ=nh,Ñ=Nh < o,O < ó,Ó < ò,Ò < p,P < r,R < s,S < t,T < ŧ=th,Ŧ=Th < u,U < ü,Ü < ú,Ú < v,V < x,X";
	@SuppressWarnings("NonConstantFieldWithUpperCaseName")
	private static Collator COLLATOR;
	static{
		try{
			COLLATOR = new RuleBasedCollator(COLLATOR_RULE);
		}
		catch(final ParseException e){
			final Locale fallbackLocale = Locale.ITALIAN;
			LOGGER.error("Bad error while creating the collator, use {} as default", fallbackLocale.getLanguage(), e);

			COLLATOR = Collator.getInstance(fallbackLocale);
		}
	}

	//NOTE: must be sorted!
	private static final char[] SIMPLE_VOWELS_ARRAY = "AEIOUaeiouÏÜïü".toCharArray();
	//NOTE: must be sorted!
	private static final char[] ACUTE_STRESSED_VOWELS_ARRAY = "ÀÉÍÓÚàéíóúÍÚíú".toCharArray();


	private WordVEC(){}

	/**
	 * Counts the number of graphemes in a given word.
	 *
	 * @param word	The word to count the graphemes in.
	 * @return	The number of graphemes in the word.
	 */
	public static int countGraphemes(final CharSequence word){
		int count = 0;
		for(int i = 0; i < word.length(); i ++){
			final char chr = word.charAt(i);
			if(Arrays.binarySearch(VOWELS_EXTENDED_ARRAY, chr) >= 0 || Arrays.binarySearch(CONSONANTS_ARRAY, chr) >= 0)
				count ++;
		}
		return count;
	}

	public static boolean isApostrophe(final char chr){
		return (chr == HyphenationParser.MODIFIER_LETTER_APOSTROPHE || chr == HyphenationParser.APOSTROPHE.charAt(0));
	}

	public static boolean isVowel(final char chr){
		return (Arrays.binarySearch(VOWELS_EXTENDED_ARRAY, chr) >= 0);
	}

	public static boolean isConsonant(final char chr){
		return !isVowel(chr);
	}

	/**
	 * Determines if a given word starts with a vowel (e.g. /^[’']?[aeiouàèéíòóú]/).
	 *
	 * @param word	The word to check.
	 * @return	Whether the word starts with a vowel.
	 */
	public static boolean startsWithVowel(final CharSequence word){
		char chr = word.charAt(0);
		if(isApostrophe(chr))
			chr = word.charAt(1);
		return isVowel(chr);
	}

	/**
	 * Determines if a given word ends with a vowel (e.g. /[aeiouàèéíòóú][^aàbcdđeéèfghiíjɉklƚmnñoóòprsʃtŧuúvxʒ]*’?$/).
	 *
	 * @param word	The word to check.
	 * @return	Whether the word ends with a vowel.
	 */
	public static boolean endsWithVowel(final CharSequence word){
		final int idx = word.length() - 1;
		char chr = word.charAt(idx);
		if(isApostrophe(chr))
			chr = word.charAt(idx - 1);
		return isVowel(chr);
	}

	/**
	 * Determines the index of the last unstressed vowel in the given word, up to the specified last letter index (e.g. /[aeiou][^aeiou]*$/).
	 *
	 * @param word	The word to search for an unstressed vowel.
	 * @param lastLetterIndex	The index of the last letter up to which to search for an unstressed vowel.
	 * @return	The index of the last unstressed vowel, or -1 if no unstressed vowel is found.
	 */
	private static int getLastUnstressedVowelIndex(final CharSequence word, int lastLetterIndex){
		for(lastLetterIndex --; lastLetterIndex >= 0; lastLetterIndex --){
			final char chr = word.charAt(lastLetterIndex);
			if(Arrays.binarySearch(VOWELS_PLAIN_ARRAY, chr) >= 0){
				if(chr != 'i' && chr != 'u')
					break;

				final int idx = (word.charAt(lastLetterIndex - 1) == '-'? lastLetterIndex - 2: lastLetterIndex - 1);
				if(idx < 0 || Arrays.binarySearch(VOWELS_UMLAUT_ARRAY, word.charAt(idx)) < 0)
					break;
			}
		}
		return lastLetterIndex;
	}

	/**
	 * Determines the index of the last unstressed vowel in the given word, up to the specified last letter index (e.g. /[aeiou][^aeiou]*$/).
	 *
	 * @param word	The word to search for an unstressed vowel.
	 * @param lastLetterIndex	The index of the last letter up to which to search for an unstressed vowel.
	 * @return	The index of the last unstressed vowel, or -1 if no unstressed vowel is found.
	 */
	private static int getLastUnstressedVowelIndex1(final CharSequence word, int lastLetterIndex){
		for(lastLetterIndex --; lastLetterIndex >= 0; lastLetterIndex --){
			final char chr = word.charAt(lastLetterIndex);
			if(Arrays.binarySearch(VOWELS_UNNECESSARY_STRESS, chr) >= 0)
				break;
		}
		return lastLetterIndex;
	}


	/**
	 * Determines if the given word contains a stressed grapheme (e.g. /[àèéíòóú]/).
	 *
	 * @param word	The word to check for a stressed grapheme.
	 * @return	Whether the word contains a stressed grapheme.
	 */
	public static boolean hasStressedGrapheme(final String word){
		return (countStresses(word) == 1);
	}

	/**
	 * Counts the number of stressed characters in a given word.
	 *
	 * @param word	The word to count the stresses in.
	 * @return	The number of stressed characters in the word.
	 */
	public static int countStresses(final String word){
		int count = 0;
		final char[] chars = word.toCharArray();
		for(int i = 0; i < chars.length; i ++)
			if(Arrays.binarySearch(VOWELS_STRESSED_ARRAY, chars[i]) >= 0)
				count ++;
		return count;
	}

	/**
	 * Suppresses stress in the given word by replacing stressed vowels with unstressed vowels.
	 *
	 * @param word	The word to suppress stress in.
	 * @return	The word with stress suppressed.
	 */
	private static String suppressStress(final String word){
		final StringBuilder sb = new StringBuilder(word);
		final char[] chars = word.toCharArray();
		for(int i = 0; i < chars.length; i ++){
			final int index = Arrays.binarySearch(VOWELS_STRESSED_ARRAY, chars[i]);
			if(index >= 0)
				sb.setCharAt(i, VOWELS_UNSTRESSED_ARRAY[index]);
		}
		return sb.toString();
	}


	/**
	 * Resets the acute stress at a specific index in the given word, by replacing the stressed vowel
	 * with the corresponding unstressed vowel.
	 *
	 * @param word	The word to reset the acute stress in.
	 * @param idx	The index of the vowel with acute stress.
	 * @return	The word with the acute stress reset at the specified index.
	 */
	private static String resetAcuteStressAtIndex(final String word, final int idx){
		final StringBuilder sb = new StringBuilder(word);
		sb.setCharAt(idx, removeStressAcute(word.charAt(idx)));
		return sb.toString();
	}

	/**
	 * Resets the acute stress at a specific index in the given word, by replacing the stressed vowel
	 * with the corresponding unstressed vowel.
	 *
	 * @param word	The word to reset the acute stress in.
	 * @param idx	The index of the vowel with acute stress.
	 * @return	The word with the acute stress reset at the specified index.
	 */
	private static String setAcuteStressAtIndex(final String word, final int idx){
		final StringBuilder sb = new StringBuilder(word);
		sb.setCharAt(idx, addStressAcute(word.charAt(idx)));
		return sb.toString();
	}

	//NOTE: is seems faster the current method (above)
//	private static String setAcuteStressAtIndex(final String word, final int idx){
//		return replaceCharAt(word, idx, addStressAcute(word.charAt(idx)));
//	}

	/**
	 * Replaces a character at a specified index in the given text with a new character.
	 *
	 * @param text	The original text.
	 * @param idx	The index of the character to be replaced.
	 * @param chr	The new character to replace the original character.
	 * @return	The updated text with the character replaced.
	 */
	static String replaceCharAt(final String text, final int idx, final char chr){
		final StringBuilder sb = new StringBuilder(text);
		sb.setCharAt(idx, chr);
		return sb.toString();
	}

	/**
	 * Removes the acute stress from a given character.
	 *
	 * @param chr	The character to remove the acute stress from.
	 * @return	The character with the acute stress removed.
	 */
	private static char removeStressAcute(final char chr){
		final int stressedIndex = Arrays.binarySearch(ACUTE_STRESSED_VOWELS_ARRAY, chr);
		return (stressedIndex >= 0? SIMPLE_VOWELS_ARRAY[stressedIndex]: chr);
	}

	/**
	 * Adds an acute stress to the given character if it belongs to the SIMPLE_VOWELS_ARRAY.
	 *
	 * @param chr	The character to add the acute stress to.
	 * @return	The character with the acute stress added if it belongs to the SIMPLE_VOWELS_ARRAY, otherwise the original character.
	 */
	private static char addStressAcute(final char chr){
		final int stressedIndex = Arrays.binarySearch(SIMPLE_VOWELS_ARRAY, chr);
		return (stressedIndex >= 0? ACUTE_STRESSED_VOWELS_ARRAY[stressedIndex]: chr);
	}

	/**
	 * Marks the default stress in the given word.
	 *
	 * @param word	The word to mark the default stress in.
	 * @return	The word with the default stress marked.
	 */
	public static String markDefaultStress(final String word){
		if("–".equals(word))
			return word;

		char delimiter = ' ';
		final char[] chars = word.toCharArray();
		for(int i = 0; delimiter == ' ' && i < chars.length; i ++){
			final char chr = chars[i];
			if(chr == '-' || chr == '–')
				delimiter = chr;
		}

		if(delimiter != ' '){
			final StringJoiner sj = new StringJoiner(Character.toString(delimiter));
			int offset = 0;
			int subwordIndex;
			while((subwordIndex = word.indexOf(delimiter, offset)) >= 0){
				sj.add(innerMarkDefaultStress(word.substring(offset, subwordIndex)));
				offset = subwordIndex + 1;
			}
			if(offset < chars.length)
				sj.add(innerMarkDefaultStress(word.substring(offset)));
			return sj.toString();
		}

		return innerMarkDefaultStress(word);
	}

//	public static String innerMarkDefaultStress(String word){
//		int stressIndex = getIndexOfStress(word);
//		if(stressIndex < 0){
//			final int lastChar = getLastUnstressedVowelIndex(word, word.length());
//
//			//last vowel if the word ends with consonant, penultimate otherwise
//			//default to the second vowel of a group of two (first one on a monosyllabe)
//			if(endsWithVowel(word))
//				stressIndex = getLastUnstressedVowelIndex(word, lastChar);
//			if(stressIndex >= 0 && RegexHelper.find(word, DEFAULT_STRESS_GROUP))
//				stressIndex --;
//			else if(stressIndex < 0)
//				stressIndex = lastChar;
//
//			if(stressIndex >= 0)
//				word = setAcuteStressAtIndex(word, stressIndex);
//		}
//		return word;
//	}

	private static String innerMarkDefaultStress(String word){
		int higherIndex = StringUtils.lastIndexOf(word, '’') - 1;
		final int wordLength = word.length();
		if(higherIndex < 0)
			higherIndex = wordLength - 1;

		final int stressIndex = getIndexOfStress(word);
		//if last character is stressed, or the stress has to be present, then it's ok
		if(higherIndex == stressIndex || stressIndex >= 0 && Arrays.binarySearch(SURE_VOWELS_GRAVE_STRESSED, word.charAt(stressIndex)) >= 0)
			return word;

		final boolean closedLastSyllable = isConsonant(word.charAt(higherIndex));
		int vowelCount = (closedLastSyllable? 1: 2);
		for(; vowelCount > 0 && higherIndex >= 0; higherIndex --){
			final char currentChar = word.charAt(higherIndex);
			boolean vowel = isVowel(currentChar);
			if(vowel && Arrays.binarySearch(VOWELS_IU_ARRAY, currentChar) >= 0)
				vowel = isVenetanVowel(word, higherIndex, wordLength, currentChar);
			if(vowel)
				vowelCount --;
		}

		if(vowelCount == 0){
			higherIndex ++;

			if(stressIndex < 0 || higherIndex == stressIndex){
				//check that there are no other vowels before higherIndex
				boolean otherVowelsPresent = false;
				for(int i = higherIndex - 1; !otherVowelsPresent && i >= 0; i --){
					final char currentChar = word.charAt(i);
					otherVowelsPresent = isVowel(currentChar);
					if(otherVowelsPresent && Arrays.binarySearch(VOWELS_IU_ARRAY, currentChar) >= 0)
						otherVowelsPresent = isVenetanVowel(word, i, wordLength, currentChar);
				}

				if(otherVowelsPresent){
					final boolean stressOnPenultimateSyllabe = (higherIndex == wordLength - 2
						&& isConsonant(word.charAt(higherIndex - 1))
						&& isVowel(word.charAt(higherIndex + 1)));
					if(stressOnPenultimateSyllabe && Arrays.binarySearch(VOWELS_IU_ARRAY, word.charAt(higherIndex)) >= 0)
						return setAcuteStressAtIndex(word, higherIndex);
				}
				word = suppressStress(word);
			}
		}
		return word;
	}

	/**
	 * Determines if a character at a given index in a word is a Venetan vowel.
	 *
	 * @param word	The word to check.
	 * @param index	The index of the character in the word.
	 * @param wordLength	The length of the word.
	 * @param currentChar	The character at the specified index.
	 * @return	Whether the character is a Venetan vowel.
	 */
	//NOTE: `word[index]` must already be a vowel!
	private static boolean isVenetanVowel(final CharSequence word, final int index, final int wordLength, final char currentChar){
		boolean vowel = true;
		final boolean insideWord = (index > 1 && index + 1 < wordLength);
		if(insideWord){
			final char previousChar = word.charAt(index - 1);
			final char nextChar = word.charAt(index + 1);
			final boolean maybeConsonant = (Arrays.binarySearch(VOWELS_NOT_UMLAUT_ARRAY, currentChar) >= 0);
			vowel = !(isConsonant(previousChar) && maybeConsonant && isVowel(nextChar));
		}
		return vowel;
	}

	static int getIndexOfStress(final CharSequence word){
		return StringUtils.indexOfAny(word, VOWELS_STRESSED_ARRAY);
	}

	public static Comparator<String> sorterComparator(){
		return COLLATOR::compare;
	}

}
