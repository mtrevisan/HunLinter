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
package unit731.hunlinter.datastructures;

import org.apache.commons.lang3.ArrayUtils;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static unit731.hunlinter.services.system.LoopHelper.match;


public class ArraySet<E> extends AbstractSet<E> implements Cloneable, Serializable{

	private static final long serialVersionUID = -5730118321825456724L;


	public static class ArrayIterator<T> implements Iterator<T>{

		private Object[] values;
		//position of next item to return
		int offset;

		ArrayIterator(final Object[] values){
			this.values = values;
		}

		@Override
		public boolean hasNext(){
			return (offset < values.length);
		}

		@Override
		@SuppressWarnings("unchecked")
		public T next(){
			if(offset == values.length)
				throw new NoSuchElementException();

			return (T)values[offset ++];
		}

		@Override
		public void remove(){
			values = (values.length == 1? EMPTY_ARRAY: ArrayUtils.remove(values, -- offset));
		}
	}


	private static final Object[] EMPTY_ARRAY = new Object[0];


	private Object[] values = EMPTY_ARRAY;


	@Override
	public Object[] toArray(){
		return values;
	}

	@Override
	@SuppressWarnings("MethodDoesntCallSuperMethod")
	public ArraySet<E> clone(){
		final ArraySet<E> ret = new ArraySet<>();
		ret.values = (Arrays.equals(values, EMPTY_ARRAY)? EMPTY_ARRAY: values.clone());
		return ret;
	}

	@Override
	public void clear(){
		values = EMPTY_ARRAY;
	}

	@Override
	public boolean isEmpty(){
		return (values.length == 0);
	}

	@Override
	public Iterator<E> iterator(){
		return new ArrayIterator<>(values);
	}

	@Override
	public int size(){
		return values.length;
	}

	@Override
	public boolean add(final E value){
		final int n = values.length;
		for(final Object o : values)
			if(o.equals(value))
				return false;

		final Object[] newValues = new Object[n + 1];
		System.arraycopy(values, 0, newValues, 0, n);
		newValues[n] = value;
		values = newValues;
		return true;
	}

	public boolean addAll(final E[] values){
		boolean modified = false;
		for(final E value : values)
			if(add(value))
				modified = true;
		return modified;
	}

	@Override
	public boolean addAll(final Collection<? extends E> values){
		boolean modified = false;
		for(final E value : values)
			if(add(value))
				modified = true;
		return modified;
	}

	@Override
	public boolean contains(final Object value){
		return (match(values, v -> v.equals(value)) != null);
	}

}
