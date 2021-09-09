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
package io.github.mtrevisan.hunlinter.datastructures.fsa.serializers;

import io.github.mtrevisan.hunlinter.datastructures.fsa.FSAAbstract;
import io.github.mtrevisan.hunlinter.datastructures.fsa.builders.FSAFlags;
import io.github.mtrevisan.hunlinter.gui.ProgressCallback;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;


/**
 * All FSA serializers will implement this interface.
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public interface FSASerializerInterface{

	/**
	 * Serialize a Finite State Automaton to an output stream.
	 *
	 * @param fsa The automaton to serialize.
	 * @param os The output stream to serialize to.
	 * @param progressCallback	The progress callback
	 * @param <T> A subclass of {@link OutputStream}, returned for chaining.
	 * @return Returns {@code T} for chaining.
	 * @throws IOException Rethrown if an I/O error occurs.
	 */
	<T extends OutputStream> T serialize(final FSAAbstract fsa, final T os, final ProgressCallback progressCallback) throws IOException;

	/**
	 * @return Returns the set of flags supported by the serializer (and the output automaton).
	 */
	Set<FSAFlags> getSupportedFlags();

	/**
	 * Sets the filler separator (only if {@link #getSupportedFlags()} returns {@link FSAFlags#SEPARATORS}).
	 *
	 * @param filler The filler separator byte.
	 * @return Returns {@code this} for call chaining.
	 */
	FSASerializerInterface withFiller(final byte filler);

	/**
	 * Sets the annotation separator (only if {@link #getSupportedFlags()} returns {@link FSAFlags#SEPARATORS}).
	 *
	 * @param annotationSeparator The filler separator byte.
	 * @return Returns {@code this} for call chaining.
	 */
	FSASerializerInterface withAnnotationSeparator(final byte annotationSeparator);

	/**
	 * Enables support for right language count on nodes, speeding up perfect hash
	 * counts (only if {@link #getSupportedFlags()} returns {@link FSAFlags#NUMBERS}).
	 *
	 * @return Returns {@code this} for call chaining.
	 */
	FSASerializerInterface serializeWithNumbers();

}
