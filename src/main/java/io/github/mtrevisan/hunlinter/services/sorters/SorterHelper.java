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
package io.github.mtrevisan.hunlinter.services.sorters;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Collection;
import java.util.HashSet;


public final class SorterHelper{

	private SorterHelper(){}

	public static <T> void swap(final T[] data, final int i, final int j){
		final T temp = data[i];
		data[i] = data[j];
		data[j] = temp;
	}

	/* Assume the array is already sorted! */
	public static <T extends Comparable<? super T>> T[] removeDuplicates(final T[] array){
		return removeDuplicates(array, 0, array.length);
	}

	/* Assume the array is already sorted! */
	public static <T extends Comparable<? super T>> T[] removeDuplicates(final T[] array, final int low, final int high){
		//fetch all the duplicates
		final Collection<T> set = new HashSet<>(high - low);
		final int[] indexes = new int[high - low];
		int offset = 0;
		for(int i = low; i < high; i ++)
			if(!set.add(array[i]))
				indexes[offset ++] = i;
		final int[] tmp = new int[offset];
		System.arraycopy(indexes, 0, tmp, 0, offset);
		return ArrayUtils.removeAll(array, tmp);
	}

}
