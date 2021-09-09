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
package io.github.mtrevisan.hunlinter.services;

import io.github.mtrevisan.hunlinter.services.system.Memoizer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.function.Function;
import java.util.regex.Pattern;


public class RegexSequencer{

	private static final Pattern PATTERN = RegexHelper.pattern("(?<!\\[\\^?)(?![^\\[]*\\])");

	private static final String CLASS_START = "[";
	private static final String NEGATED_CLASS_START = CLASS_START + "^";

	private static final Function<String, String[]> SPLIT_SEQUENCE = Memoizer.memoize(seq -> (seq.isEmpty()? new String[0]: RegexHelper.split(seq, PATTERN)));


	public static String[] splitSequence(final String sequence){
		return SPLIT_SEQUENCE.apply(sequence);
	}

	public static String[] getEmptySequence(){
		return new String[0];
	}

	public static int length(final String[] sequence){
		return sequence.length;
	}

	/**
	 * Tests if this sequence starts with the specified prefix.
	 *
	 * @param sequence	The sequence.
	 * @param prefix	The prefix.
	 * @return	{@code true} if the sequence represented by the argument is a prefix of the sequence represented by this sequence; {@code false} otherwise.
	 *				Note also that {@code true} will be returned if the argument is an empty sequence or is equal to this {@code RadixTreeKey} object as
	 *				determined by the {@link #equals} method.
	 */
	public static boolean startsWith(final String[] sequence, final String[] prefix){
		final int count = prefix.length;
		if(count > sequence.length)
			return false;

		for(int i = 0; i < count; i ++)
			if(!matches(sequence[i], prefix[i]))
				return false;
		return true;
	}

	/**
	 * Tests if this sequence ends with the specified prefix.
	 *
	 * @param sequence	The sequence.
	 * @param suffix	The suffix.
	 * @return	{@code true} if the sequence represented by the argument is a suffix of the sequence represented by this sequence; {@code false} otherwise.
	 *				Note also that {@code true} will be returned if the argument is an empty sequence or is equal to this {@code RadixTreeKey} object as
	 *				determined by the {@link #equals} method.
	 */
	public static boolean endsWith(final String[] sequence, final String[] suffix){
		final int count = suffix.length;
		if(count > sequence.length)
			return false;

		for(int i = 1; i <= count; i ++)
			if(!matches(sequence[sequence.length - i], suffix[suffix.length - i]))
				return false;
		return true;
	}

	/**
	 * Returns a sequence that is a subsequence of this sequence.
	 * The subsequence begins at the specified {@code beginIndex} and extends to the end of the sequence.
	 *
	 * @param sequence	The sequence.
	 * @param index	The index.
	 * @return	The specified `character` at index `index`.
	 * @exception IndexOutOfBoundsException	If the {@code beginIndex} is negative, or the end of the sequence is larger than the length of
	 *														this sequence, or {@code beginIndex} is larger than the length of the sequence.
	 */
	public static String[] characterAt(final String[] sequence, final int index){
		return subSequence(sequence, index, index + 1);
	}

	/**
	 * Returns a sequence that is a subsequence of this sequence.
	 * The subsequence begins at the specified {@code beginIndex} and extends to the end of the sequence.
	 *
	 * @param sequence	The sequence.
	 * @param beginIndex	The beginning index, inclusive.
	 * @return	The specified substring.
	 * @exception IndexOutOfBoundsException	If the {@code beginIndex} is negative, or the end of the sequence is larger than the length of
	 *														this sequence, or {@code beginIndex} is larger than the length of the sequence.
	 */
	public static String[] subSequence(final String[] sequence, final int beginIndex){
		return (beginIndex > 0? subSequence(sequence, beginIndex, length(sequence)): sequence);
	}

	public static boolean equals(final String[] sequenceA, final String[] sequenceB){
		if(sequenceA.length != sequenceB.length)
			return false;

		for(int i = 0; i < sequenceA.length; i ++)
			if(!matches(sequenceA[i], sequenceB[i]))
				return false;
		return true;
	}

	public static boolean equalsAtIndex(final String[] sequenceA, final String[] sequenceB, final int indexA, final int indexB){
		return matches(sequenceA[indexA], sequenceB[indexB]);
	}

	private static boolean matches(final String fieldA, final String fieldB){
		final boolean response;
		final boolean fieldAHasClassStart = fieldA.startsWith(CLASS_START);
		final boolean fieldBHasClassStart = fieldB.startsWith(CLASS_START);
		if(!fieldAHasClassStart && fieldBHasClassStart)
			response = (fieldB.startsWith(NEGATED_CLASS_START) ^ fieldB.contains(fieldA));
		else if(!fieldBHasClassStart)
			response = (fieldA.startsWith(NEGATED_CLASS_START) ^ fieldA.contains(fieldB));
		else
			response = fieldA.equals(fieldB);
		return response;
	}

	/**
	 * Returns a sequence that is a subsequence of this sequence.
	 * The subsequence begins at the specified {@code beginIndex} and extends to the character at index {@code endIndex - 1}.
	 *
	 * @param sequence	The sequence.
	 * @param beginIndex	The beginning index, inclusive.
	 * @param endIndex	The ending index, exclusive.
	 * @return	The specified substring.
	 * @exception IndexOutOfBoundsException	If the {@code beginIndex} is negative, or {@code endIndex} is larger than the length of
	 *														this sequence, or {@code beginIndex} is larger than {@code endIndex}.
	 */
	public static String[] subSequence(final String[] sequence, final int beginIndex, final int endIndex){
		return ArrayUtils.subarray(sequence, beginIndex, endIndex);
	}

	public static String[] concat(final String[] sequenceA, final String[] sequenceB){
		return (sequenceA.length > 0? ArrayUtils.addAll(sequenceA, sequenceB): sequenceB);
	}

	public static String[] reverse(final String[] sequence){
		final String[] reverse = Arrays.copyOf(sequence, sequence.length);
		ArrayUtils.reverse(reverse);
		return reverse;
	}

	public static String toString(final String[] sequence){
		return StringUtils.join(sequence, StringUtils.EMPTY);
	}

}
