package unit731.hunlinter.services.downloader;

import com.github.zafarkhaja.semver.Version;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;


public class DownloaderHelper{

	private static final String ALREADY_UPDATED = "You already have the latest version installed";

	private static final String URL_ONLINE_REPOSITORY_BASE = "https://api.github.com/repos/mtrevisan/HunLinter/contents/";
	private static final String URL_ONLINE_REPOSITORY_POM = "pom.xml";
	private static final String URL_ONLINE_REPOSITORY_APP = "bin/";


	public static final String PROPERTY_KEY_ARTIFACT_ID = "artifactId";
	public static final String PROPERTY_KEY_VERSION = "version";
	public static final String PROPERTY_KEY_BUILD_TIMESTAMP = "buildTimestamp";

	private static final Pattern PATTERN_ARTIFACT_ID_POM = PatternHelper.pattern("<artifactId>(.+?)</artifactId>");
	private static final Pattern PATTERN_VERSION_POM = PatternHelper.pattern("<version>(.+?)</version>");
	private static final Pattern PATTERN_PACKAGING_POM = PatternHelper.pattern("<packaging>(.+?)</packaging>");

	private static Map<String, Object> POM_PROPERTIES;


	private DownloaderHelper(){}

	public static GITFileData extractLastVersion() throws Exception{
		try(final InputStream is = new URL(URL_ONLINE_REPOSITORY_BASE + URL_ONLINE_REPOSITORY_POM).openStream()){
			final String response = new String(is.readAllBytes());

			final JSONParser parser = new JSONParser();
			final JSONObject jsonObject = (JSONObject)parser.parse(response);
			final GITFileData pomData = new GITFileData(jsonObject);

			final byte[] dataBytes = Base64.getMimeDecoder().decode(pomData.content);
			pomData.content = new String(dataBytes, StandardCharsets.UTF_8.name());

			validate(dataBytes, pomData);

			final String version = PatternHelper.extract(pomData.content, PATTERN_VERSION_POM)[0];
			pomData.version = Version.valueOf(version);

			//get actual version
			final Version applicationVersion = Version.valueOf((String)getPOMProperties().get(DownloaderHelper.PROPERTY_KEY_VERSION));
			if(pomData.version.lessThanOrEqualTo(applicationVersion))
				throw new Exception(ALREADY_UPDATED);

			//find last build
			GITFileData fileData;
			final String artifactID = PatternHelper.extract(pomData.content, PATTERN_ARTIFACT_ID_POM)[0];
			final String packaging = PatternHelper.extract(pomData.content, PATTERN_PACKAGING_POM)[0];
			final String name = artifactID + "-" + pomData.version + "." + packaging;
			try(final InputStream isApps = new URL(URL_ONLINE_REPOSITORY_BASE + URL_ONLINE_REPOSITORY_APP).openStream()){
				fileData = extractNewest(name, isApps.readAllBytes());
			}

			if(fileData == null)
				throw new Exception(ALREADY_UPDATED);

			fileData.version = Version.valueOf(version);
			return fileData;
		}
	}

	public static byte[] readFileContent(final String localPath) throws IOException{
		try(final FileInputStream fis = new FileInputStream(localPath)){
			return IOUtils.toByteArray(fis);
		}
	}

	public static void validate(final byte[] content, final GITFileData object) throws Exception{
		if(content.length != object.size)
			throw new Exception("Size mismatch while downloading " + object.name + ", expected " + object.size + " B, had " + content.length + " B");

		final String downloadedSha = calculateGitSha1(content);
		if(!downloadedSha.equals(object.sha))
			throw new Exception("SHA mismatch while downloading " + object.name);
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

	private static GITFileData extractNewest(final String name, final byte[] directoryContent) throws ParseException{
		final String content = new String(directoryContent, StandardCharsets.UTF_8);
		final JSONParser parser = new JSONParser();
		final JSONArray array = (JSONArray)parser.parse(content);
		for(final Object elem : array)
			if(name.equals(((JSONObject)elem).get("name")))
				return new GITFileData((JSONObject)elem);
		return null;
	}

	private static String calculateGitSha1(final byte[] content) throws NoSuchAlgorithmException{
		final MessageDigest digest = MessageDigest.getInstance("SHA-1");
		final byte[] header = ("blob " + content.length + "\0").getBytes(StandardCharsets.UTF_8);
		return StringHelper.byteArrayToHexString(digest.digest(ArrayUtils.addAll(header, content)));
	}

}
