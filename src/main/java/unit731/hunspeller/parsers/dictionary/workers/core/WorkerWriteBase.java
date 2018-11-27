package unit731.hunspeller.parsers.dictionary.workers.core;

import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.SwingWorker;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class WorkerWriteBase<T>{

	private WorkerWrite<T> worker;


	public final void createWorker(String workerName, List<T> entries, File outputFile, Charset charset,
			BiConsumer<BufferedWriter, T> lineWriter, Runnable completed, Runnable cancelled, ReadWriteLockable lockable){
		worker = new WorkerWrite<>(workerName, entries, outputFile, charset, lineWriter, completed, cancelled, lockable);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener){
		worker.addPropertyChangeListener(listener);
	}

	public void execute(){
		worker.execute();
	}

	public SwingWorker.StateValue getState(){
		return worker.getState();
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

}
