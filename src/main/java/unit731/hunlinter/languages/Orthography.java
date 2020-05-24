package unit731.hunlinter.languages;

import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;


public class Orthography{

	private static final String WRONG_APOSTROPHES = HyphenationParser.APOSTROPHE + "‘";
	private static final String CORRECT_APOSTROPHES = StringUtils.repeat(HyphenationParser.RIGHT_MODIFIER_LETTER_APOSTROPHE, WRONG_APOSTROPHES.length());

	private static class SingletonHelper{
		private static final Orthography INSTANCE = new Orthography();
	}


	protected Orthography(){}

	public static Orthography getInstance(){
		return SingletonHelper.INSTANCE;
	}

	public String correctOrthography(final String word){
		return correctApostrophes(word);
	}

	protected String correctApostrophes(final String word){
		return StringUtils.replaceChars(word, WRONG_APOSTROPHES, CORRECT_APOSTROPHES);
	}

	public boolean[] getSyllabationErrors(final String[] syllabes){
		return new boolean[syllabes.length];
	}

	public boolean hasSyllabationErrors(final String[] syllabes){
		final boolean[] errors = getSyllabationErrors(syllabes);
		return IntStream.range(0, errors.length)
			.mapToObj(idx -> errors[idx])
			.anyMatch(error -> error);
	}

	public StringJoiner formatHyphenation(final String[] syllabes){
		final StringJoiner sj = new StringJoiner(HyphenationParser.SOFT_HYPHEN);
		return formatHyphenation(syllabes, sj, Function.identity());
	}

	public StringJoiner formatHyphenation(final String[] syllabes, final StringJoiner sj, final Function<String, String> errorFormatter){
		final boolean[] errors = getSyllabationErrors(syllabes);
		for(int i = 0; i < syllabes.length; i ++){
			final Function<String, String> fun = (errors[i]? errorFormatter: Function.identity());
			sj.add(fun.apply(syllabes[i]));
		}
		return sj;
	}

	/**
	 * @param syllabes	The list of syllabes
	 * @return The 0-based index of the syllabe starting from the end
	 */
	public int getStressedSyllabeIndexFromLast(final String[] syllabes){
		return -1;
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
