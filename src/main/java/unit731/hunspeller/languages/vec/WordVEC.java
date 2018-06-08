package unit731.hunspeller.languages.vec;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


public class WordVEC{

	private static final String VOWELS_PLAIN = "aAeEiIoOuU" + GraphemeVEC.I_UMLAUT_PHONEME;
	private static final String VOWELS_STRESSED = "àÀéÉèÈíÍóÓòÒúÚ";
	private static final String VOWELS_UNSTRESSED = "aAeEeEiIoOoOuU";
	private static final char[] VOWELS_STRESSED_ARRAY = VOWELS_STRESSED.toCharArray();
	private static final String VOWELS_EXTENDED = VOWELS_PLAIN + VOWELS_STRESSED;
	public static final String CONSONANTS = "bBcCdDđĐfFgGhHjJɉɈkKlLƚȽmMnNñÑpPrRsStTŧŦvVxX";
	private static final String ALPHABET = CONSONANTS + VOWELS_EXTENDED;
	private static final String SORTING_ALPHABET = "-/.0123456789aAàÀbBcCdDđĐeEéÉèÈfFgGhHiIíÍjJɉɈkKlLƚȽmMnNñÑoOóÓòÒpPrRsStTŧŦuUúÚvVxXʼ'";

	private static final Matcher LAST_STRESSED_VOWEL = PatternService.matcher("[aeiouàèéíòóú][^aeiouàèéíòóú]*$");

	private static final Matcher DEFAULT_STRESS_GROUP = PatternService.matcher("(fr|[ln]|st)au$");

	private static final String NO_STRESS_AVER = "^(r[ei])?g?(ar)?[àé]([lƚ][oaie]|[gmnstv]e|[mn]i|nt[ei]|s?t[ou])$";
	private static final String NO_STRESS_ESER = "^(r[ei])?((s[ae]r)?[àé]|[sx]é)([lƚ][oaie]|[gmnstv]e|[mn]i|nt[ei]|s?t[ou])$";
	private static final String NO_STRESS_DAR_FAR_STAR = "^((dex)?d|((dex)?asue|des|kon(tra)?|[lƚ]iku[ei]|putre|(ra)?re|sastu|sat[iu]s|sodis|sora|stra|stupe|tore|tume)?f|(kon(tra)?|mal|move|o|re|so(ra|to))?st)([ae]rà|[àé])([lƚ][oaie]|[gmnstv]e|[mn]i|nt[ei]|s?t[ou])$";
	private static final String NO_STRESS_SAVER = "^(p?re|stra)?(sà|sav?arà)([lƚ][oaie]|[gmnstv]e|[mn]i|nt[ei]|s?t[ou])$";
	private static final String NO_STRESS_ANDAR = "^(re)?v[àé]([lƚ][oaie]|[gmnstv]e|[mn]i|nt[ei]|s?t[ou])$";
	private static final String NO_STRESS_TRAER = "^(|as?|des?|es|kon|pro|re|so|sub?)?tr[àé]([lƚ][oaie]|[gmnstv]e|[mn]i|nt[ei]|s?t[ou])$";
	private static final Matcher PREVENT_UNMARK_STRESS;
	static{
		StringJoiner sj = (new StringJoiner("|"))
			.add(NO_STRESS_AVER)
			.add(NO_STRESS_ESER)
			.add(NO_STRESS_DAR_FAR_STAR)
			.add(NO_STRESS_SAVER)
			.add(NO_STRESS_ANDAR)
			.add(NO_STRESS_TRAER);
		PREVENT_UNMARK_STRESS = PatternService.matcher(sj.toString());
	}

	private static final Map<String, String> ACUTE_STRESSES = new HashMap<>();
	static{
		ACUTE_STRESSES.put("a", "à");
		ACUTE_STRESSES.put("e", "é");
		ACUTE_STRESSES.put("i", "í");
		ACUTE_STRESSES.put("o", "ó");
		ACUTE_STRESSES.put("u", "ú");
	}


	//[aeiouàèéíòóú][^aàbcdđeéèfghiíjɉklƚmnñoóòprsʃtŧuúvxʒ]*$
	public static boolean endsInVowel(String word){
		int i = word.length();
		while((-- i) >= 0){
			char chr = word.charAt(i);
			if(ALPHABET.indexOf(chr) != -1)
				return (VOWELS_EXTENDED.indexOf(chr) != -1);
		}
		return false;
	}

	public static int getLastVowelIndex(String word){
		Matcher m = LAST_STRESSED_VOWEL.reset(word);
		return (m.find()? m.start(): -1);
	}

	//[aeiou][^aeiou]*[^aàbcdđeéèfghiíjɉklƚmnñoóòprstŧuúvx]*$
	private static int getLastUnstressedVowelIndex(String word, int idx){
		int i = (idx >= 0? idx: word.length());
		while((-- i) >= 0){
			char chr = word.charAt(i);
			if(VOWELS_PLAIN.indexOf(chr) != -1)
				return i;
		}
		return -1;
	}


	//[àèéíòóú]
	public static boolean isStressed(String word){
		return (countAccents(word) == 1);
	}

	//([^àèéíòóú]*[àèéíòóú]){2,}
	public static boolean hasMultipleAccents(String word){
		return (countAccents(word) > 1);
	}

	public static int countAccents(String word){
		int count = 0;
		for(int i = 0; i < word.length(); i ++)
			if(ArrayUtils.contains(VOWELS_STRESSED_ARRAY, word.charAt(i)))
				count ++;
		return count;
	}

	private static int getIndexOfStress(String word){
		for(int i = 0; i < word.length(); i ++)
			if(ArrayUtils.contains(VOWELS_STRESSED_ARRAY, word.charAt(i)))
				return i;
		return -1;
	}

	private static String suppressStress(String word){
		return StringUtils.replaceChars(word, VOWELS_STRESSED, VOWELS_UNSTRESSED);
	}


//FIXME speed-up?
	private static String markDefaultStress(String word){
		int idx = getIndexOfStress(word);
		if(idx < 0){
			String phones = GraphemeVEC.handleJHJWIUmlautPhonemes(word);
			int lastChar = getLastUnstressedVowelIndex(phones, -1);

			//last vowel if the word ends with consonant, penultimate otherwise, default to the second vowel of a group of two (first one on a monosyllabe)
			if(endsInVowel(phones))
				idx = getLastUnstressedVowelIndex(phones, lastChar);
			if(idx >= 0 && PatternService.find(phones.substring(0, idx + 1), DEFAULT_STRESS_GROUP))
				idx --;
			if(idx < 0)
				idx = lastChar;

			if(idx >= 0)
				word = setAcuteStressAtIndex(word, idx);
		}

		return word;
	}

	private static String setAcuteStressAtIndex(String word, int idx){
		return word.substring(0, idx) + addStressAcute(word.charAt(idx)) + word.substring(idx + 1);
	}

	//NOTE: is seems faster the current method (above)
//	private static String setAcuteStressAtIndex(String word, int idx){
//		return replaceCharAt(word, idx, addStressAcute(word.charAt(idx)));
//	}
//
//	private static String replaceCharAt(String text, int idx, char chr){
//		StringBuilder sb = new StringBuilder(text);
//		sb.setCharAt(idx, chr);
//		return sb.toString();
//	}

	private static char addStressAcute(char chr){
		String c = String.valueOf(chr);
		return ACUTE_STRESSES.getOrDefault(c, c).charAt(0);
	}

	public static String unmarkDefaultStress(String word){
		int idx = getIndexOfStress(word);
		//check if the word have a stress and this is not on the last letter
		if(idx >= 0 && idx < word.length() - 1){
			String subword = word.substring(idx, idx + 2);
			if(!GraphemeVEC.isDiphtong(subword) && !GraphemeVEC.isHyatus(subword) && !PatternService.find(word, PREVENT_UNMARK_STRESS)){
				String tmp = suppressStress(word);
				if(!tmp.equals(word) && markDefaultStress(tmp).equals(word))
					word = tmp;
			}
		}
		return word;
	}

	public static Comparator<String> sorterComparator(){
		return (str1, str2) -> {
			int result = 0;
			int len1 = str1.length();
			int len2 = str2.length();
			int size = Math.min(len1, len2);
			len1 -= len2;
			for(int i = 0; i < size; i ++){
				result = SORTING_ALPHABET.indexOf(str1.charAt(i)) - SORTING_ALPHABET.indexOf(str2.charAt(i));
				if(result != 0)
					break;
			}
			if(result == 0 && len1 != 0)
				result = len1;
			return result;
		};
	}

}
