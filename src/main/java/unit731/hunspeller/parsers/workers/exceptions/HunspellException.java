package unit731.hunspeller.parsers.workers.exceptions;


public class HunspellException extends RuntimeException{

	public enum FixActionType{ADD, REPLACE, REMOVE};


	private final Runnable fix;
	private final FixActionType fixActionType;


	public HunspellException(final String description){
		this(description, null, null);
	}

	public HunspellException(final String description, final Runnable fix, final FixActionType fixActionType){
		super(description);

		this.fix = fix;
		this.fixActionType = fixActionType;
	}

	public Runnable getFixAction(){
		return fix;
	}

	public FixActionType getFixActionType(){
		return fixActionType;
	}

}
