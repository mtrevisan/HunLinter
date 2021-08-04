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
package io.github.mtrevisan.hunlinter.services.system;

import java.util.Locale;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;


public final class TimeWatch{

	private static final String TIMER_NOT_STOPPED = "timer not stopped";
	private static final String MINUTES = "min";
	private static final String SECONDS = "sec";
	private static final String MILLIS = "ms";
	private static final String MICROS = "µs";
	private static final String SPACE = " ";

	private long start;
	private long end;


	private TimeWatch(){
		reset();
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

		final StringJoiner sj = new StringJoiner(SPACE);
		final long mins = delta / 60l;
		if(mins > 0)
			sj.add(Long.toString(mins)).add(MINUTES);
		final long secs = delta - mins * 60l;
		if(mins == 0 || secs > 0)
			sj.add(Long.toString(secs)).add(SECONDS);
		return sj.toString();
	}

	public String toStringMillis(){
		return (end > 0l? time(TimeUnit.MILLISECONDS) + SPACE + MILLIS: TIMER_NOT_STOPPED);
	}

	public String toStringMicros(final int runs){
		if(end < 0l)
			return TIMER_NOT_STOPPED;

		final double micros = (double)(end - start) / (runs * 1_000);
		final int fractionalDigits = (micros > 100? 0: (micros > 10? 1: 3));
		return String.format(Locale.US, "%." + fractionalDigits + "f " + MICROS, micros);
	}

}
