package unit731.hunspeller.parsers.dictionary.dtos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Affixes{

	private final String[] prefixes;
	private final String[] suffixes;
	private final String[] terminalAffixes;


	public Affixes(List<String> prefixes, List<String> suffixes, List<String> terminalAffixes){
		this.prefixes = prefixes.toArray(new String[prefixes.size()]);
		this.suffixes = suffixes.toArray(new String[suffixes.size()]);
		this.terminalAffixes = terminalAffixes.toArray(new String[terminalAffixes.size()]);
	}

	public String[] getTerminalAffixes(){
		return terminalAffixes;
	}

	public List<String[]> extractAllAffixes(boolean reverseAffixes){
		List<String[]> applyAffixes = new ArrayList<>(3);
		applyAffixes.add(prefixes);
		applyAffixes.add(suffixes);
		if(reverseAffixes)
			Collections.reverse(applyAffixes);
		applyAffixes.add(terminalAffixes);
		return applyAffixes;
	}

}
