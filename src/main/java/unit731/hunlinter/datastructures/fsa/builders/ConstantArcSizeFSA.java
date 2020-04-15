package unit731.hunlinter.datastructures.fsa.builders;

import unit731.hunlinter.datastructures.fsa.FSA;

import java.util.Collections;
import java.util.Set;


/**
 * An FSA with constant-size arc representation produced directly by {@link FSABuilder}.
 *
 * @see FSABuilder
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class ConstantArcSizeFSA extends FSA{

	/** Size of the target address field (constant for the builder) */
	public final static int TARGET_ADDRESS_SIZE = 4;
	/** Size of the flags field (constant for the builder) */
	public final static int FLAGS_SIZE = 1;
	/** Size of the label field (constant for the builder) */
	public final static int LABEL_SIZE = 1;
	/** Size of a single arc structure */
	public final static int ARC_SIZE = FLAGS_SIZE + LABEL_SIZE + TARGET_ADDRESS_SIZE;
	/** Offset of the flags field inside an arc */
	public final static int FLAGS_OFFSET = 0;
	/** Offset of the label field inside an arc */
	public final static int LABEL_OFFSET = FLAGS_SIZE;
	/** Offset of the address field inside an arc */
	public final static int ADDRESS_OFFSET = LABEL_OFFSET + LABEL_SIZE;

	/** A dummy address of the terminal state */
	final static int TERMINAL_STATE = 0;

	/** An arc flag indicating the arc is last within its state */
	public final static int BIT_ARC_LAST = 0x01;
	/** An arc flag indicating the target node of an arc corresponds to a final state */
	public final static int BIT_ARC_FINAL = 0x02;

	/**
	 * An epsilon state. The first and only arc of this state points either to the
	 * root or to the terminal state, indicating an empty automaton.
	 */
	private final int epsilon;
	/** FSA data, serialized as a byte array */
	private final byte[] data;


	/**
	 * @param data FSA data. There must be no trailing bytes after the last state.
	 */
	ConstantArcSizeFSA(final byte[] data, final int epsilon){
		if(epsilon != 0)
			throw new IllegalArgumentException("Epsilon is not zero: " + epsilon);

		this.epsilon = epsilon;
		this.data = data;
	}

	@Override
	public int getRootNode(){
		return getEndNode(getFirstArc(epsilon));
	}

	@Override
	public int getFirstArc(final int node){
		return node;
	}

	@Override
	public int getArc(final int node, final byte label){
		for(int arc = getFirstArc(node); arc != 0; arc = getNextArc(arc))
			if(getArcLabel(arc) == label)
				return arc;
		return 0;
	}

	@Override
	public int getNextArc(final int arc){
		return (isArcLast(arc)? 0: arc + ARC_SIZE);
	}

	@Override
	public byte getArcLabel(final int arc){
		return data[arc + LABEL_OFFSET];
	}

	/** Fills the target state address of an arc */
	private int getArcTarget(int arc){
		arc += ADDRESS_OFFSET;
		return (data[arc]) << 24
			| (data[arc + 1] & 0xFF) << 16
			| (data[arc + 2] & 0xFF) << 8
			| (data[arc + 3] & 0xFF);
	}

	@Override
	public boolean isArcFinal(final int arc){
		return ((data[arc + FLAGS_OFFSET] & BIT_ARC_FINAL) != 0);
	}

	@Override
	public boolean isArcTerminal(final int arc){
		return (getArcTarget(arc) == 0);
	}

	private boolean isArcLast(final int arc){
		return (data[arc + FLAGS_OFFSET] & BIT_ARC_LAST) != 0;
	}

	@Override
	public int getEndNode(final int arc){
		return getArcTarget(arc);
	}

	@Override
	public Set<FSAFlags> getFlags(){
		return Collections.emptySet();
	}

}