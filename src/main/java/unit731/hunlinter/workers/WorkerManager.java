package unit731.hunlinter.workers;

import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.workers.core.WorkerAbstract;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;


public class WorkerManager{

	private static final Map<String, Runnable> WORKER_ON_ENDING = new HashMap<>();


	public void addWorker(final String workerName, final Runnable onEnding){
		WORKER_ON_ENDING.put(workerName, onEnding);
	}

	public void checkAbortion(final WorkerAbstract<?, ?> worker, final Component parentComponent){
		if(worker != null && worker.getState() == SwingWorker.StateValue.STARTED){
			final String workerName = worker.getWorkerData()
				.getWorkerName();
			final Runnable onEnding = WORKER_ON_ENDING.get(workerName);
//			final Runnable resumeTask = () -> setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			GUIUtils.askUserToAbort(worker, parentComponent, onEnding, null);
		}
	}

	public void onEnd(final String workerName){
		final Runnable onEnding = WORKER_ON_ENDING.get(workerName);
		if(onEnding != null)
			onEnding.run();
	}

}
