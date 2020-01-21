package unit731.hunlinter.services;

import java.util.Locale;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;


public class TimeWatch{

	private static final String TIMER_NOT_STOPPED = "timer not stopped";
	private static final String MINUTES = "min";
	private static final String SECONDS = "s";

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

	public long time(final TimeUnit unit){
		return unit.convert(time(), TimeUnit.NANOSECONDS);
	}

	public String toStringMinuteSeconds(){
		if(end > 0l){
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
		else
			return TIMER_NOT_STOPPED;
	}

	public String toStringMinuteSecondsMillis(){
		if(end > 0l){
			final long delta = time(TimeUnit.MILLISECONDS);

			final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
			final long mins = delta / 60_000l;
			if(mins > 0)
				sj.add(Long.toString(mins)).add(MINUTES);
			final long millis = delta - mins * 60_000l;
			if(mins == 0 || millis > 0)
				sj.add(String.format(Locale.ROOT, "%.3f", millis / 1000.)).add(SECONDS);
			return sj.toString();
		}
		else
			return TIMER_NOT_STOPPED;
	}

}
