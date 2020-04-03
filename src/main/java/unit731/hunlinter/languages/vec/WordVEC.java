package unit731.hunlinter.languages.vec;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;
import unit731.hunlinter.services.RegexHelper;


public class WordVEC{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordVEC.class);

	private static final String PIPE = "|";

	private static final String VOWELS_PLAIN = "aAeEiIïÏoOuUüÜ" + GraphemeVEC.PHONEME_I_CIRCUMFLEX;
	private static final String VOWELS_STRESSED = "àÀéÉèÈíÍóÓòÒúÚ";
	private static final String VOWELS_UNSTRESSED = "aAeEeEiIoOoOuU";
	private static final String CONSONANTS = "bBcCdDđĐfFgGhHjJɉɈkKlLƚȽmMnNñÑpPrRsStTŧŦvVxX";

	private static final char[] VOWELS_PLAIN_ARRAY = VOWELS_PLAIN.toCharArray();
	private static final char[] VOWELS_STRESSED_ARRAY = VOWELS_STRESSED.toCharArray();
	private static final char[] VOWELS_EXTENDED_ARRAY = (VOWELS_PLAIN + VOWELS_STRESSED).toCharArray();
	public static final String VOWELS = "aAàÀeEéÉèÈiIíÍïÏoOóÓòÒuUúÚüÜ";
	private static final char[] CONSONANTS_ARRAY = CONSONANTS.toCharArray();
	static{
		Arrays.sort(VOWELS_PLAIN_ARRAY);
		Arrays.sort(VOWELS_STRESSED_ARRAY);
		Arrays.sort(VOWELS_EXTENDED_ARRAY);
		Arrays.sort(CONSONANTS_ARRAY);
	}

	private static final String COLLATOR_RULE = ", ' ' < ʼ=''' , '-'='‒' & '-'='–' < 0 < 1 < 2 < 3 < 4 < 5 < 6 < 7 < 8 < 9 < '/' < a,A < à,À < b,B < c,C < d,D < đ=dh,Đ=Dh < e,E < é,É < è,È < f,F < g,G < h,H < i,I < í,Í < ï,Ï < j,J < ɉ=jh,Ɉ=Jh < k,K < l,L < ƚ=lh,Ƚ=Lh < m,M < n,N < ñ=nh,Ñ=Nh < o,O < ó,Ó < ò,Ò < p,P < r,R < s,S < t,T < ŧ=th,Ŧ=Th < u,U < ú,Ú < ü,Ü < v,V < x,X";
	private static Collator COLLATOR;
	static{
		try{
			COLLATOR = new RuleBasedCollator(COLLATOR_RULE);
		}
		catch(final ParseException e){
			//cannot happen
			LOGGER.error(e.getMessage());
		}
	}

	private static final Pattern DEFAULT_STRESS_GROUP = RegexHelper.pattern("^(?:(?:de)?fr|(?:ma|ko|x)?[lƚ]|n|apl|(?:in|re)st)au");

	private static final String NO_STRESS_AVER = "^(?:r[aeiï]|ar)?g?(?:ar)?[àé]-?(?:[lƚ][oaie]|[gmnstv]e|[mn]i|nt[ei]|s?t[ou])$";
	private static final String NO_STRESS_ESER = "^(?:r[aeiï]|ar)?(?:(?:s[ae]r)?[àé]|[sx]é)-?(?:[lƚ][oaie]|[gmnstv]e|[mn]i|nt[ei]|s?t[ou])$";
	private static final String NO_STRESS_DAR_FAR_STAR = "^(?:(?:dex)?d|(?:(?:dex)?asue|des|kon(?:tra–?)?|[lƚ]iku[ei]|mal–?|putre|rare|r[ae]|ar|sastu|sat[iu]s|sodis|sora|stra–?|st[ou]pe|tore|tume)?f|(?:kon(?:tra)?|mal–?|move|o|re|so(?:ra|to))?st)(?:[ae]rà|[àé])-?(?:[lƚ][oaie]|[gmnstv]e|[mn]i|nt[ei]|s?t[ou])$";
	private static final String NO_STRESS_SAVER = "^(?:pre|r[ae]|ar|stra–?)?(?:sà|sav?arà)-?(?:[lƚ][oaie]|[gmnstv]e|[mn]i|nt[ei]|s?t[ou])$";
	private static final String NO_STRESS_ANDAR = "^(?:r[ae]|ar)?v[àé]-?(?:[lƚ][oaie]|[gmnstv]e|[mn]i|nt[ei]|s?t[ou])$";
	private static final String NO_STRESS_TRAER = "^(?:|as?|des?|es|kon|pro|re|so|sub?)?tr[àé]-?(?:[lƚ][oaie]|[gmnstv]e|[mn]i|nt[ei]|s?t[ou])$";
	private static final Pattern PREVENT_UNMARK_STRESS;
	static{
		final StringJoiner sj = (new StringJoiner(PIPE))
			.add(NO_STRESS_AVER)
			.add(NO_STRESS_ESER)
			.add(NO_STRESS_DAR_FAR_STAR)
			.add(NO_STRESS_SAVER)
			.add(NO_STRESS_ANDAR)
			.add(NO_STRESS_TRAER);
		PREVENT_UNMARK_STRESS = RegexHelper.pattern(sj.toString());
	}

	//NOTE: must be sorted!
	private static final char[] SIMPLE_VOWELS_ARRAY = "aeiou".toCharArray();
	//NOTE: must be sorted!
	private static final char[] ACUTE_STRESSED_VOWELS_ARRAY = "àéíóú".toCharArray();


	private WordVEC(){}

	public static int countGraphemes(final String word){
		return (int)IntStream.range(0, word.length())
			.mapToObj(word::charAt)
			.filter(chr -> Arrays.binarySearch(VOWELS_EXTENDED_ARRAY, chr) >= 0 || Arrays.binarySearch(CONSONANTS_ARRAY, chr) >= 0)
			.count();
	}

	public static boolean isApostrophe(final char chr){
		return (chr == HyphenationParser.RIGHT_MODIFIER_LETTER_APOSTROPHE || chr == HyphenationParser.APOSTROPHE.charAt(0));
	}

	public static boolean isVowel(final char chr){
		return (Arrays.binarySearch(VOWELS_EXTENDED_ARRAY, chr) >= 0);
	}

	public static boolean isConsonant(final char chr){
		return (Arrays.binarySearch(CONSONANTS_ARRAY, chr) >= 0);
	}

	//^[ʼ']?[aeiouàèéíòóú]
	public static boolean startsWithVowel(final String word){
		char chr = word.charAt(0);
		if(isApostrophe(chr))
			chr = word.charAt(1);
		return isVowel(chr);
	}

	//[aeiouàèéíòóú][^aàbcdđeéèfghiíjɉklƚmnñoóòprsʃtŧuúvxʒ]*ʼ?$
	public static boolean endsWithVowel(final String word){
		final int idx = word.length() - 1;
		char chr = word.charAt(idx);
		if(isApostrophe(chr))
			chr = word.charAt(idx - 1);
		return isVowel(chr);
	}

	//[aeiou][^aeiou]*$
	private static int getLastUnstressedVowelIndex(final String word, int lastLetterIndex){
		for(lastLetterIndex --; lastLetterIndex >= 0; lastLetterIndex --){
			final char chr = word.charAt(lastLetterIndex);
			if(Arrays.binarySearch(VOWELS_PLAIN_ARRAY, chr) >= 0)
				break;
		}
		return lastLetterIndex;
	}


	//[àèéíòóú]
	public static boolean hasStressedGrapheme(final String word){
		return (countAccents(word) == 1);
	}

	public static int countAccents(final String word){
		return (int)IntStream.range(0, word.length())
			.filter(i -> Arrays.binarySearch(VOWELS_STRESSED_ARRAY, word.charAt(i)) >= 0)
			.count();
	}

	private static String suppressStress(final String word){
		return StringUtils.replaceChars(word, VOWELS_STRESSED, VOWELS_UNSTRESSED);
	}


	private static String setAcuteStressAtIndex(final String word, final int idx){
		final StringBuffer sb = new StringBuffer(word);
		sb.setCharAt(idx, addStressAcute(word.charAt(idx)));
		return sb.toString();
	}

	//NOTE: is seems faster the current method (above)
//	private static String setAcuteStressAtIndex(final String word, final int idx){
//		return replaceCharAt(word, idx, addStressAcute(word.charAt(idx)));
//	}
//
//	private static String replaceCharAt(final String text, final int idx, final char chr){
//		final StringBuffer sb = new StringBuffer(text);
//		sb.setCharAt(idx, chr);
//		return sb.toString();
//	}

	private static char addStressAcute(final char chr){
		final int stressedIndex = Arrays.binarySearch(SIMPLE_VOWELS_ARRAY, chr);
		return ACUTE_STRESSED_VOWELS_ARRAY[stressedIndex];
	}

	public static String markDefaultStress(String word){
		int stressIndex = getIndexOfStress(word);
		if(stressIndex < 0){
			final String phones = GraphemeVEC.handleJHJWIUmlautPhonemes(word);
			final int lastChar = getLastUnstressedVowelIndex(phones, phones.length());

			//last vowel if the word ends with consonant, penultimate otherwise
			//default to the second vowel of a group of two (first one on a monosyllabe)
			if(endsWithVowel(phones))
				stressIndex = getLastUnstressedVowelIndex(phones, lastChar);
			if(stressIndex >= 0 && RegexHelper.find(phones, DEFAULT_STRESS_GROUP))
				stressIndex --;
			else if(stressIndex < 0)
				stressIndex = lastChar;

			if(stressIndex >= 0)
				word = setAcuteStressAtIndex(word, stressIndex);
		}
		return word;
	}

	public static String unmarkDefaultStress(String word){
		final int stressIndex = getIndexOfStress(word);
		//check if the word have a stress, not on the last letter, not followed by an en dash
		final int wordLength = word.length();
		if(stressIndex >= 0
				&& stressIndex + 1 < wordLength
				&& word.charAt(stressIndex + 1) != HyphenationParser.EN_DASH.charAt(0)
				&& !GraphemeVEC.isDiphtong(word)
				&& !GraphemeVEC.isHyatus(word)
				&& !RegexHelper.find(word, PREVENT_UNMARK_STRESS)){
			final String tmp = suppressStress(word);
			if(!tmp.equals(word) && markDefaultStress(tmp).equals(word))
				word = tmp;
		}
		return word;
	}

	static int getIndexOfStress(final String word){
		return StringUtils.indexOfAny(word, VOWELS_STRESSED_ARRAY);
	}

	public static Comparator<String> sorterComparator(){
		return (str1, str2) -> COLLATOR.compare(str1, str2);
	}

}
