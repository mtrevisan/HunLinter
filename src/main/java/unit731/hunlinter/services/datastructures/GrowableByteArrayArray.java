package unit731.hunlinter.services.datastructures;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.lang.reflect.Array;
import java.util.Arrays;


public class GrowableByteArrayArray{

	private static final float GROWTH_DEFAULT = 1.2f;


	public byte[][] data;
	public int limit;
	private final float growthRate;


	public static GrowableByteArrayArray createExact(final int size){
		return new GrowableByteArrayArray(size, GROWTH_DEFAULT);
	}

	public GrowableByteArrayArray(){
		this(0, GROWTH_DEFAULT);
	}

	public GrowableByteArrayArray(final float growthRate){
		this(0, growthRate);
	}

	public GrowableByteArrayArray(final int capacity, final float growthRate){
		data = new byte[capacity][];

		this.growthRate = growthRate;
	}

	public synchronized void add(final byte[] elem){
		grow(1);

		data[limit ++] = elem;
	}

	public synchronized void addAll(final byte[][] array){
		addAll(array, array.length);
	}

	public synchronized void addAll(final GrowableByteArrayArray array){
		addAll(array.data, array.limit);
	}

	private void addAll(final byte[][] array, final int size){
		grow(size);

		System.arraycopy(array, 0, data, limit, size);
		limit += size;
	}

	public synchronized void addAllUnique(final byte[][] array){
		addAllUnique(array, array.length);
	}

	public synchronized void addAllUnique(final GrowableByteArrayArray array){
		addAllUnique(array.data, array.limit);
	}

	private void addAllUnique(final byte[][] array, final int size){
		grow(size);

		for(int i = 0; i < size; i ++)
			if(!contains(array[i]))
				data[limit ++] = array[i];
	}

	public boolean contains(final byte[] elem){
		return (indexOf(elem) >= 0);
	}

	public synchronized void remove(final byte[] elem){
		int index = limit;
		while(limit > 0 && (index = lastIndexOf(elem, index)) >= 0){
			final int delta = limit - index - 1;
			if(delta > 0)
				System.arraycopy(data, index + 1, data, index, delta);
			data[-- limit] = null;
		}
	}

	public int indexOf(final byte[] elem){
		return indexOf(elem, 0);
	}

	public synchronized int indexOf(final byte[] elem, final int startIndex){
		if(elem == null){
			for(int i = startIndex; i < data.length; i ++)
				if(data[i] == null)
					return i;
		}
		else{
			for(int i = startIndex; i < data.length; i ++)
				if(elem.equals(data[i]))
					return i;
		}
		return -1;
	}

	public synchronized int lastIndexOf(final byte[] elem, final int startIndex){
		if(elem == null){
			for(int i = startIndex - 1; i >= 0; i --)
				if(data[i] == null)
					return i;
		}
		else{
			for(int i = startIndex - 1; i >= 0; i --)
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

	public synchronized boolean isEmpty(){
		return (limit == 0);
	}

	/** NOTE: this method should NOT be called at all because it is inefficient */
	public synchronized byte[][] extractCopyOrNull(){
		if(isEmpty())
			return null;

		final byte[][] reducedData = new byte[limit][];
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

		final GrowableByteArrayArray rhs = (GrowableByteArrayArray)obj;
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
