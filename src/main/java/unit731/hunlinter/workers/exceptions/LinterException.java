package unit731.hunlinter.workers.exceptions;


import unit731.hunlinter.workers.core.IndexDataPair;


public class LinterException extends RuntimeException{

	public enum FixActionType{ADD, REPLACE, REMOVE}


	private final IndexDataPair<?> data;
	//FIXME useful?
	private final Runnable fix;
	private final FixActionType fixActionType;


	public LinterException(final Throwable cause, final Object data){
		super(cause);

		this.data = IndexDataPair.of(-1, data);
		fix = null;
		fixActionType = null;
	}

	public LinterException(final Throwable cause, final IndexDataPair<?> data){
		super(cause);

		this.data = data;
		fix = null;
		fixActionType = null;
	}

	public LinterException(final String message, final Throwable cause, final IndexDataPair<?> data){
		super(message, cause);

		this.data = data;
		fix = null;
		fixActionType = null;
	}

	public LinterException(final String message){
		this(message, null, null, null);
	}

	public LinterException(final String message, final IndexDataPair<?> data){
		this(message, data, null, null);
	}

	public LinterException(final String message, final IndexDataPair<?> data, final Runnable fix,
			final FixActionType fixActionType){
		super(message);

		this.data = data;
		this.fix = fix;
		this.fixActionType = fixActionType;
	}

	public IndexDataPair<?> getData(){
		return data;
	}

	public boolean canFix(){
		return (fix != null);
	}

	public Runnable getFixAction(){
		return fix;
	}

	public FixActionType getFixActionType(){
		return fixActionType;
	}

}
