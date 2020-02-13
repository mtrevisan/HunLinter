package unit731.hunlinter.parsers.workers;

import unit731.hunlinter.parsers.workers.core.WorkerDataProject;
import unit731.hunlinter.parsers.workers.core.WorkerProject;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.Backbone;
import unit731.hunlinter.services.Packager;


public class ProjectLoaderWorker extends WorkerProject{

	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectLoaderWorker.class);


	public static final String WORKER_NAME = "Project loader";


	public ProjectLoaderWorker(final Packager packager, final Backbone backbone, final Runnable completed, final Consumer<Exception> cancelled){
		super(WorkerDataProject.createParallelPreventExceptionRelaunch(WORKER_NAME, packager, backbone));


		getWorkerData()
			.withCompletedCallback(completed)
			.withCancelledCallback(cancelled);
	}

}
