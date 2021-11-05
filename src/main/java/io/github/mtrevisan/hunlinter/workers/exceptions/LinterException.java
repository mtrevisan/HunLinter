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
package io.github.mtrevisan.hunlinter.workers.exceptions;

import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;

import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;


public class LinterException extends RuntimeException{

	@Serial
	private static final long serialVersionUID = 2097260898128903703L;

	public enum FixActionType{ADD, REPLACE, REMOVE}


	private IndexDataPair<?> data;
	//FIXME useful?
	private Runnable fixAction;
	private FixActionType fixActionType;


	public LinterException(final Throwable cause){
		super(cause);
	}

	public LinterException(final String message, final Object... parameters){
		this(null, message, parameters);
	}

	public LinterException(final Throwable cause, final String message, final Object... parameters){
		super(JavaHelper.textFormat(message, parameters), cause);
	}

	public final LinterException withIndexDataPair(final IndexDataPair<?> data){
		this.data = data;

		return this;
	}

	public final LinterException withData(final Object data){
		this.data = (IndexDataPair.class.isInstance(data)? (IndexDataPair<?>)data: IndexDataPair.of(-1, data));

		return this;
	}

	public final LinterException withFixAction(final Runnable fixAction, final FixActionType fixActionType){
		this.fixAction = fixAction;
		this.fixActionType = fixActionType;

		return this;
	}

	public final IndexDataPair<?> getData(){
		return data;
	}

	public final boolean canFix(){
		return (fixAction != null);
	}

	public final Runnable getFixAction(){
		return fixAction;
	}

	public final FixActionType getFixActionType(){
		return fixActionType;
	}


	@SuppressWarnings("unused")
	@Serial
	private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	@Serial
	private void readObject(final ObjectInputStream is) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

}
