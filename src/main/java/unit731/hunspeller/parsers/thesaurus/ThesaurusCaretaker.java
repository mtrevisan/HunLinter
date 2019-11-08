package unit731.hunspeller.parsers.thesaurus;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Stack;

import com.github.difflib.patch.Patch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.services.FileHelper;


public class ThesaurusCaretaker{

	private static final Logger LOGGER = LoggerFactory.getLogger(ThesaurusCaretaker.class);

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();


	private final Stack<File> mementos = new Stack<>();


	public void pushMemento(final Patch<ThesaurusEntry> memento) throws IOException{
		final String json = JSON_MAPPER.writeValueAsString(memento);
		final byte[] bytes = FileHelper.compressData(json.getBytes(DEFAULT_CHARSET));
		final File mementoFile = FileHelper.createDeleteOnExitFile(bytes, "memento", ".zip");

		LOGGER.info("Created memento file '{}', size is {} B", mementoFile.toString(), bytes.length);

		mementos.push(mementoFile);
	}

	public Patch<ThesaurusEntry> popMemento() throws IOException{
		final File mementoFile = mementos.pop();

		LOGGER.info("Retrieve memento file '{}'", mementoFile.toString());

		final byte[] bytes = Files.readAllBytes(mementoFile.toPath());
		final String json = new String(FileHelper.decompressData(bytes), DEFAULT_CHARSET);
		return JSON_MAPPER.readValue(json, Patch.class);
	}

	public boolean canUndo(){
		return !mementos.empty();
	}

}
