package unit731.hunspeller.languages;

import java.util.List;
import java.util.regex.Matcher;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.PatternService;


@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Orthography{

	private static final Matcher MATCHER_APOSTROPHE = PatternService.matcher("['‘ʼ]");

	private static class SingletonHelper{
		private static final Orthography INSTANCE = new Orthography();
	}


	public static Orthography getInstance(){
		return SingletonHelper.INSTANCE;
	}

	public String correctOrthography(String word){
		//apply stress
		return correctApostrophes(word);
	}

	protected String correctApostrophes(String word){
		return PatternService.replaceAll(word, MATCHER_APOSTROPHE, HyphenationParser.APOSTROPHE);
	}

	public boolean[] getSyllabationErrors(List<String> syllabes){
		return new boolean[syllabes.size()];
	}

	public int countGraphemes(String word){
		return word.length();
	}

	public String markDefaultStress(String word){
		return word;
	}

	public boolean hasStressedGrapheme(String word){
		return false;
	}

}
