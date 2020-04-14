package unit731.hunlinter.datastructures.ahocorasicktrie.dtos;


/**
 * Processor the handles the output when a keyword is hit
 *
 * @param <V>	The type of values stored in the tree
 */
public interface HitProcessor<V>{

	/**
	 * Hit a keyword.
	 * You can use some code like text.substring(begin, end) to get the keyword
	 *
	 * @param begin	The beginning index, inclusive.
	 * @param end	The ending index, exclusive.
	 * @param value	The value assigned to the keyword
	 * @return	<code>true</code> for continuing the search and <code>false</code> for stopping it.
	 */
	boolean hit(final int begin, final int end, final V value);

}
