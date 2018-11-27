package unit731.hunspeller.parsers.dictionary.workers.core;

import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.SwingWorker;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class WorkerDictionaryReadWriteBase{

	private WorkerDictionaryReadWrite worker;


	public final void createWorker(String workerName, DictionaryParser dicParser, File outputFile, BiConsumer<BufferedWriter, String> lineReader,
			Runnable completed, Runnable cancelled, ReadWriteLockable lockable){
		Objects.nonNull(dicParser);

		worker = new WorkerDictionaryReadWrite(workerName, dicParser.getDicFile(), outputFile, dicParser.getCharset(), lineReader,
			completed, cancelled, lockable);
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
