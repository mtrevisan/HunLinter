package unit731.hunlinter.parsers.workers.core;

import java.util.Objects;


public class WorkerDataParser<P> extends WorkerData<WorkerDataParser<P>>{

	private final P parser;


	public WorkerDataParser(final String workerName, final P parser){
		super(workerName);

		Objects.requireNonNull(parser);

		this.parser = parser;
	}

	P getParser(){
		return parser;
	}

}
