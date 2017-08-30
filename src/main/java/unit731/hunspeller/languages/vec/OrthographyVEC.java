package unit731.hunspeller.languages.vec;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.PatternService;


public class OrthographyVEC extends Orthography{

	private static final Matcher REGEX_A_GRAVE = Pattern.compile("a\\\\").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_E_GRAVE = Pattern.compile("e\\\\").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_O_GRAVE = Pattern.compile("o\\\\").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_E_ACUTE = Pattern.compile("e/").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_I_ACUTE = Pattern.compile("(i/|ì)").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_O_ACUTE = Pattern.compile("o/").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_U_ACUTE = Pattern.compile("(u/|ù)").matcher(StringUtils.EMPTY);

	private static final Matcher REGEX_H = Pattern.compile("(^|[^dfkjlnpstx])h").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_DH = Pattern.compile("dh").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_JH = Pattern.compile("jh").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_LH = Pattern.compile("lh").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_NH = Pattern.compile("nh").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_TH = Pattern.compile("th").matcher(StringUtils.EMPTY);

	private static final Matcher REGEX_MB_MP = Pattern.compile("m([bp])").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_J_INTO_I = Pattern.compile("^j(?=[^aeiouàèéíòóúh])").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_I_INITIAL_INTO_J = Pattern.compile("^i(?=[aeiouàèéíòóú])").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_I_INSIDE_INTO_J = Pattern.compile("([aeiouàèéíòóú])i(?=[aeiouàèéíòóú])").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_LH_INITIAL_INTO_L = Pattern.compile("^ƚ(?=[^ʼ'aeiouàèéíòóú])").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_LH_INSIDE_INTO_L = Pattern.compile("([^ʼ'aeiouàèéíòóú])ƚ(?=[aeiouàèéíòóú])|([aeiouàèéíòóú])ƚ(?=[^aeiouàèéíòóú])").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_FH_INTO_F = Pattern.compile("fh(?=[^aeiouàèéíòóú])").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_X_INTO_S = Pattern.compile("x(?=[cfkpt])").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_S_INTO_X = Pattern.compile("s(?=([mnñbdgjɉsvrlŧ]|jh))").matcher(StringUtils.EMPTY);

	private static final Matcher REGEX_MORPHOLOGICAL = Pattern.compile("([cjñ])i([aeiou])").matcher(StringUtils.EMPTY);

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

		word = word.replaceAll("([^aeiou]){1}\\1+", "$1");
		
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
