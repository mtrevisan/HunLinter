package unit731.hunlinter.parsers.workers.core;

import java.io.BufferedWriter;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import javax.swing.SwingWorker;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.Backbone;
import unit731.hunlinter.services.log.ExceptionHelper;
import unit731.hunlinter.services.system.TimeWatch;


public abstract class WorkerBase<S, T> extends SwingWorker<Void, Void>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerBase.class);


	protected WorkerDataAbstract workerData;

	protected BiConsumer<S, T> readDataProcessor;
	protected BiConsumer<BufferedWriter, Pair<Integer, S>> writeDataProcessor;

	private Exception exception;

	private final TimeWatch watch = TimeWatch.start();

	private final AtomicBoolean paused = new AtomicBoolean(false);


	public String getWorkerName(){
		return workerData.getWorkerName();
	}

	protected void prepareProcessing(final String message){
		setProgress(0);
		LOGGER.info(Backbone.MARKER_APPLICATION, message);

		watch.reset();

		exception = null;
	}

	protected void finalizeProcessing(final String message){
		watch.stop();

		setProgress(100);
		LOGGER.info(Backbone.MARKER_APPLICATION, message + " (in {})", watch.toStringMinuteSeconds());
	}

	protected void setProcessingProgress(final long index, final long total){
		setProgress(getProgress(index, total));
	}

	private int getProgress(final long index, final long total){
		return Math.min((int)Math.floor((index * 100.) / total), 100);
	}

	@Override
	protected void done(){
		if(!isCancelled())
			workerData.callCompletedCallback();
		else if(isCancelled())
			workerData.callCancelledCallback(exception);
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

	protected void waitIfPaused() throws InterruptedException{
		while(paused.get())
			Thread.sleep(500l);
	}

	public void cancel(){
		cancel(true);

		workerData.callCancelledCallback(exception);

		LOGGER.info(Backbone.MARKER_APPLICATION, "Stopped processing", new Object[]{});
	}

	protected void cancelWorker(final Exception e){
		exception = e;

		if(isInterruptedException(e))
			LOGGER.info("Thread interrupted");
		else if(e != null){
			final String message = ExceptionHelper.getMessage(e);
			LOGGER.error("{}: {}", e.getClass().getSimpleName(), message);
		}
		else
			LOGGER.error("Generic error", e);

		cancel(true);

		LOGGER.info(Backbone.MARKER_APPLICATION, "Stopped processing", new Object[]{});
	}

	private boolean isInterruptedException(final Exception exception){
		final Throwable t = (exception.getCause() != null? exception.getCause(): exception);
		return (t instanceof InterruptedException || t instanceof RuntimeInterruptedException || exception instanceof ClosedChannelException);
	}

}
