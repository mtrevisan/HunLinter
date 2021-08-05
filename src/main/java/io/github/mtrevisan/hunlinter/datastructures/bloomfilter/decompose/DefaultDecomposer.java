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
package io.github.mtrevisan.hunlinter.datastructures.bloomfilter.decompose;

import java.nio.charset.Charset;


/**
 * The default implementation of {@link Decomposer} that decomposes the object by converting it to a {@link String} object using the
 * {@link Object#toString()} method.
 *
 * To convert the {@link String} thus obtained into bytes, the default platform {@link Charset} encoding is used.
 *
 * @param <T> the type of objects to be stored in the filter
 */
public class DefaultDecomposer<T> implements Decomposer<T>{

	/**
	 * Decompose the object
	 */
	@Override
	public void decompose(final T object, final ByteSink sink, final Charset charset){
		if(object != null){
			final byte[] bytes;
			if(String.class.isAssignableFrom(object.getClass()))
				bytes = ((String)object).getBytes(charset);
			else
				bytes = object.toString().getBytes(charset);
			sink.putBytes(bytes);
		}
	}

}
