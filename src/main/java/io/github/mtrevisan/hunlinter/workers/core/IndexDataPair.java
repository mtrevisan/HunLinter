/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.workers.core;


public final class IndexDataPair<T>{

	public static final IndexDataPair<Object> NULL_INDEX_DATA_PAIR = new IndexDataPair<>(-1, null);


	private final int index;
	private final T data;


	public static <T> IndexDataPair<T> of(final int index, final T data){
		return new IndexDataPair<>(index, data);
	}

	private IndexDataPair(final int index, final T data){
		this.index = index;
		this.data = data;
	}

	public int getIndex(){
		return index;
	}

	public T getData(){
		return data;
	}

	@Override
	public String toString(){
		return index + ": " + data;
	}

	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final IndexDataPair<?> rhs = (IndexDataPair<?>)obj;
		return (index == rhs.index
			&& data.equals(rhs.data));
	}

	@Override
	public int hashCode(){
		int result = Integer.hashCode(index);
		result = 31 * result + data.hashCode();
		return result;
	}

}
