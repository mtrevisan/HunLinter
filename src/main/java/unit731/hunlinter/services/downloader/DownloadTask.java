package unit731.hunlinter.services.downloader;

import javax.swing.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/** Execute file download in a background thread and update the progress */
public class DownloadTask extends SwingWorker<Void, Void>{

	private static final int BUFFER_SIZE = 4096;

	private final String saveFilePath;
	private final DownloadListenerInterface listener;

	private final HttpURLConnection httpConnection;


	public DownloadTask(final String downloadURL, final String saveFilePath, final DownloadListenerInterface listener) throws IOException{
		this.saveFilePath = saveFilePath;
		this.listener = listener;

		final URL url = new URL(downloadURL);
		httpConnection = (HttpURLConnection)url.openConnection();
		final int responseCode = httpConnection.getResponseCode();
		if(responseCode != HttpURLConnection.HTTP_OK)
			throw new IOException("Cannot connect to server");
	}

	@Override
	protected Void doInBackground() throws Exception{
		try{
			final int contentLength = httpConnection.getContentLength();
			final InputStream is = httpConnection.getInputStream();

			final FileOutputStream outputStream = new FileOutputStream(saveFilePath);

			final byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead;
			long totalBytesRead = 0;
			while((bytesRead = is.read(buffer)) != -1){
				outputStream.write(buffer, 0, bytesRead);
				totalBytesRead += bytesRead;

				setProgress((int)(totalBytesRead * 100 / contentLength));
			}

			outputStream.close();

			is.close();
			httpConnection.disconnect();
		}
		catch(final Exception e){
			listener.error(e);

			cancel(true);
		}
		return null;
	}

	@Override
	protected void done(){
		setProgress(100);

		if(!isCancelled())
			listener.success(saveFilePath);
	}

}
