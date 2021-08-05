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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class ZipManager{

	public void zipDirectory(final Path dir, final int compressionLevel, final File zipFile) throws IOException{
		zipDirectory(dir, compressionLevel, zipFile, new Path[0]);
	}

	public void zipDirectory(final Path dir, final int compressionLevel, final File zipFile, Path... excludeFolderBut)
			throws IOException{
		Files.deleteIfExists(zipFile.toPath());

		final List<String> folders = extractFilesList(dir);
		excludeFolderBut = ArrayUtils.removeAllOccurrences(excludeFolderBut, null);
		final List<String> filesListInDir = filterFolders(folders, excludeFolderBut);

		//zip files one by one
		final int startIndex = dir.toFile().getAbsolutePath().length() + 1;
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

	private List<String> extractFilesList(final Path dir){
		final File[] files = Optional.ofNullable(dir.toFile().listFiles())
			.orElse(new File[0]);
		final List<String> filesListInDir = new ArrayList<>(files.length);
		for(final File file : files){
			if(file.isFile())
				filesListInDir.add(StringUtils.replace(file.getAbsolutePath(), "\\", "/"));
			else
				filesListInDir.addAll(extractFilesList(file.toPath()));
		}

		return filesListInDir;
	}

	private List<String> filterFolders(final Collection<String> folders, final Path[] excludeFolderBut){
		final ArrayList<String> filteredFolders = new ArrayList<>(folders.size());
		for(String folder : folders){
			folder = StringUtils.replaceChars(Path.of(folder)
				.toString(), '\\', '/');

			boolean process = true;
			if(!ArrayUtils.contains(excludeFolderBut, Path.of(folder))){
				final String[] includeFolders = Arrays.stream(excludeFolderBut)
					.map(Path::getParent)
					.map(Path::toString)
					.toArray(String[]::new);
				process = (match(includeFolders, folder) == null);
			}
			if(process)
				filteredFolders.add(folder);
		}
		filteredFolders.trimToSize();
		return filteredFolders;
	}

	private static String match(final String[] array, final String folder){
		final int size = (array != null? array.length: 0);
		for(int i = 0; i < size; i ++){
			final String elem = array[i];
			if(folder.startsWith(elem))
				return elem;
		}
		return null;
	}

	private static ZipOutputStream createZIPWriter(final File file, final int compressionLevel) throws IOException{
		return new ZipOutputStream(new FileOutputStream(file)){
			{
				def.setLevel(compressionLevel);
			}
		};
	}

}
