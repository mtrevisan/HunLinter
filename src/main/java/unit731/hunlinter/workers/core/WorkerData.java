package unit731.hunlinter.workers.core;

import java.util.Objects;
import java.util.function.Consumer;


public class WorkerData<T>{

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

	public final T withParallelProcessing(final boolean parallelProcessing){
		this.parallelProcessing = parallelProcessing;
		//noinspection unchecked
		return (T)this;
	}

	public final T withRelaunchException(final boolean relaunchException){
		this.relaunchException = relaunchException;
		//noinspection unchecked
		return (T)this;
	}

	public final WorkerData<T> withDataCompletedCallback(final Runnable completed){
		this.completed = completed;
		return this;
	}

	public final WorkerData<T> withDataCancelledCallback(final Consumer<Exception> cancelled){
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
