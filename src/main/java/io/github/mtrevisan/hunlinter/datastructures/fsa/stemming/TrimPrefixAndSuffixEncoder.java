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


/**
 * Encodes {@code target} relative to {@code source} by trimming whatever
 * non-equal suffix and prefix {@code source} and {@code target} have. The
 * output code is (bytes):
 *
 * <pre>
 * {P}{K}{suffix}
 * </pre>
 *
 * where ({@code P} - 'A') bytes should be trimmed from the start of
 * {@code source}, ({@code K} - 'A') bytes should be trimmed from the
 * end of {@code source} and then the {@code suffix} should be appended
 * to the resulting byte sequence.
 *
 * <p>
 * Examples:
 * </p>
 *
 * <pre>
 * source: abc
 * target: abcd
 * encoded: AAd
 *
 * source: abc
 * target: xyz
 * encoded: ADxyz
 * </pre>
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class TrimPrefixAndSuffixEncoder implements SequenceEncoderInterface{

	@Override
	public final byte[] encode(final byte[] source, final byte[] target){
		//search for the maximum matching subsequence that can be encoded
		int maxSubsequenceLength = 0;
		int maxSubsequenceIndex = 0;
		for(int i = 0; i < source.length; i ++){
			//prefix at i => shared subsequence (infix)
			final int sharedPrefix = ArrayHelper.longestCommonPrefix(source, i, target, 0);
			//only update `maxSubsequenceLength` if we will be able to encode it
			if(sharedPrefix > maxSubsequenceLength && i < REMOVE_EVERYTHING
					&& (source.length - (i + sharedPrefix)) < REMOVE_EVERYTHING){
				maxSubsequenceLength = sharedPrefix;
				maxSubsequenceIndex = i;
			}
		}

		//determine how much to remove (and where) from `source` to get a prefix of `target`
		int truncatePrefixBytes = maxSubsequenceIndex;
		int truncateSuffixBytes = (source.length - (maxSubsequenceIndex + maxSubsequenceLength));
		if(truncateSuffixBytes >= REMOVE_EVERYTHING){
			maxSubsequenceLength = 0;
			truncateSuffixBytes = REMOVE_EVERYTHING;
			truncatePrefixBytes = truncateSuffixBytes;
		}

		final int len1 = target.length - maxSubsequenceLength;
		final byte[] encoded = new byte[2 + len1];
		encoded[0] = encodeValue(truncatePrefixBytes);
		encoded[1] = encodeValue(truncateSuffixBytes);
		System.arraycopy(target, maxSubsequenceLength, encoded, 2, len1);
		return encoded;
	}

	@Override
	public final byte[] decode(final byte[] source, final byte[] encoded){
		int truncatePrefixBytes = decodeValue(encoded[0]);
		int truncateSuffixBytes = decodeValue(encoded[1]);
		if(truncatePrefixBytes == REMOVE_EVERYTHING || truncateSuffixBytes == REMOVE_EVERYTHING){
			truncatePrefixBytes = source.length;
			truncateSuffixBytes = 0;
		}

		final int len1 = source.length - (truncateSuffixBytes + truncatePrefixBytes);
		final int len2 = encoded.length - 2;
		final byte[] decoded = new byte[len1 + len2];
		System.arraycopy(source, truncatePrefixBytes, decoded, 0, len1);
		System.arraycopy(encoded, 2, decoded, len1, len2);
		return decoded;
	}

	@Override
	public final String toString(){
		return getClass().getSimpleName();
	}

}
