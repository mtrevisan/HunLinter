package unit731.hunlinter.parsers.workers.core;

import java.util.Objects;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;


public class WorkerDataDictionary extends WorkerDataAbstract<WorkerDataDictionary>{

	private final DictionaryParser dicParser;


	public WorkerDataDictionary(final String workerName, final DictionaryParser dicParser){
		super(workerName);

		Objects.requireNonNull(dicParser);

		this.dicParser = dicParser;
	}

	DictionaryParser getDicParser(){
		return dicParser;
	}

}
