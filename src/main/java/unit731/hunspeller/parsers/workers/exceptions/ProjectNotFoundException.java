package unit731.hunspeller.parsers.workers.exceptions;


import java.nio.file.Path;


public class ProjectNotFoundException extends Exception{

	private static final long serialVersionUID = 3943841591851856914L;


	private final Path projectPath;


	public ProjectNotFoundException(final Path projectPath, final String description){
		super(description);

		this.projectPath = projectPath;
	}

	public ProjectNotFoundException(final Path projectPath, final Exception e){
		super(e);

		this.projectPath = projectPath;
	}

	public Path getProjectPath(){
		return projectPath;
	}

}
