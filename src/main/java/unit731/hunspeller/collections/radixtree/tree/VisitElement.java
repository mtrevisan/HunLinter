package unit731.hunspeller.collections.radixtree.tree;

import java.io.Serializable;


class VisitElement<S, V extends Serializable> extends TraverseElement<S, V>{

	final RadixTreeNode<S, V> parent;


	VisitElement(RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent, S prefix){
		super(node, prefix);

		this.parent = parent;
	}

}
