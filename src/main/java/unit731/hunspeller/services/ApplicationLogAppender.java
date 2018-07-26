package unit731.hunspeller.services;

import ch.qos.logback.core.OutputStreamAppender;
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

}
