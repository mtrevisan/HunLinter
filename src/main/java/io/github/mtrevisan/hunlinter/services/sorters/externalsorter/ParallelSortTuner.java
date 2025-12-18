/**
 * Copyright (c) 2025 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.services.sorters.externalsorter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.function.IntFunction;


/**
 * Utility to estimate the crossover length (threshold) where `Arrays.parallelSort` becomes faster than `Arrays.sort`
 * for `String[]` (or any `Comparable[]`).
 *
 * Notes:
 * - Measures wall-clock time using `System.nanoTime()`.
 * - Performs warm-up to stabilize JIT.
 * - Uses geometric progression of sizes and multiple repetitions per size.
 * - Returns the smallest size where parallelSort is faster than sort by a given margin.
 */
public final class ParallelSortTuner{

	private ParallelSortTuner(){}


	public static int estimateThresholdForStrings(final Comparator<String> comparator){
		final IntFunction<String[]> factory = ParallelSortTuner.randomAsciiStringsFactory(5, 40,
			12_345l);
		return estimateThresholdForStrings(
			comparator,
			factory,
			16_384,
			8_388_608,
			2.,
			5,
			2,
			0.10
		);
	}

	/**
	 * Estimates the minimal length beyond which `Arrays.parallelSort` tends to outperform `Arrays.sort`.
	 *
	 * @param comparator	The comparator to use (null -> natural order). Must be consistent across both sorts.
	 * @param dataFactory	Factory that generates a fresh array of a given size for testing (no shared arrays).
	 * @param startSize	Smallest size to test (e.g., 16_384).
	 * @param maxSize	Largest size to test (e.g., 8_388_608).
	 * @param growthFactor	Size growth factor per step (e.g., 2.0).
	 * @param repetitions	Number of measured repetitions per size (e.g., 5). Minimum value is 1.
	 * @param warmupIterations	Warm-up iterations per size (e.g., 2) – not measured. Minimum value is 0.
	 * @param winMargin	Required margin for parallelSort to be considered faster (e.g., 0.10 = 10%). Minimum value is 0.
	 * @return	Estimated threshold length; if none found, returns maxSize + 1 (meaning "no crossover observed").
	 */
	public static int estimateThresholdForStrings(final Comparator<String> comparator,
			final IntFunction<String[]> dataFactory, final int startSize, final int maxSize, final double growthFactor,
			final int repetitions, final int warmupIterations, final double winMargin){
		//fallbacks and guards
		if(growthFactor <= 1.)
			throw new IllegalArgumentException("growthFactor must be > 1");
		if(startSize <= 0 || maxSize < startSize)
			throw new IllegalArgumentException("invalid sizes");

		//use natural order if `comparator` is null
		final Comparator<String> cmp = (comparator != null? comparator: Comparator.naturalOrder());

		//if only 1 CPU, parallel sort won't help
		if(Runtime.getRuntime().availableProcessors() < 2)
			//no crossover expected
			return maxSize + 1;

		//iterate sizes geometrically
		for(long size = startSize; size <= maxSize; size = Math.max((long)Math.ceil(size * growthFactor), size + 1)){
			//warm-up (not measured)
			for(int w = 0; w < warmupIterations; w ++){
				final String[] base = dataFactory.apply((int)size);
				final String[] a = base.clone();
				final String[] b = base.clone();

				sortArray(a, cmp);

				parallelSortArray(b, cmp);
			}

			//measured runs
			long sequentialSortTime = 0l;
			long parallelSortTime = 0l;

			for(int r = 0; r < repetitions; r ++){
				final String[] base = dataFactory.apply((int)size);

				//measure single-threaded sort
				{
					final String[] copy = base.clone();
					final long t0 = System.nanoTime();
					sortArray(copy, cmp);
					final long t1 = System.nanoTime();
					sequentialSortTime += (t1 - t0);
				}

				//measure parallel sort
				{
					final String[] copy = base.clone();
					final long t0 = System.nanoTime();
					parallelSortArray(copy, cmp);
					final long t1 = System.nanoTime();
					parallelSortTime += (t1 - t0);
				}
			}

			//[ms]
			final double sequentialSortAverageTime = sequentialSortTime / (1_000_000. * repetitions);
			final double parallelSortAverageTime = parallelSortTime / (1_000_000. * repetitions);

			//decide if `parallelSort` "wins" by at least `winMargin`
			//`winMargin = 0.1` means parallel must be >= 10% faster (lower time)
			final boolean parallelWins = (parallelSortAverageTime <= sequentialSortAverageTime * (1. - winMargin));

			//quick GC between size steps to reduce cross-step interference (do not rely on this for correctness; purely
			// a stabilizer)
			try{
				System.gc();

				Thread.sleep(5l);
			}
			catch(final InterruptedException ignored){}

			if(parallelWins)
				return (int)size;
		}

		//no crossover observed up to maxSize
		return maxSize + 1;
	}


	/** Sorts the array with the given comparator; falls back to natural order if `comparator` is null. */
	private static void sortArray(final String[] a, final Comparator<String> cmp){
		if(cmp == null || cmp == Comparator.naturalOrder())
			Arrays.sort(a);
		else
			Arrays.sort(a, cmp);
	}

	/** Parallel sorts the array with the given comparator; falls back to natural order if `comparator` is null. */
	private static void parallelSortArray(final String[] a, final Comparator<String> cmp){
		if(cmp == null || cmp == Comparator.naturalOrder())
			Arrays.parallelSort(a);
		else
			Arrays.parallelSort(a, cmp);
	}


	/**
	 * Returns a generator that produces random strings of bounded length, with reproducible content given a fixed seed
	 * base.
	 */
	public static IntFunction<String[]> randomAsciiStringsFactory(final int minLength, final int maxLength,
			final long seedBase){
		final int minLen = Math.max(1, minLength);
		final int maxLen = Math.max(minLen, maxLength);
		return (n) -> {
			final String[] out = new String[n];
			//create a deterministic seed from size + seedBase to reduce repetition artifacts
			final long seed = (seedBase ^ (((long)n) << 17) ^ 0x9E37_79B9_7F4A_7C15l);
			final Random r = new Random(seed);
			for(int i = 0; i < n; i ++){
				final int len = minLen + r.nextInt(maxLen - minLen + 1);
				final StringBuilder sb = new StringBuilder(len);
				for(int j = 0; j < len; j++){
					//ASCII letters and digits
					int c = 48 + r.nextInt(10 + 26 + 26);
					if(c > 57)
						//map beyond digits into letters range
						c = 65 + (c - 58);
					if(c > 90)
						//into lowercase
						c = 97 + (c - 91);
					sb.append((char)c);
				}
				out[i] = sb.toString();
			}
			return out;
		};
	}

}
