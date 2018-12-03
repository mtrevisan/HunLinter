package unit731.hunspeller.parsers.dictionary.workers.core;

import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.SwingWorker;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class WorkerDictionaryReadWriteBase{

	private WorkerDictionaryReadWrite readerWriter;


	public final void createWorker(String workerName, DictionaryParser dicParser, File outputFile,
			BiConsumer<BufferedWriter, String> lineProcessor, Runnable completed, Runnable cancelled, ReadWriteLockable lockable){
		Objects.requireNonNull(dicParser);

		readerWriter = new WorkerDictionaryReadWrite(workerName, dicParser.getDicFile(), outputFile, dicParser.getCharset(), lineProcessor,
			completed, cancelled, lockable);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener){
		readerWriter.addPropertyChangeListener(listener);
	}

	public void execute(){
		clear();

		readerWriter.execute();
	}

	public void executeInline() throws IOException{
		clear();

		readerWriter.doInBackground();
	}

	public void clear(){}

	public SwingWorker.StateValue getState(){
		return readerWriter.getState();
	}

	public void cancel(){
		readerWriter.cancel(true);
	}

	public boolean isCancelled(){
		return readerWriter.isCancelled();
	}

	public boolean isDone(){
		return readerWriter.isDone();
	}

}
