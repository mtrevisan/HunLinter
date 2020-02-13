package unit731.hunlinter.parsers.workers.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.BiConsumer;


class WorkerThesaurus extends WorkerBase<String, Integer>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerThesaurus.class);


	public static WorkerThesaurus createReadWorker(final WorkerDataAbstract workerData, final BiConsumer<String, Integer> dataProcessor){
		Objects.requireNonNull(dataProcessor);

		return new WorkerThesaurus(workerData, dataProcessor);
	}

	private WorkerThesaurus(final WorkerDataAbstract workerData, final BiConsumer<String, Integer> dataProcessor){
		Objects.requireNonNull(workerData);

		this.workerData = workerData;
		this.readDataProcessor = dataProcessor;
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
