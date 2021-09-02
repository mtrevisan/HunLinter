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
package io.github.mtrevisan.hunlinter.datastructures.bloomfilter.core;

import java.io.Closeable;
import java.io.IOException;


/**
 * A contract for all implementations of bit-arrays. This provides specific methods that will be needed for working with bloom filters.
 */
public interface BitArray extends Closeable{

	/**
	 * Get the bit at index
	 *
	 * @param index the index of the bit in the array
	 * @return {@code true} if the but is set, <code>false</code> otherwise
	 */
	boolean get(final int index);

	/**
	 * Set the bit at index
	 *
	 * @param index the index of the bit in the array
	 * @return {@code true} if the bit was updated, <code>false</code> otherwise.
	 *
	 */
	boolean set(final int index);

	/**
	 * Clear a given bit at the index.
	 *
	 * @param index the index of the bit in the array
	 */
	void clear(final int index);

	/**
	 * Clear all bits in the array.
	 */
	void clearAll();

	/**
	 * The space used in number of bits.
	 *
	 * @return the number of bits being used
	 */
	int size();

	@Override
	default void close() throws IOException{}

}
