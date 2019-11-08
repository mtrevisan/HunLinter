package unit731.hunspeller.parsers.thesaurus;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Stack;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ThesaurusCaretaker{

	private static final Logger LOGGER = LoggerFactory.getLogger(ThesaurusCaretaker.class);

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();


	private final Stack<Path> mementos = new Stack<>();


	public void pushMemento(final ThesaurusDictionary memento) throws IOException{
		final String json = JSON_MAPPER.writeValueAsString(memento);
		final byte[] bytes = compress(json.getBytes(DEFAULT_CHARSET));
		final Path mementoFile = createFile(bytes);

		LOGGER.info("Created memento file '{}', size is {} B", mementoFile.toString(), bytes.length);

		mementos.push(mementoFile);
	}

	private Path createFile(final byte[] bytes) throws IOException{
		final Path mementoFile = Files.createTempFile("memento", ".zip");
		mementoFile.toFile().deleteOnExit();
		Files.write(mementoFile, bytes);
		return mementoFile;
	}

	public ThesaurusDictionary popMemento() throws IOException{
		final Path mementoFile = mementos.pop();

		LOGGER.info("Retrieve memento file '{}'", mementoFile.toString());

		final byte[] bytes = Files.readAllBytes(mementoFile);
		final String json = new String(decompress(bytes), DEFAULT_CHARSET);
		return JSON_MAPPER.readValue(json, ThesaurusDictionary.class);
	}

	public boolean canUndo(){
		return !mementos.empty();
	}

	private byte[] compress(final byte[] bytes) throws IOException{
		if(bytes == null || bytes.length== 0)
			return new byte[0];

		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		try(final GZIPOutputStream gzip = new GZIPOutputStream(os, 2048){
			{
				def.setLevel(Deflater.BEST_COMPRESSION);
			}
		}){
			gzip.write(bytes);
		}
		return os.toByteArray();
	}

	private byte[] decompress(final byte[] bytes) throws IOException{
		if(bytes == null || bytes.length == 0)
			return new byte[0];

		try(final GZIPInputStream is = new GZIPInputStream(new ByteArrayInputStream(bytes))){
			return IOUtils.toByteArray(is);
		}
	}

}
