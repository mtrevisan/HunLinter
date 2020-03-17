package unit731.hunlinter.services;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.services.system.LoopHelper;

import java.io.BufferedInputStream;
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
import java.util.stream.Collectors;
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
		excludeFolderBut = LoopHelper.nullableToStream(excludeFolderBut)
			.toArray(Path[]::new);
		final List<String> filesListInDir = filterFolders(folders, excludeFolderBut);

		//zip files one by one
		final int startIndex = dir.toFile().getAbsolutePath().length() + 1;
		try(final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))){
			zos.setLevel(compressionLevel);

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

	private List<String> filterFolders(final List<String> folders, final Path[] excludeFolderBut){
		return folders.stream()
			.filter(folder -> {
				folder = Path.of(folder)
					.toString();
				if(ArrayUtils.contains(excludeFolderBut, Path.of(folder)))
					return true;

				final String[] includeFolders = Arrays.stream(excludeFolderBut)
					.map(Path::getParent)
					.map(Path::toString)
					.toArray(String[]::new);
				for(final String includeFolder : includeFolders)
					if(folder.startsWith(includeFolder))
						return false;
				return true;
			})
			.collect(Collectors.toList());
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

	public static void zipFile(final File file, final int compressionLevel, final File zipFile) throws IOException{
		zipStream(new FileInputStream(file), file.getName(), compressionLevel, zipFile);
	}

	public static void zipStream(final InputStream entry, final String entryName, final int compressionLevel, final File zipFile)
			throws IOException{
		//create ZipOutputStream to write to the zip file
		try(final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))){
			zos.setLevel(compressionLevel);

			//add a new Zip Entry to the ZipOutputStream
			zos.putNextEntry(new ZipEntry(entryName));

			//read the file and write to ZipOutputStream
			try(final InputStream is = new BufferedInputStream(entry)){
				IOUtils.copy(is, zos);

				//close the zip entry to write to zip file
				zos.closeEntry();
			}
		}
	}

}
