package unit731.hunlinter.services;

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
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static unit731.hunlinter.services.system.LoopHelper.match;


public class ZipManager{

	public void zipDirectory(final Path dir, final int compressionLevel, final File zipFile) throws IOException{
		zipDirectory(dir, compressionLevel, zipFile, new Path[0]);
	}

	public void zipDirectory(final Path dir, final int compressionLevel, final File zipFile, Path... excludeFolderBut)
			throws IOException{
		Files.deleteIfExists(zipFile.toPath());

		final List<String> folders = extractFilesList(dir);
		excludeFolderBut = ArrayUtils.removeAllOccurences(excludeFolderBut, null);
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

	private List<String> filterFolders(final List<String> folders, final Path[] excludeFolderBut){
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
				process = (match(includeFolders, folder::startsWith) == null);
			}
			if(process)
				filteredFolders.add(folder);
		}
		filteredFolders.trimToSize();
		return filteredFolders;
	}

	private static ZipOutputStream createZIPWriter(final File file, final int compressionLevel) throws IOException{
		return new ZipOutputStream(new FileOutputStream(file)){
			{
				def.setLevel(compressionLevel);
			}
		};
	}

}
