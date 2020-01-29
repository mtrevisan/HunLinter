package unit731.hunlinter.services.downloader;


public interface DownloadListenerInterface{

	/**
	 * Called just before starting to check for updates
	 *
	 * @throws Exception	If an error should be raised
	 */
	default void startCheckUpdates() throws Exception{}

	/**
	 * All files were passed for an update check, start download
	 *
	 * @param fileData	The data of the file about to be downloaded
	 * @throws Exception	If an error should be raised
	 */
	default void startDownloads(final GITFileData fileData) throws Exception{}

	/**
	 * The file was successfully downloaded and is now about to be passed through a series of validations.
	 *
	 * <p>
	 * You can do your own validations here using the {@code saveFilePath} to read the
	 * actual file. Throw an exception to fail the download in case of validation fail.
	 *
	 * @param fileData	The data of the file about to be downloaded
	 * @param localPath	Local path of downloaded file
	 * @throws Exception	If an error should be raised
	 */
	default void validatingFile(final GITFileData fileData, final String localPath) throws Exception{}

	/** Called when the user blocks the downloading process */
	default void stopped(){}

	/** Called when the update process is complete */
	default void succeeded(){}

	/**
	 * Called when the update process failed
	 *
	 * @param e	Exception that caused the failure
	 */
	default void failed(final Exception e){}

}
