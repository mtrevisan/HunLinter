/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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


/**
 * A fast bit-set implementation that allows direct access to data property so that it can be easily serialized.
 */
public class JavaBitArray implements BitArray{

	private static final String WRONG_NUMBER_OF_BITS = "Number of bits must be strictly positive";


	/** The data-set */
	private final long[] data;


	/**
	 * Construct an instance of the {@link JavaBitArray} that can hold the given number of bits
	 *
	 * @param bits the number of bits this instance can hold
	 */
	public JavaBitArray(final long bits){
		if(bits <= 0)
			throw new IllegalArgumentException(WRONG_NUMBER_OF_BITS);

		data = new long[(int)(bits >>> 6) + 1];
	}

	@Override
	public boolean get(final int index){
		return ((data[index >> 6] & (1l << index)) != 0l);
	}

	/** Returns true if the bit changed value. */
	@Override
	public boolean set(final int index){
		if(!get(index)){
			data[index >> 6] |= (1l << index);
			return true;
		}
		return false;
	}

	@Override
	public void clear(final int index){
		if(get(index))
			data[index >> 6] &= ~(1l << index);
	}

	@Override
	public void clearAll(){
		int size = data.length;
		while(size > 0)
			data[-- size] = 0l;
	}

	/**
	 * Number of bits
	 *
	 * @return total number of bits allocated
	 */
	@Override
	public int size(){
		return data.length * Long.SIZE;
	}

}
