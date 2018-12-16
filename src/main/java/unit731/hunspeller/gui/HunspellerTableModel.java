package unit731.hunspeller.gui;

import java.util.List;


public interface HunspellerTableModel<T>{

	void setProductions(List<T> list);

	default void clear(){
		setProductions(null);
	}

}
