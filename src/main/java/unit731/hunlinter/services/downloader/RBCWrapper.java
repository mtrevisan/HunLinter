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
		Objects.requireNonNull(delegate);

		this.delegate = delegate;
		this.expectedSize = expectedSize;
		this.rbc = rbc;
	}

	@Override
	public boolean isOpen(){ return rbc.isOpen(); }

	@Override
	public int read(final ByteBuffer bb) throws IOException{
		int readBytes;
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
