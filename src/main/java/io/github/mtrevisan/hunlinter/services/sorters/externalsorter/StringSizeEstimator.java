/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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


/**
 * Fast and conservative String size estimator tailored for Java 21 HotSpot.
 * Assumptions:
 *  - Compact Strings enabled (byte[] value + coder).
 *  - Compressed Oops (4-byte references) on 64-bit JVM.
 *  - 8-byte alignment for objects and arrays.
 *
 * Strategy:
 *  - Never underestimate: assume 2 bytes/char for content (safe upper bound).
 *  - Include String object fields, reference to value, and array header.
 *  - Round up to 8-byte alignment to approximate actual heap usage.
 *
 * @see <a href="https://github.com/lemire/externalsortinginjava">External-Memory Sorting in Java</a>, version 0.4.4, 11/3/2020
 */
final class StringSizeEstimator{

	private static final int ALIGN = 8;
	private static final long ALIGN_MASK = ALIGN - 1l;
	private static final long ALIGN_INVERTED_MASK = ~ALIGN_MASK;

	//precomputed base overhead for the detected VM profile
	private static final int BASE_OVERHEAD;
	//precomputed is 64-bit
	private static final boolean IS_64BIT_JVM;
	//precomputed is CO likely
	private static final boolean COMPRESSED_OOPS;
	static{
		IS_64BIT_JVM = detect64Bit();
		COMPRESSED_OOPS = (IS_64BIT_JVM && detectCompressedOopsLikely());

		//Conservative bases for common layouts:
		//  - 64-bit + CO: ~48 bytes upper bound
		//  - 64-bit no CO: ~72 bytes
		//  - 32-bit: ~40 bytes
		final int BASE_OVERHEAD_64_CO = 48;
		final int BASE_OVERHEAD_64_NOCO = 72;
		final int BASE_OVERHEAD_32 = 40;

		BASE_OVERHEAD = (IS_64BIT_JVM
			? (COMPRESSED_OOPS? BASE_OVERHEAD_64_CO: BASE_OVERHEAD_64_NOCO)
			: BASE_OVERHEAD_32);
	}


	private StringSizeEstimator(){}


	/**
	 * Estimates the size of a {@link String} (or {@link CharSequence}) object in bytes.
	 *
	 * This function was designed with the following goals in mind (in order of importance):
	 * First goal is speed: this function is called repeatedly, and it should execute in not much more than a nanosecond.
	 * Second goal is to never underestimate (as it would lead to memory shortage and a crash).
	 * Third goal is to never overestimate too much (say within a factor of two), as it would mean that we are leaving
	 *		much of the RAM underutilized.
	 *
	 * @param text	The string to estimate memory footprint
	 * @return	The <strong>estimated</strong> size [B]
	 */
	public static long estimatedSizeOf(final CharSequence text){
		//content upper bound: 2 bytes per char to avoid underestimation (UTF-16 worst case)
		final long bytes = BASE_OVERHEAD + (((long)text.length()) << 1);
		//align to 8 bytes: (x + 7) & ~7
		return (bytes + ALIGN_MASK) & ALIGN_INVERTED_MASK;
	}


	/** Best-effort detection of 64-bit data model; evaluated once. */
	private static boolean detect64Bit(){
		//check the system property "sun.arch.data.model" not very safe, as it might not work for all JVM implementations
		//nevertheless the worst thing that might happen is that the JVM is 32bit, but we assume its 64bit, so we will be
		//counting a few extra bytes per string object: no harm done here since this is just an approximation
		final String model = System.getProperty("sun.arch.data.model");
		if(model != null)
			return model.contains("64");

		final String arch = System.getProperty("os.arch");
		return (arch != null && arch.contains("64"));
	}

	/**
	 * Best-effort guess for Compressed Oops.
	 * If property is unavailable, assume "true" on modern 64-bit HotSpot.
	 */
	private static boolean detectCompressedOopsLikely(){
		final String co = System.getProperty("java.vm.compressedOops");
		if(co != null)
			return ("true".equalsIgnoreCase(co) || "1".equals(co));

		//likely enabled on 64-bit HotSpot JDK 21
		return true;
	}

}
