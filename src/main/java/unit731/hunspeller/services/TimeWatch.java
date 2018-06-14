package unit731.hunspeller.services;

import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;


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
		StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		long mins = time(TimeUnit.MINUTES);
		if(mins > 0)
			sj.add(String.format("%d min", mins));
		long secs = time(TimeUnit.SECONDS) - mins * 60;
		if(mins == 0 || secs > 0)
			sj.add(String.format("%d sec", secs));
		return sj.toString();
	}

}
