package unit731.hunlinter.parsers.workers.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.Backbone;

import java.util.Objects;
import java.util.function.BiConsumer;


class WorkerThesaurus extends WorkerBase<String, Integer>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerThesaurus.class);


	public static WorkerThesaurus createReadWorker(final WorkerDataAbstract workerData, final BiConsumer<String, Integer> readLineProcessor){
		Objects.requireNonNull(readLineProcessor);

		return new WorkerThesaurus(workerData, readLineProcessor);
	}

	private WorkerThesaurus(final WorkerDataAbstract workerData, final BiConsumer<String, Integer> readLineProcessor){
		Objects.requireNonNull(workerData);
		workerData.validate();

		this.workerData = workerData;
		this.readLineProcessor = readLineProcessor;
	}

	@Override
	protected Void doInBackground(){
		prepareProcessing("Start thesaurus processing");

		try{
			//...

			finalizeProcessing("Successfully processed dictionary file");
		}
		catch(final Exception e){
			cancelWorker(e);
		}

		return null;
	}

}
