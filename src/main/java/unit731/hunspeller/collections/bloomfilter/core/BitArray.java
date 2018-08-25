package unit731.hunspeller.collections.bloomfilter.core;

import java.io.Closeable;
import java.io.IOException;


/**
 * A contract for all implementations of bit-arrays. This provides specific methods that will be needed for working with bloom filters.
 */
public interface BitArray extends Closeable{

	/**
	 * Get the bit at index
	 *
	 * @param index the index of the bit in the array
	 * @return <code>true</code> if the but is set, <code>false</code> otherwise
	 */
	boolean get(int index);

	/**
	 * Set the bit at index
	 *
	 * @param index the index of the bit in the array
	 * @return <code>true</code> if the bit was updated, <code>false</code> otherwise.
	 *
	 */
	boolean set(int index);

	/**
	 * Clear a given bit at the index.
	 *
	 * @param index the index of the bit in the array
	 */
	void clear(int index);

	/**
	 * Clear all bits in the array.
	 */
	void clearAll();

	/**
	 * The space used by this {@link BitArray} in number of bits.
	 *
	 * @return the number of bits being used
	 */
	int size();

	@Override
	default void close() throws IOException{}

}
