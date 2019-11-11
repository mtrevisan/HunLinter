package unit731.hunspeller.services.diff;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Holds the information about the part of text involved in the diff process
 *
 * <p>
 * Text is represented as <code>Object[]</code> because the diff engine is capable of handling more
 * than plain ASCII. In fact, arrays or lists of any type that implements
 * {@link java.lang.Object#hashCode hashCode()} and {@link java.lang.Object#equals equals()}
 * correctly can be subject to differencing using this library.
 * </p>
 *
 * @param <T> The type of the compared elements in the 'lines'.
 */
public final class Chunk<T>{

	private final int position;
	private List<T> lines;


	/**
	 * Creates a chunk and saves a copy of affected lines
	 *
	 * @param position the start position
	 * @param lines    the affected lines
	 */
	public Chunk(final int position, final List<T> lines){
		this.position = position;
		this.lines = new ArrayList<>(lines);
	}

	/**
	 * Creates a chunk and saves a copy of affected lines
	 *
	 * @param position the start position
	 * @param lines    the affected lines
	 */
	public Chunk(final int position, final T[] lines){
		this.position = position;
		this.lines = Arrays.asList(lines);
	}

	/**
	 * Verifies that this chunk's saved text matches the corresponding text in the given sequence.
	 *
	 * @param target the sequence to verify against.
	 */
	public void verify(final List<T> target) throws PatchFailedException{
		if(position > target.size() || last() > target.size())
			throw new PatchFailedException("Incorrect Chunk: the position of chunk > target size");
		for(int i = 0; i < size(); i ++)
			if(!target.get(position + i).equals(lines.get(i)))
				throw new PatchFailedException("Incorrect Chunk: the chunk content doesn't match the target");
	}

	/**
	 * @return the start position of chunk in the text
	 */
	public int getPosition(){
		return position;
	}

	public void setLines(final List<T> lines){
		this.lines = lines;
	}

	/**
	 * @return the affected lines
	 */
	public List<T> getLines(){
		return lines;
	}

	public int size(){
		return lines.size();
	}

	/**
	 * Returns the index of the last line of the chunk.
	 */
	public int last(){
		return getPosition() + size() - 1;
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(position)
			.append(lines)
			.toHashCode();
	}

	@Override
	public boolean equals(Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final Chunk rhs = (Chunk)obj;
		return new EqualsBuilder()
			.append(position, rhs.position)
			.append(lines, rhs.lines)
			.isEquals();
	}

	@Override
	public String toString(){
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
			.append("position", position)
			.append("size", size())
			.append("lines", lines)
			.toString();
	}

}
