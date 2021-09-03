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
package io.github.mtrevisan.hunlinter.datastructures.fsa.serializers;

import com.carrotsearch.hppcrt.IntIntMap;
import com.carrotsearch.hppcrt.cursors.IntIntCursor;
import com.carrotsearch.hppcrt.maps.IntIntHashMap;
import io.github.mtrevisan.hunlinter.datastructures.dynamicarray.DynamicIntArray;
import io.github.mtrevisan.hunlinter.datastructures.fsa.CFSA2;
import io.github.mtrevisan.hunlinter.datastructures.fsa.FSA;
import io.github.mtrevisan.hunlinter.datastructures.fsa.builders.FSAFlags;
import io.github.mtrevisan.hunlinter.gui.ProgressCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;


/**
 * Serializes in-memory {@link FSA} graphs to {@link CFSA2 Compact Finite State Automata, version 2}.
 *
 * <p>
 * It is possible to serialize the automaton with numbers required for perfect hashing.
 * See {@link #serializeWithNumbers()} method.
 * </p>
 *
 * @see CFSA2
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class CFSA2Serializer implements FSASerializer{

	private static final Logger LOGGER = LoggerFactory.getLogger(CFSA2Serializer.class);

	/** Supported flags. */
	private static final Set<FSAFlags> SUPPORTED_FLAGS = EnumSet.of(FSAFlags.NUMBERS, FSAFlags.FLEXIBLE, FSAFlags.STOPBIT,
		FSAFlags.NEXTBIT);

	/** No-state id. */
	private static final int NO_STATE = -1;

	@SuppressWarnings("SubtractionInCompareTo")
	private static final Comparator<IntIntHolder> COMPARATOR = (o1, o2) -> {
		final int countDiff = o2.b - o1.b;
		return (countDiff == 0? o1.a - o2.a: countDiff);
	};


	/**
	 * Whether to serialize with numbers.
	 *
	 * @see #serializeWithNumbers()
	 */
	private boolean serializeWithNumbers;

	/** A hash map of [state, offset] pairs. */
	private final IntIntHashMap offsets = new IntIntHashMap();
	/** A hash map of [state, right-language-count] pairs. */
	private IntIntHashMap numbers;
	/** The most frequent labels for integrating with the flags field. */
	private byte[] labelsIndex;
	/**
	 * Inverted index of labels to be integrated with flags field. A label at index <code>i<code> has the index or zero
	 * (no integration).
	 */
	private int[] invertedLabelsIndex;


	/**
	 * Serialize the automaton with the number of right-language sequences in each node. This is required to implement
	 * perfect hashing. The numbering also preserves the order of input sequences.
	 *
	 * @return Returns the same object for easier call chaining.
	 */
	@Override
	public CFSA2Serializer serializeWithNumbers(){
		serializeWithNumbers = true;
		return this;
	}

	/**
	 * Serializes any {@link FSA} to {@link CFSA2} stream.
	 *
	 * @return	{@code os} for chaining.
	 * @see #serializeWithNumbers()
	 */
	@Override
	public <T extends OutputStream> T serialize(final FSA fsa, final T os, final ProgressCallback progressCallback)
			throws IOException{
		//calculate the most frequent labels and build indexed labels dictionary
		computeLabelsIndex(fsa);
		if(progressCallback != null)
			progressCallback.accept(20);

		//calculate the number of bytes required for the node data, if serializing with numbers
		numbers = (serializeWithNumbers? FSAUtils.rightLanguageForAllStates(fsa): new IntIntHashMap());

		//linearize all the states, optimizing their layout
		final DynamicIntArray linearized = linearize(fsa);
		if(progressCallback != null)
			progressCallback.accept(40);

		//emit the header
		FSAHeader.write(os, CFSA2.VERSION);

		final Set<FSAFlags> fsaFlags = EnumSet.of(FSAFlags.FLEXIBLE, FSAFlags.STOPBIT, FSAFlags.NEXTBIT);
		if(serializeWithNumbers)
			fsaFlags.add(FSAFlags.NUMBERS);
		if(progressCallback != null)
			progressCallback.accept(60);

		final short flagsMask = FSAFlags.getMask(fsaFlags);
		os.write((flagsMask >> 8) & 0xFF);
		os.write(flagsMask & 0xFF);

		//emit labels index
		os.write(labelsIndex.length);
		os.write(labelsIndex);
		if(progressCallback != null)
			progressCallback.accept(80);

		//emit the automaton
		final int size = emitNodes(fsa, os, linearized);
		if(size != 0)
			throw new IllegalArgumentException("Size changed in the final pass: " + size);

		if(progressCallback != null)
			progressCallback.accept(100);

		return os;
	}

	/** Compute a set of labels to be integrated with the flags field. */
	private void computeLabelsIndex(final FSA fsa){
		//compute labels count
		final int[] countByValue = new int[256];
		fsa.visitPostOrder(state -> {
			for(int arc = fsa.getFirstArc(state); arc != 0; arc = fsa.getNextArc(arc))
				countByValue[fsa.getArcLabel(arc) & 0xFF] ++;
			return true;
		});

		//order by descending frequency of counts and increasing label value
		final TreeSet<IntIntHolder> labelAndCount = new TreeSet<>(COMPARATOR);
		for(int label = 0; label < countByValue.length; label ++)
			if(countByValue[label] > 0)
				labelAndCount.add(new IntIntHolder(label, countByValue[label]));

		labelsIndex = new byte[1 + Math.min(labelAndCount.size(), CFSA2.LABEL_INDEX_SIZE)];
		invertedLabelsIndex = new int[256];
		for(int i = labelsIndex.length - 1; i > 0 && !labelAndCount.isEmpty(); --i){
			final IntIntHolder p = labelAndCount.pollFirst();
			invertedLabelsIndex[p.a] = i;
			labelsIndex[i] = (byte)p.a;
		}
	}

	/** Return supported flags. */
	@Override
	public Set<FSAFlags> getFlags(){
		return SUPPORTED_FLAGS;
	}

	/** Linearization of states. */
	private DynamicIntArray linearize(final FSA fsa) throws IOException{
		//states with most in-links (these should be placed as close to the start of the automaton as possible
		//so that v-coded addresses are tiny)
		final IntIntHashMap inLinkCount = computeInLinkCount(fsa);

		//ordered states for serialization
		final DynamicIntArray linearized = new DynamicIntArray();

		//determine which states should be linearized first (at fixed positions) so to minimize the place occupied by goto fields
		final int[] states = computeFirstStates(inLinkCount, Integer.MAX_VALUE, 2);

		//compute initial addresses, without node rearrangements
		final int serializedSize = linearizeAndCalculateOffsets(fsa, new DynamicIntArray(), linearized, offsets);

		//probe for better node arrangements by selecting between [lower, upper] nodes from the potential candidate nodes list
		final DynamicIntArray sublist = new DynamicIntArray();
		sublist.addAll(states);

		//probe the initial region a little, looking for optimal cut (it can't be binary search because the result isn't monotonic)
		LOGGER.trace("Compacting, initial output size: {}", serializedSize);
		int cutAt = 0;
		for(int cut = Math.min(25, states.length); cut <= Math.min(150, states.length); cut += 25){
			sublist.shrink(cut);

			final int newSize = linearizeAndCalculateOffsets(fsa, sublist, linearized, offsets);
			LOGGER.trace("Moved {} states, output size: {}", sublist.size(), newSize);
			if(newSize >= serializedSize)
				break;

			cutAt = cut;
		}

		//cut at the calculated point and repeat linearization
		sublist.shrink(cutAt);
		final int size = linearizeAndCalculateOffsets(fsa, sublist, linearized, offsets);
		LOGGER.trace("{} states moved, final size: {}", sublist.size(), size);

		return linearized;
	}

	/** Linearize all states, putting {@code states} in front of the automaton and calculating stable state offsets. */
	private int linearizeAndCalculateOffsets(final FSA fsa, final DynamicIntArray states, final DynamicIntArray linearized,
			final IntIntMap offsets) throws IOException{
		final Collection<Integer> visited = new HashSet<>();
		final DynamicIntArray nodes = new DynamicIntArray();
		linearized.clear();

		//linearize states with most in-links first
		for(int i = 0; i < states.size(); i ++)
			linearizeState(fsa, nodes, linearized, visited, states.get(i));

		//linearize the remaining states by chaining them one after another, in depth-order
		nodes.push(fsa.getRootNode());
		while(!nodes.isEmpty()){
			final int node = nodes.pop();
			if(visited.contains(node))
				continue;

			linearizeState(fsa, nodes, linearized, visited, node);
		}

		//calculate new state offsets. This is iterative. We start with maximum potential offsets and recalculate until converged
		final int maxOffset = Integer.MAX_VALUE;
		for(int i = 0; i < linearized.size(); i ++)
			offsets.put(linearized.get(i), maxOffset);

		int i;
		int j = 0;
		while((i = emitNodes(fsa, null, linearized)) > 0)
			j = i;
		return j;
	}

	/** Add a state to linearized list. */
	private void linearizeState(final FSA fsa, final DynamicIntArray nodes, final DynamicIntArray linearized, final Collection<Integer> visited,
			final int node){
		linearized.add(node);
		visited.add(node);
		for(int arc = fsa.getFirstArc(node); arc != 0; arc = fsa.getNextArc(arc))
			if(!fsa.isArcTerminal(arc)){
				final int target = fsa.getEndNode(arc);
				if(!visited.contains(target))
					nodes.push(target);
			}
	}

	/** Compute the set of states that should be linearized first to minimize other states goto length. */
	private int[] computeFirstStates(final Iterable<IntIntCursor> inLinkCount, final int maxStates, final int minInLinkCount){
		final PriorityQueue<IntIntHolder> stateInLink = new PriorityQueue<>(1, COMPARATOR);
		final IntIntHolder scratch = new IntIntHolder();
		for(final IntIntCursor c : inLinkCount)
			if(c.value > minInLinkCount){
				scratch.a = c.value;
				scratch.b = c.key;

				if(stateInLink.size() < maxStates || COMPARATOR.compare(scratch, stateInLink.peek()) > 0){
					stateInLink.add(new IntIntHolder(c.value, c.key));
					if(stateInLink.size() > maxStates)
						stateInLink.remove();
				}
			}

		final int[] states = new int[stateInLink.size()];
		for(int position = states.length; !stateInLink.isEmpty(); ){
			final IntIntHolder i = stateInLink.remove();
			states[-- position] = i.b;
		}
		return states;
	}

	/** Compute in-link count for each state. */
	private IntIntHashMap computeInLinkCount(final FSA fsa){
		final IntIntHashMap inLinkCount = new IntIntHashMap();
		final Collection<Integer> visited = new HashSet<>();
		final DynamicIntArray nodes = new DynamicIntArray();
		nodes.push(fsa.getRootNode());

		while(!nodes.isEmpty()){
			final int node = nodes.pop();
			if(visited.contains(node))
				continue;

			visited.add(node);

			for(int arc = fsa.getFirstArc(node); arc != 0; arc = fsa.getNextArc(arc))
				if(!fsa.isArcTerminal(arc)){
					final int target = fsa.getEndNode(arc);
					inLinkCount.putOrAdd(target, 1, 1);
					if(!visited.contains(target))
						nodes.push(target);
				}
		}

		return inLinkCount;
	}

	/** Update arc offsets assuming the given goto length. */
	private int emitNodes(final FSA fsa, final OutputStream os, final DynamicIntArray linearized) throws IOException{
		int offset = 0;

		//add epsilon state
		offset += emitNodeData(os, 0);
		final int targetOffset = (fsa.getRootNode() != 0? offsets.get(fsa.getRootNode()): 0);
		offset += emitArc(os, CFSA2.BIT_LAST_ARC, (byte)'^', targetOffset);

		boolean offsetsChanged = false;
		final int max = linearized.size();
		for(int index = 0, state = linearized.get(index); index < max; index ++){
			final int nextState = (index + 1 < max? linearized.get(index + 1): NO_STATE);

			if(os == null){
				offsetsChanged |= (offsets.get(state) != offset);
				offsets.put(state, offset);
			}
			else if(offsets.get(state) != offset)
				throw new IllegalArgumentException("Error on offsets: state " + state
					+ ", offset of state " + offsets.get(state) + ", offset " + offset);

			offset += emitNodeData(os, serializeWithNumbers? numbers.get(state): 0);
			offset += emitNodeArcs(fsa, os, state, nextState);

			state = nextState;
		}

		return (offsetsChanged? offset: 0);
	}

	/** Emit all arcs of a single node. */
	private int emitNodeArcs(final FSA fsa, final OutputStream os, final int state, final int nextState) throws IOException{
		int offset = 0;
		for(int arc = fsa.getFirstArc(state); arc != 0; arc = fsa.getNextArc(arc)){
			final boolean arcTerminal = fsa.isArcTerminal(arc);
			final int target = (arcTerminal? 0: fsa.getEndNode(arc));
			int targetOffset = (arcTerminal? 0: offsets.get(target));

			int flags = 0;
			if(fsa.isArcFinal(arc))
				flags |= CFSA2.BIT_FINAL_ARC;
			if(fsa.getNextArc(arc) == 0)
				flags |= CFSA2.BIT_LAST_ARC;
			if(targetOffset != 0 && target == nextState){
				flags |= CFSA2.BIT_TARGET_NEXT;
				targetOffset = 0;
			}

			offset += emitArc(os, flags, fsa.getArcLabel(arc), targetOffset);
		}

		return offset;
	}

	private int emitArc(final OutputStream os, final int flags, final byte label, final int targetOffset) throws IOException{
		int length = 0;

		final int labelIndex = invertedLabelsIndex[label & 0xFF];
		if(labelIndex > 0){
			if(os != null)
				os.write(flags | labelIndex);
			length ++;
		}
		else{
			if(os != null){
				os.write(flags);
				os.write(label);
			}
			length += 2;
		}

		if((flags & CFSA2.BIT_TARGET_NEXT) == 0){
			final byte[] scratch = new byte[5];
			final int len = writeVInt(scratch, targetOffset);
			if(os != null)
				os.write(scratch, 0, len);
			length += len;
		}

		return length;
	}

	private int emitNodeData(final OutputStream os, final int number) throws IOException{
		int size = 0;
		if(serializeWithNumbers){
			final byte[] scratch = new byte[5];
			size = writeVInt(scratch, number);
			if(os != null)
				os.write(scratch, 0, size);
		}
		return size;
	}

	@Override
	public CFSA2Serializer withFiller(final byte filler){
		throw new UnsupportedOperationException("CFSA2 does not support filler. Use .info file.");
	}

	@Override
	public CFSA2Serializer withAnnotationSeparator(final byte annotationSeparator){
		throw new UnsupportedOperationException("CFSA2 does not support separator. Use .info file.");
	}

	/** Write a v-int to a byte array. */
	private int writeVInt(final byte[] array, int value){
		if(value < 0)
			throw new IllegalArgumentException("V-code can't be negative: " + value);

		int offset = 0;
		while(value > 0x7F){
			array[offset ++] = (byte)(0x80 | value & 0x7F);
			value >>= 7;
		}
		array[offset ++] = (byte)value;

		return offset;
	}

}
