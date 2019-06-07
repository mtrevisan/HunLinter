package unit731.hunspeller.services;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingWorker;


public class ChainedWorkerBuilder<T, V>{

	private final List<SwingWorker<T, V>> workers = new ArrayList<>();


	public ChainedWorkerBuilder<T, V> add(SwingWorker<T, V> worker){
		workers.add(worker);
		return this;
	}

	public void execute(){
		if(!workers.isEmpty()){
			SwingWorker<T, V> current = workers.remove(0);
			current.addPropertyChangeListener(new PropertyChangeListener(){
				@Override
				public void propertyChange(PropertyChangeEvent evt){
					if("state".equals(evt.getPropertyName())){
						@SuppressWarnings("unchecked")
						SwingWorker<T, V> source = (SwingWorker<T, V>)evt.getSource();
						if(source.getState() == SwingWorker.StateValue.DONE){
							source.removePropertyChangeListener(this);

							execute();
						}
					}
				}
			});
		}
	}

}
