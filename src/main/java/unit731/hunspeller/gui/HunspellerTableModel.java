package unit731.hunspeller.gui;

import java.util.List;


public interface HunspellerTableModel<T>{

	void setProductions(List<T> list);

	default public void clear(){
		setProductions(null);
	}

}
