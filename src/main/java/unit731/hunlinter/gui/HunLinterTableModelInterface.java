package unit731.hunlinter.gui;

import java.util.List;


public interface HunLinterTableModelInterface<T>{

	void setInflections(List<T> list);

	default void clear(){
		setInflections(null);
	}

}
