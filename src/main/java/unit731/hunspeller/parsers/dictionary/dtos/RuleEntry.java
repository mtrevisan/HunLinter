package unit731.hunspeller.parsers.dictionary.dtos;

import unit731.hunspeller.parsers.dictionary.valueobjects.AffixEntry;
import java.util.List;
import java.util.Objects;
import lombok.Getter;


@Getter
public class RuleEntry{

	private static final char YES = 'Y';


	private final boolean isSuffix;
	//cross product flag
	private final boolean combineable;
	private final List<AffixEntry> entries;
//private final List<AffixEntry> prefixEntries;
//private final List<AffixEntry> suffixEntries;


	public RuleEntry(boolean isSuffix, char combineable, List<AffixEntry> entries){
		Objects.requireNonNull(combineable);
		Objects.requireNonNull(entries);

		this.isSuffix = isSuffix;
		this.combineable = (combineable == YES);
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

}
