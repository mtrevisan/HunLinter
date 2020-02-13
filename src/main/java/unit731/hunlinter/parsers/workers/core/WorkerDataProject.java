package unit731.hunlinter.parsers.workers.core;

import unit731.hunlinter.Backbone;
import unit731.hunlinter.services.Packager;

import java.util.Objects;


public class WorkerDataProject extends WorkerDataAbstract{

	protected final Packager packager;
	protected final Backbone backbone;


	public WorkerDataProject(final String workerName, final Packager packager, final Backbone backbone){
		super(workerName);

		Objects.requireNonNull(packager);
		Objects.requireNonNull(backbone);

		this.packager = packager;
		this.backbone = backbone;
	}

}
