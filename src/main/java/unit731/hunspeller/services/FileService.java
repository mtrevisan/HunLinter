package unit731.hunspeller.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class FileService{

	//FEFF because this is the Unicode char represented by the UTF-8 byte order mark (EF BB BF)
	private static final String BOM_MARKER = "\uFEFF";

	private static final List<Charset> HUNSPELL_CHARSETS;
	static{
		HUNSPELL_CHARSETS = Stream.of("UTF-8", "ISO_8859_1", "ISO_8859_2", "ISO_8859_3", "ISO_8859_4", "ISO_8859_5",
				"ISO_8859_6", "ISO_8859_7", "ISO_8859_8", "ISO_8859_9", "ISO_8859_10", "ISO_8859_13", "ISO_8859_14", "ISO_8859_15", "KOI8_R", "KOI8_U",
				"MICROSOFT_CP1251", "ISCII_DEVANAGARI")
			.map(name -> {
				Charset cs = null;
				try{
					cs = Charset.forName(name);
				}
				catch(Exception e){}
				return cs;
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}


	public static Charset determineCharset(Path path){
		for(Charset cs : HUNSPELL_CHARSETS){
			try{
				BufferedReader reader = Files.newBufferedReader(path, cs);
				reader.read();
				return cs;
			}
			catch(IOException e){}
		}

		throw new IllegalArgumentException("The file is not in an ammissible charset ("
			+ HUNSPELL_CHARSETS.stream().map(Charset::name).collect(Collectors.joining(", ")) + ")");
	}

	public static File getTemporaryUTF8File(String content){
		return getTemporaryUTF8File(content, ".tmp");
	}

	public static File getTemporaryUTF8File(String content, String extension){
		try{
			File tmpFile = File.createTempFile("test", extension);
			Files.write(tmpFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
			tmpFile.deleteOnExit();
			return tmpFile;
		}
		catch(IOException e){
			throw new RuntimeException("Failed creating temporary file for content '" + content + "'", e);
		}
	}

	/** Ignore any BOM marker */
	public static String clearBOMMarker(String line){
		return (line.startsWith(BOM_MARKER)? line.substring(1): line);
	}

	//https://stackoverflow.com/questions/526037/how-to-open-user-system-preferred-editor-for-given-file
	public static void openFileWithChoosenEditor(File file) throws InterruptedException, IOException{
		ProcessBuilder builder = null;
		if(SystemUtils.IS_OS_WINDOWS)
			builder = new ProcessBuilder("rundll32.exe", "shell32.dll,OpenAs_RunDLL", file.getAbsolutePath());
		else if(SystemUtils.IS_OS_LINUX)
			builder = new ProcessBuilder("edit", file.getAbsolutePath());
		else if(SystemUtils.IS_OS_MAC)
			builder = new ProcessBuilder("open", file.getAbsolutePath());

		if(builder != null){
			builder.redirectErrorStream();
			builder.redirectOutput();
			Process process = builder.start();
			process.waitFor();
		}
		else
			log.warn("Cannot open file {}, OS not recognized ({})", file.getName(), SystemUtils.OS_NAME);
	}

}
