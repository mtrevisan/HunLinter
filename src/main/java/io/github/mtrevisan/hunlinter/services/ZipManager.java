/**
 * Copyright (c) 2019-2021 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.services;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class ZipManager{

	private static final Logger LOGGER = LoggerFactory.getLogger(ZipManager.class);


	public void zipDirectory(final File dir, final int compressionLevel, final File zipFile) throws IOException{
		zipDirectory(dir, compressionLevel, zipFile, new File[0]);
	}

	public void zipDirectory(final File dir, final int compressionLevel, final File zipFile, final File... excludeFolderBut)
			throws IOException{
		Files.deleteIfExists(zipFile.toPath());

		final List<String> filesListInDir = extractFileList(dir, excludeFolderBut);

		//zip files one by one
		final int startIndex = dir.getAbsolutePath().length() + 1;
		try(final ZipOutputStream zos = createZIPWriter(zipFile, compressionLevel)){
			for(final String filePath : filesListInDir){
				//for ZipEntry we need to keep only relative file path, so we used substring on absolute path
				zos.putNextEntry(new ZipEntry(filePath.substring(startIndex)));

				//read the file and write to ZipOutputStream
				try(final InputStream is = new FileInputStream(filePath)){
					IOUtils.copy(is, zos);

					//close the zip entry to write to zip file
					zos.closeEntry();
				}
			}
		}
	}

	private List<String> extractFileList(final File projectFolder, final File[] excludeFolderBut){
		final List<File> exclusions = new ArrayList<>(excludeFolderBut.length);
		for(final File file : excludeFolderBut)
			if(file != null)
				exclusions.add(file.getParentFile());
		final List<String> filesListInDir = extractFilesList(projectFolder, exclusions);
		for(final File file : excludeFolderBut)
			if(file != null)
				filesListInDir.add(StringUtils.replace(file.getAbsolutePath(), "\\", "/"));
		return filesListInDir;
	}

	private List<String> extractFilesList(final File dir, final List<File> excludeFolderBut){
		final File[] files = Optional.ofNullable(dir.listFiles())
			.orElse(new File[0]);
		final List<String> filesListInDir = new ArrayList<>(files.length);
		for(final File file : files){
			if(file.isFile())
				filesListInDir.add(StringUtils.replace(file.getAbsolutePath(), "\\", "/"));
			else if(!excludeFolderBut.contains(file))
				filesListInDir.addAll(extractFilesList(file, excludeFolderBut));
		}

		return filesListInDir;
	}

	private static ZipOutputStream createZIPWriter(final File file, final int compressionLevel) throws IOException{
		return new ZipOutputStream(new FileOutputStream(file)){
			{
				def.setLevel(compressionLevel);
			}
		};
	}


	public void unzipFile(final File zipFile, final Path destination){
		final byte[] buffer = new byte[1024];
		try(final ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))){
			final File dest = destination.toFile();
			ZipEntry zipEntry = zis.getNextEntry();
			while(zipEntry != null){
				final File newFile = createNewFile(dest, zipEntry);
				if(zipEntry.isDirectory()){
					if(!newFile.isDirectory() && ! newFile.mkdirs())
						throw new IOException("Failed to create directory " + newFile);
				}
				else{
					//fix for Windows-created archives
					final File parent = newFile.getParentFile();
					if(!parent.isDirectory() && !parent.mkdirs())
						throw new IOException("Failed to create directory " + parent);

					//write file content
					final FileOutputStream fos = new FileOutputStream(newFile);
					int len;
					while((len = zis.read(buffer)) > 0)
						fos.write(buffer, 0, len);
					fos.close();
				}
				zipEntry = zis.getNextEntry();
			}
			zis.closeEntry();
		}
		catch(final IOException ioe){
			LOGGER.warn("Cannot extract file", ioe);
		}
	}

	//This method guards against writing files to the file system outside the target folder.
	//This vulnerability is called Zip Slip.
	private File createNewFile(final File destinationDir, final ZipEntry zipEntry) throws IOException{
		final File destFile = new File(destinationDir, zipEntry.getName());
		final String destDirPath = destinationDir.getCanonicalPath();
		final String destFilePath = destFile.getCanonicalPath();
		if(!destFilePath.startsWith(destDirPath + File.separator))
			throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());

		return destFile;
	}

}
