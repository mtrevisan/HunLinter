package unit731.hunlinter.collections.ahocorasicktrie.dtos;


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
	boolean hit(int begin, int end, V value);

}
