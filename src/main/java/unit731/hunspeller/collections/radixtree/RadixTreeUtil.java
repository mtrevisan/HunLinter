package unit731.hunspeller.collections.radixtree;

import java.io.Serializable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;


/**
 * Radix tree utility functions.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RadixTreeUtil{

	/**
	 * Prints a radix tree to <code>System.out</code>.
	 *
	 * @param tree	The tree
	 * @param <V>	The type of values stored in the tree
	 */
	public static <V extends Serializable> void dumpTree(RadixTree<V> tree){
		dumpTree(tree.getRoot(), StringUtils.EMPTY);
	}

	/**
	 * Prints a subtree to <code>System.out</code>.
	 *
	 * @param node	The subtree
	 * @param outputPrefix	Prefix to be printed to output
	 */
	private static <V extends Serializable> void dumpTree(RadixTreeNode<V> node, String outputPrefix){
		System.out.format((node.hasValue()? "%s{%s : %s}%n": "%s{%s}%n"), outputPrefix, node.getKey(), node.getValue());

		for(RadixTreeNode<V> child : node)
			dumpTree(child, outputPrefix + "\t");
	}

}
