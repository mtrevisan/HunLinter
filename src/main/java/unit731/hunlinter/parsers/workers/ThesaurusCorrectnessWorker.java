package unit731.hunlinter.parsers.workers;

import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;
import unit731.hunlinter.parsers.workers.core.WorkerData;
import unit731.hunlinter.parsers.workers.core.WorkerDictionaryBase;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.function.BiConsumer;


public class ThesaurusCorrectnessWorker extends WorkerDictionaryBase{

	public static final String WORKER_NAME = "Thesaurus correctness checking";

	private static final MessageFormat WORD_IS_NOT_SYLLABABLE = new MessageFormat("Word {0} ({1}) is not syllabable");


	public ThesaurusCorrectnessWorker(final ThesaurusParser theParser){
		Objects.requireNonNull(theParser);

		//TODO pass theParser.getSynonymsDictionary() and check for correctness
		final BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			//TODO
		};
		final WorkerData data = WorkerData.createParallelPreventExceptionRelaunch(WORKER_NAME, dicParser);
		createReadWorker(data, lineProcessor);
	}

	@Override
	public String getWorkerName(){
		return WORKER_NAME;
	}

}
