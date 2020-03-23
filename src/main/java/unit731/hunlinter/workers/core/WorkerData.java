package unit731.hunlinter.workers.core;

import java.util.Objects;
import java.util.function.Consumer;


public class WorkerData{

	private final String workerName;

	private boolean parallelProcessing;
	private boolean relaunchException = true;

	private Runnable completed;
	private Consumer<Exception> cancelled;


	WorkerData(final String workerName){
		Objects.requireNonNull(workerName);

		this.workerName = workerName;
	}

	public final String getWorkerName(){
		return workerName;
	}

	public final WorkerData withSequentialProcessing(){
		parallelProcessing = false;
		return this;
	}

	public final WorkerData withParallelProcessing(){
		parallelProcessing = true;
		return this;
	}

	public final WorkerData withRelaunchException(){
		relaunchException = true;
		return this;
	}

	public final WorkerData withDataCompletedCallback(final Runnable completed){
		this.completed = completed;
		return this;
	}

	public final WorkerData withDataCancelledCallback(final Consumer<Exception> cancelled){
		this.cancelled = cancelled;
		return this;
	}

	final void callCompletedCallback(){
		if(completed != null)
			completed.run();
	}

	final void callCancelledCallback(final Exception exception){
		if(cancelled != null)
			cancelled.accept(exception);
	}

	final boolean isParallelProcessing(){
		return parallelProcessing;
	}

	final boolean isRelaunchException(){
		return relaunchException;
	}

}
