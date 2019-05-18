package unit731.hunspeller.collections.ahocorasicktrie.dtos;

import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunspeller.collections.ahocorasicktrie.RadixTrieNode;


public class VisitElement<V extends Serializable>{

	private RadixTrieNode node;
	private final String key;
	private final V value;


	public VisitElement(RadixTrieNode node, String key, V value){
		this.node = node;
		this.value = value;
		this.key = key;
	}

	public RadixTrieNode getNode(){
		return node;
	}

	public String getKey(){
		return key;
	}

	public V getValue(){
		return value;
	}

	@Override
	public boolean equals(Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		VisitElement<?> rhs = (VisitElement<?>)obj;
		return new EqualsBuilder()
			.append(node, rhs.node)
			.append(key, rhs.key)
			.append(value, rhs.value)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(node)
			.append(key)
			.append(value)
			.toHashCode();
	}

}
