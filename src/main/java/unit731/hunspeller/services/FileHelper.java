package unit731.hunspeller.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FileHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(FileHelper.class);

	private static final MessageFormat WRONG_FILE_FORMAT = new MessageFormat("The file is not in an allowable charset ({0})");


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
				catch(final Exception ignored){}
				return cs;
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}


	private FileHelper(){}

	public static Charset determineCharset(final Path path){
		for(final Charset cs : HUNSPELL_CHARSETS){
			try{
				final BufferedReader reader = Files.newBufferedReader(path, cs);
				reader.read();
				return cs;
			}
			catch(final IOException ignored){}
		}

		throw new IllegalArgumentException(WRONG_FILE_FORMAT.format(new Object[]{HUNSPELL_CHARSETS.stream().map(Charset::name).collect(Collectors.joining(", "))}));
	}

	public static File getTemporaryUTF8File(final String prefix, final String extension, final String... lines){
		final StringJoiner sj = new StringJoiner("\n");
		for(final String line : lines)
			sj.add(line);
		final String content = sj.toString();

		try{
			final File tmpFile = File.createTempFile((prefix != null? prefix: "test"), extension);
			Files.writeString(tmpFile.toPath(), content);
			tmpFile.deleteOnExit();
			return tmpFile;
		}
		catch(final IOException e){
			throw new RuntimeException("Failed creating temporary file for content '" + content + "'", e);
		}
	}

	public static LineNumberReader createReader(final Path path, final Charset charset) throws IOException{
		final BOMInputStream bomis = new BOMInputStream(Files.newInputStream(path), ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE,
			ByteOrderMark.UTF_32BE, ByteOrderMark.UTF_32LE);
		return new LineNumberReader(new BufferedReader(new InputStreamReader(bomis, charset)));
	}

	//https://stackoverflow.com/questions/526037/how-to-open-user-system-preferred-editor-for-given-file
	public static void openFileWithChosenEditor(final File file) throws InterruptedException, IOException{
		ProcessBuilder builder = null;
		if(SystemUtils.IS_OS_WINDOWS)
			builder = new ProcessBuilder("rundll32.exe", "shell32.dll,OpenAs_RunDLL", file.getAbsolutePath());
		else if(SystemUtils.IS_OS_LINUX)
			builder = new ProcessBuilder("edit", file.getAbsolutePath());
		else if(SystemUtils.IS_OS_MAC)
			builder = new ProcessBuilder("open", file.getAbsolutePath());

		if(builder != null){
			final Process process = builder.start();
			process.waitFor();
		}
		else
			LOGGER.warn("Cannot open file {}, OS not recognized ({})", file.getName(), SystemUtils.OS_NAME);
	}

}
