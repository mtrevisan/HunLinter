package unit731.hunspeller.services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;


public class ZipManager{

	private final List<String> filesListInDir = new ArrayList<>();


	public void zipDirectory(File dir, int compressionLevel, String zipFilename) throws FileNotFoundException, IOException{
		populateFilesList(dir);
		int startIndex = dir.getAbsolutePath().length() + 1;

		Files.deleteIfExists((new File(zipFilename)).toPath());

		//now zip files one by one
		//create ZipOutputStream to write to the zip file
		try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilename))){
			zos.setLevel(compressionLevel);

			for(String filePath : filesListInDir){
				//for ZipEntry we need to keep only relative file path, so we used substring on absolute path
				zos.putNextEntry(new ZipEntry(filePath.substring(startIndex)));

				//read the file and write to ZipOutputStream
				try(InputStream is = new FileInputStream(filePath)){
					IOUtils.copy(is, zos);

					//close the zip entry to write to zip file
					zos.closeEntry();
				}
			}
		}
	}

	private void populateFilesList(File dir) throws IOException{
		File[] files = dir.listFiles();
		for(File file : files){
			if(file.isFile())
				filesListInDir.add(file.getAbsolutePath().replaceAll("\\\\", "/"));
			else
				populateFilesList(file);
		}
	}

	public static void zipFile(File file, int compressionLevel, String zipFilename) throws FileNotFoundException, IOException{
		zipStream(new FileInputStream(file), file.getName(), compressionLevel, zipFilename);
	}

	public static void zipStream(InputStream entry, String entryName, int compressionLevel, String zipFilename) throws FileNotFoundException, IOException{
		//create ZipOutputStream to write to the zip file
		try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilename))){
			zos.setLevel(compressionLevel);

			//add a new Zip Entry to the ZipOutputStream
			zos.putNextEntry(new ZipEntry(entryName));

			//read the file and write to ZipOutputStream
			try(InputStream is = new BufferedInputStream(entry)){
				IOUtils.copy(is, zos);
				
				//close the zip entry to write to zip file
				zos.closeEntry();
			}
		}
	}

}
