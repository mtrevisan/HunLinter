package unit731.hunspeller.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileService{

	//FEFF because this is the Unicode char represented by the UTF-8 byte order mark (EF BB BF)
	public static final String BOM_MARKER = "\uFEFF";

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

}
