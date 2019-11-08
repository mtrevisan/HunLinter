package unit731.hunspeller.parsers.workers.exceptions;


public class HunspellException extends Exception{

	public enum FixType{FIX, ADD, REMOVE};


	private final Runnable fix;
	private final FixType fixType;

	public HunspellException(final String description, final Runnable fix, final FixType fixType){
		super(description);

		this.fix = fix;
		this.fixType = fixType;
	}

	public Runnable getFix(){
		return fix;
	}

	public FixType getFixType(){
		return fixType;
	}

}
