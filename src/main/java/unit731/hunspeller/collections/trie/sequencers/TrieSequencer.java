package unit731.hunspeller.collections.trie.sequencers;


public interface TrieSequencer<T>{

	/**
	 * Calculates the hash of the element at the given index in the given sequence. The hash is used as a key to quickly retrieve entries.
	 * Typical implementations based on characters return the ASCII value of the character, since it yields dense numerical values.
	 * The more dense the hashes returned (the smaller the difference between the minimum and maximum returnable hash means it's more dense),
	 * the less space that is wasted.
	 *
	 * @param sequence	The sequence.
	 * @param index		The index of the element to calculate the hash of.
	 * @return	The hash of the element in the sequence at the index.
	 */
	int hashOf(T sequence, int index);

}
