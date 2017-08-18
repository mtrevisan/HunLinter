package unit731.hunspeller.interfaces;

import java.util.List;


public interface Resultable{

	void printResultLine(String chunk);

	default void printResultLine(List<String> chunks){
		chunks.forEach(this::printResultLine);
	}

}
