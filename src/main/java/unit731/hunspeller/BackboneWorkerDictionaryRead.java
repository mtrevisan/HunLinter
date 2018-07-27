package unit731.hunspeller;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.SwingWorker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.TimeWatch;


@Slf4j
public class BackboneWorkerDictionaryRead extends SwingWorker<Void, Void>{

	private final File dicFile;
	private final Charset charset;
	private final BiConsumer<String, Integer> body;
	private final Runnable done;

	@Getter
	private final TimeWatch watch = TimeWatch.start();


	public BackboneWorkerDictionaryRead(File dicFile, Charset charset, BiConsumer<String, Integer> body, Runnable done){
		Objects.requireNonNull(dicFile);
		Objects.requireNonNull(charset);
		Objects.requireNonNull(body);

		this.dicFile = dicFile;
		this.charset = charset;
		this.body = body;
		this.done = done;
	}

	@Override
	protected Void doInBackground() throws IOException{
		log.info(Backbone.MARKER_APPLICATION, "Opening Dictionary file");

		watch.reset();

		setProgress(0);
		long totalSize = dicFile.length();
		try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(dicFile.toPath(), charset))){
			String line = br.readLine();
			//ignore any BOM marker on first line
			if(br.getLineNumber() == 1)
				line = FileService.clearBOMMarker(line);
			if(!NumberUtils.isCreatable(line))
				throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

			long readSoFar = line.length();
			while((line = br.readLine()) != null){
				readSoFar += line.length();

				if(!line.isEmpty()){
					int lineNumber = br.getLineNumber();
					try{
						body.accept(line, lineNumber);
					}
					catch(IllegalArgumentException e){
						log.info(Backbone.MARKER_APPLICATION, "{} on line {}: {}", e.getMessage(), lineNumber, line);
					}
				}

				setProgress((int)Math.ceil((readSoFar * 100.) / totalSize));
			}
		}
		catch(Exception e){
			log.info(Backbone.MARKER_APPLICATION, "Stopped reading Dictionary file");

			if(e instanceof ClosedChannelException)
				log.info(Backbone.MARKER_APPLICATION, "Thread interrupted");
			else{
				String message = ExceptionService.getMessage(e);
				log.info(Backbone.MARKER_APPLICATION, "{}: {}", e.getClass().getSimpleName(), message);
			}
		}

		watch.stop();

		setProgress(100);

		log.info(Backbone.MARKER_APPLICATION, "Dictionary file read successfully (it takes " + watch.toStringMinuteSeconds() + ")");

		return null;
	}

	@Override
	protected void done(){
		if(done != null && !isCancelled())
			done.run();
	}

}
