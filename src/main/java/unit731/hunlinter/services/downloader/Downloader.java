package unit731.hunlinter.services.downloader;

import com.github.zafarkhaja.semver.Version;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import unit731.hunlinter.FileDownloaderDialog;
import unit731.hunlinter.HelpDialog;
import unit731.hunlinter.services.PatternHelper;
import unit731.hunlinter.services.StringHelper;

import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Downloader implements DownloadListenerInterface{

	private static final Pattern VERSION = PatternHelper.pattern("-([\\d.]+)+\\.jar$");

	private Frame parent;
	private JSONObject lastObject;


	public Downloader(final Frame parent){
		Objects.requireNonNull(parent);

		this.parent = parent;
	}

	public void download(final String json){
		try{
			final Pair<Version, JSONObject> newest = extractNewest(json);
			final Version lastObjectVersion = newest.getLeft();
			lastObject = newest.getRight();

			if(lastObjectVersion == null)
				throw new Exception("No versions available");

			//get actual version
			final Version actualVersion = getActualVersion();

			if(true || lastObjectVersion.greaterThan(actualVersion)){
				//warn for newer version and ask for download
				//TODO

				//download file
				final String downloadURL = (String)lastObject.getOrDefault("download_url", null);
				final String filename = (String)lastObject.getOrDefault("name", null);
				final String downloadLocation = downloadFile(downloadURL, filename);
				SwingUtilities.invokeLater(() -> {
					final FileDownloaderDialog dialog = new FileDownloaderDialog(downloadURL, downloadLocation, this, parent);
					dialog.setLocationRelativeTo(parent);
					dialog.setVisible(true);
				});
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void success(final String saveFilePath){
		//check size + sha
		try{
			final FileInputStream fis = new FileInputStream(saveFilePath);
			final byte[] content = IOUtils.toByteArray(fis);
			final Long size = (Long)lastObject.getOrDefault("size", null);
			if(content.length != size)
				throw new Exception("Size mismatch while downloading " + FilenameUtils.getBaseName(saveFilePath) + ", expected " + size + " B, had "
					+ content.length + " B");

			final String downloadedSha = calculateGitSha1(content);
			final String sha = (String)lastObject.getOrDefault("sha", null);
			if(!downloadedSha.equals(sha))
				throw new Exception("SHA mismatch while downloading " + FilenameUtils.getBaseName(saveFilePath));

			//TODO
			JOptionPane.showMessageDialog(parent, "File has been downloaded successfully!", "Message", JOptionPane.INFORMATION_MESSAGE);
		}
		catch(final Exception e){
			error(e);
		}
	}

	@Override
	public void error(final Exception e){
		//TODO
		JOptionPane.showMessageDialog(parent, "Error downloading file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
	}

	private Version getActualVersion() throws IOException{
		try(final InputStream versionInfoStream = HelpDialog.class.getResourceAsStream("/version.properties")){
			final Properties prop = new Properties();
			prop.load(versionInfoStream);
			return Version.valueOf(prop.getProperty("version"));
		}
	}

	private Pair<Version, JSONObject> extractNewest(final String json){
		Version lastObjectVersion = null;
		JSONObject lastObject = null;
		final JSONArray array = (JSONArray) JSONValue.parse(json);
		for(final Object elem : array){
			final JSONObject el = (JSONObject)elem;

			//filter for "type" equals "file"
			final String type = (String)el.getOrDefault("type", null);
			if(!"file".equals(type))
				continue;

			//filter for "name" endsWith "-{version}.jar"
			final String name = (String)el.getOrDefault("name", null);
			final Matcher m = VERSION.matcher(name);
			if(!m.find())
				continue;

			//extract version
			final Version version = Version.valueOf(m.group(1));

			//update newest object
			if(lastObjectVersion == null || version.greaterThan(lastObjectVersion)){
				lastObjectVersion = version;
				lastObject = el;
			}
		}
		return Pair.of(lastObjectVersion, lastObject);
	}

	private String downloadFile(final String downloadUrl, final String filename) throws IOException{
		final ReadableByteChannel rbc = Channels.newChannel(new URL(downloadUrl).openStream());
		final String homeFolder = System.getProperty("user.home");
		final String downloadLocation = homeFolder + "/Downloads/" + filename;
		final FileOutputStream fos = new FileOutputStream(downloadLocation);
		final FileChannel fileChannel = fos.getChannel();
		fileChannel.transferFrom(rbc, 0, Long.MAX_VALUE);
		fileChannel.close();
		fos.close();
		return downloadLocation;
	}

	private String calculateGitSha1(final byte[] content) throws NoSuchAlgorithmException{
		final MessageDigest digest = MessageDigest.getInstance("SHA-1");
		final byte[] header = ("blob " + content.length + "\0").getBytes(StandardCharsets.UTF_8);
		return StringHelper.byteArrayToHexString(digest.digest(ArrayUtils.addAll(header, content)));
	}

}
