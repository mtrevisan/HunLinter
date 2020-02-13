package unit731.hunlinter.parsers.workers.core;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.Backbone;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.workers.exceptions.LinterException;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;


public abstract class WorkerThesaurusBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerThesaurusBase.class);


	private WorkerThesaurus worker;


	public final void createReadWorker(final WorkerDataThesaurus workerData, final BiConsumer<String, Integer> lineProcessor){
		worker = WorkerThesaurus.createReadWorker(workerData, lineProcessor);
	}

	public void addPropertyChangeListener(final PropertyChangeListener listener){
		worker.addPropertyChangeListener(listener);
	}

	public abstract String getWorkerName();

	public void execute(){
		clear();

		worker.execute();
	}

	public void executeInline(){
		clear();

		worker.doInBackground();
	}

	public void clear(){}

	public SwingWorker.StateValue getState(){
		return worker.getState();
	}

	public void pause(){
		worker.pause();
	}

	public void resume(){
		worker.resume();
	}

	public void cancel(){
		worker.cancel(true);
	}

	public boolean isCancelled(){
		return worker.isCancelled();
	}

	public boolean isDone(){
		return worker.isDone();
	}

	public void askUserToAbort(final Component parentComponent, final Runnable cancelTask, final Runnable resumeTask){
		Objects.requireNonNull(parentComponent);

		worker.pause();

		final Object[] options = {"Abort", "Cancel"};
		final int answer = JOptionPane.showOptionDialog(parentComponent,
			"Do you really want to abort the " + worker.getWorkerName() + " task?", "Warning!",
			JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
		if(answer == JOptionPane.YES_OPTION){
			worker.cancel(true);

			Optional.ofNullable(cancelTask)
				.ifPresent(Runnable::run);

			LOGGER.info(Backbone.MARKER_APPLICATION, worker.getWorkerName() + " aborted");
		}
		else if(answer == JOptionPane.NO_OPTION || answer == JOptionPane.CLOSED_OPTION){
			worker.resume();

			Optional.ofNullable(resumeTask)
				.ifPresent(Runnable::run);
		}
	}

	protected LinterException wrapException(final Exception e, final Production production){
		final StringBuffer sb = new StringBuffer(e.getMessage());
		if(production.hasProductionRules())
			sb.append(" (via ").append(production.getRulesSequence()).append(")");
		return new LinterException(sb.toString());
	}

}
