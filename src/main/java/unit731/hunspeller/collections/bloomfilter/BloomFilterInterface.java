package unit731.hunspeller.collections.bloomfilter;

import java.nio.charset.Charset;
import unit731.hunspeller.collections.bloomfilter.decompose.Decomposer;


/**
 * A simple Bloom filter contract.
 * Bloom Filter is a probabilistic data structure for set membership check. Bloom Filters are
 * highly space efficient when compared to using a HashSet. Because of the probabilistic nature of
 * bloom filter false positive (element not present in bloom filter but contains() says true) are
 * possible but false negatives are not possible (if element is present then contains() will never
 * say false). The false positive probability is configurable. Lower the false positive probability,
 * greater is the space requirement.
 * Bloom filters are sensitive to number of elements that will be inserted in the bloom filter.
 * During the creation of bloom filter expected number of entries must be specified. If the number
 * of insertions exceed the specified initial number of entries then false positive probability will
 * increase accordingly.
 * Internally, this implementation of bloom filter uses Murmur3 fast non–cryptographic hash
 * algorithm. Although Murmur2 is slightly faster than Murmur3 in Java, it suffers from hash
 * collisions for specific sequence of repeating bytes. Check the following link for more info
 * https://code.google.com/p/smhasher/wiki/MurmurHash2Flaw
 * For a description see here <a href="http://en.wikipedia.org/wiki/Bloom_filter">Bloom Filter</a>.
 *
 * Cheat sheet:
 *
 * m: total bits
 * n: expected insertions
 * k: number of hashes per element
 * b: m/n, bits per insertion
 * p: expected false positive probability
 *
 * 1) Optimal k = b * ln(2)
 * 2) p = (1 - e^(-k * n/m))^k
 * 3) For optimal k: p = 2^(-k) ~= 0.6185^b
 * 4) For optimal k: m = -n * ln(p) / ln^2(2)
 *
 * @param <T> the type of objects to be stored in the filter
 */
public interface BloomFilterInterface<T>{

	/**
	 * Add the given value object to the bloom filter by decomposing it using
	 * the given/default {@link Decomposer}
	 *
	 * @param value	The object to be added to the bloom filter
	 * @return <code>true</code> if the value was added to the bloom filter, <code>false</code> otherwise
	 */
	boolean add(T value);

	/**
	 * Check if the value object is present in the bloom filter or not by decomposing it using the given/default decomposer
	 *
	 * @param value	The object to be tested for existence in bloom filter
	 * @return <code>false</code> if the value is definitely (100% surety) not contained in the bloom filter, <code>true</code> otherwise.
	 */
	boolean contains(T value);

	/**
	 * Override the default charset that will be used when decomposing the
	 * {@link String} values into byte arrays. The default {@link Charset} used
	 * in the platform's default {@link Charset}.
	 *
	 * @param charset	The {@link Charset} to be used
	 * @throws NullPointerException	if the charset is null
	 */
	void setCharset(Charset charset);

	/**
	 * Get the number of added elements.
	 *
	 * @return the number of added elements
	 */
	int getAddedElements();

	/**
	 * Returns a boolean indicating if the Bloom filter has reached its maximal capacity.
	 * 
	 * @return Whether the Bloom filter has reached its maximal capacity.
	 */
	boolean isFull();

	double getFalsePositiveProbability();

	/**
	 * Calculates the expected probability of false positives based on the number of expected filter elements and the size of the Bloom filter.
	 * The value returned by this method is the <i>expected</i> rate of false positives, assuming the number of inserted elements equals
	 * the number of expected elements. If the number of elements in the Bloom filter is less than the expected value, the true probability
	 * of false positives will be lower.
	 *
	 * @return the expected false positive rate
	 */
	double getExpectedFalsePositiveProbability();

	/**
	 * Get the current probability of a false positive.
	 * The probability is calculated from the size of the Bloom filter and the current number of elements added to it.
	 *
	 * @return the approximated false positive rate
	 */
	double getTrueFalsePositiveProbability();

	/**
	 * Estimate the current false positive rate (approximated) when given number of elements have been inserted in to the filter.
	 *
	 * @param numInsertedElements	The number of elements inserted into the filter
	 * @return the approximated false positive rate
	 */
	double getTrueFalsePositiveProbability(int numInsertedElements);

	/** Clear the Bloom filter. */
	void clear();

	/** Close down the Bloom filter and flush any pending changes to the disk. */
	void close();

}
