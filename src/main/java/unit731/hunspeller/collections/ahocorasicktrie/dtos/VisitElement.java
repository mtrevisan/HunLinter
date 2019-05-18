package unit731.hunspeller.collections.ahocorasicktrie.dtos;

import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


public class VisitElement<V extends Serializable>{

	private final String key;
	private final V value;


	public VisitElement(String key, V value){
		this.value = value;
		this.key = key;
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
			.append(key, rhs.key)
			.append(value, rhs.value)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(key)
			.append(value)
			.toHashCode();
	}

}
