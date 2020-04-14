package unit731.hunlinter.services.fsa.stemming;

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

	/**
	 * Encodes <code>target</code> relative to <code>source</code>, optionally reusing the provided {@link ByteBuffer}.
	 *
	 * @param source   The source byte sequence.
	 * @param target   The target byte sequence to encode relative to <code>source</code>
	 * @param reuse   Reuses the provided {@link ByteBuffer} or allocates a new one if there is not enough remaining space.
	 * @return	The {@link ByteBuffer} with encoded <code>target</code>.
	 */
	ByteBuffer encode(final ByteBuffer source, final ByteBuffer target, final ByteBuffer reuse);

	/**
	 * Decodes <code>encoded</code> relative to <code>source</code>, optionally reusing the provided {@link ByteBuffer}.
	 *
	 * @param reuse	Reuses the provided {@link ByteBuffer} or allocates a new one if there is not enough remaining space.
	 * @param source	The source byte sequence.
	 * @param encoded	The {@linkplain #encode previously encoded} byte sequence.
	 * @return	The {@link ByteBuffer} with decoded <code>target</code>.
	 */
	ByteBuffer decode(final ByteBuffer reuse, final ByteBuffer source, final ByteBuffer encoded);

	/**
	 * The number of encoded form's prefix bytes that should be ignored (needed for separator lookup).
	 * An ugly workaround for GH-85, should be fixed by prior knowledge of whether the dictionary contains tags;
	 * then we can scan for separator right-to-left.
	 *
	 * @return	The number of bytes
	 * @see "https://github.com/morfologik/morfologik-stemming/issues/85"
	 */
	@Deprecated
	int prefixBytes();

}
