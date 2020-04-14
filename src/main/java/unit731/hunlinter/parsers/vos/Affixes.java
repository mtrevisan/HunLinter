package unit731.hunlinter.parsers.vos;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunlinter.services.datastructures.FixedArray;


public class Affixes{

	public static final int INDEX_PREFIXES = 0;
	public static final int INDEX_SUFFIXES = 1;
	public static final int INDEX_TERMINALS = 2;

	private final FixedArray<String> prefixes;
	private final FixedArray<String> suffixes;
	private final FixedArray<String> terminals;


	public Affixes(final FixedArray<String> prefixes, final FixedArray<String> suffixes,
			final FixedArray<String> terminals){
		this.prefixes = prefixes;
		this.suffixes = suffixes;
		this.terminals = terminals;
	}

	public FixedArray[] extractAllAffixes(final boolean reverseAffixes){
		return new FixedArray[]{
			(reverseAffixes? suffixes: prefixes),
			(reverseAffixes? prefixes: suffixes),
			terminals};
	}

	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final Affixes other = (Affixes)obj;
		return new EqualsBuilder()
			.append(prefixes, other.prefixes)
			.append(suffixes, other.suffixes)
			.append(terminals, other.terminals)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(prefixes)
			.append(suffixes)
			.append(terminals)
			.toHashCode();
	}

}
