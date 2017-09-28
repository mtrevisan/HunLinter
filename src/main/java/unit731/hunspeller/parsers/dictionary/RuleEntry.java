package unit731.hunspeller.parsers.dictionary;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import unit731.hunspeller.collections.trie.Trie;


@Getter
public class RuleEntry{

	private static final String YES = "Y";


	private final boolean isSuffix;
	//cross product flag
	private final boolean combineable;
	private final List<AffixEntry> entries;
//private final Trie<String[], String, AffixEntry> prefixEntries;
//private final Trie<String[], String, AffixEntry> suffixEntries;


	public RuleEntry(boolean isSuffix, String combineable, List<AffixEntry> entries){
		Objects.requireNonNull(combineable);
		Objects.requireNonNull(entries);

		this.isSuffix = isSuffix;
		this.combineable = YES.equals(combineable);
		this.entries = entries;
	}

//public RuleEntry(boolean isSuffix, String combineable, List<AffixEntry> entries, Trie<String[], String, AffixEntry> prefixEntries, Trie<String[], String, AffixEntry> suffixEntries){
//	Objects.requireNonNull(combineable);
//	Objects.requireNonNull(prefixEntries);
//	Objects.requireNonNull(suffixEntries);
//
//	this.isSuffix = isSuffix;
//	this.combineable = YES.equals(combineable);
//	this.entries = entries;
//	this.prefixEntries = prefixEntries;
//	this.suffixEntries = suffixEntries;
//}

}
