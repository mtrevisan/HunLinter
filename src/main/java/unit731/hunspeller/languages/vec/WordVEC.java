package unit731.hunspeller.languages.vec;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class WordVEC{

	private static final String SLASH_TAB = "/\t";

	private static final String VOWELS_PLAIN = "aAeEiIoOuU" + GraphemeVEC.I_UMLAUT_PHONEME;
	private static final String VOWELS_STRESSED = "àÀéÉèÈíÍóÓòÒúÚ";
	private static final String VOWELS_UNSTRESSED = "aAeEeEiIoOoOuU";
	private static final String CONSONANTS = "bBcCdDđĐfFgGhHjJɉɈkKlLƚȽmMnNñÑpPrRsStTŧŦvVxX";

	private static final char[] VOWELS_PLAIN_ARRAY = VOWELS_PLAIN.toCharArray();
	private static final char[] VOWELS_STRESSED_ARRAY = VOWELS_STRESSED.toCharArray();
	private static final char[] VOWELS_EXTENDED_ARRAY = (VOWELS_PLAIN + VOWELS_STRESSED).toCharArray();
	private static final char[] CONSONANTS_ARRAY = CONSONANTS.toCharArray();
	static{
		Arrays.sort(VOWELS_PLAIN_ARRAY);
		Arrays.sort(VOWELS_STRESSED_ARRAY);
		Arrays.sort(VOWELS_EXTENDED_ARRAY);
		Arrays.sort(CONSONANTS_ARRAY);
	}

	private static final String COLLATOR_RULE = ", ʼ=''' , '-'='‒' & '-'='–' < 0 < 1 < 2 < 3 < 4 < 5 < 6 < 7 < 8 < 9 < '/' < a,A < à,À < b,B < c,C < d,D < đ=dh,Đ=Dh < e,E < é,É < è,È < f,F < g,G < h,H < i,I < í,Í < j,J < ɉ=jh,Ɉ=Jh < k,K < l,L < ƚ=lh,Ƚ=Lh < m,M < n,N < ñ=nh,Ñ=Nh < o,O < ó,Ó < ò,Ò < p,P < r,R < s,S < t,T < ŧ=th,Ŧ=Th < u,U < ú,Ú < v,V < x,X";
	private static Collator COLLATOR;
	static{
		try{
			COLLATOR = new RuleBasedCollator(COLLATOR_RULE);
		}
		catch(ParseException e){
			//cannot happen
			log.error(e.getMessage());
		}
	}
	private static final Matcher REGEX_REMOVE_RULE_FLAGS = PatternService.matcher("(/[^\t]+)?\t");

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

	public static boolean isConsonant(char chr){
		return (Arrays.binarySearch(CONSONANTS_ARRAY, chr) >= 0);
	}

	//[aeiouàèéíòóú][^aàbcdđeéèfghiíjɉklƚmnñoóòprsʃtŧuúvxʒ]*$
	private static boolean endsInVowel(String word){
		int i = word.length();
		while((-- i) >= 0){
			char chr = word.charAt(i);
			if(chr != 'ʼ' && chr != '\'')
				return (Arrays.binarySearch(VOWELS_EXTENDED_ARRAY, chr) >= 0);
		}
		return false;
	}

	public static int getLastVowelIndex(String word){
		Matcher m = LAST_STRESSED_VOWEL.reset(word);
		return (m.find()? m.start(): -1);
	}

	//[aeiou][^aeiou]*$
	private static int getLastUnstressedVowelIndex(String word, int idx){
		int i = (idx >= 0? idx: word.length());
		while((-- i) >= 0){
			char chr = word.charAt(i);
			if(Arrays.binarySearch(VOWELS_PLAIN_ARRAY, chr) >= 0)
				return i;
		}
		return -1;
	}


	//[àèéíòóú]
	public static boolean isStressed(String word){
		return (countAccents(word) == 1);
	}

	public static int countAccents(String word){
		int count = 0;
		for(int i = 0; i < word.length(); i ++)
			if(Arrays.binarySearch(VOWELS_STRESSED_ARRAY, word.charAt(i)) >= 0)
				count ++;
		return count;
	}

	private static String suppressStress(String word){
		return StringUtils.replaceChars(word, VOWELS_STRESSED, VOWELS_UNSTRESSED);
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

	public static String markDefaultStress(String word){
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

	public static String unmarkDefaultStress(String word){
		int idx = getIndexOfStress(word);
		//check if the word have a stress and this is not on the last letter
		if(idx >= 0 && idx < word.length() - 1){
			String subword = word.substring(idx, idx + 2);
//FIXME is there a way to optimize this PatternService.find?
			if(!GraphemeVEC.isDiphtong(subword) && !GraphemeVEC.isHyatus(subword) && !PatternService.find(word, PREVENT_UNMARK_STRESS)){
				String tmp = suppressStress(word);
				if(!tmp.equals(word) && markDefaultStress(tmp).equals(word))
					word = tmp;
			}
		}
		return word;
	}

	private static int getIndexOfStress(String word){
		for(int i = 0; i < word.length(); i ++)
			if(ArrayUtils.contains(VOWELS_STRESSED_ARRAY, word.charAt(i)))
				return i;
		return -1;
	}

	public static Comparator<String> sorterComparator(){
		return (str1, str2) -> {
			str1 = PatternService.replaceAll(str1, REGEX_REMOVE_RULE_FLAGS, SLASH_TAB);
			str2 = PatternService.replaceAll(str2, REGEX_REMOVE_RULE_FLAGS, SLASH_TAB);
			return COLLATOR.compare(str1, str2);
		};
	}

}
