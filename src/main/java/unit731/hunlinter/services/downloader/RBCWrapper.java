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
package unit731.hunlinter.services.downloader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;


class RBCWrapper implements ReadableByteChannel{

	private final RBCWrapperDelegate delegate;
	private final long expectedSize;
	private final ReadableByteChannel rbc;

	private long readSoFar;


	RBCWrapper(final ReadableByteChannel rbc, final long expectedSize, final RBCWrapperDelegate delegate){
		Objects.requireNonNull(delegate, "Delegate cannot be null");

		this.delegate = delegate;
		this.expectedSize = expectedSize;
		this.rbc = rbc;
	}

	@Override
	public boolean isOpen(){ return rbc.isOpen(); }

	@Override
	public int read(final ByteBuffer bb) throws IOException{
		final int readBytes;
		if((readBytes = rbc.read(bb)) > 0){
			readSoFar += readBytes;

			final double progress = (expectedSize > 0? readSoFar * 100. / expectedSize: -1.);
			delegate.rbcProgressCallback(this, progress);
		}
		return readBytes;
	}

	public long getReadSoFar(){ return readSoFar; }

	@Override
	public void close() throws IOException{ rbc.close(); }

}
