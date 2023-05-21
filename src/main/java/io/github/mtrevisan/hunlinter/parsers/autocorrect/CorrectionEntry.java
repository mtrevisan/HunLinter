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
package io.github.mtrevisan.hunlinter.parsers.autocorrect;

import java.util.Objects;


public class CorrectionEntry implements Comparable<CorrectionEntry>{

	private final String incorrectForm;
	private final String correctForm;


	public CorrectionEntry(final String incorrectForm, final String correctForm){
		Objects.requireNonNull(incorrectForm, "Incorrect form cannot be null");
		Objects.requireNonNull(correctForm, "Correct form cannot be null");

		this.incorrectForm = incorrectForm;
		this.correctForm = correctForm;
	}

	public final String getIncorrectForm(){
		return incorrectForm;
	}

	public final String getCorrectForm(){
		return correctForm;
	}

	@Override
	public final String toString(){
		return ("\"" + incorrectForm + " -> " + correctForm + "\"");
	}

	@Override
	public final int compareTo(final CorrectionEntry other){
		final int comparison = incorrectForm.compareTo(other.incorrectForm);
		return (comparison == 0? correctForm.compareTo(other.correctForm): comparison);
	}

	@Override
	public final boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final CorrectionEntry rhs = (CorrectionEntry)obj;
		return (incorrectForm.equals(rhs.incorrectForm)
			&& correctForm.equals(rhs.correctForm));
	}

	@Override
	public final int hashCode(){
		int result = incorrectForm.hashCode();
		result = 31 * result + correctForm.hashCode();
		return result;
	}

}
