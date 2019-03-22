package unit731.hunspeller.parsers.aid;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import unit731.hunspeller.services.FileHelper;


public class AidParser{

	private final List<String> lines = new ArrayList<>();


	/**
	 * Parse the rows out from a .aid file.
	 *
	 * @param aidFile	The content of the dictionary file
	 * @throws IOException	If an I/O error occurse
	 */
	public void parse(File aidFile) throws IOException{
		lines.clear();

		Path path = aidFile.toPath();
		Charset charset = FileHelper.determineCharset(path);
		try(LineNumberReader br = FileHelper.createReader(path, charset)){
			String line;
			while((line = br.readLine()) != null)
				if(!line.isEmpty())
					lines.add(line);
		}
	}

	public void clear(){
		lines.clear();
	}

	public List<String> getLines(){
		return lines;
	}

}
