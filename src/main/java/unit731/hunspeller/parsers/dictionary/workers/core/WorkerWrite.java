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
import javax.swing.SwingWorker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.TimeWatch;


@Slf4j
public class WorkerWrite<T> extends SwingWorker<Void, Void>{

	private final String workerName;
	private final List<T> entries;
	private final File outputFile;
	private final Charset charset;
	private final BiConsumer<BufferedWriter, T> body;
	private final Runnable done;

	@Getter
	private final TimeWatch watch = TimeWatch.start();


	public WorkerWrite(String workerName, List<T> entries, File outputFile, Charset charset, BiConsumer<BufferedWriter, T> body, Runnable done){
		Objects.requireNonNull(entries);
		Objects.requireNonNull(outputFile);
		Objects.requireNonNull(charset);
		Objects.requireNonNull(body);

		this.workerName = workerName;
		this.entries = entries;
		this.outputFile = outputFile;
		this.charset = charset;
		this.body = body;
		this.done = done;
	}

	@Override
	protected Void doInBackground() throws IOException{
		log.info(Backbone.MARKER_APPLICATION, "Opening output file" + (workerName != null? " - " + workerName: StringUtils.EMPTY));

		watch.reset();

		setProgress(0);
		int writtenSoFar = 0;
		long totalSize = entries.size();
		try(BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), charset)){
			for(T entry : entries){
				body.accept(writer, entry);

				writtenSoFar ++;
				setProgress((int)((writtenSoFar * 100.) / totalSize));
			}
		}
		catch(Exception e){
			log.info(Backbone.MARKER_APPLICATION, "Stopped writing output file");

			if(e instanceof ClosedChannelException)
				log.info(Backbone.MARKER_APPLICATION, "Thread interrupted");
			else{
				String message = ExceptionService.getMessage(e);
				log.info(Backbone.MARKER_APPLICATION, "{}: {}", e.getClass().getSimpleName(), message);
			}
		}

		if(!isCancelled()){
			watch.stop();

			setProgress(100);

			log.info(Backbone.MARKER_APPLICATION, "Output file written successfully (it takes " + watch.toStringMinuteSeconds() + ")");
		}

		return null;
	}

	@Override
	protected void done(){
		if(done != null && !isCancelled())
			done.run();
	}

}
