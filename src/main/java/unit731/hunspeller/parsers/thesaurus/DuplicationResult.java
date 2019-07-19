package unit731.hunspeller.parsers.thesaurus;

import java.util.List;
import java.util.Objects;


public class DuplicationResult{

	private final List<ThesaurusEntry> duplicates;
	private final boolean forceInsertion;


	public DuplicationResult(final List<ThesaurusEntry> duplicates, final boolean forceInsertion){
		Objects.requireNonNull(duplicates);

		this.duplicates = duplicates;
		this.forceInsertion = forceInsertion;
	}

	public List<ThesaurusEntry> getDuplicates(){
		return duplicates;
	}

	public boolean isForceInsertion(){
		return forceInsertion;
	}

}
