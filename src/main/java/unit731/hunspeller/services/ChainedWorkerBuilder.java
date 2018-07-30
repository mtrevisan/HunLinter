package unit731.hunspeller.services;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingWorker;


public class ChainedWorkerBuilder<T, V>{

	private final List<SwingWorker<T, V>> workers = new ArrayList<>();
	private volatile SwingWorker<T, V> current;


	public ChainedWorkerBuilder add(SwingWorker<T, V> worker){
		workers.add(worker);
		return this;
	}

	public void execute(){
		if(!workers.isEmpty()){
			current = workers.remove(0);
			current.addPropertyChangeListener(new PropertyChangeListener(){
				@Override
				public void propertyChange(PropertyChangeEvent evt){
					if("state".equals(evt.getPropertyName())){
						SwingWorker<T, V> source = (SwingWorker<T, V>)evt.getSource();
						switch(source.getState()){
							case DONE:
								source.removePropertyChangeListener(this);

								execute();
						}
					}
				}
			});
		}
	}

}
