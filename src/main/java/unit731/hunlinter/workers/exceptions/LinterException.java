package unit731.hunlinter.workers.exceptions;


import unit731.hunlinter.workers.core.IndexDataPair;


public class LinterException extends RuntimeException{

	private static final long serialVersionUID = 2097260898128903703L;

	public enum FixActionType{ADD, REPLACE, REMOVE}


	private final IndexDataPair<?> data;
	//FIXME useful?
	private final Runnable fix;
	private final FixActionType fixActionType;


	public LinterException(final Throwable cause, final Object data){
		this(null, cause, IndexDataPair.of(-1, data), null, null);
	}

	public LinterException(final Throwable cause, final IndexDataPair<?> data){
		this(null, cause, data, null, null);
	}

	public LinterException(final String message){
		this(message, null, null, null);
	}

	public LinterException(final String message, final IndexDataPair<?> data){
		this(message, null, data, null, null);
	}

	public LinterException(final String message, final Throwable cause, final IndexDataPair<?> data){
		this(message, cause, data, null, null);
	}

	public LinterException(final String message, final IndexDataPair<?> data, final Runnable fix,
			final FixActionType fixActionType){
		this(message, null, data, fix, fixActionType);
	}

	public LinterException(final String message,  final Throwable cause, final IndexDataPair<?> data, final Runnable fix,
			final FixActionType fixActionType){
		super(message, cause);

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
