package unit731.hunlinter.workers.exceptions;

import unit731.hunlinter.workers.core.IndexDataPair;


public class LinterWarning extends Exception{

	private final IndexDataPair<?> data;


	public LinterWarning(final Throwable cause, final Object data){
		this(null, cause, IndexDataPair.of(-1, data));
	}

	public LinterWarning(final Throwable cause, final IndexDataPair<?> data){
		this(null, cause, data);
	}

	public LinterWarning(final String message){
		this(message, null, null);
	}

	public LinterWarning(final String message, final IndexDataPair<?> data){
		this(message, null, data);
	}

	public LinterWarning(final String message, final Throwable cause, final IndexDataPair<?> data){
		super(message, cause);

		this.data = data;
	}

	public IndexDataPair<?> getData(){
		return data;
	}

}
