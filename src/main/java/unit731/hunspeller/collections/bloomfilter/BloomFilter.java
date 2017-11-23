package unit731.hunspeller.collections.bloomfilter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import unit731.hunspeller.collections.bloomfilter.core.BitArray;
import unit731.hunspeller.collections.bloomfilter.core.BitArrayBuilder;
import unit731.hunspeller.collections.bloomfilter.decompose.ByteSink;
import unit731.hunspeller.collections.bloomfilter.decompose.Decomposer;
import unit731.hunspeller.collections.bloomfilter.decompose.DefaultDecomposer;
import unit731.hunspeller.collections.bloomfilter.hash.HashFunction;
import unit731.hunspeller.collections.bloomfilter.hash.Murmur3HashFunction;


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
 */
@Slf4j
public class BloomFilter<T> implements BloomFilterInterface<T>{

	private static final double LN2 = Math.log(2);
	private static final double LN2_SQUARE = LN2 * LN2;

	/** The decomposer to use when there is none specified at construction */
	private static final Decomposer<Object> DEFAULT_DECOMPOSER = new DefaultDecomposer();
	/** The default hasher to use if one is not specified */
	private static final HashFunction DEFAULT_HASHER = new Murmur3HashFunction();

	/** The default {@link Charset} is the platform encoding charset */
	protected transient Charset currentCharset = Charset.defaultCharset();


	protected final BitArrayBuilder.Type type;
	/** The {@link BitArray} instance that holds the entire data */
	private final BitArray bitArray;
	/** Optimal number of hash functions based on the size of the Bloom filter and the expected number of inserted elements */
	private final int hashFunctions;

	private final Decomposer<T> decomposer;
	/** The hashing method to be used for hashing */
	private final HashFunction hasher;
	/** Expected (maximum) number of elements to be added without to transcend the falsePositiveProbability */
	protected int expectedElements;
	/** The maximum false positive probability rate that the bloom filter can give */
	@Getter protected double falsePositiveProbability;
	/** Number of bits required for the bloom filter */
	private final int bitsRequired;

	/** Number of elements actually added to the Bloom filter */
	@Getter protected int addedElements;


	/**
	 * Create a new bloom filter.
	 *
	 * @param type								The type of the bit array
	 * @param expectedNumberOfElements	The number of max expected insertions
	 * @param falsePositiveProbability	The max false positive probability rate that the bloom filter can give
	 */
	public BloomFilter(BitArrayBuilder.Type type, int expectedNumberOfElements, double falsePositiveProbability){
		this(type, expectedNumberOfElements, falsePositiveProbability, null, null);
	}

	/**
	 * Create a new bloom filter.
	 *
	 * @param type								The type of the bit array
	 * @param expectedNumberOfElements	The number of max expected insertions
	 * @param falsePositiveProbability	The max false positive probability rate that the bloom filter can give
	 * @param decomposer	A {@link Decomposer} that helps decompose the given object
	 */
	public BloomFilter(BitArrayBuilder.Type type, int expectedNumberOfElements, double falsePositiveProbability, Decomposer<T> decomposer){
		this(type, expectedNumberOfElements, falsePositiveProbability, decomposer, null);
	}

	/**
	 * Create a new bloom filter.
	 *
	 * @param type								The type of the bit array
	 * @param expectedNumberOfElements	The number of max expected insertions
	 * @param falsePositiveProbability	The max false positive probability rate that the bloom filter can give
	 * @param decomposer	A {@link Decomposer} that helps decompose the given object
	 * @param hasher	The hash function to use. If <code>null</code> is specified the {@link AbstractBloomFilter#DEFAULT_HASHER} will be used
	 */
	public BloomFilter(BitArrayBuilder.Type type, int expectedNumberOfElements, double falsePositiveProbability, Decomposer<T> decomposer, HashFunction hasher){
		if(expectedNumberOfElements <= 0)
			throw new IllegalArgumentException("Number of elements must be strict positive");
		if(falsePositiveProbability <= 0. || falsePositiveProbability >= 1.)
			throw new IllegalArgumentException("False positive probability must be in ]0, 1[ interval");

		this.type = type;
		expectedElements = expectedNumberOfElements;
		this.falsePositiveProbability = falsePositiveProbability;

		bitsRequired = optimalBitSize(expectedNumberOfElements, falsePositiveProbability);
		hashFunctions = optimalNumberOfHashFunctions(falsePositiveProbability);
		bitArray = BitArrayBuilder.getBitArray(type, bitsRequired);

		this.decomposer = decomposer;
		this.hasher = (hasher != null? hasher: DEFAULT_HASHER);

		addedElements = 0;
	}

	//Default bloom filter functions follow
	/**
	 * Compute the optimal size <code>m</code> of the bloom filter in bits.
	 *
	 * @param expectedNumberOfElements	The number of expected insertions, or <code>n</code>
	 * @param falsePositiveProbability	The maximum false positive rate expected, or <code>p</code>
	 * @return the optimal size in bits for the filter, or <code>m</code>
	 */
	public static int optimalBitSize(double expectedNumberOfElements, double falsePositiveProbability){
		return (int)Math.round(-expectedNumberOfElements * Math.log(falsePositiveProbability) / LN2_SQUARE);
	}

	/**
	 * Compute the optimal number of hash functions, <code>k</code>
	 *
	 * @param falsePositiveProbability	The max false positive probability rate that the bloom filter can give
	 * @return the optimal number of hash functions to be used also known as <code>k</code>
	 */
	public static int optimalNumberOfHashFunctions(double falsePositiveProbability){
		return Math.max(1, (int)Math.round(-Math.log(falsePositiveProbability) / LN2));
	}

	//Main functions that govern the bloom filter
	/**
	 * Add the given value represented as bytes in to the bloom filter.
	 *
	 * @param bytes	The bytes to be added to bloom filter
	 * @return <code>true</code> if any bit was modified when adding the value, <code>false</code> otherwise
	 */
	public boolean add(byte[] bytes){
		boolean bitsChanged = calculateIndexes(bytes, index -> {});
		addedElements ++;
		return bitsChanged;
	}

	private List<Integer> indexes(byte[] bytes){
		List<Integer> indexes = new ArrayList<>();
		calculateIndexes(bytes, indexes::add);
		return indexes;
	}

	/**
	 * NOTE: use the trick mentioned in "Less Hashing, Same Performance: Building a Better Bloom Filter" by Kirsch et.al.
	 * From abstract 'only two hash functions are necessary to effectively implement a Bloom filter without any loss in the
	 * asymptotic false positive probability'.
	 * Lets split up 64-bit hashcode into two 32-bit hashcodes and employ the technique mentioned in the above paper
	 */
	private boolean calculateIndexes(byte[] bytes, Consumer<Integer> callback){
		boolean bitsChanged = false;
		long hash64 = getLongHash64(bytes);
		int hash1 = (int)hash64;
		int hash2 = (int)(hash64 >>> 32);
		for(int i = 1; i <= hashFunctions; i ++){
			int nextHash = hash1 + i * hash2;
			//hashcode should be positive, flip all the bits if it's negative
			if(nextHash < 0)
				nextHash = ~nextHash;
			int index = nextHash % bitArray.bitSize();
			bitsChanged |= bitArray.set(index);

			callback.accept(index);
		}
		return bitsChanged;
	}

	public boolean contains(byte[] bytes){
		//NOTE: use the trick mentioned in "Less Hashing, Same Performance: Building a Better Bloom Filter"
		//by Kirsch et.al. From abstract 'only two hash functions are necessary to effectively
		//implement a Bloom filter without any loss in the asymptotic false positive probability'
		//Lets split up 64-bit hashcode into two 32-bit hashcodes and employ the technique mentioned
		//in the above paper
		long hash64 = getLongHash64(bytes);
		int hash1 = (int)hash64;
		int hash2 = (int)(hash64 >>> 32);
		for(int i = 1; i <= hashFunctions; i ++){
			int nextHash = hash1 + i * hash2;
			//hashcode should be positive, flip all the bits if it's negative
			if(nextHash < 0)
				nextHash = ~nextHash;
			int index = nextHash % bitArray.bitSize();
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
	 * @throws NullPointerException	if the byte array is <code>null</code>
	 */
	private long getLongHash64(byte[] bytes){
		Objects.requireNonNull(bytes, "Bytes to add to bloom filter cannot be null");

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
	private byte[] decomposeValue(T value){
		ByteSink sink = new ByteSink();
		if(decomposer != null)
			decomposer.decompose(value, sink, currentCharset);
		else
			DEFAULT_DECOMPOSER.decompose(value, sink, currentCharset);
		return sink.getByteArray();
	}

	//Overridden helper functions follow
	@Override
	public boolean add(T value){
		return (value != null && add(decomposeValue(value)));
	}

	@Override
	public List<Integer> indexes(T value){
		return (value != null? indexes(decomposeValue(value)): Collections.<Integer>emptyList());
	}

	@Override
	public boolean contains(T value){
		return (value != null && contains(value.toString().getBytes(currentCharset)));
	}

	@Override
	public void setCharset(Charset charset){
		Objects.requireNonNull(charset, "Charset to be changed to cannot be null");

		currentCharset = charset;
	}

	@Override
	public boolean isFull(){
		return (addedElements >= expectedElements);
	}

	@Override
	public double getExpectedFalsePositiveProbability(){
		return getTrueFalsePositiveProbability(expectedElements);
	}

	@Override
	public double getTrueFalsePositiveProbability(){
		return getTrueFalsePositiveProbability(addedElements);
	}

	@Override
	public double getTrueFalsePositiveProbability(int insertedElements){
		//(1 - e^(-k * n / m)) ^ k
		return Math.pow((1 - Math.exp(-hashFunctions * (double)insertedElements / (double)bitsRequired)), hashFunctions);
	}

	/** Sets all bits to false in the Bloom filter. */
	@Override
	public void clear(){
		bitArray.clearAll();
		addedElements = 0;
	}

	@Override
	public void close(){
		if(bitArray instanceof Closeable){
			try{
				((Closeable)bitArray).close();
			}
			catch(IOException e){
				log.error("Error closing the Bloom filter", e);
			}
		}
	}

}
