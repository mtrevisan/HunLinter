package unit731.hunlinter.languages;

import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;


public class Orthography{

	private static final String WRONG_APOSTROPHES = "'‘ʼ";

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
		String correctedWord = word;
		if(StringUtils.containsAny(word, WRONG_APOSTROPHES)){
			final StringBuffer sb = new StringBuffer(word);
			IntStream.range(0, word.length())
				.filter(i -> WRONG_APOSTROPHES.contains(Character.toString(word.charAt(i))))
				.forEach(i -> sb.setCharAt(i, HyphenationParser.RIGHT_MODIFIER_LETTER_APOSTROPHE));
			correctedWord = sb.toString();
		}
		return correctedWord;
	}

	public boolean[] getSyllabationErrors(final List<String> syllabes){
		return new boolean[syllabes.size()];
	}

	public boolean hasSyllabationErrors(final List<String> syllabes){
		final boolean[] errors = getSyllabationErrors(syllabes);
		return IntStream.range(0, errors.length)
			.mapToObj(idx -> errors[idx])
			.anyMatch(error -> error);
	}

	public StringJoiner formatHyphenation(final List<String> syllabes, final StringJoiner sj, final Function<String, String> errorFormatter){
		final boolean[] errors = getSyllabationErrors(syllabes);
		for(int i = 0; i < syllabes.size(); i ++){
			final Function<String, String> fun = (errors[i]? errorFormatter: Function.identity());
			sj.add(fun.apply(syllabes.get(i)));
		}
		return sj;
	}

	public StringJoiner formatHyphenation(final List<String> syllabes){
		final StringJoiner sj = new StringJoiner(HyphenationParser.SOFT_HYPHEN);
		return formatHyphenation(syllabes, sj, Function.identity());
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
