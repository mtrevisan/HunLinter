package unit731.hunspeller.parsers.thesaurus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.github.difflib.patch.Patch;


public class MementoCaretaker{

	private final List<Patch<String>> mementos = new ArrayList<>();
	private int index;


	public void pushMemento(final Patch<String> memento) throws IOException{
		if(index < mementos.size())
			mementos.set(index, memento);
		else{
			mementos.add(memento);
			index ++;
		}
	}

	public Patch<String> popPreviousMemento() throws IOException{
		return (canUndo()? mementos.get(-- index): null);
	}

	public Patch<String> popNextMemento() throws IOException{
		return (canRedo()? mementos.get(index ++): null);
	}

	public boolean canUndo(){
		return (index > 0);
	}

	public boolean canRedo(){
		return (index < mementos.size());
	}

}
