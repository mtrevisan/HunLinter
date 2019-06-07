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
import org.apache.commons.lang3.StringUtils;


public class ZipManager{

	public void zipDirectory(File dir, int compressionLevel, String zipFilename) throws FileNotFoundException, IOException{
		Files.deleteIfExists((new File(zipFilename)).toPath());

		List<String> filesListInDir = extractFilesList(dir);

		//now zip files one by one
		int startIndex = dir.getAbsolutePath().length() + 1;
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

	private List<String> extractFilesList(File dir){
		List<String> filesListInDir = new ArrayList<>();

		File[] files = dir.listFiles();
		for(File file : files){
			if(file.isFile())
				filesListInDir.add(StringUtils.replace(file.getAbsolutePath(), "\\", "/"));
			else
				filesListInDir.addAll(extractFilesList(file));
		}

		return filesListInDir;
	}

	public static void zipFile(File file, int compressionLevel, String zipFilename) throws FileNotFoundException, IOException{
		zipStream(new FileInputStream(file), file.getName(), compressionLevel, zipFilename);
	}

	public static void zipStream(InputStream entry, String entryName, int compressionLevel, String zipFilename) throws FileNotFoundException,
			IOException{
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
