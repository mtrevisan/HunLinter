package unit731.hunspeller.languages.vec;

import java.text.Normalizer;
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
	private static final char[] VOWELS_STRESSED_ARRAY = VOWELS_STRESSED.toCharArray();
	private static final String VOWELS_EXTENDED = VOWELS_PLAIN + VOWELS_STRESSED;
	public static final String CONSONANTS = "bBcCdDđĐfFgGhHjJɉɈkKlLƚȽmMnNñÑpPrRsStTŧŦvVxX";
	private static final String ALPHABET = CONSONANTS + VOWELS_EXTENDED;
	private static final String SORTING_ALPHABET = "-/.0123456789aAàÀbBcCdDđĐeEéÉèÈfFgGhHiIíÍjJɉɈkKlLƚȽmMnNñÑoOóÓòÒpPrRsStTŧŦuUúÚvVxXʼ'";

	private static final Matcher LAST_STRESSED_VOWEL = PatternService.matcher("[aeiouàèéíòóú][^aeiouàèéíòóú]*$");

	private static final char COMBINING_GRAVE_ACCENT = '\u0300';
	private static final char COMBINING_ACUTE_ACCENT = '\u0301';
	private static final String COMBINING_GRAVE_AND_ACUTE_ACCENTS = String.valueOf(COMBINING_GRAVE_ACCENT) + String.valueOf(COMBINING_ACUTE_ACCENT);

	private static final Matcher DEFAULT_STRESS_GROUP = PatternService.matcher("(fr|[ln]|st)au$");

	private static final String NO_STRESS_AVER = "^(r[ei])?g?(ar)?[àé]([lƚ][oaie]|[gmnstv]e|[mn]i|nt[ei]|s?t[ou])$";
	private static final String NO_STRESS_ESER = "^(r[ei])?((s[ae]r)?[àé]|[sx]é)([lƚ][oaie]|[gmnstv]e|[mn]i|nt[ei]|s?t[ou])$";
	private static final String NO_STRESS_DAR_FAR_STAR = "^((dex)?d|((dex)?asue|des|kon(tra)?|[lƚ]iku[ei]|putre|(ra)?re|sastu|sat[iu]s|sodis|sora|stra|stupe|tore|tume)?f|(kon(tra)?|mal|move|o|re|so(ra|to))?st)([ae]rà|[àé])([lƚ][oaie]|[gmnstv]e|[mn]i|nt[ei]|s?t[ou])$";
	private static final String NO_STRESS_SAVER = "^(p?re|stra)?(sà|sav?arà)([lƚ][oaie]|[gmnstv]e|[mn]i|nt[ei]|s?t[ou])$";
	private static final String NO_STRESS_ANDAR = "^(re)?v[àé]([lƚ][oaie]|[gmnstv]e|[mn]i|nt[ei]|s?t[ou])$";
	private static final String NO_STRESS_TRAER = "^(|as?|des?|es|kon|pro|re|so|sub?)?tr[àé]([lƚ][oaie]|[gmnstv]e|[mn]i|nt[ei]|s?t[ou])$";
	private static final Matcher NO_STRESS;
	static{
		StringJoiner sj = (new StringJoiner("|"))
			.add(NO_STRESS_AVER)
			.add(NO_STRESS_ESER)
			.add(NO_STRESS_DAR_FAR_STAR)
			.add(NO_STRESS_SAVER)
			.add(NO_STRESS_ANDAR)
			.add(NO_STRESS_TRAER);
		NO_STRESS = PatternService.matcher(sj.toString());
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

	/*public static int getLastVowelIndex(String word, int idx){
		if(idx < 0)
			throw new IllegalArgumentException("The index should be non-negative, having " + idx);

		return getLastVowelIndex(word.substring(0, idx));
	}*/

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

	//[àèéíòóú]$
	public static boolean isStressedLastGrapheme(String word){
		return ArrayUtils.contains(VOWELS_STRESSED_ARRAY, word.charAt(word.length() - 1));
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

	//[àèéíòóú]
	private static int getIndexOfStress(String word){
		for(int i = 0; i < word.length(); i ++)
			if(ArrayUtils.contains(VOWELS_STRESSED_ARRAY, word.charAt(i)))
				return i;
		return -1;
	}

/*	var isStressAcute = function(chr){
		return (chr && chr.match(/^[éíóú]$/));
	};

	var isStressGrave = function(chr){
		return (chr && chr.match(/^[àèò]$/));
	};

	var getStressType = function(chr){
		if(isStressAcute(chr))
			return 'acute';
		if(isStressGrave(chr))
			return 'grave';
		return undefined;
	};

	//NOTE: duplicated in Grapheme
	var suppressStress = function(word){
		word = normalize(word);
		return word.replaceAll("\\p{M}", StringUtils.EMPTY);
	};*/

//FIXME optimize! (remove normalize)
	private static String suppressDefaultStress(String word){
		String normalizedWord = normalize(word);
		normalizedWord = StringUtils.replaceChars(normalizedWord, COMBINING_GRAVE_AND_ACUTE_ACCENTS, null);
		return denormalize(normalizedWord);
	}

	private static String normalize(String word){
		return Normalizer.normalize(word, Normalizer.Form.NFD);
	}

	private static String denormalize(String word){
		return Normalizer.normalize(word, Normalizer.Form.NFC);
	}

	/*private static char addStressGrave(char chr){
		return replaceCharacter(chr, "aeiou", "àèíòú");
	}*/

	/** Replaces a char in the 'from' string to the corresponding char in the 'to' string, returning the char itself if not in 'from'. * /
	private static char replaceCharacter(char chr, String from, String to){
		return (chr + to).charAt(from.indexOf(chr) + 1);
	};*/


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

	//NOTE: is seems faster the current method
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

	//FIXME speed-up?
	public static String unmarkDefaultStress(String word){
		int idx = getIndexOfStress(word);
		if(idx >= 0 && !isStressedLastGrapheme(word)){
			String subword = word.substring(idx, idx + 2);
			String tmp = (!GraphemeVEC.isDiphtong(subword) && !GraphemeVEC.isHyatus(subword) && !PatternService.find(word, NO_STRESS)?
				suppressDefaultStress(word): word);
			if(!tmp.equals(word) && markDefaultStress(tmp).equals(word))
				word = tmp;
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
