/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.datastructures.fsa;

import com.carrotsearch.hppcrt.IntSet;
import com.carrotsearch.hppcrt.sets.IntHashSet;
import io.github.mtrevisan.hunlinter.datastructures.dynamicarray.DynamicIntArray;
import io.github.mtrevisan.hunlinter.datastructures.fsa.builders.FSAFlags;
import io.github.mtrevisan.hunlinter.datastructures.fsa.lookup.ByteSequenceIterator;
import io.github.mtrevisan.hunlinter.datastructures.fsa.serializers.FSAHeader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;


/**
 * This is a top abstract class for handling Finite State Automata.
 * These automata are arc-based, a design described in Jan Daciuk's <i>Incremental Construction of Finite-State Automata
 * and Transducers, and their use in the Natural Language Processing</i> (PhD thesis, Technical University of Gdańsk).
 *
 * @see <a href="http://www.jandaciuk.pl/thesis/thesis.html">Incremental Construction of Finite-State Automata and Transducers, and their use in the Natural Language Processing</a>
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public abstract class FSAAbstract implements Iterable<ByteBuffer>{

	/**
	 * @return Returns the identifier of the root node of this automaton.
	 * Returns 0 if the start node is also the end node (the automaton is empty).
	 */
	public abstract int getRootNode();

	/**
	 * @param node Identifier of the node.
	 * @return Returns the identifier of the first arc leaving {@code node} or 0 if the node has no outgoing arcs.
	 */
	public abstract int getFirstArc(final int node);

	/**
	 * @param arc The arc's identifier.
	 * @return Returns the identifier of the next arc after {@code arc} and leaving {@code node}.
	 * 	Zero is returned if no more arcs are available for the node.
	 */
	public abstract int getNextArc(final int arc);

	/**
	 * @param node	Identifier of the node.
	 * @param label	The arc's label.
	 * @return	The identifier of an arc leaving {@code node} and labeled with {@code label}.
	 * 	An identifier equal to 0 means the node has no outgoing arc labeled {@code label}.
	 */
	public final int getArc(final int node, final byte label){
		for(int arc = getFirstArc(node); arc != 0; arc = getNextArc(arc))
			if(getArcLabel(arc) == label)
				return arc;
		//an arc labeled with "label" not found
		return 0;
	}

	/**
	 * @param arc The arc's identifier.
	 * @return Return the label associated with a given {@code arc}.
	 */
	public abstract byte getArcLabel(final int arc);

	/**
	 * @param arc The arc's identifier.
	 * @return Returns {@code true} if the destination node at the end of this {@code arc} corresponds to
	 * 	an input sequence created when building this automaton.
	 */
	public abstract boolean isArcFinal(final int arc);

	/**
	 * @param arc The arc's identifier.
	 * @return Returns whether this {@code arc} does NOT have a terminating node
	 * 	(@link {@link #getEndNode(int)} will throw an exception). Implies {@link #isArcFinal(int)}.
	 */
	public abstract boolean isArcTerminal(final int arc);

	/**
	 * @param arc The arc's identifier.
	 * @return Return the end node pointed to by a given {@code arc}.
	 * 	Terminal arcs (those that point to a terminal state) have no end node representation and throw a runtime exception.
	 */
	public abstract int getEndNode(final int arc);

	/**
	 * @return Returns a set of flags for this FSA instance.
	 */
	public abstract Set<FSAFlags> getFlags();

	/**
	 * @param node	Identifier of the node.
	 * @return	The number of sequences reachable from the given state if the automaton was compiled with {@link FSAFlags#NUMBERS}. The size
	 * 	of the right language of the state, in other words.
	 * @throws UnsupportedOperationException	If the automaton was not compiled with {@link FSAFlags#NUMBERS}.
	 *		The value can then be computed by manual count of {@link #getSequences}.
	 */
	@SuppressWarnings("DesignForExtension")
	public int getRightLanguageCount(final int node){
		throw new UnsupportedOperationException("Automaton not compiled with " + FSAFlags.NUMBERS);
	}

	/**
	 * Returns an iterator over all binary sequences starting at the given FSA
	 * state (node) and ending in final nodes. This corresponds to a set of
	 * suffixes of a given prefix from all sequences stored in the automaton.
	 *
	 * <p>
	 * The returned iterator is a {@link ByteBuffer} whose contents changes on
	 * each call to {@link Iterator#next()}. The keep the contents between calls
	 * to {@link Iterator#next()}, one must copy the buffer to some other
	 * location.
	 * </p>
	 *
	 * <p>
	 * <b>Important.</b> It is guaranteed that the returned byte buffer is backed
	 * by a byte array and that the content of the byte buffer starts at the
	 * array's index 0.
	 * </p>
	 *
	 * @param node	Identifier of the starting node from which to return subsequences.
	 * @return	An iterable over all sequences encoded starting at the given node.
	 */
	public final Iterable<ByteBuffer> getSequences(final int node){
		return (node > 0? () -> new ByteSequenceIterator(this, node): Collections.emptyList());
	}

	/**
	 * An alias of calling {@link #iterator} directly.
	 *
	 * @return Returns all sequences encoded in the automaton.
	 */
	public final Iterable<ByteBuffer> getSequences(){
		return getSequences(getRootNode());
	}

	/**
	 * Returns an iterator over all binary sequences starting from the initial FSA
	 * state (node) and ending in final nodes. The returned iterator is a
	 * {@link ByteBuffer} whose contents changes on each call to
	 * {@link Iterator#next()}. The keep the contents between calls to
	 * {@link Iterator#next()}, one must copy the buffer to some other location.
	 *
	 * <p>
	 * <b>Important.</b> It is guaranteed that the returned byte buffer is backed
	 * by a byte array and that the content of the byte buffer starts at the
	 * array's index 0.
	 * </p>
	 */
	@Override
	public final Iterator<ByteBuffer> iterator(){
		return getSequences().iterator();
	}

	/**
	 * Visits all states reachable from the root node in post-order.
	 * Returning false from {@link StateVisitor#accept(int)} skips traversal of all sub-states of a given state.
	 *
	 * @param v   Visitor to receive traversal calls.
	 */
	public final void visitPostOrder(final StateVisitor v){
		final DynamicIntArray stack = new DynamicIntArray();
		//push root node to first stack
		stack.push(getRootNode());

		//post-order traversal stack
		final DynamicIntArray out = new DynamicIntArray();

		//loop while first stack is not empty
		while(!stack.isEmpty()){
			final int current = stack.pop();

			//pop a node from first stack and push it to second stack
			out.push(current);

			//push children of the popped node from left to right to first stack
			for(int arc = getFirstArc(current); arc != 0; arc = getNextArc(arc))
				if(!isArcTerminal(arc))
					stack.add(getEndNode(arc));
		}

		//process nodes from second stack
		final IntSet visited = new IntHashSet(out.size() - 1);
		for(int i = out.size() - 1; i >= 0; i --){
			final int state = out.get(i);
			if(!visited.contains(state) && !v.accept(state))
				break;

			visited.add(state);
		}
	}

	/**
	 * @param in The input stream.
	 * @return Reads all remaining bytes from an input stream and returns
	 * them as a byte array.
	 * @throws IOException Rethrown if an I/O exception occurs.
	 */
	protected static byte[] readRemaining(final InputStream in) throws IOException{
		try(final ByteArrayOutputStream baos = new ByteArrayOutputStream()){
			final byte[] buffer = new byte[1024 * 8];
			int len;
			while((len = in.read(buffer)) >= 0)
				baos.write(buffer, 0, len);
			return baos.toByteArray();
		}
	}

	/**
	 * A factory for reading automata in any of the supported versions.
	 *
	 * @param stream The input stream to read automaton data from. The stream is not closed.
	 * @return Returns an instantiated automaton. Never null.
	 * @throws IOException If the input stream does not represent an automaton or is otherwise invalid.
	 */
	public static FSAAbstract read(final InputStream stream) throws IOException{
		final FSAHeader header = FSAHeader.read(stream);
		return switch(header.getVersion()){
			case FSA.VERSION -> new FSA(stream);
			case CFSA.VERSION -> new CFSA(stream);
			default -> throw new IOException(String.format(Locale.ROOT, "Unsupported automaton version: 0x%02x", header.getVersion() & 0xFF));
		};
	}

	/**
	 * A factory for reading a specific FSA subclass, including proper casting.
	 *
	 * @param stream The input stream to read automaton data from. The stream is not closed.
	 * @param clazz  A subclass of this class to cast the read automaton to.
	 * @param <T>    A subclass of this class to cast the read automaton to.
	 * @return Returns an instantiated automaton. Never null.
	 * @throws IOException If the input stream does not represent an automaton, is otherwise
	 *                     invalid or the class of the automaton read from the input stream
	 *                     is not assignable to {@code clazz}.
	 */
	public static <T extends FSAAbstract> T read(final InputStream stream, final Class<? extends T> clazz) throws IOException{
		final FSAAbstract fsa = read(stream);
		if(!clazz.isInstance(fsa))
			throw new IOException(String.format(Locale.ROOT, "Expected FSA type %s, but read an incompatible type %s.",
				clazz.getSimpleName(), fsa.getClass().getSimpleName()));

		return clazz.cast(fsa);
	}

}
