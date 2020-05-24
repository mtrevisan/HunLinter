package unit731.hunlinter.gui.events;

import java.nio.file.Path;


public class LoadProjectEvent{

	private final Path project;


	public LoadProjectEvent(final Path project){
		this.project = project;
	}

	public Path getProject(){
		return project;
	}

}
