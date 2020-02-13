package unit731.hunlinter.parsers.workers.core;

import unit731.hunlinter.Backbone;
import unit731.hunlinter.services.Packager;

import java.util.Objects;


public class WorkerDataProject extends WorkerDataAbstract{

	protected final Packager packager;
	protected final Backbone backbone;


	public static WorkerDataProject create(final String workerName, final Packager packager, final Backbone backbone){
		return new WorkerDataProject(workerName, packager, backbone, false, false);
	}

	public static WorkerDataProject createPreventExceptionRelaunch(final String workerName, final Packager packager, final Backbone backbone){
		return new WorkerDataProject(workerName, packager, backbone, false, true);
	}

	public static WorkerDataProject createParallel(final String workerName, final Packager packager, final Backbone backbone){
		return new WorkerDataProject(workerName, packager, backbone, true, false);
	}

	public static WorkerDataProject createParallelPreventExceptionRelaunch(final String workerName, final Packager packager, final Backbone backbone){
		return new WorkerDataProject(workerName, packager, backbone, true, true);
	}

	private WorkerDataProject(final String workerName, final Packager packager, final Backbone backbone, final boolean parallelProcessing,
			final boolean preventExceptionRelaunch){
		super(workerName, parallelProcessing, preventExceptionRelaunch);

		Objects.requireNonNull(packager);
		Objects.requireNonNull(backbone);

		this.packager = packager;
		this.backbone = backbone;
	}

}
