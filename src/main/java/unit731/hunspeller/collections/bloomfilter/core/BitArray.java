package unit731.hunspeller.collections.bloomfilter.core;

import java.io.Closeable;


/**
 * A contract for all implementations of bit-arrays. This provides specific methods that will be needed for working with bloom filters.
 */
public interface BitArray extends Closeable{

	/**
	 * Get the bit at index
	 *
	 * @param index the index of the bit in the array
	 *
	 * @return <code>true</code> if the but is set, <code>false</code> otherwise
	 */
	boolean getBit(int index);

	/**
	 * Set the bit at index
	 *
	 * @param index the index of the bit in the array
	 *
	 * @return <code>true</code> if the bit was updated, <code>false</code> otherwise.
	 *
	 */
	boolean setBit(int index);

	/**
	 * Clear all bits in the array.
	 *
	 */
	void clear();

	/**
	 * Clear a given bit at the index.
	 *
	 * @param index the index of the bit in the array
	 */
	void clearBit(int index);

	/**
	 * Set the bit at index if the bit is unset.
	 *
	 * @param index the index of the bit in the array
	 *
	 * @return <code>true</code> if the bit was updated, <code>false</code> otherwise.
	 */
	boolean setBitIfUnset(int index);

	/**
	 * Do a Boolean OR with the second {@link BitArray}.
	 *
	 * @param bitArray the bitArray to OR with
	 */
	void or(BitArray bitArray);

	/**
	 * Do a Boolean AND with the second {@link BitArray}.
	 *
	 * @param bitArray the bitArray to AND with
	 */
	void and(BitArray bitArray);

	/**
	 * The space used by this {@link BitArray} in number of bytes.
	 *
	 * @return the number of bytes being used
	 */
	int bitSize();

}
