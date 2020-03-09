package unit731.hunlinter.services.fsa.builders;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;


/** All FSA serializers will implement this interface. */
public interface FSABinarySerializer{

	/**
	 * Serialize a finite state automaton to an output stream.
	 *
	 * @param fsa The automaton to serialize.
	 * @param os The output stream to serialize to.
	 * @param <T> A subclass of {@link OutputStream}, returned for chaining.
	 * @return Returns <code>T</code> for chaining.
	 * @throws IOException Rethrown if an I/O error occurs.
	 */
	<T extends OutputStream> T serialize(final FSA fsa, final T os) throws IOException;

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
	FSABinarySerializer withFiller(final byte filler);

	/**
	 * Sets the annotation separator (only if {@link #getFlags()} returns {@link FSAFlags#SEPARATORS}).
	 *
	 * @param annotationSeparator The filler separator byte.
	 * @return Returns <code>this</code> for call chaining.
	 */
	FSABinarySerializer withAnnotationSeparator(final byte annotationSeparator);

	/**
	 * Enables support for right language count on nodes, speeding up perfect hash
	 * counts (only if {@link #getFlags()} returns {@link FSAFlags#NUMBERS}).
	 *
	 * @return Returns <code>this</code> for call chaining.
	 */
	FSABinarySerializer withNumbers();

}
