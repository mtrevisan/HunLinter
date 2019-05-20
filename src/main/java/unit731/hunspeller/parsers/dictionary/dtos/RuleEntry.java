package unit731.hunspeller.parsers.dictionary.dtos;

import java.util.List;
import java.util.Objects;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;


public class RuleEntry{

	public static final char COMBINEABLE = 'Y';
	public static final char NOT_COMBINEABLE = 'N';


	private final boolean suffix;
	//cross product flag
	private final boolean combineable;
	private final List<AffixEntry> entries;
//private final List<AffixEntry> prefixEntries;
//private final List<AffixEntry> suffixEntries;


	public RuleEntry(final boolean suffix, final char combineable, final List<AffixEntry> entries){
		Objects.requireNonNull(combineable);
		Objects.requireNonNull(entries);

		this.suffix = suffix;
		this.combineable = (combineable == COMBINEABLE);
		this.entries = entries;
	}

//public RuleEntry(boolean isSuffix, char combineable, List<AffixEntry> entries, List<AffixEntry> prefixEntries, List<AffixEntry> suffixEntries){
//	Objects.requireNonNull(combineable);
//	Objects.requireNonNull(prefixEntries);
//	Objects.requireNonNull(suffixEntries);
//
//	this.isSuffix = isSuffix;
//	this.combineable = (combineable == YES);
//	this.entries = entries;
//	this.prefixEntries = prefixEntries;
//	this.suffixEntries = suffixEntries;
//}

	public boolean isSuffix(){
		return suffix;
	}

	public boolean isCombineable(){
		return combineable;
	}

	public AffixEntry.Type getType(){
		return (isSuffix()? AffixEntry.Type.SUFFIX: AffixEntry.Type.PREFIX);
	}

	public List<AffixEntry> getEntries(){
		return entries;
	}

}
