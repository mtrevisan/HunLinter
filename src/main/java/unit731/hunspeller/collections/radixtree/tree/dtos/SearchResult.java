package unit731.hunspeller.collections.radixtree.tree.dtos;

import java.io.Serializable;
import unit731.hunspeller.collections.radixtree.tree.utils.RadixTreeNode;


public class SearchResult<S, V extends Serializable>{

	/** the node found */
	private final RadixTreeNode<S, V> node;

	/** the beginning index, inclusive */
	private final int index;


	public SearchResult(RadixTreeNode<S, V> node, int index){
		this.node = node;
		this.index = index;
	}

	public RadixTreeNode<S, V> getNode(){
		return node;
	}

	public int getIndex(){
		return index;
	}

}
