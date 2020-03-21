package unit731.hunlinter.parsers.aid;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import unit731.hunlinter.services.FileHelper;


public class AidParser{

	private final List<String> lines = new ArrayList<>();


	/**
	 * Parse the rows out from a .aid file.
	 *
	 * @param aidFile	The content of the dictionary file
	 * @throws IOException	If an I/O error occurs
	 */
	public void parse(final File aidFile) throws IOException{
		lines.clear();

		final Path path = aidFile.toPath();
		final Charset charset = FileHelper.determineCharset(path);
		try(final Scanner scanner = FileHelper.createScanner(path, charset)){
			while(scanner.hasNextLine()){
				final String line = scanner.nextLine();
				if(!line.isEmpty())
					lines.add(line);
			}
		}
	}

	public void clear(){
		lines.clear();
	}

	public List<String> getLines(){
		return lines;
	}

}
