package unit731.hunspeller.parsers.dictionary.workers.core;

import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.util.function.BiConsumer;
import javax.swing.SwingWorker;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class WorkerDictionaryReadWriteBase{

	private ReadWriteLockable lockable;
	private WorkerDictionaryReadWrite worker;


	public final void createWorker(String workerName, ReadWriteLockable lockable, DictionaryParser dicParser, File outputFile,
			BiConsumer<BufferedWriter, String> body, Runnable done){
		this.lockable = lockable;

		Runnable wrappedDone = () -> {
			lockable.releaseReadLock();

			if(done != null)
				done.run();
		};
		worker = new WorkerDictionaryReadWrite(workerName, dicParser.getDicFile(), outputFile, dicParser.getCharset(), body, wrappedDone);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener){
		worker.addPropertyChangeListener(listener);
	}

	public void execute(){
		lockable.acquireReadLock();

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
