package unit731.hunspeller.collections.ahocorasicktrie;

/**
 * Processor handles the output when hit a keyword
 * 
 * @param <V>	The type of values stored in the tree
 */
public interface Visitor<V>{

	/**
	 * Hit a keyword, you can use some code like text.substring(begin, end) to get the keyword
	 *
	 * @param value the value assigned to the keyword
	 * @return Return true for continuing the search and false for stopping it.
	 */
	boolean visit(V value);

}
