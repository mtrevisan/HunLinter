package unit731.hunspeller.languages.vec;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;


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
	private static final Matcher REGEX_J_INTO_I = Pattern.compile("j(?=[^aeiouàèéíòóúh])").matcher(StringUtils.EMPTY);
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
		word = REGEX_A_GRAVE.reset(word).replaceAll("à");
		word = REGEX_E_GRAVE.reset(word).replaceAll("è");
		word = REGEX_O_GRAVE.reset(word).replaceAll("ò");
		word = REGEX_E_ACUTE.reset(word).replaceAll("é");
		word = REGEX_I_ACUTE.reset(word).replaceAll("í");
		word = REGEX_O_ACUTE.reset(word).replaceAll("ó");
		word = REGEX_U_ACUTE.reset(word).replaceAll("ú");

		//correct h occurrences not after d, f, k, j, l, n, p, s, t, x
		word = REGEX_H.reset(word).replaceAll("$1");
		//rewrite characters
		word = REGEX_DH.reset(word).replaceAll("đ");
		word = REGEX_JH.reset(word).replaceAll("ɉ");
		word = REGEX_LH.reset(word).replaceAll("ƚ");
		word = REGEX_NH.reset(word).replaceAll("ñ");
		word = REGEX_TH.reset(word).replaceAll("ŧ");

		//correct mb/mp occurrences into nb/np
		word = REGEX_MB_MP.reset(word).replaceAll("n$1");
		//correct i occurrences into j at the beginning of a word followed by a vowel and between vowels, correcting also the converse
		word = REGEX_J_INTO_I.reset(word).replaceAll("i");
		word = REGEX_I_INITIAL_INTO_J.reset(word).replaceAll("j");
		word = REGEX_I_INSIDE_INTO_J.reset(word).replaceAll("$1j");
		//correct lh occurrences into l not at the beginning of a word and not between vowels
		word = REGEX_LH_INITIAL_INTO_L.reset(word).replaceAll("l");
		word = REGEX_LH_INSIDE_INTO_L.reset(word).replaceAll("$1l");
		//correct fh occurrences into f not before vowel
		word = REGEX_FH_INTO_F.reset(word).replaceAll("f");
		//correct x occurrences into s prior to c, f, k, p, t
		//correct s occurrences into x prior to m, n, ñ, b, d, g, j, ɉ, s, v, r, l
		word = REGEX_X_INTO_S.reset(word).replaceAll("s");
		word = REGEX_S_INTO_X.reset(word).replaceAll("x");

		//correct morphologic error
		word = REGEX_MORPHOLOGICAL.reset(word).replaceAll("$1$2");

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
