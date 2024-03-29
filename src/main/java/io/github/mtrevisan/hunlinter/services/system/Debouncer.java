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
package io.github.mtrevisan.hunlinter.services.system;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Debouncer<T>{

	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
	private final ConcurrentHashMap<T, TimerTask> delayedMap = new ConcurrentHashMap<>(0);

	private final Runnable callback;
	private final int interval;


	public Debouncer(final Runnable callback, final int interval){
		Objects.requireNonNull(callback, "Callback cannot be null");

		this.callback = callback;
		this.interval = interval;
	}

	public final void call(final T key){
		final TimerTask task = new TimerTask(key);

		TimerTask prev;
		do{
			prev = delayedMap.putIfAbsent(key, task);
			if(prev == null)
				executorService.schedule(task, interval, TimeUnit.MILLISECONDS);
		//exit only if new task was added to map, or existing task was extended successfully
		}while(prev != null && !prev.extend());
	}

	public final void terminate(){
		executorService.shutdownNow();
	}

	//The task that wakes up when the wait time elapses
	private final class TimerTask implements Runnable{

		private final T key;
		private long dueTime;
		private final Object lock = new Object();


		TimerTask(final T key){
			this.key = key;

			extend();
		}

		public boolean extend(){
			synchronized(lock){
				//task has been shut down
				if(dueTime < 0)
					return false;

				dueTime = System.currentTimeMillis() + interval;
				return true;
			}
		}

		@Override
		public void run(){
			synchronized(lock){
				final long remaining = dueTime - System.currentTimeMillis();
				//re-schedule task
				if(remaining > 0)
					executorService.schedule(this, remaining, TimeUnit.MILLISECONDS);
				//mark as terminated and invoke callback
				else{
					dueTime = -1;
					try{
						callback.run();
					}
					finally{
						delayedMap.remove(key);
					}
				}
			}
		}

		@Override
		public boolean equals(final Object obj){
			if(this == obj)
				return true;
			if(obj == null || getClass() != obj.getClass())
				return false;

			@SuppressWarnings("unchecked")
			final TimerTask rhs = (TimerTask)obj;
			return key.equals(rhs.key);
		}

		@Override
		public int hashCode(){
			return key.hashCode();
		}

	}

}
