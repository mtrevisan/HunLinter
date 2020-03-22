package unit731.hunlinter.workers.core;

import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.services.Packager;

import java.util.Objects;


public class WorkerDataProject extends WorkerData{

	private final Packager packager;
	private final ParserManager parserManager;


	public WorkerDataProject(final String workerName, final Packager packager, final ParserManager parserManager){
		super(workerName);

		Objects.requireNonNull(packager);
		Objects.requireNonNull(parserManager);

		this.packager = packager;
		this.parserManager = parserManager;
	}

	Packager getPackager(){
		return packager;
	}

	ParserManager getParserManager(){
		return parserManager;
	}

}
