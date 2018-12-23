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
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class WorkerDictionary extends WorkerBase<String, Integer>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerDictionary.class);

	private static final int NEWLINE_SIZE = 2;


	private final AtomicInteger processingIndex = new AtomicInteger(0);

	protected boolean parallelProcessing;
	protected boolean preventExceptionRelaunch;

	private final File dicFile;
	private final File outputFile;


	public WorkerDictionary(String workerName, File dicFile, Charset charset,
			BiConsumer<String, Integer> readLineProcessor,
			Runnable completed, Runnable cancelled, ReadWriteLockable lockable){
		this(workerName, dicFile, null, charset, readLineProcessor, null, completed, cancelled, lockable);

		Objects.requireNonNull(readLineProcessor);
	}

	public WorkerDictionary(String workerName, File dicFile, File outputFile, Charset charset,
			BiConsumer<BufferedWriter, Pair<Integer, String>> writeLineProcessor,
			Runnable completed, Runnable cancelled, ReadWriteLockable lockable){
		this(workerName, dicFile, outputFile, charset, null, writeLineProcessor, completed, cancelled, lockable);

		Objects.requireNonNull(outputFile);
		Objects.requireNonNull(writeLineProcessor);
	}

	private WorkerDictionary(String workerName, File dicFile, File outputFile, Charset charset,
			BiConsumer<String, Integer> readLineProcessor,
			BiConsumer<BufferedWriter, Pair<Integer, String>> writeLineProcessor,
			Runnable completed, Runnable cancelled, ReadWriteLockable lockable){
		Objects.requireNonNull(workerName);
		Objects.requireNonNull(dicFile);
		Objects.requireNonNull(charset);
		Objects.requireNonNull(lockable);

		this.workerName = workerName;
		this.dicFile = dicFile;
		this.outputFile = outputFile;
		this.charset = charset;
		this.readLineProcessor = readLineProcessor;
		this.writeLineProcessor = writeLineProcessor;
		this.completed = completed;
		this.cancelled = cancelled;
		this.lockable = lockable;
	}

	public boolean isParallelProcessing(){
		return parallelProcessing;
	}

	public boolean isPreventExceptionRelaunch(){
		return preventExceptionRelaunch;
	}

	@Override
	protected Void doInBackground() throws IOException{
		LOGGER.info(Backbone.MARKER_APPLICATION, "Opening Dictionary file (pass 1/2)");
		setProgress(0);

		watch.reset();

		List<Pair<Integer, String>> lines = readLines();

		if(outputFile == null)
			readProcess(lines);
		else
			writeProcess(lines);

		return null;
	}

	private List<Pair<Integer, String>> readLines(){
		lockable.acquireReadLock();

		List<Pair<Integer, String>> lines = new ArrayList<>();
		long totalSize = dicFile.length();
		try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(dicFile.toPath(), charset))){
			String line = br.readLine();
			if(line == null)
				throw new EOFException("Unexpected EOF while reading Dictionary file");
			
			long readSoFar = line.getBytes(charset).length + NEWLINE_SIZE;
			
			//ignore any BOM marker on first line
			if(br.getLineNumber() == 1)
				line = FileHelper.clearBOMMarker(line);
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
		catch(Exception e){
			if(e instanceof ClosedChannelException)
				LOGGER.warn("Thread interrupted");
			else{
				String message = ExceptionHelper.getMessage(e);
				LOGGER.error("{}: {}", e.getClass().getSimpleName(), message);
			}
			
			LOGGER.info(Backbone.MARKER_APPLICATION, "Stopped processing Dictionary file");
			
			cancel(true);
		}
		finally{
			lockable.releaseReadLock();
		}
		return lines;
	}

	private void readProcess(List<Pair<Integer, String>> lines){
		try{
			LOGGER.info(Backbone.MARKER_APPLICATION, workerName + " (pass 2/2)");
			setProgress(0);

			int totalLines = lines.size();
			processingIndex.set(0);
			Consumer<Pair<Integer, String>> processor = rowLine -> {
				if(isCancelled())
					throw new RuntimeInterruptedException();

				try{
					processingIndex.incrementAndGet();

					readLineProcessor.accept(rowLine.getValue(), rowLine.getKey());

					setProgress(getProgress(processingIndex.get(), totalLines));
				}
				catch(Exception e){
					LOGGER.info(Backbone.MARKER_APPLICATION, "{} on line {}: {}", e.getMessage(), rowLine.getKey(), rowLine.getValue());

					if(!preventExceptionRelaunch)
						throw e;
				}
			};
			if(parallelProcessing)
				lines.parallelStream()
					.forEach(processor);
			else
				lines.stream()
					.forEach(processor);
			
			
			watch.stop();

			setProgress(100);

			LOGGER.info(Backbone.MARKER_APPLICATION, "Successfully processed dictionary file (it takes {})", watch.toStringMinuteSeconds());
		}
		catch(Exception e){
			if(e instanceof ClosedChannelException || e instanceof RuntimeInterruptedException)
				LOGGER.warn("Thread interrupted");
			else{
				String message = ExceptionHelper.getMessage(e);
				LOGGER.error("{}: {}", e.getClass().getSimpleName(), message);
			}

			LOGGER.info(Backbone.MARKER_APPLICATION, "Stopped processing Dictionary file");

			cancel(true);
		}
	}

	private void writeProcess(List<Pair<Integer, String>> lines){
		LOGGER.info(Backbone.MARKER_APPLICATION, workerName + " (pass 2/2)");

		setProgress(0);

		int writtenSoFar = 0;
		int totalLines = lines.size();
		try(BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), charset)){
			for(Pair<Integer, String> rowLine : lines){
				if(isCancelled())
					throw new RuntimeInterruptedException();

				try{
					writtenSoFar ++;

					writeLineProcessor.accept(writer, rowLine);

					setProgress(getProgress(writtenSoFar, totalLines));
				}
				catch(Exception e){
					LOGGER.info(Backbone.MARKER_APPLICATION, "{} on line {}: {}", e.getMessage(), rowLine.getKey(), rowLine.getValue());

					if(!preventExceptionRelaunch)
						throw e;
				}
			}
			
			
			watch.stop();

			setProgress(100);

			LOGGER.info(Backbone.MARKER_APPLICATION, "Successfully processed dictionary file (it takes {})", watch.toStringMinuteSeconds());
		}
		catch(Exception e){
			if(e instanceof ClosedChannelException)
				LOGGER.warn("Thread interrupted");
			else{
				String message = ExceptionHelper.getMessage(e);
				LOGGER.error("{}: {}", e.getClass().getSimpleName(), message);
			}

			LOGGER.info(Backbone.MARKER_APPLICATION, "Stopped processing Dictionary file");

			cancel(true);
		}
	}

	private int getProgress(double index, double total){
		return Math.min((int)Math.floor((index * 100.) / total), 100);
	}

	@Override
	protected void done(){
		if(!isCancelled() && completed != null)
			completed.run();
		else if(isCancelled() && cancelled != null)
			cancelled.run();
	}

}
