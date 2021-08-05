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
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public final class BufferUtils{

	private BufferUtils(){}

	/**
	 * Ensure the buffer's capacity is large enough to hold a given number
	 * of elements. If the input buffer is not large enough, a new buffer is allocated
	 * and returned.
	 *
	 * @param elements The required number of elements to be appended to the buffer.
	 * @param buffer   The buffer to check or <code>null</code> if a new buffer should be
	 *                 allocated.
	 * @return Returns the same buffer or a new buffer with the given capacity.
	 */
	public static ByteBuffer clearAndEnsureCapacity(ByteBuffer buffer, final int elements){
		if(buffer == null || buffer.capacity() < elements)
			buffer = ByteBuffer.allocate(elements);
		else
			buffer.clear();
		return buffer;
	}

}
