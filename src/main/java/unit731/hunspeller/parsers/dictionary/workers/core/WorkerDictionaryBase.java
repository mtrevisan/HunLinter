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


public class WorkerDictionaryBase{

	private WorkerDictionary worker;


	public final void createReadWorker(String workerName, DictionaryParser dicParser,
			BiConsumer<String, Integer> lineProcessor,
			Runnable completed, Runnable cancelled){
		Objects.requireNonNull(dicParser);

		worker = new WorkerDictionary(workerName, dicParser.getDicFile(), dicParser.getCharset(), lineProcessor,
			completed, cancelled);
	}

	public final void createReadWorkerPreventExceptionRelaunch(String workerName, DictionaryParser dicParser,
			BiConsumer<String, Integer> lineProcessor,
			Runnable completed, Runnable cancelled){
		Objects.requireNonNull(dicParser);

		worker = new WorkerDictionary(workerName, dicParser.getDicFile(), dicParser.getCharset(), lineProcessor,
			completed, cancelled);
		worker.preventExceptionRelaunch = true;
	}

	public final void createReadParallelWorker(String workerName, DictionaryParser dicParser,
			BiConsumer<String, Integer> lineProcessor,
			Runnable completed, Runnable cancelled){
		Objects.requireNonNull(dicParser);

		worker = new WorkerDictionary(workerName, dicParser.getDicFile(), dicParser.getCharset(), lineProcessor,
			completed, cancelled);
		worker.parallelProcessing = true;
	}

	public final void createReadParallelWorkerPreventExceptionRelaunch(String workerName, DictionaryParser dicParser,
			BiConsumer<String, Integer> lineProcessor,
			Runnable completed, Runnable cancelled){
		Objects.requireNonNull(dicParser);

		worker = new WorkerDictionary(workerName, dicParser.getDicFile(), dicParser.getCharset(), lineProcessor,
			completed, cancelled);
		worker.parallelProcessing = true;
		worker.preventExceptionRelaunch = true;
	}

	public final void createReadWriteWorker(String workerName, DictionaryParser dicParser, File outputFile,
			BiConsumer<BufferedWriter, Pair<Integer, String>> lineProcessor,
			Runnable completed, Runnable cancelled){
		Objects.requireNonNull(dicParser);

		worker = new WorkerDictionary(workerName, dicParser.getDicFile(), outputFile, dicParser.getCharset(), lineProcessor,
			completed, cancelled);
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
