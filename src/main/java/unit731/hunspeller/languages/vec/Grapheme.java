package unit731.hunspeller.languages.vec;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


public class Grapheme{

	private static final Matcher DIPHTONG = Pattern.compile("([iu][íú]|[àèéòó][iu])").matcher(StringUtils.EMPTY);
	private static final Matcher HYATUS = Pattern.compile("([aeoàèéòó][aeo]|[íú][aeiou]|[aeiou][àèéíòóú])").matcher(StringUtils.EMPTY);
//	private static final Matcher HYATUS = Pattern.compile("([íú][aeiou]|[iu][aeoàèéòó]|[aeo][aeoàèéíòóú]|[àèéòó][aeo])").matcher(StringUtils.EMPTY);
	private static final Matcher ENDS_IN_VOWEL = Pattern.compile("[aeiouàèéíòóú][^aàbcdđeéèfghiíjɉklƚmnñoóòprsʃtŧuúvxʒ]*$").matcher(StringUtils.EMPTY);

	private static final Matcher ETEROPHONIC_SEQUENCE = Pattern.compile("(^|[^aeiouàèéíòóú])[iju][àèéíòóú]").matcher(StringUtils.EMPTY);
	private static final Matcher ETEROPHONIC_SEQUENCE_J = Pattern.compile("([^aeiouàèéíòóú])i([aeiouàèéíòóú])").matcher(StringUtils.EMPTY);
	private static final Matcher ETEROPHONIC_SEQUENCE_W1 = Pattern.compile("((^|[^s])t)u([aeiouàèéíòóú])").matcher(StringUtils.EMPTY);
	private static final Matcher ETEROPHONIC_SEQUENCE_W2 = Pattern.compile("((^|[^t])[kgrs])u([aeiouàèéíòóú])").matcher(StringUtils.EMPTY);


	public static boolean endsInVowel(String word){
		return PatternService.find(word, ENDS_IN_VOWEL);
	}

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
	 * NOTE: Use IPA standard.
	 * 
	 * @param word	The word to be converted
	 * @return	The converted word
	 */
	public static String preConvertGraphemesIntoPhones(String word){
		word = phonizeJAffineGrapheme(word);
		word = phonizeEterophonicSequence(word);
		return word;
	}

	/**
	 * NOTE: Use non-standard IPA to mark /d͡ʒ/-affine grapheme.
	 */
	private static String phonizeJAffineGrapheme(String word){
		//this step is mandatory before eterophonic sequence VjV
		return StringUtils.replace(word, "j", "ʝ");
	}

	private static String phonizeEterophonicSequence(String word){
		word = PatternService.replaceAll(word, ETEROPHONIC_SEQUENCE_J, "$1j$2");
		word = PatternService.replaceAll(word, ETEROPHONIC_SEQUENCE_W1, "$1w$3");
		word = PatternService.replaceAll(word, ETEROPHONIC_SEQUENCE_W2, "$1w$3");
		return word;
	}

}
