package unit731.hunspeller.languages.vec;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternHelper;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
class GraphemeVEC{

	public static final String JJH_PHONEME = "ʝ";
	public static final String FH_PHONEME = "\uA799";
	public static final String I_UMLAUT_PHONEME = "ï";
	public static final String D_STROKE_GRAPHEME = "đ";
	private static final String F_GRAPHEME = "f";
	public static final String H_GRAPHEME = "h";
	private static final String FH_GRAPHEME = F_GRAPHEME + H_GRAPHEME;
	private static final String J_GRAPHEME = "j";
	public static final String I_GRAPHEME = "i";
	public static final String L_GRAPHEME = "l";
	public static final String L_STROKE_GRAPHEME = "ƚ";
	private static final String W_GRAPHEME = "w";
	private static final String U_GRAPHEME = "u";
	public static final String S_GRAPHEME = "s";
	public static final String T_STROKE_GRAPHEME = "ŧ";
	public static final String X_GRAPHEME = "x";

	private static final Matcher DIPHTONG = PatternHelper.matcher("[iu][íú]|[àèéòó][iu]");
	private static final Matcher HYATUS = PatternHelper.matcher("[aeoàèéòó][aeo]|[íú][aeiou]|[aeiou][àèéíòóú]");
//	private static final Matcher HYATUS = PatternService.matcher("[íú][aeiou]|[iu][aeoàèéòó]|[aeo][aeoàèéíòóú]|[àèéòó][aeo]");

	private static final Matcher ETEROPHONIC_SEQUENCE = PatternHelper.matcher("(?:^|[^aeiouàèéíòóú])[iju][àèéíòóú]");
	private static final Matcher ETEROPHONIC_SEQUENCE_W = PatternHelper.matcher("((?:^|[^s])t|(?:^|[^t])[kgrs]|i)u([aeiouàèéíòóú])");
	private static final Matcher ETEROPHONIC_SEQUENCE_J = PatternHelper.matcher("([^aeiouàèéíòóúw])i([aeiouàèéíòóú])");
	private static final List<Matcher> ETEROPHONIC_SEQUENCE_J_FALSE_POSITIVES = Arrays.asList(PatternHelper.matcher("^(c)i(uí)$"),
		PatternHelper.matcher("^(teñ|ko[" + JJH_PHONEME + "ɉñ])i([ou]r)"),
		PatternHelper.matcher("^([d" + JJH_PHONEME + "ɉ])i(aspr)")
	);


	public static boolean isDiphtong(String group){
		return PatternHelper.find(group, DIPHTONG);
	}

	public static boolean isHyatus(String group){
		return PatternHelper.find(group, HYATUS);
	}

	public static boolean isEterophonicSequence(String group){
		return PatternHelper.find(group, ETEROPHONIC_SEQUENCE);
	}


	/**
	 * Handle /j/ and /w/ phonemes.
	 *
	 * NOTE: Use mostly IPA standard, non–standard IPA character is used to mark /d͡ʒ/-affine grapheme.
	 * 
	 * @param word	The word to be converted
	 * @return	The converted word
	 */
	public static String handleJHJWIUmlautPhonemes(String word){
		//correct fh occurrences
		if(word.contains(FH_GRAPHEME))
			word = StringUtils.replace(word, FH_GRAPHEME, F_GRAPHEME);

		//this step is mandatory before eterophonic sequence VjV
//FIXME is there a way to optimize this PatternService.replaceAll?
		if(word.contains(J_GRAPHEME))
			word = StringUtils.replace(word, J_GRAPHEME, JJH_PHONEME);
		if(word.contains(I_GRAPHEME))
			for(Matcher m : ETEROPHONIC_SEQUENCE_J_FALSE_POSITIVES)
				word = PatternHelper.replaceAll(word, m, "$1" + I_UMLAUT_PHONEME + "$2");


		//phonize etherophonic sequences
		if(word.contains(U_GRAPHEME))
			word = PatternHelper.replaceAll(word, ETEROPHONIC_SEQUENCE_W, "$1w$2");
		if(word.contains(I_GRAPHEME))
			word = PatternHelper.replaceAll(word, ETEROPHONIC_SEQUENCE_J, "$1j$2");
		return word;
	}

	/**
	 * Convert back the /j/ and /w/ phonemes into the original alphabetical characters.
	 * 
	 * @param word	The "phonemized" word to be converted
	 * @return	The converted word
	 */
	public static String rollbackJHJWIUmlautPhonemes(String word){
		word = StringUtils.replace(word, FH_PHONEME, FH_GRAPHEME);
		//this step is mandatory before eterophonic sequence VjV
		word = StringUtils.replace(word, J_GRAPHEME, I_GRAPHEME);
		word = StringUtils.replace(word, I_UMLAUT_PHONEME, I_GRAPHEME);
		word = StringUtils.replace(word, W_GRAPHEME, U_GRAPHEME);
		word = StringUtils.replace(word, JJH_PHONEME, J_GRAPHEME);
		return word;
	}

}
