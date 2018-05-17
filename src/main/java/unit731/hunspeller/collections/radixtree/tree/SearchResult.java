package unit731.hunspeller.collections.radixtree.tree;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public class SearchResult<S, V extends Serializable>{

	/** the node found */
	private final RadixTreeNode<S, V> node;

	/** the beginning index, inclusive */
	private final int index;

}
