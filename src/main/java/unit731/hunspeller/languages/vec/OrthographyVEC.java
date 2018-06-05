package unit731.hunspeller.languages.vec;

import java.util.Arrays;
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

	private static final String[] STRESS_CODES = new String[]{"a\\", "e\\", "o\\", "e/", "i/", "i\\", "ì", "o/", "u/", "u\\", "ù"};
	private static final String[] TRUE_STRESS = new String[]{"à", "è", "ò", "é", "í", "í", "í", "ó", "ú", "ú", "ú"};

	private static final String[] EXTENDED_CHARS = new String[]{"dh", "jh", "lh", "nh", "th"};
	private static final String[] TRUE_CHARS = new String[]{"đ", "ɉ", "ƚ", "ñ", "ŧ"};

	private static final String[] MB_MP = new String[]{"mb", "mp"};
	private static final String[] NB_NP = new String[]{"nb", "np"};

	private static final Matcher REGEX_J_INTO_I = PatternService.matcher("^" + GraphemeVEC.JJH_PHONEME + "(?=[^aeiouàèéí" + GraphemeVEC.I_UMLAUT_PHONEME + "òóúh])");
	private static final Matcher REGEX_I_INITIAL_INTO_J = PatternService.matcher("^i(?=[aeiouàèéíòóú])");
	private static final Matcher REGEX_I_INSIDE_INTO_J = PatternService.matcher("([aeiouàèéíòóú])i(?=[aeiouàèéíòóú])");
	private static final List<Matcher> REGEX_I_INSIDE_INTO_J_FALSE_POSITIVES = Arrays.asList(
		PatternService.matcher("b[ae]roi[aeèi]r")
	);
	private static final Matcher REGEX_LH_INITIAL_INTO_L = PatternService.matcher("^ƚ(?=[^ʼ'aeiouàèéíòóújw])");
	private static final Matcher REGEX_LH_INSIDE_INTO_L = PatternService.matcher("([^ʼ'aeiouàèéíòóú-])ƚ(?=[aeiouàèéíòóújw])|([aeiouàèéíòóú])ƚ(?=[^aeiouàèéíòóújw])");
	private static final Matcher REGEX_FH_INTO_F = PatternService.matcher("fh(?=[^aeiouàèéíòóú])");
	private static final Matcher REGEX_X_INTO_S = PatternService.matcher("x(?=[cfkpt])");
	private static final Matcher REGEX_S_INTO_X = PatternService.matcher("s(?=([mnñbdg" + GraphemeVEC.JJH_PHONEME + "ɉsvrlŧ]))");

	private static final Matcher REGEX_MORPHOLOGICAL = PatternService.matcher("([c" + GraphemeVEC.JJH_PHONEME + "ñ])i([aeiou])");

	private static final Matcher REGEX_CONSONANT_GEMINATES = PatternService.matcher("([^aeiou]){1}\\1+");

	private static class SingletonHelper{
		private static final Orthography INSTANCE = new OrthographyVEC();
	}


	public static synchronized Orthography getInstance(){
		return SingletonHelper.INSTANCE;
	}

	@Override
	public String correctOrthography(String word){
		//correct stress
		word = StringUtils.replaceEach(word, STRESS_CODES, TRUE_STRESS);

		//correct h occurrences after d, j, l, n, t
		word = StringUtils.replaceEach(word, EXTENDED_CHARS, TRUE_CHARS);

		//correct mb/mp occurrences into nb/np
		word = StringUtils.replaceEach(word, MB_MP, NB_NP);

		word = GraphemeVEC.handleJHJWIUmlautPhonemes(word);

		//correct i occurrences into j at the beginning of a word followed by a vowel and between vowels, correcting also the converse
		word = PatternService.replaceAll(word, REGEX_J_INTO_I, "i");
		word = PatternService.replaceAll(word, REGEX_I_INITIAL_INTO_J, GraphemeVEC.JJH_PHONEME);
		boolean iInsideIntoJFalsePositive = false;
		for(Matcher m : REGEX_I_INSIDE_INTO_J_FALSE_POSITIVES)
			if(PatternService.find(word, m)){
				iInsideIntoJFalsePositive = true;
				break;
			}
		if(!iInsideIntoJFalsePositive)
			word = PatternService.replaceAll(word, REGEX_I_INSIDE_INTO_J, "$1" + GraphemeVEC.JJH_PHONEME);
		//correct lh occurrences into l not at the beginning of a word and not between vowels
		word = PatternService.replaceAll(word, REGEX_LH_INITIAL_INTO_L, "l");
		word = PatternService.replaceAll(word, REGEX_LH_INSIDE_INTO_L, "$1l");
		//correct fh occurrences into f not before vowel
		word = PatternService.replaceAll(word, REGEX_FH_INTO_F, "f");
		//correct x occurrences into s prior to c, f, k, p, t
		//correct s occurrences into x prior to m, n, ñ, b, d, g, j, ɉ, s, v, r, l
		word = PatternService.replaceAll(word, REGEX_X_INTO_S, "s");
		word = PatternService.replaceAll(word, REGEX_S_INTO_X, "x");

		//correct morphological errors
		word = PatternService.replaceAll(word, REGEX_MORPHOLOGICAL, "$1$2");

		word = GraphemeVEC.rollbackJHJWIUmlautPhonemes(word);

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
			errors[i] = (!syllabe.contains(APOSTROPHE) && !syllabe.contains(APOSTROPHE2) && !HyphenationParser.HYPHEN.equals(syllabe)
				&& WordVEC.getLastVowelIndex(syllabe) < 0);
		}
		return errors;
	}

}
