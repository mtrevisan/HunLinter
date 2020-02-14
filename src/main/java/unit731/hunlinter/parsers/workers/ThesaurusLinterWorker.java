package unit731.hunlinter.parsers.workers;

import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;
import unit731.hunlinter.parsers.workers.core.WorkerDataThesaurus;
import unit731.hunlinter.parsers.workers.core.WorkerThesaurus;


public class ThesaurusLinterWorker extends WorkerThesaurus{

	public static final String WORKER_NAME = "Thesaurus correctness checking";


	public ThesaurusLinterWorker(final ThesaurusParser theParser){
		super((WorkerDataThesaurus)new WorkerDataThesaurus(WORKER_NAME, theParser)
			.withParallelProcessing(true));
	}

}
