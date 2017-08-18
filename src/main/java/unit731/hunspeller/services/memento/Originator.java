package unit731.hunspeller.services.memento;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * @see <a href="https://github.com/jmcalma/MementoPattern">Memento Pattern</a>
 * 
 * @param <T>	Type for the data
 */
@NoArgsConstructor
@Getter
@Setter
public class Originator<T>{
	
	private T data;


	public Memento<T> storeInMemento(){
		return new Memento<>(data);
	}

	public void restoreFromMemento(Memento<T> memento){
		data = memento.getData();
	}

}
