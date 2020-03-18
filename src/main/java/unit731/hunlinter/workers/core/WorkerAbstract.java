package unit731.hunlinter.workers.core;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.services.log.ExceptionHelper;
import unit731.hunlinter.services.system.JavaHelper;
import unit731.hunlinter.services.system.TimeWatch;
import unit731.hunlinter.workers.exceptions.LinterException;


public abstract class WorkerAbstract<T, WD extends WorkerData<WD>> extends SwingWorker<Void, Void>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerAbstract.class);


	protected final WD workerData;

	private final AtomicBoolean paused = new AtomicBoolean(false);
	private static final ReentrantLock PAUSE_LOCK = new ReentrantLock();
	private static final Condition UNPAUSE = PAUSE_LOCK.newCondition();

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


	protected void manageException(final LinterException e){
		if(JavaHelper.isInterruptedException(e))
			cancel(e);
		else if(e.getData() != null){
			final String errorMessage = ExceptionHelper.getMessage(e);
			final IndexDataPair<?> data = e.getData();
			final int index = data.getIndex();
			if(index >= 0){
				LOGGER.trace("{}, line {}: {}", errorMessage, index, data.getData());
				LOGGER.info(ParserManager.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), index, data.getData());
			}
			else{
				LOGGER.trace("{}: {}", errorMessage, data.getData());
				LOGGER.info(ParserManager.MARKER_APPLICATION, "{}: {}", e.getMessage(), data.getData());
			}
		}
		else
			e.printStackTrace();
	}

	protected void executeWriteProcess(final BiConsumer<BufferedWriter, IndexDataPair<T>> dataProcessor,
			final List<IndexDataPair<T>> lines, final File outputFile, final Charset charset){
		int writtenSoFar = 0;
		final int totalLines = lines.size();
		try(final BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), charset)){
			for(final IndexDataPair<T> line : lines){
				try{
					dataProcessor.accept(writer, line);

					setProgress(++ writtenSoFar, totalLines);

					sleepOnPause();
				}
				catch(final Exception e){
					if(!JavaHelper.isInterruptedException(e))
						LOGGER.info(ParserManager.MARKER_APPLICATION, "{}, line {}: {}", e.getMessage(), line.getIndex(),
							line.getData());

					throw e;
				}
			}

			finalizeProcessing("Successfully processed dictionary file");
		}
		catch(final RuntimeException e){
			throw e;
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
		if(!isDone() && paused.compareAndSet(true, false)){
			PAUSE_LOCK.lock();
			try{
				UNPAUSE.signal();
			}
			finally{
				PAUSE_LOCK.unlock();
			}

			firePropertyChange("paused", true, false);
		}
	}

	/**
	 * NOTE: this should be called inside `SwingWorker.doInBackground()` to allow process abortion
	 */
	protected void sleepOnPause(){
		if(paused.get()){
			PAUSE_LOCK.lock();
			try{
				try{
					UNPAUSE.await();
				}
				catch(final InterruptedException ignored){}
			}
			finally{
				PAUSE_LOCK.unlock();
			}
		}
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
