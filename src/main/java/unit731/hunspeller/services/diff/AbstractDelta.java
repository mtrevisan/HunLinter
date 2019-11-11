package unit731.hunspeller.services.diff;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;
import java.util.Objects;


/**
 * Abstract delta between a source and a target
 *
 * @see <a href="https://github.com/java-diff-utils/java-diff-utils">java-diff-utils</a>
 */
public abstract class AbstractDelta<T>{

	private final Chunk<T> source;
	private final Chunk<T> target;
	private final DeltaType type;


	public AbstractDelta(final DeltaType type, final Chunk<T> source, final Chunk<T> target){
		Objects.requireNonNull(source);
		Objects.requireNonNull(target);
		Objects.requireNonNull(type);

		this.type = type;
		this.source = source;
		this.target = target;
	}

	public Chunk<T> getSource(){
		return source;
	}

	public Chunk<T> getTarget(){
		return target;
	}

	public DeltaType getType(){
		return type;
	}

	/**
	 * Verify the chunk of this delta, to fit the target.
	 */
	protected void verifyChunk(final List<T> target) throws PatchFailedException{
		getSource().verify(target);
	}

	public abstract void applyTo(final List<T> target) throws PatchFailedException;

	public abstract void restore(final List<T> target);

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(source)
			.append(target)
			.append(type)
			.toHashCode();
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final AbstractDelta rhs = (AbstractDelta)obj;
		return new EqualsBuilder()
			.append(source, rhs.source)
			.append(target, rhs.target)
			.append(type, rhs.type)
			.isEquals();
	}

}
