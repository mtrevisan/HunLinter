package unit731.hunspeller.parsers.aid;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import unit731.hunspeller.services.FileService;


public class AidParser{

	private final ReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock();


	private final List<String> lines = new ArrayList<>();


	/**
	 * Parse the rows out from a .aid file.
	 *
	 * @param aidFile	The content of the dictionary file
	 * @throws IOException	If an I/O error occurse
	 */
	public void parse(File aidFile) throws IOException{
		READ_WRITE_LOCK.writeLock().lock();
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
			READ_WRITE_LOCK.writeLock().unlock();
		}
	}

	public void clear(){
		READ_WRITE_LOCK.writeLock().lock();
		try{
			lines.clear();
		}
		finally{
			READ_WRITE_LOCK.writeLock().unlock();
		}
	}

	public List<String> getLines(){
		READ_WRITE_LOCK.readLock().lock();
		try{
			return lines;
		}
		finally{
			READ_WRITE_LOCK.readLock().unlock();
		}
	}

}
