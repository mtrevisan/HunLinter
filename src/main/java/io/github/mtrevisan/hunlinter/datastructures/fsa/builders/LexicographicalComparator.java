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
package io.github.mtrevisan.hunlinter.datastructures.fsa.builders;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Comparator;


/**
 * Provides a <a href="http://en.wikipedia.org/wiki/Lexicographical_order">lexicographical comparator</a> implementation;
 * either a Java implementation or a faster implementation based on {@link Unsafe}.
 *
 * <p>Uses reflection to gracefully fall back to the Java implementation if {@code Unsafe} isn't available.
 *
 * <p>The returned comparator is inconsistent with {@link Object#equals(Object)} (since arrays
 * support only identity equality), but it is consistent with {@link
 * java.util.Arrays#equals(byte[], byte[])}.
 *
 * @see <a href="https://dzone.com/articles/understanding-sunmiscunsafe">Understanding sun.misc.Unsafe</a>
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public final class LexicographicalComparator{

	private static final int UNSIGNED_MASK = 0xFF;

	private static final String UNSAFE_COMPARATOR_NAME = LexicographicalComparator.class.getName() + "$UnsafeComparator";

	private static final Comparator<byte[]> BEST_COMPARATOR = getBestComparator();


	private LexicographicalComparator(){}

	public static Comparator<byte[]> lexicographicalComparator(){
		return BEST_COMPARATOR;
	}

	@SuppressWarnings("SameReturnValue")
	public static Comparator<byte[]> lexicographicalComparatorJavaImpl(){
		return LexicographicalComparator.PureJavaComparator.INSTANCE;
	}

	enum UnsafeComparator implements Comparator<byte[]>{
		INSTANCE;

		static final boolean BIG_ENDIAN = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);

		/*
		 * The following static final fields exist for performance reasons.
		 *
		 * In UnsignedBytesBenchmark, accessing the following objects via static final fields is the
		 * fastest (more than twice as fast as the Java implementation, vs ~1.5x with non-final static
		 * fields, on x86_32) under the Hotspot server compiler. The reason is obviously that the
		 * non-final fields need to be reloaded inside the loop.
		 *
		 * And, no, defining (final or not) local variables out of the loop still isn't as good
		 * because the null check on the theUnsafe object remains inside the loop and
		 * BYTE_ARRAY_BASE_OFFSET doesn't get constant-folded.
		 *
		 * The compiler can treat static final fields as compile-time constants and can constant-fold
		 * them while (final or not) local variables are runtime values.
		 */

		@SuppressWarnings("UseOfSunClasses")
		static final Unsafe theUnsafe = getUnsafe();

		/** The offset to the first element in a byte array. */
		static final int BYTE_ARRAY_BASE_OFFSET = theUnsafe.arrayBaseOffset(byte[].class);
		static{
			//fall back to the safer pure java implementation unless we're in
			//a 64-bit JVM with an 8-byte aligned field offset.
			if(!("64".equals(System.getProperty("sun.arch.data.model")) && (BYTE_ARRAY_BASE_OFFSET % 8) == 0
					//sanity check - this should never fail
					&& theUnsafe.arrayIndexScale(byte[].class) == 1))
				//force fallback to PureJavaComparator
				throw new Error();
		}

		/**
		 * A {@link sun.misc.Unsafe Unsafe} object suitable for use in a 3rd party package.
		 * Replace with a simple call to Unsafe.getUnsafe when integrating into a jdk.
		 *
		 * @return	A {@link sun.misc.Unsafe Unsafe}.
		 */
		private static Unsafe getUnsafe(){
			try{
				return Unsafe.getUnsafe();
			}
			catch(final SecurityException ignored){
				//that's okay; try reflection instead
			}
			try{
				return AccessController.doPrivileged((PrivilegedExceptionAction<Unsafe>)() -> {
					final Class<Unsafe> k = Unsafe.class;
					for(final Field f : k.getDeclaredFields()){
						f.setAccessible(true);
						final Object x = f.get(null);
						if(k.isInstance(x))
							return k.cast(x);
					}
					throw new NoSuchFieldError("the Unsafe");
				});
			}
			catch(final PrivilegedActionException e){
				throw new Error("Could not initialize intrinsics", e);
			}
		}

		@Override
		public int compare(final byte[] left, final byte[] right){
			final int stride = 8;
			final int minLength = Math.min(left.length, right.length);
			final int strideLimit = minLength & -stride;
			int i;

			/*
			 * Compare 8 bytes at a time. Benchmarking on x86 shows a stride of 8 bytes is no slower
			 * than 4 bytes even on 32-bit. On the other hand, it is substantially faster on 64-bit.
			 */
			for(i = 0; i < strideLimit; i += stride){
				final long lw = theUnsafe.getLong(left, BYTE_ARRAY_BASE_OFFSET + (long)i);
				final long rw = theUnsafe.getLong(right, BYTE_ARRAY_BASE_OFFSET + (long)i);
				if(lw != rw){
					if(BIG_ENDIAN)
						return Long.compare(lw, rw);

					/*
					 * We want to compare only the first index where left[index] != right[index]. This
					 * corresponds to the least significant nonzero byte in lw ^ rw, since lw and rw are
					 * little-endian. Long.numberOfTrailingZeros(diff) tells us the least significant
					 * nonzero bit, and zeroing out the first three bits of L.nTZ gives us the shift to get
					 * that least significant nonzero byte.
					 */
					final int n = Long.numberOfTrailingZeros(lw ^ rw) & ~0x7;
					return ((int)((lw >>> n) & UNSIGNED_MASK)) - ((int)((rw >>> n) & UNSIGNED_MASK));
				}
			}

			//the epilogue to cover the last (minLength % stride) elements
			for(; i < minLength; i ++){
				final int result = (left[i] & 0xFF) - (right[i] & 0xFF);
				if(result != 0)
					return result;
			}
			return left.length - right.length;
		}

		@Override
		public String toString(){
			return "UnsignedBytes.lexicographicalComparator() (sun.misc.Unsafe version)";
		}
	}

	enum PureJavaComparator implements Comparator<byte[]>{
		INSTANCE;

		@Override
		public int compare(final byte[] left, final byte[] right){
			final int minLength = Math.min(left.length, right.length);
			for(int i = 0; i < minLength; i ++){
				final int result = (left[i] & 0xFF) - (right[i] & 0xFF);
				if(result != 0)
					return result;
			}
			return left.length - right.length;
		}

		@Override
		public String toString(){
			return "UnsignedBytes.lexicographicalComparator() (pure Java version)";
		}
	}

	/** Returns the Unsafe-using Comparator, or falls back to the pure-Java implementation if unable to do so. */
	static Comparator<byte[]> getBestComparator(){
		try{
			final Class<?> theClass = Class.forName(UNSAFE_COMPARATOR_NAME);

			//yes, UnsafeComparator does implement Comparator<byte[]>
			@SuppressWarnings("unchecked")
			final Comparator<byte[]> comparator = (Comparator<byte[]>)theClass.getEnumConstants()[0];
			return comparator;
		}
		//ensure we really catch *everything*
		catch(final ClassNotFoundException ignored){
			return lexicographicalComparatorJavaImpl();
		}
	}

}
