package unit731.hunspeller.parsers.thesaurus;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public class DuplicationResult{

	private final List<ThesaurusEntry> duplicates;
	private final boolean forcedInsertion;
	
}
