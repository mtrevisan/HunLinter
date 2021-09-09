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

import io.github.mtrevisan.hunlinter.gui.dialogs.HelpDialog;
import io.github.mtrevisan.hunlinter.services.semanticversioning.Version;
import io.github.mtrevisan.hunlinter.services.system.PropertiesUTF8;
import io.github.mtrevisan.hunlinter.services.text.StringHelper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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


public final class DownloaderHelper{

	private static final String ALREADY_UPDATED = "You already have the latest version installed";

	private static final String URL_CONNECTIVITY = "https://www.google.com/";
	private static final String URL_ONLINE_REPOSITORY_BASE = "https://api.github.com/repos/mtrevisan/HunLinter/";
	private static final String URL_ONLINE_REPOSITORY_RELEASES = "releases";
	private static final String URL_ONLINE_REPOSITORY_CONTENTS_APP = "contents/bin/";

	private static final Comparator<Pair<Version, String>> VERSION_COMPARATOR;
	static{
		final Comparator<Pair<Version, String>> cmp = Comparator.comparing(Pair::getKey);
		VERSION_COMPARATOR = cmp.reversed();
	}


	private static final String PROPERTY_KEY_TAG_NAME = "tag_name";
	private static final String PROPERTY_KEY_WHATS_NEW = "body";
	private static final String PROPERTY_KEY_FILENAME = "name";
	public static final String PROPERTY_KEY_ARTIFACT_ID = "artifactId";
	public static final String PROPERTY_KEY_VERSION = "version";
	public static final String PROPERTY_KEY_BUILD_TIMESTAMP = "buildTimestamp";

	private static final String DEFAULT_PACKAGING_EXTENSION = ".jar";
	private static final String DEFAULT_EXECUTABLE_EXTENSION = ".exe";

	public static final Map<String, Object> APPLICATION_PROPERTIES = new HashMap<>(3);
	static{
		try(final InputStreamReader is = new InputStreamReader(HelpDialog.class.getResourceAsStream("/version.properties"), StandardCharsets.UTF_8)){
			final PropertiesUTF8 prop = new PropertiesUTF8();
			prop.load(is);

			APPLICATION_PROPERTIES.put(PROPERTY_KEY_ARTIFACT_ID, prop.getProperty(PROPERTY_KEY_ARTIFACT_ID));
			APPLICATION_PROPERTIES.put(PROPERTY_KEY_VERSION, prop.getProperty(PROPERTY_KEY_VERSION));
			APPLICATION_PROPERTIES.put(PROPERTY_KEY_BUILD_TIMESTAMP, LocalDate.parse(prop.getProperty(PROPERTY_KEY_BUILD_TIMESTAMP)));
		}
		catch(final IOException ignored){}
	}


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
	 * @throws VersionException	If something went wrong, or current version is already the last one
	 */
	public static List<Pair<Version, String>> extractNewerVersions() throws VersionException, IOException, ParseException{
		try(final InputStream is = new URL(URL_ONLINE_REPOSITORY_BASE + URL_ONLINE_REPOSITORY_RELEASES).openStream()){
			final String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);

			final JSONParser parser = new JSONParser();
			final JSONArray jsonArray = (JSONArray)parser.parse(response);
			final Version applicationVersion = new Version((String)APPLICATION_PROPERTIES.get(PROPERTY_KEY_VERSION));
			final List<Pair<Version, String>> whatsNew = new ArrayList<>(jsonArray.size());
			for(final Object elem : jsonArray){
				final JSONObject obj = (JSONObject)elem;
				final Version tagName = new Version((String)obj.get(PROPERTY_KEY_TAG_NAME));
				if(tagName.isGreaterThan(applicationVersion))
					whatsNew.add(Pair.of(tagName, (String)obj.get(PROPERTY_KEY_WHATS_NEW)));
			}

			if(whatsNew.isEmpty())
				throw new VersionException(ALREADY_UPDATED);

			whatsNew.sort(VERSION_COMPARATOR);
			return whatsNew;
		}
	}

	public static GITFileData extractVersionData(final Version version) throws VersionException, IOException, ParseException{
		//find last build by filename
		final String filename = "-" + version;
		try(final InputStream is = new URL(URL_ONLINE_REPOSITORY_BASE + URL_ONLINE_REPOSITORY_CONTENTS_APP).openStream()){
			final byte[] dataBytes = is.readAllBytes();
			final GITFileData fileData = extractData(filename, dataBytes);

			if(fileData == null)
				throw new VersionException(ALREADY_UPDATED);

			fileData.version = version;
			return fileData;
		}
	}

	public static byte[] readFileContent(final String localPath) throws IOException{
		try(final FileInputStream fis = new FileInputStream(localPath)){
			return IOUtils.toByteArray(fis);
		}
	}

	public static void validate(final byte[] content, final GITFileData object) throws DownloadException, NoSuchAlgorithmException{
		if(object.size != null && content.length != object.size)
			throw new DownloadException("Size mismatch while downloading {}, expected {} B, had {} B", object.name, object.size,
				content.length);

		final String downloadedSha = calculateGitSha1(content);
		if(!downloadedSha.equals(object.sha))
			throw new DownloadException("SHA mismatch while downloading {}", object.name);
	}

	private static GITFileData extractData(final String filename, final byte[] directoryContent) throws ParseException{
		final String content = new String(directoryContent, StandardCharsets.UTF_8);
		final JSONParser parser = new JSONParser();
		final JSONArray array = (JSONArray)parser.parse(content);
		for(final Object elem : array){
			final String repositoryFilename = (String)((JSONObject)elem).get(PROPERTY_KEY_FILENAME);
			//either a jar or an exe
			if(repositoryFilename.endsWith(filename + DEFAULT_PACKAGING_EXTENSION)
					|| repositoryFilename.endsWith(filename + DEFAULT_EXECUTABLE_EXTENSION))
				return new GITFileData((JSONObject)elem);
		}
		return null;
	}

	private static String calculateGitSha1(final byte[] content) throws NoSuchAlgorithmException{
		final MessageDigest digest = MessageDigest.getInstance("SHA-1");
		final byte[] header = ("blob " + content.length + "\0").getBytes(StandardCharsets.UTF_8);
		return StringHelper.byteArrayToHexString(digest.digest(ArrayUtils.addAll(header, content)));
	}

}
