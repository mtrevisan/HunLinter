package unit731.hunspeller.languages.vec;

import java.util.Comparator;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


public class Word{

	private static final String ALPHABET = "-/.0123456789aAàÀbBcCdDđĐeEéÉèÈfFgGhHiIíÍjJɉɈkKlLƚȽmMnNñÑoOóÓòÒpPrRsStTŧŦuUúÚvVxXʼ'";

	private static final Matcher LAST_STRESSED_VOWEL = PatternService.matcher("[aeiouàèéíòóú][^aeiouàèéíòóú]*$");

	private static final Matcher STRESSED = PatternService.matcher("[àèéíòóú]");
	private static final Matcher STRESSED_LAST = PatternService.matcher("[àèéíòóú]$");

	private static final Matcher DEFAULT_STRESS_GROUP = PatternService.matcher("(fr|[ln]|st)au$");
//	private static final Matcher LAST_UNSTRESSED_VOWEL = PatternService.matcher("[aeiou][^aeiou]*$");
	private static final Matcher LAST_UNSTRESSED_VOWEL = PatternService.matcher("[aeiou][^aeiou]*[^aàbcdđeéèfghiíjɉklƚmnñoóòprstŧuúvx]*$");

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


	public static int getLastVowelIndex(String word){
		Matcher m = LAST_STRESSED_VOWEL.reset(word);
		return (m.find()? m.start(): -1);
	}

	/*public static int getLastVowelIndex(String word, int idx){
		if(idx < 0)
			throw new IllegalArgumentException("The index should be non-negative, having " + idx);

		return getLastVowelIndex(word.substring(0, idx));
	}*/

	private static int getLastUnstressedVowelIndex(String word, int idx){
		Matcher m = LAST_UNSTRESSED_VOWEL.reset(idx < 0? word: word.substring(0, idx));
		return (m.find()? m.start(): -1);
	}


	public static boolean isStressed(String word){
		return PatternService.find(word, STRESSED);
	}

	private static int getIndexOfStress(String word){
		Matcher m = STRESSED.reset(word);
		return (m.find()? m.start(): -1);
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
		word = Normalizer.normalize(word, Normalizer.Form.NFD);
		return word.replaceAll("\\p{M}", "");
	};*/

	private static String suppressDefaultStress(String word){
		word = StringUtils.replace(word, "à", "a");
		word = StringUtils.replace(word, "é", "e");
		word = StringUtils.replace(word, "í", "i");
		word = StringUtils.replace(word, "ó", "o");
		return StringUtils.replace(word, "ú", "u");
	}

	private static char addStressAcute(char chr){
		return replaceCharacter(chr, "aeiou", "àéíóú");
	}

	/*private static char addStressGrave(char chr){
		return replaceCharacter(chr, "aeiou", "àèíòú");
	}*/

	/** Replaces a char in the 'from' string to the corresponding char in the 'to' string, returning the char itself if not in 'from'. */
	private static char replaceCharacter(char chr, String from, String to){
		return (chr + to).charAt(from.indexOf(chr) + 1);
	};


	private static String markDefaultStress(String word){
		int idx = getIndexOfStress(word);
		if(idx < 0){
			String phones = Grapheme.preConvertGraphemesIntoPhones(word);
			int lastChar = getLastUnstressedVowelIndex(phones, -1);

			//last vowel if the word ends with consonant, penultimate otherwise, default to the second vowel of a group of two (first one on a monosyllabe)
			if(Grapheme.endsInVowel(phones))
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
		return (idx >= 0? word.substring(0, idx) + addStressAcute(word.charAt(idx)) + word.substring(idx + 1): word);
	}

	public static String unmarkDefaultStress(String word){
		if(word == null)
			return null;

		int idx = getIndexOfStress(word);
		if(idx >= 0 && !PatternService.find(word, STRESSED_LAST)){
			String subword = word.substring(idx, idx + 2);
			String tmp = (!Grapheme.isDiphtong(subword) && !Grapheme.isHyatus(subword) && !PatternService.find(word, NO_STRESS)?
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
				result = ALPHABET.indexOf(str1.charAt(i)) - ALPHABET.indexOf(str2.charAt(i));
				if(result != 0)
					break;
			}
			if(result == 0 && len1 != 0)
				result = len1;
			return result;
		};
	}

}
