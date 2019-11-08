package unit731.hunspeller.parsers.thesaurus;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.github.difflib.patch.Patch;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.services.FileHelper;


public class MementoCaretaker{

	private static final Logger LOGGER = LoggerFactory.getLogger(MementoCaretaker.class);

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();


	private final List<File> mementos = new ArrayList<>();
	private int index;
	private int maxIndex;


	public void pushMemento(final Pair<Patch<String>, Patch<String>> memento) throws IOException{
		final String json = JSON_MAPPER.writeValueAsString(memento);
		final byte[] bytes = FileHelper.compressData(json.getBytes(DEFAULT_CHARSET));
		final File mementoFile = FileHelper.createDeleteOnExitFile(bytes, "memento", ".zip");

		LOGGER.info("Created memento file '{}', size is {} B", mementoFile.toString(), bytes.length);

		if(index < maxIndex)
			mementos.set(index, mementoFile);
		else{
			mementos.add(mementoFile);
			index ++;
			maxIndex ++;
		}
	}

	public Pair<Patch<String>, Patch<String>> popPreviousMemento() throws IOException{
		Pair<Patch<String>, Patch<String>> memento = null;
		if(canUndo()){
			final File mementoFile = mementos.get(-- index);

			LOGGER.info("Retrieve memento file '{}'", mementoFile.toString());

			final byte[] bytes = Files.readAllBytes(mementoFile.toPath());
			final String json = new String(FileHelper.decompressData(bytes), DEFAULT_CHARSET);
			memento = JSON_MAPPER.readValue(json, Pair.class);
		}
		return memento;
	}

	public Pair<Patch<String>, Patch<String>> popNextMemento() throws IOException{
		Pair<Patch<String>, Patch<String>> memento = null;
		if(canRedo()){
			final File mementoFile = mementos.get(index ++);

			LOGGER.info("Retrieve memento file '{}'", mementoFile.toString());

			final byte[] bytes = Files.readAllBytes(mementoFile.toPath());
			final String json = new String(FileHelper.decompressData(bytes), DEFAULT_CHARSET);
			memento = JSON_MAPPER.readValue(json, Pair.class);
		}
		return memento;
	}

	public boolean canUndo(){
		return (index > 0);
	}

	public boolean canRedo(){
		return (index < maxIndex);
	}

}
