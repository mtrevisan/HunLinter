package unit731.hunlinter.workers;

import java.util.HashMap;
import java.util.Map;


public class WorkerManager{

	private static final Map<String, Runnable> WORKER_ON_ENDING = new HashMap<>();


	public void addWorker(final String workerName, final Runnable onEnding){
		WORKER_ON_ENDING.put(workerName, onEnding);
	}

	public void onEnd(final String workerName){
		final Runnable onEnding = WORKER_ON_ENDING.get(workerName);
		if(onEnding != null)
			onEnding.run();
	}

}
