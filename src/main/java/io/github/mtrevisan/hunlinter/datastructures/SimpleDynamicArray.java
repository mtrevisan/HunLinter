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
package io.github.mtrevisan.hunlinter.datastructures;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.Predicate;


public class SimpleDynamicArray<T>{

	private static final float GROWTH_RATE_DEFAULT = 1.2f;


	public T[] data;
	public int limit;

	private final float growthRate;


	public static <T> SimpleDynamicArray<T> wrap(final T[] array){
		final SimpleDynamicArray<T> wrapped = new SimpleDynamicArray<>(array, GROWTH_RATE_DEFAULT);
		wrapped.limit = array.length;
		return wrapped;
	}

	public static <T> SimpleDynamicArray<T> create(final Class<T> type){
		return new SimpleDynamicArray<>(type, 0, GROWTH_RATE_DEFAULT);
	}

	public static <T> SimpleDynamicArray<T> create(final Class<T> type, final int capacity){
		return new SimpleDynamicArray<>(type, capacity, GROWTH_RATE_DEFAULT);
	}

	public static <T> SimpleDynamicArray<T> create(final Class<T> type, final int capacity, final float growthRate){
		return new SimpleDynamicArray<>(type, capacity, growthRate);
	}


	@SuppressWarnings("unchecked")
	private SimpleDynamicArray(final Class<T> type, final int capacity, final float growthRate){
		this((T[])Array.newInstance(type, capacity), growthRate);
	}

	private SimpleDynamicArray(final T[] array, final float growthRate){
		data = array;

		this.growthRate = growthRate;
	}

	/**
	 * Appends the specified element to the end of this array.
	 *
	 * @param elem	Element to be appended to the internal array.
	 */
	public void add(final T elem){
		grow(1);

		data[limit ++] = elem;
	}

	/**
	 * Appends the specified element to the end of this array if not {@code null}.
	 *
	 * @param elem	Element to be appended to the internal array.
	 */
	public void addIfNotNull(final T elem){
		if(elem != null)
			add(elem);
	}

	public void addAll(final T[] array){
		addAll(array, array.length);
	}

	/**
	 * Appends all the elements in the specified collection to the end of this array.
	 *
	 * @param array	Collection containing elements to be added to this array.
	 */
	public void addAll(final SimpleDynamicArray<T> array){
		addAll(array.data, array.limit);
	}

	/**
	 * Inserts all the elements in the specified collection into this array at the specified position.
	 * <p>Shifts the element currently at that position (if any) and any subsequent elements to the right
	 * (increases their indices).</p>
	 *
	 * @param index	Index at which to insert the first element from the specified collection.
	 * @param array	Collection containing elements to be added to this array.
	 */
	public void addAll(final int index, final SimpleDynamicArray<T> array){
		final int addLength = array.limit;
		if(addLength != 0){
			grow(addLength);

			if(index < limit)
				System.arraycopy(data, index, data, index + addLength, limit - index);
			System.arraycopy(array.data, 0, data, index, addLength);
			limit += addLength;
		}
	}

	/**
	 * Appends all the elements in the specified collection to the end of this array.
	 *
	 * @param array	Collection containing elements to be added to this array.
	 * @param length	Length of the array.
	 */
	public void addAll(final T[] array, final int length){
		grow(length);

		System.arraycopy(array, 0, data, limit, length);
		limit += length;
	}

	public void addAllUnique(final T[] array){
		addAllUnique(array, array.length);
	}

	public void addAllUnique(final SimpleDynamicArray<T> array){
		addAllUnique(array.data, array.limit);
	}

	private void addAllUnique(final T[] array, final int size){
		grow(size);

		for(int i = 0; i < size; i ++)
			if(!contains(array[i]))
				data[limit ++] = array[i];
	}

	public boolean contains(final T elem){
		return (indexOf(elem) >= 0);
	}

	public void remove(final T elem){
		int index = limit;
		while(limit > 0 && (index = lastIndexOf(elem, index)) >= 0){
			final int delta = limit - index - 1;
			if(delta > 0)
				System.arraycopy(data, index + 1, data, index, delta);
			data[-- limit] = null;
		}
	}

	public void removeAtIndex(final int index){
		data = ArrayUtils.remove(data, index);
		limit --;
	}

	public void removeIf(final Predicate<T> filter){
		removeIf(filter, 0);
	}

	public void removeIf(final Predicate<T> filter, final int startIndex){
		int index = indexOf(filter, startIndex);
		if(index >= 0){
			final int[] indices = new int[limit - index];
			indices[0] = index;

			int count;
			for(count = 1; (index = indexOf(filter, indices[count - 1] + 1)) != -1; indices[count ++] = index){}

			data = ArrayUtils.removeAll(data, Arrays.copyOf(indices, count));
			limit -= count;
		}
	}

	public void filter(final Predicate<? super T> filter){
		reset();

		for(final T elem : data)
			if(filter.test(elem))
				data[limit ++] = elem;
	}

	private int indexOf(final Predicate<T> filter, final int startIndex){
		for(int i = startIndex; i < limit; i ++)
			if(filter.test(data[i]))
				return i;
		return -1;
	}

	public int indexOf(final T elem){
		return indexOf(elem, 0);
	}

	public int indexOf(final T elem, final int startIndex){
		return (elem != null
			? indexOfNonNull(elem, startIndex)
			: indexOfNull(startIndex));
	}

	private int indexOfNull(final int startIndex){
		for(int i = startIndex; i < data.length; i ++)
			if(data[i] == null)
				return i;
		return -1;
	}

	private int indexOfNonNull(final T elem, final int startIndex){
		for(int i = startIndex; i < data.length; i ++)
			if(elem.equals(data[i]))
				return i;
		return -1;
	}

	public int lastIndexOf(final T elem, final int startIndex){
		return (elem != null
			? lastIndexOfNonNull(elem, startIndex)
			: lastIndexOfNull(startIndex));
	}

	private int lastIndexOfNull(final int startIndex){
		for(int i = startIndex - 1; i >= 0; i --)
			if(data[i] == null)
				return i;
		return -1;
	}

	private int lastIndexOfNonNull(final T elem, final int startIndex){
		for(int i = startIndex - 1; i >= 0; i --)
			if(elem.equals(data[i]))
				return i;
		return -1;
	}

	private void grow(final int size){
		final int delta = limit - data.length + size;
		if(delta > 0){
			final int newLength = data.length + (int)Math.ceil(delta * growthRate);
			final T[] copy = newInstance(newLength);
			System.arraycopy(data, 0, copy, 0, Math.min(data.length, newLength));
			data = copy;
		}
	}

	@SuppressWarnings("unchecked")
	private T[] newInstance(final int size){
		final Class<?> type = getDataType();
		return (T[])Array.newInstance(type, size);
	}

	private Class<?> getDataType(){
		return data.getClass().getComponentType();
	}

	public void truncate(int size){
		if(size < limit)
			limit = size;
	}

	public SimpleDynamicArray<T> collectIf(final Predicate<T> condition){
		@SuppressWarnings("unchecked")
		final SimpleDynamicArray<T> collect = SimpleDynamicArray.create((Class<T>)getDataType(), limit, growthRate);
		for(int i = 0; i < limit; i ++){
			final T elem = data[i];
			if(condition.test(elem))
				collect.add(elem);
		}
		return collect;
	}

	public boolean isEmpty(){
		return (limit == 0);
	}

	/**
	 * NOTE: this method should NOT be called at all because it is inefficient
	 *
	 * @return	A copy of the array
	 */
	public T[] extractCopyOrNull(){
		if(isEmpty())
			return null;

		return extractCopy();
	}

	/**
	 * NOTE: this method should NOT be called at all because it is inefficient
	 *
	 * @return	A copy of the array
	 */
	public T[] extractCopy(){
		final T[] reducedData = newInstance(limit);
		System.arraycopy(data, 0, reducedData, 0, limit);
		return reducedData;
	}

	public void reset(){
		limit = 0;
	}

	public void clear(){
		data = null;
		limit = -1;
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final SimpleDynamicArray<?> rhs = (SimpleDynamicArray<?>)obj;
		return (limit == rhs.limit && Arrays.equals(data, rhs.data));
	}

	@Override
	public int hashCode(){
		return Integer.hashCode(limit) ^ Arrays.hashCode(data);
	}

}
