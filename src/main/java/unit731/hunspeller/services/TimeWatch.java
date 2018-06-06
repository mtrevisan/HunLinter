package unit731.hunspeller.services;

import java.util.concurrent.TimeUnit;


public class TimeWatch{

	private long start;


	private TimeWatch(){
		start = System.nanoTime();
	}

	public static TimeWatch start(){
		return new TimeWatch();
	}

	public TimeWatch reset(){
		start = System.nanoTime();
		return this;
	}

	public long time(){
		long ends = System.nanoTime();
		return ends - start;
	}

	public long time(TimeUnit unit){
		return unit.convert(time(), TimeUnit.NANOSECONDS);
	}

	public String toStringMinuteSeconds(){
		return String.format("%d min %d sec", time(TimeUnit.MINUTES), time(TimeUnit.SECONDS) - time(TimeUnit.MINUTES));
	}

}
