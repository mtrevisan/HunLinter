package unit731.hunlinter.workers.core;

import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.ParserHelper;


public class WorkerDictionary extends WorkerAbstract<String, WorkerDataParser<DictionaryParser>>{

	private static final MessageFormat WRONG_FILE_FORMAT = new MessageFormat("Dictionary file malformed, the first line is not a number, was ''{0}''");


	protected WorkerDictionary(final WorkerDataParser<DictionaryParser> workerData){
		super(workerData);
	}

	@Override
	protected Void doInBackground(){
		try{
			processor.apply(null);
		}
		catch(final Exception e){
			cancel(e);
		}
		return null;
	}

	protected List<Pair<Integer, String>> readLines(){
		final List<Pair<Integer, String>> lines = new ArrayList<>();
		final DictionaryParser dicParser = workerData.getParser();
		final Path dicPath = dicParser.getDicFile().toPath();
		final Charset charset = dicParser.getCharset();
		int currentLine = 0;
		final int totalLines = FileHelper.countLines(dicPath);
		try(final LineNumberReader br = FileHelper.createReader(dicPath, charset)){
			String line = ParserHelper.extractLine(br);
			currentLine ++;

			if(!NumberUtils.isCreatable(line))
				throw new LinterException(WRONG_FILE_FORMAT.format(new Object[]{line}));

			while((line = br.readLine()) != null){
				currentLine ++;

				line = ParserHelper.cleanLine(line);
				if(!line.isEmpty())
					lines.add(Pair.of(br.getLineNumber(), line));

				setProgress(currentLine, totalLines);

				sleepOnPause();
			}
		}
		catch(final Exception e){
			cancel(e);
		}
		return lines;
	}

}
