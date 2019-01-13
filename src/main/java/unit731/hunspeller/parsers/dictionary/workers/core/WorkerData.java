package unit731.hunspeller.parsers.dictionary.workers.core;

import java.util.Objects;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;


public class WorkerData{

	final String workerName;
	final DictionaryParser dicParser;
	Runnable completed;
	Runnable cancelled;

	final boolean parallelProcessing;
	final boolean preventExceptionRelaunch;


	public static final WorkerData create(String workerName, DictionaryParser dicParser){
		return new WorkerData(workerName, dicParser, false, false);
	}

	public static final WorkerData createParallel(String workerName, DictionaryParser dicParser){
		return new WorkerData(workerName, dicParser, true, false);
	}

	public static final WorkerData createParallelPreventExceptionRelaunch(String workerName, DictionaryParser dicParser){
		return new WorkerData(workerName, dicParser, true, true);
	}

	private WorkerData(String workerName, DictionaryParser dicParser, boolean parallelProcessing, boolean preventExceptionRelaunch){
		Objects.requireNonNull(workerName);
		Objects.requireNonNull(dicParser);

		this.workerName = workerName;
		this.dicParser = dicParser;
		this.parallelProcessing = parallelProcessing;
		this.preventExceptionRelaunch = preventExceptionRelaunch;
	}

	public void setCompletedCallback(Runnable completed){
		Objects.requireNonNull(completed);

		this.completed = completed;
	}

	public void setCancelledCallback(Runnable cancelled){
		Objects.requireNonNull(cancelled);

		this.cancelled = cancelled;
	}

	public void validate() throws NullPointerException{
		Objects.requireNonNull(workerName);
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(dicParser.getDicFile());
		Objects.requireNonNull(dicParser.getCharset());
	}

}
