package unit731.hunspeller.languages.vec;

import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


public class Grapheme{

	private static final Matcher DIPHTONG = PatternService.matcher("([iu][íú]|[àèéòó][iu])");
	private static final Matcher HYATUS = PatternService.matcher("([aeoàèéòó][aeo]|[íú][aeiou]|[aeiou][àèéíòóú])");
//	private static final Matcher HYATUS = PatternService.matcher("([íú][aeiou]|[iu][aeoàèéòó]|[aeo][aeoàèéíòóú]|[àèéòó][aeo])");
	private static final Matcher ENDS_IN_VOWEL = PatternService.matcher("[aeiouàèéíòóú][^aàbcdđeéèfghiíjɉklƚmnñoóòprsʃtŧuúvxʒ]*$");

	private static final Matcher ETEROPHONIC_SEQUENCE = PatternService.matcher("(^|[^aeiouàèéíòóú])[iju][àèéíòóú]");
	private static final Matcher ETEROPHONIC_SEQUENCE_J = PatternService.matcher("([^aeiouàèéíòóú])i([aeiouàèéíòóú])");
	private static final Matcher ETEROPHONIC_SEQUENCE_W1 = PatternService.matcher("((^|[^s])t)u([aeiouàèéíòóú])");
	private static final Matcher ETEROPHONIC_SEQUENCE_W2 = PatternService.matcher("((^|[^t])[kgrs])u([aeiouàèéíòóú])");


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
