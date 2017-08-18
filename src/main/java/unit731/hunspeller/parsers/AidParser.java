package unit731.hunspeller.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;


public class AidParser{

	//FEFF because this is the Unicode char represented by the UTF-8 byte order mark (EF BB BF)
	private static final String BOM_MARKER = "\uFEFF";


	@Getter private final List<String> lines = new ArrayList<>();


	/**
	 * Parse the rows out from a .aid file.
	 *
	 * @param aidFile	The content of the dictionary file
	 * @throws IOException	If an I/O error occurse
	 */
	public void parse(File aidFile) throws IOException{
		Charset charset = extractCharset(aidFile);
		try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(aidFile.toPath(), charset))){
			String line;
			while((line = br.readLine()) != null)
				if(br.getLineNumber() > 1 && !line.isEmpty())
					lines.add(line);
		}
	}

	private Charset extractCharset(File file) throws IOException{
		String charsetCode = StandardCharsets.UTF_8.name();
		try(BufferedReader br = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)){
			String line = br.readLine();
			//ignore any BOM marker on first line
			if(line.startsWith(BOM_MARKER))
				charsetCode = line.substring(1);
		}
		return Charset.forName(charsetCode);
	}

	public void clear(){
		lines.clear();
	}

}
