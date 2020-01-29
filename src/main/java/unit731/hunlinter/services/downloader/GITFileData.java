package unit731.hunlinter.services.downloader;

import com.github.zafarkhaja.semver.Version;
import org.json.simple.JSONObject;


public class GITFileData{

	public String name;
	public Version version;
	public Long size;
	public String sha;
	public String content;
	public String encoding;
	public String downloadUrl;


	GITFileData(final JSONObject jsonObject){
		name = (String)jsonObject.get("name");
		size = (Long)jsonObject.get("size");
		sha = (String)jsonObject.get("sha");
		content = (String)jsonObject.get("content");
		encoding = (String)jsonObject.get("encoding");
		downloadUrl = (String)jsonObject.get("download_url");
	}

}
