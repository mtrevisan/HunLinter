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
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;


/**
 * FSA binary format implementation for version 5.
 *
 * <p>
 * Version 5 indicates the dictionary was built with these flags:
 * {@link FSAFlags#FLEXIBLE}, {@link FSAFlags#STOPBIT} and
 * {@link FSAFlags#NEXTBIT}. The internal representation of the FSA must
 * therefore follow this description (please note this format describes only a
 * single transition (arc), not the entire dictionary file).
 *
 * <pre>
 * ---- this node header present only if automaton was compiled with NUMBERS option.
 * Byte
 *        +-+-+-+-+-+-+-+-+\
 *      0 | | | | | | | | | \  LSB
 *        +-+-+-+-+-+-+-+-+  +
 *      1 | | | | | | | | |  |      number of strings recognized
 *        +-+-+-+-+-+-+-+-+  +----- by the automaton starting
 *        : : : : : : : : :  |      from this node.
 *        +-+-+-+-+-+-+-+-+  +
 *  ctl-1 | | | | | | | | | /  MSB
 *        +-+-+-+-+-+-+-+-+/
 *
 * ---- remaining part of the node
 *
 * Byte
 *       +-+-+-+-+-+-+-+-+\
 *     0 | | | | | | | | | +------ label
 *       +-+-+-+-+-+-+-+-+/
 *
 *                  +------------- node pointed to is next
 *                  | +----------- the last arc of the node
 *                  | | +--------- the arc is final
 *                  | | |
 *             +-----------+
 *             |    | | |  |
 *         ___+___  | | |  |
 *        /       \ | | |  |
 *       MSB           LSB |
 *        7 6 5 4 3 2 1 0  |
 *       +-+-+-+-+-+-+-+-+ |
 *     1 | | | | | | | | | \ \
 *       +-+-+-+-+-+-+-+-+  \ \  LSB
 *       +-+-+-+-+-+-+-+-+     +
 *     2 | | | | | | | | |     |
 *       +-+-+-+-+-+-+-+-+     |
 *     3 | | | | | | | | |     +----- target node address (in bytes)
 *       +-+-+-+-+-+-+-+-+     |      (not present except for the byte
 *       : : : : : : : : :     |       with flags if the node pointed to
 *       +-+-+-+-+-+-+-+-+     +       is next)
 *   gtl | | | | | | | | |    /  MSB
 *       +-+-+-+-+-+-+-+-+   /
 * gtl+1                           (gtl = gotoLength)
 * </pre>
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class FSA extends FSAAbstract{

	/** Default filler byte. */
	public static final byte DEFAULT_FILLER = '_';
	/** Default annotation byte. */
	public static final byte DEFAULT_ANNOTATION = '+';

	/** Automaton version as in the file header. */
	public static final byte VERSION = 5;

	/**
	 * Bit indicating that an arc corresponds to the last character of a sequence
	 * available when building the automaton.
	 */
	public static final int BIT_FINAL_ARC = 0x01;

	/**
	 * Bit indicating that an arc is the last one of the node's list and the
	 * following one belongs to another node.
	 */
	public static final int BIT_LAST_ARC = 0x02;

	/**
	 * Bit indicating that the target node of this arc follows it in the
	 * compressed automaton structure (no goto field).
	 */
	public static final int BIT_TARGET_NEXT = 0x04;

	/**
	 * An offset in the arc structure, where the address and flags field begins.
	 * In version 5 of FSA automata, this value is constant (1, skip label).
	 */
	public static final int ADDRESS_OFFSET = 1;

	/**
	 * An array of bytes with the internal representation of the automaton. Please
	 * see the documentation of this class for more information on how this
	 * structure is organized.
	 */
	public final byte[] arcs;

	/** The length of the node header structure (if the automaton was compiled with {@code NUMBERS} option). Otherwise, zero. */
	public final int nodeDataLength;

	/** Flags for this automaton version. */
	private Set<FSAFlags> flags;

	/** Number of bytes each address takes in full, expanded form (goto length). */
	public final int gtl;

	/** Filler character. */
	public final byte filler;

	/** Annotation character. */
	public final byte annotation;


	/** Read and wrap a binary automaton in FSA version 5. */
	FSA(final InputStream stream) throws IOException{
		final DataInputStream in = new DataInputStream(stream);

		filler = in.readByte();
		annotation = in.readByte();
		final byte hgtl = in.readByte();

		/*
		 * Determine if the automaton was compiled with NUMBERS
		 * If so, modify ctl and goto fields accordingly.
		 */
		flags = EnumSet.of(FSAFlags.FLEXIBLE, FSAFlags.STOPBIT, FSAFlags.NEXTBIT);
		if((hgtl & 0xF0) != 0)
			flags.add(FSAFlags.NUMBERS);

		flags = Collections.unmodifiableSet(flags);

		nodeDataLength = (hgtl >>> 4) & 0x0F;
		gtl = hgtl & 0x0F;

		arcs = readRemaining(in);
	}

	/**
	 * @return The start node of this automaton.
	 */
	@Override
	public final int getRootNode(){
		// Skip dummy node marking terminating state.
		final int epsilonNode = skipArc(getFirstArc(0));

		// And follow the epsilon node's first (and only) arc.
		return getDestinationNodeOffset(getFirstArc(epsilonNode));
	}

	@Override
	public final int getFirstArc(final int node){
		return (nodeDataLength + node);
	}

	@Override
	public final int getNextArc(final int arc){
		return (isArcLast(arc)? 0: skipArc(arc));
	}

	@Override
	public final int getEndNode(final int arc){
		return getDestinationNodeOffset(arc);
	}

	@Override
	public final byte getArcLabel(final int arc){
		return arcs[arc];
	}

	@Override
	public final boolean isArcFinal(final int arc){
		return ((arcs[arc + ADDRESS_OFFSET] & BIT_FINAL_ARC) != 0);
	}

	@Override
	public final boolean isArcTerminal(final int arc){
		return (getDestinationNodeOffset(arc) == 0);
	}

	/**
	 * @return	The number encoded at the given node. The number equals the count
	 *		of the set of suffixes reachable from {@code node} (called its right language).
	 */
	@Override
	public final int getRightLanguageCount(final int node){
		assert flags.contains(FSAFlags.NUMBERS) : "This FSA was not compiled with NUMBERS.";

		return decodeFromBytes(arcs, node, nodeDataLength);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * For this automaton version, an additional {@link FSAFlags#NUMBERS} flag may
	 * be set to indicate the automaton contains extra fields for each node.
	 * </p>
	 */
	@Override
	public final Set<FSAFlags> getFlags(){
		return flags;
	}

	/**
	 * Returns {@code true} if this arc has {@code NEXT} bit set.
	 *
	 * @param arc The node's arc identifier.
	 * @return Returns true if the argument is the last arc of a node.
	 * @see #BIT_LAST_ARC
	 */
	public final boolean isArcLast(final int arc){
		return ((arcs[arc + ADDRESS_OFFSET] & BIT_LAST_ARC) != 0);
	}

	/**
	 * @param arc	The node's arc identifier.
	 * @return   {@code true} if {@link #BIT_TARGET_NEXT} is set for this arc.
	 * @see #BIT_TARGET_NEXT
	 */
	public final boolean isNextSet(final int arc){
		return (arcs[arc + ADDRESS_OFFSET] & BIT_TARGET_NEXT) != 0;
	}

	/** Returns the address of the node pointed to by this . */
	final int getDestinationNodeOffset(final int arc){
		if(isNextSet(arc))
			//the destination node follows this arc in the array
			return skipArc(arc);
		else
			//the destination node address has to be extracted from the arc's goto field
			return decodeFromBytes(arcs, arc + ADDRESS_OFFSET, gtl) >>> 3;
	}

	/** Read the arc's layout and skip as many bytes, as needed. */
	private int skipArc(final int offset){
		return offset + (isNextSet(offset)
			//label + flags
			? 1 + 1
			//label + flags/address
			: 1 + gtl);
	}

	/** Returns an n-byte integer encoded in byte-packed representation. */
	private int decodeFromBytes(final byte[] arcs, final int start, final int n){
		int r = 0;
		for(int i = n; -- i >= 0; )
			r = r << 8 | (arcs[start + i] & 0xFF);
		return r;
	}

}
