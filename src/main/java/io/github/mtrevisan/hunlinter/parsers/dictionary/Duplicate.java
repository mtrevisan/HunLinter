/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.parsers.dictionary;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;

import java.util.Objects;


public class Duplicate{

	private final Inflection inflection;
	private final String word;
	private final int lineIndex;


	public Duplicate(final Inflection inflection, final String word, final int lineIndex){
		Objects.requireNonNull(inflection, "Inflection cannot be null");
		Objects.requireNonNull(word, "Word cannot be null");

		this.inflection = inflection;
		this.word = word;
		this.lineIndex = lineIndex;
	}

	public Inflection getInflection(){
		return inflection;
	}

	public String getWord(){
		return word;
	}

	public int getLineIndex(){
		return lineIndex;
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final Duplicate rhs = (Duplicate)obj;
		return new EqualsBuilder()
			.append(lineIndex, rhs.lineIndex)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(lineIndex)
			.toHashCode();
	}

}
