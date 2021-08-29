/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.datastructures.bloomfilter.hash;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * A Murmur3 hash function.
 */
public class Murmur3HashFunction implements HashFunction{

	private static final long SEED = 0x7F3A21EAl;

	/** Helps convert a byte into its unsigned value. */
	private static final int UNSIGNED_MASK = 0xFF;
	/** Helps convert integer to its unsigned value. */
	private static final long UINT_MASK = 0xFFFFFFFFl;

	private static final int X86_32_C1 = 0xCC9E2D51;
	private static final int X86_32_C2 = 0x1B873593;
	private static final long X64_128_C1 = 0x87C37B91114253D5L;
	private static final long X64_128_C2 = 0x4CF5AD432745937FL;


	@Override
	public boolean isSingleValued(){
		return false;
	}

	@Override
	public long hash(final byte[] bytes){
		return hash_x86_32(bytes, 0, SEED);
	}

	@Override
	public long[] hashMultiple(final byte[] bytes){
		return hash_x64_128(bytes, 0, SEED);
	}

	/**
	 * Compute the Murmur3 hash as described in the original source code.
	 *
	 * @param data	The data that needs to be hashed
	 * @param length	The length of the data that needs to be hashed
	 * @param seed	The seed to use to compute the hash
	 * @return	The computed hash value
	 */
	@SuppressWarnings("fallthrough")
	private static long hash_x86_32(final byte[] data, final int length, final long seed){
		final int nblocks = length >> 2;
		long hash = seed;

		//----------
		// body
		for(int i = 0; i < nblocks; i ++){
			final int i4 = i << 2;

			long k1 = (data[i4] & UNSIGNED_MASK);
			k1 |= (data[i4 + 1] & UNSIGNED_MASK) << 8;
			k1 |= (data[i4 + 2] & UNSIGNED_MASK) << 16;
			k1 |= (data[i4 + 3] & UNSIGNED_MASK) << 24;

//			int k1 = (data[i4] & 0xFF) + ((data[i4 + 1] & 0xFF) << 8) + ((data[i4 + 2] & 0xFF) << 16) + ((data[i4 + 3] & 0xFF) << 24);
			k1 = (k1 * X86_32_C1) & UINT_MASK;
			k1 = rotl32(k1, 15);
			k1 = (k1 * X86_32_C2) & UINT_MASK;

			hash ^= k1;
			hash = rotl32(hash, 13);
			hash = (((hash * 5) & UINT_MASK) + 0xE6546B64l) & UINT_MASK;
		}

		//----------
		// tail
		//advance offset to the unprocessed tail of the data
		final int offset = (nblocks << 2);
		long k1 = 0;
		switch(length & 3){
			case 3:
				k1 ^= (data[offset + 2] << 16) & UINT_MASK;

			case 2:
				k1 ^= (data[offset + 1] << 8) & UINT_MASK;

			case 1:
				k1 ^= data[offset];
				k1 = (k1 * X86_32_C1) & UINT_MASK;
				k1 = rotl32(k1, 15);
				k1 = (k1 * X86_32_C2) & UINT_MASK;
				hash ^= k1;
		}

		// ----------
		// finalization
		hash ^= length;
		hash = fmix32(hash);

		return hash;
	}

	/** Rotate left (for 32 bits). */
	private static long rotl32(final long original, final int shift){
		return ((original << shift) & UINT_MASK) | ((original >>> (32 - shift)) & UINT_MASK);
	}

	private static long fmix32(long h){
		h ^= (h >> 16) & UINT_MASK;
		h = (h * 0x85EBCA6Bl) & UINT_MASK;
		h ^= (h >> 13) & UINT_MASK;
		h = (h * 0xC2B2AE35) & UINT_MASK;
		h ^= (h >> 16) & UINT_MASK;
		return h;
	}

	/**
	 * Compute the Murmur3 hash (128-bit version) as described in the original source code.
	 *
	 * @param data	The data that needs to be hashed
	 * @param length	The length of the data that needs to be hashed
	 * @param seed	The seed to use to compute the hash
	 * @return	The computed hash value
	 */
	@SuppressWarnings("fallthrough")
	private static long[] hash_x64_128(final byte[] data, final int length, final long seed){
		long h1 = seed;
		long h2 = seed;

		final ByteBuffer buffer = ByteBuffer.wrap(data);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		while(buffer.remaining() >= 16){
			final long k1 = buffer.getLong();
			final long k2 = buffer.getLong();

			h1 ^= mixK1(k1);

			h1 = Long.rotateLeft(h1, 27);
			h1 += h2;
			h1 = h1 * 5 + 0x52DCE729;

			h2 ^= mixK2(k2);

			h2 = Long.rotateLeft(h2, 31);
			h2 += h1;
			h2 = h2 * 5 + 0x38495AB5;
		}

		// ----------
		// tail
		buffer.compact();
		buffer.flip();

		final int remaining = buffer.remaining();
		if(remaining > 0){
			long k1 = 0;
			long k2 = 0;
			switch(buffer.remaining()){
				case 15:
					k2 ^= (long)(buffer.get(14) & UNSIGNED_MASK) << 48;

				case 14:
					k2 ^= (long)(buffer.get(13) & UNSIGNED_MASK) << 40;

				case 13:
					k2 ^= (long)(buffer.get(12) & UNSIGNED_MASK) << 32;

				case 12:
					k2 ^= (long)(buffer.get(11) & UNSIGNED_MASK) << 24;

				case 11:
					k2 ^= (long)(buffer.get(10) & UNSIGNED_MASK) << 16;

				case 10:
					k2 ^= (long)(buffer.get(9) & UNSIGNED_MASK) << 8;

				case 9:
					k2 ^= (buffer.get(8) & UNSIGNED_MASK);

				case 8:
					k1 ^= buffer.getLong();
					break;

				case 7:
					k1 ^= (long)(buffer.get(6) & UNSIGNED_MASK) << 48;

				case 6:
					k1 ^= (long)(buffer.get(5) & UNSIGNED_MASK) << 40;

				case 5:
					k1 ^= (long)(buffer.get(4) & UNSIGNED_MASK) << 32;

				case 4:
					k1 ^= (long)(buffer.get(3) & UNSIGNED_MASK) << 24;

				case 3:
					k1 ^= (long)(buffer.get(2) & UNSIGNED_MASK) << 16;

				case 2:
					k1 ^= (long)(buffer.get(1) & UNSIGNED_MASK) << 8;

				case 1:
					k1 ^= (buffer.get(0) & UNSIGNED_MASK);
					break;

				default:
					throw new AssertionError("Code should not reach here!");
			}

			// mix
			h1 ^= mixK1(k1);
			h2 ^= mixK2(k2);
		}

		// ----------
		// finalization
		h1 ^= length;
		h2 ^= length;

		h1 += h2;
		h2 += h1;

		h1 = fmix64(h1);
		h2 = fmix64(h2);

		h1 += h2;
		h2 += h1;

		return (new long[]{h1, h2});
	}

	private static long mixK1(long k1){
		k1 *= X64_128_C1;
		k1 = Long.rotateLeft(k1, 31);
		k1 *= X64_128_C2;
		return k1;
	}

	private static long mixK2(long k2){
		k2 *= X64_128_C2;
		k2 = Long.rotateLeft(k2, 33);
		k2 *= X64_128_C1;
		return k2;
	}

	private static long fmix64(long k){
		k ^= k >>> 33;
		k *= 0xFF51AFD7ED558CCDL;
		k ^= k >>> 33;
		k *= 0xC4CEB9FE1A85EC53L;
		k ^= k >>> 33;
		return k;
	}

}
