package unit731.hunspeller.collections.radixtree.dtos;

import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunspeller.collections.radixtree.utils.RadixTreeNode;


public class VisitElement<S, V extends Serializable> extends TraverseElement<S, V>{

	private final RadixTreeNode<S, V> parent;


	public VisitElement(RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent, S prefix){
		super(node, prefix);

		this.parent = parent;
	}

	public RadixTreeNode<S, V> getParent(){
		return parent;
	}

	@Override
	public boolean equals(Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		VisitElement<?, ?> rhs = (VisitElement<?, ?>)obj;
		return new EqualsBuilder()
			.appendSuper(super.equals(obj))
			.append(parent, rhs.parent)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.appendSuper(super.hashCode())
			.append(parent)
			.toHashCode();
	}

}
