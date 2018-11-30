package unit731.hunspeller.parsers.dictionary.workers.core;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.SwingWorker;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class WorkerDictionaryReadBase{

	private WorkerDictionaryRead reader;


	public final void createWorker(String workerName, DictionaryParser dicParser, BiConsumer<String, Integer> lineReader,
			Runnable completed, Runnable cancelled, ReadWriteLockable lockable){
		Objects.requireNonNull(dicParser);

		reader = new WorkerDictionaryRead(workerName, dicParser.getDicFile(), dicParser.getCharset(), lineReader,
			completed, cancelled, lockable);
	}

	public final void createWorkerPreventExceptionRelaunch(String workerName, DictionaryParser dicParser,
			BiConsumer<String, Integer> lineReader, Runnable completed, Runnable cancelled, ReadWriteLockable lockable){
		Objects.requireNonNull(dicParser);

		reader = new WorkerDictionaryRead(workerName, dicParser.getDicFile(), dicParser.getCharset(), lineReader,
			completed, cancelled, lockable);
		reader.preventExceptionRelaunch = true;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener){
		reader.addPropertyChangeListener(listener);
	}

	public void execute(){
		clear();

		reader.execute();
	}

	public void executeInline() throws IOException{
		clear();

		reader.doInBackground();
	}

	public void clear(){}

	public SwingWorker.StateValue getState(){
		return reader.getState();
	}

	public void cancel(){
		reader.cancel(true);
	}

	public boolean isCancelled(){
		return reader.isCancelled();
	}

	public boolean isDone(){
		return reader.isDone();
	}

}
