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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DownloaderHelper{

	public static final String PROPERTY_KEY_ARTIFACT_ID = "artifactId";
	public static final String PROPERTY_KEY_VERSION = "version";
	public static final String PROPERTY_KEY_BUILD_TIMESTAMP = "buildTimestamp";

	private static final Pattern VERSION = PatternHelper.pattern("-([\\d.]+)+\\.jar$");

	private static Map<String, Object> POM_PROPERTIES;


	private DownloaderHelper(){}

	public static JSONObject extractLastVersion(final String url) throws Exception{
		//TODO read pom.xml instead of whole directory?
		try(final InputStream is = new URL(url).openStream()){
			final String response = new String(is.readAllBytes());

			final Pair<Version, JSONObject> newest = extractNewest(response);
			final Version lastObjectVersion = newest.getLeft();
			final JSONObject lastObject = newest.getRight();

			//get actual version
			final Version applicationVersion = Version.valueOf((String)getPOMProperties().get(DownloaderHelper.PROPERTY_KEY_VERSION));
//			if(lastObjectVersion.lessThanOrEqualTo(applicationVersion))
//				throw new Exception("You already have the latest version installed");

			return lastObject;
		}
	}

	public static String extractVersion(final String filename){
		final Matcher m = VERSION.matcher(FilenameUtils.getBaseName(filename) + "." + FilenameUtils.getExtension(filename));
		return (m.find()? m.group(1): null);
	}

	public static void validate(final String localPath, final JSONObject object) throws Exception{
		final byte[] content;
		try(final FileInputStream fis = new FileInputStream(localPath)){
			content = IOUtils.toByteArray(fis);
		}
		final Long size = (Long)object.getOrDefault("size", null);
		if(content.length != size)
			throw new Exception("Size mismatch while downloading " + FilenameUtils.getBaseName(localPath) + ", expected "
				+ size + " B, had " + content.length + " B");

		final String downloadedSha = calculateGitSha1(content);
		final String sha = (String)object.getOrDefault("sha", null);
		if(!downloadedSha.equals(sha))
			throw new Exception("SHA mismatch while downloading " + FilenameUtils.getBaseName(localPath));
	}

	public static Map<String, Object> getPOMProperties(){
		if(POM_PROPERTIES == null){
			POM_PROPERTIES = new HashMap<>();
			try(final InputStream versionInfoStream = HelpDialog.class.getResourceAsStream("/version.properties")){
				final Properties prop = new Properties();
				prop.load(versionInfoStream);

				POM_PROPERTIES.put(PROPERTY_KEY_ARTIFACT_ID, prop.getProperty(PROPERTY_KEY_ARTIFACT_ID));
				POM_PROPERTIES.put(PROPERTY_KEY_VERSION, prop.getProperty(PROPERTY_KEY_VERSION));
				POM_PROPERTIES.put(PROPERTY_KEY_BUILD_TIMESTAMP, LocalDate.parse(prop.getProperty(PROPERTY_KEY_BUILD_TIMESTAMP)));
			}
			catch(final IOException ignored){}
		}
		return POM_PROPERTIES;
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
