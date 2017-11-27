package unit731.hunspeller.services.memento;

import java.io.IOException;


/**
 * @see <a href="https://github.com/jmcalma/MementoPattern">Memento Pattern</a>
 * 
 * @param <M>	Type of memento
 */
public interface CaretakerInterface<M>{

	void pushMemento(M memento) throws IOException;

	M popMemento() throws IOException;

	boolean canUndo();

}
