package unit731.hunlinter.parsers.workers.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.Backbone;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.workers.exceptions.LinterException;
import unit731.hunlinter.services.log.ExceptionHelper;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.ParserHelper;


class WorkerDictionary extends WorkerBase<String, Integer>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerDictionary.class);

	private static final MessageFormat WRONG_FILE_FORMAT = new MessageFormat("Dictionary file malformed, the first line is not a number, was ''{0}''");

	private static final int NEWLINE_SIZE = 2;


	private final AtomicInteger processingIndex = new AtomicInteger(0);

	private final File outputFile;


	public static WorkerDictionary createReadWorker(final WorkerDataAbstract workerData, final BiConsumer<String, Integer> readLineProcessor){
		Objects.requireNonNull(readLineProcessor);

		return new WorkerDictionary(workerData, readLineProcessor, null, null);
	}

	public static WorkerDictionary createWriteWorker(final WorkerDataAbstract workerData,
			final BiConsumer<BufferedWriter, Pair<Integer, String>> writeLineProcessor, final File outputFile){
		Objects.requireNonNull(writeLineProcessor);
		Objects.requireNonNull(outputFile);

		return new WorkerDictionary(workerData, null, writeLineProcessor, outputFile);
	}

	private WorkerDictionary(final WorkerDataAbstract workerData, final BiConsumer<String, Integer> readLineProcessor,
				final BiConsumer<BufferedWriter, Pair<Integer, String>> writeLineProcessor, final File outputFile){
		Objects.requireNonNull(workerData);
		workerData.validate();

		this.workerData = workerData;
		this.outputFile = outputFile;
		this.readDataProcessor = readLineProcessor;
		this.writeDataProcessor = writeLineProcessor;
	}

	@Override
	protected Void doInBackground(){
		prepareProcessing("Opening Dictionary file (pass 1/2)");

		final List<Pair<Integer, String>> lines = readLines();
		if(outputFile == null)
			readProcess(lines);
		else
			writeProcess(lines);

		return null;
	}

	private List<Pair<Integer, String>> readLines(){
		final List<Pair<Integer, String>> lines = new ArrayList<>();
		final DictionaryParser dicParser = ((WorkerDataDictionary)workerData).getDicParser();
		final File dicFile = dicParser.getDicFile();
		final Charset charset = dicParser.getCharset();
		final long totalSize = dicFile.length();
		try(LineNumberReader br = FileHelper.createReader(dicFile.toPath(), charset)){
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
			}
		}
		catch(final Exception e){
			cancelWorker(e);
		}
		return lines;
	}

	private void readProcess(final List<Pair<Integer, String>> lines){
		try{
			LOGGER.info(Backbone.MARKER_APPLICATION, workerData.getWorkerName() + " (pass 2/2)");

			processLines(lines);

			finalizeProcessing("Successfully processed dictionary file");
		}
		catch(final Exception e){
			cancelWorker(e);
		}
	}

	private void processLines(final List<Pair<Integer, String>> lines){
		final int totalLines = lines.size();
		processingIndex.set(0);
		final Consumer<Pair<Integer, String>> processor = rowLine -> {
			if(isCancelled())
				throw new RuntimeInterruptedException();

			try{
				while(isPaused())
					Thread.sleep(500l);

				processingIndex.incrementAndGet();

				readDataProcessor.accept(rowLine.getValue(), rowLine.getKey());

				setProcessingProgress(processingIndex.get(), totalLines);
			}
			catch(final InterruptedException e){
				if(!workerData.isPreventExceptionRelaunch())
					throw new RuntimeException(e);
			}
			catch(final Exception e){
				String errorMessage = ExceptionHelper.getMessage(e);
				LOGGER.trace("{}, line {}: {}", errorMessage, rowLine.getKey(), rowLine.getValue());
				LOGGER.info(Backbone.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), rowLine.getKey(), rowLine.getValue());

				if(!workerData.isPreventExceptionRelaunch())
					throw e;
			}
		};

		processLines(lines, processor);
	}

	private void processLines(final List<Pair<Integer, String>> lines, final Consumer<Pair<Integer, String>> processor){
		if(workerData.isParallelProcessing())
			lines.parallelStream()
				.forEach(processor);
		else
			lines
				.forEach(processor);
	}

	private void writeProcess(final List<Pair<Integer, String>> lines){
		LOGGER.info(Backbone.MARKER_APPLICATION, workerData.getWorkerName() + " (pass 2/2)");

		int writtenSoFar = 0;
		final int totalLines = lines.size();
		final DictionaryParser dicParser = ((WorkerDataDictionary)workerData).getDicParser();
		final Charset charset = dicParser.getCharset();
		try(final BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), charset)){
			for(final Pair<Integer, String> rowLine : lines){
				if(isCancelled())
					throw new RuntimeInterruptedException();

				try{
					writtenSoFar ++;

					writeDataProcessor.accept(writer, rowLine);

					setProcessingProgress(writtenSoFar, totalLines);
				}
				catch(final Exception e){
					LOGGER.info(Backbone.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), rowLine.getKey(), rowLine.getValue());

					if(!workerData.isPreventExceptionRelaunch())
						throw e;
				}
			}

			finalizeProcessing("Successfully processed dictionary file");
		}
		catch(final Exception e){
			cancelWorker(e);
		}
	}

}
