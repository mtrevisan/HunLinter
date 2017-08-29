package unit731.hunspeller.collections.bloomfilter;

import unit731.hunspeller.collections.bloomfilter.core.BitArray;
import unit731.hunspeller.collections.bloomfilter.core.JavaBitArray;


/**
 * An in-memory implementation of the bloom filter.
 * Not suitable for persistence.
 *
 * @param <T> the type of object to be stored in the filter
 */
public class InMemoryBloomFilter<T> extends AbstractBloomFilter<T>{

	public InMemoryBloomFilter(int expectedNumberOfElements, double falsePositiveProbability){
		super(expectedNumberOfElements, falsePositiveProbability);
	}

	/**
	 * NOTE: A normal {@link JavaBitArray} is used.
	 *
	 * @param numBits	The number of required bits in the underlying array
	 * @return the {@link BitArray} implementation to be used
	 */
	@Override
	protected BitArray createBitArray(int numBits){
		return new JavaBitArray(numBits);
	}

}
