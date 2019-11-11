package unit731.hunspeller.services.diff;


/** Thrown whenever a delta cannot be applied as a patch to a given text */
public class PatchFailedException extends Exception{

	public PatchFailedException(){
	}

	public PatchFailedException(final String message){
		super(message);
	}

}
