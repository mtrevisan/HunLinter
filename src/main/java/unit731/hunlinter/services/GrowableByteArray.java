package unit731.hunlinter.services;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;


public class GrowableByteArray{

	private static final float GROWTH_DEFAULT = 1.2f;


	public byte[][] data;
	public int limit;
	private final float growthRate;


	public static GrowableByteArray createExact(final int size){
		return new GrowableByteArray(size, GROWTH_DEFAULT);
	}

	public GrowableByteArray(final float growthRate){
		this(0, growthRate);
	}

	public GrowableByteArray(final int capacity, final float growthRate){
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

	public synchronized void addAll(final GrowableByteArray array){
		addAll(array.data, array.limit);
	}

	private void addAll(final byte[][] array, final int size){
		grow(size);

		System.arraycopy(array, 0, data, limit, size);
		limit += size;
	}

	private void grow(final int size){
		final int delta = limit - data.length + size;
		if(delta > 0)
			data = Arrays.copyOf(data, data.length + (int)Math.ceil(delta * growthRate));
	}

	public synchronized boolean isEmpty(){
		return (limit == 0);
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

		final GrowableByteArray rhs = (GrowableByteArray)obj;
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
