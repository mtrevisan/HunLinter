package unit731.hunlinter.datastructures.fsa.stemming;

import java.nio.ByteBuffer;


/**
 * The logic of encoding one sequence of bytes relative to another sequence of
 * bytes. The "base" form and the "derived" form are typically the stem of
 * a word and the inflected form of a word.
 *
 * <p>Derived form encoding helps in making the data for the automaton smaller
 * and more repetitive (which results in higher compression rates).
 *
 * <p>See example implementation for details.
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public interface SequenceEncoderInterface{

	/** Maximum encodable single-byte code */
	int REMOVE_EVERYTHING = 255;


	/**
	 * Encodes <code>target</code> relative to <code>source</code>, optionally reusing the provided {@link ByteBuffer}.
	 *
	 * @param source   The source byte sequence.
	 * @param target   The target byte sequence to encode relative to <code>source</code>
	 * @return	The {@link ByteBuffer} with encoded <code>target</code>.
	 */
	byte[] encode(final byte[] source, final byte[] target);

	/**
	 * Decodes <code>encoded</code> relative to <code>source</code>, optionally reusing the provided {@link ByteBuffer}.
	 *
	 * @param source	The source byte sequence.
	 * @param encoded	The {@linkplain #encode previously encoded} byte sequence.
	 * @return	The {@link ByteBuffer} with decoded <code>target</code>.
	 */
	byte[] decode(final byte[] source, final byte[] encoded);


	default byte encodeValue(final int value){
		return (byte)(value + 'A');
	}

	default int decodeValue(final byte value){
		return ((value - 'A') & 0xFF);
	}

}
