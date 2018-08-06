package unit731.hunspeller.parsers.aid;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class AidParser extends ReadWriteLockable{

	private final List<String> lines = new ArrayList<>();


	/**
	 * Parse the rows out from a .aid file.
	 *
	 * @param aidFile	The content of the dictionary file
	 * @throws IOException	If an I/O error occurse
	 */
	public void parse(File aidFile) throws IOException{
		acquireWriteLock();
		try{
			clear();

			Charset charset = FileService.determineCharset(aidFile.toPath());
			try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(aidFile.toPath(), charset))){
				String line;
				while((line = br.readLine()) != null){
					//ignore any BOM marker on first line
					if(br.getLineNumber() == 1)
						line = FileService.clearBOMMarker(line);

					if(!line.isEmpty())
						lines.add(line);
				}
			}
		}
		finally{
			releaseWriteLock();
		}
	}

	public void clear(){
		acquireWriteLock();
		try{
			lines.clear();
		}
		finally{
			releaseWriteLock();
		}
	}

	public List<String> getLines(){
		acquireReadLock();
		try{
			return lines;
		}
		finally{
			releaseReadLock();
		}
	}

}
