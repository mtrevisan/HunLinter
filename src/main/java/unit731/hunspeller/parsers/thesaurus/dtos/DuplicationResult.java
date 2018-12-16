package unit731.hunspeller.parsers.thesaurus.dtos;

import java.util.List;
import java.util.Objects;


public class DuplicationResult{

	private final List<ThesaurusEntry> duplicates;
	private final boolean forcedInsertion;


	public DuplicationResult(List<ThesaurusEntry> duplicates, boolean forcedInsertion){
		Objects.requireNonNull(duplicates);

		this.duplicates = duplicates;
		this.forcedInsertion = forcedInsertion;
	}

	public List<ThesaurusEntry> getDuplicates(){
		return duplicates;
	}

	public boolean isForcedInsertion(){
		return forcedInsertion;
	}

}
