package unit731.hunlinter.parsers.workers.core;

import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;

import java.util.Objects;


public class WorkerDataThesaurus extends WorkerDataAbstract{

	private final ThesaurusParser theParser;


	public static WorkerDataThesaurus create(final String workerName, final ThesaurusParser theParser){
		return new WorkerDataThesaurus(workerName, theParser, false, false);
	}

	public static WorkerDataThesaurus createPreventExceptionRelaunch(final String workerName, final ThesaurusParser theParser){
		return new WorkerDataThesaurus(workerName, theParser, false, true);
	}

	public static WorkerDataThesaurus createParallel(final String workerName, final ThesaurusParser theParser){
		return new WorkerDataThesaurus(workerName, theParser, true, false);
	}

	public static WorkerDataThesaurus createParallelPreventExceptionRelaunch(final String workerName){
		return new WorkerDataThesaurus(workerName, true, true);
	}

	public static WorkerDataThesaurus createParallelPreventExceptionRelaunch(final String workerName, final ThesaurusParser theParser){
		return new WorkerDataThesaurus(workerName, theParser, true, true);
	}

	private WorkerDataThesaurus(final String workerName, final boolean parallelProcessing, final boolean preventExceptionRelaunch){
		super(workerName, parallelProcessing, preventExceptionRelaunch);

		theParser = null;
	}

	private WorkerDataThesaurus(final String workerName, final ThesaurusParser theParser, final boolean parallelProcessing,
			final boolean preventExceptionRelaunch){
		super(workerName, parallelProcessing, preventExceptionRelaunch);

		Objects.requireNonNull(theParser);

		this.theParser = theParser;
	}

	ThesaurusParser getTheParser(){
		return theParser;
	}

}
