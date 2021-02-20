/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package unit731.hunlinter.datastructures.fsa.lookup;

import unit731.hunlinter.datastructures.fsa.FSA;
import unit731.hunlinter.datastructures.fsa.builders.FSAFlags;


/** This class implements some common matching and scanning operations on a generic FSA */
public final class FSATraversal{

	/** target automaton */
	private final FSA fsa;


	/**
	 * Traversals of the given FSA.
	 *
	 * @param fsa	The target automaton for traversals.
	 */
	public FSATraversal(final FSA fsa){
		this.fsa = fsa;
	}

	/**
	 * @param sequence The byte sequence to calculate perfect hash for.
	 * @return Returns a unique integer assigned to the input sequence in the automaton (reflecting
	 * the number of that sequence in the input used to build the automaton). Returns a negative
	 * integer if the input sequence was not part of the input from which the automaton was created.
	 * The type of mismatch is a constant defined in {@link FSAMatchResult}.
	 * @see #perfectHash(byte[], int, int, int)
	 */
	public int perfectHash(final byte[] sequence){
		return perfectHash(sequence, 0, sequence.length, fsa.getRootNode());
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
	 * The type of mismatch is a constant defined in {@link FSAMatchResult}.
	 */
	public int perfectHash(final byte[] sequence, final int start, final int length, final int node){
		if(!fsa.getFlags().contains(FSAFlags.NUMBERS))
			throw new IllegalArgumentException("FSA not built with NUMBERS option.");

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
					return FSAMatchResult.AUTOMATON_HAS_PREFIX;

				//the sequence is a prefix of one of the sequences stored in the automaton
				if(seqIndex == end)
					return FSAMatchResult.PREFIX_MATCH;

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
			return FSAMatchResult.AUTOMATON_HAS_PREFIX;
		else
			//labels of this node ended without a match on the sequence, perfect hash does not exist
			return FSAMatchResult.NO_MATCH;
	}

	/**
	 * @param sequence	Input sequence to look for in the automaton.
	 * @return	{@link FSAMatchResult} with updated match {@link FSAMatchResult#kind}.
	 */
	public FSAMatchResult match(final byte[] sequence){
		return match(sequence, 0, sequence.length, fsa.getRootNode());
	}

	/**
	 * @param sequence	Input sequence to look for in the automaton.
	 * @param node	The node to start traversal from, typically the {@linkplain FSA#getRootNode() root node}.
	 * @return	{@link FSAMatchResult} with updated match {@link FSAMatchResult#kind}.
	 */
	public FSAMatchResult match(final byte[] sequence, final int node){
		return match(sequence, 0, sequence.length, node);
	}

	/**
	 * Finds a matching path in the dictionary for a given sequence of labels from
	 * <code>sequence</code> and starting at node <code>node</code>.
	 *
	 * @param sequence	Input sequence to look for in the automaton.
	 * @param start	Start index in the sequence array.
	 * @param length	Length of the byte sequence, must be at least 1.
	 * @param node	The node to start traversal from, typically the {@linkplain FSA#getRootNode() root node}.
	 * @return	{@link FSAMatchResult} with updated match {@link FSAMatchResult#kind}.
	 */
	public FSAMatchResult match(final byte[] sequence, final int start, final int length, int node){
		if(node == 0)
			return new FSAMatchResult(FSAMatchResult.NO_MATCH, start, node);

		final int end = start + length;
		for(int i = start; i < end; i ++){
			final int arc = fsa.getArc(node, sequence[i]);
			if(arc == 0)
				return new FSAMatchResult((i > start? FSAMatchResult.AUTOMATON_HAS_PREFIX: FSAMatchResult.NO_MATCH), i, node);

			if(i + 1 == end && fsa.isArcFinal(arc))
				//the automaton has an exact match of the input sequence
				return new FSAMatchResult(FSAMatchResult.EXACT_MATCH, i, node);

			if(fsa.isArcTerminal(arc))
				//the automaton contains a prefix of the input sequence
				return new FSAMatchResult(FSAMatchResult.AUTOMATON_HAS_PREFIX, i + 1, node);

			//make a transition along the arc
			node = fsa.getEndNode(arc);
		}

		//the sequence is a prefix of at least one sequence in the automaton
		return new FSAMatchResult(FSAMatchResult.PREFIX_MATCH, 0, node);
	}

}
