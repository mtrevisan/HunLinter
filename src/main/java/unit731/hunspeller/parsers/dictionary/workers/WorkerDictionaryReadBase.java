package unit731.hunspeller.parsers.dictionary.workers;

import java.beans.PropertyChangeListener;
import java.util.function.Consumer;
import javax.swing.SwingWorker;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.BackboneWorkerDictionaryRead;


public class WorkerDictionaryReadBase{

	private BackboneWorkerDictionaryRead worker;


	public final void createWorker(Backbone backbone, Consumer<String> body, Runnable done){
		worker = new BackboneWorkerDictionaryRead(backbone.getDictionaryFile(), backbone.getCharset(), body, done);
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
