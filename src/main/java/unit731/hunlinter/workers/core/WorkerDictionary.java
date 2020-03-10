package unit731.hunlinter.workers.core;

import java.io.BufferedWriter;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.services.system.JavaHelper;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.ParserHelper;


public class WorkerDictionary extends WorkerAbstract<String, WorkerDataParser<DictionaryParser>>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerDictionary.class);

	private static final MessageFormat WRONG_FILE_FORMAT = new MessageFormat("Dictionary file malformed, the first line is not a number, was ''{0}''");

	private final List<Function<?, ?>> steps = new ArrayList<>();


	protected WorkerDictionary(final WorkerDataParser<DictionaryParser> workerData){
		super(workerData);
	}

	//NOTE: if it's the first step, then its input must be Void, otherwise must be the output of the previous step
	public <I> void addStep(final Function<I, ?> stepFunction){
		steps.add(stepFunction);
	}

	@Override
	protected Void doInBackground(){
		//TODO find a way to add steps at runtime
		prepareProcessing("Reading dictionary file (step 1/2)");

		final List<Pair<Integer, String>> lines = readLines();

		LOGGER.info(ParserManager.MARKER_APPLICATION, "Execute " + workerData.getWorkerName() + " (step 2/2)");
		if(outputFile == null)
			executeReadProcess(lines);
		else
			executeWriteProcess(lines);

		return null;
	}

	private List<Pair<Integer, String>> readLines(){
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

	private void executeReadProcess(final List<Pair<Integer, String>> lines){
		processData(lines);
	}

	//NOTE: cannot use `processData` because the file must be ordered
	private void executeWriteProcess(final List<Pair<Integer, String>> lines){
		int writtenSoFar = 0;
		final int totalLines = lines.size();
		final DictionaryParser dicParser = workerData.getParser();
		final Charset charset = dicParser.getCharset();
		try(final BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), charset)){
			for(final Pair<Integer, String> rowLine : lines){
				try{
					writtenSoFar ++;

					writeDataProcessor.accept(writer, rowLine);

					setProgress(writtenSoFar, totalLines);

					sleepOnPause();
				}
				catch(final Exception e){
					if(!JavaHelper.isInterruptedException(e))
						LOGGER.info(ParserManager.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), rowLine.getKey(), rowLine.getValue());

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
