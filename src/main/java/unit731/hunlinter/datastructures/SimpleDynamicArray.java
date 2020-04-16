package unit731.hunlinter.datastructures;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;


public class SimpleDynamicArray<T> implements Iterable<T>{

	private static final float GROWTH_DEFAULT = 1.2f;


	public T[] data;
	public int limit;
	private final float growthRate;


	public static <T> SimpleDynamicArray<T> createExact(final Class<T> cl, final int size){
		return new SimpleDynamicArray<>(cl, size, GROWTH_DEFAULT);
	}

	public SimpleDynamicArray(final Class<T> cl){
		this(cl, 0, GROWTH_DEFAULT);
	}

	public SimpleDynamicArray(final Class<T> cl, final float growthRate){
		this(cl, 0, growthRate);
	}

	public SimpleDynamicArray(final Class<T> cl, final int capacity, final float growthRate){
		//noinspection unchecked
		data = (T[])Array.newInstance(cl, capacity);

		this.growthRate = growthRate;
	}

	public synchronized void add(final T elem){
		grow(1);

		data[limit ++] = elem;
	}

	public synchronized void addAll(final T[] array){
		addAll(array, array.length);
	}

	public synchronized void addAll(final SimpleDynamicArray<T> array){
		addAll(array.data, array.limit);
	}

	private void addAll(final T[] array, final int size){
		grow(size);

		System.arraycopy(array, 0, data, limit, size);
		limit += size;
	}

	public synchronized void addAllUnique(final T[] array){
		addAllUnique(array, array.length);
	}

	public synchronized void addAllUnique(final SimpleDynamicArray<T> array){
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

	public synchronized void remove(final T elem){
		int index = limit;
		while(limit > 0 && (index = lastIndexOf(elem, index)) >= 0){
			final int delta = limit - index - 1;
			if(delta > 0)
				System.arraycopy(data, index + 1, data, index, delta);
			data[-- limit] = null;
		}
	}

	@Override
	public synchronized Iterator<T> iterator(){
		return new Iterator<>(){
			private int idx = 0;

			public boolean hasNext(){
				return (idx < limit);
			}

			public T next(){
				return data[idx ++];
			}
		};
	}

	public int indexOf(final T elem){
		return indexOf(elem, 0);
	}

	public synchronized int indexOf(final T elem, final int startIndex){
		return (elem != null?
			indexOfNonNull(elem, startIndex):
			indexOfNull(startIndex));
	}

	private int indexOfNull(final int startIndex){
		for(int i = startIndex; i < data.length; i ++)
			if(data[i] == null)
				return i;
		return -1;
	}

	private int indexOfNonNull(final T elem, final int startIndex){
		for(int i = startIndex; i < data.length; i ++)
			if(elem.equals(data[i]))
				return i;
		return -1;
	}

	public synchronized int lastIndexOf(final T elem, final int startIndex){
		return (elem != null?
			lastIndexOfNonNull(elem, startIndex):
			lastIndexOfNull(startIndex));
	}

	private int lastIndexOfNull(final int startIndex){
		for(int i = startIndex - 1; i >= 0; i --)
			if(data[i] == null)
				return i;
		return -1;
	}

	private int lastIndexOfNonNull(final T elem, final int startIndex){
		for(int i = startIndex - 1; i >= 0; i --)
			if(elem.equals(data[i]))
				return i;
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

	public synchronized boolean isEmpty(){
		return (limit == 0);
	}

	/**
	 * NOTE: this method should NOT be called at all because it is inefficient
	 *
	 * @return	A copy of the array
	 */
	public synchronized T[] extractCopyOrNull(){
		if(isEmpty())
			return null;

		return extractCopy();
	}

	/**
	 * NOTE: this method should NOT be called at all because it is inefficient
	 *
	 * @return	A copy of the array
	 */
	public synchronized T[] extractCopy(){
		final Class<?> type = getDataType();
		//noinspection unchecked
		final T[] reducedData = (T[])Array.newInstance(type, limit);
		System.arraycopy(data, 0, reducedData, 0, limit);
		return reducedData;
	}

	public synchronized void reset(){
		limit = 0;
	}

	public synchronized void clear(){
		data = null;
		limit = -1;
	}

	@Override
	public synchronized boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final SimpleDynamicArray<?> rhs = (SimpleDynamicArray<?>)obj;
		return new EqualsBuilder()
			.append(data, rhs.data)
			.append(limit, rhs.limit)
			.isEquals();
	}

	@Override
	public synchronized int hashCode(){
		return new HashCodeBuilder()
			.append(data)
			.append(limit)
			.toHashCode();
	}

}
