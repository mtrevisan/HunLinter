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

import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;

import java.util.Arrays;


public class LetterMatcherEntry{

	private final String messagePattern;
	private final Character masterLetter;
	private final String[] wrongFlags;
	private final String correctRule;


	public LetterMatcherEntry(final String messagePattern, final Character masterLetter, final String[] wrongFlags,
			final String correctRule){
		this.messagePattern = messagePattern;
		this.masterLetter = masterLetter;
		this.wrongFlags = wrongFlags;
		this.correctRule = correctRule;
	}

	public final void match(final Inflection inflection){
		if(!inflection.getContinuationFlags().isEmpty())
			for(int i = 0; i < wrongFlags.length; i ++)
				if(inflection.hasContinuationFlag(wrongFlags[i]))
					throw new LinterException(messagePattern, masterLetter, wrongFlags[i], correctRule);
	}

	@Override
	public final boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final LetterMatcherEntry rhs = (LetterMatcherEntry)obj;
		return (messagePattern.equals(rhs.messagePattern)
			&& masterLetter.equals(rhs.masterLetter)
			&& Arrays.equals(wrongFlags, rhs.wrongFlags)
			&& correctRule.equals(rhs.correctRule));
	}

	@Override
	public final int hashCode(){
		int result = (messagePattern == null? 0: messagePattern.hashCode());
		result = 31 * result + Character.hashCode(masterLetter);
		result = 31 * result + Arrays.hashCode(wrongFlags);
		result = 31 * result + correctRule.hashCode();
		return result;
	}

}
