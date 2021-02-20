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
package unit731.hunlinter.parsers.autocorrect;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Objects;


public class CorrectionEntry implements Comparable<CorrectionEntry>{

	private final String incorrectForm;
	private final String correctForm;


	public CorrectionEntry(final String incorrectForm, final String correctForm){
		Objects.requireNonNull(incorrectForm);
		Objects.requireNonNull(correctForm);

		this.incorrectForm = incorrectForm;
		this.correctForm = correctForm;
	}

	public String getIncorrectForm(){
		return incorrectForm;
	}

	public String getCorrectForm(){
		return correctForm;
	}

	@Override
	public String toString(){
		return ("\"" + incorrectForm + " -> " + correctForm + "\"");
	}

	@Override
	public int compareTo(final CorrectionEntry other){
		return new CompareToBuilder()
			.append(incorrectForm, other.incorrectForm)
			.append(correctForm, other.correctForm)
			.toComparison();
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final CorrectionEntry rhs = (CorrectionEntry)obj;
		return new EqualsBuilder()
			.append(incorrectForm, rhs.incorrectForm)
			.append(correctForm, rhs.correctForm)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(incorrectForm)
			.append(correctForm)
			.toHashCode();
	}

}
