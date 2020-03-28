package unit731.hunlinter.services.fsa.serializers;

import unit731.hunlinter.services.fsa.FSA;
import unit731.hunlinter.services.fsa.FSAFlags;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.function.BiConsumer;


/**
 * All FSA serializers will implement this interface.
 *
 * @see "org.carrot2.morfologik-parent, 2.1.8-SNAPSHOT, 2020-01-02"
 */
public interface FSASerializer{

	/**
	 * Serialize a Finite State Automaton to an output stream.
	 *
	 * @param fsa The automaton to serialize.
	 * @param os The output stream to serialize to.
	 * @param progressCallback	The progress callback
	 * @param <T> A subclass of {@link OutputStream}, returned for chaining.
	 * @return Returns <code>T</code> for chaining.
	 * @throws IOException Rethrown if an I/O error occurs.
	 */
	<T extends OutputStream> T serialize(final FSA fsa, final T os, final BiConsumer<Integer, Integer> progressCallback) throws IOException;

	/**
	 * @return Returns the set of flags supported by the serializer (and the output automaton).
	 */
	Set<FSAFlags> getFlags();

	/**
	 * Sets the filler separator (only if {@link #getFlags()} returns {@link FSAFlags#SEPARATORS}).
	 *
	 * @param filler The filler separator byte.
	 * @return Returns <code>this</code> for call chaining.
	 */
	FSASerializer withFiller(final byte filler);

	/**
	 * Sets the annotation separator (only if {@link #getFlags()} returns {@link FSAFlags#SEPARATORS}).
	 *
	 * @param annotationSeparator The filler separator byte.
	 * @return Returns <code>this</code> for call chaining.
	 */
	FSASerializer withAnnotationSeparator(final byte annotationSeparator);

	/**
	 * Enables support for right language count on nodes, speeding up perfect hash
	 * counts (only if {@link #getFlags()} returns {@link FSAFlags#NUMBERS}).
	 *
	 * @return Returns <code>this</code> for call chaining.
	 */
	FSASerializer serializeWithNumbers();

}
