/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.services.sorters.externalsorter;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;


/**
 * This is essentially a thin wrapper on top of a BufferedReader which keeps the last line in memory.
 *
 * @see <a href="https://github.com/lemire/externalsortinginjava">External-Memory Sorting in Java</a>, version 0.4.4, 11/3/2020
 */
class BinaryFileBuffer implements Closeable{

	private final BufferedReader reader;
	private String cache;


	BinaryFileBuffer(final BufferedReader reader) throws IOException{
		Objects.requireNonNull(reader, "Scanner cannot be null");

		this.reader = reader;

		readNextLine();
	}

	@Override
	public final void close() throws IOException{
		reader.close();
	}

	/** Returns true when no more lines are available. */
	public final boolean isEmpty(){
		return (cache == null);
	}

	/** Peek the current cached line without consuming it. */
	public final String peek(){
		return cache;
	}

	/** Pop the cached line and load the next one. */
	public final String pop() throws IOException{
		final String answer = peek();
		readNextLine();
		return answer;
	}

	/** Load the next line into cache; sets cache to null at EOF. */
	private void readNextLine() throws IOException{
		cache = reader.readLine();
	}

}
