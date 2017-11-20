package unit731.hunspeller.services.filelistener;


/** Interface definition for a callback to be invoked when a file under watch is changed. */
public interface FileChangeListener{

	/**
	 * Called when the file is created.
	 *
	 * @param filePath	The file path.
	 */
	default void fileCreated(String filePath){}

	/**
	 * Called when the file is modified.
	 *
	 * @param filePath	The file path.
	 */
	default void fileModified(String filePath){}

	/**
	 * Called when the file is deleted.
	 *
	 * @param filePath	The file path.
	 */
	default void fileDeleted(String filePath){}
	
}
