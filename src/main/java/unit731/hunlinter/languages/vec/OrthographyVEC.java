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
import unit731.hunlinter.languages.Orthography;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;
import unit731.hunlinter.services.RegexHelper;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;


public final class OrthographyVEC extends Orthography{

	private static final String[] STRESS_CODES = {"a\\", "e\\", "o\\", "e/", "i/", "i\\", "ì", "i:", "o/", "u/", "u\\", "ù", "u:"};
	private static final String[] TRUE_STRESS = {"à", "è", "ò", "é", "í", "í", "í", "ï", "ó", "ú", "ú", "ú", "ü"};

	private static final String[] EXTENDED_CHARS = {"dh", "jh", "lh", "nh", "th"};
	private static final String[] TRUE_CHARS = {"đ", "ɉ", "ƚ", "ñ", "ŧ"};

	private static final String[] MB_MP = {"mb", "mp"};
	private static final String[] NB_NP = {"nb", "np"};

	//here `ï` and `ü` are really consonants, but are treated as vowels, in order for `argüïo` to be valid
	private static final Pattern PATTERN_IUMLAUT_C = RegexHelper.pattern("ï([^aeiïouàéèíóòúü])");
	private static final Pattern PATTERN_UUMLAUT_C = RegexHelper.pattern("ü([^aeiïouàéèíóòúü])");

	private static final Pattern PATTERN_REMOVE_H_FROM_NOT_FH = RegexHelper.pattern("(?<!f)h(?!aeeioouàéèíóòú)");

	private static final Pattern PATTERN_J_INTO_I = RegexHelper.pattern("^" + GraphemeVEC.PHONEME_JJH + "(?=[^aeiouàèéíïòóúüh])");
	private static final Pattern PATTERN_I_INITIAL_INTO_J = RegexHelper.pattern("^i(?=[aeiouàèéíïòóúü])");
	private static final Pattern PATTERN_I_INSIDE_INTO_J = RegexHelper.pattern("([aeiouàèéíïòóúü])i(?=[aeiouàèéíïòóúü])");
	private static final List<Pattern> PATTERN_I_INSIDE_INTO_J_FALSE_POSITIVES = Arrays.asList(
		RegexHelper.pattern("[nv][ou]ialtri"),
		RegexHelper.pattern("b[ae]ro[iï][aeèi]r"),
		RegexHelper.pattern("re[sŧ]e[iï][ouü]r[aeio]?")
	);
	private static final Pattern PATTERN_I_INSIDE_INTO_J_EXCLUSIONS = RegexHelper.pattern("[aeiouàèéíïòóúü]i(?:o|(?:[oó]n|on-)(?:[gmnstv].{1,3}|[ei])?(?:[lƚ][oiae])?|é(?:-?[ou])?|e[dg]e(?:-[ou])?|omi|ent[eoi]?-?(?:[gmnstv].{1,3})?(?:[lƚ][oiae])?|inti)$");
	private static final Pattern PATTERN_LH_INITIAL_INTO_L = RegexHelper.pattern("^ƚ(?=[^ʼaeiouàèéíïòóúüjw])");
	private static final Pattern PATTERN_LH_INSIDE_INTO_L = RegexHelper.pattern("([aeiouàèéíïòóúü])ƚ(?=[^aeiouàèéíïòóúüjw])|([^ʼaeiouàèéíïòóúü–-])ƚ(?=[aeiouàèéíïòóúüjw])");
	private static final Pattern PATTERN_X_INTO_S = RegexHelper.pattern(GraphemeVEC.GRAPHEME_X + "(?=[cfkpt])");
	private static final Pattern PATTERN_S_INTO_X = RegexHelper.pattern(GraphemeVEC.GRAPHEME_S + "(?=([mnñbdg" + GraphemeVEC.PHONEME_JJH
		+ "ɉsvrlŧ]))");
	private static final String FALSE_S_INTO_X = "èsre";

	private static final Pattern PATTERN_MORPHOLOGICAL = RegexHelper.pattern("([c" + GraphemeVEC.PHONEME_JJH + "ñ])i([aeiou])");

	//a double consonant, not ^inn, not [eo]nne$
	private static final Pattern PATTERN_CONSONANT_GEMINATES = RegexHelper.pattern("([^aeiou])\\1+");
	private static final Function<String, String> GEMINATES_REDUCER = word -> {
		String strippedWord = word;
		String starting = "";
		if(strippedWord.startsWith("inn")){
			strippedWord = strippedWord.substring(3);
			starting = "inn";
		}
		String ending = "";
		if(strippedWord.endsWith("onne")){
			strippedWord = strippedWord.substring(0, strippedWord.length() - 4);
			ending = "onne";
		}
		else if(strippedWord.endsWith("enne")){
			strippedWord = strippedWord.substring(0, strippedWord.length() - 4);
			ending = "enne";
		}
		strippedWord = RegexHelper.replaceAll(strippedWord, PATTERN_CONSONANT_GEMINATES, "$1");
		return starting + strippedWord + ending;
	};

	private static class SingletonHelper{
		private static final Orthography INSTANCE = new OrthographyVEC();
	}


	private OrthographyVEC(){}

	public static Orthography getInstance(){
		return SingletonHelper.INSTANCE;
	}

	@Override
	public String correctOrthography(String word){
		word = WordVEC.unmarkDefaultStress(word);

		//correct stress
		String correctedWord = StringUtils.replaceEach(word, STRESS_CODES, TRUE_STRESS);

		//correct h occurrences after d, j, l, n, t
		correctedWord = StringUtils.replaceEach(correctedWord, EXTENDED_CHARS, TRUE_CHARS);

		//remove other occurrences of h not into fhV
		if(!GraphemeVEC.GRAPHEME_H.equals(correctedWord))
			correctedWord = RegexHelper.replaceAll(correctedWord, PATTERN_REMOVE_H_FROM_NOT_FH, StringUtils.EMPTY);

		//correct mb/mp occurrences into nb/np
		correctedWord = StringUtils.replaceEach(correctedWord, MB_MP, NB_NP);

		//correct ïC/üC occurrences into iC/uC
		correctedWord = RegexHelper.replaceAll(correctedWord, PATTERN_IUMLAUT_C, "i$1");
		correctedWord = RegexHelper.replaceAll(correctedWord, PATTERN_UUMLAUT_C, "u$1");

		correctedWord = GraphemeVEC.handleJHJWIUmlautPhonemes(correctedWord);

		correctedWord = correctIJOccurrences(correctedWord);

		//correct lh occurrences into l not at the beginning of a word and not between vowels
		correctedWord = RegexHelper.replaceAll(correctedWord, PATTERN_LH_INITIAL_INTO_L, GraphemeVEC.GRAPHEME_L);
		correctedWord = RegexHelper.replaceAll(correctedWord, PATTERN_LH_INSIDE_INTO_L, "$1l");
		//correct x occurrences into s prior to c, f, k, p, t
		//correct s occurrences into x prior to m, n, ñ, b, d, g, j, ɉ, s, v, r, l
		correctedWord = RegexHelper.replaceAll(correctedWord, PATTERN_X_INTO_S, GraphemeVEC.GRAPHEME_S);
		if(!correctedWord.endsWith(FALSE_S_INTO_X))
			correctedWord = RegexHelper.replaceAll(correctedWord, PATTERN_S_INTO_X, GraphemeVEC.GRAPHEME_X);

		//correct morphological errors
		correctedWord = RegexHelper.replaceAll(correctedWord, PATTERN_MORPHOLOGICAL, "$1$2");

		correctedWord = GraphemeVEC.rollbackJHJWIUmlautPhonemes(correctedWord);

		//eliminate consonant geminates
		correctedWord = GEMINATES_REDUCER.apply(correctedWord);

		return correctedWord;
	}

	private String correctIJOccurrences(String word){
		//correct i occurrences into j at the beginning of a word followed by a vowel and between vowels,
		//correcting also the converse
		word = RegexHelper.replaceAll(word, PATTERN_J_INTO_I, GraphemeVEC.GRAPHEME_I);
		word = RegexHelper.replaceAll(word, PATTERN_I_INITIAL_INTO_J, GraphemeVEC.PHONEME_JJH);
		boolean iInsideIntoJFalsePositive = false;
		for(final Pattern p : PATTERN_I_INSIDE_INTO_J_FALSE_POSITIVES)
			if(RegexHelper.find(word, p)){
				iInsideIntoJFalsePositive = true;
				break;
			}
		if(!iInsideIntoJFalsePositive && !RegexHelper.find(word, PATTERN_I_INSIDE_INTO_J_EXCLUSIONS))
			word = RegexHelper.replaceAll(word, PATTERN_I_INSIDE_INTO_J, "$1" + GraphemeVEC.PHONEME_JJH);
		return word;
	}

	@Override
	public boolean[] getSyllabationErrors(final String[] syllabes){
		final boolean[] errors = new boolean[syllabes.length];
		for(int i = 0; i < syllabes.length; i ++){
			final String syllabe = syllabes[i];
			errors[i] = (!syllabe.contains(HyphenationParser.APOSTROPHE)
				&& !StringUtils.contains(syllabe, HyphenationParser.MODIFIER_LETTER_APOSTROPHE)
				&& !syllabe.equals(HyphenationParser.MINUS_SIGN) && !StringUtils.containsAny(syllabe, WordVEC.VOWELS));
		}
		return errors;
	}

	@Override
	public int getStressedSyllabeIndexFromLast(final String[] syllabes){
		for(int i = syllabes.length - 1; i >= 0; i --)
			if(hasStressedGrapheme(syllabes[i]))
				return i;
		return -1;
	}

	@Override
	public int countGraphemes(final String word){
		return WordVEC.countGraphemes(word);
	}

	@Override
	public String markDefaultStress(final String word){
		return WordVEC.markDefaultStress(word);
	}

	@Override
	public boolean hasStressedGrapheme(final String word){
		return WordVEC.hasStressedGrapheme(word);
	}

}
