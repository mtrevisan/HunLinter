/**
 * Copyright (c) 2019-2021 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.services.sorters.externalsorter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.RandomAccess;


public class StringArrayList implements RandomAccess{

	public static final float GROWTH_RATE_DEFAULT = 1.2f;


	/**
	 * The array buffer into which the elements of the list are stored.
	 * The capacity of the ArrayList is the length of this array buffer.
	 */
	private String[] data;
	/** The size of the ArrayList (the number of elements it contains). */
	private int size;
	private final float growthRate;


	StringArrayList(final int capacity){
		this(capacity, GROWTH_RATE_DEFAULT);
	}

	StringArrayList(final int capacity, final float growthRate){
		data = new String[capacity];

		this.growthRate = growthRate;
	}

	/**
	 * Returns the number of elements in this list.
	 *
	 * @return	The number of elements in this list.
	 */
	public final synchronized int size(){
		return size;
	}

	/**
	 * Appends the specified element to the end of this list.
	 *
	 * @param element	Element to be appended to this list.
	 */
	public final synchronized void add(final String element){
		if(size == data.length)
			grow(size + 1);

		data[size] = element;
		size ++;
	}

	public final synchronized String get(final int index){
		return data[index];
	}

	/**
	 * Increases the capacity to ensure that it can hold at least the number of elements specified by the minimum capacity argument.
	 *
	 * @param minCapacity	The desired minimum capacity.
	 * @throws OutOfMemoryError	If `minCapacity` is less than zero.
	 */
	private void grow(final int minCapacity){
		final int delta = minCapacity - data.length;
		if(delta > 0)
			data = Arrays.copyOf(data, data.length + (int)Math.ceil(delta * growthRate));
	}

	/**
	 * Removes all the elements from this list.
	 * The list will be empty after this call returns.
	 */
	public final synchronized void clear(){
		for(int i = 0; i < size; i ++)
			data[i] = null;
	}

	public final synchronized void sort(final Comparator<? super String> comparator){
		Arrays.sort(data, 0, size, comparator);
	}

	public final synchronized void parallelSort(final Comparator<? super String> comparator){
		Arrays.parallelSort(data, 0, size, comparator);
	}

	@Override
	public final synchronized boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final StringArrayList rhs = (StringArrayList)obj;
		return (size == rhs.size
			&& Arrays.deepEquals(data, rhs.data));
	}

	@Override
	public final synchronized int hashCode(){
		return 31 * size + Arrays.deepHashCode(data);
	}

}
