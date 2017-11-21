package unit731.hunspeller.languages.vec;

import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


public class Grapheme{

	public static final String JJH_PHONEME = "ʝ";
	public static final String J_CHAR = "j";
	public static final String I_CHAR = "i";
	public static final String W_CHAR = "w";
	public static final String U_CHAR = "u";

	private static final Matcher DIPHTONG = PatternService.matcher("[iu][íú]|[àèéòó][iu]");
	private static final Matcher HYATUS = PatternService.matcher("[aeoàèéòó][aeo]|[íú][aeiou]|[aeiou][àèéíòóú]");
//	private static final Matcher HYATUS = PatternService.matcher("[íú][aeiou]|[iu][aeoàèéòó]|[aeo][aeoàèéíòóú]|[àèéòó][aeo]");

	private static final Matcher ETEROPHONIC_SEQUENCE = PatternService.matcher("(?:^|[^aeiouàèéíòóú])[iju][àèéíòóú]");
	private static final Matcher ETEROPHONIC_SEQUENCE_W = PatternService.matcher("((?:^|[^s])t|(?:^|[^t])[kgrs]|i)u([aeiouàèéíòóú])");
	private static final Matcher ETEROPHONIC_SEQUENCE_J = PatternService.matcher("([^aeiouàèéíòóúw])i([aeiouàèéíòóú])");


	public static boolean isDiphtong(String group){
		return PatternService.find(group, DIPHTONG);
	}

	public static boolean isHyatus(String group){
		return PatternService.find(group, HYATUS);
	}

	public static boolean isEterophonicSequence(String group){
		return PatternService.find(group, ETEROPHONIC_SEQUENCE);
	}


	/**
	 * Handle /j/ and /w/ phonemes.
	 *
	 * NOTE: Use mostly IPA standard, non-standard IPA character is used to mark /d͡ʒ/-affine grapheme.
	 * 
	 * @param word	The word to be converted
	 * @return	The converted word
	 */
	public static String handleJHJWPhonemes(String word){
		//this step is mandatory before eterophonic sequence VjV
		word = StringUtils.replace(word, J_CHAR, JJH_PHONEME);
		//phonize etherophonic sequences
		word = PatternService.replaceAll(word, ETEROPHONIC_SEQUENCE_W, "$1w$2");
		word = PatternService.replaceAll(word, ETEROPHONIC_SEQUENCE_J, "$1j$2");
		return word;
	}

	/**
	 * Convert back the /j/ and /w/ phonemes into the original alphabetical characters.
	 * 
	 * @param word	The "phonemized" word to be converted
	 * @return	The converted word
	 */
	public static String rollbackJHJWPhonemes(String word){
		//this step is mandatory before eterophonic sequence VjV
		word = StringUtils.replace(word, J_CHAR, I_CHAR);
		word = StringUtils.replace(word, W_CHAR, U_CHAR);
		word = StringUtils.replace(word, JJH_PHONEME, J_CHAR);
		return word;
	}

}
