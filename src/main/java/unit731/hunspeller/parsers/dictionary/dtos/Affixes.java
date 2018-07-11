package unit731.hunspeller.parsers.dictionary.dtos;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public class Affixes{

	private final Set<String> terminalAffixes;
	private final Set<String> prefixes;
	private final Set<String> suffixes;

}
