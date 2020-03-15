package unit731.hunlinter.parsers.vos;


public class Affixes{

	public static final int INDEX_PREFIXES = 0;
	public static final int INDEX_SUFFIXES = 1;
	public static final int INDEX_TERMINALS = 2;

	private final String[] prefixes;
	private final String[] suffixes;
	private final String[] terminals;


	public Affixes(final String[] prefixes, final String[] suffixes, final String[] terminals){
		this.prefixes = prefixes;
		this.suffixes = suffixes;
		this.terminals = terminals;
	}

	public String[] getTerminals(){
		return terminals;
	}

	public String[][] extractAllAffixes(final boolean reverseAffixes){
		final String[][] applyAffixes = new String[3][];
		applyAffixes[0] = (reverseAffixes? suffixes: prefixes);
		applyAffixes[1] = (reverseAffixes? prefixes: suffixes);
		applyAffixes[2] = terminals;
		return applyAffixes;
	}

}
