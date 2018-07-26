package unit731.hunspeller.parsers.aid;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import unit731.hunspeller.services.FileService;


@Getter
public class AidParser{

	private final List<String> lines = new ArrayList<>();


	/**
	 * Parse the rows out from a .aid file.
	 *
	 * @param aidFile	The content of the dictionary file
	 * @throws IOException	If an I/O error occurse
	 */
	public void parse(File aidFile) throws IOException{
		clear();

		Charset charset = FileService.determineCharset(aidFile.toPath());
		try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(aidFile.toPath(), charset))){
			String line;
			while(Objects.nonNull(line = br.readLine())){
				//ignore any BOM marker on first line
				if(br.getLineNumber() == 1)
					line = FileService.clearBOMMarker(line);

				if(!line.isEmpty())
					lines.add(line);
			}
		}
	}

	public void clear(){
		lines.clear();
	}

}
