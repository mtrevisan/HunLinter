package unit731.hunlinter.actions;

import unit731.hunlinter.workers.WorkerManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.function.Consumer;


public class ProjectLoaderAction extends AbstractAction{

	private final WorkerManager workerManager;
	private final Runnable completed;
	private final Consumer<Exception> canceled;


	public ProjectLoaderAction(final WorkerManager workerManager, final Runnable completed, final Consumer<Exception> canceled){
		super("project.load");

		Objects.requireNonNull(workerManager);
		Objects.requireNonNull(completed);
		Objects.requireNonNull(canceled);

		this.workerManager = workerManager;
		this.completed = completed;
		this.canceled = canceled;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		workerManager.createProjectLoaderWorker(completed, canceled);
	}

}
