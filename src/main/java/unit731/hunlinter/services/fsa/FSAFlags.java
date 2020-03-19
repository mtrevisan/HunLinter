package unit731.hunlinter.services.fsa;

import java.util.Set;


/** FSA automaton flags. Where applicable, flags follow Daciuk's <code>fsa</code> package. */
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
	 * @return Returns <code>true</code> iff this flag is set in <code>flags</code>.
	 */
	public boolean isSet(final int flags){
		return ((flags & bits) != 0);
	}

	/**
	 * @param flags A set of flags to encode.
	 * @return Returns the set of flags encoded as packed <code>short</code>.
	 */
	public static short getMask(final Set<FSAFlags> flags){
		short value = 0;
		for(final FSAFlags f : flags)
			value |= f.bits;
		return value;
	}

}
