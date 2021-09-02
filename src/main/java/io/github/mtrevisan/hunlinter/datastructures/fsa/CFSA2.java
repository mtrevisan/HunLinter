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
package io.github.mtrevisan.hunlinter.datastructures.fsa;

import io.github.mtrevisan.hunlinter.datastructures.fsa.builders.FSAFlags;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Set;


/**
 * CFSA (Compact Finite State Automaton) binary format implementation, version 2:
 * <ul>
 *  <li>{@link #BIT_TARGET_NEXT} applicable on all arcs, not necessarily the last one.</li>
 *  <li>v-coded goto field</li>
 *  <li>v-coded perfect hashing numbers, if any</li>
 *  <li>31 most frequent labels integrated with flags byte</li>
 * </ul>
 *
 * <p>The encoding of automaton body is as follows.</p>
 *
 * <pre>
 * ---- CFSA header
 * Byte                            Description
 *       +-+-+-+-+-+-+-+-+\
 *     0 | | | | | | | | | +------ '\'
 *       +-+-+-+-+-+-+-+-+/
 *       +-+-+-+-+-+-+-+-+\
 *     1 | | | | | | | | | +------ 'f'
 *       +-+-+-+-+-+-+-+-+/
 *       +-+-+-+-+-+-+-+-+\
 *     2 | | | | | | | | | +------ 's'
 *       +-+-+-+-+-+-+-+-+/
 *       +-+-+-+-+-+-+-+-+\
 *     3 | | | | | | | | | +------ 'a'
 *       +-+-+-+-+-+-+-+-+/
 *       +-+-+-+-+-+-+-+-+\
 *     4 | | | | | | | | | +------ version (fixed 0xC6)
 *       +-+-+-+-+-+-+-+-+/
 *       +-+-+-+-+-+-+-+-+\
 *     5 | | | | | | | | | +----\
 *       +-+-+-+-+-+-+-+-+/      \ flags [MSB first]
 *       +-+-+-+-+-+-+-+-+\      /
 *     6 | | | | | | | | | +----/
 *       +-+-+-+-+-+-+-+-+/
 *       +-+-+-+-+-+-+-+-+\
 *     7 | | | | | | | | | +------ label lookup table size
 *       +-+-+-+-+-+-+-+-+/
 *       +-+-+-+-+-+-+-+-+\
 *  8-32 | | | | | | | | | +------ label value lookup table
 *       : : : : : : : : : |
 *       +-+-+-+-+-+-+-+-+/
 *
 * ---- Start of a node; only if automaton was compiled with NUMBERS option.
 *
 * Byte
 *        +-+-+-+-+-+-+-+-+\
 *      0 | | | | | | | | | \
 *        +-+-+-+-+-+-+-+-+  +
 *      1 | | | | | | | | |  |      number of strings recognized
 *        +-+-+-+-+-+-+-+-+  +----- by the automaton starting
 *        : : : : : : : : :  |      from this node. v-coding
 *        +-+-+-+-+-+-+-+-+  +
 *        | | | | | | | | | /
 *        +-+-+-+-+-+-+-+-+/
 *
 * ---- A vector of this node's arcs. An arc's layout depends on the combination of flags.
 *
 * 1) NEXT bit set, mapped arc label.
 *
 *        +----------------------- node pointed to is next
 *        | +--------------------- the last arc of the node
 *        | | +------------------- this arc leads to a final state (acceptor)
 *        | | |  _______+--------- arc's label; indexed if M &gt; 0, otherwise explicit label follows
 *        | | | / | | | |
 *       +-+-+-+-+-+-+-+-+\
 *     0 |N|L|F|M|M|M|M|M| +------ flags + (M) index of the mapped label.
 *       +-+-+-+-+-+-+-+-+/
 *       +-+-+-+-+-+-+-+-+\
 *     1 | | | | | | | | | +------ optional label if M == 0
 *       +-+-+-+-+-+-+-+-+/
 *       : : : : : : : : :
 *       +-+-+-+-+-+-+-+-+\
 *       |A|A|A|A|A|A|A|A| +------ v-coded goto address
 *       +-+-+-+-+-+-+-+-+/
 * </pre>
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class CFSA2 extends FSA{

	/** Automaton header version value. */
	public static final byte VERSION = (byte)0xC6;

	/** The target node of this arc follows the last arc of the current state (no goto field). */
	public static final int BIT_TARGET_NEXT = 0x80;

	/** The arc is the last one from the current node's arcs list. */
	public static final int BIT_LAST_ARC = 0x40;

	/**
	 * The arc corresponds to the last character of a sequence
	 * available when building the automaton (acceptor transition).
	 */
	public static final int BIT_FINAL_ARC = 0x20;

	/** The count of bits assigned to storing an indexed label. */
	private static final int LABEL_INDEX_BITS = 5;

	/** Masks only the M bits of a flag byte. */
	private static final int LABEL_INDEX_MASK = (1 << LABEL_INDEX_BITS) - 1;

	/** Maximum size of the labels index. */
	public static final int LABEL_INDEX_SIZE = (1 << LABEL_INDEX_BITS) - 1;

	/** Epsilon node's offset. */
	private static final int EPSILON = 0;

	/**
	 * An array of bytes with the internal representation of the automaton.
	 * Please see the documentation of this class for more information on how
	 * this structure is organized.
	 */
	private final byte[] arcs;

	/** Flags for this automaton version. */
	private final Set<FSAFlags> flags;

	/** Label mapping for M-indexed labels. */
	private final byte[] labelMapping;

	/** If {@code true} states are prepended with numbers. */
	private final boolean hasNumbers;


	/** Reads an automaton from a byte stream. */
	CFSA2(final InputStream stream) throws IOException{
		final DataInputStream in = new DataInputStream(stream);

		//read flags
		final short flagBits = in.readShort();
		flags = EnumSet.noneOf(FSAFlags.class);
		for(final FSAFlags f : FSAFlags.values())
			if(f.isSet(flagBits))
				flags.add(f);

		if(flagBits != FSAFlags.getMask(flags))
			throw new IOException("Unrecognized flags: 0x" + Integer.toHexString(flagBits));

		hasNumbers = flags.contains(FSAFlags.NUMBERS);

		//read mapping dictionary
		final int labelMappingSize = in.readByte() & 0xFF;
		labelMapping = new byte[labelMappingSize];
		in.readFully(labelMapping);

		//read arcs' data
		arcs = readRemaining(in);
	}

	@Override
	public int getRootNode(){
		//skip dummy node marking terminating state
		return getDestinationNodeOffset(getFirstArc(EPSILON));
	}

	@Override
	public final int getFirstArc(final int node){
		return (hasNumbers? skipVInt(node): node);
	}

	@Override
	public final int getNextArc(final int arc){
		return (isArcLast(arc)? 0: skipArc(arc));
	}

	@Override
	public int getEndNode(final int arc){
		return getDestinationNodeOffset(arc);
	}

	@Override
	public byte getArcLabel(final int arc){
		final int index = arcs[arc] & LABEL_INDEX_MASK;
		return (index > 0? labelMapping[index]: arcs[arc + 1]);
	}

	@Override
	public int getRightLanguageCount(final int node){
		assert getFlags().contains(FSAFlags.NUMBERS) : "This FSA was not compiled with NUMBERS.";

		return readVInt(arcs, node);
	}

	@Override
	public boolean isArcFinal(final int arc){
		return ((arcs[arc] & BIT_FINAL_ARC) != 0);
	}

	@Override
	public boolean isArcTerminal(final int arc){
		return (getDestinationNodeOffset(arc) == 0);
	}

	/**
	 * Returns {@code true} if this arc has {@code NEXT} bit set.
	 *
	 * @param arc The node's arc identifier.
	 * @return Returns true if the argument is the last arc of a node.
	 * @see #BIT_LAST_ARC
	 */
	public boolean isArcLast(final int arc){
		return ((arcs[arc] & BIT_LAST_ARC) != 0);
	}

	/**
	 * @param arc The node's arc identifier.
	 * @return Returns true if {@link #BIT_TARGET_NEXT} is set for this arc.
	 * @see #BIT_TARGET_NEXT
	 */
	public boolean isNextSet(final int arc){
		return ((arcs[arc] & BIT_TARGET_NEXT) != 0);
	}

	@Override
	public Set<FSAFlags> getFlags(){
		return flags;
	}

	/**
	 * Returns the address of the node pointed to by this arc.
	 */
	private int getDestinationNodeOffset(int arc){
		if(isNextSet(arc)){
			//follow until the last arc of this state
			while(!isArcLast(arc))
				arc = getNextArc(arc);

			//and return the byte right after it
			return skipArc(arc);
		}
		else
			//the destination node address is v-coded. v-code starts either at the next byte (label indexed)
			//or after the next byte (label explicit)
			return readVInt(arcs, arc + ((arcs[arc] & LABEL_INDEX_MASK) == 0? 2: 1));
	}

	/** Read the arc's layout and skip as many bytes, as needed, to skip it. */
	private int skipArc(int offset){
		final int flag = arcs[offset ++];

		//explicit label?
		if((flag & LABEL_INDEX_MASK) == 0)
			offset ++;

		//explicit goto?
		if((flag & BIT_TARGET_NEXT) == 0)
			offset = skipVInt(offset);

		//assert offset < arcs.length;

		return offset;
	}

	/** Read a v-int. */
	private static int readVInt(final byte[] array, int offset){
		byte b = array[offset];
		int value = b & 0x7F;
		for(int shift = 7; b < 0; shift += 7){
			b = array[++ offset];
			value |= (b & 0x7F) << shift;
		}
		return value;
	}

	/** Skip a v-int. */
	private int skipVInt(int offset){
		//do nothing
		while(arcs[offset ++] < 0){}
		return offset;
	}

}
