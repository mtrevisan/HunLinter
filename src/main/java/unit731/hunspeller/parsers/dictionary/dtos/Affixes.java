package unit731.hunspeller.parsers.dictionary.dtos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Affixes{

	private final String[] terminalAffixes;
	private final String[] prefixes;
	private final String[] suffixes;


	public Affixes(List<String> terminalAffixes, List<String> prefixes, List<String> suffixes){
		this.terminalAffixes = terminalAffixes.toArray(new String[terminalAffixes.size()]);
		this.prefixes = prefixes.toArray(new String[prefixes.size()]);
		this.suffixes = suffixes.toArray(new String[suffixes.size()]);
	}

	public List<String[]> extractAffixes(boolean reverse){
		List<String[]> applyAffixes = new ArrayList<>(3);
		applyAffixes.add(prefixes);
		applyAffixes.add(suffixes);
		if(reverse)
			Collections.reverse(applyAffixes);
		applyAffixes.add(terminalAffixes);
		return applyAffixes;
	}

}
