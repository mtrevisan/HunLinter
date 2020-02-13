package unit731.hunlinter.parsers.workers;

import unit731.hunlinter.parsers.workers.core.WorkerDataDictionary;
import unit731.hunlinter.parsers.workers.core.WorkerDataProject;
import unit731.hunlinter.parsers.workers.core.WorkerProject;
import java.util.function.Consumer;
import unit731.hunlinter.Backbone;
import unit731.hunlinter.services.Packager;


public class ProjectLoaderWorker extends WorkerProject{

	public static final String WORKER_NAME = "Project loader";


	public ProjectLoaderWorker(final Packager packager, final Backbone backbone, final Runnable completed, final Consumer<Exception> cancelled){
		super((WorkerDataProject)new WorkerDataProject(WORKER_NAME, packager, backbone)
			.withParallelProcessing(true)
			.withPreventExceptionRelaunch(true));


		getWorkerData()
			.withCompletedCallback(completed)
			.withCancelledCallback(cancelled);
	}

}
