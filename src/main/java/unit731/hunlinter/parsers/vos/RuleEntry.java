package unit731.hunlinter.parsers.vos;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunlinter.parsers.enums.AffixType;
import unit731.hunlinter.services.system.LoopHelper;
import unit731.hunlinter.services.text.SmithWatermanAlignment;

import java.util.Objects;


public class RuleEntry{

	private static final char COMBINABLE = 'Y';
	private static final char NOT_COMBINABLE = 'N';


	private final AffixType type;
	/** ID used to represent the affix */
	private final String flag;
	//cross product flag
	private final boolean combinable;
	private AffixEntry[] entries;
//private final List<AffixEntry> prefixEntries;
//private final List<AffixEntry> suffixEntries;


	public RuleEntry(final AffixType type, final String flag, final char combinable){
		Objects.requireNonNull(type);
		Objects.requireNonNull(flag);

		this.type = type;
		this.flag = flag;
		this.combinable = (combinable == COMBINABLE);
	}

	public void setEntries(final AffixEntry... entries){
		this.entries = entries;
		LoopHelper.forEach(entries, entry -> entry.setParent(this));
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

	public AffixType getType(){
		return type;
	}

	public String getFlag(){
		return flag;
	}

	public boolean isCombinable(){
		return combinable;
	}

	public char combinableChar(){
		return (isCombinable()? COMBINABLE: NOT_COMBINABLE);
	}

	public AffixEntry[] getEntries(){
		return entries;
	}

	public boolean isProductiveFor(final String word){
		return (LoopHelper.match(entries, entry -> entry.canApplyTo(word)) != null);
	}


	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final RuleEntry rhs = (RuleEntry)obj;
		return new EqualsBuilder()
			.append(type, rhs.type)
			.append(flag, rhs.flag)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(type)
			.append(flag)
			.toHashCode();
	}

}
