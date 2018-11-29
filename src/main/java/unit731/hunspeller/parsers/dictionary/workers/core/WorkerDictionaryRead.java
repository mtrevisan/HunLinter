package unit731.hunspeller.parsers.dictionary.workers.core;

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
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.services.ExceptionHelper;
import unit731.hunspeller.services.FileHelper;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class WorkerDictionaryRead extends WorkerBase<String, Integer>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerDictionaryRead.class);


	private final AtomicInteger processingIndex = new AtomicInteger(0);

	protected boolean preventExceptionRelaunch;

	private final File dicFile;


	public WorkerDictionaryRead(String workerName, File dicFile, Charset charset, BiConsumer<String, Integer> lineReader,
			Runnable completed, Runnable cancelled, ReadWriteLockable lockable){
		Objects.requireNonNull(workerName);
		Objects.requireNonNull(dicFile);
		Objects.requireNonNull(charset);
		Objects.requireNonNull(lineReader);
		Objects.requireNonNull(lockable);

		this.workerName = workerName;
		this.dicFile = dicFile;
		this.charset = charset;
		this.lineReader = lineReader;
		this.completed = completed;
		this.cancelled = cancelled;
		this.lockable = lockable;
	}

	public boolean isPreventExceptionRelaunch(){
		return preventExceptionRelaunch;
	}

	@Override
	protected Void doInBackground() throws IOException{
		LOGGER.info(Backbone.MARKER_APPLICATION, "Opening Dictionary file (pass 1/2)");
		setProgress(0);

		watch.reset();

		lockable.acquireReadLock();

		long totalSize = dicFile.length();
		List<Pair<Integer, String>> lines = new ArrayList<>();
		try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(dicFile.toPath(), charset))){
			String line = br.readLine();
			if(line == null)
				throw new IllegalArgumentException("Dictionary file empty");

			long readSoFar = line.getBytes(charset).length + 2;

			//ignore any BOM marker on first line
			if(br.getLineNumber() == 1)
				line = FileHelper.clearBOMMarker(line);
			if(!NumberUtils.isCreatable(line))
				throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

			while((line = br.readLine()) != null){
				readSoFar += line.getBytes(charset).length + 2;

				line = DictionaryParser.cleanLine(line);
				if(!line.isEmpty())
					lines.add(Pair.of(br.getLineNumber(), line));

				setProgress(Math.min((int)Math.ceil((readSoFar * 100.) / totalSize), 100));
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


		try{
			LOGGER.info(Backbone.MARKER_APPLICATION, workerName + " (pass 2/2)");
			setProgress(0);

			int totalLines = lines.size();
//			processingIndex.set(0);
//			for(String line : lines){
//				if(isCancelled())
//					throw new InterruptedException();
//
//				try{
//					processingIndex.incrementAndGet();
//
//					lineReader.accept(line, processingIndex.get());
//
//					setProgress(Math.min((int)Math.ceil((processingIndex.get() * 100.) / totalLines), 100));
//				}
//				catch(Exception e){
//					LOGGER.info(Backbone.MARKER_APPLICATION, "{} on line {}: {}", e.getMessage(), processingIndex.get(), line);
//
//					if(!preventExceptionRelaunch)
//						throw e;
//				}
//			}
			processingIndex.set(0);
			lines.parallelStream()
				.forEach(rowLine -> {
					if(isCancelled())
						throw new RuntimeInterruptedException();

					try{
						processingIndex.incrementAndGet();

						lineReader.accept(rowLine.getValue(), rowLine.getKey());

						setProgress(Math.min((int)Math.ceil((processingIndex.get() * 100.) / totalLines), 100));
					}
					catch(Exception e){
						LOGGER.info(Backbone.MARKER_APPLICATION, "{} on line {}: {}", e.getMessage(), rowLine.getKey(), rowLine.getValue());

						if(!preventExceptionRelaunch)
							throw e;
					}
				});


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

		return null;
	}

	@Override
	protected void done(){
		if(!isCancelled() && completed != null)
			completed.run();
		else if(isCancelled() && cancelled != null)
			cancelled.run();
	}

}
