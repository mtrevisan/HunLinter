package unit731.hunlinter.services.downloader;


public interface DownloadListenerInterface{

	void success(final String saveFilePath);

	void error(Exception e);

}
