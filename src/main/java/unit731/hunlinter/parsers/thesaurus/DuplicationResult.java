package unit731.hunlinter.parsers.thesaurus;

import java.util.List;
import java.util.Objects;


public class DuplicationResult<T>{

	private final List<T> duplicates;
	private final boolean forceInsertion;


	public DuplicationResult(final List<T> duplicates, final boolean forceInsertion){
		Objects.requireNonNull(duplicates);

		this.duplicates = duplicates;
		this.forceInsertion = forceInsertion;
	}

	public List<T> getDuplicates(){
		return duplicates;
	}

	public boolean isForceInsertion(){
		return forceInsertion;
	}

}
