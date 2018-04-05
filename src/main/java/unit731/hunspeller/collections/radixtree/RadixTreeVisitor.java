package unit731.hunspeller.collections.radixtree;


/**
 * An interface for implementing visitors that can traverse {@link RadixTree}.
 * A visitor defines how to treat a key/value pair in the radix tree, and can
 * also return a result from the traversal.
 *
 * @param <V> the type stored in the radix tree we will visit
 * @param <R> the type used for results
 */
public interface RadixTreeVisitor<V, R>{

	/**
	 * Visits a node in a radix tree.
	 *
	 * @param key the key of the node being visited
	 * @param value the value of the node being visited
	 */
	void visit(String key, V value);

	/**
	 * An overall result from the traversal of the radix tree.
	 *
	 * @return the result
	 */
	R getResult();

}
