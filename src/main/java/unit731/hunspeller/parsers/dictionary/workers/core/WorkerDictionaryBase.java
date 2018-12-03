package unit731.hunspeller.parsers.dictionary.workers.core;

import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.SwingWorker;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class WorkerDictionaryBase{

	private WorkerDictionary reader;


	public final void createReadWorker(String workerName, DictionaryParser dicParser, BiConsumer<String, Integer> lineProcessor,
			Runnable completed, Runnable cancelled, ReadWriteLockable lockable){
		Objects.requireNonNull(dicParser);

		reader = new WorkerDictionary(workerName, dicParser.getDicFile(), dicParser.getCharset(), lineProcessor,
			completed, cancelled, lockable);
	}

	public final void createReadWorkerPreventExceptionRelaunch(String workerName, DictionaryParser dicParser,
			BiConsumer<String, Integer> lineProcessor, Runnable completed, Runnable cancelled, ReadWriteLockable lockable){
		Objects.requireNonNull(dicParser);

		reader = new WorkerDictionary(workerName, dicParser.getDicFile(), dicParser.getCharset(), lineProcessor,
			completed, cancelled, lockable);
		reader.preventExceptionRelaunch = true;
	}

	public final void createReadParallelWorker(String workerName, DictionaryParser dicParser, BiConsumer<String, Integer> lineProcessor,
			Runnable completed, Runnable cancelled, ReadWriteLockable lockable){
		Objects.requireNonNull(dicParser);

		reader = new WorkerDictionary(workerName, dicParser.getDicFile(), dicParser.getCharset(), lineProcessor,
			completed, cancelled, lockable);
		reader.parallelProcessing = true;
	}

	public final void createReadParallelWorkerPreventExceptionRelaunch(String workerName, DictionaryParser dicParser,
			BiConsumer<String, Integer> lineProcessor, Runnable completed, Runnable cancelled, ReadWriteLockable lockable){
		Objects.requireNonNull(dicParser);

		reader = new WorkerDictionary(workerName, dicParser.getDicFile(), dicParser.getCharset(), lineProcessor,
			completed, cancelled, lockable);
		reader.parallelProcessing = true;
		reader.preventExceptionRelaunch = true;
	}

	public final void createReadWriteWorker(String workerName, DictionaryParser dicParser, File outputFile,
			BiConsumer<BufferedWriter, Pair<Integer, String>> lineProcessor, Runnable completed, Runnable cancelled, ReadWriteLockable lockable){
		Objects.requireNonNull(dicParser);

		reader = new WorkerDictionary(workerName, dicParser.getDicFile(), outputFile, dicParser.getCharset(), lineProcessor,
			completed, cancelled, lockable);
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
