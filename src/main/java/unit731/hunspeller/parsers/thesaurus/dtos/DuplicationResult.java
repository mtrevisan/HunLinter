package unit731.hunspeller.parsers.thesaurus.dtos;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;


@AllArgsConstructor
@Getter
public class DuplicationResult{

	@NonNull
	private final List<ThesaurusEntry> duplicates;
	private final boolean forcedInsertion;
	
}
