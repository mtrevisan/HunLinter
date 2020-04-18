package unit731.hunlinter.datastructures.fsa.builders;

import unit731.hunlinter.datastructures.fsa.FSA;

import java.util.Arrays;


/**
 * Fast, memory-conservative, Finite State Automaton builder, returning an in-memory {@link FSA} that is a trade-off
 * between construction speed and memory consumption.
 *
 * @see <a href="https://www.aclweb.org/anthology/J00-1002.pdf">Incremental Construction of Minimal Acyclic Finite-State Automata</a>
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class FSABuilder{

	/** A megabyte */
	private static final int MB = 1024 * 1024;
	/** Internal serialized FSA buffer expand ratio */
	private static final int BUFFER_GROWTH_SIZE = 5 * MB;
	/** Maximum number of labels from a single state */
	private static final int MAX_LABELS = 256;


	/** Internal serialized FSA buffer expand ratio */
	private final int bufferGrowthSize;
	/**
	 * Holds serialized and mutable states.
	 * Each state is a sequential list of arcs, the last arc is marked with {@link ConstantArcSizeFSA#BIT_ARC_LAST}.
	 */
	private byte[] serialized = new byte[0];
	/**
	 * Number of bytes already taken in {@link #serialized}.
	 * Start from 1 to keep 0 a sentinel value (for the hash set and final state).
	 */
	private int size;

	/**
	 * States on the "active path" (still mutable).
	 * Values are addresses of each state's first arc.
	 */
	private int[] activePath = new int[0];
	/** Current length of the active path */
	private int activePathLen;

	/** The next offset at which an arc will be added to the given state on {@link #activePath} */
	private int[] nextArcOffset = new int[0];
	/** Root state (if negative, the automaton has been built already and cannot be extended) */
	private int root;
	/**
	 * An epsilon state.
	 * The first and only arc of this state points either to the
	 * root or to the terminal state, indicating an empty automaton.
	 */
	private final int epsilon;

	/**
	 * Hash set of state addresses in {@link #serialized}, hashed by {@link #hash(int, int)}.
	 * Zero reserved for an unoccupied slot.
	 */
	private int[] hashSet = new int[2];
	/** Number of entries currently stored in {@link #hashSet} */
	private int hashSize;


	public FSABuilder(){
		this(BUFFER_GROWTH_SIZE);
	}

	/**
	 * @param bufferGrowthSize Buffer growth size (in bytes) when constructing the automaton.
	 */
	public FSABuilder(final int bufferGrowthSize){
		this.bufferGrowthSize = Math.max(bufferGrowthSize, ConstantArcSizeFSA.ARC_SIZE * MAX_LABELS);

		//allocate epsilon state
		epsilon = allocateState(1);
		serialized[epsilon + ConstantArcSizeFSA.FLAGS_OFFSET] |= ConstantArcSizeFSA.BIT_ARC_LAST;

		//allocate root, with an initial empty set of output arcs
		expandActivePath(1);
		root = activePath[0];
	}

	/**
	 * Build a minimal, deterministic automaton from an iterable list of byte sequences.
	 * NOTE: The input MUST BE lexicographically greater than any previously added sequence!
	 *
	 * @param input	Input sequences to build automaton from.
	 * @return	The automaton encoding of all input sequences.
	 */
	public FSA build(final Iterable<byte[]> input){
		for(final byte[] chs : input)
			add(chs);
		return complete();
	}

	public FSA build(final byte[][] input){
		for(final byte[] chs : input)
			add(chs);
		return complete();
	}

	/**
	 * Add a single sequence of bytes to the FSA.
	 * NOTE: The input MUST BE lexicographically greater than any previously added sequence!
	 *
	 * @param sequence The array holding input sequence of bytes.
	 */
	public final void add(final byte[] sequence){
		if(serialized == null)
			throw new IllegalArgumentException("Automaton already built");
		final int len = sequence.length;

		//determine common prefix length
		final int commonPrefix = commonPrefix(sequence, len);

		//make room for extra states on active path, if needed
		expandActivePath(len);

		//freeze all the states after the common prefix
		for(int i = activePathLen - 1; i > commonPrefix; i --){
			final int frozenState = freezeState(i);
			setArcTarget(nextArcOffset[i - 1] - ConstantArcSizeFSA.ARC_SIZE, frozenState);
			nextArcOffset[i] = activePath[i];
		}

		//create arcs to new suffix states
		final int start = 0;
		for(int i = commonPrefix + 1, j = start + commonPrefix; i <= len; i ++){
			final int p = nextArcOffset[i - 1];

			serialized[p + ConstantArcSizeFSA.FLAGS_OFFSET] = (byte)(i == len? ConstantArcSizeFSA.BIT_ARC_FINAL: 0);
			serialized[p + ConstantArcSizeFSA.LABEL_OFFSET] = sequence[j ++];
			setArcTarget(p, i == len? ConstantArcSizeFSA.TERMINAL_STATE: activePath[i]);

			nextArcOffset[i - 1] = p + ConstantArcSizeFSA.ARC_SIZE;
		}

		//save last sequence's length so that we don't need to calculate it again
		activePathLen = len;
	}

	/**
	 * Finalizes the construction of the automaton and returns it
	 *
	 * @return	The {@link FSA} just constructed
	 */
	public final FSA complete(){
		add(new byte[0]);

		if(nextArcOffset[0] - activePath[0] == 0)
			//an empty FSA
			setArcTarget(epsilon, ConstantArcSizeFSA.TERMINAL_STATE);
		else{
			//an automaton with at least a single arc from root
			root = freezeState(0);
			setArcTarget(epsilon, root);
		}

		final FSA fsa = new ConstantArcSizeFSA(Arrays.copyOf(serialized, size), epsilon);

		//clear support data:
		serialized = null;
		hashSet = null;

		return fsa;
	}

	private boolean isArcLast(final int arc){
		return ((serialized[arc + ConstantArcSizeFSA.FLAGS_OFFSET] & ConstantArcSizeFSA.BIT_ARC_LAST) != 0);
	}

	private boolean isArcFinal(final int arc){
		return ((serialized[arc + ConstantArcSizeFSA.FLAGS_OFFSET] & ConstantArcSizeFSA.BIT_ARC_FINAL) != 0);
	}

	private byte getArcLabel(final int arc){
		return serialized[arc + ConstantArcSizeFSA.LABEL_OFFSET];
	}

	/** Fills the target state address of an arc */
	private void setArcTarget(int arc, int state){
		arc += ConstantArcSizeFSA.ADDRESS_OFFSET + ConstantArcSizeFSA.TARGET_ADDRESS_SIZE;
		for(int i = 0; i < ConstantArcSizeFSA.TARGET_ADDRESS_SIZE; i ++){
			serialized[-- arc] = (byte)state;
			state >>>= 8;
		}
	}

	/** Returns the address of an arc */
	private int getArcTarget(int arc){
		arc += ConstantArcSizeFSA.ADDRESS_OFFSET;
		return (serialized[arc]) << 24
			| (serialized[arc + 1] & 0xFF) << 16
			| (serialized[arc + 2] & 0xFF) << 8
			| (serialized[arc + 3] & 0xFF);
	}

	/**
	 * @return The number of common prefix characters with the previous sequence.
	 */
	private int commonPrefix(final byte[] sequence, final int len){
		//empty root state case
		final int max = Math.min(len, activePathLen);
		int index;
		int start = 0;
		for(index = 0; index < max; index ++){
			final int lastArc = nextArcOffset[index] - ConstantArcSizeFSA.ARC_SIZE;
			if(sequence[start ++] != getArcLabel(lastArc))
				break;
		}
		return index;
	}

	/**
	 * Freeze a state: try to find an equivalent state in the interned states
	 * dictionary first, if found, return it, otherwise, serialize the mutable
	 * state at <code>activePathIndex</code> and return it.
	 */
	private int freezeState(final int activePathIndex){
		final int start = activePath[activePathIndex];
		final int end = nextArcOffset[activePathIndex];
		final int length = end - start;

		//set the last arc flag on the current active path's state
		serialized[end - ConstantArcSizeFSA.ARC_SIZE + ConstantArcSizeFSA.FLAGS_OFFSET] |= ConstantArcSizeFSA.BIT_ARC_LAST;

		//try to locate a state with an identical content in the hash set
		final int bucketMask = (hashSet.length - 1);
		int slot = hash(start, length) & bucketMask;
		for(int i = 0; ; ){
			int state = hashSet[slot];
			if(state == 0){
				state = hashSet[slot] = serialize(activePathIndex);
				if(++ hashSize > hashSet.length / 2)
					expandAndRehash();

				return state;
			}
			else if(equivalent(state, start, length))
				return state;

			slot = (slot + (++ i)) & bucketMask;
		}
	}

	/** Reallocate and rehash the hash set */
	private void expandAndRehash(){
		final int[] newHashSet = new int[hashSet.length * 2];
		final int bucketMask = (newHashSet.length - 1);
		for(final int state : hashSet)
			if(state > 0){
				int slot = hash(state, stateLength(state)) & bucketMask;
				for(int i = 0; newHashSet[slot] > 0; )
					slot = (slot + (++ i)) & bucketMask;
				newHashSet[slot] = state;
			}
		hashSet = newHashSet;
	}

	/** The total length of the serialized state data (all arcs) */
	private int stateLength(final int state){
		int arc = state;
		while(!isArcLast(arc))
			arc += ConstantArcSizeFSA.ARC_SIZE;
		return arc - state + ConstantArcSizeFSA.ARC_SIZE;
	}

	/** Return <code>true</code> if two regions in {@link #serialized} are identical */
	private boolean equivalent(int start1, int start2, int len){
		if(Math.max(start1, start2) + len > size)
			return false;

		while(len -- > 0)
			if(serialized[start1 ++] != serialized[start2 ++])
				return false;
		return true;
	}

	/** Serialize a given state on the active path */
	private int serialize(final int activePathIndex){
		expandBuffers();

		final int newState = size;
		final int start = activePath[activePathIndex];
		final int len = nextArcOffset[activePathIndex] - start;
		System.arraycopy(serialized, start, serialized, newState, len);

		size += len;
		return newState;
	}

	/** Hash code of a fragment of {@link #serialized} array */
	private int hash(int start, final int byteCount){
		if(byteCount % ConstantArcSizeFSA.ARC_SIZE != 0)
			throw new IllegalArgumentException("Not an arc multiply: " + byteCount + " mod " + ConstantArcSizeFSA.ARC_SIZE);

		int h = 0;
		for(int arcs = byteCount / ConstantArcSizeFSA.ARC_SIZE; -- arcs >= 0; start += ConstantArcSizeFSA.ARC_SIZE){
			h = 17 * h + getArcLabel(start);
			h = 17 * h + getArcTarget(start);
			if(isArcFinal(start))
				h += 17;
		}
		return h;
	}

	/** Append a new mutable state to the active path */
	private void expandActivePath(final int size){
		if(activePath.length < size){
			final int p = activePath.length;
			activePath = Arrays.copyOf(activePath, size);
			nextArcOffset = Arrays.copyOf(nextArcOffset, size);

			for(int i = p; i < size; i ++)
				//assume max labels count
				nextArcOffset[i] = activePath[i] = allocateState(MAX_LABELS);
		}
	}

	/**
	 * Allocate space for a state with the given number of outgoing labels.
	 *
	 * @return state offset
	 */
	private int allocateState(final int labels){
		expandBuffers();

		final int state = size;
		size += labels * ConstantArcSizeFSA.ARC_SIZE;
		return state;
	}

	/** Expand internal buffers for the next state */
	private void expandBuffers(){
		if(serialized.length < size + ConstantArcSizeFSA.ARC_SIZE * MAX_LABELS)
			serialized = Arrays.copyOf(serialized, serialized.length + bufferGrowthSize);
	}

}
