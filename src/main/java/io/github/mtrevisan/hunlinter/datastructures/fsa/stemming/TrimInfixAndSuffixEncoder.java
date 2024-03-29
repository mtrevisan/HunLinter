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
package io.github.mtrevisan.hunlinter.datastructures.fsa.stemming;

import io.github.mtrevisan.hunlinter.services.text.ArrayHelper;

import java.nio.ByteBuffer;


/**
 * Encodes {@code target} relative to {@code source} by trimming whatever
 * non-equal suffix and infix {@code source} and {@code target} have. The
 * output code is (bytes):
 *
 * <pre>
 * {X}{L}{K}{suffix}
 * </pre>
 *
 * where {@code source}'s infix at position ({@code X} - 'A') and of
 * length ({@code L} - 'A') should be removed, then ({@code K} -
 * 'A') bytes should be trimmed from the end and then the {@code suffix}
 * should be appended to the resulting byte sequence.
 *
 * <p>
 * Examples:
 * </p>
 *
 * <pre>
 * source:	ayz
 * target:	abc
 * encoded:	AACbc
 *
 * source:	aillent
 * target:	aller
 * encoded:	BBCr
 * </pre>
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class TrimInfixAndSuffixEncoder implements SequenceEncoderInterface{

	@Override
	public final byte[] encode(final byte[] source, final byte[] target){
		//FIXME
		//Search for the infix that can be encoded and remove from `source` to get a maximum-length prefix of `target`.
		//This could be done more efficiently by running a smarter longest-common-subsequence algorithm and some pruning (?).
		//For now, naïve loop should do.

		//There can be only two positions for the infix to delete:
		//	1) we remove leading bytes, even if they are partially matching (but a longer match exists somewhere later on)
		//	2) we leave maximum matching prefix and remove non-matching bytes that follow
		int maxInfixIndex = 0;
		int maxSubsequenceLength = ArrayHelper.longestCommonPrefix(source, target);
		int maxInfixLength = 0;
		ByteBuffer scratch = ByteBuffer.allocate(0);
		for(final int i : new int[]{0, maxSubsequenceLength})
			for(int j = 1; j <= source.length - i; j ++){
				//compute temporary `source` with the infix removed
				//concatenate in scratch space for simplicity
				final int len2 = source.length - (i + j);
				scratch = clearAndEnsureCapacity(scratch, i + len2);
				scratch.put(source, 0, i);
				scratch.put(source, i + j, len2);

				final int sharedPrefix = ArrayHelper.longestCommonPrefix(scratch, target);

				//only update `maxSubsequenceLength` if we will be able to encode it
				if(sharedPrefix > 0 && sharedPrefix > maxSubsequenceLength && i < REMOVE_EVERYTHING && j < REMOVE_EVERYTHING){
					maxSubsequenceLength = sharedPrefix;
					maxInfixIndex = i;
					maxInfixLength = j;
				}
			}

		int truncateSuffixBytes = source.length - (maxInfixLength + maxSubsequenceLength);

		//special case: if we're removing the suffix in the infix code, move it to the suffix code instead
		if(truncateSuffixBytes == 0 && maxInfixIndex + maxInfixLength == source.length){
			truncateSuffixBytes = maxInfixLength;
			maxInfixLength = 0;
			maxInfixIndex = 0;
		}

		if(truncateSuffixBytes >= REMOVE_EVERYTHING){
			maxSubsequenceLength = 0;
			maxInfixIndex = 0;
			truncateSuffixBytes = REMOVE_EVERYTHING;
			maxInfixLength = REMOVE_EVERYTHING;
		}

		final int len1 = target.length - maxSubsequenceLength;
		final byte[] encoded = new byte[3 + len1];
		encoded[0] = encodeValue(maxInfixIndex);
		encoded[1] = encodeValue(maxInfixLength);
		encoded[2] = encodeValue(truncateSuffixBytes);
		System.arraycopy(target, maxSubsequenceLength, encoded, 3, len1);
		return encoded;
	}

	/**
	 * Ensure the buffer's capacity is large enough to hold a given number
	 * of elements. If the input buffer is not large enough, a new buffer is allocated
	 * and returned.
	 *
	 * @param elements The required number of elements to be appended to the buffer.
	 * @param buffer   The buffer to check or {@code null} if a new buffer should be allocated.
	 * @return Returns the same buffer or a new buffer with the given capacity.
	 */
	private static ByteBuffer clearAndEnsureCapacity(ByteBuffer buffer, final int elements){
		if(buffer == null || buffer.capacity() < elements)
			buffer = ByteBuffer.allocate(elements);
		else
			buffer.clear();
		return buffer;
	}

	@Override
	public final byte[] decode(final byte[] source, final byte[] encoded){
		int infixIndex = decodeValue(encoded[0]);
		int infixLength = decodeValue(encoded[1]);
		int truncateSuffixBytes = decodeValue(encoded[2]);

		if(infixLength == REMOVE_EVERYTHING || truncateSuffixBytes == REMOVE_EVERYTHING){
			infixIndex = 0;
			infixLength = source.length;
			truncateSuffixBytes = 0;
		}

		final int len1 = source.length - (infixIndex + infixLength + truncateSuffixBytes);
		final int len2 = encoded.length - 3;
		final byte[] decoded = new byte[infixIndex + len1 + len2];
		System.arraycopy(source, 0, decoded, 0, infixIndex);
		System.arraycopy(source, infixIndex + infixLength, decoded, infixIndex, len1);
		System.arraycopy(encoded, 3, decoded, infixIndex + len1, len2);
		return decoded;
	}

	@Override
	public final String toString(){
		return getClass().getSimpleName();
	}

}
