/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package unit731.hunlinter.services.downloader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import unit731.hunlinter.gui.dialogs.HelpDialog;
import unit731.hunlinter.services.semanticversioning.Version;
import unit731.hunlinter.services.text.StringHelper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class DownloaderHelper{

	private static final String ALREADY_UPDATED = "You already have the latest version installed";

	private static final String URL_CONNECTIVITY = "https://www.google.com/";
	private static final String URL_ONLINE_REPOSITORY_BASE = "https://api.github.com/repos/mtrevisan/HunLinter/";
	private static final String URL_ONLINE_REPOSITORY_RELEASES = "releases";
	private static final String URL_ONLINE_REPOSITORY_CONTENTS_APP = "contents/bin/";

	@SuppressWarnings("CanBeFinal")
	private static Comparator<Pair<Version, String>> VERSION_COMPARATOR = Comparator.comparing(Pair::getKey);
	static{
		VERSION_COMPARATOR = VERSION_COMPARATOR.reversed();
	}


	public static final String PROPERTY_KEY_TAG_NAME = "tag_name";
	public static final String PROPERTY_KEY_WHATS_NEW = "body";
	public static final String PROPERTY_KEY_FILENAME = "name";
	public static final String PROPERTY_KEY_ARTIFACT_ID = "artifactId";
	public static final String PROPERTY_KEY_VERSION = "version";
	public static final String PROPERTY_KEY_BUILD_TIMESTAMP = "buildTimestamp";

	private static final String DEFAULT_PACKAGING_EXTENSION = ".jar";

	private static Map<String, Object> APPLICATION_PROPERTIES;


	private DownloaderHelper(){}

	public static boolean hasInternetConnectivity(){
		try{
			final URL url = new URL(URL_CONNECTIVITY);
			final HttpURLConnection httpConnection = (HttpURLConnection)url.openConnection();
			final int responseCode = httpConnection.getResponseCode();
			return (responseCode == HttpURLConnection.HTTP_OK);
		}
		catch(final Exception e){
			return false;
		}
	}

	/**
	 * Extracts a list of version and whats-news
	 *
	 * @return	A list of pairs version-release-notes
	 * @throws Exception	If something went wrong, or current version is already the last one
	 */
	public static List<Pair<Version, String>> extractNewerVersions() throws Exception{
		try(final InputStream is = new URL(URL_ONLINE_REPOSITORY_BASE + URL_ONLINE_REPOSITORY_RELEASES).openStream()){
			final String response = new String(is.readAllBytes());

			final JSONParser parser = new JSONParser();
			final JSONArray jsonArray = (JSONArray)parser.parse(response);
			final Version applicationVersion = new Version((String)getApplicationProperties().get(DownloaderHelper.PROPERTY_KEY_VERSION));
			final List<Pair<Version, String>> whatsNew = new ArrayList<>();
			for(final Object elem : jsonArray){
				final JSONObject obj = (JSONObject)elem;
				final Version tagName = new Version((String)obj.get(PROPERTY_KEY_TAG_NAME));
				if(tagName.greaterThan(applicationVersion))
					whatsNew.add(Pair.of(tagName, (String)obj.get(PROPERTY_KEY_WHATS_NEW)));
			}

			if(whatsNew.isEmpty())
				throw new Exception(ALREADY_UPDATED);

			whatsNew.sort(VERSION_COMPARATOR);
			return whatsNew;
		}
	}

	public static GITFileData extractVersionData(final Version version) throws Exception{
		//find last build by filename
		final String filename = "-" + version + DEFAULT_PACKAGING_EXTENSION;
		try(final InputStream is = new URL(URL_ONLINE_REPOSITORY_BASE + URL_ONLINE_REPOSITORY_CONTENTS_APP).openStream()){
			final byte[] dataBytes = is.readAllBytes();
			final GITFileData fileData = extractData(filename, dataBytes);

			if(fileData == null)
				throw new Exception(ALREADY_UPDATED);

			fileData.version = version;
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

	public static Map<String, Object> getApplicationProperties(){
		if(APPLICATION_PROPERTIES == null){
			APPLICATION_PROPERTIES = new HashMap<>();
			try(final InputStreamReader is = new InputStreamReader(HelpDialog.class.getResourceAsStream("/version.properties"), StandardCharsets.UTF_8)){
				final Properties prop = new Properties();
				prop.load(is);

				APPLICATION_PROPERTIES.put(PROPERTY_KEY_ARTIFACT_ID, prop.getProperty(PROPERTY_KEY_ARTIFACT_ID));
				APPLICATION_PROPERTIES.put(PROPERTY_KEY_VERSION, prop.getProperty(PROPERTY_KEY_VERSION));
				APPLICATION_PROPERTIES.put(PROPERTY_KEY_BUILD_TIMESTAMP, LocalDate.parse(prop.getProperty(PROPERTY_KEY_BUILD_TIMESTAMP)));
			}
			catch(final IOException ignored){}
		}
		return APPLICATION_PROPERTIES;
	}

	private static GITFileData extractData(final String filename, final byte[] directoryContent) throws ParseException{
		final String content = new String(directoryContent, StandardCharsets.UTF_8);
		final JSONParser parser = new JSONParser();
		final JSONArray array = (JSONArray)parser.parse(content);
		for(final Object elem : array)
			if(((String)((JSONObject)elem).get(PROPERTY_KEY_FILENAME)).endsWith(filename))
				return new GITFileData((JSONObject)elem);
		return null;
	}

	private static String calculateGitSha1(final byte[] content) throws NoSuchAlgorithmException{
		final MessageDigest digest = MessageDigest.getInstance("SHA-1");
		final byte[] header = ("blob " + content.length + "\0").getBytes(StandardCharsets.UTF_8);
		return StringHelper.byteArrayToHexString(digest.digest(ArrayUtils.addAll(header, content)));
	}

}
