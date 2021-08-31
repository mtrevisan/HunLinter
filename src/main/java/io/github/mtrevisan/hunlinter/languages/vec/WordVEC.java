/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.IntStream;


public final class WordVEC{

	private static final String PIPE = "|";

	private static final String VOWELS_PLAIN = "aAeEiIïÏoOuUüÜ";
	private static final String VOWELS_STRESSED = "àÀéÉèÈíÍóÓòÒúÚ";
	private static final String VOWELS_UNSTRESSED = "aAeEeEiIoOoOuU";
	private static final String CONSONANTS = "bBcCdDđĐfFgGhHjJɉɈkKlLƚȽmMnNñÑpPrRsStTŧŦvVxX";

	private static final char[] VOWELS_PLAIN_ARRAY = VOWELS_PLAIN.toCharArray();
	private static final char[] VOWELS_STRESSED_ARRAY = VOWELS_STRESSED.toCharArray();
	private static final char[] VOWELS_EXTENDED_ARRAY = (VOWELS_PLAIN + VOWELS_STRESSED).toCharArray();
	public static final String VOWELS = "aAàÀeEéÉèÈiIíÍïÏoOóÓòÒuUúÚüÜ";
	private static final char[] CONSONANTS_ARRAY = CONSONANTS.toCharArray();
	static{
		Arrays.sort(VOWELS_PLAIN_ARRAY);
		Arrays.sort(VOWELS_STRESSED_ARRAY);
		Arrays.sort(VOWELS_EXTENDED_ARRAY);
		Arrays.sort(CONSONANTS_ARRAY);
	}

	private static final String COLLATOR_RULE = "; ̿  < ' '='\t' < '-';\u00AD;‐;‑;‒;–;—;―;− < ’=''' < '/' < 0 < 1 < 2 < 3 < 4 < 5 < 6 < 7 < 8 < 9 < a,A < à,À < b,B < c,C < d,D < đ=dh,Đ=Dh < e,E < é,É < è,È < f,F < g,G < h,H < i,I < ï,Ï < í,Í < j,J < ɉ=jh,Ɉ=Jh < k,K < l,L < ƚ=lh,Ƚ=Lh < m,M < n,N < ñ=nh,Ñ=Nh < o,O < ó,Ó < ò,Ò < p,P < r,R < s,S < t,T < ŧ=th,Ŧ=Th < u,U < ü,Ü < ú,Ú < v,V < x,X";
	private static Collator COLLATOR;
	static{
		try{
			COLLATOR = new RuleBasedCollator(COLLATOR_RULE);
		}
		catch(final ParseException ignored){}
	}

	private static final Pattern DEFAULT_STRESS_GROUP = RegexHelper.pattern("^(?:(?:de)?fr|(?:ma|ko|x)?[lƚ]|n|apl|(?:in|re)st)au(?![^aeiou][aeiou].|tj?[aeèi].|fra)");

	//https://www.rexegg.com/regex-optimizations.html
	private static final String NO_STRESS_AVER = "^(?:re)?(?:"
		+ "g(?:à(?:b[ei]|(?:[bp]i[ae]?|[gmv]e|s(?:tu|e)|t[euo]||[lƚ][aeio]|ne?))|é(?:mo|(?:b(?:ia|e)|de)))"
		+ "|"
		+ "à(?:b(?:[ei]|i[ae]?)|[gmv]e|s(?:tu|e)|t[euo]|[lƚ][aeio]|ne?|pi[ae]?)"
		+ "|"
		+ "é(?:b(?:ia|e)|de)"
		+ ")$";
	private static final String NO_STRESS_ESER = "^(?:r[ei])?(?:" +
		"s(?:er(?:à[lƚ][aeio]|é(?:stu|t[ou]))|aré(?:stu|t[ou])|ión(?:[ei]|[jɉ]o|mi|t[ei])|ó(?:[jɉ]o|n(?:[ei]|[jɉ]o|mi|t[ei]))|é(?:[lƚ][aeio]|n[ei]|stu|t[ou])|í(?:stu|t[ou]))"
		+ "|"
		+ "xé(?:[lƚ][aeio]|stu|t[ou])"
		+ "|"
		+ "é(?:[lƚ][aeio]|stu|t[ou]|r[aei])"
		+ ")$";
	private static final String NO_STRESS_DAR_FAR_STAR = "^(?:"
			+ "(?:dex|re)?d"
			+ "|"
			+ "(?:asue|de(s|xasue)|kon(?:tra)?|[lƚ]iku[ei]|mal|putre|rare|re|s(?:a(?:t[iu]s|stu)|o(?:[dt]is|ra|sti)|t(?:[ou]pe|ra))|t(?:(?:or|um)e))?f"
			+ "|"
			+ "(?:m(al|ove)|soto)?st"
		+ ")à(?:g[oaie]?|[lƚ][oaie])?$";
	private static final String NO_STRESS_SAVER = "^(?:pre|re|stra)?sà(?:[gmstv]e|[lƚ][oiea]|ne?|stu|t[ou])$";
	private static final String NO_STRESS_ANDAR = "^(?:re)?và(?:[lƚ][oiea]|[mstv]e|ne?|stu|t[ou]|g(?:[oea]|io?))$";
	private static final String NO_STRESS_TRAER = "^(?:as?|des?|es|kon|pro|re|s(?:o|ub?))?trà(?:[gmstv]e|ne?|r)$";
	private static final Pattern PREVENT_UNMARK_STRESS;
	static{
		final StringJoiner sj = (new StringJoiner(PIPE))
			.add(NO_STRESS_AVER)
			.add(NO_STRESS_ESER)
			.add(NO_STRESS_DAR_FAR_STAR)
			.add(NO_STRESS_SAVER)
			.add(NO_STRESS_ANDAR)
			.add(NO_STRESS_TRAER);
		PREVENT_UNMARK_STRESS = RegexHelper.pattern(sj.toString());
	}

	//NOTE: must be sorted!
	private static final char[] SIMPLE_VOWELS_ARRAY = "AEIOUaeiouÏÜïü".toCharArray();
	//NOTE: must be sorted!
	private static final char[] ACUTE_STRESSED_VOWELS_ARRAY = "ÀÉÍÓÚàéíóúÍÚíú".toCharArray();


	private WordVEC(){}

	public static int countGraphemes(final String word){
		return (int)IntStream.range(0, word.length())
			.mapToObj(word::charAt)
			.filter(chr -> Arrays.binarySearch(VOWELS_EXTENDED_ARRAY, chr) >= 0 || Arrays.binarySearch(CONSONANTS_ARRAY, chr) >= 0)
			.count();
	}

	public static boolean isApostrophe(final char chr){
		return (chr == HyphenationParser.MODIFIER_LETTER_APOSTROPHE || chr == HyphenationParser.APOSTROPHE.charAt(0));
	}

	public static boolean isVowel(final char chr){
		return (Arrays.binarySearch(VOWELS_EXTENDED_ARRAY, chr) >= 0);
	}

	public static boolean isConsonant(final char chr){
		return (Arrays.binarySearch(CONSONANTS_ARRAY, chr) >= 0);
	}

	//^[‘’']?[aeiouàèéíòóú]
	public static boolean startsWithVowel(final CharSequence word){
		char chr = word.charAt(0);
		if(isApostrophe(chr))
			chr = word.charAt(1);
		return isVowel(chr);
	}

	//[aeiouàèéíòóú][^aàbcdđeéèfghiíjɉklƚmnñoóòprsʃtŧuúvxʒ]*[‘’]?$
	public static boolean endsWithVowel(final CharSequence word){
		final int idx = word.length() - 1;
		char chr = word.charAt(idx);
		if(isApostrophe(chr))
			chr = word.charAt(idx - 1);
		return isVowel(chr);
	}

	//[aeiou][^aeiou]*$
	private static int getLastUnstressedVowelIndex(final CharSequence word, int lastLetterIndex){
		for(lastLetterIndex --; lastLetterIndex >= 0; lastLetterIndex --){
			final char chr = word.charAt(lastLetterIndex);
			if(Arrays.binarySearch(VOWELS_PLAIN_ARRAY, chr) >= 0)
				break;
		}
		return lastLetterIndex;
	}


	//[àèéíòóú]
	public static boolean hasStressedGrapheme(final CharSequence word){
		return (countStresses(word) == 1);
	}

	public static int countStresses(final CharSequence word){
		return (int)IntStream.range(0, word.length())
			.filter(i -> Arrays.binarySearch(VOWELS_STRESSED_ARRAY, word.charAt(i)) >= 0)
			.count();
	}

	private static String suppressStress(final String word){
		return StringUtils.replaceChars(word, VOWELS_STRESSED, VOWELS_UNSTRESSED);
	}


	private static String setAcuteStressAtIndex(final String word, final int idx){
		final StringBuilder sb = new StringBuilder(word);
		sb.setCharAt(idx, addStressAcute(word.charAt(idx)));
		return sb.toString();
	}

	//NOTE: is seems faster the current method (above)
//	private static String setAcuteStressAtIndex(final String word, final int idx){
//		return replaceCharAt(word, idx, addStressAcute(word.charAt(idx)));
//	}

	static String replaceCharAt(final String text, final int idx, final char chr){
		final StringBuilder sb = new StringBuilder(text);
		sb.setCharAt(idx, chr);
		return sb.toString();
	}

	private static char addStressAcute(final char chr){
		final int stressedIndex = Arrays.binarySearch(SIMPLE_VOWELS_ARRAY, chr);
		return ACUTE_STRESSED_VOWELS_ARRAY[stressedIndex];
	}

	public static String markDefaultStress(String word){
		int stressIndex = getIndexOfStress(word);
		if(stressIndex < 0){
			final String phones = GraphemeVEC.handleJHJWIUmlautPhonemes(word);
			final int lastChar = getLastUnstressedVowelIndex(phones, phones.length());

			//last vowel if the word ends with consonant, penultimate otherwise
			//default to the second vowel of a group of two (first one on a monosyllabe)
			if(endsWithVowel(phones))
				stressIndex = getLastUnstressedVowelIndex(phones, lastChar);
			if(stressIndex >= 0 && RegexHelper.find(phones, DEFAULT_STRESS_GROUP))
				stressIndex --;
			else if(stressIndex < 0)
				stressIndex = lastChar;

			if(stressIndex >= 0)
				word = setAcuteStressAtIndex(word, stressIndex);
		}
		return word;
	}

	public static String unmarkDefaultStress(String word){
		final int stressIndex = getIndexOfStress(word);
		//check if the word have a stress, not on the last letter, not followed by an en dash
		final int wordLength = word.length();
		if(stressIndex >= 0
				//filter out stress on last vowel of the word
				&& stressIndex + 1 < wordLength
				&& word.charAt(stressIndex + 1) != HyphenationParser.MINUS_SIGN.charAt(0)
				&& word.charAt(stressIndex + 1) != HyphenationParser.MODIFIER_LETTER_APOSTROPHE
				&& !GraphemeVEC.isDiphtong(word)
				&& !GraphemeVEC.isHyatus(word)
				&& !RegexHelper.find(word, PREVENT_UNMARK_STRESS)){
			final String tmp = suppressStress(word);
			if(!tmp.equals(word) && markDefaultStress(tmp).equals(word))
				word = tmp;
		}
		return word;
	}

	static int getIndexOfStress(final CharSequence word){
		return StringUtils.indexOfAny(word, VOWELS_STRESSED_ARRAY);
	}

	public static Comparator<String> sorterComparator(){
		return COLLATOR::compare;
	}

}
