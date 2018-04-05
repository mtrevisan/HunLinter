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
	 * Finds the length of the largest prefix for two character sequences.
	 *
	 * @param a character sequence
	 * @param b character sequence
	 * @return the length of largest prefix of <code>a</code> and <code>b</code>
	 * @throws IllegalArgumentException if either <code>a</code> or <code>b</code> is <code>null</code>
	 */
	public static int largestPrefixLength(CharSequence a, CharSequence b){
		int len = 0;
		for(int i = 0; i < Math.min(a.length(), b.length());  ++ i){
			if(a.charAt(i) != b.charAt(i))
				break;

			 ++ len;
		}
		return len;
	}

	/**
	 * Prints a radix tree to <code>System.out</code>.
	 *
	 * @param tree	The tree
	 * @param <V> the type of values stored in the tree
	 */
	public static <V extends Serializable> void dumpTree(RadixTree<V> tree){
		dumpTree(tree.getRoot(), StringUtils.EMPTY);
	}

	/**
	 * Prints a subtree to <code>System.out</code>.
	 *
	 * @param node the subtree
	 * @param outputPrefix prefix to be printed to output
	 */
	static <V extends Serializable> void dumpTree(RadixTreeNode<V> node, String outputPrefix){
		if(node.hasValue())
			System.out.format("%s{%s : %s}%n", outputPrefix, node.getPrefix(), node.getValue());
		else
			System.out.format("%s{%s}%n", outputPrefix, node.getPrefix(), node.getValue());

		for(RadixTreeNode<V> child : node)
			dumpTree(child, outputPrefix + "\t");
	}

}
