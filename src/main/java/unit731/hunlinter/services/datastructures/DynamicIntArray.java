package unit731.hunlinter.services.datastructures;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;


public class DynamicIntArray{

	protected static final float GROWTH_DEFAULT = 1.2f;


	public int[] data;
	public int limit;
	private final float growthRate;


	public static DynamicIntArray createExact(final int size){
		return new DynamicIntArray(size, GROWTH_DEFAULT);
	}

	public DynamicIntArray(){
		this(0, GROWTH_DEFAULT);
	}

	public DynamicIntArray(final float growthRate){
		this(0, growthRate);
	}

	public DynamicIntArray(final int capacity, final float growthRate){
		data = new int[capacity];

		this.growthRate = growthRate;
	}

	public synchronized void add(final int elem){
		grow(1);

		data[limit ++] = elem;
	}

	public synchronized void push(final int elem){
		add(elem);
	}

	public synchronized void addAll(final int[] array){
		addAll(array, array.length);
	}

	public synchronized void addAll(final DynamicIntArray array){
		addAll(array.data, array.limit);
	}

	private void addAll(final int[] array, final int size){
		grow(size);

		System.arraycopy(array, 0, data, limit, size);
		limit += size;
	}

	public synchronized void addAllUnique(final int[] array){
		addAllUnique(array, array.length);
	}

	public synchronized void addAllUnique(final DynamicIntArray array){
		addAllUnique(array.data, array.limit);
	}

	private void addAllUnique(final int[] array, final int size){
		grow(size);

		for(int i = 0; i < size; i ++)
			if(!contains(array[i]))
				data[limit ++] = array[i];
	}

	public boolean contains(final int elem){
		return (indexOf(elem) >= 0);
	}

	public synchronized void remove(final int elem){
		int index = limit;
		while(limit > 0 && (index = lastIndexOf(elem, index)) >= 0){
			final int delta = limit - index - 1;
			if(delta > 0)
				System.arraycopy(data, index + 1, data, index, delta);
			limit --;
		}
	}

	public synchronized int pop(){
		return data[-- limit];
	}

	public int indexOf(final int elem){
		return indexOf(elem, 0);
	}

	public synchronized int indexOf(final int elem, final int startIndex){
		for(int i = startIndex; i < data.length; i ++)
			if(data[i] == elem)
				return i;
		return -1;
	}

	public synchronized int lastIndexOf(final int elem, final int startIndex){
		for(int i = startIndex - 1; i >= 0; i --)
			if(data[i] == elem)
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

	/** NOTE: this method should NOT be called at all because it is inefficient */
	public synchronized int[] extractCopyOrNull(){
		if(isEmpty())
			return null;

		final int[] reducedData = new int[limit];
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

		final DynamicIntArray rhs = (DynamicIntArray)obj;
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
