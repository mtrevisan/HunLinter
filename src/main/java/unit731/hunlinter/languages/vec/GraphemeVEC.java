package unit731.hunlinter.languages.vec;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.services.PatternHelper;


class GraphemeVEC{

	public static final String PHONEME_JJH = "ʝ";
	public static final String PHONEME_FH = "\uA799";
	public static final String PHONEME_I_UMLAUT = "ï";
	public static final String GRAPHEME_D_STROKE = "đ";
	private static final String GRAPHEME_F = "f";
	public static final String GRAPHEME_H = "h";
	private static final String GRAPHEME_FH = GRAPHEME_F + GRAPHEME_H;
	private static final String GRAPHEME_J = "j";
	public static final String GRAPHEME_I = "i";
	public static final String GRAPHEME_L = "l";
	public static final String GRAPHEME_L_STROKE = "ƚ";
	private static final String GRAPHEME_W = "w";
	private static final String GRAPHEME_U = "u";
	public static final String GRAPHEME_S = "s";
	public static final String GRAPHEME_T_STROKE = "ŧ";
	public static final String GRAPHEME_X = "x";

	private static final Pattern DIPHTONG = PatternHelper.pattern("[iu][íú]|[àèéòó][iu]");
	private static final Pattern HYATUS = PatternHelper.pattern("[aeoàèéòó][aeo]|[íú][aeiou]|[aeiou][àèéíòóú]");
//	private static final Pattern HYATUS = PatternService.pattern("[íú][aeiou]|[iu][aeoàèéòó]|[aeo][aeoàèéíòóú]|[àèéòó][aeo]");

	private static final Pattern ETEROPHONIC_SEQUENCE = PatternHelper.pattern("(?:^|[^aeiouàèéíòóú])[iju][àèéíòóú]");
	private static final Pattern ETEROPHONIC_SEQUENCE_W = PatternHelper.pattern("((?:^|[^s])t|(?:^|[^t])[kgrs]|i)u([aeiouàèéíòóú])");
	private static final Pattern ETEROPHONIC_SEQUENCE_J = PatternHelper.pattern("([^aeiouàèéíòóúw])i([aeiouàèéíòóú])");
	private static final List<Pattern> ETEROPHONIC_SEQUENCE_J_FALSE_POSITIVES = Arrays.asList(
		PatternHelper.pattern("^(teñ|ko[" + PHONEME_JJH + "ɉñ])i([ou]r)"),
		PatternHelper.pattern("^([d" + PHONEME_JJH + "ɉ])i(aspr)"),
		PatternHelper.pattern("^((?:r[ae]|ar)?bo[" + PHONEME_JJH + "ɉ])i(ur[ae])"),
		PatternHelper.pattern("^(re[sŧ]e)i([ou]r[aeio]?)")
	);
	private static final Pattern ETEROPHONIC_SEQUENCE_FALSE_POSITIVES = PatternHelper.pattern("^[j\u0249]iog?r[aà]f|[j\u0249]iur(et)?[ae]$");


	private GraphemeVEC(){}

	public static boolean isDiphtong(final String group){
		return PatternHelper.find(group, DIPHTONG);
	}

	public static boolean isHyatus(final String group){
		return PatternHelper.find(group, HYATUS);
	}

	public static boolean isEterophonicSequence(final String group){
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
	public static String handleJHJWIUmlautPhonemes(final String word){
		String phonemizedWord = correctFhOccurrences(word);

		phonemizedWord = correctIJGraphemes(phonemizedWord);

		//phonize etherophonic sequences
		if(phonemizedWord.contains(GRAPHEME_U))
			phonemizedWord = PatternHelper.replaceAll(phonemizedWord, ETEROPHONIC_SEQUENCE_W, "$1w$2");
		if(phonemizedWord.contains(GRAPHEME_I) && !containsNonEterophonicSequence(word))
			phonemizedWord = PatternHelper.replaceAll(phonemizedWord, ETEROPHONIC_SEQUENCE_J, "$1j$2");

		return phonemizedWord;
	}

	public static boolean containsNonEterophonicSequence(final String word){
		return PatternHelper.find(word, ETEROPHONIC_SEQUENCE_FALSE_POSITIVES);
	}

	private static String correctFhOccurrences(String word){
		if(word.contains(GRAPHEME_FH))
			word = StringUtils.replace(word, GRAPHEME_FH, GRAPHEME_F);
		return word;
	}

	private static String correctIJGraphemes(String word){
		//this step is mandatory before eterophonic sequence VjV
		if(word.contains(GRAPHEME_J))
			word = StringUtils.replace(word, GRAPHEME_J, PHONEME_JJH);
		if(word.contains(GRAPHEME_I))
			for(final Pattern p : ETEROPHONIC_SEQUENCE_J_FALSE_POSITIVES)
				word = PatternHelper.replaceAll(word, p, "$1" + PHONEME_I_UMLAUT + "$2");
		return word;
	}

	/**
	 * Convert back the /j/ and /w/ phonemes into the original alphabetical characters.
	 *
	 * @param word	The "phonemized" word to be converted
	 * @return	The converted word
	 */
	public static String rollbackJHJWIUmlautPhonemes(String word){
		word = StringUtils.replace(word, PHONEME_FH, GRAPHEME_FH);
		//this step is mandatory before eterophonic sequence VjV
		word = StringUtils.replace(word, GRAPHEME_J, GRAPHEME_I);
		word = StringUtils.replace(word, PHONEME_I_UMLAUT, GRAPHEME_I);
		word = StringUtils.replace(word, GRAPHEME_W, GRAPHEME_U);
		word = StringUtils.replace(word, PHONEME_JJH, GRAPHEME_J);
		return word;
	}

}
