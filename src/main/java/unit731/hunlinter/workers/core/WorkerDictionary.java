package unit731.hunlinter.workers.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.Backbone;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.ParserHelper;


public class WorkerDictionary extends WorkerAbstract<String, WorkerDataParser<DictionaryParser>>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerDictionary.class);

	private static final MessageFormat WRONG_FILE_FORMAT = new MessageFormat("Dictionary file malformed, the first line is not a number, was ''{0}''");

	private static final int NEWLINE_SIZE = 2;


	protected WorkerDictionary(final WorkerDataParser<DictionaryParser> workerData){
		super(workerData);
	}

	@Override
	protected Void doInBackground(){
		prepareProcessing("Opening Dictionary file (pass 1/2)");

		final List<Pair<Integer, String>> lines = readLines();

		LOGGER.info(Backbone.MARKER_APPLICATION, workerData.getWorkerName() + " (pass 2/2)");
		if(outputFile == null)
			readProcess(lines);
		else
			writeProcess(lines);

		return null;
	}

	private List<Pair<Integer, String>> readLines(){
		final List<Pair<Integer, String>> lines = new ArrayList<>();
		final DictionaryParser dicParser = workerData.getParser();
		final File dicFile = dicParser.getDicFile();
		final Charset charset = dicParser.getCharset();
		final long totalSize = dicFile.length();
		try(final LineNumberReader br = FileHelper.createReader(dicFile.toPath(), charset)){
			String line = ParserHelper.extractLine(br);

			long readSoFar = line.getBytes(charset).length + NEWLINE_SIZE;

			if(!NumberUtils.isCreatable(line))
				throw new LinterException(WRONG_FILE_FORMAT.format(new Object[]{line}));

			while((line = br.readLine()) != null){
				readSoFar += line.getBytes(charset).length + NEWLINE_SIZE;

				line = ParserHelper.cleanLine(line);
				if(!line.isEmpty())
					lines.add(Pair.of(br.getLineNumber(), line));

				setProcessingProgress(readSoFar, totalSize);

				sleepOnPause();
			}
		}
		catch(final Exception e){
			cancel(e);
		}
		return lines;
	}

	private void readProcess(final List<Pair<Integer, String>> lines){
		processData(lines);
	}

	//NOTE: cannot use `processData` because the file must be ordered
	private void writeProcess(final List<Pair<Integer, String>> lines){
		int writtenSoFar = 0;
		final int totalLines = lines.size();
		final DictionaryParser dicParser = workerData.getParser();
		final Charset charset = dicParser.getCharset();
		try(final BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), charset)){
			for(final Pair<Integer, String> rowLine : lines){
				try{
					writtenSoFar ++;

					writeDataProcessor.accept(writer, rowLine);

					setProcessingProgress(writtenSoFar, totalLines);

					sleepOnPause();
				}
				catch(final Exception e){
					LOGGER.info(Backbone.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), rowLine.getKey(), rowLine.getValue());

					if(workerData.isRelaunchException())
						throw e;
				}
			}

			finalizeProcessing("Successfully processed dictionary file");
		}
		catch(final Exception e){
			cancel(e);
		}
	}

}
