package unit731.hunspeller.parsers.workers.exceptions;


public class ProjectNotFoundException extends Exception{

	private static final long serialVersionUID = 3943841591851856914L;


	private final String path;


	public ProjectNotFoundException(final String path, final Exception e){
		super(e);

		this.path = path;
	}

	public String getPath(){
		return path;
	}

}
