package unit731.hunspeller.collections.radixtree.tree.dtos;

import java.io.Serializable;
import unit731.hunspeller.collections.radixtree.tree.utils.RadixTreeNode;


public class TraverseElement<S, V extends Serializable>{

	private final RadixTreeNode<S, V> node;
	private final S prefix;


	public TraverseElement(RadixTreeNode<S, V> node, S prefix){
		this.node = node;
		this.prefix = prefix;
	}

	public RadixTreeNode<S, V> getNode(){
		return node;
	}

	public S getPrefix(){
		return prefix;
	}

}
