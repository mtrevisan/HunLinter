package unit731.hunspeller.services;

import java.util.concurrent.TimeUnit;


public class TimeWatch{

	private long start;
	private long end;


	private TimeWatch(){
		start = System.nanoTime();
	}

	public static TimeWatch start(){
		return new TimeWatch();
	}

	public TimeWatch reset(){
		start = System.nanoTime();
		end = 0l;
		return this;
	}

	public TimeWatch stop(){
		end = System.nanoTime();
		return this;
	}

	public long time(){
		return (end > 0l? end - start: 0l);
	}

	public long time(TimeUnit unit){
		return unit.convert(time(), TimeUnit.NANOSECONDS);
	}

	public String toStringMinuteSeconds(){
		long mins = time(TimeUnit.MINUTES);
		return String.format("%d min %d sec", mins, time(TimeUnit.SECONDS) - mins * 60);
	}

}
