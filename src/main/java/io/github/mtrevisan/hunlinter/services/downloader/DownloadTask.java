/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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

import javax.swing.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;


/** Execute file download in a background thread and update the progress. */
public class DownloadTask extends SwingWorker<Void, Void> implements RBCWrapperDelegate{

	private final String localPath;
	private final GITFileData remoteObject;
	private final DownloadListenerInterface listener;


	public DownloadTask(final String localPath, final GITFileData remoteObject, final DownloadListenerInterface listener){
		this.localPath = localPath;
		this.remoteObject = remoteObject;
		this.listener = listener;
	}

	@Override
	protected final Void doInBackground(){
		try(
				final FileOutputStream fos = new FileOutputStream(localPath);
				final FileChannel fileChannel = fos.getChannel()){
			listener.startCheckUpdates();

			if(!DownloaderHelper.hasInternetConnectivity())
				throw new IOException("Cannot connect to server");

			listener.startDownloads(remoteObject);

			final URL url = new URL(remoteObject.downloadUrl);
			final ReadableByteChannel rbc = new RBCWrapper(Channels.newChannel(url.openStream()), contentLength(url), this);
			fileChannel.transferFrom(rbc, 0, Long.MAX_VALUE);
		}
		catch(@SuppressWarnings("OverlyBroadCatchBlock") final IOException e){
			listener.failed(e);

			cancel(true);
		}
		return null;
	}

	private static int contentLength(final URL url){
		int contentLength = -1;
		try{
			HttpURLConnection.setFollowRedirects(false);

			final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("HEAD");

			contentLength = connection.getContentLength();
		}
		catch(@SuppressWarnings("OverlyBroadCatchBlock") final IOException ignored){}
		return contentLength;
	}

	public final void cancelTask(){
		listener.stopped();

		cancel(true);
	}

	@Override
	public final void rbcProgressCallback(final RBCWrapper rbc, final double progress){
		setProgress(Math.min((int)Math.round(progress), 100));
	}

	@Override
	protected final void done(){
		if(!isCancelled()){
			try{
				listener.validatingFile(remoteObject, localPath);

				listener.succeeded();
			}
			catch(final RuntimeException e){
				listener.failed(e);
			}
		}
	}

}
