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
package io.github.mtrevisan.hunlinter.services.text;

import io.github.mtrevisan.hunlinter.workers.exceptions.LinterIllegalArgumentException;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;
import java.util.stream.IntStream;


/**
 * The hamming distance between two strings of equal length is the number of positions at which the corresponding symbols are different.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Hamming_distance">Hamming distance</a>
 */
public final class HammingDistance{

	private static final String DIFFERENT_LENGTHS = "Strings `{}` and `{}` must have the same length";


	private HammingDistance(){}

	/**
	 * Find the Hamming Distance between two strings with the same length.
	 *
	 * <p>The distance starts with zero, and for each occurrence of a different character in either String, it increments the distance
	 * by 1, and finally return its value.</p>
	 *
	 * <p>Since the Hamming Distance can only be calculated between strings of equal length, input of different lengths
	 * will throw IllegalArgumentException</p>
	 *
	 * <pre>
	 * distance.apply("", "")               = 0
	 * distance.apply("pappa", "pappa")     = 0
	 * distance.apply("1011101", "1011111") = 1
	 * distance.apply("ATCG", "ACCC")       = 2
	 * distance.apply("karolin", "kerstin"  = 3
	 * </pre>
	 *
	 * @param left	the first CharSequence, must not be {@code null}
	 * @param right	the second CharSequence, must not be {@code null}
	 * @return	the hamming distance between the given strings
	 * @throws IllegalArgumentException	if either input is {@code null} or if they do not have the same length
	 */
	public static int getDistance(final CharSequence left, final CharSequence right){
		Objects.requireNonNull(left, "Left cannot be null");
		Objects.requireNonNull(right, "Right cannot be null");
		if(left.length() != right.length())
			throw new LinterIllegalArgumentException(DIFFERENT_LENGTHS, left, right);

		return (int)IntStream.range(0, left.length())
			.filter(idx -> left.charAt(idx) != right.charAt(idx))
			.count();
	}

	public static Pair<Character, Character> findFirstDifference(final CharSequence left, final CharSequence right){
		return findFirstDifference(left, right, 0);
	}

	public static Pair<Character, Character> findFirstDifference(final CharSequence left, final CharSequence right, final int offset){
		Objects.requireNonNull(left, "Left cannot be null");
		Objects.requireNonNull(right, "Right cannot be null");
		if(left.length() != right.length())
			throw new LinterIllegalArgumentException(DIFFERENT_LENGTHS, left, right);

		boolean found = false;
		char chrLeft = 0;
		char chrRight = 0;
		for(int i = offset; i < left.length(); i ++){
			chrLeft = left.charAt(i);
			chrRight = right.charAt(i);
			if(chrLeft != chrRight){
				found = true;
				break;
			}
		}
		return (found? Pair.of(chrLeft, chrRight): null);
	}

}
