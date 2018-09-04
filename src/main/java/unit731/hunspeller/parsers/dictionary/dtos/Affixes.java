package unit731.hunspeller.parsers.dictionary.dtos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Affixes{

	private final String[] prefixes;
	private final String[] suffixes;
	private final String[] terminalAffixes;
	private final String[] compoundAffixes;


	public Affixes(List<String> prefixes, List<String> suffixes, List<String> terminalAffixes, List<String> compoundAffixes){
		this.prefixes = prefixes.toArray(new String[prefixes.size()]);
		this.suffixes = suffixes.toArray(new String[suffixes.size()]);
		this.terminalAffixes = terminalAffixes.toArray(new String[terminalAffixes.size()]);
		this.compoundAffixes = compoundAffixes.toArray(new String[compoundAffixes.size()]);
	}

	public List<String[]> extractAffixes(boolean reverseAffixes){
		List<String[]> applyAffixes = new ArrayList<>(3);
		applyAffixes.add(prefixes);
		applyAffixes.add(suffixes);
		if(reverseAffixes)
			Collections.reverse(applyAffixes);
		applyAffixes.add(terminalAffixes);
		applyAffixes.add(compoundAffixes);
		return applyAffixes;
	}

}
