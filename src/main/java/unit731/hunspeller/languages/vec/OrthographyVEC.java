package unit731.hunspeller.languages.vec;

import java.util.List;
import java.util.regex.Matcher;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.PatternService;


public class OrthographyVEC extends Orthography{

	private static final Matcher REGEX_A_GRAVE = PatternService.matcher("a\\\\");
	private static final Matcher REGEX_E_GRAVE = PatternService.matcher("e\\\\");
	private static final Matcher REGEX_O_GRAVE = PatternService.matcher("o\\\\");
	private static final Matcher REGEX_E_ACUTE = PatternService.matcher("e/");
	private static final Matcher REGEX_I_ACUTE = PatternService.matcher("(i/|ì)");
	private static final Matcher REGEX_O_ACUTE = PatternService.matcher("o/");
	private static final Matcher REGEX_U_ACUTE = PatternService.matcher("(u/|ù)");

	private static final Matcher REGEX_H = PatternService.matcher("(^|[^dfkjlnpstx])h");
	private static final Matcher REGEX_DH = PatternService.matcher("dh");
	private static final Matcher REGEX_JH = PatternService.matcher("jh");
	private static final Matcher REGEX_LH = PatternService.matcher("lh");
	private static final Matcher REGEX_NH = PatternService.matcher("nh");
	private static final Matcher REGEX_TH = PatternService.matcher("th");

	private static final Matcher REGEX_MB_MP = PatternService.matcher("m([bp])");
	private static final Matcher REGEX_J_INTO_I = PatternService.matcher("^j(?=[^aeiouàèéíòóúh])");
	private static final Matcher REGEX_I_INITIAL_INTO_J = PatternService.matcher("^i(?=[aeiouàèéíòóú])");
	private static final Matcher REGEX_I_INSIDE_INTO_J = PatternService.matcher("([aeiouàèéíòóú])i(?=[aeiouàèéíòóú])");
	private static final Matcher REGEX_LH_INITIAL_INTO_L = PatternService.matcher("^ƚ(?=[^ʼ'aeiouàèéíòóú])");
	private static final Matcher REGEX_LH_INSIDE_INTO_L = PatternService.matcher("([^ʼ'aeiouàèéíòóú])ƚ(?=[aeiouàèéíòóú])|([aeiouàèéíòóú])ƚ(?=[^aeiouàèéíòóú])");
	private static final Matcher REGEX_FH_INTO_F = PatternService.matcher("fh(?=[^aeiouàèéíòóú])");
	private static final Matcher REGEX_X_INTO_S = PatternService.matcher("x(?=[cfkpt])");
	private static final Matcher REGEX_S_INTO_X = PatternService.matcher("s(?=([mnñbdgjɉsvrlŧ]|jh))");

	private static final Matcher REGEX_MORPHOLOGICAL = PatternService.matcher("([cjñ])i([aeiou])");

	private static final Matcher REGEX_CONSONANT_GEMINATES = PatternService.matcher("([^aeiou]){1}\\1+");

	private static class SingletonHelper{
		private static final OrthographyVEC INSTANCE = new OrthographyVEC();
	}


	public static synchronized Orthography getInstance(){
		return SingletonHelper.INSTANCE;
	}

	@Override
	public String correctOrthography(String word){
		//apply stress
		word = PatternService.replaceAll(word, REGEX_A_GRAVE, "à");
		word = PatternService.replaceAll(word, REGEX_E_GRAVE, "è");
		word = PatternService.replaceAll(word, REGEX_O_GRAVE, "ò");
		word = PatternService.replaceAll(word, REGEX_E_ACUTE, "é");
		word = PatternService.replaceAll(word, REGEX_I_ACUTE, "í");
		word = PatternService.replaceAll(word, REGEX_O_ACUTE, "ó");
		word = PatternService.replaceAll(word, REGEX_U_ACUTE, "ú");

		//correct h occurrences not after d, f, k, j, l, n, p, s, t, x
		word = PatternService.replaceAll(word, REGEX_H, "$1");
		//rewrite characters
		word = PatternService.replaceAll(word, REGEX_DH, "đ");
		word = PatternService.replaceAll(word, REGEX_JH, "ɉ");
		word = PatternService.replaceAll(word, REGEX_LH, "ƚ");
		word = PatternService.replaceAll(word, REGEX_NH, "ñ");
		word = PatternService.replaceAll(word, REGEX_TH, "ŧ");

		//correct mb/mp occurrences into nb/np
		word = PatternService.replaceAll(word, REGEX_MB_MP, "n$1");
		//correct i occurrences into j at the beginning of a word followed by a vowel and between vowels, correcting also the converse
		word = PatternService.replaceFirst(word, REGEX_J_INTO_I, "i");
		word = PatternService.replaceFirst(word, REGEX_I_INITIAL_INTO_J, "j");
		word = PatternService.replaceAll(word, REGEX_I_INSIDE_INTO_J, "$1j");
		//correct lh occurrences into l not at the beginning of a word and not between vowels
		word = PatternService.replaceFirst(word, REGEX_LH_INITIAL_INTO_L, "l");
		word = PatternService.replaceAll(word, REGEX_LH_INSIDE_INTO_L, "$1l");
		//correct fh occurrences into f not before vowel
		word = PatternService.replaceAll(word, REGEX_FH_INTO_F, "f");
		//correct x occurrences into s prior to c, f, k, p, t
		//correct s occurrences into x prior to m, n, ñ, b, d, g, j, ɉ, s, v, r, l
		word = PatternService.replaceAll(word, REGEX_X_INTO_S, "s");
		word = PatternService.replaceAll(word, REGEX_S_INTO_X, "x");

		//correct morphologic error
		word = PatternService.replaceAll(word, REGEX_MORPHOLOGICAL, "$1$2");

		word = eliminateConsonantGeminates(word);

		word = correctApostrophes(word);

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
			errors[i] = (!syllabe.contains(APOSTROPHE) && !syllabe.contains("'") && !HyphenationParser.HYPHEN.equals(syllabe)
				&& Word.getLastVowelIndex(syllabe) < 0);
		}
		return errors;
	}

}
