package unit731.hunlinter.parsers.workers.core;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.channels.ClosedChannelException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import javax.swing.SwingWorker;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.Backbone;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.workers.exceptions.LinterException;
import unit731.hunlinter.services.log.ExceptionHelper;
import unit731.hunlinter.services.system.TimeWatch;


public abstract class WorkerAbstract<S, T> extends SwingWorker<Void, Void>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerAbstract.class);


	protected WorkerDataAbstract workerData;

	//read section
	protected BiConsumer<S, T> readDataProcessor;
	//write section
	protected BiConsumer<BufferedWriter, Pair<Integer, S>> writeDataProcessor;
	protected File outputFile;

	private Exception exception;

	private final TimeWatch watch = TimeWatch.start();

	private final AtomicBoolean paused = new AtomicBoolean(false);


	public final WorkerDataAbstract getWorkerData(){
		return workerData;
	}

	public final String getWorkerName(){
		return workerData.getWorkerName();
	}

	protected void setReadDataProcessor(final BiConsumer<S, T> readDataProcessor){
		this.readDataProcessor = readDataProcessor;
	}

	protected void setWriteDataProcessor(final BiConsumer<BufferedWriter, Pair<Integer, S>> writeDataProcessor, final File outputFile){
		this.writeDataProcessor = writeDataProcessor;
		if(writeDataProcessor != null){
			Objects.requireNonNull(outputFile);

			this.outputFile = outputFile;
		}
		else
			this.outputFile = null;
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

	public void executeAsynchronously(){
		execute();
	}

	public void executeSynchronously() throws Exception{
		doInBackground();
	}

	public static LinterException wrapException(final Exception e, final Production production){
		final StringBuffer sb = new StringBuffer(e.getMessage());
		if(production.hasProductionRules())
			sb.append(" (via ").append(production.getRulesSequence()).append(")");
		return new LinterException(sb.toString());
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

	/** NOTE: to be called inside `SwingWorker.doInBackground()` to allow process abortion */
	protected void waitIfPaused() throws InterruptedException{
		while(paused.get())
			Thread.sleep(500l);
	}

	/** User canceled worker */
	public void cancel(){
		cancel(true);

		workerData.callCancelledCallback(exception);
	}

	/** Worker canceled itself due to an internal exception */
	protected void cancel(final Exception e){
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

		LOGGER.info(Backbone.MARKER_APPLICATION, "Process stopped", new Object[]{});
	}

	private boolean isInterruptedException(final Exception exception){
		final Throwable t = (exception.getCause() != null? exception.getCause(): exception);
		return (t instanceof InterruptedException || t instanceof RuntimeInterruptedException || exception instanceof ClosedChannelException);
	}

}
