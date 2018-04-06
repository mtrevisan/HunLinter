package unit731.hunspeller.collections.radixtree;

import lombok.Getter;



/**
 * An interface for implementing visitors that can traverse {@link RadixTree}.
 * A visitor defines how to treat a key/value pair in the radix tree, and can also return a result from the traversal.
 *
 * @param <V> the type stored in the radix tree we will visit
 * @param <R> the type used for results
 */
public abstract class RadixTreeVisitor<V, R>{

	/** An overall result from the traversal of the radix tree. */
	@Getter
	protected R result;


	public RadixTreeVisitor(R initialValue){
		result = initialValue;
	}

	/**
	 * Visits a node in a radix tree.
	 *
	 * @param key	The key of the node being visited
	 * @param value	The value of the node being visited
	 */
	public abstract void visit(String key, V value);

}
