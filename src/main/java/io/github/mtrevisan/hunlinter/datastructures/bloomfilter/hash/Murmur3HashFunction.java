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
 * Implementation of the MurmurHash3 32-bit and 128-bit hash functions.
 *
 * <p>
 * MurmurHash is a non-cryptographic hash function suitable for general hash-based lookup. The name comes from two basic
 * operations, multiply (MU) and rotate (R), used in its inner loop. Unlike cryptographic hash functions, it is not
 * specifically designed to be difficult to reverse by an adversary, making it unsuitable for cryptographic purposes.
 * </p>
 *
 * <p>
 * This contains a Java port of the 32-bit hash function {@code MurmurHash3_x86_32} and the 128-bit hash function
 * {@code MurmurHash3_x64_128} from Austin Applyby's original {@code c++} code in SMHasher.
 * </p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/MurmurHash">MurmurHash</a>
 * @see <a href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">Original MurmurHash3 c++code</a>
 * @see <a href="https://github.com/apache/hive/blob/master/storage-api/src/java/org/apache/hive/common/util/Murmur3.java">Apache Hive Murmer3</a>
 */
public class Murmur3HashFunction implements HashFunction{

	private static final int SEED = 0x7F3A_21EA;

	/** Helps convert a byte into its unsigned value. */
	private static final int UNSIGNED_MASK = 0xFF;

	private static final int C1_32 = 0xCC9E_2D51;
	private static final int C2_32 = 0x1B87_3593;
	private static final int N_32 = 0xE654_6B64;

	private static final long C1_64 = 0x87C3_7B91_1142_53D5l;
	private static final long C2_64 = 0x4CF5_AD43_2745_937Fl;


	@Override
	public final boolean isSingleValued(){
		return false;
	}

	@Override
	public final int hash(final byte[] bytes){
		return hash32(bytes, 0, SEED);
	}

	@Override
	public final long[] hashMultiple(final byte[] bytes){
		return hash128(bytes, 0, SEED);
	}


	/**
	 * Compute the Murmur3 hash (32-bit version).
	 *
	 * @param data	The data that needs to be hashed.
	 * @param length	The length of the data that needs to be hashed.
	 * @param seed	The seed to use to compute the hash.
	 * @return	The computed hash value.
	 */
	private static int hash32(final byte[] data, final int length, final int seed){
		int hash = seed;

		for(int i = length >> 2; i > 0; i --){
			final int i4 = i << 2;
			int k = (data[i4] & UNSIGNED_MASK);
			k |= (data[i4 + 1] & UNSIGNED_MASK) << 8;
			k |= (data[i4 + 2] & UNSIGNED_MASK) << 16;
			k |= (data[i4 + 3] & UNSIGNED_MASK) << 24;

			hash ^= murmur3Scramble(k);
			hash = Integer.rotateLeft(hash, 13) * 5 + N_32;
		}
		//read the remaining bytes
		int k = 0;
		for(int i = length & 3; i > 0; i --){
			k <<= 8;
			k |= data[i - 1];
		}
		hash ^= murmur3Scramble(k);

		//finalization
		hash ^= length;

		hash = fmix32(hash);
		return hash;
	}

	private static int fmix32(int hash){
		hash ^= (hash >> 16);
		hash *= 0x85EBCA6B;
		hash ^= (hash >> 13);
		hash *= 0xC2B2AE35;
		hash ^= (hash >> 16);
		return hash;
	}

	private static int murmur3Scramble(int k){
		k *= C1_32;
		k = Integer.rotateLeft(k, 15);
		k *= C2_32;
		return k;
	}


	/**
	 * Compute the Murmur3 hash (128-bit version).
	 *
	 * @param data	The data that needs to be hashed.
	 * @param length	The length of the data that needs to be hashed.
	 * @param seed	The seed to use to compute the hash.
	 * @return	The computed hash value.
	 */
	@SuppressWarnings("fallthrough")
	private static long[] hash128(final byte[] data, final int length, final long seed){
		long hash1 = seed;
		long hash2 = seed;

		final ByteBuffer buffer = ByteBuffer.wrap(data);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		while(buffer.remaining() >= 16){
			final long k1 = buffer.getLong();
			final long k2 = buffer.getLong();

			hash1 ^= murmur3Scramble64K1(k1);
			hash1 = Long.rotateLeft(hash1, 27);
			hash1 += hash2;
			hash1 = hash1 * 5 + 0x52DCE729;

			hash2 ^= murmur3Scramble64K2(k2);
			hash2 = Long.rotateLeft(hash2, 31);
			hash2 += hash1;
			hash2 = hash2 * 5 + 0x38495AB5;
		}

		//tail
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
			}

			hash1 ^= murmur3Scramble64K1(k1);
			hash2 ^= murmur3Scramble64K2(k2);
		}

		// finalization
		hash1 ^= length;
		hash2 ^= length;

		hash1 += hash2;
		hash2 += hash1;

		hash1 = fmix64(hash1);
		hash2 = fmix64(hash2);

		hash1 += hash2;
		hash2 += hash1;

		return (new long[]{hash1, hash2});
	}

	private static long murmur3Scramble64K1(long k1){
		k1 *= C1_64;
		k1 = Long.rotateLeft(k1, 31);
		k1 *= C2_64;
		return k1;
	}

	private static long murmur3Scramble64K2(long k2){
		k2 *= C2_64;
		k2 = Long.rotateLeft(k2, 33);
		k2 *= C1_64;
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
