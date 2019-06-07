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

	public String correctOrthography(final String word){
		//apply stress
		return correctApostrophes(word);
	}

	protected String correctApostrophes(final String word){
		return PatternHelper.replaceAll(word, PATTERN_APOSTROPHE, HyphenationParser.APOSTROPHE);
	}

	public boolean[] getSyllabationErrors(final List<String> syllabes){
		return new boolean[syllabes.size()];
	}

	/**
	 * @param syllabes	The list of syllabes
	 * @return The 0-based index of the syllabe starting from the end
	 */
	public List<Integer> getStressIndexFromLast(final List<String> syllabes){
		return Collections.emptyList();
	}

	public int countGraphemes(final String word){
		return word.length();
	}

	public String markDefaultStress(final String word){
		return word;
	}

	public boolean hasStressedGrapheme(final String word){
		return false;
	}

}
