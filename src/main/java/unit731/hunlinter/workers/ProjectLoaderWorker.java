package unit731.hunlinter.workers;

import unit731.hunlinter.workers.core.WorkerDataProject;
import unit731.hunlinter.workers.core.WorkerProject;
import java.util.function.Consumer;
import unit731.hunlinter.Backbone;
import unit731.hunlinter.services.Packager;


public class ProjectLoaderWorker extends WorkerProject{

	public static final String WORKER_NAME = "Project loader";


	public ProjectLoaderWorker(final Packager packager, final Backbone backbone, final Runnable completed, final Consumer<Exception> cancelled){
		super(new WorkerDataProject(WORKER_NAME, packager, backbone)
			.withRelaunchException(false));


		getWorkerData()
			.withDataCompletedCallback(completed)
			.withDataCancelledCallback(cancelled);
	}

}