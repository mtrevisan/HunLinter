package unit731.hunlinter.parsers.vos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Affixes{

	private final List<String> prefixes;
	private final List<String> suffixes;
	private final List<String> terminalAffixes;


	public Affixes(final List<String> prefixes, final List<String> suffixes, final List<String> terminalAffixes){
		this.prefixes = prefixes;
		this.suffixes = suffixes;
		this.terminalAffixes = terminalAffixes;
	}

	public List<String> getTerminalAffixes(){
		return terminalAffixes;
	}

	public List<List<String>> extractAllAffixes(final boolean reverseAffixes){
		final List<List<String>> applyAffixes = new ArrayList<>(3);
		applyAffixes.add(prefixes);
		applyAffixes.add(suffixes);
		if(reverseAffixes)
			Collections.reverse(applyAffixes);
		applyAffixes.add(terminalAffixes);
		return applyAffixes;
	}

}
