package unit731.hunlinter.services;

import com.github.zafarkhaja.semver.Version;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Downloader{

	private static final Pattern VERSION = PatternHelper.pattern("-([\\d.]+)+\\.jar$");


	private Downloader(){}

	public static Pair<Version, JSONObject> getNewest(final String json){
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
			final Version currentVersion = Version.valueOf(m.group(1));
			if(lastObjectVersion == null || currentVersion.greaterThan(lastObjectVersion)){
				lastObjectVersion = currentVersion;
				lastObject = el;
			}
		}
		return Pair.of(lastObjectVersion, lastObject);
	}

	public static String downloadFile(final String downloadUrl, final String filename) throws IOException{
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

	public static String calculateGitSha1(final byte[] content) throws NoSuchAlgorithmException{
		final MessageDigest digest = MessageDigest.getInstance("SHA-1");
		final byte[] header = ("blob " + content.length + "\0").getBytes(StandardCharsets.UTF_8);
		return StringHelper.byteArrayToHexString(digest.digest(ArrayUtils.addAll(header, content)));
	}

}
