package unit731.hunspeller.collections.radixtree.sequencers;


public interface SequencerInterface<S>{

	S getNullSequence();

	int length(S sequence);

//	S charAt(S sequence, int index);

	/**
	 * Tests if this sequence starts with the specified prefix.
	 *
	 * @param sequence	The sequence.
	 * @param prefix	The prefix.
	 * @return	{@code true} if the sequence represented by the argument is a prefix of the sequence represented by this sequence; {@code false} otherwise.
	 *				Note also that {@code true} will be returned if the argument is an empty sequence or is equal to this {@code RadixTreeKey} object as
	 *				determined by the {@link #equals(Object)} method.
	 */
	boolean startsWith(S sequence, S prefix);

	boolean equals(S sequenceA, S sequenceB);

	boolean equalsAtIndex(S sequenceA, S sequenceB, int index);

	/**
	 * Returns a sequence that is a subsequence of this sequence.
	 * The subsequence begins at the specified {@code beginIndex} and extends to the character at index {@code endIndex - 1}.
	 * Thus the length of the sequence is {@code endIndex - beginIndex}.
	 * <p>
	 * Examples:
	 * <blockquote><pre>
	 * "hamburger".substring(4, 8) returns "urge"
	 * "smiles".substring(1, 5) returns "mile"
	 * </pre></blockquote>
	 *
	 * @param sequence	The sequence.
	 * @param beginIndex	The beginning index, inclusive.
	 * @param endIndex	The ending index, exclusive.
	 * @return	The specified substring.
	 * @exception IndexOutOfBoundsException	If the {@code beginIndex} is negative, or {@code endIndex} is larger than the length of
	 *														this {@code RadixTreeKey} object, or {@code beginIndex} is larger than {@code endIndex}.
	 */
	S subSequence(S sequence, int beginIndex, int endIndex);

	S concat(S sequence, S other);

	String toString(S sequence);

}
