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
package io.github.mtrevisan.hunlinter.datastructures.fsa.builders;

import io.github.mtrevisan.hunlinter.datastructures.fsa.FSAAbstract;

import java.util.Collections;
import java.util.Set;


/**
 * An FSA with constant-size arc representation produced directly by {@link FSABuilder}.
 *
 * @see FSABuilder
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class ConstantArcSizeFSA extends FSAAbstract{

	/** Size of the target address field (constant for the builder). */
	public static final int TARGET_ADDRESS_SIZE = 4;
	/** Size of the flags field (constant for the builder). */
	private static final int FLAGS_SIZE = 1;
	/** Size of the label field (constant for the builder). */
	private static final int LABEL_SIZE = 1;
	/** Size of a single arc structure. */
	public static final int ARC_SIZE = FLAGS_SIZE + LABEL_SIZE + TARGET_ADDRESS_SIZE;
	/** Offset of the flags field inside an arc. */
	public static final int FLAGS_OFFSET = 0;
	/** Offset of the label field inside an arc. */
	public static final int LABEL_OFFSET = FLAGS_SIZE;
	/** Offset of the address field inside an arc. */
	public static final int ADDRESS_OFFSET = LABEL_OFFSET + LABEL_SIZE;

	/** A dummy address of the terminal state. */
	static final int TERMINAL_STATE = 0;

	/** An arc flag indicating the arc is last within its state. */
	public static final int BIT_ARC_LAST = 0x01;
	/** An arc flag indicating the target node of an arc corresponds to a final state. */
	public static final int BIT_ARC_FINAL = 0x02;

	/**
	 * An epsilon state. The first and only arc of this state points either to the
	 * root or to the terminal state, indicating an empty automaton.
	 */
	private final int epsilon;
	/** FSA data, serialized as a byte array. */
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
	public final int getRootNode(){
		return getEndNode(getFirstArc(epsilon));
	}

	@Override
	public final int getFirstArc(final int node){
		return node;
	}

	@Override
	public final int getNextArc(final int arc){
		return (isArcLast(arc)? 0: skipArc(arc));
	}

	@Override
	public final byte getArcLabel(final int arc){
		return data[arc + LABEL_OFFSET];
	}

	/** Returns the address of an arc. */
	private int getArcTarget(int arc){
		arc += ADDRESS_OFFSET;
		return (data[arc]) << 24
			| (data[arc + 1] & 0xFF) << 16
			| (data[arc + 2] & 0xFF) << 8
			| (data[arc + 3] & 0xFF);
	}

	@Override
	public final boolean isArcFinal(final int arc){
		return ((data[arc + FLAGS_OFFSET] & BIT_ARC_FINAL) != 0);
	}

	@Override
	public final boolean isArcTerminal(final int arc){
		return (getArcTarget(arc) == 0);
	}

	private boolean isArcLast(final int arc){
		return (data[arc + FLAGS_OFFSET] & BIT_ARC_LAST) != 0;
	}

	@Override
	public final int getEndNode(final int arc){
		return getArcTarget(arc);
	}

	@Override
	public final Set<FSAFlags> getFlags(){
		return Collections.emptySet();
	}

	private int skipArc(final int offset){
		return offset + ARC_SIZE;
	}

}
