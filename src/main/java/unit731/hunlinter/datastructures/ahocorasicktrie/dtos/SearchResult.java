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
package unit731.hunlinter.datastructures.ahocorasicktrie.dtos;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


/**
 * A result output
 *
 * @param <V>	The type of values stored in the tree
 */
public class SearchResult<V>{

	/** the beginning index, inclusive */
	private final int start;
	/** the ending index, exclusive */
	private final int end;
	/** the value assigned to the keyword */
	private final V value;


	public SearchResult(final int begin, final int end, final V value){
		this.start = begin;
		this.end = end;
		this.value = value;
	}

	public int getIndexBegin(){
		return start;
	}

	public int getIndexEnd(){
		return end;
	}

	public int getMatchLength(){
		return end - start;
	}

	public V getValue(){
		return value;
	}

	@Override
	public String toString(){
		return "[" + start + ":" + end + "] = " + value;
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final SearchResult<?> rhs = (SearchResult<?>)obj;
		return new EqualsBuilder()
			.append(start, rhs.start)
			.append(end, rhs.end)
			.append(value, rhs.value)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(start)
			.append(end)
			.append(value)
			.toHashCode();
	}

}
