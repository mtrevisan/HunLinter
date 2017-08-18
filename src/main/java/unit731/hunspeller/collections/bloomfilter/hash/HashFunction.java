package unit731.hunspeller.collections.bloomfilter.hash;


/**
 * A contract for all implementation that want to provide a hash
 * function for use inside the bloom filters.
 */
public interface HashFunction{

	/**
	 * Whether the hash function returns a single long hash or multiple long values.
	 *
	 * @return whether the hash function returns a single value of multiple values
	 */
	boolean isSingleValued();

	/**
	 * Return the hash of the bytes as long.
	 *
	 * @param bytes	The bytes to be hashed
	 * @return the generated hash value
	 */
	long hash(byte[] bytes);

	/**
	 * Return the hash of the bytes as a long array.
	 *
	 * @param bytes	The bytes to be hashed
	 * @return the generated hash value
	 */
	long[] hashMultiple(byte[] bytes);

}
