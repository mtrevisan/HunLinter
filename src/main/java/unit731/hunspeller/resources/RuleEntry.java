package unit731.hunspeller.resources;

import java.util.List;
import java.util.Objects;
import lombok.Getter;
import unit731.hunspeller.collections.regexptrie.RegExpTrie;


@Getter
public class RuleEntry{

	private static final String YES = "Y";


	private final boolean isSuffix;
	//cross product flag
	private final boolean combineable;
	private final List<AffixEntry> entries;
//	private final RegExpTrie<AffixEntry> prefixEntries;
//	private final RegExpTrie<AffixEntry> suffixEntries;


	public RuleEntry(boolean isSuffix, String combineable, List<AffixEntry> entries){
		Objects.nonNull(combineable);
		Objects.nonNull(entries);

		this.isSuffix = isSuffix;
		this.combineable = YES.equals(combineable);
		this.entries = entries;
	}

//	public RuleEntry(boolean isSuffix, String combineable, RegExpTrie<AffixEntry> prefixEntries, RegExpTrie<AffixEntry> suffixEntries){
//		Objects.nonNull(combineable);
//		Objects.nonNull(prefixEntries);
//		Objects.nonNull(suffixEntries);
//
//		this.isSuffix = isSuffix;
//		this.combineable = YES.equals(combineable);
//		this.prefixEntries = prefixEntries;
//		this.suffixEntries = suffixEntries;
//	}

}
