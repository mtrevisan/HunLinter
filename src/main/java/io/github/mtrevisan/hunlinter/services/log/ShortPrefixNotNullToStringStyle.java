/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.services.log;

import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;


public final class ShortPrefixNotNullToStringStyle extends ToStringStyle{

	@Serial
	private static final long serialVersionUID = 603695866745355049L;

	public static final ToStringStyle SHORT_PREFIX_NOT_NULL_STYLE = new ShortPrefixNotNullToStringStyle();


	private ShortPrefixNotNullToStringStyle(){

		setUseShortClassName(true);
		setUseIdentityHashCode(false);
	}

	@Override
	public void append(final StringBuffer buffer, final String fieldName, final Object value, final Boolean fullDetail){
		if(value != null){
			super.append(buffer, fieldName, value, fullDetail);
		}
	}

	@SuppressWarnings({"unused", "SameReturnValue"})
	@Serial
	private Object readResolve(){
		return SHORT_PREFIX_NOT_NULL_STYLE;
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
