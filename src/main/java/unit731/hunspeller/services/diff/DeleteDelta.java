package unit731.hunspeller.services.diff;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;


/**
 * Describes the delete-delta between original and revised texts.
 *
 * @param <T> The type of the compared elements in the 'lines'.
 */
public class DeleteDelta<T> extends AbstractDelta<T>{

	/**
	 * Creates a change delta with the two given chunks.
	 *
	 * @param original The original chunk. Must not be {@code null}.
	 * @param revised  The original chunk. Must not be {@code null}.
	 */
	public DeleteDelta(final Chunk<T> original, final Chunk<T> revised){
		super(DeltaType.DELETE, original, revised);
	}

	@Override
	public void applyTo(final List<T> target) throws PatchFailedException{
		verifyChunk(target);

		final int position = getSource().getPosition();
		final int size = getSource().size();
		for(int i = 0; i < size; i ++)
			target.remove(position);
	}

	@Override
	public void restore(final List<T> target){
		final int position = this.getTarget().getPosition();
		final List<T> lines = this.getSource().getLines();
		for(int i = 0; i < lines.size(); i ++)
			target.add(position + i, lines.get(i));
	}

	@Override
	public String toString(){
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("position", getSource().getPosition())
			.append("lines", getSource().getLines())
			.toString();
	}

}
