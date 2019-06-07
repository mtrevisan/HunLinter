package unit731.hunspeller.services;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


public class Debouncer<T>{

	private final ScheduledExecutorService sched = Executors.newScheduledThreadPool(1);
	private final ConcurrentHashMap<T, TimerTask> delayedMap = new ConcurrentHashMap<>();

	private final Consumer<T> callback;
	private final int interval;


	public Debouncer(Consumer<T> callback, int interval){
		Objects.requireNonNull(callback);

		this.callback = callback;
		this.interval = interval;
	}

	public void call(T key){
		TimerTask task = new TimerTask(key);

		TimerTask prev;
		do{
			prev = delayedMap.putIfAbsent(key, task);
			if(prev == null)
				sched.schedule(task, interval, TimeUnit.MILLISECONDS);
		//exit only if new task was added to map, or existing task was extended successfully
		}while(prev != null && !prev.extend());
	}

	public void terminate(){
		sched.shutdownNow();
	}

	//The task that wakes up when the wait time elapses
	private final class TimerTask implements Runnable{

		private final T key;
		private long dueTime;
		private final Object lock = new Object();


		TimerTask(T key){
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
				long remaining = dueTime - System.currentTimeMillis();
				//re-schedule task
				if(remaining > 0)
					sched.schedule(this, remaining, TimeUnit.MILLISECONDS);
				//mark as terminated and invoke callback
				else{
					dueTime = -1;
					try{
						callback.accept(key);
					}
					finally{
						delayedMap.remove(key);
					}
				}
			}
		}
	}

}
