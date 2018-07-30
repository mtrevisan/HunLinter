package unit731.hunspeller.parsers.dictionary.workers;

import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.SwingWorker;
import unit731.hunspeller.Backbone;


public class WorkerWriteBase<T>{

	private WorkerWrite<T> worker;


	public final void createWorker(String workerName, List<T> entries, Backbone backbone, File outputFile, BiConsumer<BufferedWriter, String> body, Runnable done){
		worker = new WorkerWrite(workerName, entries, outputFile, backbone.getCharset(), body, done);
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

	public boolean isDone(){
		return worker.isDone();
	}

}
