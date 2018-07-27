package unit731.hunspeller.parsers.dictionary.workers;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.charset.Charset;
import java.util.function.BiConsumer;
import javax.swing.SwingWorker;
import unit731.hunspeller.BackboneWorkerDictionaryRead;


public class AbstractWorkerDictionaryRead{

	private BackboneWorkerDictionaryRead worker;


	public final void createWorker(File dicFile, Charset charset, BiConsumer<String, Integer> body, Runnable done){
		worker = new BackboneWorkerDictionaryRead(dicFile, charset, body, done);
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
