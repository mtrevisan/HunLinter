package unit731.hunlinter.workers.core;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.swing.SwingWorker;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.services.log.ExceptionHelper;
import unit731.hunlinter.services.system.JavaHelper;
import unit731.hunlinter.services.system.TimeWatch;


public abstract class WorkerAbstract<T, WD extends WorkerData<WD>> extends SwingWorker<Void, Void>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerAbstract.class);


	protected final WD workerData;

	private final AtomicInteger processingIndex = new AtomicInteger(0);

	private final AtomicBoolean paused = new AtomicBoolean(false);

	private final TimeWatch watch = TimeWatch.start();

	private Function<?, ?> processor;


	WorkerAbstract(final WD workerData){
		Objects.requireNonNull(workerData);

		this.workerData = workerData;
	}

	public final void setProcessor(final Function<?, ?> processor){
		this.processor = processor;
	}

	public final WorkerData<WD> getWorkerData(){
		return workerData;
	}

	@Override
	protected Void doInBackground(){
		try{
			processor.apply(null);
		}
		catch(final Exception e){
			cancel(e);
		}
		return null;
	}

	protected Void prepareProcessing(final String message){
		setProgress(0);
		LOGGER.info(ParserManager.MARKER_APPLICATION, message);

		watch.reset();
		return null;
	}

	protected void finalizeProcessing(final String message){
		watch.stop();

		setProgress(100);
		LOGGER.info(ParserManager.MARKER_APPLICATION, message + " (in {})", watch.toStringMinuteSeconds());
	}

	public void executeSynchronously(){
		doInBackground();
	}


	protected void executeReadProcessNoIndex(final Consumer<T> dataProcessor, final List<T> entries){
		try{
			final int totalEntries = entries.size();
			processingIndex.set(0);

			final Consumer<T> innerProcessor = createInnerProcessorNoIndex(dataProcessor, totalEntries);
			final Stream<T> stream = (workerData.isParallelProcessing()? entries.parallelStream(): entries.stream());
			stream.forEach(innerProcessor);
		}
		catch(final Exception e){
			cancel(e);
		}
	}

	private Consumer<T> createInnerProcessorNoIndex(final Consumer<T> dataProcessor, final int totalData){
		return data -> {
			try{
				dataProcessor.accept(data);

				setProgress(processingIndex.incrementAndGet(), totalData);

				sleepOnPause();
			}
			catch(final Exception e){
				if(!JavaHelper.isInterruptedException(e)){
					final String errorMessage = ExceptionHelper.getMessage(e);
					LOGGER.trace("{}: {}", errorMessage, data);
					LOGGER.info(ParserManager.MARKER_APPLICATION, "{}: {}", e.getMessage(), data);
				}

				if(workerData.isRelaunchException())
					throw new RuntimeException(e);
			}
		};
	}

	protected void executeReadProcess(final BiConsumer<Integer, T> dataProcessor, final List<Pair<Integer, T>> entries){
		try{
			final int totalEntries = entries.size();
			processingIndex.set(0);

			final Consumer<Pair<Integer, T>> innerProcessor = createInnerProcessor(dataProcessor, totalEntries);
			final Stream<Pair<Integer, T>> stream = (workerData.isParallelProcessing()? entries.parallelStream(): entries.stream());
			stream.forEach(innerProcessor);
		}
		catch(final Exception e){
			cancel(e);
		}
	}

	private Consumer<Pair<Integer, T>> createInnerProcessor(final BiConsumer<Integer, T> dataProcessor, final int totalData){
		return data -> {
			try{
				dataProcessor.accept(data.getKey(), data.getValue());

				setProgress(processingIndex.incrementAndGet(), totalData);

				sleepOnPause();
			}
			catch(final Exception e){
				if(!JavaHelper.isInterruptedException(e)){
					final String errorMessage = ExceptionHelper.getMessage(e);
					LOGGER.trace("{}, line {}: {}", errorMessage, data.getKey(), data.getValue());
					LOGGER.info(ParserManager.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), data.getKey(), data.getValue());
				}

				if(workerData.isRelaunchException())
					throw new RuntimeException(e);
			}
		};
	}

	protected void executeWriteProcess(final BiConsumer<BufferedWriter, Pair<Integer, String>> dataProcessor,
			final List<Pair<Integer, String>> lines, final File outputFile, final Charset charset){
		int writtenSoFar = 0;
		final int totalLines = lines.size();
		try(final BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), charset)){
			for(final Pair<Integer, String> line : lines){
				try{
					writtenSoFar ++;

					dataProcessor.accept(writer, line);

					setProgress(writtenSoFar, totalLines);

					sleepOnPause();
				}
				catch(final Exception e){
					if(!JavaHelper.isInterruptedException(e))
						LOGGER.info(ParserManager.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), line.getKey(), line.getValue());

					throw e;
				}
			}

			finalizeProcessing("Successfully processed dictionary file");
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}


	protected void setProgress(final long index, final long total){
		final int progress = calculateProgress(index, total);
		setProgress(progress);
	}

	private int calculateProgress(final long index, final long total){
		return Math.min((int)Math.floor((index * 100.) / total), 100);
	}

	@Override
	protected void done(){
		if(!isCancelled())
			workerData.callCompletedCallback();
	}

	public final void pause(){
		if(!isDone() && paused.compareAndSet(false, true))
			firePropertyChange("paused", false, true);
	}

	public final boolean isPaused(){
		return paused.get();
	}

	public final void resume(){
		if(!isDone() && paused.compareAndSet(true, false))
			firePropertyChange("paused", true, false);
	}

	/**
	 * NOTE: this souble be called inside `SwingWorker.doInBackground()` to allow process abortion
	 *
	 * @throws	InterruptedException	In case of interrupted thread
	 */
	protected void sleepOnPause() throws InterruptedException{
		while(paused.get())
			Thread.sleep(500l);
	}

	/**
	 * Worker cancelled itself due to an internal exception
	 *
	 * @param exception	Exception that causes the cancellation
	 */
	protected void cancel(final Exception exception){
		if(!JavaHelper.isInterruptedException(exception)){
			LOGGER.error(exception != null? ExceptionHelper.getMessage(exception): "Generic error");

			cancel(true);

			LOGGER.info(ParserManager.MARKER_APPLICATION, "Process {} stopped with error", workerData.getWorkerName());

			workerData.callCancelledCallback(exception);
		}
	}

	/** User cancelled worker */
	public void cancel(){
		cancel(true);

		LOGGER.info(ParserManager.MARKER_APPLICATION, "Process {} aborted", workerData.getWorkerName());

		workerData.callCancelledCallback(null);
	}

}
