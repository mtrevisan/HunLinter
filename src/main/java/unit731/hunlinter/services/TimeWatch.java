package unit731.hunlinter.services;

import java.util.Locale;
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
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		final long mins = time(TimeUnit.MINUTES);
		if(mins > 0)
			sj.add(Long.toString(mins)).add("min");
		final long secs = time(TimeUnit.SECONDS) - mins * 60l;
		if(mins == 0 || secs > 0)
			sj.add(Long.toString(secs)).add("s");
		return sj.toString();
	}

	public String toStringMinuteSecondsMillis(){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		final long mins = time(TimeUnit.MINUTES);
		if(mins > 0)
			sj.add(Long.toString(mins)).add("min");
		final long millis = time(TimeUnit.MILLISECONDS) - mins * 60_000l;
		if(mins == 0 || millis > 0)
			sj.add(String.format(Locale.ROOT, "%.3f s", millis / 1000.));
		return sj.toString();
	}

}
