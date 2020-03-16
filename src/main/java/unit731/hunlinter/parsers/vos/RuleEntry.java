package unit731.hunlinter.parsers.vos;

import unit731.hunlinter.parsers.enums.AffixType;

import java.util.List;
import java.util.Objects;


public class RuleEntry{

	private static final char COMBINABLE = 'Y';
	private static final char NOT_COMBINABLE = 'N';


	private final boolean suffix;
	//cross product flag
	private final boolean combinable;
	private final List<AffixEntry> entries;
//private final List<AffixEntry> prefixEntries;
//private final List<AffixEntry> suffixEntries;


	public RuleEntry(final boolean suffix, final char combinable, final List<AffixEntry> entries){
		Objects.requireNonNull(entries);

		this.suffix = suffix;
		this.combinable = (combinable == COMBINABLE);
		this.entries = entries;
	}

//public RuleEntry(boolean isSuffix, char combinable, List<AffixEntry> entries, List<AffixEntry> prefixEntries, List<AffixEntry> suffixEntries){
//	Objects.requireNonNull(combinable);
//	Objects.requireNonNull(prefixEntries);
//	Objects.requireNonNull(suffixEntries);
//
//	this.isSuffix = isSuffix;
//	this.combinable = (combinable == YES);
//	this.entries = entries;
//	this.prefixEntries = prefixEntries;
//	this.suffixEntries = suffixEntries;
//}

	public boolean isSuffix(){
		return suffix;
	}

	public boolean isCombinable(){
		return combinable;
	}

	public char combinableChar(){
		return (isCombinable()? COMBINABLE: NOT_COMBINABLE);
	}

	public AffixType getType(){
		return (isSuffix()? AffixType.SUFFIX: AffixType.PREFIX);
	}

	public List<AffixEntry> getEntries(){
		return entries;
	}

	public boolean isProductiveFor(final String word){
		for(final AffixEntry entry : entries)
			if(entry.canApplyTo(word))
				return true;
		return false;
	}

}
