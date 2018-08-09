package unit731.hunspeller.parsers.dictionary.workers.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


@Slf4j
public class WorkerWrite<T> extends WorkerBase<BufferedWriter, T>{

	private final List<T> entries;
	private final File outputFile;


	public WorkerWrite(String workerName, List<T> entries, File outputFile, Charset charset, BiConsumer<BufferedWriter, T> lineReader, Runnable done, ReadWriteLockable lockable){
		Objects.requireNonNull(workerName);
		Objects.requireNonNull(entries);
		Objects.requireNonNull(outputFile);
		Objects.requireNonNull(charset);
		Objects.requireNonNull(lineReader);
		Objects.requireNonNull(lockable);

		this.workerName = workerName;
		this.entries = entries;
		this.outputFile = outputFile;
		this.charset = charset;
		this.lineReader = lineReader;
		this.done = done;
		this.lockable = lockable;
	}

	@Override
	protected Void doInBackground() throws IOException{
		log.info(Backbone.MARKER_APPLICATION, "Opening output file{}", (workerName != null? " - " + workerName: StringUtils.EMPTY));

		watch.reset();

		lockable.acquireReadLock();

		setProgress(0);
		int writtenSoFar = 0;
		long totalSize = entries.size();
		try(BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), charset)){
			for(T entry : entries){
				lineReader.accept(writer, entry);

				writtenSoFar ++;
				setProgress((int)((writtenSoFar * 100.) / totalSize));
			}

			if(!isCancelled()){
				watch.stop();

				setProgress(100);

				log.info(Backbone.MARKER_APPLICATION, "Output file written successfully (it takes {})", watch.toStringMinuteSeconds());
			}
		}
		catch(Throwable t){
			log.info(Backbone.MARKER_APPLICATION, "Stopped writing output file");

			if(t instanceof ClosedChannelException)
				log.warn("Thread interrupted");
			else{
				String message = ExceptionService.getMessage(t);
				log.error(Backbone.MARKER_APPLICATION, "{}: {}", t.getClass().getSimpleName(), message);
			}
		}
		finally{
			lockable.releaseReadLock();
		}

		return null;
	}

}
