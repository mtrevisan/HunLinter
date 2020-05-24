package unit731.hunlinter.workers.core;

import java.util.Objects;


public class WorkerDataParser<P> extends WorkerData{

	private final P parser;

	private boolean noHeader;


	public WorkerDataParser(final String workerName, final P parser){
		super(workerName);

		Objects.requireNonNull(parser);

		this.parser = parser;
	}

	public P getParser(){
		return parser;
	}

	public final WorkerDataParser<P> withNoHeader(){
		noHeader = true;
		return this;
	}

	final boolean isNoHeader(){
		return noHeader;
	}

}
