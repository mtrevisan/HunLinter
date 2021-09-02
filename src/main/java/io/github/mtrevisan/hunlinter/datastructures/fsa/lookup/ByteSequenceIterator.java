/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.datastructures.fsa.lookup;

import io.github.mtrevisan.hunlinter.datastructures.fsa.FSA;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * An iterator that traverses the right language of a given node (all sequences
 * reachable from a given node).
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class ByteSequenceIterator implements Iterator<ByteBuffer>{

	/**
	 * Default expected depth of the recursion stack (estimated the longest sequence
	 * in the automaton). Buffers expand by the same value if exceeded.
	 */
	private static final int EXPECTED_MAX_STATES = 15;


	/** The FSA to which this iterator belongs. */
	private final FSA fsa;

	/** An internal cache for the next element in the FSA. */
	private ByteBuffer nextElement;

	/** A buffer for the current sequence of bytes from the current node to the root. */
	private byte[] buffer = new byte[EXPECTED_MAX_STATES];

	/** An arc stack for DFS when processing the automaton. */
	private int[] arcs = new int[EXPECTED_MAX_STATES];
	/** current processing depth in {@link #arcs}. */
	private int arcLimit;


	/**
	 * Create an instance of the iterator iterating over all automaton sequences.
	 *
	 * @param fsa The automaton to iterate over.
	 */
	public ByteSequenceIterator(final FSA fsa){
		this(fsa, fsa.getRootNode());
	}

	/**
	 * Create an instance of the iterator for a given node.
	 *
	 * @param fsa	The automaton to iterate over.
	 * @param node	The starting node's identifier (can be the {@link FSA#getRootNode()}).
	 */
	public ByteSequenceIterator(final FSA fsa, final int node){
		this.fsa = fsa;

		if(fsa.getFirstArc(node) != 0)
			restartFrom(node);
	}

	/**
	 * Restart walking from {@code node}. Allows iterator reuse.
	 *
	 * @param node Restart the iterator from {@code node}.
	 * @return Returns {@code this} for call chaining.
	 */
	public final ByteSequenceIterator restartFrom(final int node){
		arcLimit = 0;
		nextElement = null;

		pushNode(node);
		return this;
	}

	/** Returns {@code true} if there are still elements in this iterator. */
	@Override
	public boolean hasNext(){
		if(nextElement == null)
			nextElement = advance();

		return (nextElement != null);
	}

	/**
	 * @return Returns a {@link ByteBuffer} with the sequence corresponding to the next final state in the automaton.
	 */
	@Override
	public ByteBuffer next(){
		final ByteBuffer cache;
		if(nextElement != null){
			cache = nextElement;

			nextElement = null;
		}
		else{
			cache = advance();
			if(cache == null)
				throw new NoSuchElementException();
		}
		return cache;
	}

	/** Advances to the next available final state. */
	private ByteBuffer advance(){
		if(arcLimit == 0)
			return null;

		while(arcLimit > 0){
			final int lastIndex = arcLimit - 1;
			final int arc = arcs[lastIndex];

			if(arc == 0){
				//remove the current node from the queue
				arcLimit --;
				continue;
			}

			//go to the next arc, but leave it on the stack so that we keep the recursion depth level accurate
			arcs[lastIndex] = fsa.getNextArc(arc);

			//expand buffer if needed
			final int bufferLength = buffer.length;
			if(lastIndex >= bufferLength)
				buffer = Arrays.copyOf(buffer, bufferLength + EXPECTED_MAX_STATES);
			buffer[lastIndex] = fsa.getArcLabel(arc);

			if(!fsa.isArcTerminal(arc))
				//recursively descend into the arc's node
				pushNode(fsa.getEndNode(arc));

			if(fsa.isArcFinal(arc)){
				final ByteBuffer wrapper = ByteBuffer.wrap(buffer);
				wrapper.limit(lastIndex + 1);
				return wrapper;
			}
		}

		return null;
	}

	/** Descends to a given node, adds its arcs to the stack to be traversed. */
	private void pushNode(final int node){
		//expand buffers if needed
		if(arcLimit == arcs.length)
			arcs = Arrays.copyOf(arcs, arcs.length + EXPECTED_MAX_STATES);

		arcs[arcLimit ++] = fsa.getFirstArc(node);
	}

	/** Not implemented in this iterator. */
	@Override
	public void remove(){
		throw new UnsupportedOperationException("Read-only iterator.");
	}

}
