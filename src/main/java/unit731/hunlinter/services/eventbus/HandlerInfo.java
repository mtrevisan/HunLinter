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
package unit731.hunlinter.services.eventbus;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;


/** Used to hold the subscriber details */
class HandlerInfo{

	private final Class<?> eventClass;
	private final Method method;
	private final WeakReference<?> subscriber;
	private final boolean vetoHandler;


	HandlerInfo(final Class<?> eventClass, final Method method, final Object subscriber, final boolean vetoHandler){
		this.eventClass = eventClass;
		this.method = method;
		this.subscriber = new WeakReference<>(subscriber);
		this.vetoHandler = vetoHandler;
	}

	public boolean matchesEvent(final Object event){
		return event.getClass().equals(eventClass);
	}

	public Method getMethod(){
		return method;
	}

	public Object getSubscriber(){
		return subscriber.get();
	}

	public boolean isVetoHandler(){
		return vetoHandler;
	}

}
