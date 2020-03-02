package unit731.hunlinter.actions;

import unit731.hunlinter.workers.WorkerManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.function.Consumer;


public class ProjectLoaderAction extends AbstractAction{

	private final WorkerManager workerManager;
	private final Runnable completed;
	private final Consumer<Exception> cancelled;
	private final PropertyChangeListener propertyChangeListener;


	public ProjectLoaderAction(final WorkerManager workerManager, final Runnable completed, final Consumer<Exception> cancelled,
			final PropertyChangeListener propertyChangeListener){
		super("project.load");

		Objects.requireNonNull(workerManager);
		Objects.requireNonNull(completed);
		Objects.requireNonNull(cancelled);
		Objects.requireNonNull(propertyChangeListener);

		this.workerManager = workerManager;
		this.completed = completed;
		this.cancelled = cancelled;
		this.propertyChangeListener = propertyChangeListener;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		workerManager.createProjectLoaderWorker(
			worker -> {
				worker.addPropertyChangeListener(propertyChangeListener);
				worker.execute();
			},
			completed, cancelled);
	}

}
