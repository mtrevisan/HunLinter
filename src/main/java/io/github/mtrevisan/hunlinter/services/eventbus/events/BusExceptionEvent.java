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
package io.github.mtrevisan.hunlinter.services.eventbus.events;

import java.io.Serial;
import java.util.EventObject;


/**
 * For any exceptions that occur on the bus during handler execution, this event will be published.
 *
 * @see <a href="https://github.com/taftster/simpleeventbus">Simple Event Bus</a>
 */
public class BusExceptionEvent extends EventObject{

	@Serial
	private static final long serialVersionUID = -6138624437653011774L;


	private final Throwable cause;


	public BusExceptionEvent(final Object subscriber, final Throwable cause){
		super(subscriber);

		this.cause = cause;
	}

	public Object getSubscriber(){
		return getSource();
	}

	public Throwable getCause(){
		return cause;
	}

}