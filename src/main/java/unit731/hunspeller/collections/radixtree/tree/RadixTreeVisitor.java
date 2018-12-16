package unit731.hunspeller.collections.radixtree.tree;

import java.io.Serializable;


/**
 * An interface for implementing visitors that can visit {@link RadixTree}.
 * A visitor defines how to treat a key/value pair in the radix tree, and can also return a result from the traversal.
 *
 * @param <S>	The sequence/key type
 * @param <V> the type stored in the radix tree we will visit
 * @param <R> the type used for results
 */
public abstract class RadixTreeVisitor<S, V extends Serializable, R>{

	/** An overall result from the traversal of the radix tree. */
	protected R result;


	public RadixTreeVisitor(R initialValue){
		result = initialValue;
	}

	public R getResult(){
		return result;
	}

	/**
	 * Visits a node in a radix tree.
	 *
	 * @param wholeKey	The whole key of the node being visited
	 * @param node	The node that is being visited
	 * @param parent	The parent of the node being visited
	 * @return	Whether to stop visiting the tree
	 */
	public abstract boolean visit(S wholeKey, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent);

}
