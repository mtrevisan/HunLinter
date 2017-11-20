package unit731.hunspeller.services.filelistener;

import java.nio.file.Path;


/** Interface definition for a callback to be invoked when a file under watch is changed. */
public interface FileChangeListener{

	default void fileCreated(Path file){}

	default void fileModified(Path file){}

	default void fileDeleted(Path file){}
	
}
