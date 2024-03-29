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
package io.github.mtrevisan.hunlinter.parsers.thesaurus;

import java.util.List;
import java.util.Objects;


public class DuplicationResult<T>{

	private final List<T> duplicates;
	private final boolean forceInsertion;


	public DuplicationResult(final List<T> duplicates, final boolean forceInsertion){
		Objects.requireNonNull(duplicates, "Duplicates cannot be null");

		this.duplicates = duplicates;
		this.forceInsertion = forceInsertion;
	}

	public final List<T> getDuplicates(){
		return duplicates;
	}

	public final boolean isForceInsertion(){
		return forceInsertion;
	}

	@Override
	public final boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final DuplicationResult<?> rhs = (DuplicationResult<?>)obj;
		return (forceInsertion == rhs.forceInsertion
			&& duplicates.equals(rhs.duplicates));
	}

	@Override
	public final int hashCode(){
		int result = duplicates.hashCode();
		result = 31 * result + Boolean.hashCode(forceInsertion);
		return result;
	}

}
