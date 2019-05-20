package unit731.hunspeller.parsers.dictionary.workers.core;

import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;
import javax.swing.SwingWorker;
import org.apache.commons.lang3.tuple.Pair;


public class WorkerDictionaryBase{

	private WorkerDictionary worker;


	public final void createReadWorker(final WorkerData workerData, final BiConsumer<String, Integer> lineProcessor){
		worker = WorkerDictionary.createReadWorker(workerData, lineProcessor);
	}

	public final void createWriteWorker(final WorkerData workerData, final BiConsumer<BufferedWriter, Pair<Integer, String>> lineProcessor,
			final File outputFile){
		worker = WorkerDictionary.createWriteWorker(workerData, lineProcessor, outputFile);
	}

	public void addPropertyChangeListener(final PropertyChangeListener listener){
		worker.addPropertyChangeListener(listener);
	}

	public void execute(){
		clear();

		worker.execute();
	}

	public void executeInline() throws IOException{
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

}
