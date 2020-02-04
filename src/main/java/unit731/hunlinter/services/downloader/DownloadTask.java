package unit731.hunlinter.services.downloader;

import javax.swing.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;


/** Execute file download in a background thread and update the progress */
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
	protected Void doInBackground() throws Exception{
		try{
			listener.startCheckUpdates();

			if(!DownloaderHelper.hasInternetConnectivity())
				throw new IOException("Cannot connect to server");

			listener.startDownloads(remoteObject);

			final URL url = new URL(remoteObject.downloadUrl);
			final ReadableByteChannel rbc = new RBCWrapper(Channels.newChannel(url.openStream()), contentLength(url), this);
			final FileOutputStream fos = new FileOutputStream(localPath);
			final FileChannel fileChannel = fos.getChannel();
			fileChannel.transferFrom(rbc, 0, Long.MAX_VALUE);
			fileChannel.close();
			fos.close();
		}
		catch(final Exception e){
			cancel(true);

			listener.failed(e);
		}
		return null;
	}

	private int contentLength(final URL url){
		int contentLength = -1;
		try{
			HttpURLConnection.setFollowRedirects(false);

			final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("HEAD");

			contentLength = connection.getContentLength();
		}
		catch(final Exception ignored){}
		return contentLength;
	}

	public void cancelTask(){
		cancel(true);

		listener.stopped();
	}

	@Override
	public void rbcProgressCallback(final RBCWrapper rbc, final double progress){
		setProgress((int)Math.round(progress));
	}

	@Override
	protected void done(){
		setProgress(100);

		if(!isCancelled()){
			try{
				listener.validatingFile(remoteObject, localPath);

				listener.succeeded();
			}
			catch(final Exception e){
				listener.failed(e);
			}
		}
	}

}
