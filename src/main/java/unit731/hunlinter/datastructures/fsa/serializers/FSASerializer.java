/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package unit731.hunlinter.datastructures.fsa.serializers;

import unit731.hunlinter.datastructures.fsa.FSA;
import unit731.hunlinter.datastructures.fsa.builders.FSAFlags;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.function.Consumer;


/**
 * All FSA serializers will implement this interface.
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
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
	<T extends OutputStream> T serialize(final FSA fsa, final T os, final Consumer<Integer> progressCallback) throws IOException;

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
