/**
 * Copyright (c) 2019-2022 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.services.downloader;


public interface DownloadListenerInterface{

	/**
	 * Called just before starting to check for updates
	 *
	 */
	default void startCheckUpdates(){}

	/**
	 * All files were passed for an update check, start download
	 *
	 * @param fileData	The data of the file about to be downloaded
	 */
	default void startDownloads(final GITFileData fileData){}

	/**
	 * The file was successfully downloaded and is now about to be passed through a series of validations.
	 *
	 * <p>
	 * You can do your own validations here using the {@code saveFilePath} to read the
	 * actual file. Throw an exception to fail the download in case of validation fail.
	 *
	 * @param fileData	The data of the file about to be downloaded
	 * @param localPath	Local path of downloaded file
	 */
	default void validatingFile(final GITFileData fileData, final String localPath){}

	/** Called when the user blocks the downloading process. */
	default void stopped(){}

	/** Called when the update process is complete. */
	default void succeeded(){}

	/**
	 * Called when the update process failed.
	 *
	 * @param e	Exception that caused the failure.
	 */
	default void failed(final Exception e){}

}
