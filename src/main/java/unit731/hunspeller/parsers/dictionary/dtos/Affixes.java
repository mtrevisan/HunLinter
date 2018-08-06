package unit731.hunspeller.parsers.dictionary.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public class Affixes{

	private final String[] terminalAffixes;
	private final String[] prefixes;
	private final String[] suffixes;

}
