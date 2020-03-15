package unit731.hunlinter.workers.exceptions;


import unit731.hunlinter.workers.core.IndexDataPair;


public class LinterException extends RuntimeException{

	public enum FixActionType{ADD, REPLACE, REMOVE}


	private final IndexDataPair<?> data;
	//FIXME useful?
	private final Runnable fix;
	private final FixActionType fixActionType;


	public LinterException(final Exception cause, final Object data){
		super(cause);

		this.data = IndexDataPair.of(-1, data);
		fix = null;
		fixActionType = null;
	}

	public LinterException(final Exception cause, final IndexDataPair<?> data){
		super(cause);

		this.data = data;
		fix = null;
		fixActionType = null;
	}

	public LinterException(final String description){
		this(description, null, null, null);
	}

	public LinterException(final String description, final IndexDataPair<?> data){
		this(description, data, null, null);
	}

	public LinterException(final String description, final IndexDataPair<?> data, final Runnable fix,
			final FixActionType fixActionType){
		super(description);

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
