package unit731.hunspeller.parsers.dictionary.workers.core;

import java.beans.PropertyChangeListener;
import java.util.function.BiConsumer;
import javax.swing.SwingWorker;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.services.ReadWriteLockable;


public class WorkerDictionaryReadBase{

	private ReadWriteLockable lockable;
	private WorkerDictionaryRead worker;


	public final void createWorker(String workerName, ReadWriteLockable lockable, DictionaryParser dicParser, BiConsumer<String, Integer> body,
			Runnable done){
		this.lockable = lockable;

		Runnable wrappedDone = () -> {
			lockable.releaseReadLock();

			if(done != null)
				done.run();
		};
		worker = new WorkerDictionaryRead(workerName, dicParser.getDicFile(), dicParser.getCharset(), body, wrappedDone);
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
