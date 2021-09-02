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

import io.github.mtrevisan.hunlinter.datastructures.fsa.FSA;


/**
 * FSA automaton flags. Where applicable, flags follow Daciuk's {@code fsa} package.
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public enum FSAFlags{

	/** Daciuk: flexible FSA encoding. */
	FLEXIBLE(0x0001),
	/** Daciuk: stop bit in use. */
	STOPBIT(0x0002),
	/** Daciuk: next bit in use. */
	NEXTBIT(0x0004),
	/** Daciuk: tails compression. */
	TAILS(0x0008),

	/*
	 * These flags are outside of byte range (never occur in Daciuk's FSA).
	 */

	/**
	 * The FSA contains right-language count numbers on states.
	 *
	 * @see FSA#getRightLanguageCount(int)
	 */
	NUMBERS(0x0100),

	/**
	 * The FSA supports legacy built-in separator and filler characters (Daciuk's
	 * FSA package compatibility).
	 */
	SEPARATORS(0x0200);

	/**
	 * Bit mask for the corresponding flag.
	 */
	public final int bits;


	FSAFlags(final int bits){
		this.bits = bits;
	}

	/**
	 * @param flags The bitset with flags.
	 * @return Returns {@code true} iff this flag is set in {@code flags}.
	 */
	public boolean isSet(final int flags){
		return ((flags & bits) != 0);
	}

	/**
	 * @param flags A set of flags to encode.
	 * @return Returns the set of flags encoded as packed {@code short}.
	 */
	public static short getMask(final Iterable<FSAFlags> flags){
		short value = 0;
		for(final FSAFlags f : flags)
			value |= f.bits;
		return value;
	}

}
