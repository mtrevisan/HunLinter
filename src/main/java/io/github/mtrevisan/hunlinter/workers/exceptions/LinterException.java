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

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.text.MessageFormat;


public class LinterException extends RuntimeException{

	@Serial
	private static final long serialVersionUID = 2097260898128903703L;

	public enum FixActionType{ADD, REPLACE, REMOVE}


	private final IndexDataPair<?> data;
	//FIXME useful?
	private final Runnable fixAction;
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

	public LinterException(final ThreadLocal<MessageFormat> message, final Object... data){
		this(JavaHelper.createMessage(message, data), null, null, null);
	}

	public LinterException(final String message, final IndexDataPair<?> data){
		this(message, null, data, null, null);
	}

	public LinterException(final String message, final Throwable cause, final IndexDataPair<?> data){
		this(message, cause, data, null, null);
	}

	public LinterException(final String message, final IndexDataPair<?> data, final Runnable fixAction,
			final FixActionType fixActionType){
		this(message, null, data, fixAction, fixActionType);
	}

	public LinterException(final String message,  final Throwable cause, final IndexDataPair<?> data, final Runnable fixAction,
			final FixActionType fixActionType){
		super(message, cause);

		this.data = data;
		this.fixAction = fixAction;
		this.fixActionType = fixActionType;
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
	private void writeObject(final ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	@Serial
	private void readObject(final ObjectInputStream is) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

}
