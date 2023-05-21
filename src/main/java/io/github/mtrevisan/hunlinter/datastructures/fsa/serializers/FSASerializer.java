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
package io.github.mtrevisan.hunlinter.datastructures.fsa.serializers;

import com.carrotsearch.hppcrt.IntIntMap;
import com.carrotsearch.hppcrt.maps.IntIntHashMap;
import io.github.mtrevisan.hunlinter.datastructures.fsa.FSA;
import io.github.mtrevisan.hunlinter.datastructures.fsa.FSAAbstract;
import io.github.mtrevisan.hunlinter.datastructures.fsa.builders.FSAFlags;
import io.github.mtrevisan.hunlinter.gui.ProgressCallback;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Set;


/**
 * Serializes in-memory {@link FSA} graphs to a binary format compatible with
 * Jan Daciuk's {@code fsa}'s package {@code FSA5} format.
 *
 * <p>
 * It is possible to serialize the automaton with numbers required for perfect
 * hashing. See {@link #serializeWithNumbers()} method.
 * </p>
 *
 * @see FSA
 * @see FSA#read(java.io.InputStream)
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class FSASerializer implements FSASerializerInterface{

	/** Maximum number of bytes for a serialized arc. */
	private static final int MAX_ARC_SIZE = 1 + 5;

	/** Maximum number of bytes for per-node data. */
	private static final int MAX_NODE_DATA_SIZE = 16;

	/** Number of bytes for the arc's flags header (arc representation without the goto address). */
	private static final int SIZEOF_FLAGS = 1;

	/** Supported flags. */
	private static final Set<FSAFlags> SUPPORTED_FLAGS = Collections.unmodifiableSet(EnumSet.of(FSAFlags.NUMBERS, FSAFlags.SEPARATORS,
		FSAFlags.FLEXIBLE, FSAFlags.STOPBIT, FSAFlags.NEXTBIT));

	public byte fillerByte = FSA.DEFAULT_FILLER;

	public byte annotationByte = FSA.DEFAULT_ANNOTATION;

	/**
	 * {@code true} if we should serialize with numbers.
	 *
	 * @see #serializeWithNumbers()
	 */
	private boolean serializeWithNumbers;

	/** A hash map of [state, offset] pairs. */
	private final IntIntMap offsets = new IntIntHashMap();

	/** A hash map of [state, right-language-count] pairs. */
	private IntIntMap numbers = new IntIntHashMap();


	/**
	 * Serialize the automaton with the number of right-language sequences in each node.
	 * <p>
	 * This is required to implement perfect hashing. The numbering also preserves the order of input sequences.
	 * </p>
	 *
	 * @return Returns the same object for easier call chaining.
	 */
	@Override
	public final FSASerializer serializeWithNumbers(){
		serializeWithNumbers = true;

		return this;
	}

	@Override
	public final FSASerializer withFiller(final byte filler){
		fillerByte = filler;

		return this;
	}

	@Override
	public final FSASerializer withAnnotationSeparator(final byte annotationSeparator){
		annotationByte = annotationSeparator;

		return this;
	}

	/**
	 * Serialize root state {@code s} to an output stream in {@code FSA} format.
	 *
	 * @return	{@code os} for chaining.
	 * @see #serializeWithNumbers()
	 */
	@Override
	public final <T extends OutputStream> T serialize(final FSAAbstract fsa, final T os, final ProgressCallback progressCallback)
			throws IOException{
		//prepare space for arc offsets and linearize all the states
		final int[] linearized = linearize(fsa);
		if(progressCallback != null)
			progressCallback.accept(20);

		//calculate the number of bytes required for the node data, if serializing with numbers
		int nodeDataLength = 0;
		if(serializeWithNumbers){
			numbers = rightLanguageForAllStates(fsa);
			int maxNumber = numbers.get(fsa.getRootNode());
			while(maxNumber > 0){
				nodeDataLength ++;
				maxNumber >>>= 8;
			}
		}
		if(progressCallback != null)
			progressCallback.accept(40);

		//calculate minimal goto length
		int gtl = 1;
		while(true){
			//first pass: calculate offsets of states
			if(!emitArcs(fsa, null, linearized, gtl, nodeDataLength)){
				gtl ++;

				continue;
			}

			//second pass: check if goto overflows anywhere
			if(emitArcs(fsa, null, linearized, gtl, nodeDataLength))
				break;

			gtl ++;
		}
		if(progressCallback != null)
			progressCallback.accept(60);

		//emit the header
		FSAHeader.write(os, FSA.VERSION);
		os.write(fillerByte);
		os.write(annotationByte);
		os.write((nodeDataLength << 4) | gtl);
		if(progressCallback != null)
			progressCallback.accept(80);

		//emit the automaton
		final boolean gtlUnchanged = emitArcs(fsa, os, linearized, gtl, nodeDataLength);
		assert gtlUnchanged: "gtl changed in the final pass.";

		if(progressCallback != null)
			progressCallback.accept(100);

		return os;
	}

	/**
	 * Calculate the size of "right language" for each state in an FSA.
	 * The right language is the number of sequences encoded from a given node in the automaton.
	 *
	 * @param fsa	The automaton to calculate right language for
	 * @return	A map with node identifiers as keys and their right language counts as associated values
	 */
	private static IntIntHashMap rightLanguageForAllStates(final FSAAbstract fsa){
		final IntIntHashMap numbers = new IntIntHashMap();
		fsa.visitPostOrder(state -> {
			int thisNodeNumber = 0;
			for(int arc = fsa.getFirstArc(state); arc != 0; arc = fsa.getNextArc(arc))
				thisNodeNumber += (fsa.isArcFinal(arc)? 1: 0)
					+ (fsa.isArcTerminal(arc)? 0: numbers.get(fsa.getEndNode(arc)));
			numbers.put(state, thisNodeNumber);

			return true;
		});
		return numbers;
	}

	/** Return supported flags. */
	@Override
	public final Set<FSAFlags> getSupportedFlags(){
		return SUPPORTED_FLAGS;
	}

	/** Linearization of states. */
	private static int[] linearize(final FSAAbstract fsa){
		int[] linearized = new int[0];
		int last = 0;

		final BitSet visited = new BitSet();
		final Deque<Integer> nodes = new LinkedList<>();
		nodes.push(fsa.getRootNode());

		while(!nodes.isEmpty()){
			final int node = nodes.pop();
			if(visited.get(node))
				continue;

			if(last >= linearized.length)
				linearized = Arrays.copyOf(linearized, linearized.length + 100000);

			visited.set(node);
			linearized[last ++] = node;

			for(int arc = fsa.getFirstArc(node); arc != 0; arc = fsa.getNextArc(arc))
				if(!fsa.isArcTerminal(arc)){
					final int target = fsa.getEndNode(arc);
					if(!visited.get(target))
						nodes.push(target);
				}
		}

		return Arrays.copyOf(linearized, last);
	}

	/** Update arc offsets assuming the given goto length. */
	private boolean emitArcs(final FSAAbstract fsa, final OutputStream os, final int[] linearized, final int gtl, final int nodeDataLength)
			throws IOException{
		final ByteBuffer bb = ByteBuffer.allocate(Math.max(MAX_NODE_DATA_SIZE, MAX_ARC_SIZE));

		int offset = 0;

		// Add dummy terminal state.
		offset += emitNodeData(bb, os, nodeDataLength, 0);
		offset += emitArc(bb, os, gtl, 0, (byte)0, 0);

		// Add epsilon state.
		offset += emitNodeData(bb, os, nodeDataLength, 0);
		if(fsa.getRootNode() != 0)
			offset += emitArc(bb, os, gtl, FSA.BIT_LAST_ARC | FSA.BIT_TARGET_NEXT, (byte)'^', 0);
		else
			offset += emitArc(bb, os, gtl, FSA.BIT_LAST_ARC, (byte)'^', 0);

		for(int j = 0; j < linearized.length; j ++){
			final int s = linearized[j];

			if(os == null)
				offsets.put(s, offset);

			offset += emitNodeData(bb, os, nodeDataLength, serializeWithNumbers? numbers.get(s): 0);

			for(int arc = fsa.getFirstArc(s); arc != 0; arc = fsa.getNextArc(arc)){
				int targetOffset;
				final int target;
				if(fsa.isArcTerminal(arc)){
					targetOffset = 0;
					target = 0;
				}
				else{
					target = fsa.getEndNode(arc);
					targetOffset = offsets.get(target);
				}

				int flags = 0;
				if(fsa.isArcFinal(arc))
					flags |= FSA.BIT_FINAL_ARC;
				if(fsa.getNextArc(arc) == 0){
					flags |= FSA.BIT_LAST_ARC;
					if(j + 1 < linearized.length && target == linearized[j + 1] && targetOffset != 0){
						flags |= FSA.BIT_TARGET_NEXT;
						targetOffset = 0;
					}
				}

				final int bytes = emitArc(bb, os, gtl, flags, fsa.getArcLabel(arc), targetOffset);
				if(bytes < 0)
					//gtl too small, interrupt eagerly
					return false;

				offset += bytes;
			}
		}

		return true;
	}

	private static int emitArc(final ByteBuffer bb, final OutputStream os, final int gtl, int flags, final byte label, final int targetOffset)
			throws IOException{
		final int arcBytes = ((flags & FSA.BIT_TARGET_NEXT) != 0? SIZEOF_FLAGS: gtl);

		flags |= (targetOffset << 3);
		bb.put(label);
		for(int b = 0; b < arcBytes; b ++){
			bb.put((byte)flags);
			flags >>>= 8;
		}

		if(flags != 0)
			//gtl too small, interrupt eagerly
			return -1;

		bb.flip();
		final int bytes = bb.remaining();
		if(os != null)
			os.write(bb.array(), bb.position(), bb.remaining());
		bb.clear();

		return bytes;
	}

	private static int emitNodeData(final ByteBuffer bb, final OutputStream os, final int nodeDataLength, int number) throws IOException{
		if(nodeDataLength > 0 && os != null){
			for(int i = 0; i < nodeDataLength; i ++){
				bb.put((byte)number);
				number >>>= 8;
			}

			bb.flip();
			os.write(bb.array(), bb.position(), bb.remaining());
			bb.clear();
		}

		return nodeDataLength;
	}

}
