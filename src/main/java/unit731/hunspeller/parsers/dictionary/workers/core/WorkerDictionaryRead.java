package unit731.hunspeller.parsers.dictionary.workers.core;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.ExceptionHelper;
import unit731.hunspeller.services.FileHelper;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class WorkerDictionaryRead extends WorkerBase<String, Integer>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerDictionaryRead.class);

	private final File dicFile;

	protected boolean preventExceptionRelaunch;


	public WorkerDictionaryRead(String workerName, File dicFile, Charset charset, BiConsumer<String, Integer> lineReader, Runnable done, ReadWriteLockable lockable){
		Objects.requireNonNull(workerName);
		Objects.requireNonNull(dicFile);
		Objects.requireNonNull(charset);
		Objects.requireNonNull(lineReader);
		Objects.requireNonNull(lockable);

		this.workerName = workerName;
		this.dicFile = dicFile;
		this.charset = charset;
		this.lineReader = lineReader;
		this.done = done;
		this.lockable = lockable;
	}

	public boolean isPreventExceptionRelaunch(){
		return preventExceptionRelaunch;
	}

	@Override
	protected Void doInBackground() throws IOException{
		LOGGER.info(Backbone.MARKER_APPLICATION, "Opening Dictionary file"
			+ (workerName != null? StringUtils.SPACE + HyphenationParser.EM_DASH + StringUtils.SPACE + workerName: StringUtils.EMPTY));

		watch.reset();

		lockable.acquireReadLock();

		setProgress(0);
		long totalSize = dicFile.length();
//		if(totalSize == 0l)
//			throw new IllegalArgumentException("Dictionary file empty");
//		boolean firstLine = true;
//		Files.readAllLines(dicFile.toPath(), charset)
//			.parallelStream()
//			.map(line -> {
//				if(firstLine){
//					line = FileHelper.clearBOMMarker(line);
//					if(!NumberUtils.isCreatable(line))
//						throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");
//
//					firstLine = false;
//				}
//				return line;
//			});
		try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(dicFile.toPath(), charset))){
			String line = br.readLine();
			if(line == null)
				throw new IllegalArgumentException("Dictionary file empty");

			long readSoFar = line.length();

			//ignore any BOM marker on first line
			if(br.getLineNumber() == 1)
				line = FileHelper.clearBOMMarker(line);
			if(!NumberUtils.isCreatable(line))
				throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

			while((line = br.readLine()) != null){
				readSoFar += line.length();

				line = DictionaryParser.cleanLine(line);
				if(!line.isEmpty()){
					try{
						lineReader.accept(line, br.getLineNumber());
					}
					catch(Exception e){
						LOGGER.info(Backbone.MARKER_APPLICATION, "{} on line {}: {}", e.getMessage(), br.getLineNumber(), line);

						if(!preventExceptionRelaunch)
							throw e;
					}
				}

				setProgress((int)Math.ceil((readSoFar * 100.) / totalSize));
			}

			if(!isCancelled()){
				watch.stop();

				setProgress(100);

				LOGGER.info(Backbone.MARKER_APPLICATION, "Dictionary file read successfully (it takes {})", watch.toStringMinuteSeconds());
			}
		}
		catch(Exception e){
			if(e instanceof ClosedChannelException)
				LOGGER.warn("Thread interrupted");
			else{
				String message = ExceptionHelper.getMessage(e);
				LOGGER.error("{}: {}", e.getClass().getSimpleName(), message);
			}

			LOGGER.info(Backbone.MARKER_APPLICATION, "Stopped reading Dictionary file");

			cancel(true);
		}
		finally{
			lockable.releaseReadLock();
		}

		return null;
	}

	@Override
	protected void done(){
		if(done != null)
			done.run();
	}

}
