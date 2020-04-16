package unit731.hunlinter.datastructures.fsa.lookup;


import unit731.hunlinter.datastructures.fsa.FSA;
import unit731.hunlinter.datastructures.fsa.builders.FSAFlags;


/**
 * This class implements some common matching and scanning operations on a
 * generic FSA.
 */
public final class FSATraversal{

	/** target automaton */
	private final FSA fsa;


	/**
	 * Traversals of the given FSA.
	 *
	 * @param fsa The target automaton for traversals.
	 */
	public FSATraversal(final FSA fsa){
		this.fsa = fsa;
	}

	/**
	 * Calculate perfect hash for a given input sequence of bytes. The perfect hash requires
	 * that {@link FSA} is built with {@link FSAFlags#NUMBERS} and corresponds to the sequential
	 * order of input sequences used at automaton construction time.
	 *
	 * @param sequence The byte sequence to calculate perfect hash for.
	 * @param start    Start index in the sequence array.
	 * @param length   Length of the byte sequence, must be at least 1.
	 * @param node     The node to start traversal from, typically the {@linkplain FSA#getRootNode() root node}.
	 * @return Returns a unique integer assigned to the input sequence in the automaton (reflecting
	 * the number of that sequence in the input used to build the automaton). Returns a negative
	 * integer if the input sequence was not part of the input from which the automaton was created.
	 * The type of mismatch is a constant defined in {@link MatchResult}.
	 */
	public int perfectHash(final byte[] sequence, final int start, final int length, final int node){
		assert fsa.getFlags().contains(FSAFlags.NUMBERS): "FSA not built with NUMBERS option.";
		assert length > 0: "Must be a non-empty sequence.";

		int hash = 0;
		final int end = start + length - 1;

		int seqIndex = start;
		byte label = sequence[seqIndex];

		//seek through the current node's labels, looking for 'label', update hash
		for(int arc = fsa.getFirstArc(node); arc != 0; ){
			if(fsa.getArcLabel(arc) == label){
				if(fsa.isArcFinal(arc)){
					if(seqIndex == end)
						return hash;

					hash ++;
				}

				if(fsa.isArcTerminal(arc))
					//the automaton contains a prefix of the input sequence
					return MatchResult.AUTOMATON_HAS_PREFIX;

				//the sequence is a prefix of one of the sequences stored in the automaton
				if(seqIndex == end)
					return MatchResult.SEQUENCE_IS_A_PREFIX;

				//make a transition along the arc, go the target node's first arc
				arc = fsa.getFirstArc(fsa.getEndNode(arc));
				label = sequence[++ seqIndex];
				continue;
			}
			else{
				if(fsa.isArcFinal(arc))
					hash ++;
				if(!fsa.isArcTerminal(arc))
					hash += fsa.getRightLanguageCount(fsa.getEndNode(arc));
			}

			arc = fsa.getNextArc(arc);
		}

		if(seqIndex > start)
			return MatchResult.AUTOMATON_HAS_PREFIX;
		else
			//labels of this node ended without a match on the sequence.
			//perfect hash does not exist.
			return MatchResult.NO_MATCH;
	}

	/**
	 * @param sequence The byte sequence to calculate perfect hash for.
	 * @return Returns a unique integer assigned to the input sequence in the automaton (reflecting
	 * the number of that sequence in the input used to build the automaton). Returns a negative
	 * integer if the input sequence was not part of the input from which the automaton was created.
	 * The type of mismatch is a constant defined in {@link MatchResult}.
	 * @see #perfectHash(byte[], int, int, int)
	 */
	public int perfectHash(final byte[] sequence){
		return perfectHash(sequence, 0, sequence.length, fsa.getRootNode());
	}

	/**
	 * Finds a matching path in the dictionary for a given sequence of labels from
	 * <code>sequence</code> and starting at node <code>node</code>.
	 *
	 * @param sequence Input sequence to look for in the automaton.
	 * @param start    Start index in the sequence array.
	 * @param length   Length of the byte sequence, must be at least 1.
	 * @param node     The node to start traversal from, typically the {@linkplain FSA#getRootNode() root node}.
	 * @return {@link MatchResult} with updated match {@link MatchResult#kind}.
	 * @see #match(byte[], int)
	 */
	public MatchResult match(final byte[] sequence, final int start, final int length, int node){
		if(node == 0)
			return new MatchResult(MatchResult.NO_MATCH, start, node);

		final FSA fsa = this.fsa;
		final int end = start + length;
		for(int i = start; i < end; i ++){
			final int arc = fsa.getArc(node, sequence[i]);
			if(arc != 0){
				if(i + 1 == end && fsa.isArcFinal(arc))
					//the automaton has an exact match of the input sequence
					return new MatchResult(MatchResult.EXACT_MATCH, i, node);

				if(fsa.isArcTerminal(arc))
					//the automaton contains a prefix of the input sequence
					return new MatchResult(MatchResult.AUTOMATON_HAS_PREFIX, i + 1, node);

				//make a transition along the arc
				node = fsa.getEndNode(arc);
			}
			else
				return new MatchResult((i > start? MatchResult.AUTOMATON_HAS_PREFIX: MatchResult.NO_MATCH), i, node);
		}

		//the sequence is a prefix of at least one sequence in the automaton
		return new MatchResult(MatchResult.SEQUENCE_IS_A_PREFIX, 0, node);
	}

	/**
	 * @param sequence Input sequence to look for in the automaton.
	 * @param node     The node to start traversal from, typically the {@linkplain FSA#getRootNode() root node}.
	 * @return {@link MatchResult} with updated match {@link MatchResult#kind}.
	 * @see #match(byte[], int)
	 */
	public MatchResult match(final byte[] sequence, final int node){
		return match(sequence, 0, sequence.length, node);
	}

	/**
	 * @param sequence Input sequence to look for in the automaton.
	 * @return {@link MatchResult} with updated match {@link MatchResult#kind}.
	 * @see #match(byte[], int)
	 */
	public MatchResult match(final byte[] sequence){
		return match(sequence, fsa.getRootNode());
	}

}
