package unit731.hunspeller.services.filelistener;

import java.io.IOException;


public interface FileListener{

	/** Starts the service. This method blocks until the service has completely started. */
	void start();

	/** Stops the service. This method blocks until the service has completely shut down. */
	void stop();

	/**
	 * Notifies the implementation of <em>this</em> interface that <code>dirPath</code>
	 * should be monitored for file system events. If the changed file matches any
	 * of the <code>globPatterns</code>, <code>listener</code> should be notified.
	 *
	 * @param dirPath	The directory path.
	 * @throws IOException	If <code>dirPath</code> is not a directory.
	 */
	void register(String dirPath) throws IOException;

}
