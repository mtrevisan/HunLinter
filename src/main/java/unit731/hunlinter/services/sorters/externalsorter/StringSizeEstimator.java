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
package unit731.hunlinter.services.sorters.externalsorter;


/**
 * @see <a href="https://github.com/lemire/externalsortinginjava">External-Memory Sorting in Java</a>, version 0.4.4, 11/3/2020
 */
class StringSizeEstimator{

	private static final int OBJ_OVERHEAD;
	static{
		//by default we assume 64 bit JVM (defensive approach since we will get larger estimations in case we are not sure)
		boolean is64bitJVM = true;

		//check the system property "sun.arch.data.model" not very safe, as it might not work for all JVM implementations
		//nevertheless the worst thing that might happen is that the JVM is 32bit but we assume its 64bit, so we will be
		//counting a few extra bytes per string object: no harm done here since this is just an approximation
		final String arch = System.getProperty("sun.arch.data.model");
		if(arch != null && arch.contains("32"))
			//if exists and is 32 bit then we assume a 32bit JVM
			is64bitJVM = false;

		//the sizes below are a bit rough as we don't take into account advanced JVM options such as compressed oops
		//however if our calculation is not accurate it'll be a bit over so there is no danger of an out of memory error
		//because of this
		final int objectHeader = (is64bitJVM? 16: 8);
		final int arrayHeader = (is64bitJVM? 24: 12);
		final int integerFields = 12;
		final int objectReference = (is64bitJVM? 8: 4);
		OBJ_OVERHEAD = objectHeader + integerFields + objectReference + arrayHeader;
	}


	private StringSizeEstimator(){}

	/**
	 * Estimates the size of a {@link String} object in bytes.
	 *
	 * This function was designed with the following goals in mind (in order of importance):
	 * First goal is speed: this function is called repeatedly and it should execute in not much more than a nanosecond.
	 * Second goal is to never underestimate (as it would lead to memory shortage and a crash).
	 * Third goal is to never overestimate too much (say within a factor of two), as it would mean that we are leaving
	 *		much of the RAM underutilized.
	 *
	 * @param text The string to estimate memory footprint
	 * @return The <strong>estimated</strong> size [B]
	 */
	public static long estimatedSizeOf(final String text){
		return text.length() * 2 + OBJ_OVERHEAD;
	}

}
