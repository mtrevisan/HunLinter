package unit731.hunspeller.languages.vec;

import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


public class GraphemeVEC{

	public static final String JJH_PHONEME = "ʝ";
	private static final String J_GRAPHEME = "j";
	private static final String I_GRAPHEME = "i";
	private static final String W_GRAPHEME = "w";
	private static final String U_GRAPHEME = "u";

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
		if(word.contains(J_GRAPHEME))
			word = StringUtils.replace(word, J_GRAPHEME, JJH_PHONEME);

		//phonize etherophonic sequences
		if(word.contains(U_GRAPHEME))
			word = PatternService.replaceAll(word, ETEROPHONIC_SEQUENCE_W, "$1w$2");
		if(word.contains(I_GRAPHEME))
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
		word = StringUtils.replace(word, J_GRAPHEME, I_GRAPHEME);
		word = StringUtils.replace(word, W_GRAPHEME, U_GRAPHEME);
		word = StringUtils.replace(word, JJH_PHONEME, J_GRAPHEME);
		return word;
	}

}
