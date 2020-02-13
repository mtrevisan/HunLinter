package unit731.hunlinter.parsers.workers.core;

import java.util.Objects;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;


public class WorkerDataDictionary extends WorkerDataAbstract{

	private final DictionaryParser dicParser;


	public static WorkerDataDictionary create(final String workerName, final DictionaryParser dicParser){
		return new WorkerDataDictionary(workerName, dicParser, false, false);
	}

	public static WorkerDataDictionary createPreventExceptionRelaunch(final String workerName, final DictionaryParser dicParser){
		return new WorkerDataDictionary(workerName, dicParser, false, true);
	}

	public static WorkerDataDictionary createParallel(final String workerName, final DictionaryParser dicParser){
		return new WorkerDataDictionary(workerName, dicParser, true, false);
	}

	public static WorkerDataDictionary createParallelPreventExceptionRelaunch(final String workerName){
		return new WorkerDataDictionary(workerName, true, true);
	}

	public static WorkerDataDictionary createParallelPreventExceptionRelaunch(final String workerName, final DictionaryParser dicParser){
		return new WorkerDataDictionary(workerName, dicParser, true, true);
	}

	private WorkerDataDictionary(final String workerName, final boolean parallelProcessing, final boolean preventExceptionRelaunch){
		super(workerName, parallelProcessing, preventExceptionRelaunch);

		dicParser = null;
	}

	private WorkerDataDictionary(final String workerName, final DictionaryParser dicParser, final boolean parallelProcessing,
			final boolean preventExceptionRelaunch){
		super(workerName, parallelProcessing, preventExceptionRelaunch);

		Objects.requireNonNull(dicParser);

		this.dicParser = dicParser;
	}

	DictionaryParser getDicParser(){
		return dicParser;
	}

	@Override
	public void validate() throws NullPointerException{
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(dicParser.getDicFile());
		Objects.requireNonNull(dicParser.getCharset());
	}

}
