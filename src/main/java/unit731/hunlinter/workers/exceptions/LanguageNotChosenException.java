package unit731.hunlinter.workers.exceptions;


public class LanguageNotChosenException extends Exception{

	private static final long serialVersionUID = 7746149848221945649L;


	public LanguageNotChosenException(final String description){
		super(description);
	}

}
