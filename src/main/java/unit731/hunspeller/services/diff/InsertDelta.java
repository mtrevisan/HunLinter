package unit731.hunspeller.services.diff;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;


/**
 * Describes the add-delta between original and revised texts.
 *
 * @param <T> The type of the compared elements in the 'lines'.
 */
public final class InsertDelta<T> extends AbstractDelta<T>{

	/**
	 * Creates an insert delta with the two given chunks.
	 *
	 * @param original The original chunk. Must not be {@code null}.
	 * @param revised  The original chunk. Must not be {@code null}.
	 */
	public InsertDelta(final Chunk<T> original, final Chunk<T> revised){
		super(DeltaType.INSERT, original, revised);
	}

	@Override
	public void applyTo(final List<T> target) throws PatchFailedException{
		verifyChunk(target);

		final int position = this.getSource().getPosition();
		final List<T> lines = this.getTarget().getLines();
		for(int i = 0; i < lines.size(); i ++)
			target.add(position + i, lines.get(i));
	}

	@Override
	public void restore(final List<T> target){
		final int position = getTarget().getPosition();
		final int size = getTarget().size();
		for(int i = 0; i < size; i ++)
			target.remove(position);
	}

	@Override
	public String toString(){
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("position", getSource().getPosition())
			.append("lines", getTarget().getLines())
			.toString();
	}

}
