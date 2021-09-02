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
package io.github.mtrevisan.hunlinter.datastructures.fsa.stemming;

import io.github.mtrevisan.hunlinter.services.text.ArrayHelper;


/**
 * Encodes {@code target} relative to {@code source} by trimming whatever
 * non-equal suffix {@code source} has. The output code is (bytes):
 *
 * <pre>
 * {K}{suffix}
 * </pre>
 *
 * where ({@code K} - 'A') bytes should be trimmed from the end of
 * {@code source} and then the {@code suffix} should be appended to the
 * resulting byte sequence.
 *
 * <p>
 * Examples:
 * </p>
 *
 * <pre>
 * source:	foo
 * target:	foobar
 * encoded:	Abar
 *
 * source:	foo
 * target:	bar
 * encoded:	Dbar
 * </pre>
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class TrimSuffixEncoder implements SequenceEncoderInterface{

	@Override
	public byte[] encode(final byte[] source, final byte[] target){
		int sharedPrefix = ArrayHelper.longestCommonPrefix(source, target);
		int truncateBytes = source.length - sharedPrefix;
		if(truncateBytes >= REMOVE_EVERYTHING){
			truncateBytes = REMOVE_EVERYTHING;
			sharedPrefix = 0;
		}

		final byte[] encoded = new byte[1 + target.length - sharedPrefix];
		encoded[0] = encodeValue(truncateBytes);
		System.arraycopy(target, sharedPrefix, encoded, 1, target.length - sharedPrefix);
		return encoded;
	}

	@Override
	public byte[] decode(final byte[] source, final byte[] encoded){
		final byte suffixTrimCode = encoded[0];
		int truncateBytes = decodeValue(suffixTrimCode);
		if(truncateBytes == REMOVE_EVERYTHING)
			truncateBytes = source.length;

		final int len1 = source.length - truncateBytes;
		final int len2 = encoded.length - 1;

		final byte[] decoded = new byte[len1 + len2];
		System.arraycopy(source, 0, decoded, 0, len1);
		System.arraycopy(encoded, 1, decoded, len1, len2);
		return decoded;
	}

	@Override
	public String toString(){
		return getClass().getSimpleName();
	}

}
