package unit731.hunlinter.services.downloader;

import org.json.simple.JSONObject;
import unit731.hunlinter.services.semanticversioning.Version;


public class GITFileData{

	public final String name;
	public Version version;
	public final Long size;
	public final String sha;
	public String content;
	public final String encoding;
	public final String downloadUrl;


	GITFileData(final JSONObject jsonObject){
		name = (String)jsonObject.get("name");
		size = (Long)jsonObject.get("size");
		sha = (String)jsonObject.get("sha");
		content = (String)jsonObject.get("content");
		encoding = (String)jsonObject.get("encoding");
		downloadUrl = (String)jsonObject.get("download_url");
	}

}
