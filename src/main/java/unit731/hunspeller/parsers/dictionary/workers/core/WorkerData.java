package unit731.hunspeller.parsers.dictionary.workers.core;

import java.util.Objects;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;


public class WorkerData{

	final String workerName;
	final DictionaryParser dicParser;
	final Runnable completed;
	final Runnable cancelled;

	final boolean parallelProcessing;
	final boolean preventExceptionRelaunch;


	public static final WorkerData create(String workerName, DictionaryParser dicParser, Runnable completed){
		return new WorkerData(workerName, dicParser, completed, null, false, false);
	}

	public static final WorkerData createParallel(String workerName, DictionaryParser dicParser, Runnable completed, Runnable cancelled){
		return new WorkerData(workerName, dicParser, completed, cancelled, true, false);
	}

	public static final WorkerData createParallelPreventExceptionRelaunch(String workerName, DictionaryParser dicParser){
		return new WorkerData(workerName, dicParser, null, null, true, true);
	}

	private WorkerData(String workerName, DictionaryParser dicParser, Runnable completed, Runnable cancelled, boolean parallelProcessing,
			boolean preventExceptionRelaunch){
		Objects.requireNonNull(workerName);
		Objects.requireNonNull(dicParser);

		this.workerName = workerName;
		this.dicParser = dicParser;
		this.completed = completed;
		this.cancelled = cancelled;
		this.parallelProcessing = parallelProcessing;
		this.preventExceptionRelaunch = preventExceptionRelaunch;
	}

}
