package unit731.hunlinter.services.filelistener;

import java.nio.file.Path;


/** Interface definition for a callback to be invoked when a file under watch is changed. */
public interface FileChangeListener{

	default void fileModified(final Path file){}

	default void fileDeleted(final Path file){}

}
