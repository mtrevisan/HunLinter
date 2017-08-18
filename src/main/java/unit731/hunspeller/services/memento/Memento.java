package unit731.hunspeller.services.memento;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @see <a href="https://github.com/jmcalma/MementoPattern">Memento Pattern</a>
 * 
 * @param <T>	Type for the data
 */
@AllArgsConstructor
@Getter
public class Memento<T>{

	private final T data;

}
