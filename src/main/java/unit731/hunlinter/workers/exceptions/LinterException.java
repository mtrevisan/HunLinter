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
package unit731.hunlinter.workers.exceptions;

import unit731.hunlinter.workers.core.IndexDataPair;

import java.io.Serial;


public class LinterException extends RuntimeException{

	@Serial
	private static final long serialVersionUID = 2097260898128903703L;

	public enum FixActionType{ADD, REPLACE, REMOVE}


	private final IndexDataPair<?> data;
	//FIXME useful?
	private final Runnable fix;
	private final FixActionType fixActionType;


	public LinterException(final Throwable cause, final Object data){
		this(null, cause, IndexDataPair.of(-1, data), null, null);
	}

	public LinterException(final Throwable cause, final IndexDataPair<?> data){
		this(null, cause, data, null, null);
	}

	public LinterException(final String message){
		this(message, null, null, null);
	}

	public LinterException(final String message, final IndexDataPair<?> data){
		this(message, null, data, null, null);
	}

	public LinterException(final String message, final Throwable cause, final IndexDataPair<?> data){
		this(message, cause, data, null, null);
	}

	public LinterException(final String message, final IndexDataPair<?> data, final Runnable fix,
			final FixActionType fixActionType){
		this(message, null, data, fix, fixActionType);
	}

	public LinterException(final String message,  final Throwable cause, final IndexDataPair<?> data, final Runnable fix,
			final FixActionType fixActionType){
		super(message, cause);

		this.data = data;
		this.fix = fix;
		this.fixActionType = fixActionType;
	}

	public IndexDataPair<?> getData(){
		return data;
	}

	public boolean canFix(){
		return (fix != null);
	}

	public Runnable getFixAction(){
		return fix;
	}

	public FixActionType getFixActionType(){
		return fixActionType;
	}

}
