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
package io.github.mtrevisan.hunlinter.workers.exceptions;

import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;

import java.io.Serial;


public class LinterWarning extends Exception{

	@Serial
	private static final long serialVersionUID = 3853411643385148097L;


	private final IndexDataPair<?> data;


	public LinterWarning(final Throwable cause, final Object data){
		this(null, cause, IndexDataPair.of(-1, data));
	}

	public LinterWarning(final Throwable cause, final IndexDataPair<?> data){
		this(null, cause, data);
	}

	public LinterWarning(final String message){
		this(message, null, null);
	}

	public LinterWarning(final String message, final IndexDataPair<?> data){
		this(message, null, data);
	}

	public LinterWarning(final String message, final Throwable cause, final IndexDataPair<?> data){
		super(message, cause);

		this.data = data;
	}

	public IndexDataPair<?> getData(){
		return data;
	}

}