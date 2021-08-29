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

import java.nio.ByteBuffer;


/**
 * The logic of encoding one sequence of bytes relative to another sequence of
 * bytes. The "base" form and the "derived" form are typically the stem of
 * a word and the inflected form of a word.
 *
 * <p>Derived form encoding helps in making the data for the automaton smaller
 * and more repetitive (which results in higher compression rates).
 *
 * <p>See example implementation for details.
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public interface SequenceEncoderInterface{

	/** Maximum encodable single-byte code. */
	int REMOVE_EVERYTHING = 255;


	/**
	 * Encodes <code>target</code> relative to <code>source</code>, optionally reusing the provided {@link ByteBuffer}.
	 *
	 * @param source   The source byte sequence.
	 * @param target   The target byte sequence to encode relative to <code>source</code>
	 * @return	The {@link ByteBuffer} with encoded <code>target</code>.
	 */
	byte[] encode(final byte[] source, final byte[] target);

	/**
	 * Decodes <code>encoded</code> relative to <code>source</code>, optionally reusing the provided {@link ByteBuffer}.
	 *
	 * @param source	The source byte sequence.
	 * @param encoded	The {@linkplain #encode previously encoded} byte sequence.
	 * @return	The {@link ByteBuffer} with decoded <code>target</code>.
	 */
	byte[] decode(final byte[] source, final byte[] encoded);


	default byte encodeValue(final int value){
		return (byte)(value + 'A');
	}

	default int decodeValue(final byte value){
		return ((value - 'A') & 0xFF);
	}

}
