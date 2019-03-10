package unit731.hunspeller.collections.radixtree.tree.dtos;

import java.io.Serializable;
import unit731.hunspeller.collections.radixtree.tree.utils.RadixTreeNode;


public class VisitElement<S, V extends Serializable> extends TraverseElement<S, V>{

	private final RadixTreeNode<S, V> parent;


	public VisitElement(RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent, S prefix){
		super(node, prefix);

		this.parent = parent;
	}

	public RadixTreeNode<S, V> getParent(){
		return parent;
	}

}
