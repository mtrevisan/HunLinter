package unit731.hunspeller.parsers.dictionary.workers.core;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.services.ExceptionHelper;
import unit731.hunspeller.services.FileHelper;


class WorkerDictionary extends WorkerBase<String, Integer>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerDictionary.class);

	private static final int NEWLINE_SIZE = 2;


	private final AtomicBoolean paused = new AtomicBoolean(false);

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
	protected Void doInBackground() throws IOException{
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
			String line = extractLine(br);
			
			long readSoFar = line.getBytes(charset).length + NEWLINE_SIZE;
			
			if(!NumberUtils.isCreatable(line))
				throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");
			
			while((line = br.readLine()) != null){
				readSoFar += line.getBytes(charset).length + NEWLINE_SIZE;
				
				line = DictionaryParser.cleanLine(line);
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

	private String extractLine(final LineNumberReader br) throws IOException, EOFException{
		String line = br.readLine();
		if(line == null)
			throw new EOFException("Unexpected EOF while reading Dictionary file");

		return DictionaryParser.cleanLine(line);
	}

	private void readProcess(final List<Pair<Integer, String>> lines){
		try{
			LOGGER.info(Backbone.MARKER_APPLICATION, workerData.workerName + " (pass 2/2)");
			setProgress(0);

			processLines(lines);

			watch.stop();

			setProgress(100);

			LOGGER.info(Backbone.MARKER_APPLICATION, "Successfully processed dictionary file (in {})", watch.toStringMinuteSeconds());
		}
		catch(final Exception e){
			if(e instanceof ClosedChannelException || e instanceof RuntimeInterruptedException)
				LOGGER.warn("Thread interrupted");
			else
				LOGGER.error("{}", ExceptionHelper.getMessage(e));

			LOGGER.info(Backbone.MARKER_APPLICATION, "Stopped processing Dictionary file");
			LOGGER.info(Backbone.MARKER_RULE_REDUCER, "Stopped processing Dictionary file");

			cancel(true);
		}
	}

	private void processLines(final List<Pair<Integer, String>> lines){
		final int totalLines = lines.size();
		processingIndex.set(0);
		final Consumer<Pair<Integer, String>> processor = rowLine -> {
			if(isCancelled())
				throw new RuntimeInterruptedException();

			try{
				if(paused.get())
					Thread.sleep(500l);
				else{
					processingIndex.incrementAndGet();

					readLineProcessor.accept(rowLine.getValue(), rowLine.getKey());

					setProgress(getProgress(processingIndex.get(), totalLines));
				}
			}
			catch(final InterruptedException e){
				if(!isPreventExceptionRelaunch())
					throw new RuntimeException(e);
			}
			catch(final Exception e){
				String errorMessage = ExceptionHelper.getMessage(e);
				LOGGER.info(Backbone.MARKER_APPLICATION, "{}, line {}: {}", errorMessage, rowLine.getKey(), rowLine.getValue());
				LOGGER.info(Backbone.MARKER_RULE_REDUCER, "{}, line {}: {}", errorMessage, rowLine.getKey(), rowLine.getValue());

				if(!isPreventExceptionRelaunch())
					throw e;
			}
		};

		if(isParallelProcessing())
			lines.parallelStream()
				.forEach(processor);
		else
			lines.stream()
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

					if(!isPreventExceptionRelaunch())
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
			LOGGER.warn("Thread interrupted");
		else if(e != null){
			final String message = ExceptionHelper.getMessage(e);
			LOGGER.error("{}: {}", e.getClass().getSimpleName(), message);
		}
		
		LOGGER.info(Backbone.MARKER_APPLICATION, "Stopped processing Dictionary file");
		LOGGER.info(Backbone.MARKER_RULE_REDUCER, "Stopped processing Dictionary file");
		
		cancel(true);
	}

	private int getProgress(final double index, final double total){
		return Math.min((int)Math.floor((index * 100.) / total), 100);
	}

	@Override
	protected void done(){
		if(!isCancelled() && getCompleted() != null)
			getCompleted().run();
		else if(isCancelled() && getCancelled() != null)
			getCancelled().run();
	}

	public final void pause(){
		if(!isDone() && paused.compareAndSet(false, true))
			firePropertyChange("paused", false, true);
	}

	public final void resume(){
		if(!isDone() && paused.compareAndSet(true, false))
			firePropertyChange("paused", true, false);
	}

}
