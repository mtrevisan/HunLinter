package unit731.hunspeller.collections.radixtree.tree;

import java.io.Serializable;


class TraverseElement<S, V extends Serializable>{

	final RadixTreeNode<S, V> node;
	final S prefix;


	TraverseElement(RadixTreeNode<S, V> node, S prefix){
		this.node = node;
		this.prefix = prefix;
	}
	
}
