package unit731.hunlinter.parsers.workers.core;

import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;

import java.util.Objects;


public class WorkerDataThesaurus extends WorkerDataAbstract{

	private final ThesaurusParser theParser;


	public WorkerDataThesaurus(final String workerName, final ThesaurusParser theParser){
		super(workerName);

		Objects.requireNonNull(theParser);

		this.theParser = theParser;
	}

	ThesaurusParser getTheParser(){
		return theParser;
	}

}
