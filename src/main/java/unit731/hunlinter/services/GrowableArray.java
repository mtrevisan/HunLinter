package unit731.hunlinter.services;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.lang.reflect.Array;
import java.util.Arrays;


public class GrowableArray<T>{

	private static final float GROWTH_DEFAULT = 1.2f;


	public T[] data;
	public int limit;
	private final float growthRate;


	public static <T> GrowableArray<T> createExact(final Class<T> cl, final int size){
		return new GrowableArray<>(cl, size, GROWTH_DEFAULT);
	}

	public GrowableArray(final Class<T> cl, final float growthRate){
		this(cl, 0, growthRate);
	}

	public GrowableArray(final Class<T> cl, final int capacity, final float growthRate){
		data = (T[])Array.newInstance(cl, capacity);

		this.growthRate = growthRate;
	}

	public void add(final T elem){
		grow(1);

		data[limit ++] = elem;
	}

	public void addAll(final T[] array){
		addAll(array, array.length);
	}

	public void addAll(final GrowableArray<T> array){
		addAll(array.data, array.limit);
	}

	private void addAll(final T[] array, final int size){
		grow(size);

		System.arraycopy(array, 0, data, limit, size);
		limit += size;
	}

	public void addAllUnique(final T[] array){
		addAllUnique(array, array.length);
	}

	public void addAllUnique(final GrowableArray<T> array){
		addAllUnique(array.data, array.limit);
	}

	private void addAllUnique(final T[] array, final int size){
		grow(size);

		for(int i = 0; i < size; i ++)
			if(!contains(array[i]))
				data[limit ++] = array[i];
	}

	public boolean contains(final T elem){
		return (indexOf(elem) >= 0);
	}

	public void remove(final T elem){
		int index = limit;
		while(limit > 0 && (index = lastIndexOf(elem, index)) >= 0){
			final int delta = limit - index - 1;
			if(delta > 0)
				System.arraycopy(data, index + 1, data, index, delta);
			data[-- limit] = null;
		}
	}

	public int indexOf(final T elem){
		return indexOf(elem, 0);
	}

	public int indexOf(final T elem, final int startIndex){
		int i;
		if(elem == null){
			for(i = startIndex; i < data.length; i ++)
				if(data[i] == null)
					return i;
		}
		else{
			for(i = startIndex; i < data.length; i ++)
				if(elem.equals(data[i]))
					return i;
		}
		return -1;
	}

	public int lastIndexOf(final T elem, final int startIndex){
		int i;
		if(elem == null){
			for(i = startIndex - 1; i >= 0; i --)
				if(data[i] == null)
					return i;
		}
		else{
			for(i = startIndex - 1; i >= 0; i --)
				if(elem.equals(data[i]))
					return i;
		}
		return -1;
	}

	private void grow(final int size){
		final int delta = limit - data.length + size;
		if(delta > 0)
			data = Arrays.copyOf(data, data.length + (int)Math.ceil(delta * growthRate));
	}

	private Class<?> getDataType(){
		return data.getClass().getComponentType();
	}

	public boolean isEmpty(){
		return (limit == 0);
	}

	/** NOTE: this method should NOT be called at all because it is inefficient */
	public T[] extractCopyOrNull(){
		if(isEmpty())
			return null;

		final Class<?> type = getDataType();
		final T[] reducedData = (T[])Array.newInstance(type, limit);
		System.arraycopy(data, 0, reducedData, 0, limit);
		return reducedData;
	}

	public void clear(){
		data = null;
		limit = -1;
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final GrowableArray<?> rhs = (GrowableArray<?>)obj;
		return new EqualsBuilder()
			.append(data, rhs.data)
			.append(limit, rhs.limit)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(data)
			.append(limit)
			.toHashCode();
	}

}
