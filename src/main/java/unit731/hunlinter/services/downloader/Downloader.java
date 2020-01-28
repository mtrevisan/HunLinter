package unit731.hunlinter.services.downloader;

import com.github.zafarkhaja.semver.Version;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import unit731.hunlinter.HelpDialog;
import unit731.hunlinter.services.PatternHelper;
import unit731.hunlinter.services.StringHelper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Downloader{

	private static final Pattern VERSION = PatternHelper.pattern("-([\\d.]+)+\\.jar$");


	private Downloader(){}

	public static JSONObject download(final String json) throws Exception{
		final Pair<Version, JSONObject> newest = extractNewest(json);
		final Version lastObjectVersion = newest.getLeft();
		final JSONObject lastObject = newest.getRight();

		if(lastObjectVersion == null)
			throw new Exception("No versions available");

		//get actual version
		final Version actualVersion = getActualVersion();

		if(lastObjectVersion.lessThanOrEqualTo(actualVersion))
			throw new Exception("No newer versions available");

		return lastObject;
	}

	public static void validate(final String saveFilePath, final JSONObject object) throws Exception{
		//check size + sha
		final FileInputStream fis = new FileInputStream(saveFilePath);
		final byte[] content = IOUtils.toByteArray(fis);
		final Long size = (Long)object.getOrDefault("size", null);
		if(content.length != size)
			throw new Exception("Size mismatch while downloading " + FilenameUtils.getBaseName(saveFilePath) + ", expected " + size + " B, had "
				+ content.length + " B");

		final String downloadedSha = calculateGitSha1(content);
		final String sha = (String)object.getOrDefault("sha", null);
		if(!downloadedSha.equals(sha))
			throw new Exception("SHA mismatch while downloading " + FilenameUtils.getBaseName(saveFilePath));
	}

	private static Version getActualVersion() throws IOException{
		try(final InputStream versionInfoStream = HelpDialog.class.getResourceAsStream("/version.properties")){
			final Properties prop = new Properties();
			prop.load(versionInfoStream);
			return Version.valueOf(prop.getProperty("version"));
		}
	}

	private static Pair<Version, JSONObject> extractNewest(final String json){
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

	private static String calculateGitSha1(final byte[] content) throws NoSuchAlgorithmException{
		final MessageDigest digest = MessageDigest.getInstance("SHA-1");
		final byte[] header = ("blob " + content.length + "\0").getBytes(StandardCharsets.UTF_8);
		return StringHelper.byteArrayToHexString(digest.digest(ArrayUtils.addAll(header, content)));
	}

}
