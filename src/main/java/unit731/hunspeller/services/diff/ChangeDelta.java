package unit731.hunspeller.services.diff;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;
import java.util.Objects;


/**
 * Describes the change-delta between original and revised texts.
 *
 * @param <T> The type of the compared elements in the data 'lines'.
 */
public final class ChangeDelta<T> extends AbstractDelta<T>{

	/**
	 * Creates a change delta with the two given chunks.
	 *
	 * @param source The source chunk. Must not be {@code null}.
	 * @param target The target chunk. Must not be {@code null}.
	 */
	public ChangeDelta(final Chunk<T> source, final Chunk<T> target){
		super(DeltaType.CHANGE, source, target);

		Objects.requireNonNull(source, "source must not be null");
		Objects.requireNonNull(target, "target must not be null");
	}

	@Override
	public void applyTo(final List<T> target) throws PatchFailedException{
		verifyChunk(target);

		final int position = getSource().getPosition();
		final int size = getSource().size();
		for(int i = 0; i < size; i ++)
			target.remove(position);
		int i = 0;
		for(final T line : getTarget().getLines()){
			target.add(position + i, line);
			i ++;
		}
	}

	@Override
	public void restore(final List<T> target){
		final int position = getTarget().getPosition();
		final int size = getTarget().size();
		for(int i = 0; i < size; i ++)
			target.remove(position);
		int i = 0;
		for(final T line : getSource().getLines()){
			target.add(position + i, line);
			i ++;
		}
	}

	@Override
	public String toString(){
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("position", getSource().getPosition())
			.append("lines", getSource().getLines())
			.append("to", getTarget().getLines())
			.toString();
	}

}
