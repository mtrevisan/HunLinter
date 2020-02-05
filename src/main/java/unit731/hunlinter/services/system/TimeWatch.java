package unit731.hunlinter.services.system;

import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;


public class TimeWatch{

	private static final String TIMER_NOT_STOPPED = "timer not stopped";
	private static final String MINUTES = "min";
	private static final String SECONDS = "sec";
	private static final String MILLIS = "ms";

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
		return (end > 0l? end - start: -1l);
	}

	public long time(final TimeUnit unit){
		return unit.convert(time(), TimeUnit.NANOSECONDS);
	}

	public String toStringMinuteSeconds(){
		if(end == 0l)
			return TIMER_NOT_STOPPED;

		final long delta = time(TimeUnit.SECONDS);

		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		final long mins = delta / 60l;
		if(mins > 0)
			sj.add(Long.toString(mins)).add(MINUTES);
		final long secs = delta - mins * 60l;
		if(mins == 0 || secs > 0)
			sj.add(Long.toString(secs)).add(SECONDS);
		return sj.toString();
	}

	public String toStringMillis(){
		return (end > 0l? time(TimeUnit.MILLISECONDS) + StringUtils.SPACE + MILLIS: TIMER_NOT_STOPPED);
	}

}
