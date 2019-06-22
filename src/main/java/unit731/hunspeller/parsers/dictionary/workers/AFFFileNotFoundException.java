package unit731.hunspeller.parsers.dictionary.workers;


public class AFFFileNotFoundException extends Exception{
	
	private static final long serialVersionUID = 3943841591851856914L;


	private String path;


	public AFFFileNotFoundException(String path, Exception e){
		super(e);

		this.path = path;
	}

	public String getPath(){
		return path;
	}

}
