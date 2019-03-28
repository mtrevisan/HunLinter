package unit731.hunspeller.collections.radixtree.dtos;

import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunspeller.collections.radixtree.utils.RadixTreeNode;


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

	@Override
	public boolean equals(Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		TraverseElement<?, ?> rhs = (TraverseElement<?, ?>)obj;
		return new EqualsBuilder()
			.append(node, rhs.node)
			.append(prefix, rhs.prefix)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(node)
			.append(prefix)
			.toHashCode();
	}

}
