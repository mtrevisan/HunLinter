package unit731.hunlinter.services.fsa.serializers;

import com.carrotsearch.hppc.BoundedProportionalArraySizingStrategy;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntStack;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.cursors.IntIntCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.services.fsa.CFSA2;
import unit731.hunlinter.services.fsa.FSA;
import unit731.hunlinter.services.fsa.FSAFlags;
import unit731.hunlinter.services.fsa.FSAHeader;

import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;


/**
 * Serializes in-memory {@link FSA} graphs to {@link CFSA2 Compact Finite State Automata, version 2}.
 *
 * <p>
 * It is possible to serialize the automaton with numbers required for perfect
 * hashing. See {@link #serializeWithNumbers()} method.
 * </p>
 *
 * @see CFSA2
 */
public class CFSA2Serializer implements FSASerializer{

	private static final Logger LOGGER = LoggerFactory.getLogger(CFSA2Serializer.class);

	/** Supported flags */
	private final static EnumSet<FSAFlags> SUPPORTED_FLAGS = EnumSet.of(FSAFlags.NUMBERS, FSAFlags.FLEXIBLE, FSAFlags.STOPBIT,
		FSAFlags.NEXTBIT);

	/** No-state id */
	private final static int NO_STATE = -1;


	/**
	 * <code>true</code> if we should serialize with numbers.
	 *
	 * @see #serializeWithNumbers()
	 */
	private boolean serializeWithNumbers;

	/** A hash map of [state, offset] pairs */
	private final IntIntHashMap offsets = new IntIntHashMap();
	/** A hash map of [state, right-language-count] pairs */
	private IntIntHashMap numbers = new IntIntHashMap();
	/** Scratch array for serializing vints */
	private final byte[] scratch = new byte[5];
	/** The most frequent labels for integrating with the flags field */
	private byte[] labelsIndex;
	/** Inverted index of labels to be integrated with flags field. A label at index <code>i<code> has the index or zero (no integration) */
	private int[] invertedLabelsIndex;


	/**
	 * Serialize the automaton with the number of right-language sequences in each
	 * node. This is required to implement perfect hashing. The numbering also
	 * preserves the order of input sequences.
	 *
	 * @return Returns the same object for easier call chaining.
	 */
	public CFSA2Serializer serializeWithNumbers(){
		serializeWithNumbers = true;
		return this;
	}

	/**
	 * Serializes any {@link FSA} to {@link CFSA2} stream.
	 *
	 * @return Returns <code>os</code> for chaining.
	 * @see #serializeWithNumbers()
	 */
	@Override
	public <T extends OutputStream> T serialize(final FSA fsa, final T os) throws IOException{
		//calculate the most frequent labels and build indexed labels dictionary
		computeLabelsIndex(fsa);

		//calculate the number of bytes required for the node data, if serializing with numbers
		if(serializeWithNumbers)
			numbers = FSAUtils.rightLanguageForAllStates(fsa);

		//linearize all the states, optimizing their layout
		final IntArrayList linearized = linearize(fsa);

		//emit the header
		FSAHeader.write(os, CFSA2.VERSION);

		final EnumSet<FSAFlags> fsaFlags = EnumSet.of(FSAFlags.FLEXIBLE, FSAFlags.STOPBIT, FSAFlags.NEXTBIT);
		if(serializeWithNumbers)
			fsaFlags.add(FSAFlags.NUMBERS);

		final short sflags = FSAFlags.asShort(fsaFlags);
		os.write((sflags >> 8) & 0xFF);
		os.write((sflags) & 0xFF);

		//emit labels index
		os.write(labelsIndex.length);
		os.write(labelsIndex);

		//emit the automaton
		int size = emitNodes(fsa, os, linearized);
		assert size == 0: "Size changed in the final pass?";

		return os;
	}

	/** Compute a set of labels to be integrated with the flags field */
	private void computeLabelsIndex(final FSA fsa){
		//compute labels count
		final int[] countByValue = new int[256];

		fsa.visitAllStates(state -> {
			for(int arc = fsa.getFirstArc(state); arc != 0; arc = fsa.getNextArc(arc))
				countByValue[fsa.getArcLabel(arc) & 0xFF] ++;
			return true;
		});

		//order by descending frequency of counts and increasing label value
		Comparator<FSAUtils.IntIntHolder> comparator = (o1, o2) -> {
			final int countDiff = o2.b - o1.b;
			return (countDiff == 0? o1.a - o2.a: countDiff);
		};

		final TreeSet<FSAUtils.IntIntHolder> labelAndCount = new TreeSet<>(comparator);
		for(int label = 0; label < countByValue.length; label ++)
			if(countByValue[label] > 0)
				labelAndCount.add(new FSAUtils.IntIntHolder(label, countByValue[label]));

		labelsIndex = new byte[1 + Math.min(labelAndCount.size(), CFSA2.LABEL_INDEX_SIZE)];
		invertedLabelsIndex = new int[256];
		for(int i = labelsIndex.length - 1; i > 0 && !labelAndCount.isEmpty(); i --){
			FSAUtils.IntIntHolder p = labelAndCount.first();
			labelAndCount.remove(p);
			invertedLabelsIndex[p.a] = i;
			labelsIndex[i] = (byte) p.a;
		}
	}

	/** Return supported flags */
	@Override
	public Set<FSAFlags> getFlags(){
		return SUPPORTED_FLAGS;
	}

	/** Linearization of states */
	private IntArrayList linearize(final FSA fsa) throws IOException{
		/**
		 * Compute the states with most inlinks. These should be placed as close to the
		 * start of the automaton, as possible so that v-coded addresses are tiny.
		 */
		final IntIntHashMap inlinkCount = computeInlinkCount(fsa);

		/** An array of ordered states for serialization. */
		final IntArrayList linearized = new IntArrayList(0, new BoundedProportionalArraySizingStrategy(1000, 10000, 1.5f));

		/**
		 * Determine which states should be linearized first (at fixed positions) so as to
		 * minimize the place occupied by goto fields.
		 */
		int maxStates = Integer.MAX_VALUE;
		int minInlinkCount = 2;
		int[] states = computeFirstStates(inlinkCount, maxStates, minInlinkCount);

		/** Compute initial addresses, without node rearrangements */
		int serializedSize = linearizeAndCalculateOffsets(fsa, new IntArrayList(), linearized, offsets);

		/** Probe for better node arrangements by selecting between [lower, upper] nodes from the potential candidate nodes list */
		IntArrayList sublist = new IntArrayList();
		sublist.buffer = states;
		sublist.elementsCount = states.length;

		/** Probe the initial region a little bit, looking for optimal cut. It can't be binary search because the result isn't monotonic */
		LOGGER.trace("Compacting, initial output size: {}", serializedSize);
		int cutAt = 0;
		for(int cut = Math.min(25, states.length); cut <= Math.min(150, states.length); cut += 25){
			sublist.elementsCount = cut;
			final int newSize = linearizeAndCalculateOffsets(fsa, sublist, linearized, offsets);
			LOGGER.trace("Moved {} states, output size: {}", sublist.size(), newSize);
			if(newSize >= serializedSize)
				break;

			cutAt = cut;
		}

		//cut at the calculated point and repeat linearization
		sublist.elementsCount = cutAt;
		final int size = linearizeAndCalculateOffsets(fsa, sublist, linearized, offsets);
		LOGGER.trace("{} states moved, final size: {}", sublist.size(), size);
		return linearized;
	}

	/**
	 * Linearize all states, putting <code>states</code> in front of the automaton
	 * and calculating stable state offsets.
	 */
	private int linearizeAndCalculateOffsets(final FSA fsa, final IntArrayList states, final IntArrayList linearized, final IntIntHashMap offsets) throws IOException{
		final BitSet visited = new BitSet();
		final IntStack nodes = new IntStack();
		linearized.clear();

		//linearize states with most inlinks first
		for(int i = 0; i < states.size(); i ++)
			linearizeState(fsa, nodes, linearized, visited, states.get(i));

		//linearize the remaining states by chaining them one after another, in depth-order
		nodes.push(fsa.getRootNode());
		while(!nodes.isEmpty()){
			final int node = nodes.pop();
			if(visited.get(node))
				continue;

			linearizeState(fsa, nodes, linearized, visited, node);
		}

		//calculate new state offsets. This is iterative. We start with maximum potential offsets and recalculate until converged
		final int MAX_OFFSET = Integer.MAX_VALUE;
		for(final IntCursor c : linearized)
			offsets.put(c.value, MAX_OFFSET);

		int i, j = 0;
		while((i = emitNodes(fsa, null, linearized)) > 0)
			j = i;
		return j;
	}

	/** Add a state to linearized list */
	private void linearizeState(final FSA fsa, final IntStack nodes, final IntArrayList linearized, final BitSet visited, final int node){
		linearized.add(node);
		visited.set(node);
		for(int arc = fsa.getFirstArc(node); arc != 0; arc = fsa.getNextArc(arc))
			if(!fsa.isArcTerminal(arc)){
				final int target = fsa.getEndNode(arc);
				if(!visited.get(target))
					nodes.push(target);
			}
	}

	/** Compute the set of states that should be linearized first to minimize other states goto length */
	private int[] computeFirstStates(final IntIntHashMap inlinkCount, final int maxStates, final int minInlinkCount){
		final Comparator<FSAUtils.IntIntHolder> comparator = (o1, o2) -> {
			final int v = o1.a - o2.a;
			return (v == 0? o1.b - o2.b: v);
		};

		final PriorityQueue<FSAUtils.IntIntHolder> stateInlink = new PriorityQueue<>(1, comparator);
		final FSAUtils.IntIntHolder scratch = new FSAUtils.IntIntHolder();
		for(final IntIntCursor c : inlinkCount)
			if(c.value > minInlinkCount){
				scratch.a = c.value;
				scratch.b = c.key;

				if(stateInlink.size() < maxStates || comparator.compare(scratch, stateInlink.peek()) > 0){
					stateInlink.add(new FSAUtils.IntIntHolder(c.value, c.key));
					if(stateInlink.size() > maxStates)
						stateInlink.remove();
				}
			}

		final int[] states = new int[stateInlink.size()];
		for(int position = states.length; !stateInlink.isEmpty(); ){
			final FSAUtils.IntIntHolder i = stateInlink.remove();
			states[--position] = i.b;
		}

		return states;
	}

	/** Compute in-link count for each state */
	private IntIntHashMap computeInlinkCount(final FSA fsa){
		final IntIntHashMap inlinkCount = new IntIntHashMap();
		final BitSet visited = new BitSet();
		final IntStack nodes = new IntStack();
		nodes.push(fsa.getRootNode());

		while(!nodes.isEmpty()){
			final int node = nodes.pop();
			if(visited.get(node))
				continue;

			visited.set(node);

			for(int arc = fsa.getFirstArc(node); arc != 0; arc = fsa.getNextArc(arc))
				if(!fsa.isArcTerminal(arc)){
					final int target = fsa.getEndNode(arc);
					inlinkCount.putOrAdd(target, 1, 1);
					if(!visited.get(target))
						nodes.push(target);
				}
		}

		return inlinkCount;
	}

	/** Update arc offsets assuming the given goto length */
	private int emitNodes(final FSA fsa, final OutputStream os, final IntArrayList linearized) throws IOException{
		int offset = 0;

		//add epsilon state
		offset += emitNodeData(os, 0);
		if(fsa.getRootNode() != 0)
			offset += emitArc(os, CFSA2.BIT_LAST_ARC, (byte)'^', offsets.get(fsa.getRootNode()));
		else
			offset += emitArc(os, CFSA2.BIT_LAST_ARC, (byte)'^', 0);

		boolean offsetsChanged = false;
		final int max = linearized.size();
		for(final IntCursor c : linearized){
			final int state = c.value;
			final int nextState = (c.index + 1 < max? linearized.get(c.index + 1): NO_STATE);

			if(os == null){
				offsetsChanged |= (offsets.get(state) != offset);
				offsets.put(state, offset);
			}
			else
				assert offsets.get(state) == offset: state + " " + offsets.get(state) + " " + offset;

			offset += emitNodeData(os, serializeWithNumbers? numbers.get(state): 0);
			offset += emitNodeArcs(fsa, os, state, nextState);
		}

		return offsetsChanged? offset: 0;
	}

	/** Emit all arcs of a single node */
	private int emitNodeArcs(final FSA fsa, final OutputStream os, final int state, final int nextState) throws IOException{
		int offset = 0;
		for(int arc = fsa.getFirstArc(state); arc != 0; arc = fsa.getNextArc(arc)){
			int targetOffset;
			final int target;

			if(fsa.isArcTerminal(arc)){
				target = 0;
				targetOffset = 0;
			}
			else{
				target = fsa.getEndNode(arc);
				targetOffset = offsets.get(target);
			}

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

		int labelIndex = invertedLabelsIndex[label & 0xFF];
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
			int len = writeVInt(scratch, 0, targetOffset);
			if(os != null)
				os.write(scratch, 0, len);
			length += len;
		}

		return length;
	}

	private int emitNodeData(final OutputStream os, final int number) throws IOException{
		int size = 0;
		if(serializeWithNumbers){
			size = writeVInt(scratch, 0, number);
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

	/** Write a v-int to a byte array */
	static int writeVInt(final byte[] array, int offset, int value){
		assert value >= 0: "Can't v-code negative ints.";

		while(value > 0x7F){
			array[offset ++] = (byte) (0x80 | (value & 0x7F));
			value >>= 7;
		}
		array[offset ++] = (byte) value;

		return offset;
	}

}
