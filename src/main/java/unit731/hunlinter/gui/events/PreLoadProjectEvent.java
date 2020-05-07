package unit731.hunlinter.gui.events;

import java.nio.file.Path;


public class PreLoadProjectEvent{

	private final Path project;


	public PreLoadProjectEvent(final Path project){
		this.project = project;
	}

	public LoadProjectEvent convertToLoadEvent(){
		return new LoadProjectEvent(project);
	}

}
