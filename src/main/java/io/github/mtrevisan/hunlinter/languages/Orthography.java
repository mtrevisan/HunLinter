/**
 * Copyright (c) 2019-2022 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.languages;

import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenationParser;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;


public class Orthography{

	private static final String WRONG_APOSTROPHES = HyphenationParser.APOSTROPHE + "‘";
	private static final String CORRECT_APOSTROPHES = StringUtils.repeat(HyphenationParser.MODIFIER_LETTER_APOSTROPHE, WRONG_APOSTROPHES.length());

	private static class SingletonHelper{
		private static final Orthography INSTANCE = new Orthography();
	}


	protected Orthography(){}

	public static Orthography getInstance(){
		return SingletonHelper.INSTANCE;
	}

	@SuppressWarnings("DesignForExtension")
	public String correctOrthography(final String word){
		return correctApostrophes(word);
	}

	private static String correctApostrophes(final String word){
		return StringUtils.replaceChars(word, WRONG_APOSTROPHES, CORRECT_APOSTROPHES);
	}

	@SuppressWarnings("DesignForExtension")
	public boolean[] getSyllabationErrors(final List<String> syllabes){
		return new boolean[syllabes.size()];
	}

	public final boolean hasSyllabationErrors(final List<String> syllabes){
		final boolean[] errors = getSyllabationErrors(syllabes);
		for(int idx = 0; idx < errors.length; idx ++)
			if(errors[idx])
				return true;
		return false;
	}

	public final StringJoiner formatHyphenation(final List<String> syllabes){
		final StringJoiner sj = new StringJoiner(HyphenationParser.SOFT_HYPHEN);
		return formatHyphenation(syllabes, sj, Function.identity());
	}

	public final StringJoiner formatHyphenation(final List<String> syllabes, final StringJoiner sj,
			final Function<String, String> errorFormatter){
		final boolean[] errors = getSyllabationErrors(syllabes);
		for(int i = 0; i < syllabes.size(); i ++){
			final Function<String, String> fun = (errors[i]? errorFormatter: Function.identity());
			sj.add(fun.apply(syllabes.get(i)));
		}
		return sj;
	}

	/**
	 * @param syllabes	The list of syllabes
	 * @return The 0-based index of the syllabe starting from the end
	 */
	@SuppressWarnings("DesignForExtension")
	public int getStressedSyllabeIndexFromLast(final List<String> syllabes){
		return -1;
	}

	@SuppressWarnings("DesignForExtension")
	public int countGraphemes(final String word){
		return word.length();
	}

	@SuppressWarnings("DesignForExtension")
	public String markDefaultStress(final String word){
		return word;
	}

	@SuppressWarnings("DesignForExtension")
	public boolean hasStressedGrapheme(final String word){
		return false;
	}

}
