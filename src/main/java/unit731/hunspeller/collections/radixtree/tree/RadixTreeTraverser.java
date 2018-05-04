package unit731.hunspeller.collections.radixtree.tree;

import java.io.Serializable;


/**
 * An interface for implementing traversers that can traverse {@link RadixTree}.
 *
 * @param <S>	The sequence/key type
 * @param <V> the type stored in the radix tree we will visit
 */
public interface RadixTreeTraverser<S, V extends Serializable>{

	/**
	 * Traverse all the radix tree.
	 *
	 * @param wholeKey	The whole key of the node being visited
	 * @param node	The node that is being traversed
	 * @param parent	The parent of the node being traversed
	 */
	void traverse(S wholeKey, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent);

}
