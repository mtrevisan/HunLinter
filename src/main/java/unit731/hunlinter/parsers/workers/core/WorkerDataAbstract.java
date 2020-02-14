package unit731.hunlinter.parsers.workers.core;

import java.util.Objects;
import java.util.function.Consumer;


public abstract class WorkerDataAbstract<T>{

	private String workerName;
	private boolean parallelProcessing;
	private boolean preventExceptionRelaunch;
	private Runnable completed;
	private Consumer<Exception> cancelled;


	WorkerDataAbstract(final String workerName){
		Objects.requireNonNull(workerName);

		this.workerName = workerName;
	}

	final String getWorkerName(){
		return workerName;
	}

	public final T withParallelProcessing(final boolean parallelProcessing){
		this.parallelProcessing = parallelProcessing;
		return (T)this;
	}

	public final T withPreventExceptionRelaunch(final boolean preventExceptionRelaunch){
		this.preventExceptionRelaunch = preventExceptionRelaunch;
		return (T)this;
	}

	public final WorkerDataAbstract<T> withDataCompletedCallback(final Runnable completed){
		this.completed = completed;
		return this;
	}

	public final WorkerDataAbstract<T> withDataCancelledCallback(final Consumer<Exception> cancelled){
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

	final boolean isPreventExceptionRelaunch(){
		return preventExceptionRelaunch;
	}

}
