package unit731.hunlinter.parsers.workers.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.LineNumberReader;
import java.nio.channels.ClosedChannelException;
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


	public static WorkerDictionary createReadWorker(final WorkerData workerData, final BiConsumer<String, Integer> readLineProcessor){
		Objects.requireNonNull(readLineProcessor);

		return new WorkerDictionary(workerData, readLineProcessor, null, null);
	}

	public static WorkerDictionary createWriteWorker(final WorkerData workerData,
			final BiConsumer<BufferedWriter, Pair<Integer, String>> writeLineProcessor, final File outputFile){
		Objects.requireNonNull(writeLineProcessor);
		Objects.requireNonNull(outputFile);

		return new WorkerDictionary(workerData, null, writeLineProcessor, outputFile);
	}

	private WorkerDictionary(final WorkerData workerData, final BiConsumer<String, Integer> readLineProcessor,
			final BiConsumer<BufferedWriter, Pair<Integer, String>> writeLineProcessor, final File outputFile){
		Objects.requireNonNull(workerData);
		workerData.validate();

		this.workerData = workerData;
		this.outputFile = outputFile;
		this.readLineProcessor = readLineProcessor;
		this.writeLineProcessor = writeLineProcessor;
	}

	@Override
	protected Void doInBackground(){
		LOGGER.info(Backbone.MARKER_APPLICATION, "Opening Dictionary file (pass 1/2)");
		setProgress(0);

		watch.reset();

		final List<Pair<Integer, String>> lines = readLines();

		if(outputFile == null)
			readProcess(lines);
		else
			writeProcess(lines);

		return null;
	}

	private List<Pair<Integer, String>> readLines(){
		final List<Pair<Integer, String>> lines = new ArrayList<>();
		final File dicFile = getDicFile();
		final Charset charset = getCharset();
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

				setProgress(getProgress(readSoFar, totalSize));
			}
		}
		catch(final Exception e){
			cancelWorker(e);
		}
		return lines;
	}

	private void readProcess(final List<Pair<Integer, String>> lines){
		try{
			exception = null;

			LOGGER.info(Backbone.MARKER_APPLICATION, workerData.workerName + " (pass 2/2)");
			setProgress(0);

			processLines(lines);

			watch.stop();

			setProgress(100);

			LOGGER.info(Backbone.MARKER_APPLICATION, "Successfully processed dictionary file (in {})", watch.toStringMinuteSeconds());
		}
		catch(final Exception e){
			exception = e;

			if(isInterruptedException(e))
				LOGGER.info("Thread interrupted");
			else
				LOGGER.error("Generic error", e);

			LOGGER.info(Backbone.MARKER_APPLICATION, "Stopped processing Dictionary file", new Object[]{});

			cancel(true);
		}
	}

	private boolean isInterruptedException(final Exception exc){
		final Throwable t = (exc.getCause() != null? exc.getCause(): exc);
		return (t instanceof InterruptedException || t instanceof RuntimeInterruptedException);
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

				readLineProcessor.accept(rowLine.getValue(), rowLine.getKey());

				setProgress(getProgress(processingIndex.get(), totalLines));
			}
			catch(final InterruptedException e){
				if(isRelaunchException())
					throw new RuntimeException(e);
			}
			catch(final Exception e){
				String errorMessage = ExceptionHelper.getMessage(e);
				LOGGER.trace("{}, line {}: {}", errorMessage, rowLine.getKey(), rowLine.getValue());
				LOGGER.info(Backbone.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), rowLine.getKey(), rowLine.getValue());

				if(isRelaunchException())
					throw e;
			}
		};

		processLines(lines, processor);
	}

	private void processLines(final List<Pair<Integer, String>> lines, final Consumer<Pair<Integer, String>> processor){
		if(isParallelProcessing())
			lines.parallelStream()
				.forEach(processor);
		else
			lines
				.forEach(processor);
	}

	private void writeProcess(final List<Pair<Integer, String>> lines){
		LOGGER.info(Backbone.MARKER_APPLICATION, workerData.workerName + " (pass 2/2)");

		setProgress(0);

		int writtenSoFar = 0;
		final int totalLines = lines.size();
		final Charset charset = getCharset();
		try(final BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), charset)){
			for(final Pair<Integer, String> rowLine : lines){
				if(isCancelled())
					throw new RuntimeInterruptedException();

				try{
					writtenSoFar ++;

					writeLineProcessor.accept(writer, rowLine);

					setProgress(getProgress(writtenSoFar, totalLines));
				}
				catch(final Exception e){
					LOGGER.info(Backbone.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), rowLine.getKey(), rowLine.getValue());

					if(isRelaunchException())
						throw e;
				}
			}


			watch.stop();

			setProgress(100);

			LOGGER.info(Backbone.MARKER_APPLICATION, "Successfully processed dictionary file (in {})", watch.toStringMinuteSeconds());
		}
		catch(final Exception e){
			cancelWorker(e);
		}
	}

	private void cancelWorker(final Exception e){
		if(e instanceof ClosedChannelException)
			LOGGER.info("Thread interrupted");
		else if(e != null){
			final String message = ExceptionHelper.getMessage(e);
			LOGGER.error("{}: {}", e.getClass().getSimpleName(), message);
		}

		LOGGER.info(Backbone.MARKER_APPLICATION, "Stopped processing Dictionary file", new Object[]{});

		cancel(true);
	}

	private int getProgress(final double index, final double total){
		return Math.min((int)Math.floor((index * 100.) / total), 100);
	}

}
