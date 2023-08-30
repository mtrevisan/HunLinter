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
package io.github.mtrevisan.hunlinter.datastructures.bloomfilter;

import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.core.BitArray;
import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.core.BitArrayBuilder;
import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.decompose.ByteSink;
import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.decompose.Decomposer;
import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.decompose.DefaultDecomposer;
import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.hash.HashFunction;
import io.github.mtrevisan.hunlinter.datastructures.bloomfilter.hash.Murmur3HashFunction;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;


/**
 * An in-memory implementation of the bloom filter.
 * Not suitable for persistence.
 *
 * The default composer is a simple {@link Object#toString()} decomposer which then converts this {@link String} into raw bytes.
 * The default {@link HashFunction} used by the bloom filter is the {@link Murmur3HashFunction}.
 *
 * One may override the decomposer to be used, the hash function to be used as well as the implementation of the {@link BitArray} that
 * needs to be used.
 *
 * @param <T> the type of objects to be stored in the filter
 *
 * @see <a href="https://github.com/sangupta/bloomfilter">Bloom Filter 0.9.0</a>
 * @see <a href="https://github.com/lidanh/BPlusTree/blob/master/IntegerBloomFilter.java">IntegerBloomFilter.java</a>
 */
public class BloomFilter<T> implements BloomFilterInterface<T>{

	private static final Logger LOGGER = LoggerFactory.getLogger(BloomFilter.class);

	private static final String WRONG_NUMBER_OF_ELEMENTS = "Number of elements must be strictly positive";
	private static final String WRONG_FALSE_POSITIVE_PROBABILITY = "False positive probability must be in ]0, 1[ interval";

	private static final double LN2 = Math.log(2.);
	private static final double LN2_SQUARE = LN2 * LN2;

	/** The decomposer to use when there is none specified at construction. */
	private final Decomposer<T> decomposerDefault = new DefaultDecomposer<>();
	/** The default hasher to use if one is not specified. */
	private static final HashFunction HASHER_DEFAULT = new Murmur3HashFunction();


	/** The default {@link Charset} is the platform encoding charset. */
	private final Charset charset;
	/** The {@link BitArray} instance that holds the entire data. */
	private final BitArray bitArray;
	/** Optimal number of hash functions based on the size of the Bloom filter and the expected number of inserted elements. */
	private final int hashFunctions;
	private final Decomposer<T> decomposer;
	/** The hashing method to be used for hashing. */
	private final HashFunction hasher;
	/** Expected (maximum) number of elements to be added without to transcend the falsePositiveProbability. */
	private final int expectedElements;
	/** The maximum false positive probability rate that the bloom filter can give. */
	private final double falsePositiveProbability;
	/** Number of bits required for the bloom filter. */
	private final int bitsRequired;

	/** Number of elements actually added to the Bloom filter. */
	private volatile int addedElements;


	/**
	 * Create a new bloom filter.
	 *
	 * @param charset	The {@link Charset} to be used.
	 * @param parameters	The parameters object.
	 */
	public BloomFilter(final Charset charset, final BloomFilterParameters parameters){
		this(charset, parameters, null, null);
	}

	/**
	 * Create a new bloom filter.
	 *
	 * @param charset	The {@link Charset} to be used.
	 * @param parameters	The parameters object.
	 * @param decomposer	A {@link Decomposer} that helps decompose the given object.
	 */
	public BloomFilter(final Charset charset, final BloomFilterParameters parameters, final Decomposer<T> decomposer){
		this(charset, parameters, decomposer, null);
	}

	/**
	 * Create a new bloom filter.
	 *
	 * @param charset	The {@link Charset} to be used.
	 * @param parameters	The parameters object.
	 * @param decomposer	A {@link Decomposer} that helps decompose the given object.
	 * @param hasher	The hash function to use. If {@code null} is specified the {@link #HASHER_DEFAULT} will be used.
	 */
	public BloomFilter(final Charset charset, final BloomFilterParameters parameters, final Decomposer<T> decomposer, final HashFunction hasher){
		this(charset, parameters.getExpectedNumberOfElements(), parameters.getFalsePositiveProbability(), parameters.getBitArrayType(),
			decomposer, hasher);
	}

	/**
	 * Create a new bloom filter.
	 *
	 * @param charset	The {@link Charset} to be used.
	 * @param expectedNumberOfElements	The number of max expected insertions.
	 * @param falsePositiveProbability	The max false positive probability rate that the bloom filter can give.
	 * @param bitArrayType	The type of the bit array.
	 * @param decomposer	A {@link Decomposer} that helps decompose the given object.
	 * @param hasher	The hash function to use. If {@code null} is specified the {@link #HASHER_DEFAULT} will be used.
	 */
	protected BloomFilter(final Charset charset, final int expectedNumberOfElements, final double falsePositiveProbability,
			final BitArrayBuilder.Type bitArrayType, final Decomposer<T> decomposer, final HashFunction hasher){
		Objects.requireNonNull(charset, "Charset cannot be null");
		Objects.requireNonNull(bitArrayType, "Bit array type cannot be null");
		if(expectedNumberOfElements <= 0)
			throw new IllegalArgumentException(WRONG_NUMBER_OF_ELEMENTS);
		if(falsePositiveProbability <= 0. || falsePositiveProbability >= 1.)
			throw new IllegalArgumentException(WRONG_FALSE_POSITIVE_PROBABILITY);

		this.charset = charset;
		expectedElements = expectedNumberOfElements;
		this.falsePositiveProbability = falsePositiveProbability;

		bitsRequired = optimalBitSize(expectedNumberOfElements, falsePositiveProbability);
		hashFunctions = optimalNumberOfHashFunctions(falsePositiveProbability);
		bitArray = BitArrayBuilder.getBitArray(bitArrayType, bitsRequired);

		this.decomposer = decomposer;
		this.hasher = ObjectUtils.defaultIfNull(hasher, HASHER_DEFAULT);

		addedElements = 0;
	}

	@Override
	public final double getFalsePositiveProbability(){
		return falsePositiveProbability;
	}

	@Override
	public final synchronized int getAddedElements(){
		return addedElements;
	}

	//Default bloom filter functions follow
	/**
	 * Compute the optimal size {@code m} of the bloom filter in bits.
	 *
	 * @param expectedNumberOfElements	The number of expected insertions, or {@code n}.
	 * @param falsePositiveProbability	The maximum false positive rate expected, or {@code p}.
	 * @return the optimal size in bits for the filter, or {@code m}.
	 */
	public static int optimalBitSize(final double expectedNumberOfElements, final double falsePositiveProbability){
		final int bitSize = (int)Math.round(-expectedNumberOfElements * Math.log(falsePositiveProbability) / LN2_SQUARE);
		return (bitSize >= 0? bitSize: Integer.MAX_VALUE);
	}

	/**
	 * Compute the optimal number of hash functions, {@code k}.
	 *
	 * @param falsePositiveProbability	The max false positive probability rate that the bloom filter can give.
	 * @return the optimal number of hash functions to be used also known as {@code k}.
	 */
	public static int optimalNumberOfHashFunctions(final double falsePositiveProbability){
		return Math.max(1, (int)Math.round(-Math.log(falsePositiveProbability) / LN2));
	}

	//Main functions that govern the bloom filter
	/**
	 * Add the given value represented as bytes in to the bloom filter.
	 *
	 * @param bytes	The bytes to be added to bloom filter.
	 * @return {@code true} if any bit was modified when adding the value, {@code false} otherwise.
	 */
	public final synchronized boolean add(final byte[] bytes){
		final boolean bitsChanged = calculateIndexes(bytes);
		if(bitsChanged)
			addedElements ++;
		return bitsChanged;
	}

	/**
	 * NOTE: use the trick mentioned in "Less hashing, same performance: building a better Bloom filter" by Kirsch et al.
	 *		From abstract 'only two hash functions are necessary to effectively implement a Bloom filter without any loss in the
	 *		asymptotic false positive probability'.
	 *		Let's split up 64-bit hashcode into two 32-bit hashcodes and employ the technique mentioned in the above paper
	 */
	private synchronized boolean calculateIndexes(final byte[] bytes){
		boolean bitsChanged = false;
		final long hash = getLongHash64(bytes);
		final int lowHash = (int)hash;
		final int highHash = (int)(hash >>> 32);
		final int size = bitArray.size();
		for(int i = 1; i <= hashFunctions; i ++){
			int nextHash = lowHash + i * highHash;
			//hashcode should be positive, flip all the bits if it's negative
			if(nextHash < 0)
				nextHash = ~nextHash;

			final int index = nextHash % size;
			bitsChanged |= bitArray.set(index);
		}
		return bitsChanged;
	}

	/*
	 * NOTE: use the trick mentioned in "Less hashing, same performance: building a better Bloom filter" by Kirsch et al.
	 *		From abstract 'only two hash functions are necessary to effectively implement a Bloom filter without any loss in the
	 *		asymptotic false positive probability'.
	 *		Let's split up 64-bit hashcode into two 32-bit hashcodes and employ the technique mentioned in the above paper
	 */
	public final synchronized boolean contains(final byte[] bytes){
		final long hash = getLongHash64(bytes);
		final int lowHash = (int)hash;
		final int highHash = (int)(hash >>> 32);
		final int size = bitArray.size();
		for(int i = 1; i <= hashFunctions; i ++){
			int nextHash = lowHash + i * highHash;
			//hashcode should be positive, flip all the bits if it's negative
			if(nextHash < 0)
				nextHash = ~nextHash;

			final int index = nextHash % size;
			if(!bitArray.get(index))
				return false;
		}
		return true;
	}

	//Helper functions for functionality within
	/**
	 * Compute one 64-bit hash from the given byte-array using the specified {@link HashFunction}.
	 *
	 * @param bytes	The byte-array to use for hash computation
	 * @return the 64-bit hash
	 * @throws NullPointerException	if the byte array is {@code null}
	 */
	private long getLongHash64(final byte[] bytes){
		return (hasher.isSingleValued()? hasher.hash(bytes): hasher.hashMultiple(bytes)[0]);
	}

	/**
	 * Given the value object, decompose it into a byte-array so that hashing
	 * can be done over the returned bytes. If a custom {@link Decomposer} has
	 * been specified, it will be used, otherwise the {@link DefaultDecomposer}
	 * will be used.
	 *
	 * @param value	The value to be decomposed
	 * @return the decomposed byte array
	 */
	private byte[] decomposeValue(final T value) throws IOException{
		final ByteSink sink = new ByteSink();
		Objects.requireNonNullElse(decomposer, decomposerDefault)
			.decompose(value, sink, charset);
		return sink.getByteArray();
	}

	//Overridden helper functions follow
	@Override
	public final boolean add(final T value){
		try{
			return (value != null && add(decomposeValue(value)));
		}
		catch(final IOException ignored){}
		return false;
	}

	@Override
	public final boolean contains(final T value){
		return (value != null && contains(value.toString().getBytes(charset)));
	}

	@Override
	public final synchronized boolean isFull(){
		return (addedElements >= expectedElements);
	}

	@Override
	public final double getExpectedFalsePositiveProbability(){
		return getTrueFalsePositiveProbability(expectedElements);
	}

	@Override
	public final synchronized double getTrueFalsePositiveProbability(){
		return getTrueFalsePositiveProbability(addedElements);
	}

	@Override
	public final double getTrueFalsePositiveProbability(final int insertedElements){
		//(1 - e^(-k * n / m)) ^ k
		return Math.pow((1. - Math.exp(-hashFunctions * (double)insertedElements / bitsRequired)), hashFunctions);
	}

	/** Sets all bits to false in the Bloom filter. */
	@Override
	public final synchronized void clear(){
		bitArray.clearAll();
		addedElements = 0;
	}

	@Override
	public final synchronized void close(){
		try{
			bitArray.close();
		}
		catch(final IOException ioe){
			LOGGER.error("Error closing the Bloom filter", ioe);
		}
	}

}
