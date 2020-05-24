package unit731.hunlinter.datastructures.ahocorasicktrie.dtos;

import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


public class VisitElement<V extends Serializable>{

	private final int nodeId;
	private final String key;
	private final V value;


	public VisitElement(final int nodeId, final String key, final V value){
		this.nodeId = nodeId;
		this.value = value;
		this.key = key;
	}

	public int getNodeId(){
		return nodeId;
	}

	public String getKey(){
		return key;
	}

	public V getValue(){
		return value;
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final VisitElement<?> rhs = (VisitElement<?>)obj;
		return new EqualsBuilder()
			.append(nodeId, rhs.nodeId)
			.append(key, rhs.key)
			.append(value, rhs.value)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(nodeId)
			.append(key)
			.append(value)
			.toHashCode();
	}

}
