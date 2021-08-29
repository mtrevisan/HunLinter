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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Standard FSA file header, as described in <code>fsa</code> package documentation.
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class FSAHeader{

	private static final int FSA_MAGIC1 = '\\';
	private static final int FSA_MAGIC2 = 'f';
	private static final int FSA_MAGIC3 = 's';
	private static final int FSA_MAGIC4 = 'a';
	/** FSA magic (4 bytes). */
	private static final int FSA_MAGIC = (FSA_MAGIC1 << 24) | (FSA_MAGIC2 << 16) | (FSA_MAGIC3 << 8) | FSA_MAGIC4;


	/** FSA version number. */
	private final byte version;


	FSAHeader(final byte version){
		this.version = version;
	}

	public byte getVersion(){
		return version;
	}

	/**
	 * Read FSA header and version from a stream, consuming read bytes.
	 *
	 * @param in The input stream to read data from.
	 * @return Returns a valid {@link FSAHeader} with version information.
	 * @throws IOException If the stream ends prematurely or if it contains invalid data.
	 */
	public static FSAHeader read(final InputStream in) throws IOException{
		if(in.read() != FSA_MAGIC1 || in.read() != FSA_MAGIC2 || in.read() != FSA_MAGIC3 || in.read() != FSA_MAGIC4)
			throw new IOException("Invalid file header, probably not an FSA.");

		final int version = in.read();
		if(version == -1)
			throw new IOException("Truncated file, no version number.");

		return new FSAHeader((byte)version);
	}

	/**
	 * Writes FSA magic bytes and version information.
	 *
	 * @param os      The stream to write to.
	 * @param version Automaton version.
	 * @throws IOException Rethrown if writing fails.
	 */
	public static void write(final OutputStream os, final byte version) throws IOException{
		os.write(FSA_MAGIC >> 24);
		os.write(FSA_MAGIC >> 16);
		os.write(FSA_MAGIC >> 8);
		os.write(FSA_MAGIC);
		os.write(version);
	}

}
