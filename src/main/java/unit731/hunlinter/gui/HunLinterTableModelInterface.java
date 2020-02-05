package unit731.hunlinter.gui;

import java.util.List;


public interface HunLinterTableModelInterface<T>{

	void setProductions(List<T> list);

	default void clear(){
		setProductions(null);
	}

}
