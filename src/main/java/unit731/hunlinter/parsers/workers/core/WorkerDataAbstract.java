package unit731.hunlinter.parsers.workers.core;

import java.util.Objects;
import java.util.function.Consumer;


public abstract class WorkerDataAbstract{

	private String workerName;
	private Runnable completed;
	private Consumer<Exception> cancelled;

	boolean parallelProcessing;
	boolean preventExceptionRelaunch;


	WorkerDataAbstract(final String workerName, final boolean parallelProcessing, final boolean preventExceptionRelaunch){
		Objects.requireNonNull(workerName);

		this.workerName = workerName;
		this.parallelProcessing = parallelProcessing;
		this.preventExceptionRelaunch = preventExceptionRelaunch;
	}

	final String getWorkerName(){
		return workerName;
	}

	final void callCompletedCallback(){
		if(completed != null)
			completed.run();
	}

	public final void setCompletedCallback(final Runnable completed){
		this.completed = completed;
	}

	final void callCancelledCallback(final Exception exception){
		if(cancelled != null)
			cancelled.accept(exception);
	}

	public final void setCancelledCallback(final Consumer<Exception> cancelled){
		this.cancelled = cancelled;
	}

	final boolean isParallelProcessing(){
		return parallelProcessing;
	}

	final boolean isPreventExceptionRelaunch(){
		return preventExceptionRelaunch;
	}

	abstract void validate() throws NullPointerException;

}
