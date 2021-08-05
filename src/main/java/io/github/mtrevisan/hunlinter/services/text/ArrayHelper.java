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


public final class ArrayHelper{

	private ArrayHelper(){}

	public static byte[] concatenate(final byte[]... arrays){
		int size = 0;
		for(int i = 0; i < arrays.length; i ++)
			size += arrays[i].length;

		size = 0;
		final byte[] joinedArray = new byte[size];
		for(int i = 0; i < arrays.length; i ++, size += arrays[i].length)
			System.arraycopy(arrays[i], 0, joinedArray, size, arrays[i].length);
		return joinedArray;
	}

	/**
	 * Compute the length of the shared prefix between two byte sequences
	 *
	 * @param a	First array
	 * @param b	Second array
	 * @return	The longest common prefix
	 */
	public static int longestCommonPrefix(final byte[] a, final byte[] b){
		int i = 0;
		final int max = Math.min(a.length, b.length);
		while(i < max && a[i] == b[i])
			i ++;
		return i;
	}

	/**
	 * Compute the length of the shared prefix between two byte sequences
	 *
	 * @param a	First array
	 * @param aStart	The index to start for the first array
	 * @param b	Second array
	 * @param bStart	The index to start for the second array
	 * @return	The longest common prefix
	 */
	public static int longestCommonPrefix(final byte[] a, int aStart, final byte[] b, int bStart){
		int i = 0;
		final int max = Math.min(a.length - aStart, b.length - bStart);
		while(i < max && a[aStart ++] == b[bStart ++])
			i ++;
		return i;
	}

}
