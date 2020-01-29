package unit731.hunlinter.parsers.workers.core;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.SwingWorker;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunlinter.services.system.TimeWatch;


public abstract class WorkerBase<S, T> extends SwingWorker<Void, Void>{

	protected WorkerData workerData;

	protected BiConsumer<S, T> readLineProcessor;
	protected BiConsumer<BufferedWriter, Pair<Integer, S>> writeLineProcessor;

	protected Exception exception;

	protected final TimeWatch watch = TimeWatch.start();

	private final AtomicBoolean paused = new AtomicBoolean(false);


	public String getWorkerName(){
		return workerData.workerName;
	}

	protected File getDicFile(){
		return workerData.dicParser.getDicFile();
	}

	protected Charset getCharset(){
		return workerData.dicParser.getCharset();
	}

	protected Runnable getCompleted(){
		return workerData.completed;
	}

	protected Consumer<Exception> getCancelled(){
		return workerData.cancelled;
	}

	protected boolean isParallelProcessing(){
		return workerData.parallelProcessing;
	}

	protected boolean isPreventExceptionRelaunch(){
		return workerData.preventExceptionRelaunch;
	}

	@Override
	protected void done(){
		if(!isCancelled() && getCompleted() != null)
			getCompleted().run();
		else if(isCancelled() && getCancelled() != null)
			getCancelled().accept(exception);
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

		if(getCancelled() != null)
			getCancelled().accept(exception);
	}

}
