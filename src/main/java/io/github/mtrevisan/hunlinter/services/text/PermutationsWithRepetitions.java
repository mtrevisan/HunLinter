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
package io.github.mtrevisan.hunlinter.services.text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * Generates permutations of {@code n} items taken <code>k</code> at a time, in lexicographic order, with repetitions
 *
 * @see <a href="https://github.com/dasanjos/java-examples/blob/master/src/main/java/com/dasanjos/java/util/math/PermutationWithRepetitionIterator.java">dasanjos/java-examples</a>
 * @see <a href="https://stackoverflow.com/questions/51946590/sequence-of-numbers-without-repeating-subsequent-digits">Sequence of numbers without repeating adjacent digits</a>
 */
public class PermutationsWithRepetitions implements Iterator<int[]>{

	private static final String ONE_ELEMENT_MINIMUM = "At least one element needed";
	private static final String MORE_THAN_ONE_MAX_COMPOUND_MINIMUM = "Number of maximum compounds must be greater than one or -1 (infinity)";
	private static final String ONE_OUTPUT_MINIMUM = "Output count must be greater than one";

	public static final int MAX_COMPOUNDS_INFINITY = -1;


	private final int n;
	private final int k;
	private final boolean forbidDuplicates;

	private long currentIndex;
	private long maximumIndex;


	/**
	 * @param n	the number of elements
	 * @param k	taken at most k at a time
	 * @param forbidDuplicates	Forbid duplicates
	 */
	public PermutationsWithRepetitions(final int n, final int k, final boolean forbidDuplicates){
		if(n < 1)
			throw new IllegalArgumentException(ONE_ELEMENT_MINIMUM);
		if(k != MAX_COMPOUNDS_INFINITY && k < 2)
			throw new IllegalArgumentException(MORE_THAN_ONE_MAX_COMPOUND_MINIMUM);

		this.n = n;
		this.k = k;
		this.forbidDuplicates = forbidDuplicates;

		currentIndex = 0l;
		maximumIndex = (long)Math.pow(n, k);
		if(forbidDuplicates){
			currentIndex ++;
			maximumIndex --;
		}
	}

	/**
	 * @param limit	Count limit for the results
	 * @return	Total permutations with repetitions of {@code n} elements taken <code>2…maxCompounds</code> at a time
	 */
	public List<int[]> permutations(final int limit){
		if(limit < 1)
			throw new IllegalArgumentException(ONE_OUTPUT_MINIMUM);

		final List<int[]> all = new ArrayList<>();
		for(int kk = 2; (k == MAX_COMPOUNDS_INFINITY || kk <= k) && all.size() < limit; kk ++)
			all.addAll(extractAllKPermutations(kk, all.size(), limit));
		return all;
	}

	private List<int[]> extractAllKPermutations(final int kk, final int currentCount, final int limit){
		final PermutationsWithRepetitions pr = new PermutationsWithRepetitions(n, kk, forbidDuplicates);

		final ArrayList<int[]> all = new ArrayList<>(limit);
		while(pr.hasNext() && all.size() + currentCount < limit)
			all.add(pr.next());
		all.trimToSize();
		return all;
	}

	@Override
	public boolean hasNext(){
		return (k == MAX_COMPOUNDS_INFINITY || currentIndex < maximumIndex);
	}

	@Override
	public int[] next(){
		if(!hasNext())
			throw new NoSuchElementException("No permutations left");

		final int[] result = convertBase(currentIndex, n);

		if(forbidDuplicates){
			boolean consecutiveDuplicates = true;
			while(consecutiveDuplicates && currentIndex < maximumIndex)
				//if next doesn't contain consecutive duplicates, break
				consecutiveDuplicates = hasConsecutiveDuplicates(++ currentIndex, n);
		}
		else
			currentIndex ++;

		return result;
	}

	/**
	 * Convert a number base 10 to another base
	 *
	 * @param decimalNumber	the input number (base 10)
	 * @param radix	the desired new number base
	 * @return	The array of integers representing decimal number on new base
	 */
	private int[] convertBase(long decimalNumber, final int radix){
		final int[] result = new int[k];
		for(int i = k - 1; decimalNumber != 0l; i --){
			result[i] = (int)(decimalNumber % radix);
			decimalNumber /= radix;
		}
		return result;
	}

	/** In {@code num = sum(i=0..k, a[i] * b^i)}, check if {@code a[i]} is equals to {@code a[i - 1] for i = 1..k}. */
	private boolean hasConsecutiveDuplicates(long decimalNumber, final int radix){
		int digit = -1;
		while(decimalNumber != 0l){
			final int newDigit = (int)(decimalNumber % radix);
			if(newDigit == digit)
				return true;

			decimalNumber /= radix;
			digit = newDigit;
		}
		return false;
	}

}
