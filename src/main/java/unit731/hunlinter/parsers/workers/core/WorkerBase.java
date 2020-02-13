package unit731.hunlinter.parsers.workers.core;

import java.io.BufferedWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import javax.swing.SwingWorker;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunlinter.services.system.TimeWatch;


public abstract class WorkerBase<S, T> extends SwingWorker<Void, Void>{

	protected WorkerDataAbstract workerData;

	protected BiConsumer<S, T> readLineProcessor;
	protected BiConsumer<BufferedWriter, Pair<Integer, S>> writeLineProcessor;

	protected Exception exception;

	protected final TimeWatch watch = TimeWatch.start();

	private final AtomicBoolean paused = new AtomicBoolean(false);


	public String getWorkerName(){
		return workerData.getWorkerName();
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
	}

}
