package unit731.hunlinter.services.fsa;

import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntDeque;
import com.carrotsearch.hppc.IntStack;
import com.carrotsearch.hppc.cursors.IntCursor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * This is a top abstract class for handling Finite State Automata.
 * These automata are arc-based, a design described in Jan Daciuk's <i>Incremental Construction of Finite-State Automata
 * and Transducers, and their use in the Natural Language Processing</i> (PhD thesis, Technical University of Gdansk).
 *
 * @see <a href="http://www.jandaciuk.pl/thesis/thesis.html">Incremental Construction of Finite-State Automata and Transducers, and their use in the Natural Language Processing</>
 * @see "org.carrot2.morfologik-parent, 2.1.8-SNAPSHOT, 2020-01-02"
 */
public abstract class FSA implements Iterable<ByteBuffer>{

	/**
	 * @return Returns the identifier of the root node of this automaton.
	 * Returns 0 if the start node is also the end node (the automaton is empty).
	 */
	public abstract int getRootNode();

	/**
	 * @param node Identifier of the node.
	 * @return Returns the identifier of the first arc leaving <code>node</code> or 0 if the node has no outgoing arcs.
	 */
	public abstract int getFirstArc(final int node);

	/**
	 * @param arc The arc's identifier.
	 * @return Returns the identifier of the next arc after <code>arc</code> and leaving <code>node</code>.
	 * 	Zero is returned if no more arcs are available for the node.
	 */
	public abstract int getNextArc(final int arc);

	/**
	 * @param node	Identifier of the node.
	 * @param label	The arc's label.
	 * @return	The identifier of an arc leaving <code>node</code> and labeled with <code>label</code>.
	 * 	An identifier equal to 0 means the node has no outgoing arc labeled <code>label</code>.
	 */
	public abstract int getArc(final int node, final byte label);

	/**
	 * @param arc The arc's identifier.
	 * @return Return the label associated with a given <code>arc</code>.
	 */
	public abstract byte getArcLabel(final int arc);

	/**
	 * @param arc The arc's identifier.
	 * @return Returns <code>true</code> if the destination node at the end of this <code>arc</code> corresponds to
	 * 	an input sequence created when building this automaton.
	 */
	public abstract boolean isArcFinal(final int arc);

	/**
	 * @param arc The arc's identifier.
	 * @return Returns whether this <code>arc</code> does NOT have a terminating node
	 * 	(@link {@link #getEndNode(int)} will throw an exception). Implies {@link #isArcFinal(int)}.
	 */
	public abstract boolean isArcTerminal(final int arc);

	/**
	 * @param arc The arc's identifier.
	 * @return Return the end node pointed to by a given <code>arc</code>.
	 * 	Terminal arcs (those that point to a terminal state) have no end node representation and throw a runtime exception.
	 */
	public abstract int getEndNode(final int arc);

	/**
	 * @return Returns a set of flags for this FSA instance.
	 */
	public abstract Set<FSAFlags> getFlags();

	/**
	 * @param node Identifier of the node.
	 * @return Calculates and returns the number of arcs of a given node.
	 */
	public int getArcCount(final int node){
		int count = 0;
		for(int arc = getFirstArc(node); arc != 0; arc = getNextArc(arc))
			count ++;
		return count;
	}

	/**
	 * @param node	Identifier of the node.
	 * @return	The number of sequences reachable from the given state if the automaton was compiled with {@link FSAFlags#NUMBERS}. The size
	 * 	of the right language of the state, in other words.
	 * @throws UnsupportedOperationException	If the automaton was not compiled with {@link FSAFlags#NUMBERS}.
	 *		The value can then be computed by manual count of {@link #getSequences}.
	 */
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
	 * @param node Identifier of the starting node from which to return subsequences.
	 * @return An iterable over all sequences encoded starting at the given node.
	 */
	public Iterable<ByteBuffer> getSequences(final int node){
		return (node > 0? () -> new ByteSequenceIterator(FSA.this, node): Collections.emptyList());
	}

	/**
	 * An alias of calling {@link #iterator} directly ({@link FSA} is also
	 * {@link Iterable}).
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
	public final Iterator<ByteBuffer> iterator(){
		return getSequences().iterator();
	}

	/**
	 * Visit all states. The order of visiting is undefined. This method may be
	 * faster than traversing the automaton in post or preorder since it can scan
	 * states linearly. Returning false from {@link StateVisitor#accept(int)}
	 * immediately terminates the traversal.
	 *
	 * @param v   Visitor to receive traversal calls.
	 * @param <T> A subclass of {@link StateVisitor}.
	 * @return Returns the argument (for access to anonymous class fields).
	 */
	public <T extends StateVisitor> T visitAllStates(final T v){
		return visitInPostOrder(v);
	}

	/**
	 * Same as {@link #visitInPostOrder(StateVisitor, int)}, starting from root
	 * automaton node.
	 *
	 * @param v   Visitor to receive traversal calls.
	 * @param <T> A subclass of {@link StateVisitor}.
	 * @return Returns the argument (for access to anonymous class fields).
	 */
	public <T extends StateVisitor> T visitInPostOrder(final T v){
		return visitInPostOrder(v, getRootNode());
	}

	/**
	 * Visits all states reachable from <code>node</code> in postorder. Returning
	 * false from {@link StateVisitor#accept(int)} immediately terminates the
	 * traversal.
	 *
	 * @param v    Visitor to receive traversal calls.
	 * @param <T>  A subclass of {@link StateVisitor}.
	 * @param node Identifier of the node.
	 * @return Returns the argument (for access to anonymous class fields).
	 */
	public <T extends StateVisitor> T visitInPostOrder(final T v, final int node){
		visitInPostOrder2(v, node, new BitSet());
		return v;
	}

	private void visitInPostOrder2(final StateVisitor v, final int node, final BitSet ignored){
		final IntStack stack = new IntStack();
		//push root node to first stack
		stack.push(node);

		//post-order traversal stack
		final IntDeque out = new IntArrayDeque();

		//loop while first stack is not empty
		while(!stack.isEmpty()){
			final int current = stack.pop();

			//pop a node from first stack and push it to second stack
			out.addFirst(current);

			//push children of the popped node from left to right to first stack
			for(int arc = getFirstArc(current); arc != 0; arc = getNextArc(arc))
				if(!isArcTerminal(arc))
					stack.add(getEndNode(arc));
		}

		List<Integer> o0 = new ArrayList<>();

		//process nodes from second stack
		final BitSet visited = new BitSet();
		final int[] oo = out.toArray();
		for(int i = 0; i < oo.length; i ++){
			final int n = oo[i];
			if(!visited.get(n)){
				v.accept(n);
				o0.add(n);
			}

			visited.set(n);
		}

		String o1 = Arrays.toString(o0.toArray());
		String o2 = Arrays.toString(new int[]{
			92166, 92172, 92178, 92184, 92190, 92196, 92202, 92208, 92214, 92220, 92226, 92232, 92238, 92244, 92250, 92256, 92262,
			92268, 92274, 92280, 92286, 92292, 92298, 92304, 92310, 92316, 92322, 92328, 92334, 92340, 92346, 92352, 92358, 92364,
			92370, 92376, 92382, 92388, 92394, 92400, 92406, 92412, 92418, 92424, 92430, 92436, 92442, 92448, 92454, 92460, 92466,
			92472, 92478, 92484, 92490, 92496, 92502, 92508, 92514, 92520, 92526, 92532, 92538, 92544, 92550, 92556, 92562, 92568,
			92574, 92580, 92586, 92592, 92598, 92604, 92610, 92616, 92622, 92628, 92634, 92640, 92646, 92652, 92658, 92664, 92670,
			92676, 92682 ,92688, 92694, 92700, 92706, 92712, 92718, 92724, 92730, 92736, 92742, 92748, 92754, 92760, 92766, 92772,
			92778, 92784, 92790, 92796, 92802, 92808, 92814, 92820, 92826, 92832, 92838, 92844, 92850, 92856, 92862, 92868, 92874,
			92880, 92886, 92892, 92898, 92904, 92910, 92916, 92922, 92928, 92934, 92940, 92946, 92952, 92958, 92964, 92970, 92976,
			92982, 92988, 92994, 93000, 93006, 93012, 93018, 93024, 93030
		});
		if(!o1.equals(o2))
			throw new IllegalArgumentException("different!");
	}

	//FIXME recursion to iteration!
	private boolean visitInPostOrder(final StateVisitor v, final int node, final BitSet visited){
		if(visited.get(node))
			return true;

		visited.set(node);

		for(int arc = getFirstArc(node); arc != 0; arc = getNextArc(arc))
			if(!isArcTerminal(arc) && !visitInPostOrder(v, getEndNode(arc), visited))
				return false;
System.out.println(node);
		return v.accept(node);
	}

	/**
	 * Same as {@link #visitInPreOrder(StateVisitor, int)}, starting from root
	 * automaton node.
	 *
	 * @param v   Visitor to receive traversal calls.
	 * @param <T> A subclass of {@link StateVisitor}.
	 * @return Returns the argument (for access to anonymous class fields).
	 */
	public <T extends StateVisitor> T visitInPreOrder(final T v){
		return visitInPreOrder(v, getRootNode());
	}

	/**
	 * Visits all states in preorder. Returning false from
	 * {@link StateVisitor#accept(int)} skips traversal of all sub-states of a
	 * given state.
	 *
	 * @param v    Visitor to receive traversal calls.
	 * @param <T>  A subclass of {@link StateVisitor}.
	 * @param node Identifier of the node.
	 * @return Returns the argument (for access to anonymous class fields).
	 */
	public <T extends StateVisitor> T visitInPreOrder(final T v, final int node){
		visitInPreOrder(v, node, new BitSet());
		return v;
	}

	/**
	 * @param in The input stream.
	 * @return Reads all remaining bytes from an input stream and returns
	 * them as a byte array.
	 * @throws IOException Rethrown if an I/O exception occurs.
	 */
	protected static byte[] readRemaining(final InputStream in) throws IOException{
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final byte[] buffer = new byte[1024 * 8];
		int len;
		while((len = in.read(buffer)) >= 0)
			baos.write(buffer, 0, len);
		return baos.toByteArray();
	}

	//FIXME recursion to iteration!
	private void visitInPreOrder(final StateVisitor v, final int node, final BitSet visited){
		if(visited.get(node))
			return;

		visited.set(node);

		if(v.accept(node))
			for(int arc = getFirstArc(node); arc != 0; arc = getNextArc(arc))
				if(!isArcTerminal(arc))
					visitInPreOrder(v, getEndNode(arc), visited);
	}

	/**
	 * A factory for reading automata in any of the supported versions.
	 *
	 * @param stream The input stream to read automaton data from. The stream is not closed.
	 * @return Returns an instantiated automaton. Never null.
	 * @throws IOException If the input stream does not represent an automaton or is otherwise invalid.
	 */
	public static FSA read(final InputStream stream) throws IOException{
		final FSAHeader header = FSAHeader.read(stream);
		switch(header.version){
			case FSA5.VERSION:
				return new FSA5(stream);

			case CFSA.VERSION:
				return new CFSA(stream);

			case CFSA2.VERSION:
				return new CFSA2(stream);

			default:
				throw new IOException(String.format(Locale.ROOT, "Unsupported automaton version: 0x%02x",
					header.version & 0xFF));
		}
	}

	/**
	 * A factory for reading a specific FSA subclass, including proper casting.
	 *
	 * @param stream The input stream to read automaton data from. The stream is not closed.
	 * @param clazz  A subclass of {@link FSA} to cast the read automaton to.
	 * @param <T>    A subclass of {@link FSA} to cast the read automaton to.
	 * @return Returns an instantiated automaton. Never null.
	 * @throws IOException If the input stream does not represent an automaton, is otherwise
	 *                     invalid or the class of the automaton read from the input stream
	 *                     is not assignable to <code>clazz</code>.
	 */
	public static <T extends FSA> T read(final InputStream stream, final Class<? extends T> clazz) throws IOException{
		final FSA fsa = read(stream);
		if(!clazz.isInstance(fsa))
			throw new IOException(String.format(Locale.ROOT, "Expected FSA type %s, but read an incompatible type %s.",
				clazz.getName(), fsa.getClass().getName()));

		return clazz.cast(fsa);
	}

}
