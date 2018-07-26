package unit731.hunspeller.services;

import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.status.ErrorStatus;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class ApplicationLogAppender<T> extends OutputStreamAppender<T>{

	private static final DelegatingOutputStream DELEGATING_OUTPUT_STREAM = new DelegatingOutputStream(null);


	private static class DelegatingOutputStream extends FilterOutputStream{

		/** Creates a delegating output stream with a NO-OP delegate */
		public DelegatingOutputStream(OutputStream out){
			super(new OutputStream(){
				@Override
				public void write(int b) throws IOException{}
			});
		}

		void setOutputStream(OutputStream outputStream){
			this.out = outputStream;
		}

	}

	@Override
	public void start(){
		setOutputStream(DELEGATING_OUTPUT_STREAM);

		super.start();
	}

	public static void setStaticOutputStream(OutputStream outputStream){
		DELEGATING_OUTPUT_STREAM.setOutputStream(outputStream);
	}

	@Override
	protected void subAppend(T event){
		if(!isStarted())
			return;

		try{
			//this step avoids LBCLASSIC-139
			if(event instanceof DeferredProcessingAware)
				((DeferredProcessingAware)event).prepareForDeferredProcessing();

			// the synchronization prevents the OutputStream from being closed while we
			// are writing. It also prevents multiple threads from entering the same
			// converter. Converters assume that they are in a synchronized block.
			// lock.lock();
			byte[] byteArray = this.encoder.encode(event);
			writeBytes(byteArray);

		}
		catch(IOException ioe){
			// as soon as an exception occurs, move to non-started state
			// and add a single ErrorStatus to the SM.
			this.started = false;
			addStatus(new ErrorStatus("IO failure in appender", this, ioe));
		}
	}

}
