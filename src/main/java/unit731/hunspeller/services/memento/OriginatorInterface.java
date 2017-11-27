package unit731.hunspeller.services.memento;


/**
 * @see <a href="https://github.com/jmcalma/MementoPattern">Memento Pattern</a>
 * 
 * @param <M>	Type of memento
 */
public interface OriginatorInterface<M>{

	M createMemento();

	void restoreMemento(M memento);

}
