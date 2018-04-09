package unit731.hunspeller.collections.radixtree.sequencers;

import java.io.Serializable;
import java.util.Map;
import unit731.hunspeller.collections.radixtree.RadixTreeNode;


public interface RadixTreeSequencerInterface<S, H>{

	/**
	 * Calculates the length of the sequence.
	 * 
	 * @param sequence	The sequence.
	 * @return	The sequence length.
	 */
	int lengthOf(S sequence);

	/**
	 * Calculates the length of the sequence.
	 * 
	 * @param sequence	The sequence.
	 * @param prefix		The prefix the sequence should be compared to.
	 * @return	Whether the sequence starts with the given text.
	 */
	boolean startsWith(S sequence, S prefix);

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
	H hashOf(S sequence, int index);

	/**
	 * Returns the child that corresponds with the given stem.
	 * 
	 * @param <V>	The value type.
	 * @param children	The map of children.
	 * @param stem			The stem used to search the child in the given map.
	 * @return	The child searched, or <code>null</code> if not found.
	 */
	<V extends Serializable> RadixTreeNode<V> getChild(Map<H, RadixTreeNode<V>> children, H stem);

	/**
	 * Determines the maximum number of elements that match between sequences A and B where comparison starts at the given indices up to
	 * the given count for inserting a new sequence.
	 *
	 * @param sequenceA	The first sequence to count matches on.
	 * @param indexA		The offset into the first sequence.
	 * @param sequenceB	The second sequence to count matches on.
	 * @param indexB		The offset into the second sequence.
	 * @param maxCount	The maximum number of matches to search for.
	 * @return	A number between <code>0</code> (inclusive) and <code>maxCount</code> (inclusive) that is the number of matches between the two sequence
	 *		sections.
	 */
	int matchesPut(S sequenceA, int indexA, S sequenceB, int indexB, int maxCount);

	/**
	 * Determines the maximum number of elements that match between sequences A and B where comparison starts at the given indices up to
	 * the given count for retrieving a sequence.
	 *
	 * @param nodeSequence		The first sequence to count matches on.
	 * @param nodeIndex			The offset into the first sequence.
	 * @param searchSequence	The second sequence to count matches on.
	 * @param searchIndex		The offset into the second sequence.
	 * @param maxCount	The maximum number of matches to search for.
	 * @return	A number between <code>0</code> (inclusive) and <code>maxCount</code> (inclusive) that is the number of matches between the two sequence
	 *		sections.
	 */
	int matchesGet(S nodeSequence, int nodeIndex, S searchSequence, int searchIndex, int maxCount);

}
