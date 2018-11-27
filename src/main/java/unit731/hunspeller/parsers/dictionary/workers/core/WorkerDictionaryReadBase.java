package unit731.hunspeller.parsers.dictionary.workers.core;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.SwingWorker;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class WorkerDictionaryReadBase{

	private WorkerDictionaryRead worker;


	public final void createWorker(String workerName, DictionaryParser dicParser, BiConsumer<String, Integer> lineReader,
			Runnable completed, Runnable cancelled, ReadWriteLockable lockable){
		Objects.requireNonNull(dicParser);

		worker = new WorkerDictionaryRead(workerName, dicParser.getDicFile(), dicParser.getCharset(), lineReader,
			completed, cancelled, lockable);
	}

	public final void createWorkerPreventExceptionRelaunch(String workerName, DictionaryParser dicParser, BiConsumer<String, Integer> lineReader,
			Runnable completed, Runnable cancelled, ReadWriteLockable lockable){
		Objects.requireNonNull(dicParser);

		worker = new WorkerDictionaryRead(workerName, dicParser.getDicFile(), dicParser.getCharset(), lineReader,
			completed, cancelled, lockable);
		worker.preventExceptionRelaunch = true;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener){
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
