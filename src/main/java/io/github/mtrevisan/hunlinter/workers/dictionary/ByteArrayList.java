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
package io.github.mtrevisan.hunlinter.workers.dictionary;

import java.util.Arrays;
import java.util.Comparator;
import java.util.RandomAccess;


public class ByteArrayList implements RandomAccess{

	/**
	 * The array buffer into which the elements of the list are stored.
	 * The capacity of the ArrayList is the length of this array buffer.
	 */
	byte[][] data;
	/** The size of the ArrayList (the number of elements it contains). */
	private int size;
	private final float growthRate;


	ByteArrayList(final float growthRate){
		this(0, growthRate);
	}

	ByteArrayList(final int capacity, final float growthRate){
		data = new byte[capacity][];

		this.growthRate = growthRate;
	}

	/**
	 * Returns the number of elements in this list.
	 *
	 * @return	The number of elements in this list.
	 */
	public synchronized int size(){
		return size;
	}

	/**
	 * Appends the specified element to the end of this list.
	 *
	 * @param element	Element to be appended to this list.
	 */
	public synchronized void add(final byte[] element){
		if(size == data.length)
			grow(size + 1);

		data[size] = element;
		size ++;
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
	public synchronized void clear(){
		for(int i = 0; i < size; i ++)
			data[i] = null;
	}

	public synchronized void sort(final Comparator<? super byte[]> cmp){
		Arrays.sort(data, 0, size, cmp);
	}

	public synchronized void parallelSort(final Comparator<? super byte[]> cmp){
		Arrays.parallelSort(data, 0, size, cmp);
	}

	@Override
	public synchronized boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final ByteArrayList rhs = (ByteArrayList)obj;
		return (size == rhs.size
			&& Arrays.deepEquals(data, rhs.data));
	}

	@Override
	public synchronized int hashCode(){
		return 31 * size + Arrays.deepHashCode(data);
	}

}
