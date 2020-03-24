package unit731.hunlinter.parsers.thesaurus;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;
import java.util.Objects;


public class DuplicationResult<T>{

	private final List<T> duplicates;
	private final boolean forceInsertion;


	public DuplicationResult(final List<T> duplicates, final boolean forceInsertion){
		Objects.requireNonNull(duplicates);

		this.duplicates = duplicates;
		this.forceInsertion = forceInsertion;
	}

	public List<T> getDuplicates(){
		return duplicates;
	}

	public boolean isForceInsertion(){
		return forceInsertion;
	}

	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final DuplicationResult<?> other = (DuplicationResult<?>)obj;
		return new EqualsBuilder()
			.append(duplicates, other.duplicates)
			.append(forceInsertion, other.forceInsertion)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(duplicates)
			.append(forceInsertion)
			.toHashCode();
	}

}
