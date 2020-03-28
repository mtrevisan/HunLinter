package unit731.hunlinter.workers.core;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


public class IndexDataPair<T>{

	private final int index;
	private final T data;


	public static <T> IndexDataPair<T> of(final int index, final T data){
		return new IndexDataPair<>(index, data);
	}

	private IndexDataPair(final int index, final T data){
		this.index = index;
		this.data = data;
	}

	public int getIndex(){
		return index;
	}

	public T getData(){
		return data;
	}

	@Override
	public String toString(){
		return index + ": " + data;
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final IndexDataPair<?> rhs = (IndexDataPair<?>)obj;
		return new EqualsBuilder()
			.append(index, rhs.index)
			.append(data, rhs.data)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(index)
			.append(data)
			.toHashCode();
	}

}
