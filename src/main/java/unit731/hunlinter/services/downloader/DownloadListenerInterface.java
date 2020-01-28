package unit731.hunlinter.services.downloader;


public interface DownloadListenerInterface{

	/** Called just before starting to check for updates */
	default void startCheckUpdates() throws Exception{}

	/** All files were passed for an update check, start download */
	default void startDownloads(final String version) throws Exception{}

	/**
	 * The file was successfully downloaded and is now about to be passed through a series of validations.
	 *
	 * <p>
	 * You can do your own validations here using the {@code saveFilePath} to read the
	 * actual file. Throw an exception to fail the download in case of validation fail.
	 */
	default void validatingFile(final String localPath) throws Exception{}

	/** Called when the user blocks the downloading process */
	default void stopped(){}

	/** Called when the update process is complete */
	default void succeeded(){}

	/** Called when the update process failed */
	default void failed(final Exception e){}

}
