package unit731.hunspeller.languages;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.PatternHelper;


public class Orthography{

	private static final Pattern PATTERN_APOSTROPHE = PatternHelper.pattern("['‘ʼ]");

	private static class SingletonHelper{
		private static final Orthography INSTANCE = new Orthography();
	}


	protected Orthography(){}

	public static Orthography getInstance(){
		return SingletonHelper.INSTANCE;
	}

	public String correctOrthography(String word){
		//apply stress
		return correctApostrophes(word);
	}

	protected String correctApostrophes(String word){
		return PatternHelper.replaceAll(word, PATTERN_APOSTROPHE, HyphenationParser.APOSTROPHE);
	}

	public boolean[] getSyllabationErrors(List<String> syllabes){
		return new boolean[syllabes.size()];
	}

	/**
	 * @param syllabes	The list of syllabes
	 * @return The 0-based index of the syllabe starting from the end
	 */
	public List<Integer> getStressIndexFromLast(List<String> syllabes){
		return Collections.<Integer>emptyList();
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
