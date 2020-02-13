package unit731.hunlinter.parsers.workers.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;


public class WorkerThesaurus extends WorkerBase<String, Integer>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerThesaurus.class);


	protected WorkerThesaurus(final WorkerDataThesaurus workerData){
		Objects.requireNonNull(workerData);

		this.workerData = workerData;
	}

	@Override
	protected Void doInBackground(){
		prepareProcessing("Start thesaurus processing");

		dataProcess();

		return null;
	}

	private void dataProcess(){
		try{
			//TODO...

			finalizeProcessing("Successfully processed thesaurus file");
		}
		catch(final Exception e){
			cancelWorker(e);
		}
	}

}
