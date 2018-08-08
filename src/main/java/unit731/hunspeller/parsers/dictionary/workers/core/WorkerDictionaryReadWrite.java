package unit731.hunspeller.parsers.dictionary.workers.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Objects;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


@Slf4j
public class WorkerDictionaryReadWrite extends WorkerBase<BufferedWriter, String>{

	private final File dicFile;
	private final File outputFile;


	public WorkerDictionaryReadWrite(String workerName, File dicFile, File outputFile, Charset charset, BiConsumer<BufferedWriter, String> lineReader, Runnable done, ReadWriteLockable lockable){
		Objects.requireNonNull(workerName);
		Objects.requireNonNull(dicFile);
		Objects.requireNonNull(outputFile);
		Objects.requireNonNull(charset);
		Objects.requireNonNull(lineReader);
		Objects.requireNonNull(lockable);

		this.workerName = workerName;
		this.dicFile = dicFile;
		this.outputFile = outputFile;
		this.charset = charset;
		this.lineReader = lineReader;
		this.done = done;
		this.lockable = lockable;
	}

	@Override
	protected Void doInBackground() throws IOException{
		log.info(Backbone.MARKER_APPLICATION, "Opening Dictionary file"
			+ (workerName != null? StringUtils.SPACE + HyphenationParser.EM_DASH + StringUtils.SPACE + workerName: StringUtils.EMPTY));

		watch.reset();

		lockable.acquireReadLock();

		setProgress(0);
		long totalSize = dicFile.length();
		try(
				LineNumberReader br = new LineNumberReader(Files.newBufferedReader(dicFile.toPath(), charset));
				BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), charset);
			){
			String line = br.readLine();
			if(line == null)
				throw new IllegalArgumentException("Dictionary file empty");

			//ignore any BOM marker on first line
			if(br.getLineNumber() == 1)
				line = FileService.clearBOMMarker(line);
			if(!NumberUtils.isCreatable(line))
				throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

			long readSoFar = line.length();
			while((line = br.readLine()) != null){
				readSoFar += line.length();

				line = DictionaryParser.cleanLine(line);
				if(!line.isEmpty()){
					try{
						lineReader.accept(writer, line);
					}
					catch(Exception e){
						log.info(Backbone.MARKER_APPLICATION, "{} on line {}: {}", e.getMessage(), br.getLineNumber(), line);
						
						throw e;
					}
				}

				setProgress((int)Math.ceil((readSoFar * 100.) / totalSize));
			}

			if(!isCancelled()){
				watch.stop();

				setProgress(100);

				log.info(Backbone.MARKER_APPLICATION, "Dictionary file read successfully (it takes " + watch.toStringMinuteSeconds() + ")");
			}
		}
		catch(Exception e){
			if(e instanceof ClosedChannelException)
				log.warn("Thread interrupted");
			else{
				String message = ExceptionService.getMessage(e);
				log.error("{}: {}", e.getClass().getSimpleName(), message);
			}

			log.info(Backbone.MARKER_APPLICATION, "Stopped reading Dictionary file");
		}
		finally{
			lockable.releaseReadLock();
		}

		return null;
	}

}
