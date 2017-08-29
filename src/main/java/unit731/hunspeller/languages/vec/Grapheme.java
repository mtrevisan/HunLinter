package unit731.hunspeller.languages.vec;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;


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
		return ENDS_IN_VOWEL.reset(word).find();
	}

	public static boolean isDiphtong(String group){
		return DIPHTONG.reset(group).find();
	}

	public static boolean isHyatus(String group){
		return HYATUS.reset(group).find();
	}

	public static boolean isEterophonicSequence(String group){
		return ETEROPHONIC_SEQUENCE.reset(group).find();
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
		return StringUtils.replaceAll(word, "j", "ʝ");
	}

	private static String phonizeEterophonicSequence(String word){
		word = ETEROPHONIC_SEQUENCE_J.reset(word).replaceAll("$1j$2");
		word = ETEROPHONIC_SEQUENCE_W1.reset(word).replaceAll("$1w$3");
		word = ETEROPHONIC_SEQUENCE_W2.reset(word).replaceAll("$1w$3");
		return word;
	}

}
