package unit731.hunlinter.services.downloader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;


class RBCWrapper implements ReadableByteChannel{

	private final RBCWrapperDelegate delegate;
	private final long expectedSize;
	private final ReadableByteChannel rbc;

	private long readSoFar;


	RBCWrapper(final ReadableByteChannel rbc, final long expectedSize, final RBCWrapperDelegate delegate){
		this.delegate = delegate;
		this.expectedSize = expectedSize;
		this.rbc = rbc;
	}

	public void close() throws IOException{ rbc.close(); }

	public long getReadSoFar(){ return readSoFar; }

	public boolean isOpen(){ return rbc.isOpen(); }

	public int read(final ByteBuffer bb) throws IOException{
		int n;
		if((n = rbc.read(bb)) > 0){
			readSoFar += n;

			if(delegate != null){
				final double progress = (expectedSize > 0? (double) readSoFar * 100. / expectedSize: -1.);
				delegate.rbcProgressCallback(this, progress);
			}
		}
		return n;
	}

}
