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
package io.github.mtrevisan.hunlinter.languages.vec;

import io.github.mtrevisan.hunlinter.services.RegexHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


final class GraphemeVEC{

	static final String GRAPHEME_D_STROKE = "đ";
	static final String GRAPHEME_H = "h";
	static final String GRAPHEME_J = "j";
	static final String GRAPHEME_I = "i";
	static final String GRAPHEME_L = "l";
	static final String GRAPHEME_L_STROKE = "ƚ";
	static final String GRAPHEME_S = "s";
	static final String GRAPHEME_T_STROKE = "ŧ";
	static final String GRAPHEME_X = "x";

	private static final Pattern DIPHTONG1 = RegexHelper.pattern("[àèéíòóú][aeoiu]");
	private static final Pattern DIPHTONG2 = RegexHelper.pattern("[aeo][aeo]");
	private static final Pattern HYATUS = RegexHelper.pattern("[aeïoü][aeiouàèéíòóú]|[iu][íú]");

	private static final Pattern ETEROPHONIC_SEQUENCE = RegexHelper.pattern("(?:^|[^aeiouàèéíòóú])[iju][àèéíòóú]");


	private GraphemeVEC(){}

	public static boolean isDiphtong(final CharSequence word){
		if(RegexHelper.find(word, DIPHTONG1))
			return true;

		final Matcher m = RegexHelper.matcher(word, DIPHTONG2);
		return (m.find() && m.start() != WordVEC.getIndexOfStress(word));
	}

	public static boolean isHyatus(final CharSequence word){
		return RegexHelper.find(word, HYATUS);
	}

	public static boolean isEterophonicSequence(final CharSequence group){
		return RegexHelper.find(group, ETEROPHONIC_SEQUENCE);
	}

}
