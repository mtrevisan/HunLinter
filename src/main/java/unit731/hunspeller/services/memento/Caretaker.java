package unit731.hunspeller.services.memento;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;


/**
 * @see <a href="https://github.com/jmcalma/MementoPattern">Memento Pattern</a>
 * 
 * @param <T>	Type for the data (stored as a list)
 */
public class Caretaker<T>{

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();


	private final Stack<Path> mementos = new Stack<>();
	private final CollectionType collectionType;


	public Caretaker(Class<T> cl){
		Objects.requireNonNull(cl);

		collectionType = JSON_MAPPER.getTypeFactory().constructCollectionType(List.class, cl);
	}

	public void pushMemento(List<T> memento) throws IOException{
		String json = JSON_MAPPER.writeValueAsString(memento);
		byte[] bytes = compress(json.getBytes(DEFAULT_CHARSET));
		Path mementoFile = createFile(bytes);

		mementos.push(mementoFile);
	}

	private Path createFile(byte[] bytes) throws IOException{
		Path mementoFile = Files.createTempFile("memento", ".zip");
		mementoFile.toFile().deleteOnExit();
		Files.write(mementoFile, bytes);
		return mementoFile;
	}

	public List<T> popMemento() throws IOException{
		Path mementoFile = mementos.pop();

		byte[] bytes = Files.readAllBytes(mementoFile);
		String json = new String(decompress(bytes), DEFAULT_CHARSET);
		return JSON_MAPPER.readValue(json, collectionType);
	}

	public boolean canUndo(){
		return !mementos.empty();
	}

	private byte[] compress(byte[] bytes) throws IOException{
		if(bytes == null || bytes.length== 0)
			return new byte[0];

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try(GZIPOutputStream gzip = new GZIPOutputStream(os, 2048){
			{
				def.setLevel(Deflater.BEST_COMPRESSION);
			}
		}){
			gzip.write(bytes);
		}
		return os.toByteArray();
	}

	private byte[] decompress(byte[] bytes) throws IOException{
		if(bytes == null || bytes.length == 0)
			return new byte[0];

		try(GZIPInputStream is = new GZIPInputStream(new ByteArrayInputStream(bytes))){
			return IOUtils.toByteArray(is);
		}
	}

}
