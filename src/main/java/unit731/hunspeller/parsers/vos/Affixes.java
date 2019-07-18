package unit731.hunspeller.parsers.vos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Affixes{

	private final String[] prefixes;
	private final String[] suffixes;
	private final String[] terminalAffixes;


	public Affixes(final List<String> prefixes, final List<String> suffixes, final List<String> terminalAffixes){
		this.prefixes = prefixes.toArray(new String[0]);
		this.suffixes = suffixes.toArray(new String[0]);
		this.terminalAffixes = terminalAffixes.toArray(new String[0]);
	}

	public String[] getTerminalAffixes(){
		return terminalAffixes;
	}

	public List<String[]> extractAllAffixes(final boolean reverseAffixes){
		final List<String[]> applyAffixes = new ArrayList<>(3);
		applyAffixes.add(prefixes);
		applyAffixes.add(suffixes);
		if(reverseAffixes)
			Collections.reverse(applyAffixes);
		applyAffixes.add(terminalAffixes);
		return applyAffixes;
	}

}