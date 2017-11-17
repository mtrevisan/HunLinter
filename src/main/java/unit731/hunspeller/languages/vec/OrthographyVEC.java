package unit731.hunspeller.languages.vec;

import java.util.List;
import java.util.regex.Matcher;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.PatternService;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrthographyVEC extends Orthography{

	private static final String APOSTROPHE2 = "'";

	private static final String A_GRAVE = "a\\";
	private static final String E_GRAVE = "e\\";
	private static final String O_GRAVE = "o\\";
	private static final String E_ACUTE = "e/";
	private static final String I_ACUTE = "i/";
	private static final String I_ACUTE_WRONG_ACCENT = "ì";
	private static final String O_ACUTE = "o/";
	private static final String U_ACUTE = "u/";
	private static final String U_ACUTE_WRONG_ACCENT = "ù";

	private static final Matcher REGEX_H = PatternService.matcher("([^adeèfhijklmnoòpstux])h");
	private static final String DH = "dh";
	private static final String JH = "jh";
	private static final String LH = "lh";
	private static final String NH = "nh";
	private static final String TH = "th";

	private static final String MB = "mb";
	private static final String MP = "mp";
	private static final Matcher REGEX_J_INTO_I = PatternService.matcher("^" + Grapheme.JJH_PHONEME + "(?=[^aeiouàèéíòóúh])");
	private static final Matcher REGEX_I_INITIAL_INTO_J = PatternService.matcher("^i(?=[aeiouàèéíòóú])");
	private static final Matcher REGEX_I_INSIDE_INTO_J = PatternService.matcher("([aeiouàèéíòóú])i(?=[aeiouàèéíòóú])");
	private static final Matcher REGEX_LH_INITIAL_INTO_L = PatternService.matcher("^ƚ(?=[^ʼ'aeiouàèéíòóújw])");
	private static final Matcher REGEX_LH_INSIDE_INTO_L = PatternService.matcher("([^ʼ'aeiouàèéíòóú-])ƚ(?=[aeiouàèéíòóújw])|([aeiouàèéíòóú])ƚ(?=[^aeiouàèéíòóújw])");
	private static final Matcher REGEX_FH_INTO_F = PatternService.matcher("fh(?=[^aeiouàèéíòóú])");
	private static final Matcher REGEX_X_INTO_S = PatternService.matcher("x(?=[cfkpt])");
	private static final Matcher REGEX_S_INTO_X = PatternService.matcher("s(?=([mnñbdg" + Grapheme.JJH_PHONEME + "ɉsvrlŧ]))");

	private static final Matcher REGEX_MORPHOLOGICAL = PatternService.matcher("([c" + Grapheme.JJH_PHONEME + "ñ])i([aeiou])");

	private static final Matcher REGEX_CONSONANT_GEMINATES = PatternService.matcher("([^aeiou]){1}\\1+");

	private static class SingletonHelper{
		private static final Orthography INSTANCE = new OrthographyVEC();
	}


	public static synchronized Orthography getInstance(){
		return SingletonHelper.INSTANCE;
	}

	@Override
	public String correctOrthography(String word){
		word = correctStress(word);

		//correct h occurrences not after d, f, k, j, l, n, p, s, t, x
		word = PatternService.replaceAll(word, REGEX_H, "$1");
		word = rewriteCharacters(word);

		//correct mb/mp occurrences into nb/np
		word = StringUtils.replace(word, MB, "nb");
		word = StringUtils.replace(word, MP, "np");

		word = Grapheme.handleJHJWPhonemes(word);

		//correct i occurrences into j at the beginning of a word followed by a vowel and between vowels, correcting also the converse
		word = PatternService.replaceFirst(word, REGEX_J_INTO_I, "i");
		word = PatternService.replaceFirst(word, REGEX_I_INITIAL_INTO_J, Grapheme.JJH_PHONEME);
		word = PatternService.replaceAll(word, REGEX_I_INSIDE_INTO_J, "$1" + Grapheme.JJH_PHONEME);
		//correct lh occurrences into l not at the beginning of a word and not between vowels
		word = PatternService.replaceFirst(word, REGEX_LH_INITIAL_INTO_L, "l");
		word = PatternService.replaceAll(word, REGEX_LH_INSIDE_INTO_L, "$1l");
		//correct fh occurrences into f not before vowel
		word = PatternService.replaceAll(word, REGEX_FH_INTO_F, "f");
		//correct x occurrences into s prior to c, f, k, p, t
		//correct s occurrences into x prior to m, n, ñ, b, d, g, j, ɉ, s, v, r, l
		word = PatternService.replaceAll(word, REGEX_X_INTO_S, "s");
		word = PatternService.replaceAll(word, REGEX_S_INTO_X, "x");

		//correct morphological errors
		word = PatternService.replaceAll(word, REGEX_MORPHOLOGICAL, "$1$2");

		word = Grapheme.rollbackJHJWPhonemes(word);

		word = eliminateConsonantGeminates(word);

		word = correctApostrophes(word);

		return word;
	}

	private String correctStress(String word){
		word = StringUtils.replace(word, A_GRAVE, "à");
		word = StringUtils.replace(word, E_GRAVE, "è");
		word = StringUtils.replace(word, O_GRAVE, "ò");
		word = StringUtils.replace(word, E_ACUTE, "é");
		word = StringUtils.replace(word, I_ACUTE, "í");
		word = StringUtils.replace(word, I_ACUTE_WRONG_ACCENT, "í");
		word = StringUtils.replace(word, O_ACUTE, "ó");
		word = StringUtils.replace(word, U_ACUTE, "ú");
		word = StringUtils.replace(word, U_ACUTE_WRONG_ACCENT, "ú");
		return word;
	}

	private String rewriteCharacters(String word){
		word = StringUtils.replace(word, DH, "đ");
		word = StringUtils.replace(word, JH, "ɉ");
		word = StringUtils.replace(word, LH, "ƚ");
		word = StringUtils.replace(word, NH, "ñ");
		word = StringUtils.replace(word, TH, "ŧ");
		return word;
	}

	private String eliminateConsonantGeminates(String word){
		String prefix = null;
		boolean prefixRemoved = false;
		if(word.startsWith("dexskon")){
			word = word.substring(3);

			prefix = "dex";
			prefixRemoved = true;
		}

		word = PatternService.replaceAll(word, REGEX_CONSONANT_GEMINATES, "$1");
		
		if(prefixRemoved)
			word = prefix + word;

		return word;
	}

	@Override
	public boolean[] getSyllabationErrors(List<String> syllabes){
		int size = syllabes.size();
		boolean[] errors = new boolean[size];
		for(int i = 0; i < size; i ++){
			String syllabe = syllabes.get(i);
			errors[i] = (!syllabe.contains(APOSTROPHE) && !syllabe.contains(APOSTROPHE2) && !HyphenationParser.HYPHEN.equals(syllabe)
				&& Word.getLastVowelIndex(syllabe) < 0);
		}
		return errors;
	}

}
