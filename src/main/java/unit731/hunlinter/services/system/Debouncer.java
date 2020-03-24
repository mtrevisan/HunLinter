package unit731.hunlinter.services.system;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunlinter.parsers.vos.RuleEntry;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Debouncer<T>{

	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
	private final ConcurrentHashMap<T, TimerTask> delayedMap = new ConcurrentHashMap<>();

	private final Runnable callback;
	private final int interval;


	public Debouncer(final Runnable callback, final int interval){
		Objects.requireNonNull(callback);

		this.callback = callback;
		this.interval = interval;
	}

	public void call(final T key){
		final TimerTask task = new TimerTask(key);

		TimerTask prev;
		do{
			prev = delayedMap.putIfAbsent(key, task);
			if(prev == null)
				executorService.schedule(task, interval, TimeUnit.MILLISECONDS);
		//exit only if new task was added to map, or existing task was extended successfully
		}while(prev != null && !prev.extend());
	}

	public void terminate(){
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
			if(obj == this)
				return true;
			if(obj == null || obj.getClass() != getClass())
				return false;

			final TimerTask rhs = (TimerTask)obj;
			return new EqualsBuilder()
				.append(key, rhs.key)
				.isEquals();
		}

		@Override
		public int hashCode(){
			return new HashCodeBuilder()
				.append(key)
				.toHashCode();
		}
	}

}
